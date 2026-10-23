package com.purpal.catplayground.watersort.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun everyLevelIsSolvableAndWellFormed() {
        for (level in 1..LevelGenerator.LEVEL_COUNT) {
            val board = LevelGenerator.generate(level)
            val colors = LevelGenerator.colorCount(level)
            assertEquals(colors + LevelGenerator.EMPTY_TUBES, board.tubeCount)
            val counts = board.tubes.flatten().groupingBy { it }.eachCount()
            assertEquals(colors, counts.size)
            assertTrue(counts.values.all { it == LevelGenerator.CAPACITY })
            assertFalse("level $level starts solved", board.isSolved())

            val solution = assertNotNull(Solver.solve(board), "level $level")
            var state = board
            for (move in solution) state = state.pour(move.from, move.to)
            assertTrue("level $level solution does not solve", state.isSolved())
        }
    }

    @Test
    fun generationIsDeterministic() {
        assertEquals(LevelGenerator.generate(42), LevelGenerator.generate(42))
    }

    @Test
    fun difficultyNeverDecreases() {
        val counts = (1..LevelGenerator.LEVEL_COUNT).map { LevelGenerator.colorCount(it) }
        assertEquals(2, counts.first())
        assertEquals(12, counts.last())
        assertTrue(counts.zipWithNext().all { (a, b) -> b >= a })
    }

    private fun <T> assertNotNull(value: T?, message: String): T {
        assertNotNull(message, value)
        return value!!
    }
}
