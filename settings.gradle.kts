pluginManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()

// // Start of setting up runtime-delivery for playground, comment out if using portable sdk //////
        maven {
            url = uri("https://api.argmaxinc.com/v1/maven/")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer ${System.getenv("ARGMAX_SECRET_API_TOKEN")}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
// // End of setting up runtime-delivery for playground, comment out if using portable sdk  //////
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()

        maven {
            url = uri("https://api.argmaxinc.com/v1/maven/")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer ${System.getenv("ARGMAX_SECRET_API_TOKEN")}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

rootProject.name = "argmax-sdk-kotlin-playground"

// // Start of setting up runtime-delivery for playground, comment out if using portable sdk //////
//
plugins {
    id("com.argmaxinc.runtime-delivery.settings") version "1.2.2"
}

// Override appModulePath if your app's module name is not ":app"
extensions.configure<com.argmaxinc.runtimedelivery.plugin.RuntimeDeliverySettingsExtension>("runtimeDelivery") {
    appModulePath = ":playground-sample-app"
}
//
// // End of setting up runtime-delivery for playground, comment out if using portable sdk  //////

include(":playground-shared")
include(":playground-sample-app")
