#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="ru.navigatordosuga.app.debug"
ACTIVITY="ru.navigatordosuga.app.MainActivity"
LOG="app/build/outputs/apk/debug/android-16-smoke-logcat.txt"
SCREENSHOT="app/build/outputs/apk/debug/android-16-main.png"

test -s "$APK"
adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$PACKAGE/$ACTIVITY"

# A clean emulator opens the optional local profile wizard. Dismiss it through
# its accessibility text so the smoke test reaches the actual map on any size.
sleep 2
adb shell uiautomator dump /sdcard/navigator-window.xml >/dev/null 2>&1 || true
adb pull /sdcard/navigator-window.xml /tmp/navigator-window.xml >/dev/null 2>&1 || true
if [[ -s /tmp/navigator-window.xml ]]; then
  TAP="$(python3 - /tmp/navigator-window.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
    node = next((n for n in root.iter('node') if n.attrib.get('text') == 'Пока без настройки'), None)
    if node is not None:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
        print(f'{(x1+x2)//2} {(y1+y2)//2}')
except Exception:
    pass
PY
)"
  if [[ -n "$TAP" ]]; then adb shell input tap $TAP; fi
fi

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

sleep 8
adb exec-out screencap -p > "$SCREENSHOT"

python3 - "$SCREENSHOT" <<'PY'
import statistics
import sys
from PIL import Image, ImageStat

im = Image.open(sys.argv[1]).convert('RGB')
w, h = im.size
# Inspect a grid in the unobstructed centre of the map. A blank MapLibre
# background is nearly uniform; real raster tiles contain texture in most cells.
deviations = []
for row in range(3):
    for col in range(4):
        x1 = int(w * (.06 + col * .16))
        y1 = int(h * (.24 + row * .105))
        crop = im.crop((x1, y1, x1 + int(w*.13), y1 + int(h*.075))).convert('L')
        deviations.append(ImageStat.Stat(crop).stddev[0])
median = statistics.median(deviations)
print(f'MAP_RENDER_METRIC median_patch_stddev={median:.2f} patches={deviations}')
if median < 4.0:
    raise SystemExit('MAP_RENDER_FAIL: centre map region is blank or uniform')
print('MAP_RENDER_PASS: textured raster basemap is visible')
PY

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
