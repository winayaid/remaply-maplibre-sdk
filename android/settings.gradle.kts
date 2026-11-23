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

rootProject.name = "remaply-maps-trial"

// Project names match the published artifact IDs (io.github.winayaid:<name>), so the sample app
// can consume them through a Gradle composite build by their Maven coordinates.
include(":remaply-maps-trial-android")
project(":remaply-maps-trial-android").projectDir = file("maps")

include(":remaply-maps-trial-compose")
project(":remaply-maps-trial-compose").projectDir = file("maps-compose")
