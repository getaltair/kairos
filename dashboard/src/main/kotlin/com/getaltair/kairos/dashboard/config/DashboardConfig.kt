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
    /** True when `FIRESTORE_EMULATOR_HOST` is set, skipping real credential loading. */
    val useEmulator: Boolean
        get() = !System.getenv("FIRESTORE_EMULATOR_HOST").isNullOrBlank()

    init {
        if (!useEmulator) {
            require(firebaseServiceAccountPath.isNotBlank()) {
                "firebaseServiceAccountPath must not be blank (set FIRESTORE_EMULATOR_HOST to skip)"
            }
        }
        require(firebaseUserId.isNotBlank()) { "firebaseUserId must not be blank" }
        require(serverPort in 1..65535) { "serverPort must be in 1..65535, was $serverPort" }
        require(width > 0) { "width must be positive, was $width" }
        require(height > 0) { "height must be positive, was $height" }
    }

    companion object {
        fun load(): DashboardConfig {
            val props = Properties()
            val stream = DashboardConfig::class.java.classLoader
                .getResourceAsStream("dashboard.properties")
                ?: error("dashboard.properties not found on classpath")
            stream.use { props.load(it) }
            return DashboardConfig(
                firebaseServiceAccountPath = props.getProperty("firebase.service_account_path", ""),
                firebaseUserId = props.getProperty("firebase.user_id")
                    ?: error("Required property 'firebase.user_id' missing from dashboard.properties"),
                fullscreen = props.getProperty("dashboard.fullscreen", "true").toBoolean(),
                width = props.getProperty("dashboard.width", "1920").let { raw ->
                    raw.trim().toIntOrNull()
                        ?: error("Invalid integer for 'dashboard.width': '$raw'")
                },
                height = props.getProperty("dashboard.height", "1080").let { raw ->
                    raw.trim().toIntOrNull()
                        ?: error("Invalid integer for 'dashboard.height': '$raw'")
                },
                serverPort = props.getProperty("server.port", "8888").let { raw ->
                    raw.trim().toIntOrNull()
                        ?: error("Invalid integer for 'server.port': '$raw'")
                },
            )
        }
    }
}
