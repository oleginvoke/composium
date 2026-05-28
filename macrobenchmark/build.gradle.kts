import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val enableMacrobenchmark = providers
    .gradleProperty("enableMacrobenchmark")
    .map(String::toBoolean)
    .getOrElse(false)

plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.example.app.macrobenchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":sample"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

if (!enableMacrobenchmark) {
    tasks.configureEach {
        enabled = false
    }
}

dependencies {
    if (enableMacrobenchmark) {
        implementation(libs.androidx.benchmark.macro.junit4)
        implementation(libs.androidx.test.ext.junit)
        implementation(libs.androidx.test.uiautomator)
    }
}
