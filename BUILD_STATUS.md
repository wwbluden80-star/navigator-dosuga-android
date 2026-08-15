# Build Status

**Product:** Навигатор досуга — Android Native Edition 1.0.0
**Content source:** Web/PWA V16.3 Events Intelligence Alpha
**Status:** Android Studio source-ready build

## Completed in this environment
- Native Kotlin + Jetpack Compose source tree created.
- No WebView/TWA/Capacitor shell in the application source.
- MapLibre Native map layer integrated.
- Room local database + bundled offline seed integrated.
- WorkManager sync workers integrated.
- Fused Location / car marker / track service integrated.
- Profiles, Saved, Trips, guides, six content modes integrated.
- Events local cache + backend sync client integrated.
- Game Hub and native Kotlin/Canvas Tower game port integrated.
- Web V16.3 backend patch for versioned mobile API included.
- GitHub Actions Android build workflow included.
- Static source/data QA: 81/81 PASS.
- Tower physics JVM smoke: PASS (6 floors / score 1467).

## Binary build status
**APK/AAB: NOT PRODUCED IN THIS HOST.**

Reason: the execution host does not contain Android SDK/build-tools/adb/Gradle, outbound binary downloads for the Android command-line SDK are blocked by the file gateway, and the connected GitHub installation exposes no repository to run the included Actions workflow.

Do not represent this source artifact as a compiled APK. Open it with current Android Studio or run the included GitHub Actions workflow in a repository with Android Actions enabled to produce `app-debug.apk` and `app-release.aab`.

## QA not claimed
- Android Gradle compile: NOT RUN on this host
- Emulator: NOT TESTED
- Physical device: NOT TESTED
- DEVICE PASS: NOT CLAIMED
