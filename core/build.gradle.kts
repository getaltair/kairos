plugins {
    id("kairos.android.library")
}

android {
    namespace = "com.getaltair.kairos.core"
}

dependencies {
    implementation(project(":domain"))

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
