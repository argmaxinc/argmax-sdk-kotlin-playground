plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

val spotlessExcludes =
    listOf(
        "**/build/**",
        "**/.gradle/**",
        "**/.gradle-home/**",
        "**/.idea/**",
        "**/.venv/**",
        "**/__pycache__/**",
        "**/external/**",
        "**/third_party/**",
        "**/.cxx/**",
        "**/.source/**",
        "bazel-*",
        "**/bazel-*",
    )

spotless {
    kotlin {
        target(
            project.fileTree(".") {
                include("**/*.kt")
                exclude(spotlessExcludes)
            },
        )
        ktlint()
    }

    kotlinGradle {
        target(
            project.fileTree(".") {
                include("**/*.gradle.kts")
                exclude(spotlessExcludes)
            },
        )
        ktlint()
    }
}
