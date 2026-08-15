# Android QA

## Completed in GitHub Actions

### STATIC PASS
- 81/81 project structural checks PASS.
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

### GRADLE / ANDROID COMPILE — PASS
- JDK 17, Android API 36 and Build Tools 36.0.0.
- `:app:testDebugUnitTest`: PASS.
- `:app:lintDebug`: PASS.
- `:app:assembleDebug`: PASS.
- Successful run: [31880055027](https://github.com/wwbluden80-star/navigator-dosuga-android/actions/runs/31880055027).

### APK INSPECTION — PASS
- `aapt dump badging`: `ru.navigatordosuga.app.debug`, version `1.0.0-debug` (1), compile/target platform 36.
- Launchable activity: `ru.navigatordosuga.app.MainActivity`.
- `apksigner verify`: APK Signature Scheme v2 PASS.
- Room exported schema: version 1, 15 entities.
- APK ZIP integrity: PASS.
- SHA-256: `ba429c8a0bfd8fb7966cafe853ea63e1f444414a188372210db5158ee53d786c`.

## Not possible in this environment

### EMULATOR PASS — NOT TESTED
No Android emulator/adb.

### DEVICE PASS — NOT CLAIMED
No physical Pixel/Samsung/Xiaomi was connected and no emulator launch was performed.

## Required release QA
API 26/29/31/33/35/36, airplane/reconnect, WorkManager reboot, Room migrations, MapLibre offline pack, process death, permissions, predictive back, 100 mode cycles, 100 game cycles and real-device performance.
