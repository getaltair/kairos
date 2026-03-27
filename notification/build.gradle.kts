plugins {
    id("kairos.android.library")
}

android {
    namespace = "com.getaltair.kairos.notification"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core"))

    // Android Core
    implementation(libs.androidx.core.ktx)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
