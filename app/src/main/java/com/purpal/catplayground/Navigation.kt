package com.purpal.catplayground

import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Loadout

sealed interface Screen {
    data object Lobby : Screen
    data object WaterSortLevels : Screen
    data class WaterSortPlay(val level: Int) : Screen
    data object NovaMenu : Screen
    data object NovaTour : Screen
    data class NovaPlay(val stage: Int, val cat: CatType, val loadout: Loadout? = null, val bout: Int? = null) : Screen
    data object ShooterMenu : Screen
    /** 世界巡迴的地圖與對戰表。 */
    data object ShooterTour : Screen
    /**
     * [loadout]：打完上一站時的機體狀態；[bout]：世界巡迴的第幾站（從 0 算，決定強度）；
     * null 表示不在巡迴裡（除錯直接開某一關），照關卡的設定值打。
     */
    data class ShooterPlay(val stage: Int, val cat: CatType, val loadout: Loadout? = null, val bout: Int? = null) : Screen
}
