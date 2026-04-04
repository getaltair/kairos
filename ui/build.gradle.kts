plugins {
    id("kairos.android.library")
    id("kairos.android.compose")
}

android {
    namespace = "com.getaltair.kairos.ui"
}

dependencies {
    implementation(project(":domain"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
