plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.lint.api)
    compileOnly(libs.android.lint.checks)

    testImplementation(libs.android.lint.tests)
    testImplementation(libs.android.lint.api)
    testImplementation(libs.android.lint.checks)
    testImplementation(kotlin("test-junit"))
}

tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to "oleginvoke.com.composium.lint.ComposiumIssueRegistry",
        )
    }
}
