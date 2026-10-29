package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Nova owns its script; the original shooter's dialogue is untouched. */
fun novaOutro(stage: StageDef, cat: CatType): List<DialogLine> {
    fun pilot(text: String) = DialogLine(Speaker.CAT, text)
    fun boss(text: String) = DialogLine(Speaker.BOSS, text)
    if (stage.number == 2) return listOf(
        boss("骨刃外掛已脫離，主砲停機。櫻花防線……竟然被你突破了。"),
        pilot(if(cat==CatType.ORANGE) "橘曜收束火控。你的駕駛艙還完整，別急著跳船。" else "玄影解除鎖定。剛才那一擊，已經避開你的駕駛艙。"),
        boss("那道貓吼紋章……不是新式武器。古代核心真的回應你了？"),
        pilot("它為什麼甦醒，我們也在找答案。把下一座訊號塔的座標交給我。"),
        boss("座標已上傳。前方的守衛不會留手——帶著你的機體回來，喵航駕駛員。"),
        pilot("收到。保管好這片天空，我們還會再見。"),
    )
    val report = when(stage.number) {
        1 -> "蜂群鏈路中斷，起司無人機全數返航。你的航跡，比雷達預測還快。"
        3 -> "重裝堡壘失去動力。能穿過這層裝甲，你的核心出力確實驚人。"
        4 -> "熱泉反應爐降壓完成。水霧散了，這條航路交給你。"
        5 -> "蘿蔔導彈解除武裝。下次見面，我會重新校準追蹤系統。"
        6 -> "冰層護盾碎裂，全艦停止射擊。南極通道准許通行。"
        7 -> "尤加防禦網離線。古老的訊號正在甦醒，你也聽見了吧。"
        8 -> "雙機同步已解除。能同時讀懂我們的航跡，算你厲害。"
        9 -> "竹節重砲歸位。力量可以擊穿護甲，克制才能守住核心。"
        10 -> "物流航道恢復……別催，座標正在傳送。重要的東西，要完整送達。"
        11 -> "匿蹤矩陣被你看穿了。這次就把航路密鑰當作見面禮。"
        12 -> "遺跡防衛機關休眠。你的紋章，與石壁上的記錄一模一樣。"
        else -> "地層探測陣列停機。訊號源比我們預想的還要深。"
    }
    return listOf(boss(report), pilot("火控解除。我們需要前往下一個核心訊號源，請開放航路。"),
        boss("導航資料已傳送。記住，核心回應的從來不只是力量。"),
        pilot(if(cat==CatType.ORANGE)"橘曜收到。裝甲重新同步，向下一站出發。" else "玄影收到。相位引擎就緒，開始下一段航行。"))
}
