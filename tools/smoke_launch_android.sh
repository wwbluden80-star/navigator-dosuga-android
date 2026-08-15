#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="ru.navigatordosuga.app.debug"
ACTIVITY="ru.navigatordosuga.app.MainActivity"
LOG="app/build/outputs/apk/debug/android-16-smoke-logcat.txt"

test -s "$APK"
adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$PACKAGE/$ACTIVITY"

PID=""
for second in $(seq 1 20); do
  sleep 1
  PID="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  if [[ -z "$PID" ]]; then
    adb logcat -d -v threadtime > "$LOG"
    echo "SMOKE_FAIL: process exited ${second}s after launch"
    grep -E "${PACKAGE}|AndroidRuntime|FATAL EXCEPTION|DEBUG|libc[[:space:]]*:|crash_dump|tombstoned|MapLibre|maplibre|Fatal signal|signal [0-9]+" "$LOG" | tail -n 500 || true
    exit 1
  fi
done

adb logcat -d -v threadtime > "$LOG"

if grep -Eq "FATAL EXCEPTION:|Process: $PACKAGE|>>> $PACKAGE <<<" "$LOG"; then
  echo "SMOKE_FAIL: fatal crash found in logcat"
  grep -E "${PACKAGE}|AndroidRuntime|FATAL EXCEPTION|DEBUG|libc[[:space:]]*:|crash_dump|tombstoned|MapLibre|maplibre|Fatal signal|signal [0-9]+" "$LOG" | tail -n 500 || true
  exit 1
fi

if ! adb shell dumpsys activity activities | grep -F "$PACKAGE/$ACTIVITY"; then
  echo "SMOKE_FAIL: MainActivity is not active"
  tail -n 250 "$LOG"
  exit 1
fi

echo "ANDROID_16_SMOKE_PASS package=$PACKAGE pid=$PID activity=$ACTIVITY"
