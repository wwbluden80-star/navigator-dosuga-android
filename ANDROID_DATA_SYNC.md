# Android Data Sync

## Source of truth

Backend/web data platform остаётся центральным источником online-данных. Android UI работает из Room.

## Baseline

Bundled `app/src/main/assets/seed/` содержит V16.3 snapshot:
- mushrooms: 61
- fishing: 14
- beautiful: 102
- cinema: 38
- history: 23
- events: 22

Beautiful содержит 30 research-records без подтверждённых координат. Они сохраняются как `lat/lon = null`, а не подменяются `0,0` или центром города.

## Remote endpoints

Android сначала использует `/data/mobile/v1/{dataset}.json`. Events идут через `/api/v1/events` с fallback на старый `/api/events` для совместимости V16.3.

Дополнительные API:
- `/api/v1/config`
- `/api/v1/content-version`
- `/api/v1/events`

## Safety

- сетевой сбой не очищает Room;
- events заменяются только после успешного полного получения диапазона;
- seed всегда остаётся first-run baseline;
- sync state записывает успех/ошибку/etag/version;
- pending tower score хранится локально до сети.

## Scheduling

- stable content: 12h periodic worker + manual refresh;
- events: 4h client sync (server сам выполняет более частую source revalidation);
- pending actions: background retry.
