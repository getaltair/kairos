package com.getaltair.kairos.dashboard.server

import com.getaltair.kairos.dashboard.state.DashboardStateHolder
import com.getaltair.kairos.dashboard.state.DisplayMode
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
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

class StatusServer(private val port: Int, private val stateHolder: DashboardStateHolder,) {
    private val logger = LoggerFactory.getLogger(StatusServer::class.java)
    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json()
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
                    val request = call.receive<ModeRequest>()
                    val modeStr = request.mode.lowercase()
                    val displayMode = when (modeStr) {
                        "active" -> DisplayMode.Active

                        "standby" -> DisplayMode.Standby

                        else -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ModeResponse(
                                    status = "error",
                                    mode = "Invalid mode. Use 'active' or 'standby'.",
                                ),
                            )
                            return@post
                        }
                    }
                    stateHolder.setDisplayMode(displayMode)
                    call.respond(ModeResponse(status = "ok", mode = modeStr))
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
