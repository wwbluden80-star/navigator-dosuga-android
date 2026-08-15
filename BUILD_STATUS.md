# Build Status

**Product:** Навигатор досуга — Android Native Edition 1.0.0
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
- GitHub Actions run: [31880055027](https://github.com/wwbluden80-star/navigator-dosuga-android/actions/runs/31880055027)
- Built commit: `d41b2320f0b07c031d1e25f2a6f2fb52ab429555`
- Package: `ru.navigatordosuga.app.debug`
- Version: `1.0.0-debug` (`versionCode` 1)
- Size: 72,106,850 bytes
- SHA-256: `ba429c8a0bfd8fb7966cafe853ea63e1f444414a188372210db5158ee53d786c`
- Signature: Android debug certificate, APK Signature Scheme v2 verified.
- Artifact: `Navigator-Dosuga-Android-APK` (artifact ID `9245807745`).

## QA scope
- Android Gradle compile: PASS (`:app:assembleDebug`)
- JVM unit tests: PASS
- Android Lint: PASS
- Static source/data QA: 81/81 PASS
- `aapt` manifest/package/launchable activity inspection: PASS
- Room schema v1 / 15 entities: PASS
- Emulator: NOT TESTED (no emulator was available)
- Physical device: NOT TESTED
- DEVICE PASS: NOT CLAIMED
