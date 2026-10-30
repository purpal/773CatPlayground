package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.abs
import kotlin.math.sin

/** Naval blockade: fixed gaps, destructible projectors and a predictable thaw window. */
class NovaPenguinBoss : NovaVehicleBoss(34000f, 180f, 70000) {
    override val kind = EnemyKind.BOSS_PENGUIN
    override val name = "企鵝艦長 冰冰"
    override val transformAt = .55f
    override val artPrefix = "PENGUIN"
    var clock = 0f
        private set
    var gapX = 500f
        private set
    var secondGapX = 500f
        private set
    private var cycle = 0
    val cooling: Boolean get() = clock > 8f && !transforming
    override val damageScale: Float get() = if (cooling) 1.4f else 1f
    override fun cancelAttack(w: World) {
        w.enemies.filterIsInstance<NovaIcePylon>().filter { it.owner === this }.forEach { it.alive = false }
        clock = 0f
    }
    override fun beginForm(w: World) { clock = 0f }
    override fun combat(w: World, dt: Float) {
        clock += dt
        x = approach(x, 500f + sin(age * .35f) * 110f, 2f, dt)
        y = approach(y, homeY, 3f, dt)
        if (crossed(clock, dt, .05f)) {
            // Thaw/charge boundary: retire the previous salvo before new lanes become solid.
            // This encounter has no summoned shooters; all live hostile bullets belong to it.
            w.bullets.clear()
            gapX = floatArrayOf(400f, 600f, 500f)[cycle % 3]
            secondGapX = gapX + if (gapX <= 500f) 100f else -100f
            NovaIcePylon.row(w, this, maxOf(620f, w.height * .49f), gapX)
            w.cue(SoundCue.WARNING)
        }
        if (form == 1 && crossed(clock, dt, .8f)) {
            NovaIcePylon.row(w, this, maxOf(820f, w.height * .64f), secondGapX)
            w.cue(SoundCue.WARNING)
        }
        // The lanes thaw completely before aimed broadsides begin; gaps are never ambushed.
        if (clock in 4.8f..7.8f && every(clock, dt, .75f, offset = 4.8f)) {
            val side = if ((clock / .75f).toInt() % 2 == 0) -1f else 1f
            val bx = x + side * if (form == 1) 180f else 115f
            for (k in -1..1) {
                val b = w.shoot(bx, y + 100f, w.aimAt(bx, y + 100f) + k * 16f * DEG, 230f, BulletStyle.ICE, Tint.CYAN)
                // Small frozen salvo is supporting pressure, not a second time-field mechanic.
                b.freezeAt = .25f; b.freezeTime = .6f
            }
            w.cue(SoundCue.SHOT_HEAVY)
        }
        if (clock >= 10f) { clock = 0f; cycle++ }
    }
}

class NovaIcePylon(val owner: NovaPenguinBoss?, x: Float, y: Float, val lifetime: Float = 3.5f) : Enemy(x, y, 95f, 55f, 150) {
    override val kind = EnemyKind.NOVA_ICE_PYLON
    val active: Boolean get() = age >= WARN && age < lifetime - .6f
    override val hurtsOnContact: Boolean get() = active
    override val contactDamage = 22f
    override fun update(w: World, dt: Float) {
        age += dt
        if (owner != null && (!owner.alive || owner.hp <= 0f || owner.transforming)) alive = false
        if (age >= lifetime) alive = false
    }
    companion object {
        const val WARN = 1.3f
        const val GAP_HALF = 155f
        fun row(w: World, owner: NovaPenguinBoss?, y: Float, gapX: Float, lifetime: Float = 3.5f) {
            for (i in 0..8) {
                val x = 100f + i * 100f
                if (abs(x - gapX) >= GAP_HALF) w.spawn(NovaIcePylon(owner, x, y, lifetime))
            }
        }
    }
}
