#!/usr/bin/env bash
# 在模擬器上開幾個畫面截圖（.github/workflows/ui-shots.yml 用）。
# scripts/ui-shots.txt 每行一張圖：  檔名 | 等幾秒 | am start 的額外參數 | 截圖前要點的文字（可省略）
set -uo pipefail
OUT=${UI_SHOTS_OUT:-ui-shots}
PKG=com.purpal.catplayground
mkdir -p "$OUT"
adb install -r app/build/outputs/apk/debug/app-debug.apk > "$OUT/install.txt" 2>&1
tap_text() {
    adb shell uiautomator dump /sdcard/w.xml > /dev/null 2>&1
    adb pull /sdcard/w.xml "$OUT/w.xml" > /dev/null 2>&1 || return 1
    local point
    point=$(python3 - "$OUT/w.xml" "$1" <<'PY'
import sys,re,xml.etree.ElementTree as E
for n in E.parse(sys.argv[1]).iter('node'):
    if any(sys.argv[2] in n.get(k,'') for k in ('text','content-desc')):
        b=list(map(int,re.findall(r'-?\d+',n.get('bounds',''))))
        if len(b)==4: print((b[0]+b[2])//2,(b[1]+b[3])//2); break
PY
)
    [ -n "$point" ] && adb shell input tap $point
}
while IFS='|' read -r name wait args tap <&3; do
    name=$(echo "$name" | xargs); [ -z "$name" ] && continue
    case "$name" in \#*) continue;; esac
    # shellcheck disable=SC2086
    adb shell am start -W -S -n "$PKG/.MainActivity" $args > "$OUT/$name.launch.txt" 2>&1
    sleep 4
    tap=$(echo "${tap:-}" | xargs)
    if [ -n "$tap" ]; then tap_text "$tap"; fi
    sleep "$(echo "$wait" | xargs)"
    adb exec-out screencap -p > "$OUT/$name.png"
    echo "shot $name"
done 3< scripts/ui-shots.txt
adb logcat -d -s AndroidRuntime:E > "$OUT/crash.txt" 2>&1
rm -f "$OUT/w.xml"
exit 0
