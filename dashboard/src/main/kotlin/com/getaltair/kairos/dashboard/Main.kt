package com.getaltair.kairos.dashboard

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.getaltair.kairos.dashboard.auth.AuthSessionManager
import com.getaltair.kairos.dashboard.auth.DashboardAuthState
import com.getaltair.kairos.dashboard.config.DashboardConfig
import com.getaltair.kairos.dashboard.data.FirebaseAdminClient
import com.getaltair.kairos.dashboard.server.StatusServer
import com.getaltair.kairos.dashboard.state.DashboardStateHolder
import com.getaltair.kairos.dashboard.ui.DashboardScreen
import com.getaltair.kairos.dashboard.ui.LoginScreen
import com.getaltair.kairos.dashboard.ui.theme.DashboardTheme
import com.getaltair.kairos.dashboard.util.rememberScreenSaverOffset
import kotlin.system.exitProcess
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("KairosDashboard")

fun main() {
    logger.info("Kairos Dashboard starting...")

    val config = try {
        DashboardConfig.load()
    } catch (e: Exception) {
        logger.error("Failed to load dashboard configuration: ${e.message}", e)
        exitProcess(1)
    }
    logger.info("Config loaded: userId=${config.firebaseUserId}, fullscreen=${config.fullscreen}")

    // -- Auth session manager (checks for persisted session on disk) --------
    val authSessionManager = AuthSessionManager()
    authSessionManager.checkPersistedSession()

    // Resolve initial userId: persisted session takes priority, then config
    val persistedUserId = (authSessionManager.authState.value as? DashboardAuthState.Authenticated)?.userId
    val initialUserId = persistedUserId ?: config.firebaseUserId

    // -- Firebase client (always needed for Firestore access) ---------------
    val firebaseClient = FirebaseAdminClient(config)
    try {
        firebaseClient.initialize()
    } catch (e: Exception) {
        logger.error(
            "Failed to initialize Firebase Admin SDK with service account at '${config.firebaseServiceAccountPath}': ${e.message}",
            e,
        )
        exitProcess(2)
    }
    logger.info("Firebase Admin SDK initialized")

    // -- State holder (created eagerly if we already have a userId) ---------
    var stateHolder: DashboardStateHolder? = null
    if (initialUserId != null) {
        stateHolder = DashboardStateHolder(firebaseClient, initialUserId)
        stateHolder.start()
        logger.info("State holder started for user {}", initialUserId)
    } else {
        logger.info("No userId available yet; waiting for QR authentication")
    }

    // -- Status server (starts immediately; stateHolder may be null) --------
    val statusServer = StatusServer(
        port = config.serverPort,
        host = config.serverHost,
        stateHolder = stateHolder,
        authSessionManager = authSessionManager,
    )
    try {
        statusServer.start()
    } catch (e: Exception) {
        logger.error("Failed to start status server on port ${config.serverPort}: ${e.message}", e)
        exitProcess(3)
    }
    logger.info("Status server started on {}:{}", config.serverHost, config.serverPort)

    val windowState = if (config.fullscreen) {
        WindowState(placement = WindowPlacement.Fullscreen)
    } else {
        WindowState(size = DpSize(config.width.dp, config.height.dp))
    }

    application {
        val activeHolder = remember { mutableStateOf(stateHolder) }

        Window(
            onCloseRequest = {
                runCatching { activeHolder.value?.close() }
                    .onFailure { logger.warn("Error closing state holder", it) }
                runCatching { statusServer.stop() }
                    .onFailure { logger.warn("Error stopping status server", it) }
                exitApplication()
            },
            state = windowState,
            title = "Kairos Dashboard",
            undecorated = config.fullscreen,
        ) {
            DashboardTheme {
                val authState by authSessionManager.authState.collectAsState()

                when (val auth = authState) {
                    is DashboardAuthState.Checking -> {
                        // Defensive branch -- checkPersistedSession() resolves before the window opens
                    }

                    is DashboardAuthState.Unauthenticated -> {
                        if (initialUserId != null) {
                            // We have a config userId -- go straight to dashboard
                            val holder = activeHolder.value
                            if (holder != null) {
                                val state by holder.state.collectAsState()
                                val offset = rememberScreenSaverOffset()
                                DashboardScreen(
                                    state = state,
                                    onComplete = holder::completeHabit,
                                    offset = offset,
                                )
                            }
                        } else {
                            LoginScreen(
                                authSessionManager = authSessionManager,
                                serverPort = config.serverPort,
                            )
                        }
                    }

                    is DashboardAuthState.Authenticated -> {
                        // Lazily create the state holder on first authentication
                        LaunchedEffect(auth.userId) {
                            if (activeHolder.value == null) {
                                val newHolder = DashboardStateHolder(firebaseClient, auth.userId)
                                newHolder.start()
                                activeHolder.value = newHolder
                                statusServer.stateHolder = newHolder
                                logger.info("State holder created after QR auth for user {}", auth.userId)
                            }
                        }
                        val holder = activeHolder.value
                        if (holder != null) {
                            val state by holder.state.collectAsState()
                            val offset = rememberScreenSaverOffset()
                            DashboardScreen(
                                state = state,
                                onComplete = holder::completeHabit,
                                offset = offset,
                            )
                        }
                    }
                }
            }
        }
    }
}
