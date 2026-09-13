plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "composium.test.consumer"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
}

dependencies {
    // No direct Compose dependencies or BOM: the public API must provide them.
    implementation(project(":runtime"))
}
