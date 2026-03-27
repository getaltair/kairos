package com.getaltair.kairos.dashboard

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.getaltair.kairos.dashboard.config.DashboardConfig
import com.getaltair.kairos.dashboard.data.FirebaseAdminClient
import com.getaltair.kairos.dashboard.server.ModeServer
import com.getaltair.kairos.dashboard.state.DashboardStateHolder
import com.getaltair.kairos.dashboard.ui.DashboardScreen
import com.getaltair.kairos.dashboard.ui.theme.DashboardTheme
import com.getaltair.kairos.dashboard.util.rememberScreenSaverOffset
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("KairosDashboard")

fun main() {
    logger.info("Kairos Dashboard starting...")

    val config = DashboardConfig.load()
    logger.info("Config loaded: userId=${config.firebaseUserId}, fullscreen=${config.fullscreen}")

    val firebaseClient = FirebaseAdminClient(config)
    firebaseClient.initialize()
    logger.info("Firebase Admin SDK initialized")

    val stateHolder = DashboardStateHolder(firebaseClient, config.firebaseUserId)
    stateHolder.start()
    logger.info("State holder started")

    val modeServer = ModeServer(config.serverPort, stateHolder)
    modeServer.start()
    logger.info("Mode server started on port ${config.serverPort}")

    val windowState = if (config.fullscreen) {
        WindowState(placement = WindowPlacement.Fullscreen)
    } else {
        WindowState(size = DpSize(config.width.dp, config.height.dp))
    }

    application {
        Window(
            onCloseRequest = {
                stateHolder.close()
                modeServer.stop()
                exitApplication()
            },
            state = windowState,
            title = "Kairos Dashboard",
            undecorated = config.fullscreen,
        ) {
            DashboardTheme {
                val state by stateHolder.state.collectAsState()
                val offset = rememberScreenSaverOffset()
                DashboardScreen(state = state, offset = offset)
            }
        }
    }
}
