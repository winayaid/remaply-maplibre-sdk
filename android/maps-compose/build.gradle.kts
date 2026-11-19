import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.remaply.maps.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        // Timber arrives transitively from MapLibre. This project does not use it: SDK logging goes through
        // MapLogger (API_SPEC.md §11), so the "use Timber instead of Log" suggestion does not apply.
        disable += "LogNotTimber"
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dokka {
    moduleName.set("maps-android-compose")
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
        reportUndocumented.set(true)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/winayaid/remaply-maplibre-sdk/tree/main/android/maps-compose/src/main/kotlin")
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
    api(project(":maps-android"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit4)
}
