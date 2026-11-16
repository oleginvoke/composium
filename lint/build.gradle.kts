plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    compileOnly(libs.android.lint.api)
    compileOnly(libs.android.lint.checks)

    testImplementation(libs.android.lint.tests)
    testImplementation(libs.android.lint.api)
    testImplementation(libs.android.lint.checks)
    // The older lint frontend must also be able to read the fixtures' stdlib metadata.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${libs.versions.runtime.kotlin.get()}")
}

tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to "oleginvoke.com.composium.lint.ComposiumIssueRegistry",
        )
    }
}
