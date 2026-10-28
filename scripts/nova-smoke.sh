#!/usr/bin/env bash
# Run only on an isolated emulator: starts/replaces the Nova tour, never clears app data.
set -euo pipefail
ADB_BIN=${ADB_BIN:-adb}
OUT=${NOVA_SMOKE_OUT:-/tmp/nova-smoke}
PKG=com.purpal.catplayground
mkdir -p "$OUT"
adb() { timeout 45 "$ADB_BIN" "$@"; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/nova-window.xml >/dev/null; adb pull /sdcard/nova-window.xml "$OUT/window.xml" >/dev/null; }
click_text() {
    dump
    local x y
    read -r x y < <(python3 - "$OUT/window.xml" "$1" <<'PY'
import sys,re,xml.etree.ElementTree as E
for n in E.parse(sys.argv[1]).iter('node'):
 if sys.argv[2] in n.get('text','') or sys.argv[2] in n.get('content-desc',''):
  b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
  if len(b)==4:
   print((b[0]+b[2])//2,(b[1]+b[3])//2);break
else:raise SystemExit('Missing UI text: '+sys.argv[2])
PY
    )
    adb shell input tap "$x" "$y"
}
open() { adb shell am start -W -S -n "$PKG/.MainActivity" --es open "$1" --ei stage 2 "${@:2}" >/dev/null; }
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size 720x1600
adb shell wm density 300
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c
open nova_menu
sleep 10
shot 01_hangar
click_text '玄影'
sleep 1
adb shell input swipe 360 1400 360 450 400
sleep 1
click_text '啟動全球遠征'
sleep 2
if dump && python3 - "$OUT/window.xml" <<'PY'
import sys
raise SystemExit(0 if '確定重啟遠征' in open(sys.argv[1]).read() else 1)
PY
then click_text '確定重啟遠征'; fi
sleep 3
shot 02_globe_flight
sleep 6
shot 03_globe_arrival
dump
python3 - "$OUT/window.xml" <<'PY'
import sys
s=open(sys.argv[1]).read()
assert '日本' in s and '已抵達戰區' in s,s[:300]
PY
click_text '進入戰區'
sleep 12
shot 04_dog_stage
for ratio in 0.8 0.3 0.2; do
    open nova_boss --ef hp_ratio "$ratio"
    sleep 10
    shot "05_boss_$ratio"
done
open shooter_menu
sleep 4
shot 06_original_menu
open shooter_play --ez autotest true
sleep 10
shot 07_original_stage
adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
if rg -q 'FATAL EXCEPTION' "$OUT/crashes.txt"; then cat "$OUT/crashes.txt"; exit 1; fi
printf 'Nova and original-game UI smoke completed. Screenshots: %s\n' "$OUT"
