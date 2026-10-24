#!/usr/bin/env bash
# 在模擬器上跑一輪煙霧測試（由 .github/workflows/emulator-test.yml 呼叫）：
# 截大廳與選單畫面，接著用真實的手指拖曳事件玩一關，
# 收集截圖、錄影抽出的影格、幀耗時與當機紀錄，放到 emulator-results-out/。
set -u
STAGE="${1:-1}"
CAT="${2:-ORANGE}"
PLAY_SECONDS="${3:-110}"
PROBE="${4:-}"
PKG=com.purpal.catplayground
OUT=emulator-results-out
mkdir -p "$OUT/frames"

# 每個 adb 指令最多等 30 秒：模擬器掛掉時才不會整個工作卡到逾時。
ADB_BIN=$(command -v adb)
adb() { timeout 30 "$ADB_BIN" "$@"; }
alive() { adb get-state > /dev/null 2>&1; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
uptime_ms() { adb shell cat /proc/uptime | awk '{printf "%d", $1 * 1000}'; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
# 模擬器是軟體繪圖，降低解析度讓幀耗時比較有參考價值（仍不等於真機）。
adb shell wm size 720x1600
adb shell wm density 300
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
size=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
W=${size%x*}
H=${size#*x}
adb logcat -c
# 從頭到尾把 logcat 存下來，模擬器中途消失也看得到最後發生什麼事。
"$ADB_BIN" logcat -v time > "$OUT/logcat-full.txt" 2>&1 &
LOGCAT_PID=$!

# 1. 大廳、2. 射擊遊戲選單
adb shell am start -W -n $PKG/.MainActivity > /dev/null
sleep 8
shot 01_lobby
adb shell am start -W -S -n $PKG/.MainActivity --es open shooter_menu > /dev/null
sleep 5
shot 02_menu

# 3. 實際遊玩（autotest：無敵、自動放大絕、記錄幀耗時）
adb shell am start -W -S -n $PKG/.MainActivity --es open shooter_play --ei stage "$STAGE" --es cat "$CAT" --ez autotest true ${PROBE:+--es probe "$PROBE"} > /dev/null
REC_START=$(uptime_ms)
echo "$REC_START" > "$OUT/rec-start.txt"
"$ADB_BIN" shell screenrecord --bit-rate 6000000 --time-limit 175 /sdcard/run.mp4 > "$OUT/screenrecord.log" 2>&1 &
sleep 1
adb shell dumpsys gfxinfo $PKG reset > /dev/null
sleep 1.5
shot 03_intro

cx=$((W / 2))
cy=$((H * 3 / 4))
x=$cx
adb shell input motionevent DOWN $cx $cy
start=$(date +%s)
i=0
last=-1
burst_done=0
while true; do
  elapsed=$(( $(date +%s) - start ))
  [ "$elapsed" -ge "$PLAY_SECONDS" ] && break
  # 左右來回移動，偶爾上下
  step=$(( (i % 16) < 8 ? (i % 8) : (8 - i % 8) ))
  x=$(( cx - W / 4 + step * W / 16 ))
  y=$(( cy - ((i / 16) % 2) * H / 12 ))
  adb shell input motionevent MOVE $x $y
  i=$((i + 1))
  if [ $((i % 10)) -eq 0 ] && ! alive; then
    echo "device lost after ${elapsed}s of play" > "$OUT/device-lost.txt"
    break
  fi
  # 大絕一開始就連拍，記錄動畫過程（錄影失敗時也有影格可看）
  if [ "$burst_done" -lt 2 ] && [ "$(adb logcat -d -s ShooterPerf:I | grep -c ULTIMATE)" -gt "$burst_done" ]; then
    burst_done=$((burst_done + 1))
    for k in 0 1 2 3 4 5 6 7 8 9; do adb exec-out screencap -p > "$OUT/frames/ult${burst_done}_$k.png"; done
  fi
  # 每 5 秒截圖、每 15 秒記錄一次記憶體（含 GPU／圖形記憶體），模擬器中途掛掉時看得出當下狀態。
  if [ $((elapsed % 5)) -eq 0 ] && [ "$elapsed" -ne "$last" ] && [ "$elapsed" -gt 0 ]; then
    shot "$(printf '10_play_%03ds' "$elapsed")"
    if [ $((elapsed % 15)) -eq 0 ]; then
      { echo "== ${elapsed}s"; adb shell dumpsys meminfo $PKG | grep -E "TOTAL PSS|Graphics|GL mtrack|EGL mtrack|Native Heap|Java Heap"; } >> "$OUT/meminfo.txt" 2>&1
    fi
    last=$elapsed
  fi
done
adb shell input motionevent UP $x $cy
shot 90_end

adb shell dumpsys gfxinfo $PKG > "$OUT/gfxinfo.txt"
adb shell pkill -INT screenrecord || true
sleep 4
adb shell ls -l /sdcard/run.mp4 > "$OUT/video-diagnostics.txt" 2>&1 || true
timeout 120 "$ADB_BIN" pull /sdcard/run.mp4 "$OUT/run.mp4" >> "$OUT/video-diagnostics.txt" 2>&1 || true
adb logcat -d -s ShooterPerf:I > "$OUT/perf.txt"
kill "$LOGCAT_PID" 2> /dev/null || true
grep -E -A20 "FATAL EXCEPTION|ANR in|Fatal signal" "$OUT/logcat-full.txt" > "$OUT/crashes.txt" || true
grep -E "lowmemorykiller|Killing .*catplayground|am_kill|onTrimMemory" "$OUT/logcat-full.txt" > "$OUT/memory.txt" || true
gzip -f "$OUT/logcat-full.txt"

# 錄影檔留給下一步（scripts/extract-frames.sh）在模擬器關掉後再抽影格。

{
  echo "# Emulator smoke test"
  echo "- stage $STAGE, cat $CAT, played ${PLAY_SECONDS}s, screen ${W}x${H}, probe '$PROBE'"
  echo "- crashes: $( [ -s "$OUT/crashes.txt" ] && echo YES || echo none )"
  [ -f "$OUT/device-lost.txt" ] && echo "- $(cat "$OUT/device-lost.txt")"
  [ -s "$OUT/memory.txt" ] && echo "- memory kills: $(wc -l < "$OUT/memory.txt")"
  echo
  echo "## gfxinfo"
  grep -E "Total frames rendered|Janky frames|percentile|Number Slow|Number Frozen" "$OUT/gfxinfo.txt" | head -20
  echo
  echo "## ShooterPerf"
  cat "$OUT/perf.txt"
} > "$OUT/summary.md"
cat "$OUT/summary.md"
