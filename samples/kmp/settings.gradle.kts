pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// -PremaplyFromMavenLocal=true resolves the SDK from `./gradlew publishToMavenLocal` in ../../android,
// to try SDK changes before they are released. By default the SDK comes from Maven Central, like in an app.
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
}

rootProject.name = "remaply-kmp-sample"

include(":shared")
include(":androidApp")
