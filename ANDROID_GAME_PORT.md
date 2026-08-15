# Bashenki native port

WebView не используется.

Native Kotlin modules:
- `TowerPhysics.kt`
- `TowerSimulation.kt`
- `TowerScoring.kt`
- `TowerGameRepository.kt`
- `TowerLeaderboardSync.kt`
- Compose Canvas `TowerGameScreen.kt`

Physics поддерживает rigid bodies, angular velocity, impulses, friction/restitution, sleeping и два rope constraints подвеса. Release удаляет constraints и не переписывает накопленную horizontal velocity.

JVM smoke result текущей ветки:

`TOWER_JVM_PASS releaseV=-13.67670841185739 score=1467 floors=6 maxDrift=0.0`

Это code-level/JVM QA, не Android DEVICE PASS.
