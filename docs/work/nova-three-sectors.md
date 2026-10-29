# Nova 三戰區改版實作檢查點（2026-10-29）

分支：`feat/nova-three-sectors`。原版第二款射擊遊戲維持原流程。

## 已完成

- 固定 45° 機體、尾焰與呼吸燈，選機頁底部按鈕不捲動。
- HUD／通訊框美術及九宮格縮放。
- 熊熊／卡皮巴拉場景、Boss 狀態、小怪與精英、子彈與特殊武器、駕駛員與過關對話。
- 熊熊 25% 卸甲、卡皮巴拉 50% 增裝與 25% 配件掉落均對應原本引擎狀態。
- 新遠征前三站狗狗 → 熊熊 → 卡皮巴拉，保留全部 13 名對手與最終 Boss。
- 75 個單元測試通過，debug／release 建置成功。
- 已擴充 scripts/nova-smoke.sh：短螢幕兩個選機按鈕、三關戰鬥、Boss 狀態、對話。

## 自動驗證與發布流程

- 合併 main 後由 GitHub Actions runner 實測，`emulator-results-ui/RUN.txt` 記錄對應 commit 與執行網址，`summary.md` 記錄結果。
- 由 release-please 自動建立新版 release 與 APK。

素材規格見 `docs/art/nova-sectors/README.md`。圖片位於 `app/src/main/assets/nova/sectors/`。
本機建置：`source /workspace/.setup/android-env.sh`，再執行 Gradle（需允許本機 socket）。
GitHub API 目前不可用；git push/fetch 與公開 github.com Actions 頁面可用。
畫面驗證只在 GitHub runner 進行，不啟動本地軟體模擬器。
沒有建立自動喚醒 session 排程；定時喚醒不會重設服務端 token 額度。
