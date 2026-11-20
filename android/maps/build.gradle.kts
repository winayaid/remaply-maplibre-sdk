import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.remaply.maps"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SDK_VERSION", "\"${project.version}\"")
        buildConfigField("String", "MAPLIBRE_VERSION", "\"${libs.versions.maplibre.get()}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = false
        // Dependency version checks are handled by deliberate pinning (see libs.versions.toml), not by lint.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        // Timber arrives transitively from MapLibre. This project does not use it: SDK logging goes through
        // MapLogger (API_SPEC.md §11), so the "use Timber instead of Log" suggestion does not apply.
        disable += "LogNotTimber"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    // Every public declaration must state its visibility and type explicitly (SDK hygiene).
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Emit Kotlin 2.2 metadata so apps on Kotlin 2.2+ can consume the SDK
        // (MapLibre 13.7.1 itself already requires 2.2+).
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dokka {
    moduleName.set("maps-android")
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
        reportUndocumented.set(true)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/winayaid/remaply-maplibre-sdk/tree/main/android/maps/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }
    }
    dokkaPublications.html {
        failOnWarning.set(true)
    }
}

dependencies {
    // `api` scope: required by the experimental `withNativeMap` escape hatch (API_SPEC.md §13.2).
    api(libs.maplibre.android)

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    testImplementation(libs.junit4)

    // Instrumented tests (Phase 5). Test-only dependencies; nothing here ships with the SDK.
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.activity)
    androidTestImplementation(libs.okhttp)
}
