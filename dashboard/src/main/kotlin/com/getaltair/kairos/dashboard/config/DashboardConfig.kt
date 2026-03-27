package com.getaltair.kairos.dashboard.config

import java.util.Properties

/**
 * Configuration loaded from `dashboard.properties` on the classpath.
 *
 * Property keys match the file exactly:
 * - `firebase.service_account_path` -- path to the GCP service-account JSON
 * - `firebase.user_id` -- the Firestore user document ID to observe
 * - `dashboard.fullscreen` -- whether the window opens fullscreen
 * - `dashboard.width` / `dashboard.height` -- windowed dimensions
 * - `server.port` -- Ktor HTTP server port for Home Assistant integration
 */
data class DashboardConfig(
    val firebaseServiceAccountPath: String,
    val firebaseUserId: String,
    val fullscreen: Boolean,
    val width: Int,
    val height: Int,
    val serverPort: Int,
) {
    companion object {
        fun load(): DashboardConfig {
            val props = Properties()
            val stream = DashboardConfig::class.java.classLoader
                .getResourceAsStream("dashboard.properties")
                ?: error("dashboard.properties not found on classpath")
            props.load(stream)
            return DashboardConfig(
                firebaseServiceAccountPath = props.getProperty("firebase.service_account_path"),
                firebaseUserId = props.getProperty("firebase.user_id"),
                fullscreen = props.getProperty("dashboard.fullscreen", "true").toBoolean(),
                width = props.getProperty("dashboard.width", "1920").toInt(),
                height = props.getProperty("dashboard.height", "1080").toInt(),
                serverPort = props.getProperty("server.port", "8888").toInt(),
            )
        }
    }
}
