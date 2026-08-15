# Android Release

## Target

- minSdk 26
- targetSdk 36
- compileSdk 36
- Java/Kotlin target 17
- versionName 1.0.0

## Artifacts expected in Android-capable environment

- `app-debug.apk`
- release `.aab` (unsigned unless release keystore is provided)

## Why artifacts are not included in this source-alpha

Build host used for this work has Java/Kotlin/Node but no Android SDK, sdkmanager, adb or Gradle installation. A wrapper JAR was not fabricated. Therefore an APK/AAB cannot be truthfully claimed from this environment.

## CI

`.github/workflows/android.yml` installs JDK 17, Android 36 SDK and Gradle 8.13, then runs lint, unit tests, debug APK and release AAB build.

Production signing must use your own protected keystore/Play App Signing; no credentials are included.
