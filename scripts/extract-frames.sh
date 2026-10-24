#!/usr/bin/env bash
# 模擬器關掉之後才執行：從錄影抽出影格（整段每 3 秒一張，大絕附近每秒 10 張），再刪掉錄影檔。
# ffmpeg 在這一步才安裝，避免它的大量相依套件影響模擬器。
set -u
OUT=emulator-results-out
[ -f "$OUT/run.mp4" ] || exit 0
mkdir -p "$OUT/frames"
ffmpeg -loglevel error -i "$OUT/run.mp4" -vf "fps=1/3,scale=360:-1" "$OUT/frames/all_%03d.png" || true
ult=$(grep -o 'ULTIMATE uptimeMs=[0-9]*' "$OUT/perf.txt" 2> /dev/null | head -1 | grep -o '[0-9]*$')
start=$(cat "$OUT/rec-start.txt" 2> /dev/null)
if [ -n "$ult" ] && [ -n "$start" ]; then
  offset=$(awk -v u="$ult" -v s="$start" 'BEGIN { o = (u - s) / 1000 - 0.3; if (o < 0) o = 0; printf "%.2f", o }')
  ffmpeg -loglevel error -ss "$offset" -t 2.4 -i "$OUT/run.mp4" -vf "fps=10,scale=360:-1" "$OUT/frames/ultimate_%02d.png" || true
fi
rm -f "$OUT/run.mp4"
