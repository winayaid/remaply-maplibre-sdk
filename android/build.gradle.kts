plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    // Aggregates the API reference of both modules: ./gradlew :dokkaGenerate → build/dokka/html
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish) apply false
}

val sdkVersion = providers.gradleProperty("VERSION_NAME").get()

// Publishing (docs/releasing.md). Credentials and keys come only from Gradle properties or
// ORG_GRADLE_PROJECT_* environment variables, never from files in the repository.
// Blank counts as unset: GitHub Actions passes an unset secret or variable as an empty string.
fun optionalProperty(name: String) = providers.gradleProperty(name).filter { it.isNotBlank() }

val signingKey = optionalProperty("signingInMemoryKey")
val privateRepoUrl = optionalProperty("remaplyPrivateRepoUrl")
val privateRepoUser = optionalProperty("remaplyPrivateUsername")

subprojects {
    group = "io.github.winayaid"
    version = sdkVersion

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            // Uploads a deployment that must be released by hand in the Central Portal.
            publishToMavenCentral(automaticRelease = false)
            // Unsigned builds are fine for mavenLocal and private repos; Maven Central rejects them.
            if (signingKey.isPresent) signAllPublications()

            pom {
                url.set("https://github.com/winayaid/remaply-maplibre-sdk")
                inceptionYear.set("2026")
                licenses {
                    license {
                        name.set("BSD-2-Clause")
                        url.set("https://opensource.org/license/bsd-2-clause")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("remaply")
                        name.set("Remaply")
                        url.set("https://github.com/winayaid")
                    }
                }
                scm {
                    url.set("https://github.com/winayaid/remaply-maplibre-sdk")
                    connection.set("scm:git:https://github.com/winayaid/remaply-maplibre-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/winayaid/remaply-maplibre-sdk.git")
                }
            }
        }

        if (privateRepoUrl.isPresent) {
            extensions.configure<PublishingExtension> {
                repositories {
                    // ./gradlew publishAllPublicationsToRemaplyPrivateRepository
                    maven {
                        name = "remaplyPrivate"
                        url = uri(privateRepoUrl.get())
                        // Reads remaplyPrivateUsername / remaplyPrivatePassword.
                        if (privateRepoUser.isPresent) credentials(PasswordCredentials::class)
                    }
                }
            }
        }
    }
}

dokka {
    moduleName.set("Remaply Maps SDK for Android")
    dokkaPublications.html {
        failOnWarning.set(true)
    }
}

dependencies {
    dokka(project(":remaply-maps-trial-android"))
    dokka(project(":remaply-maps-trial-compose"))
}
