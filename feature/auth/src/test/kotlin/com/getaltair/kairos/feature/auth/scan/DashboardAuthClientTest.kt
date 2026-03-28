package com.getaltair.kairos.feature.auth.scan

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardAuthClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: DashboardAuthClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = DashboardAuthClient()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // -------------------------------------------------------------------------
    // 1. Successful auth confirmation returns true
    // -------------------------------------------------------------------------

    @Test
    fun `confirmAuth success returns true`(): Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"message":"ok"}"""),
        )

        val result = client.confirmAuth(
            host = mockWebServer.hostName,
            port = mockWebServer.port,
            sessionToken = "test-session-token",
            firebaseIdToken = "test-firebase-id-token",
        )

        assertTrue("Expected success result", result.isSuccess)

        val response = result.getOrThrow()
        // The production code reads json.getString("message") as userId
        assertEquals("ok", response.userId)

        // Verify the request was sent correctly
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/confirm", request.path)
        assertTrue(
            "Request body should contain sessionToken",
            request.body.readUtf8().contains("test-session-token"),
        )
    }

    // -------------------------------------------------------------------------
    // 2. Non-200 response returns failure with IOException
    // -------------------------------------------------------------------------

    @Test
    fun `confirmAuth non200 throws IOException`(): Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"Unauthorized"}"""),
        )

        val result = client.confirmAuth(
            host = mockWebServer.hostName,
            port = mockWebServer.port,
            sessionToken = "test-session-token",
            firebaseIdToken = "test-firebase-id-token",
        )

        assertTrue("Expected failure result", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(
            "Expected IOException but got ${exception?.javaClass?.simpleName}",
            exception is IOException,
        )
        assertTrue(
            "Expected error message to contain HTTP 401",
            exception?.message?.contains("401") == true,
        )
    }

    // -------------------------------------------------------------------------
    // 3. Malformed JSON in 200 response returns failure
    // -------------------------------------------------------------------------

    @Test
    fun `confirmAuth malformed json throws exception`(): Unit = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("not valid json at all {{{"),
        )

        val result = client.confirmAuth(
            host = mockWebServer.hostName,
            port = mockWebServer.port,
            sessionToken = "test-session-token",
            firebaseIdToken = "test-firebase-id-token",
        )

        assertTrue("Expected failure result", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(
            "Expected JSONException but got ${exception?.javaClass?.simpleName}",
            exception is org.json.JSONException,
        )
    }

    // -------------------------------------------------------------------------
    // 4. Blank host throws IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `confirmAuth blank host throws IllegalArgumentException`(): Unit = runBlocking {
        client.confirmAuth(
            host = "",
            port = 8080,
            sessionToken = "test-session-token",
            firebaseIdToken = "test-firebase-id-token",
        )
    }

    // -------------------------------------------------------------------------
    // 5. Invalid port throws IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `confirmAuth invalid port throws IllegalArgumentException`(): Unit = runBlocking {
        client.confirmAuth(
            host = "192.168.1.10",
            port = 0,
            sessionToken = "test-session-token",
            firebaseIdToken = "test-firebase-id-token",
        )
    }
}
