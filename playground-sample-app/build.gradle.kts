import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // // Start of setting up runtime-delivery for playground, comment out if using portable sdk //////
    // alias(libs.plugins.argmaxinc.runtime.delivery)
    // // End of setting up runtime-delivery for playground, comment out if using portable sdk //////
}

val argmaxSdkVersion: String = libs.versions.argmaxSdk.get()

android {
    namespace = "com.argmaxinc.playground.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.argmaxinc.playground.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "SDK_VERSION", "\"$argmaxSdkVersion\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":playground-shared"))
    // Use this to depend on lite sdk from Maven when runtime-delivery is enabled.
    // implementation(libs.argmaxinc.sdk)
    // Use this to depend on portable sdk from Maven when runtime-delivery is disabled.
    implementation(libs.argmaxinc.sdk.portable)

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
