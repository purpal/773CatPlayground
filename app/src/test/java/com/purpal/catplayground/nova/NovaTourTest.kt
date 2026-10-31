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
                assertEquals(NovaTour.COMPLETED,tour.order.take(11))
                assertEquals(13,tour.order.toSet().size)
                val original=Tour.start(cat,Random(seed))
                assertEquals(original.order.toSet(),tour.order.toSet())
                assertEquals(setOf(13),tour.order.drop(11).dropLast(1).toSet())
                assertEquals(Tour.CHAMPION,tour.order.last())
                assertEquals(cat,tour.cat)
                assertEquals(tour,TourState.decode(tour.encode()))
            }
        }
    }
    @Test fun lastUnfinishedStagePrecedesChampion() {
        val suffixes=(0..40).map { NovaTour.start(CatType.ORANGE,Random(it)).order.drop(11).dropLast(1) }
        assertEquals(setOf(listOf(13)),suffixes.toSet())
    }
    @Test fun existingSaveKeepsClearsRevealedFlightAndEquipment() {
        val original=Tour.start(CatType.BLACK,Random(2))
        for(cleared in 0..13) for(revealed in cleared..minOf(13,cleared+1)) {
            val old=original.copy(cleared=cleared,revealed=revealed,totalScore=12345,loadout=Loadout(MainWeapon.FOCUS,3,SubWeapon.HOMING,2,1,.55f))
            val next=NovaTour.resume(old)
            assertEquals(old.order.take(revealed),next.order.take(revealed))
            assertEquals(old.order.toSet(),next.order.toSet())
            assertEquals(13,next.order.size)
            assertEquals(old.copy(order=next.order),next)
            val pending=old.order.drop(revealed)
            assertEquals(NovaTour.COMPLETED.filter { it in pending },next.order.drop(revealed).filter { it in NovaTour.COMPLETED })
            val left=next.order.drop(revealed)
            assertEquals(left.filter { it in NovaTour.COMPLETED },left.takeWhile { it in NovaTour.COMPLETED })
            assertEquals(next,NovaTour.resume(next))
            assertEquals(next,TourState.decode(next.encode()))
        }
    }
    @Test fun originalGameKeepsItsRandomFirstOpponent() {
        val starts=(0..40).map { Tour.start(CatType.ORANGE,Random(it)).nextStage }.toSet()
        assertTrue(starts.size>1)
        assertTrue(starts.any { it!=2 })
    }
}
