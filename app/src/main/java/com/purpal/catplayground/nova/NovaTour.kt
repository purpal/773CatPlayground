package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Tour
import com.purpal.catplayground.shooter.model.TourState
import kotlin.random.Random

object NovaTour {
    /** Temporary art rollout policy; disable to restore a fully random first opponent. */
    const val DOG_FIRST = true
    fun start(cat: CatType, random: Random = Random.Default): TourState {
        val original = Tour.start(cat, random)
        return if (DOG_FIRST) original.copy(order = listOf(2) + original.order.filter { it != 2 }) else original
    }
}
