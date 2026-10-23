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
}
