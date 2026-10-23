package com.purpal.catplayground

sealed interface Screen {
    data object Lobby : Screen
    data object WaterSortLevels : Screen
    data class WaterSortPlay(val level: Int) : Screen
}
