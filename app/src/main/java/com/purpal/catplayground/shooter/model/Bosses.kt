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
abstract class Boss(hp: Float, radius: Float, score: Int, protected val homeY: Float) :
    Enemy(WORLD_WIDTH / 2f, -220f, hp, radius, score) {

    protected class Attack(val duration: Float, val step: (w: World, t: Float, dt: Float) -> Unit)

    abstract val name: String
    var maxHp = hp
        private set

    /** 依出場順序設定血量時乘上的倍率：有自帶「打得比較快的時機」（打瞌睡、雙胞胎…）的 Boss 會比較硬一點。 */
    open val hpFactor: Float get() = 1f

    /** 依出場順序重新設定血量（開打前呼叫）。 */
    fun setMaxHp(value: Float) {
        maxHp = value
        hp = value
    }
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

    /** 畫面用：本體傾斜的角度（度），例如企鵝肚子貼地滑行。 */
    open val tilt: Float get() = 0f

    /** 畫面用：潛到水裡看不見。 */
    open val hidden: Boolean get() = false
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

    /** 移動。用 [moveTime]（只在真的移動時才前進）而不是 age，暫停後才不會跳到別的位置。 */
    protected abstract fun move(w: World, dt: Float)

    /** 移動用的時鐘：掃擊時暫停，之後從原地接著走。 */
    protected var moveTime = 0f
        private set

    /** 每秒最多移動的距離；換招式或換移動方式時用滑的過去，不會瞬間移動。 */
    open val maxMoveSpeed: Float get() = 520f

    /** 在 (x, y) 的敵彈時間流速倍率（例如樹懶的慢動作力場），1 為正常。 */
    open fun bulletTimeScale(bx: Float, by: Float): Float = 1f

    /**
     * 第二次變身（最終 Boss 卸甲後再覺醒）：和裝甲一樣播 [ARMOR_TIME] 秒的動畫（打不到、不攻擊），
     * 攻擊從頭開始。
     */
    protected fun transform(w: World) {
        armorTime = ARMOR_TIME
        attackIndex = 0
        attackTime = 0f
        w.onBossTransform(this)
    }

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
        // 掃擊中（道具或雷射正在掃過去）原地站穩，掃的範圍才跟預告線一致、玩家看得出安全區
        if (w.lasers.none { it.owner === this && it.sweep != 0f && it.alive }) {
            val fromX = x
            val fromY = y
            moveTime += dt
            move(w, dt)
            val limit = maxMoveSpeed * dt
            x = fromX + (x - fromX).coerceIn(-limit, limit)
            y = fromY + (y - fromY).coerceIn(-limit, limit)
        }
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
        // 越後面的站出招越快（只影響招式本身的節奏；預告雷射的預告時間不變）
        val tdt = dt * w.difficulty.tempo
        attackTime += tdt
        attack.step(w, attackTime, tdt)
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
 * 最終關（瑞士）：萌王 鼠鼠。戴著起司帽和歪掉的紙王冠，拿著瑞士刀——刀刃突刺、剪刀交叉、開瓶器螺旋，
 * 還有招牌的「起司洞洞牆」：一整排起司從上面壓下來，只有洞的地方鑽得過去。
 * 60% 瑞士刀被打飛「卸甲」：在地上擺老鼠夾（靠近會先抖再夾下去）、讓起司輪沿著預告的跑道滾下來。
 * 30% 搬出起司火鍋「萌王覺醒」（第二次變身）：起司火鍋雨、火鍋冒泡炸開的起司圈，洞洞牆和老鼠夾一起來。
 */
class MouseBoss : Boss(hp = 46000f, radius = 180f, score = 300000, homeY = 410f) {
    override val kind = EnemyKind.BOSS_MOUSE
    override val name = "萌王 鼠鼠"
    override val breakAt = 0.6f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.SWISSKNIFE
    override val lostWeapon = LostProp.SWISSKNIFE
    override val lostProps = intArrayOf(LostProp.CROWN)
    override val handX = 156f
    override val handY = 138f

    /** 搬出起司火鍋（萌王覺醒）了。 */
    var awakened = false
        private set

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        if (broken && !awakened && hpRatio <= AWAKEN_AT) {
            awakened = true
            transform(w)
        }
    }

    override fun move(w: World, dt: Float) {
        x = when {
            // 守著起司火鍋，只在附近左右晃
            awakened -> WORLD_WIDTH / 2f + sin(moveTime * 0.5f) * 120f
            // 卸甲後慌張地快速左右跑
            broken -> WORLD_WIDTH / 2f + sin(moveTime * 1.2f) * 240f
            else -> WORLD_WIDTH / 2f + sin(moveTime * 0.6f) * 180f
        }
        y = approach(y, homeY, 3f, dt)
    }

    /** 起司洞洞牆：一整排起司從畫面上方壓下來，只有 holes 個洞鑽得過去。 */
    private fun cheeseWall(w: World, speed: Float, holes: Int) {
        val holeHalf = 90f
        val centers = ArrayList<Float>()
        var guard = 0
        while (centers.size < holes && guard++ < 50) {
            val c = 140f + w.random.nextFloat() * (WORLD_WIDTH - 280f)
            if (centers.all { kotlin.math.abs(it - c) > holeHalf * 3f }) centers += c
        }
        var bx = 26f
        while (bx < WORLD_WIDTH) {
            if (centers.none { kotlin.math.abs(it - bx) < holeHalf }) w.bullet(bx, -20f, 0f, speed, BulletStyle.CHEESE, Tint.CHEESE)
            bx += 52f
        }
        w.cue(SoundCue.SWING)
    }

    /** 瑞士刀的刀刃朝玩家突刺兩次，刀尖再射出三發能量針。 */
    private val knifeThrust = Attack(2.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.4f)) melee(w, HazardStyle.SWISSKNIFE, aimFrom(w, handX, handY), 44f, warn = 0.7f)
        if (crossed(t, dt, 0.9f) || crossed(t, dt, 2.3f)) {
            val a = aimFrom(w, handX, handY)
            for (k in -1..1) w.shoot(x + handX, y + handY, a + k * 9f * DEG, 420f, BulletStyle.RICE, Tint.PINK)
        }
    }
    /**
     * 剪刀交叉：兩片刀刃從左右兩邊往中間合起來，停在正下方兩側，
     * 中間那一條和外側是安全區。
     */
    private val scissorCross = Attack(3.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            melee(w, HazardStyle.SWISSKNIFE, 150f * DEG, 40f, warn = 0.9f, active = 1f, extend = 0.2f, sweep = -50f * DEG, length = 1300f, fromX = -110f, fromY = 110f)
            melee(w, HazardStyle.SWISSKNIFE, 30f * DEG, 40f, warn = 0.9f, active = 1f, extend = 0.2f, sweep = 50f * DEG, length = 1300f, fromX = 110f, fromY = 110f)
        }
        if (every(t, dt, 1.1f, offset = 2f)) w.ring(x, y + 40f, 14, 180f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    /** 開瓶器螺旋：刀尖快速旋轉，種子彈扭成一道道螺旋。 */
    private val corkscrew = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.08f)) {
            val a = t * 6f
            w.shoot(x + handX, y + handY, a, 230f, BulletStyle.SEED, Tint.SEED).turn = 0.6f
            w.shoot(x + handX, y + handY, a + PI_F, 230f, BulletStyle.SEED, Tint.SEED).turn = 0.6f
        }
    }
    /** 起司洞洞牆兩道。 */
    private val cheeseWalls = Attack(4.6f) { w, t, dt ->
        if (crossed(t, dt, 0.2f) || crossed(t, dt, 2.4f)) cheeseWall(w, 150f, 2)
        if (every(t, dt, 1.2f, offset = 0.8f)) w.aimed(x, y + 60f, 260f, BulletStyle.SEED, Tint.SEED)
    }
    /** 叫小老鼠親衛隊來幫忙。 */
    private val summon = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.3f) || crossed(t, dt, 1.4f)) {
            w.spawn(MiniMouse(x - 130f, y + 40f, -1f))
            w.spawn(MiniMouse(x + 130f, y + 40f, 1f))
        }
        if (every(t, dt, 1f)) w.aimed(x, y + 60f, 180f, BulletStyle.BIG_ORB, Tint.CHEESE)
    }
    /** 卸甲後：在地上擺三個老鼠夾（其中一個在玩家附近），靠近就會抖一抖再夾下去。 */
    private val mouseTraps = Attack(4f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            w.spawn(MouseTrap(w.player.x + w.jitter(80f), (w.player.y - 120f).coerceAtLeast(y + 320f)))
            repeat(2) {
                val top = y + 320f
                w.spawn(MouseTrap(120f + w.random.nextFloat() * (WORLD_WIDTH - 240f), top + w.random.nextFloat() * maxOf(60f, w.height * 0.75f - top)))
            }
            w.cue(SoundCue.SWING)
        }
        if (every(t, dt, 1f, offset = 1f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 50f * DEG, 280f, BulletStyle.SEED, Tint.SEED)
    }
    /** 卸甲後：起司輪雪崩，先亮出跑道，起司輪再一顆顆滾下來（兩波，跑道錯開）。 */
    private val cheeseAvalanche = Attack(4.6f) { w, t, dt ->
        if (crossed(t, dt, 0.2f) || crossed(t, dt, 2.2f)) {
            val lanes = (0 until RollingCheese.LANES).shuffled(w.random).take(RollingCheese.LANES - 3)
            for (lane in lanes) w.spawn(RollingCheese(RollingCheese.laneX(lane), w.random.nextFloat() * 0.3f))
        }
    }
    /** 卸甲後：起司洞洞牆變快、一次壓兩道。 */
    private val fastWalls = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.2f) || crossed(t, dt, 1.6f) || crossed(t, dt, 3f)) cheeseWall(w, 200f, 2)
    }
    /** 卸甲後：邊跑邊撒種子。 */
    private val panicSpray = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.16f)) w.shoot(x, y + 60f, HALF_PI + w.jitter(0.5f), 260f, BulletStyle.SEED, Tint.SEED)
    }
    /** 卸甲後：甩電漿尾巴，從玩家的另一側掃過來 70°，玩家那一側的外緣是安全區。 */
    private val tailWhip = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x
            melee(
                w, HazardStyle.TAIL, if (playerLeft) HALF_PI - 62f * DEG else HALF_PI + 62f * DEG, 34f,
                warn = 0.8f, active = 1.5f, extend = 0.25f, sweep = if (playerLeft) 70f * DEG else -70f * DEG,
                length = 1150f, fromX = 0f, fromY = 110f,
            )
        }
        if (every(t, dt, 1.2f, offset = 0.4f)) w.ring(x, y + 40f, 14, 180f, t, BulletStyle.CHEESE, Tint.CHEESE)
    }
    /** 萌王覺醒：起司火鍋雨，融化的起司從上面一滴滴落下，越落越快。 */
    private val fondueRain = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.13f)) w.bullet(40f + w.random.nextFloat() * (WORLD_WIDTH - 80f), -20f, 0f, 100f, BulletStyle.HONEY, Tint.CHEESE).ay = 280f
        if (every(t, dt, 1.3f, offset = 0.6f)) w.aimed(x, y + 80f, 300f, BulletStyle.BIG_ORB, Tint.CHEESE)
    }
    /** 萌王覺醒：火鍋咕嘟冒泡，一圈圈起司炸開，再補一道洞洞牆。 */
    private val fondueBubbles = Attack(4.4f) { w, t, dt ->
        if (every(t, dt, 0.7f)) w.ring(x, y + 160f, 16, 180f, t, BulletStyle.CHEESE, Tint.CHEESE)
        if (crossed(t, dt, 1.5f)) cheeseWall(w, 170f, 2)
    }
    /** 萌王覺醒：老鼠夾和洞洞牆一起來。 */
    private val trapsAndWall = Attack(4.4f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            w.spawn(MouseTrap(w.player.x + w.jitter(80f), (w.player.y - 120f).coerceAtLeast(y + 320f)))
            w.spawn(MouseTrap(120f + w.random.nextFloat() * (WORLD_WIDTH - 240f), y + 420f + w.random.nextFloat() * 300f))
        }
        if (crossed(t, dt, 1.2f)) cheeseWall(w, 170f, 2)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(knifeThrust, cheeseWalls, corkscrew, summon, scissorCross)
        !broken -> listOf(scissorCross, cheeseWalls, corkscrew, knifeThrust)
        !awakened -> listOf(mouseTraps, cheeseAvalanche, fastWalls, tailWhip, panicSpray)
        else -> listOf(fondueRain, cheeseAvalanche, fondueBubbles, trapsAndWall)
    }

    companion object {
        const val AWAKEN_AT = 0.3f
        private const val PI_F = HALF_PI * 2f
    }
}

/** 萌王的老鼠夾：貓咪靠近（或擺好一段時間）就會抖一抖、預告，再「啪」地夾下去。打不到。 */
class MouseTrap(x: Float, y: Float) : Enemy(x, y, 1f, 70f, 0) {
    override val kind = EnemyKind.MOUSETRAP
    override val invulnerable = true
    override val contactDamage = 0f
    /** 被觸發後的秒數（-1 表示還沒觸發）。 */
    var triggered = -1f
        private set
    val snapped: Boolean get() = triggered >= SNAP_DELAY

    override fun update(w: World, dt: Float) {
        age += dt
        if (triggered < 0f && (age > AUTO_TRIGGER || dist2(x, y, w.player.x, w.player.y) < TRIGGER * TRIGGER)) {
            triggered = 0f
            w.cue(SoundCue.WARNING)
        }
        if (triggered >= 0f) {
            triggered += dt
            if (crossed(triggered, dt, SNAP_DELAY)) {
                w.shake(0.2f)
                w.cue(SoundCue.SWING)
                w.burst(x, y, 0xFFFFD54FL, 8, 220f, ParticleKind.STAR)
                if (dist2(x, y, w.player.x, w.player.y) < SNAP * SNAP) w.damagePlayer(SNAP_DAMAGE)
            }
            if (triggered > SNAP_DELAY + 0.8f) alive = false
        }
    }

    companion object {
        const val TRIGGER = 140f
        const val SNAP = 100f
        const val SNAP_DELAY = 0.7f
        const val SNAP_DAMAGE = 26f
        const val AUTO_TRIGGER = 4.5f
    }
}

/** 起司輪雪崩的起司輪：先亮出整條跑道（預告），再從畫面上方高速滾下來。打不到，碰到會痛。 */
class RollingCheese(x: Float, private val delay: Float) : Enemy(x, -80f, 1f, 54f, 0) {
    override val kind = EnemyKind.ROLLING_CHEESE
    override val invulnerable = true
    override val hurtsOnContact: Boolean get() = rolling
    override val contactDamage = 26f
    val rolling: Boolean get() = age > WARN + delay
    /** 滾動的角度（度，畫面用）。 */
    val spin: Float get() = y * 0.8f

    override fun update(w: World, dt: Float) {
        age += dt
        if (rolling) y += SPEED * w.difficulty.bulletSpeed * dt
        if (y > w.height + 120f) alive = false
    }

    companion object {
        const val WARN = 0.9f
        const val SPEED = 700f
        const val LANES = 8
        fun laneX(lane: Int) = WORLD_WIDTH / LANES * (lane + 0.5f)
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
    override val maxMoveSpeed = 1100f
    /** 骨頭丟出去還沒回來。 */
    var boneOut = false

    override fun heldProp(): HazardStyle? = if (boneOut) null else super.heldProp()

    override fun move(w: World, dt: Float) {
        if (!dashing) targetX = WORLD_WIDTH / 2f + sin(moveTime * 0.8f) * 220f
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
    /**
     * 卸甲後：雙眼光束。從窄窄的 V 字往兩側張開，柴犬正下方那條通道一直是安全區
     * （光束掃到哪裡，預告線就畫到哪裡）。
     */
    private val eyeBeams = Attack(4.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            for (side in listOf(-1f, 1f)) {
                w.lasers += Laser(
                    x + side * 40f, y + 20f, HALF_PI - side * 14f * DEG, length = 2400f, width = 26f, warnTime = 1f,
                    activeTime = 2.8f, color = Tint.RED, damage = BEAM_DAMAGE, sweep = -side * 12f * DEG,
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
        x = WORLD_WIDTH / 2f + sin(moveTime * 0.45f) * 150f
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
    /**
     * 抓著蜂蜜鑽原地轉一大圈（312°），熱熔的鑽頭掃過畫面；
     * 正下方留一個 48° 的缺口，鑽頭也搆不到畫面最下方，兩處都是安全區。
     */
    private val dipperSpin = Attack(4.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            melee(w, HazardStyle.DIPPER, HALF_PI + 24f * DEG, 50f, warn = 1f, active = 2.6f, extend = 0.3f, sweep = 120f * DEG, length = 700f, fromX = 0f, fromY = 40f)
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
        x = WORLD_WIDTH / 2f + sin(moveTime * (if (broken) 0.7f else 0.35f)) * (if (broken) 220f else 140f)
        y += sin(moveTime * 1.3f) * 6f * dt
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

/**
 * 第 5 關：兔兔隊長 蘿蔔（兔子）。在胡蘿蔔田裡蹦來蹦去，落地就炸開一圈星星；
 * 看起來膽小溫和，但「兔子急了也會咬人」——打到 40% 才「裝甲」：戴上護目鏡、裝上肩甲，
 * 拿出胡蘿蔔鑽頭——突刺、橫掃，再加上會轉彎的胡蘿蔔飛彈。
 */
class RabbitBoss : Boss(hp = 36000f, radius = 180f, score = 60000, homeY = 430f) {
    override val kind = EnemyKind.BOSS_RABBIT
    override val name = "兔兔隊長 蘿蔔"
    override val breakAt = 0.4f
    override val breakKind = BreakKind.ARMOR
    override val weapon = HazardStyle.CARROT
    override val lostProps = intArrayOf(LostProp.RIBBON)
    override val handX = 130f
    override val handY = 153f
    private var baseY = Float.NaN
    override val maxMoveSpeed = 1500f
    private var hopFromX = WORLD_WIDTH / 2f
    private var hopToX = WORLD_WIDTH / 2f
    private var hopT = 1f
    private var hopTimer = 0f

    /** 得意（裝甲前）→ 裝甲暴怒 → 裝甲被打哭。 */
    override fun mood(): Int = when {
        !broken -> 0
        hpRatio > 0.15f -> 1
        else -> 2
    }

    override fun move(w: World, dt: Float) {
        if (baseY.isNaN()) baseY = y
        // 蹦蹦跳：每隔一段時間跳到新的位置，落地時炸開一圈星星（裝甲後跳得更勤）
        hopTimer += dt
        val interval = if (broken) 1.6f else 2.4f
        if (hopT >= 1f && hopTimer >= interval) {
            hopTimer = 0f
            hopFromX = x
            hopToX = 240f + w.random.nextFloat() * (WORLD_WIDTH - 480f)
            hopT = 0f
        }
        if (hopT < 1f) {
            hopT = (hopT + dt / 0.6f).coerceAtMost(1f)
            x = hopFromX + (hopToX - hopFromX) * hopT
            y = baseY - sin(hopT * HALF_PI * 2f) * 90f
            if (hopT >= 1f) {
                y = baseY
                w.shake(0.2f)
                w.ring(x, y + 120f, 10, 160f, w.random.nextFloat(), BulletStyle.STAR, Tint.STAR)
            }
        }
    }

    /** 胡蘿蔔雨：從畫面上方隨機落下。 */
    private val carrotRain = Attack(4.2f) { w, t, dt ->
        if (every(t, dt, 0.22f)) {
            val cx = 40f + w.random.nextFloat() * (WORLD_WIDTH - 80f)
            w.bullet(cx, -20f, 0f, 230f + w.random.nextFloat() * 90f, BulletStyle.RICE, Tint.ORANGE)
        }
    }
    /** 月亮兔的星星環：一圈圈旋轉擴散。 */
    private val moonRings = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.9f)) w.ring(x, y + 40f, 16, 180f, t * 0.8f, BulletStyle.STAR, Tint.STAR)
    }
    /** 胡蘿蔔扇形連射。 */
    private val carrotFans = Attack(3.4f) { w, t, dt ->
        if (every(t, dt, 0.65f)) w.fan(x, y + 80f, w.aimAt(x, y), 5, 46f * DEG, 300f, BulletStyle.RICE, Tint.ORANGE)
    }
    /** 叫跳跳小兔來幫忙。 */
    private val callBunnies = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(Bunny(130f + i * 150f, i * 0.1f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 200f, BulletStyle.BIG_ORB, Tint.ORANGE)
    }
    /** 裝甲後：胡蘿蔔鑽頭朝玩家突刺兩次。 */
    private val drillThrust = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.3f)) melee(w, HazardStyle.CARROT, aimFrom(w, handX, handY), 48f, warn = 0.7f)
    }
    /** 裝甲後：鑽頭從玩家的另一側橫掃 70°，玩家那一側的外緣是安全區。 */
    private val drillSweep = Attack(3.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.CARROT, if (playerLeft) HALF_PI - 55f * DEG else HALF_PI + 55f * DEG, 46f,
                warn = 0.9f, active = 1.4f, extend = 0.2f, sweep = if (playerLeft) 70f * DEG else -70f * DEG, length = 1150f,
            )
        }
        if (every(t, dt, 1.1f, offset = 1f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.STAR, Tint.STAR)
    }
    /** 裝甲後：會轉彎的胡蘿蔔飛彈，左右輪流射出。 */
    private val carrotMissiles = Attack(3.4f) { w, t, dt ->
        if (every(t, dt, 0.28f, offset = 0.2f)) {
            val side = if ((t * 10).toInt() % 2 == 0) 1f else -1f
            val b = w.shoot(x + side * handX, y + handY - 20f, -HALF_PI + side * 50f * DEG, 270f, BulletStyle.RICE, Tint.ORANGE)
            b.turn = -side * 2.4f
        }
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(carrotRain, carrotFans, moonRings, callBunnies)
        !broken -> listOf(moonRings, carrotRain, callBunnies, carrotFans)
        phase < 2 -> listOf(drillThrust, carrotMissiles, drillSweep, moonRings)
        else -> listOf(drillSweep, carrotMissiles, drillThrust, carrotRain, callBunnies)
    }
}

/**
 * 第 6 關：企鵝艦長 冰冰（企鵝）。拿著急凍光線槍：冰晶彈打出去會在半空中「急凍」停住一下再繼續飛，
 * 還有預告後落下的冰柱雨、留安全區的急凍光線掃射。企鵝認真又愛面子，55% 就被打到「卸甲」：
 * 槍飛走之後改成肚子貼地滑行衝過畫面（沿路留下冰晶）、丟會轉彎的小魚。
 */
class PenguinBoss : Boss(hp = 34000f, radius = 190f, score = 70000, homeY = 430f) {
    override val kind = EnemyKind.BOSS_PENGUIN
    override val name = "企鵝艦長 冰冰"
    override val breakAt = 0.55f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.CRYO
    override val lostWeapon = LostProp.CRYO
    override val lostProps = intArrayOf(LostProp.BEANIE)
    override val handX = 183f
    override val handY = 120f
    /** 肚子貼地滑行的目的地（NaN 表示沒在滑）。 */
    private var slideTo = Float.NaN
    private var slideDir = 0f
    private var slideStep = false
    /** 這一步移動用的速度上限（在 move 裡決定，滑行結束的那一步也照滑行的速度算）。 */
    private var speed = 520f
    override val maxMoveSpeed: Float get() = speed
    override val tilt: Float get() = slideDir * 78f

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        // 滑行被換階段打斷時也要站起來
        if (!slideStep) {
            slideTo = Float.NaN
            slideDir = 0f
        }
        slideStep = false
    }

    override fun move(w: World, dt: Float) {
        speed = if (slideTo.isNaN()) 520f else 900f
        if (!slideTo.isNaN()) {
            // 速度由 maxMoveSpeed 限制，直接把目的地交給它，就會等速滑過去
            x = slideTo
            y = homeY + 20f
        } else {
            x = WORLD_WIDTH / 2f + sin(moveTime * 0.5f) * 170f
            y = approach(y, homeY, 3f, dt)
        }
    }

    private fun freeze(b: EnemyBullet, at: Float, time: Float = 0.9f) {
        b.freezeAt = at
        b.freezeTime = time
    }

    /** 雪球扇：每隔一扇就有一扇在半空中急凍停住，快慢錯開。 */
    private val snowballFans = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.7f)) {
            val frozen = (t / 0.7f).toInt() % 2 == 0
            for (k in -2..2) {
                val b = w.shoot(x, y + 80f, w.aimAt(x, y) + k * 13f * DEG * 1.2f, 300f, BulletStyle.ORB, Tint.CYAN)
                if (frozen) freeze(b, 0.45f)
            }
        }
    }
    /** 冰柱雨：冰柱先掛在畫面頂端閃一下（預告），再加速落下。 */
    private val icicleRain = Attack(4.2f) { w, t, dt ->
        if (every(t, dt, 0.2f)) {
            val ix = 40f + w.random.nextFloat() * (WORLD_WIDTH - 80f)
            val b = w.bullet(ix, 24f, 0f, 220f, BulletStyle.ICE, Tint.CYAN)
            b.ay = 260f
            freeze(b, 0f, 0.8f)
        }
    }
    /** 急凍光線槍朝玩家射兩次（短促的光線）。 */
    private val cryoShot = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.3f)) melee(w, HazardStyle.CRYO, aimFrom(w, handX, handY), 40f, warn = 0.7f, active = 0.5f)
        if (every(t, dt, 1.3f, offset = 0.7f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.ICE, Tint.CYAN)
    }
    /** 急凍光線從玩家的另一側掃過來 60°，玩家那一側的外緣是安全區；掃完留下一圈急凍冰晶。 */
    private val cryoSweep = Attack(4.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.CRYO, if (playerLeft) HALF_PI - 50f * DEG else HALF_PI + 50f * DEG, 44f,
                warn = 0.9f, active = 1.8f, extend = 0.2f, sweep = if (playerLeft) 60f * DEG else -60f * DEG, length = 1700f,
            )
        }
        if (crossed(t, dt, 3.2f)) {
            for (k in 0 until 16) freeze(w.shoot(x, y + 40f, k * TWO_PI / 16f, 200f, BulletStyle.ICE, Tint.CYAN), 0.5f, 0.8f)
        }
    }
    /** 叫企鵝小隊滑下來。 */
    private val squad = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(PenguinSlider(130f + i * 148f, (i % 3) * 0.12f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 220f, BulletStyle.BIG_ORB, Tint.CYAN)
    }
    /** 卸甲後：肚子貼地滑過整個畫面再滑回來，沿路留下一排冰晶（先凍住，再依序落下）。 */
    private val bellySlide = Attack(3.8f) { w, t, dt ->
        slideStep = t < 3f
        if (crossed(t, dt, 0.01f)) {
            slideTo = if (x < WORLD_WIDTH / 2f) WORLD_WIDTH - 200f else 200f
            slideDir = if (slideTo > x) 1f else -1f
        }
        if (crossed(t, dt, 1.6f)) {
            slideTo = if (slideDir > 0f) 200f else WORLD_WIDTH - 200f
            slideDir = -slideDir
        }
        if (t > 0.1f && t < 3f && every(t, dt, 0.07f)) {
            val b = w.bullet(x, y + 60f, 0f, 260f, BulletStyle.ICE, Tint.CYAN)
            freeze(b, 0f, 1.1f - (t % 1.6f) * 0.3f)
        }
        if (t >= 3f) {
            slideTo = Float.NaN
            slideDir = 0f
        }
    }
    /** 卸甲後：從兩邊翅膀丟出會轉彎的小魚。 */
    private val fishCurve = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.35f)) {
            val side = if ((t / 0.35f).toInt() % 2 == 0) 1f else -1f
            val b = w.shoot(x + side * 150f, y + 60f, HALF_PI - side * 55f * DEG, 270f, BulletStyle.FISH, Tint.CYAN)
            b.turn = side * 1.5f
        }
    }
    /** 卸甲後：一圈圈冰晶，擴散到一半凍住，再一起散開。 */
    private val frostRings = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.9f)) {
            for (k in 0 until 18) freeze(w.shoot(x, y + 40f, t + k * TWO_PI / 18f, 210f, BulletStyle.ICE, Tint.CYAN), 0.6f, 0.7f)
        }
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(snowballFans, cryoShot, icicleRain, squad)
        !broken -> listOf(cryoSweep, snowballFans, icicleRain, cryoShot)
        phase < 2 -> listOf(bellySlide, fishCurve, frostRings, squad)
        else -> listOf(bellySlide, fishCurve, icicleRain, frostRings, squad)
    }
}

/**
 * 第 7 關：無尾熊長老 尤加（無尾熊）。一天睡 20 小時——掛在樹枝上盪來盪去，打著瞌睡也照樣攻擊：
 * 慢吞吞的 Zzz 泡泡、掉下來的尤加利果實、葉片螺旋。睡著時防禦變低（受到兩倍傷害），
 * 但鼻涕泡泡會越吹越大，沒打破就炸成一圈子彈。打到 30% 才「被吵醒」而「裝甲」：
 * 拿出尤加利葉刃發射器——葉刃迴力鏢、葉片風暴、預告後俯衝重壓。
 */
class KoalaBoss : Boss(hp = 44000f, radius = 200f, score = 80000, homeY = 420f) {
    override val kind = EnemyKind.BOSS_KOALA
    override val name = "無尾熊長老 尤加"
    // 打瞌睡時受到兩倍傷害
    override val hpFactor = 1.2f
    override val breakAt = 0.3f
    override val breakKind = BreakKind.ARMOR
    override val weapon = HazardStyle.LEAF
    override val lostProps = intArrayOf(LostProp.NIGHTCAP)
    override val handX = 162f
    override val handY = 150f
    /** 打瞌睡中：受到兩倍傷害。 */
    var napping = false
        private set
    private var napStep = false
    /** 俯衝重壓：目標位置（NaN 表示沒在俯衝）與預告剩餘秒數（畫面用）。 */
    private var diveY = Float.NaN
    var diveX = Float.NaN
        private set
    var diveWarn = 0f
        private set
    private var landed = false
    private var diveStep = false
    /** 這一步移動用的速度上限（在 move 裡決定）。 */
    private var speed = 600f
    override val maxMoveSpeed: Float get() = speed
    override val damageScale: Float get() = if (napping) 2f else 1f

    /** 打瞌睡（裝甲前）→ 被吵醒暴怒 → 裝甲被打哭。 */
    override fun mood(): Int = when {
        !broken -> 0
        hpRatio > 0.12f -> 1
        else -> 2
    }

    /** 盪鞦韆的樹枝掛在畫面上方的這個點（畫面用來畫樹枝）。 */
    val pivotX = WORLD_WIDTH / 2f
    val pivotY = -420f
    val swinging: Boolean get() = !broken

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        napping = napStep && !broken
        napStep = false
        // 俯衝被換階段打斷時直接飛回原位
        if (!diveStep) diveY = Float.NaN
        diveStep = false
        if (diveWarn > 0f) diveWarn -= dt
    }

    override fun move(w: World, dt: Float) {
        speed = if (diveY.isNaN()) 600f else 2000f
        when {
            !diveY.isNaN() -> {
                x = diveX
                y = diveY
            }
            !broken -> {
                // 掛在樹枝上盪鞦韆
                val length = homeY - pivotY
                val a = sin(moveTime * 0.9f) * 0.3f
                x = pivotX + sin(a) * length
                y = pivotY + cos(a) * length
            }
            else -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * 0.7f) * 220f
                y = homeY + sin(moveTime * 1.6f) * 20f
            }
        }
    }

    /** 盪鞦韆時灑出兩道葉片螺旋。 */
    private val leafSpiral = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.11f)) {
            val a = t * 2.2f
            w.shoot(x, y + 60f, a, 200f, BulletStyle.LEAF, Tint.GREEN)
            w.shoot(x, y + 60f, a + HALF_PI * 2f, 200f, BulletStyle.LEAF, Tint.GREEN)
        }
    }
    /** 夢話 Zzz：一串慢吞吞的大泡泡。 */
    private val zzzBubbles = Attack(3.8f) { w, t, dt ->
        if (every(t, dt, 0.3f)) w.shoot(x, y + 80f, HALF_PI + sin(t * 1.7f) * 1f, 110f, BulletStyle.BUBBLE, Tint.CYAN)
        if (every(t, dt, 1.2f, offset = 0.6f)) w.fan(x, y + 80f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.LEAF, Tint.GREEN)
    }
    /** 尤加利果實從樹上掉下來，越掉越快。 */
    private val nutFall = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.26f)) {
            val nx = 40f + w.random.nextFloat() * (WORLD_WIDTH - 80f)
            w.bullet(nx, -20f, 0f, 70f, BulletStyle.BIG_ORB, Tint.GREEN).ay = 320f
        }
    }
    /** 睡著了：防禦變低，鼻子吹出一個鼻涕泡泡（沒打破就炸開）。 */
    private val nap = Attack(4.2f) { w, t, dt ->
        napStep = t < 3.8f
        if (crossed(t, dt, 0.4f)) w.spawn(SnotBubble(this))
        if (every(t, dt, 0.8f, offset = 0.8f)) w.shoot(x, y + 80f, HALF_PI + w.jitter(0.8f), 100f, BulletStyle.BUBBLE, Tint.CYAN)
    }
    /** 裝甲後：葉刃迴力鏢，左右兩邊彎回來。 */
    private val leafBoomerang = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.5f)) {
            for (side in listOf(-1f, 1f)) {
                val b = w.shoot(x + handX, y + handY, HALF_PI + side * 50f * DEG, 320f, BulletStyle.LEAF, Tint.GREEN)
                b.turn = -side * 1.9f
            }
        }
    }
    /** 裝甲後：葉片風暴，旋轉的葉子快慢交錯捲出來。 */
    private val leafStorm = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.07f)) {
            val a = t * 3.1f + w.jitter(0.2f)
            w.shoot(x, y + 40f, a, 180f + (t * 10f).toInt() % 3 * 60f, BulletStyle.LEAF, Tint.GREEN)
        }
    }
    /**
     * 裝甲後：先在玩家那一行畫出預告，再整隻俯衝壓下來，落地震出一圈葉片。
     * 壓到玩家剛才位置的上方一點就停（身體中心不會越過玩家，被撞到的話是往下推開）。
     */
    private val diveSlam = Attack(4.2f) { w, t, dt ->
        diveStep = true
        if (crossed(t, dt, 0.01f)) {
            diveX = w.player.x.coerceIn(220f, WORLD_WIDTH - 220f)
            diveWarn = 1.1f
            landed = false
            w.cue(SoundCue.SWING)
        }
        if (crossed(t, dt, 1.1f)) diveY = (w.player.y - 40f).coerceIn(homeY + 150f, w.height * 0.62f)
        if (!diveY.isNaN() && !landed && t > 1.1f && y > diveY - 5f) {
            landed = true
            w.shake(0.5f)
            w.cue(SoundCue.EXPLODE_SMALL)
            w.ring(x, y + 100f, 16, 200f, t, BulletStyle.LEAF, Tint.GREEN)
        }
        if (crossed(t, dt, 2.2f)) diveY = homeY
        if (t > 2.2f && !diveY.isNaN() && y < homeY + 5f) {
            diveY = Float.NaN
            diveX = Float.NaN
        }
    }
    /** 裝甲後：葉刃從玩家的另一側橫掃 70°，玩家那一側的外緣是安全區。 */
    private val bladeSweep = Attack(3.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.LEAF, if (playerLeft) HALF_PI - 55f * DEG else HALF_PI + 55f * DEG, 46f,
                warn = 0.9f, active = 1.4f, extend = 0.2f, sweep = if (playerLeft) 70f * DEG else -70f * DEG, length = 1150f,
            )
        }
        if (every(t, dt, 1.1f, offset = 1f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.LEAF, Tint.GREEN)
    }
    /** 裝甲後：葉刃朝玩家突刺兩次。 */
    private val bladeThrust = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.3f)) melee(w, HazardStyle.LEAF, aimFrom(w, handX, handY), 46f, warn = 0.7f)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(leafSpiral, zzzBubbles, nap, nutFall)
        !broken -> listOf(nap, leafSpiral, nutFall, zzzBubbles)
        phase < 2 -> listOf(leafBoomerang, diveSlam, bladeSweep, leafStorm)
        else -> listOf(diveSlam, leafStorm, leafBoomerang, bladeSweep, bladeThrust)
    }
}

/** 無尾熊打瞌睡時吹出的鼻涕泡泡：越吹越大，2.4 秒後炸成一圈子彈；先打破就沒事。 */
class SnotBubble(private val koala: KoalaBoss) : Enemy(koala.x, koala.y + 90f, 60f, 46f, 500) {
    override val kind = EnemyKind.SNOT_BUBBLE
    override val contactDamage = 0f
    /** 吹大的程度（畫面用）：0～1。 */
    val grow: Float get() = (age / LIFE).coerceIn(0f, 1f)

    override fun update(w: World, dt: Float) {
        age += dt
        x = koala.x
        y = koala.y + 90f + grow * 30f
        if (!koala.alive || koala.broken) {
            alive = false
            w.puff(x, y, Tint.CYAN)
            return
        }
        if (age >= LIFE) {
            alive = false
            w.puff(x, y, Tint.CYAN)
            w.ring(x, y, 18, 190f, age, BulletStyle.BUBBLE, Tint.CYAN)
            w.ring(x, y, 9, 300f, age + 0.17f, BulletStyle.ORB, Tint.CYAN)
            w.cue(SoundCue.EXPLODE_SMALL)
        }
    }

    companion object {
        const val LIFE = 2.4f
    }
}

/**
 * 第 8 關：水獺姊妹 啾啾＆嚕嚕。兩隻一起上、共用一條血量（這個物件是姊姊啾啾，妹妹嚕嚕是 [OtterTwin]）。
 * 拿著重力石錘：石頭雜耍、貝殼震波、兩把石錘交叉敲下來。45% 被打到「卸甲」：
 * 石錘飛走，改成手牽手旋轉灑出雙螺旋、丟貝殼迴力鏢、潛到水裡再從預告的水花處冒出來。
 */
class OtterBoss : Boss(hp = 40000f, radius = 140f, score = 90000, homeY = 400f) {
    override val kind = EnemyKind.BOSS_OTTER
    override val name = "水獺姊妹 啾啾＆嚕嚕"
    // 兩隻一起上，打得到的地方比較多
    override val hpFactor = 1.1f
    override val breakAt = 0.45f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.HAMMER
    override val lostWeapon = LostProp.HAMMER
    override val lostProps = intArrayOf(LostProp.SHELL)
    override val handX = 112f
    override val handY = 104f
    override val maxMoveSpeed = 900f
    var twin: OtterTwin? = null
        private set
    /** 手牽手旋轉中。 */
    var spinning = false
        private set
    private var spinStep = false
    private var spinAngle = 0f
    /** 潛到水裡（看不見也打不到），會從 surface 的位置冒出來。 */
    var submerged = false
        private set
    private var diveStep = false
    var surfaceX = 0f
        private set
    var surfaceY = 0f
        private set
    var twinSurfaceX = 0f
        private set
    var twinSurfaceY = 0f
        private set
    private var centerX = WORLD_WIDTH / 2f
    private var sep = 230f

    override val invulnerable: Boolean get() = super.invulnerable || submerged
    override val solid: Boolean get() = !submerged
    override val hidden: Boolean get() = submerged

    init {
        x = WORLD_WIDTH / 2f - sep
    }

    override fun update(w: World, dt: Float) {
        if (twin == null) twin = OtterTwin(this).also { w.spawn(it) }
        super.update(w, dt)
        spinning = spinStep
        spinStep = false
        if (submerged && !diveStep) surface(w)
        diveStep = false
        twin?.let { tw ->
            when {
                submerged -> { tw.targetX = twinSurfaceX; tw.targetY = twinSurfaceY }
                spinning -> { tw.targetX = 2f * centerX - x; tw.targetY = 2f * homeY - y }
                else -> { tw.targetX = centerX + sep; tw.targetY = y }
            }
        }
    }

    override fun move(w: World, dt: Float) {
        when {
            submerged -> {
                x = surfaceX
                y = surfaceY
            }
            spinning -> {
                spinAngle += 2.6f * dt
                x = centerX + cos(spinAngle) * 150f
                y = homeY + sin(spinAngle) * 60f
            }
            else -> {
                centerX = WORLD_WIDTH / 2f + sin(moveTime * 0.5f) * 60f
                sep = 230f + sin(moveTime * 0.9f) * 40f
                x = centerX - sep
                y = homeY + sin(moveTime * 2f) * 10f
            }
        }
    }

    /** 兩姊妹各做一次：(x, y, 手的 x 偏移, 1=姊姊／-1=妹妹)。 */
    private inline fun both(action: (Float, Float, Float, Float) -> Unit) {
        action(x, y, handX, 1f)
        twin?.let { action(it.x, it.y, it.handX, -1f) }
    }

    /** 妹妹的石錘攻擊（判定跟著妹妹移動）。 */
    private fun twinMelee(w: World, angle: Float, warn: Float, active: Float, extend: Float) {
        val t = twin ?: return
        w.lasers += Laser(
            t.x + t.handX, t.y + t.handY, angle, length = (kotlin.math.sqrt(dist2(t.x + t.handX, t.y + t.handY, w.player.x, w.player.y)) + 140f).coerceIn(520f, 1500f),
            width = 50f, warnTime = warn, activeTime = active, color = Tint.RED, damage = MELEE_DAMAGE,
            owner = t, offsetX = t.handX, offsetY = t.handY, style = HazardStyle.HAMMER, extendTime = extend,
        )
    }

    private fun surface(w: World) {
        submerged = false
        w.shake(0.4f)
        w.cue(SoundCue.EXPLODE_SMALL)
        w.ring(surfaceX, surfaceY, 16, 190f, 0f, BulletStyle.BUBBLE, Tint.CYAN)
        w.ring(twinSurfaceX, twinSurfaceY, 16, 190f, 0.2f, BulletStyle.BUBBLE, Tint.CYAN)
        w.burst(surfaceX, surfaceY, 0xFFBFF8FFL, 14, 360f)
        w.burst(twinSurfaceX, twinSurfaceY, 0xFFBFF8FFL, 14, 360f)
    }

    /** 石頭雜耍：兩姊妹輪流把石頭往上拋，石頭劃著拋物線掉下來。 */
    private val juggle = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.32f)) {
            val k = (t / 0.32f).toInt()
            val tw = twin
            val fromTwin = k % 2 == 1 && tw != null
            val sx = if (fromTwin) tw!!.x else x
            val sy = if (fromTwin) tw!!.y else y
            val dx = (w.player.x - sx) / 2.2f + w.jitter(160f)
            w.bullet(sx, sy - 40f, dx, -380f, BulletStyle.BIG_ORB, Tint.PURPLE).ay = 420f
        }
    }
    /** 用石錘敲貝殼：兩姊妹交替放出一圈貝殼震波。 */
    private val shellRings = Attack(3.8f) { w, t, dt ->
        if (every(t, dt, 1.1f)) w.ring(x, y + 30f, 14, 180f, t, BulletStyle.SHELL, Tint.PINK)
        if (every(t, dt, 1.1f, offset = 0.55f)) twin?.let { w.ring(it.x, it.y + 30f, 14, 180f, t + 0.1f, BulletStyle.SHELL, Tint.PINK) }
    }
    /** 兩把重力石錘從左右交叉敲向玩家，敲到的地方炸開一圈碎石。 */
    private val hammerCross = Attack(3.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.7f)) {
            melee(w, HazardStyle.HAMMER, aimFrom(w, handX, handY), 50f, warn = 0.85f, active = 0.6f, extend = 0.1f)
            twin?.let { tw -> twinMelee(w, angleTo(tw.x + tw.handX, tw.y + tw.handY, w.player.x, w.player.y), 0.85f, 0.6f, 0.1f) }
        }
    }
    /** 叫跳躍魚來幫忙。 */
    private val callFish = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(LeapFish(130f + i * 148f, i * 0.1f))
        if (every(t, dt, 1f, offset = 0.8f)) both { bx, by, _, _ -> w.aimed(bx, by + 60f, 200f, BulletStyle.BUBBLE, Tint.CYAN) }
    }
    /** 卸甲後：手牽手轉圈，兩人各灑出一道螺旋（雙螺旋）。 */
    private val spinDance = Attack(4.6f) { w, t, dt ->
        spinStep = t < 4.4f
        if (every(t, dt, 0.1f)) {
            val a = t * 2.4f
            w.shoot(x, y + 30f, a, 200f, BulletStyle.SHELL, Tint.PINK)
            twin?.let { w.shoot(it.x, it.y + 30f, a + HALF_PI * 2f, 200f, BulletStyle.SHELL, Tint.PINK) }
        }
    }
    /** 卸甲後：兩人往外丟貝殼迴力鏢，劃一個大弧再彎回來。 */
    private val shellBoomerang = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.6f)) {
            both { bx, by, _, side ->
                val b = w.shoot(bx, by + 50f, HALF_PI - side * 55f * DEG, 280f, BulletStyle.SHELL, Tint.PINK)
                b.turn = side * 1.6f
            }
        }
        if (every(t, dt, 1.2f, offset = 0.9f)) w.aimed(x, y + 60f, 260f, BulletStyle.BIG_ORB, Tint.PURPLE)
    }
    /** 卸甲後：一起潛到水裡（打不到），水面上先冒出漣漪預告，再從那裡跳出來濺起一圈水花。 */
    private val dive = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            w.puff(x, y, Tint.CYAN)
            twin?.let { w.puff(it.x, it.y, Tint.CYAN) }
            val left = 200f + w.random.nextFloat() * 260f
            val right = WORLD_WIDTH - 200f - w.random.nextFloat() * 260f
            val swap = w.random.nextBoolean()
            surfaceX = if (swap) right else left
            twinSurfaceX = if (swap) left else right
            surfaceY = homeY - 40f + w.random.nextFloat() * 160f
            twinSurfaceY = homeY - 40f + w.random.nextFloat() * 160f
            submerged = true
        }
        diveStep = t > 0.3f && t < 1.9f
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(juggle, shellRings, hammerCross, callFish)
        !broken -> listOf(hammerCross, juggle, callFish, shellRings)
        phase < 2 -> listOf(spinDance, shellBoomerang, dive, shellRings)
        else -> listOf(dive, spinDance, shellBoomerang, callFish, juggle)
    }
}

/** 水獺妹妹嚕嚕：位置由姊姊決定，被打到的傷害算在姊姊的血量上；身體一樣是實心的。 */
class OtterTwin(val boss: OtterBoss) : Enemy(WORLD_WIDTH - boss.x, boss.y, 1f, 140f, 0) {
    override val kind = EnemyKind.OTTER_TWIN
    override val damageTarget: Enemy get() = boss
    override val invulnerable: Boolean get() = boss.invulnerable
    override val solid: Boolean get() = !boss.submerged
    override val contactDamage = 32f
    /** 石錘拿在左手（圖是姊姊的左右相反）。 */
    val handX = -112f
    val handY = 104f
    var targetX = x
    var targetY = y

    override fun update(w: World, dt: Float) {
        age += dt
        val limit = 1400f * dt
        x += (targetX - x).coerceIn(-limit, limit)
        y += (targetY - y).coerceIn(-limit, limit)
        if (!boss.alive) alive = false
    }
}

/**
 * 第 9 關：竹林武館 熊貓師父 波波（熊貓）。下巴黏著假鬍子、拿著電漿竹節棍，裝成威嚴的功夫師父：
 * 棍法橫掃、竹節連發、從地上長出來的竹林陣、紅白兩色的太極雙旋。愛面子但其實很軟，40% 就被打到「卸甲」：
 * 竹棍飛走、變回熊貓寶寶——縮成一顆球在畫面裡反彈滾動、躺著耍賴亂踢、大口咬竹子噴竹葉。
 * 招牌機制「竹筍」：地上冒出竹筍，沒在時限內打掉就會被拔去吃。吃的時候坐著不動、受到 1.5 倍傷害，
 * 但會回 3% 血（整場最多 [MAX_FEASTS] 次）。
 */
class PandaBoss : Boss(hp = 38000f, radius = 200f, score = 100000, homeY = 420f) {
    override val kind = EnemyKind.BOSS_PANDA
    override val name = "熊貓師父 波波"
    // 竹筍會擋子彈、吃到還會回血
    override val hpFactor = 0.9f
    override val breakAt = 0.4f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.STAFF
    override val lostWeapon = LostProp.STAFF
    override val lostProps = intArrayOf(LostProp.BEARD)
    override val handX = 165f
    override val handY = 150f

    /** 正在吃竹筍：坐著不動，受到 [EAT_DAMAGE] 倍傷害。 */
    var eating = false
        private set
    private var eatStep = false
    /** 已經吃竹筍回血的次數。 */
    var feasts = 0
        private set

    /** 縮成球在畫面裡反彈滾動中。 */
    var rolling = false
        private set
    private var rollStep = false
    private val ball = RollBall()
    /** 縮成球前的預告剩餘秒數；預告時畫出預測的滾動路線。 */
    var rollWarn = 0f
        private set
    /** 預測的滾動路線（x, y 交錯），在開始預告時算好。 */
    val rollPath = ArrayList<Float>()
    private var rollAngle = 0f
    /** 躺著耍賴時左右搖晃的角度。 */
    private var rock = 0f
    private var rockStep = false
    /** 這一步移動用的速度上限（在 move 裡決定，滾動結束的那一步也照滾動的速度算）。 */
    private var speed = 520f
    override val maxMoveSpeed: Float get() = speed
    override val damageScale: Float get() = if (eating) EAT_DAMAGE else 1f
    override val tilt: Float get() = rollAngle + rock

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        eating = eatStep
        eatStep = false
        // 滾動被換階段打斷時也要停下來
        if (!rollStep) {
            rolling = false
            rollWarn = 0f
        }
        rollStep = false
        if (rollWarn > 0f) rollWarn -= dt
        rock = if (rockStep) rock else 0f
        rockStep = false
        rollAngle = when {
            rolling -> rollAngle + ball.vx * dt * 0.45f
            rollWarn > 0f -> rollAngle + sin(age * 30f) * 2f
            else -> {
                // 滾完慢慢轉正
                val upright = kotlin.math.round(rollAngle / 360f) * 360f
                val a = approach(rollAngle, upright, 10f, dt)
                if (near(a, upright, 0.5f)) 0f else a
            }
        }
    }

    override fun move(w: World, dt: Float) {
        speed = if (rolling) ROLL_SPEED + 40f else 520f
        when {
            rolling -> {
                ball.x = x
                ball.y = y
                if (ball.step(dt, homeY - 60f, rollBottom(w))) {
                    w.shake(0.2f)
                    w.cue(SoundCue.EXPLODE_SMALL)
                    w.ring(x, y, 8, 150f, age, BulletStyle.LEAF, Tint.GREEN)
                }
                x = ball.x
                y = ball.y
            }
            // 吃竹筍、縮成球預告時坐著不動
            eating || rollWarn > 0f -> Unit
            else -> {
                x = WORLD_WIDTH / 2f + if (broken) sin(moveTime * 0.8f) * 230f else sin(moveTime * 0.55f) * 190f
                y = approach(y, homeY, 3f, dt)
            }
        }
    }

    /** 滾動範圍的下緣：離玩家平常的位置留一段距離。 */
    private fun rollBottom(w: World) = maxOf(homeY + 200f, w.height * 0.72f - 300f)

    /** 回血，但不會回到上一個階段（不會重新觸發換階段）。 */
    private fun feast(w: World) {
        feasts++
        val cap = when (phase) {
            0 -> 1f
            1 -> 0.6f
            else -> 0.25f
        } - 0.002f
        hp = minOf(hp + maxHp * FEAST_HEAL, maxHp * cap).coerceAtLeast(hp)
        w.burst(x, y - 40f, 0xFF69F0AEL, 12, 260f, ParticleKind.HEART)
        w.cue(SoundCue.POWER_UP)
    }

    private fun shoots(w: World) = w.enemies.filter { it is BambooShoot && it.alive && it.owner === this }

    /** 這一輪竹筍有吃到（坐下來吃）。 */
    private var fed = false

    /** 竹節連發：從竹棍前端射出三波扇形竹節彈。 */
    private val segmentVolley = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f) || crossed(t, dt, 1.1f) || crossed(t, dt, 1.9f)) {
            w.fan(x + handX, y + handY, aimFrom(w, handX, handY), 5, 48f * DEG, 330f, BulletStyle.BAMBOO, Tint.HONEY)
        }
    }
    /** 竹棍朝玩家突刺兩次。 */
    private val staffThrust = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.3f)) melee(w, HazardStyle.STAFF, aimFrom(w, handX, handY), 42f, warn = 0.7f)
        if (every(t, dt, 1.3f, offset = 0.8f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.BAMBOO, Tint.HONEY)
    }
    /** 棍法橫掃：從玩家的另一側掃過來 70°，玩家那一側的外緣是安全區。 */
    private val staffSweep = Attack(3.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.STAFF, if (playerLeft) HALF_PI - 55f * DEG else HALF_PI + 55f * DEG, 44f,
                warn = 0.9f, active = 1.4f, extend = 0.2f, sweep = if (playerLeft) 70f * DEG else -70f * DEG, length = 1150f,
            )
        }
        if (every(t, dt, 1.1f, offset = 1f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.BAMBOO, Tint.HONEY)
    }
    /**
     * 竹林陣：地上先畫出預告線，接著一排竹子從畫面下方長出來，把玩家關在竹子之間的縫裡，
     * 師父再從上面射竹節彈。
     */
    private val bambooGrove = Attack(4.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val gap = 230f
            var gx = 60f + w.random.nextFloat() * gap
            val top = homeY + 280f
            while (gx < WORLD_WIDTH - 40f) {
                w.lasers += Laser(
                    gx, w.height + 20f, -HALF_PI, w.height + 20f - top, 64f, warnTime = 0.9f, activeTime = 2.6f,
                    color = Tint.RED, damage = MELEE_DAMAGE, style = HazardStyle.BAMBOO, extendTime = 0.35f,
                )
                gx += gap
            }
            w.cue(SoundCue.SWING)
        }
        if (t > 1.2f && every(t, dt, 0.6f, offset = 1.2f)) w.aimed(x + handX, y + handY, 300f, BulletStyle.BAMBOO, Tint.HONEY)
    }
    /** 太極雙旋：紅、白兩道螺旋往同一個方向捲，互相繞著擴散。 */
    private val taijiSpiral = Attack(4.6f) { w, t, dt ->
        if (every(t, dt, 0.1f)) {
            val a = t * 1.9f
            w.shoot(x, y + 40f, a, 200f, BulletStyle.ORB, Tint.RED).turn = 0.7f
            w.shoot(x, y + 40f, a + HALF_PI * 2f, 200f, BulletStyle.BUBBLE, Tint.PINK).turn = 0.7f
        }
    }
    /** 叫熊貓寶寶滾下來。 */
    private val summonCubs = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(PandaCub(130f + i * 148f, (i % 3) * 0.12f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 220f, BulletStyle.BIG_ORB, Tint.HONEY)
    }
    /**
     * 竹筍：地上冒出三根竹筍，[BambooShoot.RIPE] 秒內沒打掉的會被拔去吃——
     * 吃的時候坐著不動、受到 1.5 倍傷害，但會回血。
     */
    private val bambooFeast = Attack(6f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            fed = false
            for (k in 0 until 3) {
                val sx = 230f + k * 270f + w.jitter(50f)
                val sy = homeY + 300f + w.random.nextFloat() * 140f
                w.spawn(BambooShoot(this, sx, sy))
            }
            w.cue(SoundCue.ITEM)
        }
        if (t < 4.3f && every(t, dt, 1.2f, offset = 0.9f)) w.fan(x, y + 80f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.LEAF, Tint.GREEN)
        if (crossed(t, dt, 4.3f)) {
            val ripe = shoots(w)
            for (s in ripe) {
                s.alive = false
                w.puff(s.x, s.y, Tint.GREEN)
                w.burst(s.x, s.y, 0xFF9CCB6AL, 8, 220f)
            }
            if (ripe.isNotEmpty() && feasts < MAX_FEASTS) {
                feast(w)
                fed = true
            } else if (ripe.isEmpty()) {
                // 竹筍全被打掉：師父氣得跺腳
                w.sfx(x, y - 150f, SfxText.CRITICAL)
                w.ring(x, y + 40f, 14, 180f, t, BulletStyle.LEAF, Tint.GREEN)
            }
        }
        // 有吃到才會坐下來吃
        eatStep = fed && t > 4.3f && t < 4.3f + EAT_TIME
    }

    /** 卸甲後：縮成一顆球，先畫出預測路線，再在畫面裡反彈滾動，沿路掉竹葉。 */
    private val rollBall = Attack(4.6f) { w, t, dt ->
        rollStep = t < 3.9f
        if (crossed(t, dt, 0.01f)) {
            var a = w.aimAt(x, y).coerceIn(30f * DEG, 150f * DEG)
            if (kotlin.math.abs(cos(a)) < 0.35f) a = if (w.random.nextBoolean()) 60f * DEG else 120f * DEG
            ball.vx = cos(a) * ROLL_SPEED
            ball.vy = sin(a) * ROLL_SPEED
            rollWarn = ROLL_WARN
            // 預測路線：和實際滾動用同一套反彈規則
            val ghost = RollBall()
            ghost.x = x
            ghost.y = y
            ghost.vx = ball.vx
            ghost.vy = ball.vy
            rollPath.clear()
            rollPath += x
            rollPath += y
            var s = 0f
            while (s < ROLL_TIME) {
                if (ghost.step(STEP, homeY - 60f, rollBottom(w))) {
                    rollPath += ghost.x
                    rollPath += ghost.y
                }
                s += STEP
            }
            rollPath += ghost.x
            rollPath += ghost.y
            w.cue(SoundCue.SWING)
        }
        if (crossed(t, dt, ROLL_WARN)) rolling = true
        if (rolling && every(t, dt, 0.2f)) w.shoot(x, y, w.random.nextFloat() * TWO_PI, 70f, BulletStyle.LEAF, Tint.GREEN)
        if (t >= ROLL_WARN + ROLL_TIME) rolling = false
    }
    /** 卸甲後：躺著耍賴亂踢，子彈往下方亂噴，但會留一條慢慢移動的空隙。 */
    private val tantrum = Attack(4f) { w, t, dt ->
        rockStep = t < 3.8f
        rock = sin(t * 14f) * 22f
        if (every(t, dt, 0.06f)) {
            val a = HALF_PI + w.jitter(1.35f)
            val gapCenter = HALF_PI + sin(t * 0.9f) * 0.9f
            if (kotlin.math.abs(a - gapCenter) > 0.24f) w.shoot(x, y + 120f, a, 230f + w.random.nextFloat() * 60f, BulletStyle.ORB, Tint.RED)
        }
    }
    /** 卸甲後：大口咬竹子，碎竹葉像散彈一樣噴出來。 */
    private val leafSpray = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.9f, offset = 0.3f)) w.fan(x, y + 80f, w.aimAt(x, y), 7, 70f * DEG, 320f, BulletStyle.LEAF, Tint.GREEN)
        if (every(t, dt, 0.9f, offset = 0.75f)) w.fan(x, y + 80f, w.aimAt(x, y), 4, 40f * DEG, 220f, BulletStyle.BAMBOO, Tint.HONEY)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(segmentVolley, staffThrust, bambooFeast, taijiSpiral, summonCubs)
        !broken -> listOf(staffSweep, bambooGrove, segmentVolley, bambooFeast, taijiSpiral)
        phase < 2 -> listOf(rollBall, leafSpray, bambooFeast, tantrum, summonCubs)
        else -> listOf(rollBall, tantrum, bambooGrove, leafSpray, bambooFeast)
    }

    companion object {
        const val EAT_DAMAGE = 1.5f
        const val EAT_TIME = 1.5f
        const val FEAST_HEAL = 0.03f
        const val MAX_FEASTS = 3
        const val ROLL_SPEED = 820f
        const val ROLL_WARN = 0.9f
        const val ROLL_TIME = 3f
        /** 滾動時離左右邊的距離（球的中心）。 */
        const val ROLL_MARGIN = 190f
    }
}

/** 縮成球的熊貓：等速直線滾動，碰到範圍邊緣就反彈。 */
private class RollBall {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f

    /** 走一步；有反彈時回傳 true。 */
    fun step(dt: Float, top: Float, bottom: Float): Boolean {
        x += vx * dt
        y += vy * dt
        var bounced = false
        if (x < PandaBoss.ROLL_MARGIN && vx < 0f || x > WORLD_WIDTH - PandaBoss.ROLL_MARGIN && vx > 0f) {
            vx = -vx
            bounced = true
        }
        if (y < top && vy < 0f || y > bottom && vy > 0f) {
            vy = -vy
            bounced = true
        }
        return bounced
    }
}

/** 熊貓師父的竹筍：從地上冒出來，[RIPE] 秒後被拔去吃；沒人管的話 [WITHER] 秒後自己枯掉。 */
class BambooShoot(val owner: PandaBoss, x: Float, y: Float) : Enemy(x, y, 50f, 42f, 300) {
    override val kind = EnemyKind.BAMBOO_SHOOT
    override val contactDamage = 0f
    /** 長出來的程度（畫面用）：0～1。 */
    val grow: Float get() = (age / 0.4f).coerceIn(0f, 1f)
    /** 快被吃掉的程度（畫面用來畫倒數圈）：0～1。 */
    val ripeness: Float get() = (age / RIPE).coerceIn(0f, 1f)

    override fun update(w: World, dt: Float) {
        age += dt
        if (!owner.alive || age > WITHER) {
            alive = false
            w.puff(x, y, Tint.GREEN)
        }
    }

    companion object {
        const val RIPE = 4f
        const val WITHER = 6f
    }
}
