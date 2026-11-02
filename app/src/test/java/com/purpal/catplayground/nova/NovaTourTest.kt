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
    @Test fun newTourUsesNineRandomOpponentsThenKingsAndChampion() {
        repeat(40) { seed ->
            for(cat in CatType.entries) {
                val tour=NovaTour.start(cat,Random(seed))
                assertEquals(Tour.POOL.toSet(),tour.order.take(9).toSet())
                assertEquals(Tour.KINGS,tour.order.drop(9).take(3))
                assertEquals(13,tour.order.toSet().size)
                val original=Tour.start(cat,Random(seed))
                assertEquals(original.order.toSet(),tour.order.toSet())
                assertEquals(Tour.CHAMPION,tour.order.last())
                assertEquals(cat,tour.cat)
                assertEquals(tour,TourState.decode(tour.encode()))
            }
        }
    }
    @Test fun randomPoolActuallyShufflesAndLegacySaveMigratesOnce() {
        assertTrue((0..40).map { NovaTour.start(CatType.ORANGE,Random(it)).order.take(9) }.toSet().size>20)
        val legacy=Tour.start(CatType.ORANGE).copy(order=(2..13).toList()+1,cleared=1,revealed=2)
        val migrated=NovaTour.resume(legacy,Random(7))
        assertEquals(legacy.order.take(2),migrated.order.take(2))
        assertEquals(Tour.POOL.toSet(),migrated.order.take(9).toSet())
        assertEquals(Tour.KINGS+Tour.CHAMPION,migrated.order.drop(9))
        assertEquals(migrated,NovaTour.resume(migrated,Random(99)))
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
            val left=next.order.drop(revealed)
            assertEquals(left.filter{it in Tour.POOL},left.takeWhile{it in Tour.POOL})
            assertEquals(Tour.KINGS.filter{it in left},left.filter{it in Tour.KINGS})
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
