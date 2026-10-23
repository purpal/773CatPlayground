package com.purpal.catplayground

import com.purpal.catplayground.shooter.model.CatType

sealed interface Screen {
    data object Lobby : Screen
    data object WaterSortLevels : Screen
    data class WaterSortPlay(val level: Int) : Screen
    data object ShooterMenu : Screen
    data class ShooterPlay(val stage: Int, val cat: CatType) : Screen
}
