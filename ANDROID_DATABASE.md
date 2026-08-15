# Room Database

Database v1 содержит content, events, profiles, Saved, Trip, sync, pending actions, game stats/runs, track points и car markers.

Основные индексы: coordinates, score, updatedAt, event datetime/status/category, profile and game keys.

Production rule: новые схемы должны добавляться только явными Room migrations. Destructive migration вверх не используется. Downgrade fallback в source предназначен для dev recovery и должен быть пересмотрен перед final release policy.
