#!/usr/bin/env bash
# 在模擬器上開幾個畫面截圖（.github/workflows/ui-shots.yml 用）。
# scripts/ui-shots.txt 每行一張圖：  檔名 | 等幾秒 | am start 的額外參數 | 截圖前要點的文字（可省略）| 找不到文字時點的座標（可省略）
set -uo pipefail
OUT=${UI_SHOTS_OUT:-ui-shots}
PKG=com.purpal.catplayground
mkdir -p "$OUT"
ADB_BIN=$(command -v adb)
# 每個 adb 指令都加逾時：畫面一直在動時 uiautomator dump 可能等不到「靜止」而卡住
adb() { timeout 40 "$ADB_BIN" "$@"; }
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
while IFS='|' read -r name wait args tap xy <&3; do
    name=$(echo "$name" | xargs); [ -z "$name" ] && continue
    case "$name" in \#*) continue;; esac
    # shellcheck disable=SC2086
    adb shell am start -W -S -n "$PKG/.MainActivity" $args > "$OUT/$name.launch.txt" 2>&1
    sleep 4
    adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1
    tap=$(echo "${tap:-}" | xargs)
    xy=$(echo "${xy:-}" | xargs)
    # shellcheck disable=SC2086
    if [ -n "$tap" ]; then tap_text "$tap" || { [ -n "$xy" ] && adb shell input tap $xy; }; fi
    sleep "$(echo "$wait" | xargs)"
    # 模擬器的桌面偶爾會跳「沒有回應」的對話框，擋住畫面也擋住點擊：拍照前先關掉
    adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1
    sleep 1
    adb exec-out screencap -p > "$OUT/$name.png"
    echo "shot $name"
done 3< scripts/ui-shots.txt
adb logcat -d -s AndroidRuntime:E > "$OUT/crash.txt" 2>&1
rm -f "$OUT/w.xml"
exit 0
