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

// Catch dependency leakage that compiling the public API alone cannot detect.
tasks.register("verifyRuntimeDependencyIsolation") {
    val runtimeClasspath = configurations.named("releaseRuntimeClasspath")
    doLast {
        val forbidden = setOf(
            "androidx.compose:compose-bom",
            "androidx.compose.material3:material3",
            "androidx.compose.material3:material3-android",
            "androidx.compose.ui:ui-tooling",
            "androidx.compose.ui:ui-tooling-android",
            "androidx.compose.ui:ui-tooling-preview",
            "androidx.compose.ui:ui-tooling-preview-android",
            "com.google.devtools.ksp:symbol-processing-api",
        )
        val leaked = runtimeClasspath.get().incoming.resolutionResult.allComponents
            .mapNotNull { it.moduleVersion }
            .map { "${it.group}:${it.name}" }
            .filter { it in forbidden }
        check(leaked.isEmpty()) { "Unexpected consumer runtime dependencies: $leaked" }
    }
}