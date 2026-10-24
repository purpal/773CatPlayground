package com.purpal.catplayground

/**
 * 只給模擬器測試用的開關（debug 版用 `--es probe 名稱,名稱` 設定），
 * 用來逐一關掉可能拖垮模擬器繪圖的功能，找出問題所在。正式版永遠是 false。
 */
object DebugFlags {
    /** 不在背景先把圖片上傳到 GPU（不呼叫 Bitmap.prepareToDraw）。 */
    @Volatile var noPrepare = false

    /** 關掉新特效：拖尾、道具倒數圈、彩紙、果凍壓扁、虛線預告（下面四個加上虛線）。 */
    @Volatile var plainFx = false
    @Volatile var noTrail = false
    @Volatile var noArc = false
    @Volatile var noConfetti = false
    @Volatile var noStretch = false

    /** HUD 用一般文字、大絕按鈕用 emoji，不用自動縮字與圖片。 */
    @Volatile var plainHud = false

    fun apply(probe: String) {
        val names = probe.split(",").map { it.trim() }.toSet()
        noPrepare = "noprepare" in names
        plainFx = "plainfx" in names
        noTrail = plainFx || "notrail" in names
        noArc = plainFx || "noarc" in names
        noConfetti = plainFx || "noconfetti" in names
        noStretch = plainFx || "nostretch" in names
        plainHud = "plainhud" in names
    }
}
