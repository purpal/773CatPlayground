package com.purpal.catplayground.lobby

import androidx.compose.ui.graphics.Color
import com.purpal.catplayground.Screen

/**
 * 遊戲大廳的遊戲清單。新增遊戲時：
 * 1. 在 [Screen] 加上新遊戲的畫面
 * 2. 在 MainActivity 的 AppRoot 處理該畫面
 * 3. 在這裡加一筆 [GameEntry]
 */
data class GameEntry(
    val id: String,
    val title: String,
    val description: String,
    /** assets/art 裡的圖示名稱（v2 麻糬萌風，tools/art/icons.py）。 */
    val icon: String,
    val colors: List<Color>,
    val start: Screen,
)

object GameRegistry {
    const val WATER_SORT = "water_sort"
    const val SHOOTER = "shooter"

    val games = listOf(
        GameEntry(
            id = WATER_SORT,
            title = "彩虹倒水",
            description = "把同顏色的水倒在一起",
            icon = "lobby_watersort",
            colors = listOf(Color(0xFF4FC3F7), Color(0xFF7B1FA2)),
            start = Screen.WaterSortLevels,
        ),
        GameEntry(
            id = SHOOTER,
            title = "喵星萌王爭霸戰",
            description = "貓咪射擊！打倒可愛動物成為萌王",
            icon = "lobby_shooter",
            colors = listOf(Color(0xFFFF9100), Color(0xFFD81B60)),
            start = Screen.ShooterMenu,
        ),
    )
}
