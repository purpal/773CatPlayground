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
- 副武器：追蹤彈、聲波彈（展開弧形牆擋子彈，雷射擋不住），疊加到 Lv3
- 大絕：小橘變身老虎、小黑變身黑豹大吼，消除全螢幕子彈（最多 3 次）
- 中彈會降低護甲與移動速度，修理道具可回復；每次挑戰可接關 3 次
- 目前完成第 1～3 關（老鼠、狗狗、熊熊），其餘 10 關製作中

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
