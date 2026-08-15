# Offline First

1. Database seed импортируется локально.
2. Compose читает только Room/repository Flow.
3. Network sync обновляет Room в фоне.
4. Без сети map POI, search, Saved, profiles, trip и Bashenki продолжают работать.
5. MapLibre OfflineManager позволяет отдельно скачать карту региона.

## Offline map packs

Native source содержит download/list/delete flow для Offline Regions. В UI предусмотрены стартовые регионы Москва и Московская область. Production deployment должен использовать лицензированный tile/style provider, который разрешает offline caching.

## Events

Events доступны из последней локальной синхронизации. При устаревании интерфейс должен показывать freshness, а не обещать live-статус без сети.
