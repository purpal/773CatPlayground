package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.sin

/** Choose between a vulnerable reactor and the capacitors powering the next discharge. */
class NovaKoalaBoss : NovaVehicleBoss(36000f, 145f, 75000) {
    override val kind = EnemyKind.BOSS_KOALA
    override val name = "眠巢司令 尤加"
    override val transformAt = .30f
    override val artPrefix = "KOALA"
    var clock = 0f
        private set
    var cycle = 0
        private set
    val cooling get() = !transforming && clock in 2f..5f
    val weakSide get() = if (cycle % 2 == 0) -1f else 1f
    override val damageScale get() = if (form == 0 && cooling) 2f else 1f
    private var hatch: NovaKoalaHatch? = null
    override fun cancelAttack(w: World) {
        w.enemies.filter { it is NovaCapacitor && it.owner === this || it is NovaKoalaHatch && it.owner === this }
            .forEach { it.alive = false }
        hatch = null
        clock = 0f
    }
    override fun beginForm(w: World) { clock = 0f }
    override fun combat(w: World, dt: Float) {
        clock += dt
        if (form == 0) x = approach(x, 500f + sin(age * .35f) * 65f, 2f, dt)
        else x = approach(x, 500f, 3f, dt)
        if (form == 1 && (hatch == null || w.enemies.none { it === hatch })) {
            hatch = NovaKoalaHatch(this).also { w.spawn(it) }
        }
        if (crossed(clock, dt, .05f)) {
            for (side in listOf(-1f, 1f)) {
                // Anchored form alternates one side, rather than doubling the hazard density.
                if (form == 0 || side == weakSide) w.spawn(NovaCapacitor(this, side, x + side * 250f, y + 170f))
            }
            w.cue(SoundCue.WARNING)
        }
        // Modest support fire ends before the reactor opens.
        if (clock in .6f..1.6f && every(clock, dt, .7f)) {
            for (k in -1..1) w.shoot(x, y + 100f, HALF_PI + k * .35f, 200f, BulletStyle.SEED, Tint.GREEN)
        }
        if (clock >= 9f) { clock = 0f; cycle++ }
    }
}

class NovaKoalaHatch(val owner: NovaKoalaBoss) : Enemy(owner.x, owner.y, 1f, 55f, 0) {
    override val kind = EnemyKind.NOVA_KOALA_HATCH
    override val damageTarget get() = owner
    override val damageIdentity get() = owner
    override val damageScale get() = 2f
    override val invulnerable get() = !owner.cooling || owner.entering || owner.transforming
    override val contactDamage = 0f
    override fun update(w: World, dt: Float) {
        x = owner.x + owner.weakSide * 205f; y = owner.y + 55f
        if (!owner.alive || owner.hp <= 0f || owner.transforming) alive = false
    }
}

/** Visible charge -> locked warning -> one aimed pulse; killing the node cancels that pulse. */
class NovaCapacitor(val owner: NovaKoalaBoss?, val side: Float, x: Float, y: Float) : Enemy(x, y, 180f, 52f, 180) {
    override val kind = EnemyKind.NOVA_CAPACITOR
    override val contactDamage = 0f
    var aim = HALF_PI
        private set
    var discharged = false
        private set
    val warning get() = age >= LOCK && age < FIRE
    override fun update(w: World, dt: Float) {
        age += dt
        if (owner != null && (!owner.alive || owner.hp <= 0f || owner.transforming)) { alive = false; return }
        if (crossed(age, dt, LOCK)) { aim = w.aimAt(x, y); w.cue(SoundCue.WARNING) }
        if (crossed(age, dt, FIRE)) {
            discharged = true
            for (k in -1..1) w.shoot(x, y, aim + k * .18f, 250f, BulletStyle.ORB, Tint.GREEN)
            w.cue(SoundCue.SHOT_HEAVY)
        }
        if (age >= FIRE + .7f) alive = false
    }
    companion object { const val LOCK = 4.8f; const val FIRE = 6.2f }
}
