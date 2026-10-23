#!/usr/bin/env bash
# 在模擬器上跑一輪煙霧測試（由 .github/workflows/emulator-test.yml 呼叫）：
# 截大廳與選單畫面，接著用真實的手指拖曳事件玩一關，
# 收集截圖、錄影抽出的影格、幀耗時與當機紀錄，放到 emulator-results-out/。
set -u
STAGE="${1:-1}"
CAT="${2:-ORANGE}"
PLAY_SECONDS="${3:-110}"
PKG=com.purpal.catplayground
OUT=emulator-results-out
mkdir -p "$OUT/frames"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
uptime_ms() { adb shell cat /proc/uptime | awk '{printf "%d", $1 * 1000}'; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
size=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
W=${size%x*}
H=${size#*x}
adb logcat -c

# 1. 大廳、2. 射擊遊戲選單
adb shell am start -W -n $PKG/.MainActivity > /dev/null
sleep 8
shot 01_lobby
adb shell am start -W -S -n $PKG/.MainActivity --es open shooter_menu > /dev/null
sleep 5
shot 02_menu

# 3. 實際遊玩（autotest：無敵、自動放大絕、記錄幀耗時）
adb shell am start -W -S -n $PKG/.MainActivity --es open shooter_play --ei stage "$STAGE" --es cat "$CAT" --ez autotest true > /dev/null
REC_START=$(uptime_ms)
adb shell screenrecord --bit-rate 6000000 --time-limit 175 /sdcard/run.mp4 &
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
while true; do
  elapsed=$(( $(date +%s) - start ))
  [ "$elapsed" -ge "$PLAY_SECONDS" ] && break
  # 左右來回移動，偶爾上下
  step=$(( (i % 16) < 8 ? (i % 8) : (8 - i % 8) ))
  x=$(( cx - W / 4 + step * W / 16 ))
  y=$(( cy - ((i / 16) % 2) * H / 12 ))
  adb shell input motionevent MOVE $x $y
  i=$((i + 1))
  if [ $((elapsed % 15)) -eq 0 ] && [ "$elapsed" -ne "$last" ] && [ "$elapsed" -gt 0 ]; then
    shot "$(printf '10_play_%03ds' "$elapsed")"
    last=$elapsed
  fi
done
adb shell input motionevent UP $x $cy
shot 90_end

adb shell dumpsys gfxinfo $PKG > "$OUT/gfxinfo.txt"
adb shell pkill -INT screenrecord || true
sleep 4
adb pull /sdcard/run.mp4 "$OUT/run.mp4" > /dev/null || true
adb logcat -d -s ShooterPerf:I > "$OUT/perf.txt"
adb logcat -d | grep -E -A20 "FATAL EXCEPTION|ANR in" > "$OUT/crashes.txt" || true

# 整段每 3 秒一張影格，大絕動畫附近每秒 10 張
if [ -f "$OUT/run.mp4" ]; then
  ffmpeg -loglevel error -i "$OUT/run.mp4" -vf "fps=1/3,scale=360:-1" "$OUT/frames/all_%03d.png" || true
  ult=$(grep -o 'ULTIMATE uptimeMs=[0-9]*' "$OUT/perf.txt" | head -1 | grep -o '[0-9]*$')
  if [ -n "$ult" ]; then
    offset=$(awk -v u="$ult" -v s="$REC_START" 'BEGIN { o = (u - s) / 1000 - 0.3; if (o < 0) o = 0; printf "%.2f", o }')
    ffmpeg -loglevel error -ss "$offset" -t 2.4 -i "$OUT/run.mp4" -vf "fps=10,scale=360:-1" "$OUT/frames/ultimate_%02d.png" || true
  fi
  rm -f "$OUT/run.mp4"
fi

{
  echo "# Emulator smoke test"
  echo "- stage $STAGE, cat $CAT, played ${PLAY_SECONDS}s, screen ${W}x${H}"
  echo "- crashes: $( [ -s "$OUT/crashes.txt" ] && echo YES || echo none )"
  echo
  echo "## gfxinfo"
  grep -E "Total frames rendered|Janky frames|percentile|Number Slow|Number Frozen" "$OUT/gfxinfo.txt" | head -20
  echo
  echo "## ShooterPerf"
  cat "$OUT/perf.txt"
} > "$OUT/summary.md"
cat "$OUT/summary.md"
