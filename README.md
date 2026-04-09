# Argmax SDK Android Playground

This project is an example Android app for integrating Argmax SDK into an Android app.

`playground-sample-app` is the runnable sample app. `playground-shared` contains most of the shared
UI, state management, and SDK integration glue used by the sample. Most Argmax SDK integration
points live in
`playground-shared/src/main/java/com/argmaxinc/playground/transcription/ArgmaxSDKCoordinator.kt`.

## Quick Start

1. Export `ARGMAX_SECRET_API_TOKEN` before opening the project or running Gradle. `settings.gradle.kts`
   reads this environment variable so Gradle can resolve the Argmax SDK artifacts.

   ```bash
   export ARGMAX_SECRET_API_TOKEN=axst_your_secret_api_token
   ```

2. Replace the placeholder API key in
   `playground-sample-app/src/main/java/com/argmaxinc/playground/sample/di/LicenseModule.kt`
   with your own `ax_...` API key.

3. Build and run the sample app.

   ```bash
   ./gradlew :playground-sample-app:assembleDebug
   ```

## SDK Modes

The sample defaults to the portable SDK because it is the simplest way to build and run locally.

- Portable mode keeps the runtime-delivery blocks in `settings.gradle.kts` commented out and uses
  `implementation(libs.argmaxinc.sdk.portable)` in `playground-sample-app/build.gradle.kts`.
- Lite mode requires uncommenting the runtime-delivery settings/plugin wiring and switching the app
  dependency to `implementation(libs.argmaxinc.sdk)`.

If you are switching modes for the first time, follow the Argmax upgrade guide and setup playlist:

- [Upgrade to Pro SDK guide](https://app.argmaxinc.com/docs/guides/upgrading-to-pro-sdk)
- [Android setup playlist](https://www.youtube.com/playlist?list=PLL3GZ85RK9KdK_fcR3VPwd3noslHeu2y8)

## API Key Guidance

The sample app intentionally ships with the placeholder `REPLACE_WITH_ARGMAX_API_KEY`.

- Long-lived client-side keys can be extracted from a compiled app package.
- If you do not want to keep the key inline in source, load it from your own configuration or
  backend at runtime, but treat any client-delivered key as recoverable.
- If you still decide to embed a client-side key, obfuscate it before checking it into source and
  treat that obfuscation as a weak deterrent, not a security boundary.

The shared module fails fast when the placeholder value is still present so you get a clear setup
error instead of a vague licensing failure.
