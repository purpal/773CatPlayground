# 773CatPlayground
做給小7玩的遊戲

Android App「773喵遊樂園」：一個遊戲大廳，目前收錄「彩虹倒水」。

## 遊戲

### 彩虹倒水
- 把試管裡的彩色水倒來倒去，讓每支試管只剩一種顏色
- 200 關，顏色從 2 種逐步增加到 12 種；每關都由解題器確認有解
- 依通過關卡數升級稱號（🐱 小貓見習生 → 🏆 倒水之神）
- 倒水動畫、過關動畫與隨機稱讚語；音效與背景音樂由程式即時合成
- 可復原上一步、重玩本關

### 喵星萌王爭霸戰
- 貓咪縱向捲軸射擊：橘斑貓「小橘」（護甲特化）或黑貓「小黑」（速度特化）
- 觸控板式操作：按住畫面下方自動射擊，手指移動多少貓咪就跟著移動多少
- 主武器：散射（藍色電漿彈）／集中（連續光束）／專武（小橘巨肉球砲、小黑連射爪擊），同類道具疊加到 Lv5
- 副武器：追蹤彈、力場盾（展開弧形牆擋子彈，雷射擋不住；有耐久，擋太多會碎掉），疊加到 Lv3
- 快速一甩手指：小黑「閃身步」短暫無敵；小橘「金鐘罩」短暫套上一層臨時護甲（後面的站給得比較多）
- 大絕：小橘變身老虎、小黑變身黑豹大吼，消除全螢幕子彈（最多 3 次）
- 滿等後再吃同樣的道具（主武器 Lv5、副武器 Lv3、大絕 3 顆、滿血時的修理）會換成 5000 分
- 中彈會降低護甲與移動速度，修理道具可回復；每次挑戰可接關 3 次
- 世界巡迴：從台灣出發搭小飛機環遊世界，前 9 站從狗狗、熊熊、兔子、企鵝、無尾熊、熊貓、浣熊、猴子、豬豬裡隨機抽，接著挑戰三大天王（水獺 → 樹懶 → 卡皮巴拉），最後打倒萌王鼠鼠就是新萌王！
- 難度依出場順序逐站提高，機體狀態一站一站帶下去

### 新增遊戲
1. 在 `Navigation.kt` 的 `Screen` 加上新遊戲畫面
2. 在 `MainActivity.kt` 的 `AppRoot` 顯示該畫面
3. 在 `lobby/GameRegistry.kt` 加一筆 `GameEntry`，大廳就會出現

## 開發

需要 JDK 17 以上與 Android SDK（compileSdk 35）。

```sh
./gradlew testDebugUnitTest   # 單元測試（含 200 關可解性檢查）
./gradlew assembleDebug       # 建置 debug APK
```

## 版號與發佈

版號由 GitHub Actions 的 [release-please](https://github.com/googleapis/release-please-action) 自動管理，請勿手動修改版號或 `CHANGELOG.md`。

1. Commit 訊息請遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hant/)：
   - `fix: ...` → 修訂版號 +1（0.1.0 → 0.1.1）
   - `feat: ...` → 次版號 +1（0.1.0 → 0.2.0）
   - `feat!: ...` 或內文含 `BREAKING CHANGE:` → 1.0 之前升次版號，1.0 之後升主版號
   - `docs:`、`chore:`、`refactor:`、`test:` 等不會觸發新版本
2. 推送到 `main` 後，release-please 會自動開 Release PR（更新 `.release-please-manifest.json` 中的版號與 `CHANGELOG.md`）並**自動合併**。
3. 合併後會立即建立 `vX.Y.Z` tag 與 GitHub Release，並建置簽署過的 APK 上傳到該 Release。
4. 推送到 `main` 時會先跑單元測試與建置，失敗就不會發版。

APK 簽署需要在 repo 的 **Settings → Secrets and variables → Actions** 設定
`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。

> 首次使用前，請到 repo 的 **Settings → Actions → General → Workflow permissions**，
> 勾選「Allow GitHub Actions to create and approve pull requests」。

## 喵航特攻隊（第三款遊戲）

大廳新增獨立的科幻射擊入口，與「喵星萌王爭霸戰」共用底層戰鬥引擎，使用獨立機體選擇、巡迴進度、最高分與設定存檔。Nova 的兔子／企鵝由獨立關卡工廠建立新 Boss，原遊戲行為與存檔保持原樣。

- 選機：固定 45° 機體、尾焰與呼吸燈，小橘／小黑駕駛員，短螢幕可直接操作底部按鈕。
- 巡迴：球面地球儀、依經緯度計算的大圓航線、同步轉動與飛行動畫。
- 狗狗關：新場景、機體、敵人、道具、彈體，以及 35% 卸武器和 25% 掉指揮裝甲片的艦體狀態。警告線、碰撞、出招節奏仍由既有戰鬥模型決定。
- 熊熊／卡皮巴拉：科技場景、敵機、駕駛員與對話，對應卸鑽頭、增裝雙砲與配件掉落。
- 兔子：月躍戰車於 40% 變形成跳躍機甲，鎖定落點後跳躍，落地有散熱輸出窗口。
- 企鵝：霜脊破冰艦於 55% 展成雙體平台，佈設可破壞的冰障，保留通道並在解凍後交替舷砲射擊。
- 目前 `nova/NovaTour.kt` 的 `DOG_FIRST` 為 `true`，新遠征前三站固定狗狗→熊熊→卡皮巴拉，兔子／企鵝仍在其後的隨機池；設為 `false` 恢復原巡迴。未重製的對手沿用既有素材。
- `assets/nova/` 保存獨立素材與圖集矩形。載入時依 alpha 去除留白並按顯示大小縮放，不修改原 `assets/art/` 檔案。

在隔離 Android 模擬器上可執行 `bash scripts/nova-smoke.sh`，檢查選機、台灣到日本航線、狗狗關及卸甲畫面，再回歸檢查原射擊遊戲。此腳本會開始／取代測試機內第三款遊戲的巡迴，不清除 app 資料。Debug 額外支援 `--es open nova_menu|nova_tour|nova_play|nova_boss`（狗狗關請加 `--ei stage 2`）；`nova_boss --ef hp_ratio 0.3` 用來檢查殘血外觀。正式版不接受這些除錯入口。

## 畫面測試（GitHub Actions）

依 2026-10-30 指示，改版以單元測試與建置檢查為發布門檻，實機試玩由使用者進行。本次不啟動模擬器；`.github/workflows/ui-test.yml` 保留為手動工具。

- PR 與 main push 不再自動執行 UI 試玩；只有明確需要時才在 Actions → **ui-test** → **Run workflow** 手動執行。手動工具使用有 KVM 的 GitHub runner，不在雲端容器執行軟體模擬器。
- 既有腳本檢查大廳入口、機庫、台灣到日本航線、射擊拖曳、暫停／繼續與原射擊遊戲。
- Nova 檢查包含 固定 45° 機體尾焰與呼吸燈、720×1280 選機／航線按鈕可見性、國旗、狗狗／熊熊／卡皮巴拉三關與 Boss 狀態、兩色超載圖騰，以及全畫面點擊推進對話與返回航路。
- 每次執行的 **Artifacts → ui-test-執行編號-次數** 保存截圖、UI 階層、啟動日誌、logcat、記憶體與繪圖統計，保留 14 天；失敗也會上傳已取得的資料。
- main 執行後也會將最新報告保存到 `emulator-results-ui` 分支，方便直接瀏覽；該分支只保存最新一次結果。PR 測試不會寫入 repo。
- 手動執行時，腳本檢查畫面狀態、操作與崩潰；尚未建立像素差異基準，也未擴充兔子／企鵝的畫面試玩。
- 原有 **emulator-test** 保留作手動長時間遊玩與效能診斷，仍在 GitHub runner 執行。**ui-test** 是獨立檢查，目前不作為 Release 發布的前置條件。
