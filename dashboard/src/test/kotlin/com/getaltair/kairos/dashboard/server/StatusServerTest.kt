package com.getaltair.kairos.dashboard.server

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
}
