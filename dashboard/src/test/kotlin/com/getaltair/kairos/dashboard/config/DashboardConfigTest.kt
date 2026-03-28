package com.getaltair.kairos.dashboard.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardConfigTest {

    // -----------------------------------------------------------------------
    // Valid construction
    // -----------------------------------------------------------------------

    @Test
    fun validConfig_loadsCorrectly() {
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/opt/kairos/service-account.json",
            firebaseUserId = "user-123",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
        )

        assertEquals("/opt/kairos/service-account.json", config.firebaseServiceAccountPath)
        assertEquals("user-123", config.firebaseUserId)
        assertTrue(config.fullscreen)
        assertEquals(1920, config.width)
        assertEquals(1080, config.height)
        assertEquals(8888, config.serverPort)
        assertEquals("0.0.0.0", config.serverHost)
    }

    @Test
    fun defaultsApplyForOptionalProperties() {
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/opt/sa.json",
            firebaseUserId = "uid-456",
            fullscreen = false,
            width = 800,
            height = 480,
            serverPort = 9090,
        )

        assertEquals(800, config.width)
        assertEquals(480, config.height)
        assertEquals(9090, config.serverPort)
        assertFalse(config.fullscreen)
        assertEquals("0.0.0.0", config.serverHost)
    }

    @Test
    fun edgePorts_validBoundaries() {
        val minPort = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = false,
            width = 100,
            height = 100,
            serverPort = 1,
        )
        assertEquals(1, minPort.serverPort)

        val maxPort = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = false,
            width = 100,
            height = 100,
            serverPort = 65535,
        )
        assertEquals(65535, maxPort.serverPort)
    }

    // -----------------------------------------------------------------------
    // firebaseUserId is optional (nullable)
    // -----------------------------------------------------------------------

    @Test
    fun nullUserId_isAccepted() {
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = null,
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
        )
        assertNull(config.firebaseUserId)
    }

    @Test
    fun blankUserId_isPassedThrough() {
        // The constructor no longer rejects blank -- load() normalises blanks to null.
        // A blank string passed directly to the constructor is still a valid String?.
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
        )
        assertEquals("", config.firebaseUserId)
    }

    // -----------------------------------------------------------------------
    // serverHost property
    // -----------------------------------------------------------------------

    @Test
    fun defaultServerHost_isAllInterfaces() {
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
        )
        assertEquals("0.0.0.0", config.serverHost)
    }

    @Test
    fun customServerHost_isPreserved() {
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
            serverHost = "192.168.1.100",
        )
        assertEquals("192.168.1.100", config.serverHost)
    }

    @Test
    fun localhostServerHost_isAccepted() {
        val config = DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
            serverHost = "127.0.0.1",
        )
        assertEquals("127.0.0.1", config.serverHost)
    }

    // -----------------------------------------------------------------------
    // firebaseServiceAccountPath validation
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun blankServiceAccountPath_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "   ",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyServiceAccountPath_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 8888,
        )
    }

    // -----------------------------------------------------------------------
    // serverPort validation
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun invalidPortZero_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 0,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPortNegative_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = -1,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPortTooHigh_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 1080,
            serverPort = 65536,
        )
    }

    // -----------------------------------------------------------------------
    // width validation
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun invalidWidthZero_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 0,
            height = 1080,
            serverPort = 8888,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidWidthNegative_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = -100,
            height = 1080,
            serverPort = 8888,
        )
    }

    // -----------------------------------------------------------------------
    // height validation
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun invalidHeightZero_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = 0,
            serverPort = 8888,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHeightNegative_fails() {
        DashboardConfig(
            firebaseServiceAccountPath = "/sa.json",
            firebaseUserId = "uid",
            fullscreen = true,
            width = 1920,
            height = -480,
            serverPort = 8888,
        )
    }

    // -----------------------------------------------------------------------
    // load() with test properties
    // -----------------------------------------------------------------------

    @Test
    fun missingRequiredProperty_throwsWithPropertyName() {
        try {
            DashboardConfig(
                firebaseServiceAccountPath = "",
                firebaseUserId = "some-user",
                fullscreen = true,
                width = 1920,
                height = 1080,
                serverPort = 8080,
            )
            @Suppress("UNREACHABLE_CODE")
            assertTrue("Expected IllegalArgumentException for blank required property", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Error should mention the property: ${e.message}",
                e.message?.contains("firebaseServiceAccountPath") == true,
            )
        }
    }

    @Test
    fun invalidIntegerWidth_throwsWithPropertyName() {
        try {
            DashboardConfig(
                firebaseServiceAccountPath = "/sa.json",
                firebaseUserId = "uid",
                fullscreen = true,
                width = -999,
                height = 1080,
                serverPort = 8888,
            )
            @Suppress("UNREACHABLE_CODE")
            assertTrue("Expected IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Error should mention 'width': ${e.message}",
                e.message?.contains("width") == true,
            )
        }
    }
}
