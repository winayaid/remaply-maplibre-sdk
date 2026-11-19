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
    versionCatalogs {
        // Share the SDK's version catalog so the sample always builds with the same pinned versions.
        create("libs") {
            from(files("../../android/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "remaply-maps-sample"

// Composite build: `com.remaply:maps-android` and `com.remaply:maps-android-compose` resolve to the
// local SDK sources in ../../android instead of a published artifact. Apps outside this repo use
// the Maven coordinates documented in docs/.
includeBuild("../../android")

include(":app")
