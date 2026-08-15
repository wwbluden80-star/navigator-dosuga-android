# Android QA

## Completed in current build environment

### STATIC PASS
- 77/77 project structural checks PASS.
- bundled data counts/IDs/coordinates validated.
- no fake 0,0 coordinates.
- XML resources parse.
- manifest parses.
- no `android.webkit.WebView`, TrustedWebActivity, Capacitor or URL-loading shell patterns.
- Compose/Room/WorkManager/MapLibre/FusedLocation/targetSdk36 architectural gates present.
- mobile backend `.mjs` files pass `node --check`.

### JVM GAME PASS
`TOWER_JVM_PASS releaseV=-13.67670841185739 score=1467 floors=6 maxDrift=0.0`

### Data baseline
61 / 14 / 102 / 38 / 23 / 22 (events seed).

## Not possible in this environment

### GRADLE / ANDROID COMPILE — NOT TESTED
No Android SDK/Gradle runtime.

### EMULATOR PASS — NOT TESTED
No Android emulator/adb.

### DEVICE PASS — NOT TESTED
No physical Pixel/Samsung/Xiaomi connected.

### APK/AAB PASS — NOT TESTED
No artifact is claimed without a successful Android build.

## Required release QA
API 26/29/31/33/35/36, airplane/reconnect, WorkManager reboot, Room migrations, MapLibre offline pack, process death, permissions, predictive back, 100 mode cycles, 100 game cycles and real-device performance.
