package com.getaltair.kairos.dashboard.auth

import com.google.firebase.auth.FirebaseAuth
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private const val SESSION_FILE = "session.json"
private const val TOKEN_BYTE_LENGTH = 32

/**
 * Manages the dashboard authentication lifecycle:
 * 1. Check for a persisted session on disk.
 * 2. If absent, create a pending session for QR login.
 * 3. Confirm the session when the mobile app sends a Firebase ID token.
 * 4. Persist the confirmed session to disk for future restarts.
 */
class AuthSessionManager(
    private val sessionDir: Path = Path.of(System.getProperty("user.home"), ".kairos"),
    firebaseAuth: FirebaseAuth? = null,
) {
    private val firebaseAuth: FirebaseAuth by lazy { firebaseAuth ?: FirebaseAuth.getInstance() }
    private val log = LoggerFactory.getLogger(AuthSessionManager::class.java)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val random = SecureRandom()

    private val _authState = MutableStateFlow<DashboardAuthState>(DashboardAuthState.Checking)

    /** Observable authentication state. */
    val authState: StateFlow<DashboardAuthState> = _authState.asStateFlow()

    // Only one pending session at a time
    @Volatile
    private var pendingSession: PendingSession? = null

    private val sessionFile: Path
        get() = sessionDir.resolve(SESSION_FILE)

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Checks for an existing `session.json` on disk. If found and parseable,
     * transitions to [DashboardAuthState.Authenticated]; otherwise transitions
     * to [DashboardAuthState.Unauthenticated].
     */
    fun checkPersistedSession() {
        try {
            if (sessionFile.exists()) {
                val text = sessionFile.readText()
                val session = json.decodeFromString<PersistedSession>(text)
                log.info("Restored persisted session for user {}", session.userId)
                _authState.value = DashboardAuthState.Authenticated(session.userId, session.email)
                return
            }
        } catch (e: Exception) {
            log.warn("Failed to read persisted session, treating as unauthenticated", e)
            if (e is SecurityException) {
                log.error("Security exception reading session file -- possible tampering", e)
            }
        }
        _authState.value = DashboardAuthState.Unauthenticated
    }

    /**
     * Creates a cryptographically random pending session for QR-code login.
     *
     * @return the [PendingSession] whose token is encoded in the QR payload
     */
    fun createPendingSession(): PendingSession {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH)
        random.nextBytes(tokenBytes)
        val token = tokenBytes.joinToString("") { "%02x".format(it) }

        val now = Instant.now()
        val session = PendingSession(
            sessionToken = token,
            createdAt = now,
            expiresAt = now.plusSeconds(SESSION_TTL_SECONDS),
        )
        pendingSession = session
        log.info("Created pending session (expires {})", session.expiresAt)
        return session
    }

    /**
     * Confirms a pending session using a Firebase ID token sent by the mobile app.
     *
     * @param sessionToken the token from the QR code
     * @param firebaseIdToken the Firebase ID token obtained by the mobile app
     * @return [Result.success] with the persisted session, or [Result.failure]
     */
    @Synchronized
    fun confirmSession(sessionToken: String, firebaseIdToken: String,): Result<PersistedSession> {
        // Validate the pending session token (constant-time comparison)
        val pending = pendingSession
        if (pending == null ||
            !MessageDigest.isEqual(pending.sessionToken.toByteArray(), sessionToken.toByteArray())
        ) {
            log.warn("Session token mismatch or no pending session")
            return Result.failure(IllegalArgumentException("Invalid or unknown session token"))
        }
        if (pending.isExpired) {
            log.warn("Pending session expired at {}", pending.expiresAt)
            pendingSession = null
            return Result.failure(IllegalStateException("Session token has expired"))
        }

        // Verify the Firebase ID token via Admin SDK
        val firebaseToken = try {
            firebaseAuth.verifyIdToken(firebaseIdToken)
        } catch (e: Exception) {
            log.error("Firebase ID token verification failed: {}", e.message, e)
            return Result.failure(e)
        }

        val uid = firebaseToken.uid
        if (uid.isNullOrBlank()) {
            log.error("Firebase token verified but UID was blank")
            return Result.failure(IllegalStateException("Firebase token contained no user ID"))
        }
        val email = firebaseToken.email

        val persisted = PersistedSession(
            userId = uid,
            email = email,
            authenticatedAt = Instant.now(),
        )

        // Write to disk
        try {
            sessionDir.createDirectories()
            sessionFile.writeText(json.encodeToString(PersistedSession.serializer(), persisted))
            try {
                java.nio.file.Files.setPosixFilePermissions(
                    sessionFile,
                    PosixFilePermissions.fromString("rw-------"),
                )
            } catch (_: UnsupportedOperationException) {
            }
            log.info("Persisted session for user {} to {}", uid, sessionFile)
        } catch (e: Exception) {
            log.error("Failed to persist session: {}", e.message, e)
            return Result.failure(e)
        }

        // Clear the pending session and update auth state
        pendingSession = null
        _authState.value = DashboardAuthState.Authenticated(uid, email)
        return Result.success(persisted)
    }

    /**
     * Logs out by deleting the persisted session file and resetting auth state.
     */
    fun logout() {
        try {
            sessionFile.deleteIfExists()
            log.info("Deleted session file")
        } catch (e: Exception) {
            log.warn("Failed to delete session file: {}", e.message)
        }
        pendingSession = null
        _authState.value = DashboardAuthState.Unauthenticated
    }
}
