#!/usr/bin/env bash
# Isolated emulator only: replaces the Nova tour, but never clears app data.
# Run by .github/workflows/ui-test.yml on a KVM-capable GitHub Actions runner.
set -euo pipefail
ADB_BIN=${ADB_BIN:-adb}
OUT=${NOVA_SMOKE_OUT:-/tmp/nova-smoke}
PKG=com.purpal.catplayground
STEP_LABEL=setup
LOGCAT_PID=
mkdir -p "$OUT"
adb() { timeout 30 "$ADB_BIN" "$@"; }
step() { STEP_LABEL=$1; printf '\nUI check: %s\n' "$STEP_LABEL"; }
shot() {
    adb shell pidof "$PKG" >/dev/null
    adb exec-out screencap -p > "$OUT/$1.png"
    test -s "$OUT/$1.png"
}
dump() {
    adb shell rm -f /sdcard/nova-window.xml >/dev/null &&
    adb shell uiautomator dump /sdcard/nova-window.xml > "$OUT/uiautomator.txt" 2>&1 &&
    adb pull /sdcard/nova-window.xml "$OUT/window.xml" >/dev/null 2>&1
}
# Compose may merge text into content-desc. Only target visible, enabled nodes.
node() {
    python3 - "$OUT/window.xml" "$1" <<'PY'
import sys,re,xml.etree.ElementTree as E
for n in E.parse(sys.argv[1]).iter('node'):
    if n.get('enabled') == 'false': continue
    if not any(sys.argv[2] in n.get(k,'') for k in ('text','content-desc')): continue
    b=list(map(int,re.findall(r'-?\d+',n.get('bounds',''))))
    if len(b)==4 and b[2]>b[0] and b[3]>b[1]:
        print((b[0]+b[2])//2,(b[1]+b[3])//2);break
else: raise SystemExit(1)
PY
}
wait_text() {
    local deadline=$((SECONDS+60))
    while (( SECONDS < deadline )); do
        if dump && node "$1" >/dev/null; then return 0; fi
        sleep 2
    done
    printf 'Missing UI text: %s\n' "$1" >&2
    return 1
}
click_text() {
    wait_text "$1"
    local point x y
    point=$(node "$1")
    read -r x y <<< "$point"
    adb shell input tap "$x" "$y"
}
scroll_to() {
    local attempt
    for attempt in 1 2 3 4; do
        if dump && node "$1" >/dev/null; then return 0; fi
        adb shell input swipe 360 1370 360 450 450
        sleep 1
    done
    printf 'Cannot scroll to UI text: %s\n' "$1" >&2
    return 1
}
open() { adb shell am start -W -S -n "$PKG/.MainActivity" --es open "$1" --ei stage 2 "${@:2}" > "$OUT/launch-$1.txt"; }
ready_game() {
    wait_text "${1:-暫停}"
    local deadline=$((SECONDS+60))
    while (( SECONDS < deadline )); do
        if dump && ! node '艦載系統同步中' >/dev/null; then sleep 4; return 0; fi
        sleep 2
    done
    printf 'Game assets did not finish loading.\n' >&2
    return 1
}
drag_and_fire() {
    adb shell input motionevent DOWN 360 1200
    adb shell input motionevent MOVE 260 1120
    sleep 1
    adb shell input motionevent MOVE 460 1180
    sleep 1
    adb shell input motionevent UP 460 1180
}
finish() {
    local result=$?
    trap - EXIT
    set +e
    adb exec-out screencap -p > "$OUT/last-screen.png"
    dump
    adb shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt"
    adb shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt"
    adb shell getprop sys.boot_completed > "$OUT/boot-completed.txt"
    adb logcat -d -v threadtime > "$OUT/logcat-final.txt"
    if [ -n "$LOGCAT_PID" ]; then kill "$LOGCAT_PID" 2>/dev/null; wait "$LOGCAT_PID" 2>/dev/null; fi
    grep -E -A24 'FATAL EXCEPTION|Fatal signal|ANR in com\.purpal\.catplayground|am_crash.*com\.purpal\.catplayground' "$OUT"/logcat-*.txt > "$OUT/crashes.txt"
    if [ -s "$OUT/crashes.txt" ]; then result=1; fi
    if [ "$result" != 0 ]; then printf '::error title=UI smoke failed::Checkpoint: %s\n' "$STEP_LABEL"; fi
    {
        printf '## UI smoke test\n\n- Result: %s\n' "$([ "$result" = 0 ] && echo PASS || echo FAIL)"
        printf -- '- Last checkpoint: %s\n- Commit: %s\n' "$STEP_LABEL" "${GITHUB_SHA:-local}"
        printf -- '- Screenshots, UI hierarchy, launch logs and logcat are in the workflow artifact.\n'
        printf -- '- Screenshot capture is evidence for visual review; it is not a pixel-diff approval.\n'
    } > "$OUT/summary.md"
    cat "$OUT/summary.md"
    exit "$result"
}
trap finish EXIT

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size 720x1600
adb shell wm density 300
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
# Keep Compose animations active: rotation and globe flight are test subjects.
adb shell settings put global animator_duration_scale 1
adb logcat -c
"$ADB_BIN" logcat -v threadtime > "$OUT/logcat-full.txt" 2>&1 &
LOGCAT_PID=$!

step 'Lobby to Nova hangar'
open lobby --ez autotest true
wait_text '773喵遊樂園'
shot 00_lobby
scroll_to '喵航特攻隊'
click_text '喵航特攻隊'
wait_text '機庫連線中'
click_text '玄影'
for frame in 1 2 3; do shot "01_hangar_breathing_$frame"; sleep 1; done
# The hangar has no vertical scroll: both actions must fit on compact displays.
node '啟動全球遠征' >/dev/null

step 'Taiwan to Japan flight'
node '啟動全球遠征' >/dev/null
click_text '啟動全球遠征'
if dump && node '確定重啟遠征' >/dev/null; then click_text '確定重啟遠征'; fi
for frame in 1 2 3 4 5 6; do shot "02_globe_$frame"; sleep 1; done
wait_text '已抵達戰區'
node '日本' >/dev/null
shot 03_globe_arrival
# No scrolling: departure must be immediately reachable.
node '進入戰區' >/dev/null
node '日本國旗' >/dev/null
click_text '進入戰區'

step 'Dog stage controls and pause / resume'
ready_game
drag_and_fire
shot 04_dog_stage
click_text '暫停'
wait_text '航控暫停'
shot 04_pause
click_text '繼續航行'
wait_text '暫停'

for ratio in 0.8 0.3 0.2; do
    step "Dog boss hp $ratio"
    open nova_boss --ef hp_ratio "$ratio"
    ready_game
    shot "05_boss_$ratio"
done

step 'Compact route layout'
adb shell wm size 720x1280
open nova_menu
wait_text '機庫連線中'
node '繼續遠征' >/dev/null
node '啟動全球遠征' >/dev/null
shot 05_compact_hangar
open nova_tour
wait_text '進入戰區'
shot 05_compact_route
# The pinned button is checked without any swipe.
node '進入戰區' >/dev/null
adb shell wm size 720x1600

step 'Overdrive seals for both pilots'
for cat in ORANGE BLACK; do
    open nova_boss --es cat "$cat" --ef hp_ratio 0.8
    ready_game
    # The retained roar control is bottom-right by default. Capture the whole 1.6s sequence.
    adb shell input tap 628 1435
    for frame in 1 2 3 4 5 6; do shot "08_overdrive_${cat}_$frame"; sleep 0.12; done
done

step 'Tap-anywhere dialogue and clear'
open nova_outro --es cat BLACK
wait_text '戰後通訊'
node '柴犬隊長駕駛員' >/dev/null
node '▼' >/dev/null
shot 09_dialogue_boss
# Tap outside the panel as well as inside it; every sentence must advance exactly once.
for line in 2 3 4 5 6; do
    adb shell input tap 50 250
    wait_text "$line・6"
    shot "09_dialogue_$line"
done
if node '▼' >/dev/null; then echo 'Last sentence must not show a continuation marker' >&2; exit 1; fi
adb shell input tap 360 800
wait_text '戰區解除'
shot 10_clear
adb shell input tap 50 250
wait_text '軌道航路指揮台'
shot 11_return_route

for sector in 3 4; do
    step "Sector $sector stage art"
    open nova_play --ei stage "$sector" --ez autotest true
    ready_game
    sleep 4
    drag_and_fire
    shot "12_sector_${sector}_combat"
    for ratio in 0.8 0.4 0.2; do
        step "Sector $sector boss hp $ratio"
        open nova_boss --ei stage "$sector" --ef hp_ratio "$ratio"
        ready_game
        shot "13_sector_${sector}_boss_$ratio"
        sleep 1
        shot "13_sector_${sector}_attack_$ratio"
    done
    step "Sector $sector pilot dialogue"
    open nova_outro --ei stage "$sector"
    wait_text '戰後通訊'
    node '駕駛員' >/dev/null
    shot "14_sector_${sector}_pilot"
    for line in 2 3 4 5 6; do
        adb shell input tap 50 250
        wait_text "$line・6"
    done
    adb shell input tap 360 800
    wait_text '戰區解除'
    shot "14_sector_${sector}_clear"
done

step 'Original shooter regression'
open shooter_menu
wait_text '喵星萌王爭霸戰'
shot 06_original_menu
open shooter_play --ez autotest true
ready_game 'SCORE'
drag_and_fire
shot 07_original_stage
step complete
