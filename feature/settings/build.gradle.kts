plugins {
    id("kairos.android.library")
    id("kairos.android.compose")
}

android {
    namespace = "com.getaltair.kairos.feature.settings"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":ui"))
    implementation(project(":core"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Logging
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
