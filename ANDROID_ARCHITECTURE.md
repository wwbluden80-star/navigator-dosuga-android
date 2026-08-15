# Android Architecture

## Принцип

Android является самостоятельным native-клиентом. Web и Android используют один backend/data platform, но Android не загружает web UI и не парсит HTML приложения.

```text
Compose UI -> ViewModel -> Repository -> Room
                         -> sync layer -> HTTPS API
WorkManager -> API -> validation -> Room transaction -> Flow -> UI
```

## Native stack

- Kotlin
- Jetpack Compose
- Room
- DataStore
- WorkManager
- MapLibre Native Android
- Fused Location Provider
- Kotlin Coroutines / Flow
- Kotlin Serialization
- native Compose Canvas physics game

## Модули текущего source tree

- `data/db` — Room entities/DAO/database
- `data/network` — HTTPS client + normalizers
- `data/repository` — content/events/profile/saved/trip/search repositories
- `data/seed` — V16.3 bundled baseline importer
- `data/sync` — background/manual sync
- `map` / `offline` — MapLibre map, markers, offline packs
- `location` — fused location, car marker, foreground track service
- `game/tower` — physics/scoring/simulation/persistence/sync
- `ui` — Compose MAP-FIRST shell, profiles, offline maps, Game Hub

## Map-first

Один MapView остаётся основным экраном. Режим меняет data source/layers, а не создаёт web page или новый браузерный map instance.
