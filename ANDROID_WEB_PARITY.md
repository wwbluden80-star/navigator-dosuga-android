# Android / Web parity

Статусы:
- **SOURCE IMPLEMENTED** — native source code реализован, но Android SDK runtime QA ещё нужен.
- **PARTIAL** — базовый сценарий реализован, rich web UX ещё не полностью перенесён.
- **NOT PORTED** — сознательно не выдаётся за готовое.

| Возможность | Web V16.3 | Android source | Offline | Комментарий |
|---|---:|---:|---:|---|
| Native MAP-FIRST shell | ✓ | SOURCE IMPLEMENTED | ✓ | Compose + MapLibre |
| Грибы | ✓ | SOURCE IMPLEMENTED | ✓ | 61 seed records + semantic markers |
| Рыбалка | ✓ | SOURCE IMPLEMENTED | ✓ | 14 records |
| Красивые места | ✓ | SOURCE IMPLEMENTED | ✓ | 102 records; 30 research records без выдуманных координат |
| Кино | ✓ | SOURCE IMPLEMENTED | ✓ | 38 locations |
| История | ✓ | SOURCE IMPLEMENTED | ✓ | 23 production places |
| Мероприятия | ✓ | SOURCE IMPLEMENTED | cached | 22 seed + API sync/date/free-paid filters |
| Поиск по 6 режимам | ✓ | SOURCE IMPLEMENTED | ✓ | Room queries, события тоже локально |
| Saved | ✓ | SOURCE IMPLEMENTED | ✓ | profile-isolated Room |
| Поездка | ✓ | SOURCE IMPLEMENTED | ✓ | локальный список + external geo route intent |
| Профили | ✓ | SOURCE IMPLEMENTED | ✓ | local-first onboarding/preferences |
| Avatar import/crop | ✓ | PARTIAL | ✓ | schema готова, picker/crop UI ещё не перенесён |
| GPS | ✓ | SOURCE IMPLEMENTED | ✓ | Fused Location, permission in context |
| Car marker | ✓ | SOURCE IMPLEMENTED CORE | ✓ | repository реализован; полный UX ещё требует wiring |
| Track recording | ✓ | SOURCE IMPLEMENTED CORE | ✓ | foreground location service; полный UX ещё требует wiring |
| Offline map download | —/PWA cache | SOURCE IMPLEMENTED | ✓ | MapLibre OfflineManager |
| Semantic map markers | ✓ | SOURCE IMPLEMENTED | ✓ | 61 PNG assets derived from OPR semantic set |
| Game Hub | ✓ | SOURCE IMPLEMENTED | ✓ | Compose |
| Башенки physics | ✓ | SOURCE IMPLEMENTED | ✓ | Kotlin rigid-body/constraints/Canvas |
| Tower global leaderboard | ✓ | SOURCE IMPLEMENTED CORE | queued offline | same web endpoint |
| Liquid Glass visual language | ✓ | PARTIAL | ✓ | native translucent Glass components; device visual polish pending |
| Live Glass / Sensor motion | ✓ | NOT PORTED | — | SensorManager effect still pending |
| Weather layer | ✓ | NOT PORTED | — | web weather service contract needs native/API port |
| Mushroom guide | ✓ | NOT PORTED | — | rich guide/safety content not yet ported |
| Fishing guide | ✓ | NOT PORTED | — | not yet ported |
| Full mushroom intelligence filters | ✓ | PARTIAL | ✓ | core dataset present, detailed specialist UI pending |
| Full Cinema guide/Then&Now UI | ✓ | PARTIAL | ✓ | data present; rich detail/film guide pending |
| Full History filters/Time Slider/What was here | ✓ | PARTIAL | ✓ | map/data/search present, specialist controls pending |
| Full Events category/custom-calendar/reminders UI | ✓ | PARTIAL | cached | today/tomorrow/weekend/7d + free/paid implemented; advanced UX pending |
| Native event reminders | planned web | NOT PORTED | — | notification scheduling pending |
| Rich route/ETA provider | ✓ | PARTIAL | limited | current native source hands coordinates to installed map app |
| API delta/tombstones for all stable datasets | target | PARTIAL | ✓ | versioned API patch + safe full snapshot sync; granular delta expansion pending |

## Вывод

Это уже не оболочка сайта и не пустой Android scaffold, но **100% feature parity пока не заявляется**. Главные архитектурно сложные native основы реализованы; specialist screens выше должны быть завершены до Play Store release.
