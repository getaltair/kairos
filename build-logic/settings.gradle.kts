dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    // This is what lets convention plugins use version catalog
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
