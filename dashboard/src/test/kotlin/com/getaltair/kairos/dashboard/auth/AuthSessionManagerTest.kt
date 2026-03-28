package com.getaltair.kairos.dashboard.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionManagerTest {

    private fun tempDir(): Path = Files.createTempDirectory("kairos-auth-test")

    // -----------------------------------------------------------------------
    // Session creation
    // -----------------------------------------------------------------------

    @Test
    fun createPendingSession_generatesValidToken() {
        val manager = AuthSessionManager(sessionDir = tempDir())
        val session = manager.createPendingSession()

        // 32 bytes encoded as hex = 64 characters
        assertEquals(64, session.sessionToken.length)
        assertTrue(session.sessionToken.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(session.expiresAt.isAfter(session.createdAt))
    }

    @Test
    fun createPendingSession_generatesUniqueTokens() {
        val manager = AuthSessionManager(sessionDir = tempDir())
        val session1 = manager.createPendingSession()
        val session2 = manager.createPendingSession()

        assertNotEquals(session1.sessionToken, session2.sessionToken)
    }

    // -----------------------------------------------------------------------
    // Persistence: write then read
    // -----------------------------------------------------------------------

    @Test
    fun persistedSession_canBeRestoredFromDisk() = runTest {
        val dir = tempDir()

        // Manually write a session file
        val persisted = PersistedSession(
            userId = "user-abc",
            email = "test@example.com",
            authenticatedAt = java.time.Instant.parse("2026-01-15T12:00:00Z"),
        )
        dir.toFile().mkdirs()
        val sessionFile = dir.resolve("session.json")
        sessionFile.writeText(Json.encodeToString(PersistedSession.serializer(), persisted))

        // Create a new manager and verify it restores
        val manager = AuthSessionManager(sessionDir = dir)
        manager.checkPersistedSession()

        val state = manager.authState.first()
        assertTrue(state is DashboardAuthState.Authenticated)
        val auth = state as DashboardAuthState.Authenticated
        assertEquals("user-abc", auth.userId)
        assertEquals("test@example.com", auth.email)
    }

    @Test
    fun restoredSession_withNullEmail() = runTest {
        val dir = tempDir()
        val persisted = PersistedSession(
            userId = "user-xyz",
            email = null,
            authenticatedAt = java.time.Instant.parse("2026-01-15T12:00:00Z"),
        )
        dir.toFile().mkdirs()
        val sessionFile = dir.resolve("session.json")
        sessionFile.writeText(Json.encodeToString(PersistedSession.serializer(), persisted))

        val manager = AuthSessionManager(sessionDir = dir)
        manager.checkPersistedSession()

        val state = manager.authState.first()
        assertTrue(state is DashboardAuthState.Authenticated)
        val auth = state as DashboardAuthState.Authenticated
        assertEquals("user-xyz", auth.userId)
        assertEquals(null, auth.email)
    }

    // -----------------------------------------------------------------------
    // Missing / corrupt session file
    // -----------------------------------------------------------------------

    @Test
    fun missingSessionFile_resultsInUnauthenticated() = runTest {
        val dir = tempDir()
        val manager = AuthSessionManager(sessionDir = dir)
        manager.checkPersistedSession()

        val state = manager.authState.first()
        assertTrue(state is DashboardAuthState.Unauthenticated)
    }

    @Test
    fun corruptSessionFile_resultsInUnauthenticated() = runTest {
        val dir = tempDir()
        dir.toFile().mkdirs()
        dir.resolve("session.json").writeText("this is not json {{{")

        val manager = AuthSessionManager(sessionDir = dir)
        manager.checkPersistedSession()

        val state = manager.authState.first()
        assertTrue(state is DashboardAuthState.Unauthenticated)
    }

    @Test
    fun emptySessionFile_resultsInUnauthenticated() = runTest {
        val dir = tempDir()
        dir.toFile().mkdirs()
        dir.resolve("session.json").writeText("")

        val manager = AuthSessionManager(sessionDir = dir)
        manager.checkPersistedSession()

        val state = manager.authState.first()
        assertTrue(state is DashboardAuthState.Unauthenticated)
    }

    // -----------------------------------------------------------------------
    // Logout
    // -----------------------------------------------------------------------

    @Test
    fun logout_deletesSessionFileAndResetsState() = runTest {
        val dir = tempDir()
        dir.toFile().mkdirs()
        val sessionFile = dir.resolve("session.json")

        // Write a session first
        val persisted = PersistedSession(
            userId = "user-logout",
            email = null,
            authenticatedAt = java.time.Instant.parse("2026-01-15T12:00:00Z"),
        )
        sessionFile.writeText(Json.encodeToString(PersistedSession.serializer(), persisted))

        val manager = AuthSessionManager(sessionDir = dir)
        manager.checkPersistedSession()

        // Verify authenticated
        assertTrue(manager.authState.first() is DashboardAuthState.Authenticated)

        // Logout
        manager.logout()

        assertTrue(manager.authState.first() is DashboardAuthState.Unauthenticated)
        assertTrue(!sessionFile.toFile().exists())
    }

    // -----------------------------------------------------------------------
    // Session token expiry
    // -----------------------------------------------------------------------

    @Test
    fun createPendingSession_hasCorrectTTL() {
        val dir = tempDir()
        val manager = AuthSessionManager(sessionDir = dir)

        // Create a session and then manually expire it by checking the field
        val session = manager.createPendingSession()
        assertNotNull(session)

        // We cannot call confirmSession without a real FirebaseAuth,
        // but we can verify the session data structure is correct
        assertTrue(session.expiresAt.isAfter(session.createdAt))
        // The TTL should be ~120 seconds
        val ttl = java.time.Duration.between(session.createdAt, session.expiresAt)
        assertEquals(120, ttl.seconds)
    }

    // -----------------------------------------------------------------------
    // Session file JSON round-trip
    // -----------------------------------------------------------------------

    @Test
    fun persistedSession_jsonRoundTrip() {
        val original = PersistedSession(
            userId = "roundtrip-user",
            email = "roundtrip@test.com",
            authenticatedAt = java.time.Instant.parse("2026-03-28T10:30:00Z"),
        )

        val jsonString = Json.encodeToString(PersistedSession.serializer(), original)
        val decoded = Json.decodeFromString<PersistedSession>(jsonString)

        assertEquals(original.userId, decoded.userId)
        assertEquals(original.email, decoded.email)
        assertEquals(original.authenticatedAt, decoded.authenticatedAt)
    }

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    fun initialState_isChecking() = runTest {
        val manager = AuthSessionManager(sessionDir = tempDir())
        val state = manager.authState.first()
        assertTrue(state is DashboardAuthState.Checking)
    }

    // -----------------------------------------------------------------------
    // Session file content verification
    // -----------------------------------------------------------------------

    @Test
    fun persistedSession_fileContainsExpectedJson() {
        val dir = tempDir()
        dir.toFile().mkdirs()
        val sessionFile = dir.resolve("session.json")

        val session = PersistedSession(
            userId = "file-check",
            email = "file@test.com",
            authenticatedAt = java.time.Instant.parse("2026-03-28T14:00:00Z"),
        )
        sessionFile.writeText(Json.encodeToString(PersistedSession.serializer(), session))

        val content = sessionFile.readText()
        assertTrue(content.contains("file-check"))
        assertTrue(content.contains("file@test.com"))
        assertTrue(content.contains("2026-03-28T14:00:00Z"))
    }

    // -----------------------------------------------------------------------
    // confirmSession -- token mismatch
    // -----------------------------------------------------------------------

    @Test
    fun confirmSession_tokenMismatch_returnsFailure() {
        val dir = tempDir()
        val mockFirebaseAuth = mockk<FirebaseAuth>()
        val manager = AuthSessionManager(sessionDir = dir, firebaseAuth = mockFirebaseAuth)

        // Create a pending session with a known token
        val pending = manager.createPendingSession()

        // Attempt to confirm with a completely wrong token
        val result = manager.confirmSession(
            sessionToken = "0000000000000000000000000000000000000000000000000000000000000000",
            firebaseIdToken = "some-firebase-token",
        )

        assertTrue("Expected failure for mismatched token", result.isFailure)
        assertTrue(
            "Error should indicate invalid token",
            result.exceptionOrNull() is IllegalArgumentException,
        )
    }

    // -----------------------------------------------------------------------
    // confirmSession -- expired session
    // -----------------------------------------------------------------------

    @Test
    fun confirmSession_expiredSession_returnsFailure() {
        val dir = tempDir()
        val mockFirebaseAuth = mockk<FirebaseAuth>()
        val manager = AuthSessionManager(sessionDir = dir, firebaseAuth = mockFirebaseAuth)

        // Create a pending session
        val pending = manager.createPendingSession()

        // Use reflection to replace the pending session with one that is already expired
        val pendingField = AuthSessionManager::class.java.getDeclaredField("pendingSession")
        pendingField.isAccessible = true

        val expiredSession = PendingSession(
            sessionToken = pending.sessionToken,
            createdAt = java.time.Instant.now().minusSeconds(300),
            expiresAt = java.time.Instant.now().minusSeconds(60),
        )
        pendingField.set(manager, expiredSession)

        val result = manager.confirmSession(
            sessionToken = pending.sessionToken,
            firebaseIdToken = "some-firebase-token",
        )

        assertTrue("Expected failure for expired session", result.isFailure)
        assertTrue(
            "Error should indicate session expired",
            result.exceptionOrNull() is IllegalStateException,
        )
    }

    // -----------------------------------------------------------------------
    // confirmSession -- Firebase verification failure
    // -----------------------------------------------------------------------

    @Test
    fun confirmSession_firebaseVerificationFailure_returnsFailure() {
        val dir = tempDir()
        val mockFirebaseAuth = mockk<FirebaseAuth>()

        // Make verifyIdToken throw an exception
        every { mockFirebaseAuth.verifyIdToken(any<String>()) } throws
            RuntimeException("Firebase token verification failed")

        val manager = AuthSessionManager(sessionDir = dir, firebaseAuth = mockFirebaseAuth)

        val pending = manager.createPendingSession()

        val result = manager.confirmSession(
            sessionToken = pending.sessionToken,
            firebaseIdToken = "invalid-firebase-token",
        )

        assertTrue("Expected failure when Firebase verification fails", result.isFailure)
        assertTrue(
            "Error message should contain verification failure",
            result.exceptionOrNull()?.message?.contains("verification failed") == true,
        )
    }

    // -----------------------------------------------------------------------
    // confirmSession -- success persists session and updates state
    // -----------------------------------------------------------------------

    @Test
    fun confirmSession_success_persistsSessionAndUpdatesState() = runTest {
        val dir = tempDir()
        val mockFirebaseAuth = mockk<FirebaseAuth>()
        val mockFirebaseToken = mockk<FirebaseToken>()

        every { mockFirebaseToken.uid } returns "firebase-uid-123"
        every { mockFirebaseToken.email } returns "user@example.com"
        every { mockFirebaseAuth.verifyIdToken(any<String>()) } returns mockFirebaseToken

        val manager = AuthSessionManager(sessionDir = dir, firebaseAuth = mockFirebaseAuth)

        val pending = manager.createPendingSession()

        val result = manager.confirmSession(
            sessionToken = pending.sessionToken,
            firebaseIdToken = "valid-firebase-token",
        )

        assertTrue("Expected success for valid confirmation", result.isSuccess)

        val persisted = result.getOrNull()
        assertNotNull("Persisted session should not be null", persisted)
        assertEquals("firebase-uid-123", persisted!!.userId)
        assertEquals("user@example.com", persisted.email)

        // Verify session file was written to disk
        val sessionFile = dir.resolve("session.json")
        assertTrue("Session file should exist on disk", sessionFile.exists())

        val fileContent = sessionFile.readText()
        assertTrue("File should contain the user ID", fileContent.contains("firebase-uid-123"))
        assertTrue("File should contain the email", fileContent.contains("user@example.com"))

        // Verify auth state is Authenticated
        val state = manager.authState.first()
        assertTrue("State should be Authenticated", state is DashboardAuthState.Authenticated)
        val authState = state as DashboardAuthState.Authenticated
        assertEquals("firebase-uid-123", authState.userId)
        assertEquals("user@example.com", authState.email)
    }

    // -----------------------------------------------------------------------
    // confirmSession -- disk write failure
    // -----------------------------------------------------------------------

    @Test
    fun confirmSession_diskWriteFailure_returnsFailure() {
        val mockFirebaseAuth = mockk<FirebaseAuth>()
        val mockFirebaseToken = mockk<FirebaseToken>()

        every { mockFirebaseToken.uid } returns "firebase-uid-456"
        every { mockFirebaseToken.email } returns "user@test.com"
        every { mockFirebaseAuth.verifyIdToken(any<String>()) } returns mockFirebaseToken

        // Use a path that cannot be written to (a file path, not a directory)
        val readOnlyDir = tempDir()
        readOnlyDir.toFile().mkdirs()
        readOnlyDir.toFile().setReadOnly()

        val manager =
            AuthSessionManager(sessionDir = readOnlyDir.resolve("nonexistent/nested"), firebaseAuth = mockFirebaseAuth)

        val pending = manager.createPendingSession()

        val result = manager.confirmSession(
            sessionToken = pending.sessionToken,
            firebaseIdToken = "valid-token",
        )

        assertTrue("Expected failure when disk write fails", result.isFailure)

        // Clean up: restore write permission so the temp dir can be cleaned
        readOnlyDir.toFile().setWritable(true)
    }

    // -----------------------------------------------------------------------
    // createPendingSession -- replaces old session
    // -----------------------------------------------------------------------

    @Test
    fun createPendingSession_replacesOldSession() {
        val dir = tempDir()
        val mockFirebaseAuth = mockk<FirebaseAuth>()
        val manager = AuthSessionManager(sessionDir = dir, firebaseAuth = mockFirebaseAuth)

        val session1 = manager.createPendingSession()
        val session2 = manager.createPendingSession()

        // The second session should have a different token
        assertNotEquals(
            "New pending session should have a different token",
            session1.sessionToken,
            session2.sessionToken,
        )

        // The first token should no longer be accepted
        val result = manager.confirmSession(
            sessionToken = session1.sessionToken,
            firebaseIdToken = "some-token",
        )

        assertTrue(
            "Old session token should be rejected after a new session is created",
            result.isFailure,
        )
        assertTrue(
            "Error should indicate invalid token",
            result.exceptionOrNull() is IllegalArgumentException,
        )
    }
}
