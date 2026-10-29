package com.purpal.catplayground.nova

import com.purpal.catplayground.Screen
import com.purpal.catplayground.lobby.GameRegistry
import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class NovaTourTest {
    @Test fun thirdGameDoesNotReplaceExistingEntries() {
        assertEquals(listOf("water_sort","shooter","nova"), GameRegistry.games.map { it.id })
        assertEquals(Screen.ShooterMenu, GameRegistry.games[1].start)
        assertEquals(Screen.NovaMenu, GameRegistry.games[2].start)
    }
    @Test fun newTourStartsWithCompletedArtAndKeepsEveryOpponentOnce() {
        repeat(40) { seed ->
            for(cat in CatType.entries) {
                val tour=NovaTour.start(cat,Random(seed))
                assertEquals(listOf(2,3,4),tour.order.take(3))
                assertEquals(13,tour.order.toSet().size)
                val original=Tour.start(cat,Random(seed))
                assertEquals(original.order.toSet(),tour.order.toSet())
                assertEquals(original.order.filter { it !in listOf(2,3,4) },tour.order.drop(3))
                assertEquals(Tour.CHAMPION,tour.order.last())
                assertEquals(cat,tour.cat)
                assertEquals(tour,TourState.decode(tour.encode()))
            }
        }
    }
    @Test fun originalGameKeepsItsRandomFirstOpponent() {
        val starts=(0..40).map { Tour.start(CatType.ORANGE,Random(it)).nextStage }.toSet()
        assertTrue(starts.size>1)
        assertTrue(starts.any { it!=2 })
    }
}
