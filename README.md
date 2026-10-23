# 773CatPlayground
做給小7玩的遊戲

## 版號與發佈

版號由 GitHub Actions 的 [release-please](https://github.com/googleapis/release-please-action) 自動管理，請勿手動修改版號或 `CHANGELOG.md`。

1. Commit 訊息請遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hant/)：
   - `fix: ...` → 修訂版號 +1（0.1.0 → 0.1.1）
   - `feat: ...` → 次版號 +1（0.1.0 → 0.2.0）
   - `feat!: ...` 或內文含 `BREAKING CHANGE:` → 1.0 之前升次版號，1.0 之後升主版號
   - `docs:`、`chore:`、`refactor:`、`test:` 等不會觸發新版本
2. 推送到 `main` 後，release-please 會自動開 Release PR（更新 `version.txt` 與 `CHANGELOG.md`）並**自動合併**。
3. 合併後會立即建立 `vX.Y.Z` tag 與 GitHub Release，並執行 release build，把產出的檔案上傳到該 Release。

> 首次使用前，請到 repo 的 **Settings → Actions → General → Workflow permissions**，
> 勾選「Allow GitHub Actions to create and approve pull requests」。
