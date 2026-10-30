# 玄竹／時差素材包

六張 PNG、24 個圖集區域，僅供 Nova 使用，位於 `app/src/main/assets/nova/forest`。全部由 OpenAI 圖片生成工具製作；原始產物保留於工作區 `/workspace/generated_images`。小怪、駕駛員與 Boss 圖集經第二次調整留白後整合，避免相鄰素材滲入裁切範圍。

| 檔案 | 內容 | 最終來源檔案 |
|---|---|---|
| bosses.png | 熊貓履帶車／收納中間態／球形車，樹懶纜索運輸艦／展環中間態／時序飛行器 | exec-15fd4ae3-c92b-448c-bcd9-bc1ed4ae3020.png |
| enemies.png | 熊貓小戰車、蒸氣砲台、小熊貓精英、供能柱；蛙型無人機、時序砲台、巨嘴鳥精英、時序信標 | exec-a2d86109-d99e-4815-8d58-ff7da4901e21.png |
| effects.png | 玉色能量彈／核心／衝擊環，紫色時序彈／場環／核心 | exec-3ab2de9e-a311-4483-bd75-17c48e70ba64.png |
| pilots.png | 熊貓守備官、樹懶領航員 | exec-be144a66-eaa1-4d36-beeb-6e2eade189bc.png |
| panda-background.png | 中國玄竹反彈試驗場 | exec-59eb7992-dc1c-45a4-a9d6-19382b778b88.png |
| sloth-background.png | 哥斯大黎加時差雨林中繼站 | exec-115cd9ef-6f96-4f0a-bf7f-d7b06d477c14.png |

Boss 三形態使用固定畫布，不依透明邊界重新縮放。反彈預告、場域判定邊界、閘門序號與倒數由程式依戰鬥狀態繪製，不依賴生成圖中的裝飾線條。客串敵人、玩家武器及通用升級道具沿用既有 Nova 科技風素材。

## 黑貓戰機裁切修正

`approved-top-view.png` 寬 1536 像素，但兩機不是以 768 像素等分：橘貓最右側不透明翼尖到 x=798，黑貓最左側從 x=806 開始。舊裁切將 x=768–798、y=514–740 的橘色翼尖帶入黑貓素材，該區有 3875 個 alpha >40 的像素。

`NovaShipRegion` 現在從兩機間透明帶的 x=802 分界。這同時移除黑貓左翼旁的異物並補回橘貓右翼，不改動定稿 PNG、選機圖或飛行器設計。PNG 回歸測試驗證來源異物確實存在、修正後裁切排除它、橘色翼尖保留、分界落在透明帶。
