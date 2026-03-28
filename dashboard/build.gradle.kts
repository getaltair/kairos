import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Shared domain models (pure JVM module)
    implementation(project(":domain"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Firebase Admin SDK (JVM)
    implementation(libs.firebase.admin)

    // Ktor embedded server (for Home Assistant status API)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.kotlinx.serialization.json)

    // QR Code generation (ZXing)
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.slf4j.simple)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.jvm)
}

compose.desktop {
    application {
        mainClass = "com.getaltair.kairos.dashboard.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "kairos-dashboard"
            packageVersion = "1.0.0"

            linux {
                debPackageVersion = "1.0.0"
            }
        }

        jvmArgs += listOf(
            "-Xmx256m",
        )
    }
}
