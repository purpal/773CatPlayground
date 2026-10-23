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
    override val contactDamage = 32f
    val hpRatio: Float get() = (hp / maxHp).coerceIn(0f, 1f)

    /** 這個階段拿在手上的道具（沒有拿東西時為 null）。 */
    abstract fun heldProp(phase: Int): HazardStyle?

    /** 進入最後階段（被打哭）時被打飛的道具，見 [LostProp]。 */
    abstract val lostProps: IntArray

    /** 手的位置（相對於 Boss 中心），道具從這裡伸出去。 */
    open val handX: Float get() = 100f
    open val handY: Float get() = 70f

    /**
     * 用道具或肢體攻擊：預告後從手（或身體）伸出去，貫穿、無法被聲波牆擋下。
     * [length] 為 null 時伸到玩家身後一點。
     */
    protected fun melee(
        w: World,
        style: HazardStyle,
        angle: Float,
        width: Float,
        warn: Float = 0.75f,
        active: Float = 0.55f,
        extend: Float = 0.12f,
        sweep: Float = 0f,
        length: Float? = null,
        fromX: Float = handX,
        fromY: Float = handY,
    ): Laser {
        val reach = length ?: (kotlin.math.sqrt(dist2(x + fromX, y + fromY, w.player.x, w.player.y)) + 140f).coerceIn(520f, 1500f)
        val l = Laser(
            x + fromX, y + fromY, angle, reach, width, warn, active, color = Tint.PINK, damage = MELEE_DAMAGE,
            sweep = sweep, owner = this, offsetX = fromX, offsetY = fromY, style = style, extendTime = extend,
        )
        w.lasers += l
        w.cue(SoundCue.SWING)
        return l
    }

    protected fun aimFrom(w: World, fx: Float, fy: Float): Float = angleTo(x + fx, y + fy, w.player.x, w.player.y)

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

const val MELEE_DAMAGE = 30f
const val BEAM_DAMAGE = 26f

/** 第 1 關：起司鼠王（小起司）。拿著大叉子；被打哭後叉子和紙王冠都被打飛，改用尾巴甩。 */
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
    /** 大叉子朝玩家突刺。 */
    private val forkStab = Attack(2.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) melee(w, HazardStyle.FORK, aimFrom(w, handX, handY), 44f)
        if (every(t, dt, 0.7f, offset = 0.5f)) w.fan(x, y + 60f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.SEED, Tint.SEED)
    }
    /** 生氣了：連續兩次突刺，中間夾一圈起司。 */
    private val doubleStab = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.4f)) melee(w, HazardStyle.FORK, aimFrom(w, handX, handY), 48f, warn = 0.6f)
        if (crossed(t, dt, 1f)) w.ring(x, y + 40f, 14, 200f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    /** 哭著甩尾巴：從一邊掃到另一邊。 */
    private val tailWhip = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            // 從玩家的另一側掃過來
            val playerLeft = w.player.x < x
            melee(
                w, HazardStyle.TAIL, if (playerLeft) HALF_PI - 62f * DEG else HALF_PI + 62f * DEG, 34f,
                warn = 0.8f, active = 1.5f, extend = 0.25f, sweep = if (playerLeft) 84f * DEG else -84f * DEG,
                length = 1150f, fromX = 0f, fromY = 110f,
            )
        }
        if (every(t, dt, 1.2f, offset = 0.4f)) w.ring(x, y + 40f, 14, 180f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }

    override fun heldProp(phase: Int) = if (phase < 2) HazardStyle.FORK else null
    override val lostProps = intArrayOf(LostProp.CROWN, LostProp.FORK)
    override val handX = 104f
    override val handY = 92f
    private val rageStream = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.13f)) w.shoot(x, y + 60f, w.aimAt(x, y) + w.jitter(0.12f), 330f, BulletStyle.SEED, Tint.SEED)
        if (every(t, dt, 1f)) w.ring(x, y + 40f, 20, 170f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }

    override fun attacks(phase: Int) = when (phase) {
        0 -> listOf(cheeseRings, forkStab, seedFans, summon)
        1 -> listOf(spiral, doubleStab, wheels, seedFans)
        else -> listOf(tailWhip, rageStream, spiral)
    }
}

/** 第 2 關：柴犬隊長。拿著大骨頭揮擊；被打哭後隊長帽被打飛。 */
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
    /** 大骨頭橫掃：從右下掃到左下。 */
    private val boneSwing = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            melee(w, HazardStyle.BONE, 20f * DEG, 54f, warn = 0.8f, active = 1.3f, extend = 0.18f, sweep = 110f * DEG, length = 760f)
        }
        if (every(t, dt, 1f, offset = 1.2f)) w.fan(x, y + 50f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.BONE, Tint.BONE)
    }
    /** 大骨頭往玩家戳過去。 */
    private val boneThrust = Attack(2.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.2f)) melee(w, HazardStyle.BONE, aimFrom(w, handX, handY), 54f, warn = 0.65f)
    }
    private val eyeBeams = Attack(4.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            for (side in listOf(-1f, 1f)) {
                w.lasers += Laser(
                    x + side * 40f, y + 20f, HALF_PI + side * 30f * DEG, length = 2400f, width = 26f, warnTime = 0.9f,
                    activeTime = 2.8f, color = Tint.CYAN, damage = BEAM_DAMAGE, sweep = -side * 14f * DEG,
                    owner = this, offsetX = side * 40f, offsetY = 20f,
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
        0 -> listOf(boomerangs, boneThrust, barkRings)
        1 -> listOf(dash, boneSwing, frisbeeStorm, barkRings)
        else -> listOf(eyeBeams, boneSwing, boneBarrage, dash, boneThrust, frisbeeStorm)
    }

    override fun heldProp(phase: Int) = HazardStyle.BONE
    override val lostProps = intArrayOf(LostProp.HAT)
    override val handX = 112f
    override val handY = 86f
}

/** 第 3 關：熊熊大王。拿著蜂蜜棒重擊、熊掌爪擊；被打哭後頭上的蜂蜜罐被打飛。 */
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
    /** 熊掌爪擊：三道爪痕往下劃開。 */
    private val claws = Attack(3.8f) { w, t, dt ->
        fun swipe(vararg degrees: Float) {
            for (d in degrees) {
                melee(w, HazardStyle.CLAW, d * DEG, 56f, warn = 0.9f, active = 0.55f, extend = 0.14f, length = 1300f, fromX = -110f, fromY = 100f)
            }
        }
        if (crossed(t, dt, 0.2f)) swipe(60f, 90f, 120f)
        if (crossed(t, dt, 2f)) swipe(45f, 75f, 105f, 135f)
    }
    /** 蜂蜜棒往玩家重重敲下去，打到的地方濺出一圈蜂蜜。 */
    private val dipperSlam = Attack(2.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) melee(w, HazardStyle.DIPPER, aimFrom(w, handX, handY), 50f, warn = 0.85f, active = 0.6f, extend = 0.1f)
        if (every(t, dt, 0.8f, offset = 1.4f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 40f * DEG, 240f, BulletStyle.RICE, Tint.ORANGE)
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
            // 哭著把蜂蜜棒拖過地面
            melee(
                w, HazardStyle.DIPPER, HALF_PI, 50f, warn = 1f, active = 1.6f, extend = 0.15f,
                sweep = if (w.player.x > x) 18f * DEG else -18f * DEG, length = 1500f,
            )
        }
        if (crossed(t, dt, 3f)) groundSlam.step(w, 0.6f, 0.6f)
    }

    override fun attacks(phase: Int) = when (phase) {
        0 -> listOf(honeyCurtain, dipperSlam, groundSlam)
        1 -> listOf(hives, claws, dipperSlam, spiral)
        else -> listOf(fury, claws, hives, dipperSlam, spiral)
    }

    override fun heldProp(phase: Int) = HazardStyle.DIPPER
    override val lostProps = intArrayOf(LostProp.POT)
    override val handX = 124f
    override val handY = 92f
}
