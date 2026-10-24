package com.purpal.catplayground.shooter.model

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Boss 打到一定血量時的變化：卸甲（武器被打飛、換徒手招式）或裝甲（拿出新武器、招式更強）。 */
enum class BreakKind { DISARM, ARMOR }

/**
 * Boss 依血量分成三個階段（決定表情與招式強度），另外在 [breakAt] 的血量比例「卸甲」或「裝甲」，
 * 換一整組攻擊模式。每個攻擊有固定長度，`step` 每一步被呼叫，t 是該攻擊開始後的秒數。
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

    /** 卸甲／裝甲的血量比例，依動物的刻板印象決定：膽小的老鼠很快就丟下武器，熊撐到最後。 */
    abstract val breakAt: Float
    abstract val breakKind: BreakKind
    /** 已經卸甲（或已經裝甲）。 */
    var broken = false
        private set
    /** 裝甲動畫剩餘秒數；這段時間不能被打、也不攻擊。 */
    var armorTime = 0f
        private set

    override val isBoss = true
    override val invulnerable: Boolean get() = entering || armorTime > 0f
    override val contactDamage = 32f
    val hpRatio: Float get() = (hp / maxHp).coerceIn(0f, 1f)

    /** 攻擊用的科技道具。卸甲型在卸甲前拿著、裝甲型在裝甲後才拿出來。 */
    abstract val weapon: HazardStyle

    /** 現在手上拿的道具（沒有時為 null）。 */
    open fun heldProp(): HazardStyle? = when (breakKind) {
        BreakKind.DISARM -> if (broken) null else weapon
        BreakKind.ARMOR -> if (broken) weapon else null
    }

    /** 卸甲時被打飛的武器，見 [LostProp]。 */
    open val lostWeapon: Int get() = 0

    /** 進入最後階段（被打哭）時被打飛的配件，見 [LostProp]。 */
    abstract val lostProps: IntArray

    /** 表情：0 逞強、1 生氣、2 被打哭。 */
    open fun mood(): Int = phase

    /** 手的位置（相對於 Boss 中心），道具從這裡伸出去。 */
    open val handX: Float get() = 150f
    open val handY: Float get() = 105f

    /**
     * 用道具或肢體攻擊：預告後從手（或身體）伸出去，貫穿、無法被力場盾擋下。
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
        val reach = length ?: (sqrt(dist2(x + fromX, y + fromY, w.player.x, w.player.y)) + 140f).coerceIn(520f, 1500f)
        val l = Laser(
            x + fromX, y + fromY, angle, reach, width, warn, active, color = Tint.RED, damage = MELEE_DAMAGE,
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
        if (!broken && hpRatio <= breakAt) {
            broken = true
            attackIndex = 0
            attackTime = 0f
            if (breakKind == BreakKind.ARMOR) armorTime = ARMOR_TIME
            w.onBossBreak(this)
        }
        if (armorTime > 0f) {
            armorTime -= dt
            return
        }
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

    companion object {
        const val ARMOR_TIME = 1.6f
    }
}

const val MELEE_DAMAGE = 30f
const val BEAM_DAMAGE = 26f

/**
 * 第 1 關：起司鼠王（小起司）。拿著電漿叉。
 * 老鼠膽小，65% 血就被打到「卸甲」：叉子飛走，嚇得滿場亂跑、叫小老鼠來幫忙、甩電漿尾巴。
 */
class MouseBoss : Boss(hp = 15000f, radius = 180f, score = 20000, homeY = 410f) {
    override val kind = EnemyKind.BOSS_MOUSE
    override val name = "起司鼠王"
    override val breakAt = 0.65f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.FORK
    override val lostWeapon = LostProp.FORK
    override val lostProps = intArrayOf(LostProp.CROWN)
    override val handX = 156f
    override val handY = 138f

    override fun move(w: World, dt: Float) {
        // 卸甲後慌張地快速左右跑
        x = if (broken) WORLD_WIDTH / 2f + sin(age * 1.5f) * 240f else WORLD_WIDTH / 2f + sin(age * 0.6f) * 180f
    }

    private val cheeseRings = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.9f)) w.ring(x, y + 40f, 16, 190f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    private val seedFans = Attack(3.5f) { w, t, dt ->
        if (every(t, dt, 0.55f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 50f * DEG, 280f, BulletStyle.SEED, Tint.SEED)
    }
    /** 電漿叉朝玩家突刺。 */
    private val forkStab = Attack(2.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) melee(w, HazardStyle.FORK, aimFrom(w, handX, handY), 44f)
        if (every(t, dt, 0.7f, offset = 0.5f)) w.fan(x, y + 60f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.SEED, Tint.SEED)
    }
    /** 叉尖射出三發能量針。 */
    private val forkVolley = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.3f) || crossed(t, dt, 1.1f) || crossed(t, dt, 1.9f)) {
            val a = aimFrom(w, handX, handY)
            for (k in -1..1) w.shoot(x + handX, y + handY, a + k * 9f * DEG, 420f, BulletStyle.RICE, Tint.PINK)
        }
    }
    /** 蓄力三連刺：扇形依序刺出。 */
    private val tripleStab = Attack(3f) { w, t, dt ->
        val base = aimFrom(w, handX, handY)
        if (crossed(t, dt, 0.01f)) melee(w, HazardStyle.FORK, base - 22f * DEG, 40f, warn = 0.7f, active = 0.4f)
        if (crossed(t, dt, 0.5f)) melee(w, HazardStyle.FORK, base + 22f * DEG, 40f, warn = 0.6f, active = 0.4f)
        if (crossed(t, dt, 1.0f)) melee(w, HazardStyle.FORK, base, 48f, warn = 0.6f, active = 0.5f)
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
    /** 邊跑邊撒種子。 */
    private val panicSpray = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.16f)) w.shoot(x, y + 60f, HALF_PI + w.jitter(0.5f), 260f, BulletStyle.SEED, Tint.SEED)
    }
    /** 甩電漿尾巴：從玩家的另一側掃過來。 */
    private val tailWhip = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x
            melee(
                w, HazardStyle.TAIL, if (playerLeft) HALF_PI - 62f * DEG else HALF_PI + 62f * DEG, 34f,
                warn = 0.8f, active = 1.5f, extend = 0.25f, sweep = if (playerLeft) 84f * DEG else -84f * DEG,
                length = 1150f, fromX = 0f, fromY = 110f,
            )
        }
        if (every(t, dt, 1.2f, offset = 0.4f)) w.ring(x, y + 40f, 14, 180f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    private val rageStream = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.13f)) w.shoot(x, y + 60f, w.aimAt(x, y) + w.jitter(0.12f), 330f, BulletStyle.SEED, Tint.SEED)
        if (every(t, dt, 1f)) w.ring(x, y + 40f, 20, 170f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }

    override fun attacks(phase: Int) = when {
        !broken -> listOf(cheeseRings, forkStab, forkVolley, seedFans, tripleStab)
        phase < 2 -> listOf(panicSpray, summon, tailWhip, spiral)
        else -> listOf(tailWhip, rageStream, summon, spiral)
    }
}

/**
 * 第 2 關：柴犬隊長。拿著硬光骨頭：橫掃、突刺，還會把骨頭當迴力鏢丟出去。
 * 柴犬固執，35% 才鬆口「卸甲」：之後改用吠叫的音波、眼睛光束與衝撞。
 */
class DogBoss : Boss(hp = 21000f, radius = 180f, score = 30000, homeY = 420f) {
    override val kind = EnemyKind.BOSS_DOG
    override val name = "柴犬隊長"
    override val breakAt = 0.35f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.BONE
    override val lostWeapon = LostProp.BONE
    override val lostProps = intArrayOf(LostProp.HAT)
    override val handX = 168f
    override val handY = 129f
    private var targetX = WORLD_WIDTH / 2f
    private var dashing = false
    /** 骨頭丟出去還沒回來。 */
    var boneOut = false

    override fun heldProp(): HazardStyle? = if (boneOut) null else super.heldProp()

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
        if (every(t, dt, 1f)) targetX = 200f + w.random.nextFloat() * (WORLD_WIDTH - 400f)
        if (every(t, dt, 0.15f)) w.shoot(x, y + 60f, HALF_PI, 210f, BulletStyle.BONE, Tint.BONE)
    }
    private val frisbeeStorm = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.12f)) {
            for (k in 0 until 3) w.shoot(x, y + 30f, t * 1.8f + k * TWO_PI / 3f, 200f, BulletStyle.STAR, Tint.STAR)
        }
    }
    /** 硬光骨頭橫掃：從右下掃到左下。 */
    private val boneSwing = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            melee(w, HazardStyle.BONE, 20f * DEG, 54f, warn = 0.8f, active = 1.3f, extend = 0.18f, sweep = 110f * DEG, length = 760f)
        }
        if (every(t, dt, 1f, offset = 1.2f)) w.fan(x, y + 50f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.BONE, Tint.BONE)
    }
    /** 骨頭往玩家戳過去（兩次）。 */
    private val boneThrust = Attack(2.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.2f)) melee(w, HazardStyle.BONE, aimFrom(w, handX, handY), 54f, warn = 0.65f)
    }
    /** 把骨頭當迴力鏢丟向玩家，飛一圈再回到手上。 */
    private val boneBoomerang = Attack(3.2f) { w, t, dt ->
        if (crossed(t, dt, 0.4f) && !boneOut) {
            w.spawn(ThrownBone(this, w.player.x, w.player.y))
            w.cue(SoundCue.SWING)
        }
        if (every(t, dt, 0.8f, offset = 0.8f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.NOTE, Tint.NOTE)
    }
    /** 卸甲後：雙眼光束。 */
    private val eyeBeams = Attack(4.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            for (side in listOf(-1f, 1f)) {
                w.lasers += Laser(
                    x + side * 40f, y + 20f, HALF_PI + side * 30f * DEG, length = 2400f, width = 26f, warnTime = 0.9f,
                    activeTime = 2.8f, color = Tint.RED, damage = BEAM_DAMAGE, sweep = -side * 14f * DEG,
                    owner = this, offsetX = side * 40f, offsetY = 20f,
                )
            }
            w.cue(SoundCue.LASER)
        }
        if (every(t, dt, 1.3f, offset = 0.5f)) w.ring(x, y + 40f, 20, 170f, t, BulletStyle.NOTE, Tint.NOTE)
    }
    /** 卸甲後：狂吠，音符一圈圈密集擴散。 */
    private val howl = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.45f)) w.ring(x, y + 40f, 18, 200f, t * 1.7f, BulletStyle.NOTE, Tint.NOTE)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(boomerangs, boneThrust, barkRings, boneBoomerang)
        !broken -> listOf(dash, boneSwing, boneBoomerang, frisbeeStorm, boneThrust)
        else -> listOf(eyeBeams, howl, dash, frisbeeStorm, barkRings)
    }
}

/** 柴犬隊長丟出去的硬光骨頭：飛到玩家剛才的位置附近再繞回 Boss 手上，打不壞，碰到會受傷。 */
class ThrownBone(private val boss: DogBoss, targetX: Float, targetY: Float) :
    Enemy(boss.x + boss.handX, boss.y + boss.handY, 1f, 52f, 0) {
    override val kind = EnemyKind.THROWN_BONE
    override val invulnerable = true
    override val hurtsOnContact = true
    override val contactDamage = MELEE_DAMAGE
    private val startX = x
    private val startY = y
    private val farX = targetX.coerceIn(80f, WORLD_WIDTH - 80f)
    private val farY = targetY
    /** 旋轉角度（度）。 */
    var spin = 0f
        private set

    init {
        boss.boneOut = true
    }

    override fun update(w: World, dt: Float) {
        age += dt
        spin += 900f * dt
        val t = (age / FLIGHT).coerceIn(0f, 1f)
        // 去程與回程走不同的弧線（像迴力鏢一樣繞一圈）
        val a = t * TWO_PI
        val side = if (farX > startX) 1f else -1f
        val homeX = boss.x + boss.handX
        val homeY = boss.y + boss.handY
        val baseX = if (t < 0.5f) startX + (farX - startX) * (t * 2f) else farX + (homeX - farX) * ((t - 0.5f) * 2f)
        val baseY = if (t < 0.5f) startY + (farY - startY) * (t * 2f) else farY + (homeY - farY) * ((t - 0.5f) * 2f)
        x = baseX + sin(a) * 160f * side
        y = baseY - (1f - cos(a)) * 30f
        if (t >= 1f || !boss.alive) {
            alive = false
            boss.boneOut = false
        }
    }

    companion object {
        const val FLIGHT = 2.4f
    }
}

/**
 * 第 3 關：熊熊大王。拿著熱熔蜂蜜鑽：重擊地面、360 度旋轉橫掃。
 * 熊又壯又耐打，撐到 25% 才「卸甲」：之後改用熊掌爪擊、捶地與丟蜂巢。
 */
class BearBoss : Boss(hp = 28000f, radius = 210f, score = 40000, homeY = 440f) {
    override val kind = EnemyKind.BOSS_BEAR
    override val name = "熊熊大王"
    override val breakAt = 0.25f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.DIPPER
    override val lostWeapon = LostProp.DIPPER
    override val lostProps = intArrayOf(LostProp.POT)
    override val handX = 186f
    override val handY = 138f

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
    /** 熱熔蜂蜜鑽往玩家重重敲下去，打到的地方濺出一圈蜂蜜。 */
    private val dipperSlam = Attack(2.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) melee(w, HazardStyle.DIPPER, aimFrom(w, handX, handY), 50f, warn = 0.85f, active = 0.6f, extend = 0.1f)
        if (every(t, dt, 0.8f, offset = 1.4f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 40f * DEG, 240f, BulletStyle.RICE, Tint.ORANGE)
    }
    /** 抓著蜂蜜鑽原地轉一整圈，熱熔的鑽頭掃過整個畫面。 */
    private val dipperSpin = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            melee(w, HazardStyle.DIPPER, 0f, 50f, warn = 1f, active = 2.6f, extend = 0.3f, sweep = 140f * DEG, length = 820f, fromX = 0f, fromY = 40f)
        }
        if (every(t, dt, 0.5f, offset = 1f)) w.aimed(x, y + 60f, 230f, BulletStyle.HONEY, Tint.HONEY)
    }
    private val spiral = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.1f)) {
            val a = -t * 2f
            w.shoot(x, y + 40f, a, 200f, BulletStyle.HONEY, Tint.HONEY)
            w.shoot(x, y + 40f, a + HALF_PI * 2f, 200f, BulletStyle.HONEY, Tint.HONEY)
        }
        if (every(t, dt, 1.2f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 40f * DEG, 260f, BulletStyle.RICE, Tint.ORANGE)
    }
    /** 卸甲後：熊掌爪擊，三道爪痕往下劃開。 */
    private val claws = Attack(3.8f) { w, t, dt ->
        fun swipe(vararg degrees: Float) {
            for (d in degrees) {
                melee(w, HazardStyle.CLAW, d * DEG, 56f, warn = 0.9f, active = 0.55f, extend = 0.14f, length = 1300f, fromX = -110f, fromY = 100f)
            }
        }
        if (crossed(t, dt, 0.2f)) swipe(60f, 90f, 120f)
        if (crossed(t, dt, 2f)) swipe(45f, 75f, 105f, 135f)
    }
    /** 卸甲後：暴怒，蜂蜜雨＋左右兩爪＋捶地。 */
    private val fury = Attack(4.5f) { w, t, dt ->
        honeyCurtain.step(w, t, dt)
        if (crossed(t, dt, 0.5f)) {
            for (side in listOf(-1f, 1f)) {
                melee(w, HazardStyle.CLAW, HALF_PI + side * 25f * DEG, 56f, warn = 0.9f, active = 0.6f, extend = 0.14f, length = 1400f, fromX = side * 110f, fromY = 100f)
            }
        }
        if (crossed(t, dt, 3f)) groundSlam.step(w, 0.6f, 0.6f)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(honeyCurtain, dipperSlam, groundSlam, dipperSpin)
        !broken -> listOf(hives, dipperSpin, dipperSlam, spiral)
        else -> listOf(fury, claws, groundSlam, hives)
    }
}

/**
 * 第 4 關：溫泉大王 卡皮（卡皮巴拉）。泡在檜木桶裡悠哉地放空，攻擊也慢吞吞（泡泡、滾橘子、蒸氣）。
 * 卡皮巴拉淡定、什麼都無所謂，打到一半（50%）才慢慢「裝甲」：戴上護目鏡、裝上肩甲，
 * 拿出橘子砲——高壓溫泉水柱掃射、橘子飛彈連發。
 */
class CapyBoss : Boss(hp = 32000f, radius = 210f, score = 50000, homeY = 440f) {
    override val kind = EnemyKind.BOSS_CAPY
    override val name = "溫泉大王 卡皮"
    override val breakAt = 0.5f
    override val breakKind = BreakKind.ARMOR
    override val weapon = HazardStyle.WATER
    override val lostProps = intArrayOf(LostProp.YUZU)
    override val handX = 195f
    override val handY = 105f

    /** 放空（裝甲前）→ 裝甲（護目鏡）→ 裝甲被打哭。 */
    override fun mood(): Int = when {
        !broken -> 0
        hpRatio > 0.2f -> 1
        else -> 2
    }

    override fun move(w: World, dt: Float) {
        // 漂在溫泉上慢慢晃；裝甲後稍微積極一點
        x = WORLD_WIDTH / 2f + sin(age * (if (broken) 0.7f else 0.35f)) * (if (broken) 220f else 140f)
        y += sin(age * 1.3f) * 6f * dt
    }

    /** 慢慢吐出一串大泡泡。 */
    private val bubbleStream = Attack(4.4f) { w, t, dt ->
        if (every(t, dt, 0.32f)) {
            val a = HALF_PI + sin(t * 1.4f) * 0.9f
            w.shoot(x, y + 60f, a, 120f, BulletStyle.BUBBLE, Tint.CYAN)
        }
    }
    /** 頭上的橘子一顆顆滾下來，左右彈跳。 */
    private val yuzuRoll = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.7f)) {
            val side = if ((t * 10).toInt() % 2 == 0) 1f else -1f
            w.bullet(x, y - 120f, side * (120f + w.random.nextFloat() * 80f), -160f, BulletStyle.BIG_ORB, Tint.ORANGE).ay = 260f
        }
    }
    /** 溫泉蒸氣一圈圈慢慢擴散。 */
    private val steamRings = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 1.1f)) w.ring(x, y + 30f, 18, 130f, t * 0.6f, BulletStyle.BUBBLE, Tint.CYAN)
    }
    /** 叫小鴨子排隊游過來。 */
    private val callDucks = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            for (i in 0 until 5) w.spawn(Duckling(120f + i * 190f, i * 0.15f))
        }
    }
    /** 裝甲後：橘子砲的高壓溫泉水柱，從一側掃到另一側。 */
    private val waterJet = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x
            melee(
                w, HazardStyle.WATER, if (playerLeft) HALF_PI - 50f * DEG else HALF_PI + 50f * DEG, 46f,
                warn = 0.9f, active = 1.8f, extend = 0.2f, sweep = if (playerLeft) 60f * DEG else -60f * DEG, length = 1700f,
            )
        }
        if (every(t, dt, 1f, offset = 1f)) w.ring(x, y + 30f, 12, 170f, t, BulletStyle.BUBBLE, Tint.CYAN)
    }
    /** 裝甲後：橘子飛彈連發，先往上飛再轉向玩家。 */
    private val yuzuMissiles = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.3f, offset = 0.2f)) {
            val side = if ((t * 10).toInt() % 2 == 0) 1f else -1f
            val b = w.shoot(x + handX, y + handY - 20f, -HALF_PI + side * 50f * DEG, 260f, BulletStyle.BIG_ORB, Tint.ORANGE, radius = 16f)
            b.turn = -side * 2.4f
        }
    }
    /** 裝甲後：水柱打叉，同時蒸氣爆開。 */
    private val jetCross = Attack(4.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            melee(w, HazardStyle.WATER, HALF_PI - 55f * DEG, 40f, warn = 1f, active = 1.6f, extend = 0.2f, sweep = 45f * DEG, length = 1600f)
            melee(w, HazardStyle.WATER, HALF_PI + 55f * DEG, 40f, warn = 1f, active = 1.6f, extend = 0.2f, sweep = -45f * DEG, length = 1600f, fromX = -handX)
        }
        if (crossed(t, dt, 2.8f)) w.ring(x, y + 30f, 24, 210f, 0f, BulletStyle.BUBBLE, Tint.CYAN)
    }

    override fun attacks(phase: Int) = when {
        !broken -> listOf(bubbleStream, yuzuRoll, steamRings, callDucks)
        phase < 2 -> listOf(waterJet, yuzuMissiles, steamRings, jetCross)
        else -> listOf(jetCross, yuzuMissiles, waterJet, callDucks, yuzuRoll)
    }
}
