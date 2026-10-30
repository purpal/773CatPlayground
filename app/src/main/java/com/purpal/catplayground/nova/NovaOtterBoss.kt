package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.cos

/** Two real targets, one life pool. Alternate roles, then dock and expose one broadside. */
class NovaOtterBoss : NovaVehicleBoss(19000f, 1f, 80000) {
    // Narrow hulls intercept only the center pellet at normal range; compensate in the shared pool.
    override val hpFactor = .5f
    override val kind = EnemyKind.BOSS_OTTER
    override val name = "雙潮領航員 啾啾＆嚕嚕"
    override val transformAt = .45f
    override val artPrefix = "OTTER"
    // The bridge is not a third invisible hitbox; the two visible hulls receive all hits.
    override val invulnerable get() = true
    override val solid get() = false
    override val hurtsOnContact get() = false
    var clock = 0f
        private set
    var cycle = 0
        private set
    val activeSide get() = if (cycle % 2 == 0) -1f else 1f
    val cooling get() = form == 1 && clock in 5.4f..8.5f && !transforming
    val diving get() = form == 0 && clock in 8f..9f && !transforming
    val diveProgress get() = ((clock - 8f) / 1f).coerceIn(0f, 1f)
    val hulls = mutableListOf<NovaOtterHull>()
    fun hullX(side: Float): Float {
        val separation = if (form == 1) 150f else 235f
        val swap = if (form == 1) 1f else (if (cycle % 2 == 0) 1f else -1f) * if (diving) cos(diveProgress * HALF_PI * 2f) else if (clock >= 9f) -1f else 1f
        return x + side * separation * swap
    }
    /** Fire roles alternate by screen side even after the sister hulls swap positions. */
    fun batteryX(side: Float) = x + side * if (form == 1) 150f else 235f
    override fun cancelAttack(w: World) {
        w.enemies.filterIsInstance<NovaSonarBuoy>().filter { it.owner === this }.forEach { it.alive = false }
        clock = 0f
    }
    override fun beginForm(w: World) { clock = 0f }
    override fun combat(w: World, dt: Float) {
        // Also repairs debug-preview removal without ever duplicating a live hull.
        for (side in listOf(-1f, 1f)) if (hulls.none { it.side == side && it.alive && it in w.enemies }) {
            val hull = NovaOtterHull(this, side); hulls.removeAll { it.side == side }; hulls += hull; w.spawn(hull)
        }
        clock += dt
        if (crossed(clock, dt, .2f)) {
            val launch = batteryX(-activeSide)
            w.spawn(NovaSonarBuoy(this, launch, y + 230f))
            w.cue(SoundCue.WARNING)
        }
        // Torpedo corridor has a full 1.4-second fixed-position warning. No tracking after launch.
        if (clock in 1.6f..3.6f && every(clock, dt, .65f, offset=1.6f)) {
            val launch = batteryX(activeSide)
            for (dx in listOf(-60f, 60f)) w.shoot(launch + dx, y + 100f, HALF_PI, 260f, BulletStyle.FISH, Tint.CYAN)
        }
        // Docked connecting arms warn then project a short horizontal anti-air barrier.
        if (form == 1 && crossed(clock, dt, 3.9f)) {
            w.lasers += Laser(x - 150f, y + 90f, 0f, 300f, 35f, 1.3f, .55f,
                color=Tint.CYAN, damage=22f, style=HazardStyle.BEAM)
            w.cue(SoundCue.WARNING)
        }
        if (form == 0 && crossed(clock, dt, 8f)) w.cue(SoundCue.WARNING)
        if (clock >= 10f) { clock = 0f; cycle++ }
    }
}

class NovaOtterHull(val owner: NovaOtterBoss, val side: Float) : Enemy(owner.hullX(side), owner.y, 1f, 100f, 0) {
    override val kind = EnemyKind.NOVA_OTTER_HULL
    override val damageTarget get() = owner
    override val damageIdentity get() = owner
    override val invulnerable get() = owner.entering || owner.transforming || owner.diving || owner.hp <= 0f
    override val damageScale get() = if (owner.cooling && side == -owner.activeSide) 1.8f else 1f
    override val solid get() = alive && !invulnerable
    override val hurtsOnContact get() = solid
    override fun update(w: World, dt: Float) {
        x = owner.hullX(side); y = owner.y
        if (!owner.alive || owner.hp <= 0f) alive = false
    }
}

class NovaSonarBuoy(val owner: NovaOtterBoss?, x: Float, y: Float) : Enemy(x, y, 120f, 45f, 160) {
    override val kind = EnemyKind.NOVA_SONAR_BUOY
    override val contactDamage = 0f
    var aim = HALF_PI
        private set
    val warning get() = age in 1f..2.4f
    override fun update(w: World, dt: Float) {
        age += dt
        if (owner != null && (!owner.alive || owner.hp <= 0f || owner.transforming)) { alive = false; return }
        if (crossed(age, dt, 1f)) { aim = w.aimAt(x, y); w.cue(SoundCue.WARNING) }
        if (crossed(age, dt, 2.4f)) {
            for (k in -1..1) w.shoot(x, y, aim + k * .32f, 210f, BulletStyle.BUBBLE, Tint.CYAN)
        }
        if (age >= 3.3f) alive = false
    }
}
