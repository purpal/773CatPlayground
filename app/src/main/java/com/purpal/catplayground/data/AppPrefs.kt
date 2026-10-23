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

    /** 射擊遊戲已通過的最高關卡。 */
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

    /** 破關時保留下來、下一關可以用一次的機體狀態（[com.purpal.catplayground.shooter.model.Loadout] 的編碼）。 */
    var shooterCarry: String?
        get() = prefs.getString("shooter_carry", null)
        set(value) = prefs.edit().putString("shooter_carry", value).apply()

    /** [shooterCarry] 給哪一關用；0 表示沒有。 */
    var shooterCarryStage: Int
        get() = prefs.getInt("shooter_carry_stage", 0)
        set(value) = prefs.edit().putInt("shooter_carry_stage", value).apply()

    fun shooterBest(stage: Int): Int = prefs.getInt("shooter_best_$stage", 0)

    fun setShooterBest(stage: Int, score: Int) {
        if (score > shooterBest(stage)) prefs.edit().putInt("shooter_best_$stage", score).apply()
    }
}
