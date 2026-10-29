package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Invoked only from the existing debuggable-intent gate, never by normal play. */
object NovaPreview {
    fun outro(world: World) {
        boss(world, .01f)
        world.damageEnemy(world.boss!!, world.boss!!.maxHp * 10f)
        repeat(240) { world.update(STEP) }
        check(world.phase == Phase.OUTRO_DIALOG)
    }

    fun boss(world: World, hpRatio: Float) {
        world.godMode = true
        repeat(7200) {
            if (world.phase != Phase.BOSS) world.update(STEP)
        }
        val boss = checkNotNull(world.boss)
        world.enemies.removeAll { it !== boss }
        world.bullets.clear(); world.items.clear(); world.particles.clear()
        boss.hp = boss.maxHp * hpRatio.coerceIn(.01f,1f)
        repeat(90) { world.update(STEP) }
    }
}
