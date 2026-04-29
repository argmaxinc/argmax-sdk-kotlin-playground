# Argmax SDK Kotlin Playground

This project hosts the source code for [Argmax Playground for Android](https://play.google.com/apps/testing/com.argmaxinc.playground).

It is open-sourced to demonstrate best practices when building with [Argmax Pro SDK Kotlin](https://app.argmaxinc.com/docs) through an end-to-end example app. Specifically, this app demonstrates Real-time Transcription and File Transcription.

---

## Getting Started

### 1. Get Argmax credentials

This project requires a secret token and an API key from that you may generate from your [Argmax Dashboard](https://app.argmaxinc.com).

### 2. Set Argmax credentials

Once obtained, please set the following environment variable with your secret token that starts with `axst_`:

```bash
export ARGMAX_SECRET_API_TOKEN=axst_****
```

Furthermore, you will need to replace `REPLACE_WITH_ARGMAX_API_KEY` in `playground-sample-app/src/main/java/com/argmaxinc/playground/sample/di/LicenseModule.kt` with your real API key that starts with `ax_`

> **Do not commit your API key.**.


### 3. Follow Installation instructions

Please see [Installation](https://app.argmaxinc.com/docs/guides/upgrading-to-pro-sdk) for details or see the following tutorial videos that are based on this repository:

- [Part 1: Integrating Argmax Pro SDK in your Android app](https://www.youtube.com/watch?v=rt7V79XXbVw&list=PLL3GZ85RK9KdK_fcR3VPwd3noslHeu2y8)
- [Part 2: Shipping your Android app with Argmax Pro SDK and Google Play integration](https://www.youtube.com/watch?v=iWl63MaP9Rw&list=PLL3GZ85RK9KdK_fcR3VPwd3noslHeu2y8&index=2)

---

## `argmax-sdk-kotlin` and `argmax-sdk-kotlin-portable`

For an overview of how the **default** (`argmax-sdk-kotlin`) and **portable** (`argmax-sdk-kotlin-portable`) SDKs differ, see [this video playlist](https://www.youtube.com/watch?v=X9oxMXnOjkc&list=PLL3GZ85RK9KdK_fcR3VPwd3noslHeu2y8).

The default project configuration uses the `argmax-sdk-kotlin` package to demonstrate the production-shaped Google Play runtime-delivery integration. `argmax-sdk-kotlin-portable` is simpler for quick local development because it bundles runtime libraries directly, but it has a larger app size impact (>50 MB vs <5 MB).

Because the default SDK setup generates runtime-delivery modules and model assets, Gradle initialization can take a while the first time. Install the sample app through the runtime-delivery tasks:

```bash
./gradlew installRuntimeDeliveryDebugForDevice
./gradlew installRuntimeDeliveryReleaseForDevice
```

Use the portable SDK if you want the regular Android Studio or Gradle install flow.

When updating the SDK version, keep the runtime-delivery settings plugin version in `settings.gradle.kts` aligned with the SDK version in `gradle/libs.versions.toml`; the sample app dependency in `playground-sample-app/build.gradle.kts` reads that version catalog entry.
