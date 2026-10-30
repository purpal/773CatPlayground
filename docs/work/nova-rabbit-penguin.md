# Nova 兔子／企鵝改版檢查點

日期：2026-10-30。分支：`feat/nova-rabbit-penguin`。

## 使用者授權與驗證方式

- 依差異化規劃先製作兔子與企鵝。
- 不進行實機或模擬器試玩；跑測試通過後合併 main，觸發自動 release build，由使用者試玩。
- `ui-test.yml` 已改為僅手動觸發；`emulator-test.yml` 原本即為手動。Release 流程仍先跑單元測試與 debug build，再建立正式 APK。

## 已實作

- Nova 專用 `StageDef` 工廠複本，第二款遊戲的兔子／企鵝類別及其他關卡維持原樣。
- 基底 `Boss` 增加預設開啟的舊階段策略開關；Nova 新 Boss 使用自己的形態狀態、轉形事件與真實秒數預告。
- 月躍輪式車→跳躍機甲，落點鎖定、雙跳、落地弱點及透明背景噴口／衝擊環。
- 霜脊破冰艦→雙體封鎖平台，可破壞冰障、錯位缺口、解凍與交替舷砲；轉形移除舊投射器。
- 六張載具形態、兩關背景、小怪／精英、彈體／特效、兩位駕駛員與六句通訊。
- 既有遠征次序不變；兔子／企鵝仍在前三站之後的隨機池。

## 驗證紀錄

- 第一輪 debug 編譯成功，83 項單元測試有一項失敗，抓到舷砲殘彈延續到下一輪冰障；已在新一輪預告前清除。
- 第二輪全套單元測試：83 項通過，0 失敗／錯誤／跳過；debug／release 建置成功（4 分 42 秒）。
- 24 個 atlas 窗口皆在 PNG 範圍內且 alpha 非空；workflow 通過 actionlint。
- 本次沒有執行畫面試玩，也不宣稱已通過實機驗收。

本地驗證命令：`./gradlew testDebugUnitTest assembleDebug assembleRelease --offline --no-daemon --max-workers=2 --console=plain -Pkotlin.compiler.execution.strategy=in-process`。合併後由既有 release-please workflow 再跑測試並產出簽署 APK。

## 發布阻塞與接續

- 功能提交：`3a708bd`。已通過測試，準備合併並推送。
- 遠端推送失敗：環境未設定預設 credential helper；改用既有 GitHub CLI 認證後，GitHub 回覆 `Invalid username or token`。
- `gh auth status --hostname github.com` 確認環境的 `GH_TOKEN` 無效。未變更或輸出 token，也沒有另行要求使用者貼出秘密。
- 將完成本地 main 合併；認證恢復後，推送 `feat/nova-rabbit-penguin` 與 `main`，再確認 release-please 的測試、版本建立與簽署 APK 工作成功。尚未觸發遠端 build，不能把本地 APK 當成已發布的正式版。

設計依據：[十關差異化規劃](nova-boss-diversity-plan.md)。素材與參數：[月躍／霜脊美術說明](../art/nova-frontiers/README.md)。
