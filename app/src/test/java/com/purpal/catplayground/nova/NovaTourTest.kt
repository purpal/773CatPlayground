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
    @Test fun dogStartsNewTourButAllOpponentsAndFinalOrderRemain() {
        repeat(40) { seed ->
            for(cat in CatType.entries) {
                val tour=NovaTour.start(cat,Random(seed))
                assertEquals(2,tour.nextStage)
                assertEquals(13,tour.order.toSet().size)
                assertEquals(Tour.POOL.toSet(),tour.order.take(9).toSet())
                assertEquals(Tour.KINGS+Tour.CHAMPION,tour.order.drop(9))
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
