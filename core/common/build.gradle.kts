plugins {
    id("kairos.jvm.library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":domain"))
    implementation(platform("libs.kotlinx.coroutines.core"))
    implementation(libs.junit)
}
