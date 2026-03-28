package com.getaltair.kairos.dashboard.server

import com.getaltair.kairos.dashboard.state.DashboardStateHolder
import com.getaltair.kairos.dashboard.state.DisplayMode
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class StatusResponse(
    val totalHabits: Int,
    val completedHabits: Int,
    val connectionStatus: String,
    val isStale: Boolean,
    val displayMode: String,
)

@Serializable
data class ModeRequest(val mode: String)

@Serializable
data class ModeResponse(val status: String, val mode: String)

@Serializable
data class ErrorResponse(val status: String = "error", val error: String)

class StatusServer(private val port: Int, private val stateHolder: DashboardStateHolder) {
    private val logger = LoggerFactory.getLogger(StatusServer::class.java)
    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json()
            }
            install(StatusPages) {
                exception<ContentTransformationException> { call, cause ->
                    logger.warn("Bad request on {}: {}", call.request.uri, cause.message)
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
                        logger.warn("Malformed /mode request: {}", e.message)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "Invalid request body. Expected JSON: {\"mode\": \"active|standby\"}",
                            ),
                        )
                        return@post
                    }
                    logger.info(
                        "Mode change to '{}' requested from {}",
                        request.mode.lowercase(),
                        call.request.local.remoteAddress,
                    )
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
        server?.start(wait = false)
        logger.info("StatusServer started on port $port")
    }

    fun stop() {
        try {
            server?.stop(1000, 2000)
        } catch (e: Exception) {
            logger.warn("Error stopping StatusServer", e)
        }
    }
}
