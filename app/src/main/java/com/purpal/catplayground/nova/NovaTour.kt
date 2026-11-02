package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Tour
import com.purpal.catplayground.shooter.model.TourState
import kotlin.random.Random

object NovaTour {
    /** Art coverage metadata; challenge order follows the original nine-pool / kings / champion rules. */
    val COMPLETED = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
    fun start(cat: CatType, random: Random = Random.Default): TourState {
        return Tour.start(cat,random)
    }

    /** Preserve completed and already revealed destinations, loadout and score when resuming. */
    fun resume(tour: TourState, random: Random = Random.Default): TourState {
        val keep = maxOf(tour.cleared, tour.revealed).coerceIn(0, tour.order.size)
        val remaining = tour.order.drop(keep)
        val ordinary=remaining.filter{it in Tour.POOL}
        // Shuffle only a legacy mixed order; a resumed randomized tour must stay stable.
        val pool=if(remaining.take(ordinary.size).any{it !in Tour.POOL})ordinary.shuffled(random)else ordinary
        return tour.copy(order=tour.order.take(keep)+pool+Tour.KINGS.filter{it in remaining}+remaining.filter{it==Tour.CHAMPION})
    }
}
