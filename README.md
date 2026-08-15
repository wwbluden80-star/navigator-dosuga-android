# Навигатор досуга — Android Native Edition

Нативная Android-ветка продукта, построенная поверх web/PWA V16.3 как источника данных и функциональных требований.

## Статус этой сборки

**Android Native Source Alpha.** Это не WebView/TWA/PWA-обёртка. UI написан на Kotlin + Jetpack Compose, карта — MapLibre Native, локальное хранилище — Room/DataStore, фоновые обновления — WorkManager, местоположение — Fused Location Provider, «Башенки» портированы на Kotlin/Compose Canvas.

В текущей среде разработки отсутствуют Android SDK, adb и Gradle runtime, поэтому APK/AAB здесь не собраны и DEVICE PASS не заявляется. Для воспроизводимой внешней сборки добавлен GitHub Actions workflow `.github/workflows/android.yml`.

## Быстрый запуск в Android Studio

1. Откройте эту папку как Android Studio project.
2. Установите Android SDK Platform 36 и Build Tools 36.0.0.
3. В Gradle Settings выберите Gradle 8.13 (wrapper JAR намеренно не подделывался в среде, где его невозможно было получить/сгенерировать).
4. Скопируйте `gradle.properties.example` в локальные Gradle properties или задайте переменные окружения:
   - `NAVIGATOR_WEB_BASE_URL=https://<ваш-netlify-домен>`
   - `NAVIGATOR_MAP_STYLE_URL=<MapLibre style URL>`
5. Sync Project with Gradle Files.
6. Запустите `app` на Android 8+ (minSdk 26).
7. Для release используйте собственный keystore; production credentials в репозитории отсутствуют.

Команды с системным Gradle 8.13:

```bash
gradle :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
gradle :app:bundleRelease
```

## Backend

Папка `../web-backend-patch` содержит versioned Android API patch для V16.3. Полная копия web V16.3 с уже наложенным patch находится в соседнем артефакте `web_android_sync` при упаковке devkit.

## Offline-first

При первом запуске Room заполняется bundled snapshot'ом V16.3. Интерфейс читает данные из Room независимо от сети. WorkManager синхронизирует свежие версии с backend и не очищает последнюю рабочую локальную базу при сетевой ошибке.

## QA

См. `ANDROID_QA.md` и `ANDROID_PARITY_MATRIX.md`.
