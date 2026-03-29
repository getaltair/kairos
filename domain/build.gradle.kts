plugins {
    id("kairos.jvm.library")
    `java-test-fixtures`
}

dependencies {
    // Coroutines (use cases are suspend functions)
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
}
