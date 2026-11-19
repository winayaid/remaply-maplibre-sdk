plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}

val sdkVersion = providers.gradleProperty("VERSION_NAME").get()

subprojects {
    group = "com.remaply"
    version = sdkVersion
}
