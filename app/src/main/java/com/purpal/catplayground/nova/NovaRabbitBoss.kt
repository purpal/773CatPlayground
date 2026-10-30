package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.abs

/** Lock a position, jump there, then expose the knee cooling system. No sweeping drill. */
class NovaRabbitBoss : NovaVehicleBoss(36000f, 145f, 60000) {
    override val kind = EnemyKind.BOSS_RABBIT
    override val name = "兔兔隊長 蘿蔔"
    override val transformAt = .4f
    override val artPrefix = "RABBIT"
    enum class Motion { CRUISE, LOCK, FLIGHT, RECOVER }
    var motion = Motion.CRUISE
        private set
    var clock = 0f
        private set
    var targetX = 500f
        private set
    var targetY = 700f
        private set
    var nextX = 500f
        private set
    var nextY = 700f
        private set
    var jumpNumber = 1
        private set
    private var fromX = 500f
    private var fromY = 400f
    private var cycle = 0
    val flight: Float get() = if (motion == Motion.FLIGHT) (clock / FLIGHT).coerceIn(0f, 1f) else 0f
    val cooling: Boolean get() = motion == Motion.RECOVER && !transforming
    val landingRadius: Float get() = if (form == 1) 185f else 160f
    override val solid: Boolean get() = super.solid && motion != Motion.FLIGHT
    override val hurtsOnContact: Boolean get() = solid
    override val damageScale: Float get() = if (cooling) 1.6f else 1f

    override fun cancelAttack(w: World) { motion = Motion.CRUISE; clock = 0f }
    override fun beginForm(w: World) { motion = Motion.CRUISE; clock = 0f }
    private fun start(m: Motion) { motion = m; clock = 0f }
    override fun combat(w: World, dt: Float) {
        clock += dt
        when (motion) {
            Motion.CRUISE -> {
                x = approach(x, floatArrayOf(250f, 500f, 750f)[cycle % 3], 2.5f, dt)
                y = approach(y, homeY, 3f, dt)
                if (every(clock, dt, .8f) && clock < 1.8f) {
                    w.fan(x, y + 70f, w.aimAt(x, y), 3, 36f * DEG, 220f, BulletStyle.RICE, Tint.ORANGE)
                }
                if (clock >= 2.4f) {
                    targetX = w.player.x.coerceIn(180f, 820f)
                    targetY = w.player.y.coerceIn(520f, maxOf(520f, w.height * .62f))
                    nextX = (targetX + if (targetX < 500f) 230f else -230f).coerceIn(180f, 820f)
                    nextY = (targetY - 170f).coerceAtLeast(500f)
                    jumpNumber = 1
                    start(Motion.LOCK)
                    w.cue(SoundCue.WARNING)
                }
            }
            Motion.LOCK -> if (clock >= WARN) {
                fromX = x; fromY = y
                start(Motion.FLIGHT)
                w.cue(SoundCue.MISSILE)
            }
            Motion.FLIGHT -> {
                val p = flight
                val ease = p * p * (3f - 2f * p)
                x = fromX + (targetX - fromX) * ease
                y = fromY + (targetY - fromY) * ease
                if (clock >= FLIGHT) {
                    x = targetX; y = targetY
                    if (dist2(x, y, w.player.x, w.player.y) < (landingRadius + w.player.radius).let { it * it }) w.damagePlayer(26f)
                    w.shake(.35f)
                    w.burst(x, y, Tint.ORANGE, 16, 220f)
                    w.cue(SoundCue.EXPLODE_SMALL)
                    // A visibly open downward escape fan, rather than a closed point-blank ring.
                    for (k in 0 until 12) {
                        val a = k * TWO_PI / 12f
                        if (abs(a - HALF_PI) > .65f) w.shoot(x, y, a, 160f, BulletStyle.STAR, Tint.ORANGE)
                    }
                    start(Motion.RECOVER)
                }
            }
            Motion.RECOVER -> if (clock >= 1.5f) {
                if (form == 1 && jumpNumber == 1) {
                    targetX = nextX; targetY = nextY; jumpNumber = 2
                    start(Motion.LOCK)
                    w.cue(SoundCue.WARNING)
                } else {
                    cycle++
                    start(Motion.CRUISE)
                }
            }
        }
    }
    companion object { const val WARN = 1.25f; const val FLIGHT = .7f }
}
