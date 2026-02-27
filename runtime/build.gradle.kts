import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "oleginvoke.com.composium"

    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_11)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }
}

val isLocalPublishOnly = gradle.startParameter.taskNames
    .let { taskNames ->
        taskNames.isNotEmpty() && taskNames.all { taskName ->
            taskName.contains("mavenlocal", ignoreCase = true)
        }
    }

mavenPublishing {
    coordinates(
        groupId = providers.gradleProperty("GROUP").get(),
        artifactId = "composium",
        version = providers.gradleProperty("VERSION_NAME").get(),
    )

    publishToMavenCentral("CENTRAL_PORTAL")
    if (!isLocalPublishOnly) {
        signAllPublications()
    }

    pom {
        name.set("Composium")
        description.set("Composable runtime UI to host and register scenes.")
        inceptionYear.set("2026")
        url.set("https://github.com/oleginvoke/composium")

        issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/oleginvoke/composium/issues")
        }

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                distribution.set("repo")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("oleginvoke")
                name.set("oleginvoke")
                url.set("https://github.com/oleginvoke")
            }
        }

        scm {
            url.set("https://github.com/oleginvoke/composium")
            connection.set("scm:git:https://github.com/oleginvoke/composium.git")
            developerConnection.set("scm:git:ssh://git@github.com/oleginvoke/composium.git")
        }
    }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.material.icons)
    implementation(libs.kotlin.reflect)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Previews live in the debug source-set.
    debugImplementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
}
