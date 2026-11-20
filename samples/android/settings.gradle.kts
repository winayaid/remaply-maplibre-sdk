pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// -PremaplyFromMavenLocal=true builds against the artifacts from `./gradlew publishToMavenLocal` in
// ../../android instead of the SDK sources. CI uses it to check the published POMs and AARs.
val fromMavenLocal = providers.gradleProperty("remaplyFromMavenLocal").orNull == "true"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (fromMavenLocal) {
            mavenLocal {
                content { includeGroup("io.github.winayaid") }
            }
        }
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

// Composite build: `io.github.winayaid:maps-android` and `io.github.winayaid:maps-android-compose` resolve to the
// local SDK sources in ../../android instead of a published artifact. Apps outside this repo use
// the Maven coordinates documented in docs/.
if (!fromMavenLocal) {
    includeBuild("../../android")
}

include(":app")
