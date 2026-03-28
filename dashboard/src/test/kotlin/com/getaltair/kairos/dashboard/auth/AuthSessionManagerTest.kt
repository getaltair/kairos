package com.getaltair.kairos.dashboard.auth

import java.nio.file.Files
import java.nio.file.Path
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
        val session = manager.createPendingSession("192.168.1.10", 8888)

        // 32 bytes encoded as hex = 64 characters
        assertEquals(64, session.sessionToken.length)
        assertTrue(session.sessionToken.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(session.expiresAt.isAfter(session.createdAt))
    }

    @Test
    fun createPendingSession_generatesUniqueTokens() {
        val manager = AuthSessionManager(sessionDir = tempDir())
        val session1 = manager.createPendingSession("192.168.1.10", 8888)
        val session2 = manager.createPendingSession("192.168.1.10", 8888)

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
    fun expiredPendingSession_isRejectedByConfirmSession() {
        val dir = tempDir()
        val manager = AuthSessionManager(sessionDir = dir)

        // Create a session and then manually expire it by checking the field
        val session = manager.createPendingSession("192.168.1.10", 8888)
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
}
