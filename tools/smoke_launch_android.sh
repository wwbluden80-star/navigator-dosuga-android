#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="ru.navigatordosuga.app.debug"
ACTIVITY="ru.navigatordosuga.app.MainActivity"
LOG="app/build/outputs/apk/debug/android-16-smoke-logcat.txt"
SCREENSHOT="app/build/outputs/apk/debug/android-16-main.png"
LOCATION_SCREENSHOT="app/build/outputs/apk/debug/android-16-location.png"
FILTER_SCREENSHOT="app/build/outputs/apk/debug/android-16-filter.png"

test -s "$APK"
adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$PACKAGE/$ACTIVITY"

# A clean emulator opens the optional local profile wizard. Cold API 36
# emulators can take several seconds to expose Compose semantics, so retry the
# accessibility lookup instead of taking a screenshot of the wizard.
DISMISSED=false
for attempt in $(seq 1 12); do
  sleep 2
  adb shell uiautomator dump /sdcard/navigator-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/navigator-window.xml /tmp/navigator-window.xml >/dev/null 2>&1 || true
  TAP=""
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
  fi
  if [[ -n "$TAP" ]]; then
    adb shell input tap $TAP
    DISMISSED=true
    echo "PROFILE_WIZARD_DISMISSED attempt=$attempt"
    break
  fi
done
if [[ "$DISMISSED" != true ]]; then
  echo "PROFILE_WIZARD_FAIL: skip action did not become accessible"
  exit 1
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
adb logcat -d -v threadtime > "$LOG"

if ! grep -Eq "MARKER_OVERLAY_RENDERED count=[1-9][0-9]*" "$LOG"; then
  echo "MARKER_SOURCE_FAIL: projected map overlay was not populated"
  grep -E "NativeMap|MapLibre|Mbgl" "$LOG" | tail -n 200 || true
  exit 1
fi

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

# Mushroom markers and clusters use the bundled #278C67 signature colour.
# The analysed region excludes the green top mode chip and active bottom tab.
marker_region = im.crop((0, int(h*.20), w, int(h*.68)))
marker_pixels = sum(
    20 <= red <= 70 and 110 <= green <= 170 and 65 <= blue <= 135
    for red, green, blue in marker_region.getdata()
)
print(f'MARKER_RENDER_METRIC green_signature_pixels={marker_pixels}')
if marker_pixels < 250:
    raise SystemExit('MARKER_RENDER_FAIL: seeded map points are not visibly rendered')
print('MARKER_RENDER_PASS: seeded map points are visible')
PY

# Exercise the actual runtime location flow. Granting both permissions models
# the precise-location path; the emulator fix must produce the blue user marker.
adb shell pm grant "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant "$PACKAGE" android.permission.ACCESS_FINE_LOCATION
adb emu geo fix 37.618423 55.751244 >/dev/null
adb shell uiautomator dump /sdcard/navigator-location.xml >/dev/null 2>&1
adb pull /sdcard/navigator-location.xml /tmp/navigator-location.xml >/dev/null 2>&1
LOCATION_TAP="$(python3 - /tmp/navigator-location.xml <<'PY'
import re,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
node=next((n for n in root.iter('node') if n.attrib.get('content-desc')=='Моё местоположение'),None)
if node is not None:
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
PY
)"
test -n "$LOCATION_TAP"
adb shell input tap $LOCATION_TAP
sleep 8
adb exec-out screencap -p > "$LOCATION_SCREENSHOT"
python3 - "$LOCATION_SCREENSHOT" <<'PY'
import sys
from PIL import Image
im=Image.open(sys.argv[1]).convert('RGB')
w,h=im.size
centre=im.crop((w//2-90,h//2-90,w//2+90,h//2+90))
blue=sum(25<=r<=80 and 90<=g<=160 and 180<=b<=255 for r,g,b in centre.getdata())
print(f'LOCATION_RENDER_METRIC blue_signature_pixels={blue}')
if blue<80: raise SystemExit('LOCATION_RENDER_FAIL: precise user marker is not visible')
print('LOCATION_RENDER_PASS: precise user marker and accuracy state are visible')
PY

# Open filters and drag the first real Compose slider. The ViewModel logs a
# smaller marker overlay after score filtering, proving UI -> state -> data.
adb shell uiautomator dump /sdcard/navigator-filter-button.xml >/dev/null 2>&1
adb pull /sdcard/navigator-filter-button.xml /tmp/navigator-filter-button.xml >/dev/null 2>&1
FILTER_TAP="$(python3 - /tmp/navigator-filter-button.xml <<'PY'
import re,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
node=next((n for n in root.iter('node') if n.attrib.get('text')=='Фильтры'),None)
if node is not None:
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
PY
)"
test -n "$FILTER_TAP"
adb shell input tap $FILTER_TAP
sleep 2
adb shell uiautomator dump /sdcard/navigator-filter.xml >/dev/null 2>&1
adb pull /sdcard/navigator-filter.xml /tmp/navigator-filter.xml >/dev/null 2>&1
SLIDER_SWIPE="$(python3 - /tmp/navigator-filter.xml <<'PY'
import re,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
node=next((n for n in root.iter('node') if n.attrib.get('class','').endswith('SeekBar')),None)
if node is not None:
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    y=(y1+y2)//2
    print(f'{x1+8} {y} {int(x1+(x2-x1)*.78)} {y}')
PY
)"
test -n "$SLIDER_SWIPE"
adb logcat -c
adb shell input swipe $SLIDER_SWIPE 650
sleep 3
adb exec-out screencap -p > "$FILTER_SCREENSHOT"
adb logcat -d -v threadtime > /tmp/navigator-slider-log.txt
python3 - /tmp/navigator-slider-log.txt <<'PY'
import re,sys
raw=open(sys.argv[1],encoding='utf-8',errors='ignore').read()
counts=[int(x) for x in re.findall(r'MARKER_OVERLAY_RENDERED count=(\d+)',raw)]
print(f'SLIDER_FILTER_METRIC overlay_counts={counts}')
if not counts or min(counts)>=37: raise SystemExit('SLIDER_FILTER_FAIL: marker dataset was not reduced')
print('SLIDER_FILTER_PASS: drag updated the marker dataset in real time')
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
