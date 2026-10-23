package com.purpal.catplayground.watersort.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    @Test
    fun poursWholeTopRunWhenThereIsSpace() {
        val board = Board(listOf(listOf(0, 1, 1), listOf(1), emptyList()), 4)
        assertEquals(2, board.pourAmount(0, 1))
        val next = board.pour(0, 1)
        assertEquals(listOf(0), next.tubes[0])
        assertEquals(listOf(1, 1, 1), next.tubes[1])
    }

    @Test
    fun poursOnlyWhatFits() {
        val board = Board(listOf(listOf(1, 1, 1), listOf(0, 0, 1)), 4)
        assertEquals(1, board.pourAmount(0, 1))
    }

    @Test
    fun rejectsDifferentColorsAndFullTubes() {
        val board = Board(listOf(listOf(0), listOf(1), listOf(0, 0, 0, 0)), 4)
        assertFalse(board.canPour(0, 1))
        assertFalse(board.canPour(0, 2))
        assertFalse(board.canPour(0, 0))
    }

    @Test
    fun solvedWhenEveryTubeIsEmptyOrComplete() {
        assertTrue(Board(listOf(listOf(2, 2, 2, 2), emptyList()), 4).isSolved())
        assertFalse(Board(listOf(listOf(2, 2, 2), listOf(2)), 4).isSolved())
    }
}
