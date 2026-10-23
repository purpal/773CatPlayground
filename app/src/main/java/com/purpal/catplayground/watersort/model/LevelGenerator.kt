package com.purpal.catplayground.watersort.model

import kotlin.random.Random

object LevelGenerator {
    const val LEVEL_COUNT = 200
    const val CAPACITY = 4
    const val EMPTY_TUBES = 2
    const val PALETTE_SIZE = 12

    // 到達這些關卡時顏色數 +1：第 1 關 2 色，第 161 關起 12 色。
    private val colorThresholds = intArrayOf(3, 7, 13, 21, 33, 49, 71, 96, 126, 161)

    fun colorCount(level: Int): Int = 2 + colorThresholds.count { level >= it }

    /** 同一關每次產生的盤面都一樣，而且保證有解。 */
    fun generate(level: Int): Board {
        require(level in 1..LEVEL_COUNT)
        val colors = colorCount(level)
        val palette = (0 until PALETTE_SIZE).shuffled(Random(level)).take(colors)
        var attempt = 0L
        while (true) {
            val random = Random(level * 1_000_003L + attempt++)
            val units = palette.flatMap { color -> List(CAPACITY) { color } }.shuffled(random)
            val board = Board(units.chunked(CAPACITY) + List(EMPTY_TUBES) { emptyList() }, CAPACITY)
            if (isInteresting(board, level, colors) && Solver.solve(board) != null) return board
        }
    }

    private fun isInteresting(board: Board, level: Int, colors: Int): Boolean {
        if (board.tubes.indices.any { board.isComplete(it) }) return false
        val samePairs = board.tubes.sumOf { tube -> tube.zipWithNext().count { (a, b) -> a == b } }
        val maxPairs = when {
            level <= 6 -> colors
            level <= 60 -> colors / 2
            else -> colors / 4
        }
        return samePairs <= maxPairs
    }
}
