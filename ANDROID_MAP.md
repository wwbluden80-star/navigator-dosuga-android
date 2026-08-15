# Native Map

Map engine: MapLibre Native Android.

- MapView живёт нативно внутри Compose AndroidView.
- GeoJSON source + symbol layers используются вместо Compose View на каждый marker.
- clustering выполняется на map source/layer уровне.
- camera state сохраняется в DataStore.
- semantic marker registry переносит OPR species/category system из V16.1.
- null-coordinate research records остаются в списках, но не рисуются как фальшивые точки.

## Offline

`OfflineMapManager` создаёт `OfflineTilePyramidRegionDefinition`, отслеживает progress/status и умеет удалять пакеты.
