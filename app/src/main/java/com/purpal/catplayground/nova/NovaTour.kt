package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Tour
import com.purpal.catplayground.shooter.model.TourState
import kotlin.random.Random

object NovaTour {
    /** Released sectors first; remaining opponents are shuffled, with the champion last. */
    val COMPLETED = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10)
    fun start(cat: CatType, random: Random = Random.Default): TourState {
        val original = Tour.start(cat, random)
        val remaining = original.order.filter { it !in COMPLETED && it != Tour.CHAMPION }.shuffled(random)
        return original.copy(order = COMPLETED + remaining + Tour.CHAMPION)
    }

    /** Preserve completed and already revealed destinations, loadout and score when resuming. */
    fun resume(tour: TourState): TourState {
        val keep = maxOf(tour.cleared, tour.revealed).coerceIn(0, tour.order.size)
        val remaining = tour.order.drop(keep)
        return tour.copy(order = tour.order.take(keep) + COMPLETED.filter { it in remaining } +
            remaining.filter { it !in COMPLETED && it != Tour.CHAMPION } + remaining.filter { it == Tour.CHAMPION })
    }
}
