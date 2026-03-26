import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            // Compose is enabled via the kotlin-compose plugin.
            // Build features and compiler options are handled by the plugin.
            // No manual buildFeatures { compose = true } needed with kotlin-compose plugin.
        }
    }
}
