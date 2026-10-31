# 浣熊與猴子美術

使用 OpenAI image generation 產生。圖集以透明間隔切割，原圖不重新壓縮或修改像素。Boss 每列三形態；小怪與特效每列四張；駕駛員左右各一張。

- `bosses.png` ← `exec-2c2fbba3-4159-43e2-a589-45c20471c62f.png`
- `enemies.png` ← `exec-00cc556f-924f-4013-93e9-2bef57896741.png`
- `effects.png` ← `exec-afbe25c5-0e7c-40b0-aee5-7c0667cca247.png`
- `pilots.png` ← `exec-607b11a7-6c94-4ac7-a315-fbbe7a5757b9.png`
- `raccoon-background.png` ← `exec-c5876fde-901f-4a82-8149-4378e769a1e5.png`
- `monkey-background.png` ← `exec-796a5906-d8b1-4ba7-85b8-157c99e87b6e.png`

遊戲使用 `app/src/main/assets/nova/echoes/manifest.json` 的 24 個區域。Boss 保留等大畫布以便形態交叉淡化；其餘依 alpha 自動去除留白。圖集邊界 alpha ≤ 40，駕駛員共享的中央切線也通過此檢查；人像外側保留原本的半身裁切。
