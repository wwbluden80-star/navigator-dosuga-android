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
sleep 20
adb logcat -d -v threadtime > "$LOG"

PID="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [[ -z "$PID" ]]; then
  echo "SMOKE_FAIL: process exited after launch"
  tail -n 250 "$LOG"
  exit 1
fi

if grep -Eq "FATAL EXCEPTION:|Process: $PACKAGE|>>> $PACKAGE <<<" "$LOG"; then
  echo "SMOKE_FAIL: fatal crash found in logcat"
  tail -n 250 "$LOG"
  exit 1
fi

if ! adb shell dumpsys activity activities | grep -F "$PACKAGE/$ACTIVITY"; then
  echo "SMOKE_FAIL: MainActivity is not active"
  tail -n 250 "$LOG"
  exit 1
fi

echo "ANDROID_16_SMOKE_PASS package=$PACKAGE pid=$PID activity=$ACTIVITY"
