package com.purpal.catplayground.nova

/** The approved 1536px sheet is asymmetric: orange ends at 798, black begins at 806. */
object NovaShipRegion {
    data class Window(val x: Int, val width: Int)
    fun window(orange: Boolean, sheetWidth: Int): Window {
        val split = (sheetWidth * 802L / 1536L).toInt()
        return if (orange) Window(0, split) else Window(split, sheetWidth - split)
    }
}
