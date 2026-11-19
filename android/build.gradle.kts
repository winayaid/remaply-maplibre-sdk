plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    // Aggregates the API reference of both modules: ./gradlew :dokkaGenerate → build/dokka/html
    alias(libs.plugins.dokka)
}

val sdkVersion = providers.gradleProperty("VERSION_NAME").get()

subprojects {
    group = "com.remaply"
    version = sdkVersion
}

dokka {
    moduleName.set("Remaply Maps SDK for Android")
    dokkaPublications.html {
        failOnWarning.set(true)
    }
}

dependencies {
    dokka(project(":maps-android"))
    dokka(project(":maps-android-compose"))
}
