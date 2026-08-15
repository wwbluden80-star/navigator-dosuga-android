# Build Status

**Product:** Навигатор досуга — Android Native Edition 1.0.1
**Content source:** Web/PWA V16.3 Events Intelligence Alpha
**Status:** Debug APK built and verified

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
**APK: PASS**

- File: `app/build/outputs/apk/debug/app-debug.apk`
- GitHub Actions run: [31881851089](https://github.com/wwbluden80-star/navigator-dosuga-android/actions/runs/31881851089)
- Built commit: `c46db8c93570e7f3917a73ae0b645bda9aa7ad08`
- Package: `ru.navigatordosuga.app.debug`
- Version: `1.0.1-debug` (`versionCode` 2)
- Size: 64,242,026 bytes
- SHA-256: `bbd76ec44eb8f05ba93366c23ffc7d3a2a40b3fb854c5e6d63cd1991ad740fb6`
- Signature: Android debug certificate, APK Signature Scheme v2 verified.
- Artifact: `Navigator-Dosuga-Android-APK` (artifact ID `9246278867`).

## QA scope
- Android Gradle compile: PASS (`:app:assembleDebug`)
- JVM unit tests: PASS
- Android Lint: PASS
- Static source/data QA: 81/81 PASS
- `aapt` manifest/package/launchable activity inspection: PASS
- Room schema v1 / 15 entities: PASS
- Android 16/API 36 emulator cold launch: PASS (`ANDROID_16_SMOKE_PASS`)
- Physical device: NOT TESTED
- DEVICE PASS: NOT CLAIMED
