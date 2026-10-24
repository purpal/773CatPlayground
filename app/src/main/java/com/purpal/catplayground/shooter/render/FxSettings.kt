package com.purpal.catplayground.shooter.render

/** 玩家在設定裡選的特效選項（進入關卡時從 AppPrefs 讀進來）。 */
object FxSettings {
    /** 顯示傷害數字。 */
    @Volatile var damageNumbers = true

    /** 特效品質高：多畫一層柔光（子彈光暈、光束外光、閃電光暈）。 */
    @Volatile var high = true
}
