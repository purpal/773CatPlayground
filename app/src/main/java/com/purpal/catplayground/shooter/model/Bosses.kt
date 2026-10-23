package com.purpal.catplayground.shooter.model

import kotlin.math.sin

/**
 * Boss 依血量分成三個階段，每個階段輪流使用一組攻擊。
 * 每個攻擊有固定長度，`step` 每一步被呼叫，t 是該攻擊開始後的秒數。
 */
abstract class Boss(hp: Float, radius: Float, score: Int, private val homeY: Float) :
    Enemy(WORLD_WIDTH / 2f, -220f, hp, radius, score) {

    protected class Attack(val duration: Float, val step: (w: World, t: Float, dt: Float) -> Unit)

    abstract val name: String
    val maxHp = hp
    var entering = true
        private set
    var phase = 0
        private set
    private var attackIndex = 0
    private var attackTime = 0f

    override val isBoss = true
    override val invulnerable: Boolean get() = entering
    override val contactDamage = 25f
    val hpRatio: Float get() = (hp / maxHp).coerceIn(0f, 1f)

    protected abstract fun attacks(phase: Int): List<Attack>

    /** 每一步都會執行的移動。 */
    protected abstract fun move(w: World, dt: Float)

    override fun update(w: World, dt: Float) {
        age += dt
        if (entering) {
            y = approach(y, homeY, 1.6f, dt)
            if (near(y, homeY, 3f)) {
                entering = false
                w.onBossArrived()
            }
            return
        }
        move(w, dt)
        val newPhase = when {
            hpRatio > 0.6f -> 0
            hpRatio > 0.25f -> 1
            else -> 2
        }
        if (newPhase != phase) {
            phase = newPhase
            attackIndex = 0
            attackTime = 0f
            w.onBossPhaseChanged(this)
        }
        val list = attacks(phase)
        val attack = list[attackIndex % list.size]
        attackTime += dt
        attack.step(w, attackTime, dt)
        if (attackTime >= attack.duration) {
            attackTime = 0f
            attackIndex++
        }
    }
}

/** 第 1 關：起司鼠王。 */
class MouseBoss : Boss(hp = 7500f, radius = 120f, score = 20000, homeY = 330f) {
    override val kind = EnemyKind.BOSS_MOUSE
    override val name = "起司鼠王"

    override fun move(w: World, dt: Float) {
        x = WORLD_WIDTH / 2f + sin(age * 0.6f) * 200f
    }

    private val cheeseRings = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.9f)) w.ring(x, y + 40f, 16, 190f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    private val seedFans = Attack(3.5f) { w, t, dt ->
        if (every(t, dt, 0.55f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 50f * DEG, 280f, BulletStyle.SEED, Tint.SEED)
    }
    private val summon = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.3f) || crossed(t, dt, 1.4f)) {
            w.spawn(MiniMouse(x - 130f, y + 40f, -1f))
            w.spawn(MiniMouse(x + 130f, y + 40f, 1f))
        }
        if (every(t, dt, 1f)) w.aimed(x, y + 60f, 180f, BulletStyle.BIG_ORB, Tint.CHEESE)
    }
    private val spiral = Attack(5f) { w, t, dt ->
        if (every(t, dt, 0.09f)) {
            val a = t * 2.4f
            w.shoot(x, y + 40f, a, 210f, BulletStyle.CHEESE, Tint.CHEESE)
            w.shoot(x, y + 40f, a + HALF_PI * 2f, 210f, BulletStyle.CHEESE, Tint.CHEESE)
        }
    }
    private val wheels = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.8f)) w.aimed(x, y + 60f, 170f, BulletStyle.BIG_ORB, Tint.CHEESE)
        if (every(t, dt, 1.6f, offset = 0.4f)) w.ring(x, y + 40f, 12, 200f, t, BulletStyle.SEED, Tint.SEED)
    }
    private val tailLaser = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            w.lasers += Laser(
                x, y + 90f, HALF_PI - 35f * DEG, length = 2400f, width = 34f, warnTime = 0.8f, activeTime = 2.6f,
                color = Tint.PINK, damage = 20f, sweep = 27f * DEG, owner = this, offsetY = 90f,
            )
            w.cue(SoundCue.LASER)
        }
        if (every(t, dt, 1.2f, offset = 0.4f)) w.ring(x, y + 40f, 14, 180f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    private val rageStream = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.13f)) w.shoot(x, y + 60f, w.aimAt(x, y) + w.jitter(0.12f), 330f, BulletStyle.SEED, Tint.SEED)
        if (every(t, dt, 1f)) w.ring(x, y + 40f, 20, 170f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }

    override fun attacks(phase: Int) = when (phase) {
        0 -> listOf(cheeseRings, seedFans, summon)
        1 -> listOf(spiral, wheels, seedFans)
        else -> listOf(tailLaser, rageStream, spiral)
    }
}

/** 第 2 關：柴犬隊長。 */
class DogBoss : Boss(hp = 10500f, radius = 120f, score = 30000, homeY = 340f) {
    override val kind = EnemyKind.BOSS_DOG
    override val name = "柴犬隊長"
    private var targetX = WORLD_WIDTH / 2f
    private var dashing = false

    override fun move(w: World, dt: Float) {
        if (!dashing) targetX = WORLD_WIDTH / 2f + sin(age * 0.8f) * 220f
        x = approach(x, targetX, if (dashing) 4f else 3f, dt)
    }

    private val boomerangs = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.6f)) {
            for (side in listOf(-1f, 1f)) {
                val b = w.shoot(x, y + 50f, HALF_PI + side * 60f * DEG, 260f, BulletStyle.BONE, Tint.BONE)
                b.turn = -side * 1.5f
            }
        }
    }
    private val barkRings = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 1f)) w.ring(x, y + 40f, 24, 170f, w.jitter(1f), BulletStyle.NOTE, Tint.NOTE)
    }
    private val dash = Attack(3.2f) { w, t, dt ->
        dashing = t < 3f
        if (every(t, dt, 1f)) targetX = 120f + w.random.nextFloat() * (WORLD_WIDTH - 240f)
        if (every(t, dt, 0.15f)) w.shoot(x, y + 60f, HALF_PI, 210f, BulletStyle.BONE, Tint.BONE)
    }
    private val frisbeeStorm = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.12f)) {
            for (k in 0 until 3) w.shoot(x, y + 30f, t * 1.8f + k * TWO_PI / 3f, 200f, BulletStyle.STAR, Tint.STAR)
        }
    }
    private val eyeBeams = Attack(4.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            for (side in listOf(-1f, 1f)) {
                w.lasers += Laser(
                    x + side * 40f, y, HALF_PI + side * 30f * DEG, length = 2400f, width = 26f, warnTime = 0.9f,
                    activeTime = 2.8f, color = Tint.CYAN, damage = 20f, sweep = -side * 14f * DEG,
                    owner = this, offsetX = side * 40f,
                )
            }
            w.cue(SoundCue.LASER)
        }
        if (every(t, dt, 1.3f, offset = 0.5f)) w.ring(x, y + 40f, 20, 170f, t, BulletStyle.NOTE, Tint.NOTE)
    }
    private val boneBarrage = Attack(3.5f) { w, t, dt ->
        if (every(t, dt, 0.7f)) w.fan(x, y + 50f, w.aimAt(x, y), 7, 70f * DEG, 250f, BulletStyle.BONE, Tint.BONE)
    }

    override fun attacks(phase: Int) = when (phase) {
        0 -> listOf(boomerangs, barkRings)
        1 -> listOf(dash, frisbeeStorm, barkRings)
        else -> listOf(eyeBeams, boneBarrage, dash, frisbeeStorm)
    }
}

/** 第 3 關：熊熊大王。 */
class BearBoss : Boss(hp = 14000f, radius = 140f, score = 40000, homeY = 360f) {
    override val kind = EnemyKind.BOSS_BEAR
    override val name = "熊熊大王"

    override fun move(w: World, dt: Float) {
        x = WORLD_WIDTH / 2f + sin(age * 0.45f) * 150f
    }

    private val honeyCurtain = Attack(4.5f) { w, t, dt ->
        if (every(t, dt, 0.16f)) {
            val hx = 40f + w.random.nextFloat() * (WORLD_WIDTH - 80f)
            w.bullet(hx, -20f, 0f, 150f, BulletStyle.HONEY, Tint.HONEY).ay = 60f
        }
    }
    private val groundSlam = Attack(3.6f) { w, t, dt ->
        if (crossed(t, dt, 0.6f) || crossed(t, dt, 2.2f)) {
            w.shake(0.5f)
            w.ring(x, y + 80f, 14, 150f, t, BulletStyle.BIG_ORB, Tint.ORANGE)
            w.ring(x, y + 80f, 22, 230f, t + 0.1f, BulletStyle.ORB, Tint.HONEY)
            w.cue(SoundCue.EXPLODE_SMALL)
        }
    }
    private val hives = Attack(4f) { w, t, dt ->
        if (every(t, dt, 1f, offset = 0.2f)) {
            w.spawn(Beehive(x, y + 40f, (w.random.nextFloat() - 0.5f) * 520f))
        }
    }
    private val claws = Attack(3.8f) { w, t, dt ->
        fun swipe(vararg degrees: Float) {
            for (d in degrees) {
                w.lasers += Laser(
                    x, y + 60f, d * DEG, length = 2600f, width = 30f, warnTime = 1f, activeTime = 1.1f,
                    color = Tint.RED, damage = 20f, owner = this, offsetY = 60f,
                )
            }
            w.cue(SoundCue.LASER)
        }
        if (crossed(t, dt, 0.2f)) swipe(60f, 90f, 120f)
        if (crossed(t, dt, 2f)) swipe(45f, 75f, 105f, 135f)
    }
    private val spiral = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.1f)) {
            val a = -t * 2f
            w.shoot(x, y + 40f, a, 200f, BulletStyle.HONEY, Tint.HONEY)
            w.shoot(x, y + 40f, a + HALF_PI * 2f, 200f, BulletStyle.HONEY, Tint.HONEY)
        }
        if (every(t, dt, 1.2f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 40f * DEG, 260f, BulletStyle.RICE, Tint.ORANGE)
    }
    private val fury = Attack(4.5f) { w, t, dt ->
        honeyCurtain.step(w, t, dt)
        if (crossed(t, dt, 0.5f)) {
            w.lasers += Laser(
                x, y + 60f, HALF_PI, length = 2600f, width = 40f, warnTime = 1f, activeTime = 1.6f,
                color = Tint.RED, damage = 20f, sweep = if (w.player.x > x) 18f * DEG else -18f * DEG,
                owner = this, offsetY = 60f,
            )
            w.cue(SoundCue.LASER)
        }
        if (crossed(t, dt, 3f)) groundSlam.step(w, 0.6f, 0.6f)
    }

    override fun attacks(phase: Int) = when (phase) {
        0 -> listOf(honeyCurtain, groundSlam)
        1 -> listOf(hives, claws, spiral)
        else -> listOf(fury, claws, hives, spiral)
    }
}
