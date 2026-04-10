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

## `argmax-sdk-kotlin-portable` and `argmax-sdk-kotlin`

The default project configuration uses the `argmax-sdk-kotlin-portable` package which is recommended for a quick start and ease of local development. `argmax-sdk-kotlin` integration is more involved because it requires Google Play integration but leads to minimal app size impact (<5 MB vs >50 MB).
