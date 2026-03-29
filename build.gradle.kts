// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt)
    id("com.diffplug.spotless") version "7.0.0.BETA4"
}

detekt {
    config.setFrom("$projectDir/config/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    source.setFrom(
        "app/src",
        "wear/src",
        "domain/src",
        "data/src",
        "core/src",
        "ui/src",
        "dashboard/src",
        "feature",
    )
}

dependencies {
    detektPlugins(libs.detekt.compose)
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        // Configure kotlin for modules with Kotlin plugin
        kotlin {
            target("**/*.kt", "**/*.kts")
            ktlint("1.8.0")
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("1.8.0")
        }
        format("misc") {
            target(".gitignore", ".gitattributes")
            trimTrailingWhitespace()
            endWithNewline()
            indentWithTabs()
        }
    }
}
