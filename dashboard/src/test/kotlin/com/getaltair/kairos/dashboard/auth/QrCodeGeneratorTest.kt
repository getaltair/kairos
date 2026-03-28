package com.getaltair.kairos.dashboard.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeGeneratorTest {

    @Test
    fun buildQrDataString_producesExpectedFormat() {
        val result = buildQrDataString(
            host = "192.168.1.42",
            port = 8888,
            sessionToken = "abc123def456",
        )

        assertEquals(
            "kairos://link-dashboard?host=192.168.1.42&port=8888&session=abc123def456",
            result,
        )
    }

    @Test
    fun buildQrDataString_handlesLocalhostAndDifferentPort() {
        val result = buildQrDataString(
            host = "127.0.0.1",
            port = 3000,
            sessionToken = "token99",
        )

        assertEquals(
            "kairos://link-dashboard?host=127.0.0.1&port=3000&session=token99",
            result,
        )
    }

    @Test
    fun buildQrDataString_handlesLongHexToken() {
        // A realistic 64-character hex token (32 bytes)
        val hexToken = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"

        val result = buildQrDataString(
            host = "10.0.0.1",
            port = 8888,
            sessionToken = hexToken,
        )

        assertEquals(
            "kairos://link-dashboard?host=10.0.0.1&port=8888&session=$hexToken",
            result,
        )
    }
}
