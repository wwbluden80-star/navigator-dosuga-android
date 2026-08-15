# Android permissions

Manifest source запрашивает только необходимые возможности:
- INTERNET / ACCESS_NETWORK_STATE
- ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION
- FOREGROUND_SERVICE / FOREGROUND_SERVICE_LOCATION
- POST_NOTIFICATIONS

`ACCESS_BACKGROUND_LOCATION` не добавлен.

Location permission запрашивается по действию пользователя. Track recording должен запускаться пользователем как foreground service. Notification permission должен запрашиваться только при функции, которая реально требует уведомления.
