import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.remaply.maps.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.remaply.maps.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        // Timber arrives transitively from MapLibre. This project does not use it: SDK logging goes through
        // MapLogger (API_SPEC.md §11), so the "use Timber instead of Log" suggestion does not apply.
        disable += "LogNotTimber"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// The SDK version from ../../android/gradle.properties, so the sample also resolves published artifacts.
val sdkVersion: String =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("../../android/gradle.properties"))
        .asText
        .get()
        .lineSequence()
        .first { it.startsWith("VERSION_NAME=") }
        .substringAfter("=")

dependencies {
    implementation("io.github.winayaid:maps-android:$sdkVersion")
    implementation("io.github.winayaid:maps-android-compose:$sdkVersion")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)

    // Sample-screen UI tests (Phase 6). Test-only.
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    debugImplementation(libs.compose.ui.test.manifest)
}
