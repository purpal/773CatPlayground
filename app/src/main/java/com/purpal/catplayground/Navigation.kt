package com.purpal.catplayground

import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Loadout

sealed interface Screen {
    data object Lobby : Screen
    data object WaterSortLevels : Screen
    data class WaterSortPlay(val level: Int) : Screen
    data object ShooterMenu : Screen
    /** [loadout]：從上一關帶過來的機體狀態（只在這次進入時使用）。 */
    data class ShooterPlay(val stage: Int, val cat: CatType, val loadout: Loadout? = null) : Screen
}
