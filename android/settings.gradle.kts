pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "remaply-maps-android"

// Project names match the published artifact IDs (com.remaply:<name>), so the sample app
// can consume them through a Gradle composite build by their Maven coordinates.
include(":maps-android")
project(":maps-android").projectDir = file("maps")

include(":maps-android-compose")
project(":maps-android-compose").projectDir = file("maps-compose")
