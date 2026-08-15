# Android Changelog

## 1.0.1 Android 16 crash fix
- switched MapLibre 13.5.0 from its Vulkan backend to the supported OpenGL artifact to avoid the Android 16 native renderer crash;
- made `MapView` destruction idempotent across lifecycle and Compose disposal;
- accepted both numeric scores and evidence grades in bundled/offline dataset parsing;
- prevented a failed seed import from terminating the application process;
- added a regression unit test for mixed score types;
- added an Android 16/API 36 emulator cold-launch smoke test and crash-log artifact to CI.

## 1.0.0 Native Source Alpha
- created native Kotlin/Compose application shell;
- Room offline-first database + V16.3 baseline;
- WorkManager background synchronization;
- MapLibre native map/clustering/semantic markers;
- offline map region manager;
- six activity modes;
- local-first profiles;
- native Saved/Trip storage;
- local global search across six datasets;
- Fused Location and foreground track core;
- car marker repository;
- native Game Hub;
- native Kotlin/Canvas Bashenki port;
- same tower leaderboard server endpoint/pending offline queue;
- versioned mobile web API patch;
- target/compile SDK 36;
- Android CI workflow.
