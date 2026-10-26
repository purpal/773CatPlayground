package com.purpal.catplayground.data

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("cat_playground", Context.MODE_PRIVATE)

    var musicOn: Boolean
        get() = prefs.getBoolean("music_on", true)
        set(value) = prefs.edit().putBoolean("music_on", value).apply()

    var sfxOn: Boolean
        get() = prefs.getBoolean("sfx_on", true)
        set(value) = prefs.edit().putBoolean("sfx_on", value).apply()

    /** 倒水遊戲已通過的最高關卡（關卡依序解鎖）。 */
    var waterSortCleared: Int
        get() = prefs.getInt("water_sort_cleared", 0)
        set(value) = prefs.edit().putInt("water_sort_cleared", value).apply()

    /** 射擊遊戲世界巡迴最多打到第幾站。 */
    var shooterCleared: Int
        get() = prefs.getInt("shooter_cleared", 0)
        set(value) = prefs.edit().putInt("shooter_cleared", value).apply()

    var shooterCat: String
        get() = prefs.getString("shooter_cat", "ORANGE") ?: "ORANGE"
        set(value) = prefs.edit().putString("shooter_cat", value).apply()

    /** 大絕按鈕放在左下角（預設右下角）。 */
    var shooterUltimateOnLeft: Boolean
        get() = prefs.getBoolean("shooter_ultimate_left", false)
        set(value) = prefs.edit().putBoolean("shooter_ultimate_left", value).apply()

    /** 射擊遊戲顯示傷害數字。 */
    var shooterDamageNumbers: Boolean
        get() = prefs.getBoolean("shooter_damage_numbers", true)
        set(value) = prefs.edit().putBoolean("shooter_damage_numbers", value).apply()

    /** 射擊遊戲特效品質高（多一層柔光）；低階手機可以關掉。 */
    var shooterFxHigh: Boolean
        get() = prefs.getBoolean("shooter_fx_high", true)
        set(value) = prefs.edit().putBoolean("shooter_fx_high", value).apply()

    /** 目前這一趟世界巡迴的進度（[com.purpal.catplayground.shooter.model.TourState] 的編碼）；null 表示還沒開始。 */
    var shooterTour: String?
        get() = prefs.getString("shooter_tour", null)
        set(value) = prefs.edit().putString("shooter_tour", value).apply()

    fun shooterBest(stage: Int): Int = prefs.getInt("shooter_best_$stage", 0)

    fun setShooterBest(stage: Int, score: Int) {
        if (score > shooterBest(stage)) prefs.edit().putInt("shooter_best_$stage", score).apply()
    }
}
