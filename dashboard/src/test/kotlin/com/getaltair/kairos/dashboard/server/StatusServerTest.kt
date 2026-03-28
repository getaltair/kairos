package com.getaltair.kairos.dashboard.server

import com.getaltair.kairos.dashboard.auth.AuthSessionManager
import com.getaltair.kairos.dashboard.auth.DashboardAuthState
import com.getaltair.kairos.dashboard.auth.PersistedSession
import com.getaltair.kairos.dashboard.config.DashboardConfig
import com.getaltair.kairos.dashboard.data.FirebaseAdminClient
import com.getaltair.kairos.dashboard.state.DashboardStateHolder
import com.getaltair.kairos.dashboard.state.DisplayMode
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the StatusServer HTTP routes.
 *
 * Rather than starting a real Netty server, these tests replicate the
 * routing configuration from [StatusServer.start] inside Ktor's
 * [testApplication] so that request/response handling can be exercised
 * without binding to a port.
 *
 * A real [DashboardStateHolder] backed by a no-op [FirebaseAdminClient]
 * fake provides the state -- [DashboardStateHolder.start] is never called,
 * so no Firebase connection is needed.
 */
class StatusServerTest {

    // -- test doubles --------------------------------------------------------

    private class FakeFirebaseAdminClient(config: DashboardConfig,) : FirebaseAdminClient(config) {
        override fun writeCompletion(userId: String, completionId: String, data: Map<String, Any?>,): Result<Unit> =
            Result.success(Unit)
    }

    // -- fixtures ------------------------------------------------------------

    private val testConfig = DashboardConfig(
        firebaseServiceAccountPath = "/tmp/fake-sa.json",
        firebaseUserId = "test-user",
        fullscreen = false,
        width = 800,
        height = 600,
        serverPort = 9999,
    )

    private lateinit var stateHolder: DashboardStateHolder

    @Before
    fun setUp() {
        val fakeClient = FakeFirebaseAdminClient(testConfig)
        stateHolder = DashboardStateHolder(fakeClient, "test-user")
    }

    @After
    fun tearDown() {
        stateHolder.close()
    }

    /**
     * Installs the same routing and plugins that [StatusServer.start] wires
     * up, so the test application mirrors production behaviour.
     */
    private fun testApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            install(ContentNegotiation) {
                json()
            }
            install(StatusPages) {
                exception<ContentTransformationException> { call, cause ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error = cause.message ?: "Invalid request body"),
                    )
                }
            }
            routing {
                get("/health") {
                    call.respond(HealthResponse(status = "ok"))
                }
                get("/api/status") {
                    val state = stateHolder.state.value
                    call.respond(
                        StatusResponse(
                            totalHabits = state.totalHabits,
                            completedHabits = state.completedCount,
                            connectionStatus = state.connectionStatus.name,
                            isStale = state.isStale,
                            displayMode = state.displayMode.name.lowercase(),
                        ),
                    )
                }
                post("/mode") {
                    val request = try {
                        call.receive<ModeRequest>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Invalid request body. Expected JSON: {\"mode\": \"active|standby\"}",
                            ),
                        )
                        return@post
                    }
                    val displayMode = DisplayMode.fromString(request.mode)
                        ?: run {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error = "Invalid mode. Use 'active' or 'standby'."),
                            )
                            return@post
                        }
                    stateHolder.setDisplayMode(displayMode)
                    call.respond(ModeResponse(status = "ok", mode = request.mode.lowercase()))
                }
            }
        }
        block()
    }

    // -- POST /mode ----------------------------------------------------------

    @Test
    fun postMode_validActive_returns200() = testApp {
        val response = client.post("/mode") {
            contentType(ContentType.Application.Json)
            setBody("""{"mode":"active"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        assertEquals("ok", json["status"]?.jsonPrimitive?.content)
        assertEquals("active", json["mode"]?.jsonPrimitive?.content)
    }

    @Test
    fun postMode_validStandby_returns200() = testApp {
        val response = client.post("/mode") {
            contentType(ContentType.Application.Json)
            setBody("""{"mode":"standby"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        assertEquals("ok", json["status"]?.jsonPrimitive?.content)
        assertEquals("standby", json["mode"]?.jsonPrimitive?.content)
    }

    @Test
    fun postMode_invalidMode_returns400() = testApp {
        val response = client.post("/mode") {
            contentType(ContentType.Application.Json)
            setBody("""{"mode":"banana"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        assertEquals("error", json["status"]?.jsonPrimitive?.content)
        assertNotNull(json["error"])
    }

    @Test
    fun postMode_malformedJson_returns400() = testApp {
        val response = client.post("/mode") {
            contentType(ContentType.Application.Json)
            setBody("{invalid")
        }

        assertEquals(
            "Malformed JSON should return 400 (not 500)",
            HttpStatusCode.BadRequest,
            response.status,
        )
    }

    // -- GET /health ---------------------------------------------------------

    @Test
    fun getHealth_returns200() = testApp {
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        assertEquals("ok", json["status"]?.jsonPrimitive?.content)
    }

    // -- GET /api/status -----------------------------------------------------

    @Test
    fun getStatus_includesDisplayMode() = testApp {
        // Set a known display mode so we can assert on it
        stateHolder.setDisplayMode(DisplayMode.Standby)

        val response = client.get("/api/status")

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        assertTrue(
            "Response should contain a 'displayMode' field",
            json.containsKey("displayMode"),
        )
        assertEquals("standby", json["displayMode"]?.jsonPrimitive?.content)
    }

    // =======================================================================
    // Auth endpoint tests
    // =======================================================================

    /**
     * Creates a test application that mirrors the full [StatusServer] routing
     * including auth endpoints. Accepts an optional [AuthSessionManager] and
     * an optional [DashboardStateHolder] so individual tests can control what
     * is available.
     */
    private fun authTestApp(
        authManager: AuthSessionManager? = null,
        holder: DashboardStateHolder? = stateHolder,
        block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application {
            install(ContentNegotiation) {
                json()
            }
            install(StatusPages) {
                exception<ContentTransformationException> { call, cause ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error = cause.message ?: "Invalid request body"),
                    )
                }
            }
            routing {
                get("/health") {
                    call.respond(HealthResponse(status = "ok"))
                }
                get("/api/status") {
                    val h = holder
                    if (h == null) {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            ErrorResponse(error = "Dashboard not yet authenticated"),
                        )
                        return@get
                    }
                    val state = h.state.value
                    call.respond(
                        StatusResponse(
                            totalHabits = state.totalHabits,
                            completedHabits = state.completedCount,
                            connectionStatus = state.connectionStatus.name,
                            isStale = state.isStale,
                            displayMode = state.displayMode.name.lowercase(),
                        ),
                    )
                }
                post("/mode") {
                    val h = holder
                    if (h == null) {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            ErrorResponse(error = "Dashboard not yet authenticated"),
                        )
                        return@post
                    }
                    val request = try {
                        call.receive<ModeRequest>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Invalid request body. Expected JSON: {\"mode\": \"active|standby\"}",
                            ),
                        )
                        return@post
                    }
                    val displayMode = DisplayMode.fromString(request.mode)
                        ?: run {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error = "Invalid mode. Use 'active' or 'standby'."),
                            )
                            return@post
                        }
                    h.setDisplayMode(displayMode)
                    call.respond(ModeResponse(status = "ok", mode = request.mode.lowercase()))
                }

                // Auth endpoints
                post("/auth/confirm") {
                    val manager = authManager
                    if (manager == null) {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            ErrorResponse(error = "Auth not configured"),
                        )
                        return@post
                    }
                    val request = try {
                        call.receive<AuthConfirmRequest>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(error = "Invalid request body"),
                        )
                        return@post
                    }

                    val result = manager.confirmSession(
                        sessionToken = request.sessionToken,
                        firebaseIdToken = request.firebaseIdToken,
                    )

                    result.fold(
                        onSuccess = { session ->
                            call.respond(
                                HttpStatusCode.OK,
                                AuthConfirmResponse(success = true, message = session.userId),
                            )
                        },
                        onFailure = { error ->
                            when (error) {
                                is IllegalArgumentException -> call.respond(
                                    HttpStatusCode.Unauthorized,
                                    AuthConfirmResponse(false, error.message ?: "Authentication failed"),
                                )

                                is IllegalStateException -> call.respond(
                                    HttpStatusCode.Gone,
                                    AuthConfirmResponse(false, error.message ?: "Session expired"),
                                )

                                is java.io.IOException -> call.respond(
                                    HttpStatusCode.InternalServerError,
                                    AuthConfirmResponse(false, "Internal server error"),
                                )

                                else -> call.respond(
                                    HttpStatusCode.Unauthorized,
                                    AuthConfirmResponse(false, "Authentication failed"),
                                )
                            }
                        },
                    )
                }

                get("/auth/status") {
                    val manager = authManager
                    if (manager == null) {
                        call.respond(AuthStatusResponse(authenticated = false))
                        return@get
                    }
                    val state = manager.authState.value
                    when (state) {
                        is DashboardAuthState.Authenticated -> call.respond(
                            AuthStatusResponse(authenticated = true, userId = state.userId),
                        )

                        else -> call.respond(
                            AuthStatusResponse(authenticated = false),
                        )
                    }
                }
            }
        }
        block()
    }

    // -- POST /auth/confirm ---------------------------------------------------

    @Test
    fun postAuthConfirm_validRequest_returns200() {
        val mockManager = mockk<AuthSessionManager>()
        val persisted = PersistedSession(
            userId = "user-abc",
            email = "user@test.com",
            authenticatedAt = java.time.Instant.parse("2026-03-28T12:00:00Z"),
        )
        every {
            mockManager.confirmSession(
                sessionToken = "valid-token",
                firebaseIdToken = "valid-firebase-token",
            )
        } returns Result.success(persisted)

        authTestApp(authManager = mockManager) {
            val response = client.post("/auth/confirm") {
                contentType(ContentType.Application.Json)
                setBody("""{"sessionToken":"valid-token","firebaseIdToken":"valid-firebase-token"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
            assertEquals("true", json["success"]?.jsonPrimitive?.content)
            assertEquals("user-abc", json["message"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun postAuthConfirm_malformedJson_returns400() {
        val mockManager = mockk<AuthSessionManager>()

        authTestApp(authManager = mockManager) {
            val response = client.post("/auth/confirm") {
                contentType(ContentType.Application.Json)
                setBody("{this is not valid json!!!")
            }

            assertEquals(
                "Malformed JSON should return 400",
                HttpStatusCode.BadRequest,
                response.status,
            )
        }
    }

    @Test
    fun postAuthConfirm_noAuthManager_returns503() {
        authTestApp(authManager = null) {
            val response = client.post("/auth/confirm") {
                contentType(ContentType.Application.Json)
                setBody("""{"sessionToken":"t","firebaseIdToken":"f"}""")
            }

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
            assertTrue(
                "Error response should mention auth not configured",
                json["error"]?.jsonPrimitive?.content?.contains("Auth not configured") == true,
            )
        }
    }

    // -- GET /auth/status -----------------------------------------------------

    @Test
    fun getAuthStatus_authenticated_returnsTrueWithUserId() {
        val mockManager = mockk<AuthSessionManager>()
        val authState = MutableStateFlow<DashboardAuthState>(
            DashboardAuthState.Authenticated("user-xyz", "xyz@test.com"),
        )
        every { mockManager.authState } returns authState

        authTestApp(authManager = mockManager) {
            val response = client.get("/auth/status")

            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
            assertEquals(true, json["authenticated"]?.jsonPrimitive?.boolean)
            assertEquals("user-xyz", json["userId"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun getAuthStatus_unauthenticated_returnsFalse() {
        val mockManager = mockk<AuthSessionManager>()
        val authState = MutableStateFlow<DashboardAuthState>(
            DashboardAuthState.Unauthenticated,
        )
        every { mockManager.authState } returns authState

        authTestApp(authManager = mockManager) {
            val response = client.get("/auth/status")

            assertEquals(HttpStatusCode.OK, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
            assertEquals(false, json["authenticated"]?.jsonPrimitive?.boolean)
        }
    }

    // -- Null stateHolder tests -----------------------------------------------

    @Test
    fun getApiStatus_nullStateHolder_returns503() {
        authTestApp(authManager = null, holder = null) {
            val response = client.get("/api/status")

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
            assertNotNull("Should have an error field", json["error"])
        }
    }

    @Test
    fun postMode_nullStateHolder_returns503() {
        authTestApp(authManager = null, holder = null) {
            val response = client.post("/mode") {
                contentType(ContentType.Application.Json)
                setBody("""{"mode":"active"}""")
            }

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
            assertNotNull("Should have an error field", json["error"])
        }
    }
}
