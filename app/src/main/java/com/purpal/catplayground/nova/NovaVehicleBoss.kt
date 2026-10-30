package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Explicit vehicle forms, separate from mood, damage art and legacy disarm/armor flags. */
abstract class NovaVehicleBoss(hp: Float, radius: Float, score: Int) : Boss(hp, radius, score, 400f) {
    final override val usesLegacyPhases = false
    final override val breakAt = 0f
    final override val breakKind = BreakKind.ARMOR
    final override val weapon = HazardStyle.BEAM
    final override val lostProps = intArrayOf()
    final override fun heldProp(): HazardStyle? = null
    final override fun move(w: World, dt: Float) = Unit
    private val idle = listOf(Attack(1f) { _, _, _ -> })
    final override fun attacks(phase: Int) = idle

    var form = 0
        private set
    var transforming = false
        private set
    var transformations = 0
        private set
    protected abstract val transformAt: Float
    abstract val artPrefix: String
    val artId: String get() = "${artPrefix}_" + when {
        transforming -> "TRANSITION"
        form == 1 -> "DEPLOYED"
        else -> "BASE"
    }
    override val solid: Boolean get() = !entering && !transforming
    override val hurtsOnContact: Boolean get() = solid
    override fun mood(): Int = if (form == 1) 1 else 0

    final override fun update(w: World, dt: Float) {
        super.update(w, dt)
        if (entering || !alive || hp <= 0f) return
        if (form == 0 && !transforming && hpRatio <= transformAt) {
            cancelAttack(w)
            transforming = true
            transformations++
            transform(w)
            w.items += Item(x, y + 100f, ItemType.REPAIR)
            return
        }
        if (armorTime > 0f) return
        if (transforming) {
            form = 1
            transforming = false
            beginForm(w)
        }
        // Warnings and motion use real simulation seconds, never the attack tempo multiplier.
        combat(w, dt)
    }
    protected abstract fun cancelAttack(w: World)
    protected abstract fun beginForm(w: World)
    protected abstract fun combat(w: World, dt: Float)
}
