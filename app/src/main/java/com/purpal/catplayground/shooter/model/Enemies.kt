package com.purpal.catplayground.shooter.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Tint {
    const val CHEESE = 0xFFFFD54FL
    const val SEED = 0xFFFF7043L
    const val PINK = 0xFFFF6FAEL
    const val BONE = 0xFFFFF3E0L
    const val NOTE = 0xFF7C4DFFL
    const val TENNIS = 0xFFC6FF00L
    const val HONEY = 0xFFFFB300L
    const val STAR = 0xFFFFEE58L
    const val RED = 0xFFFF5252L
    const val CYAN = 0xFF40C4FFL
    const val ORANGE = 0xFFFF9100L
    const val PURPLE = 0xFFB388FFL
    const val GREEN = 0xFF69F0AEL
}

/** 畫面依這個決定怎麼畫敵人。 */
enum class EnemyKind {
    MOUSE_RUNNER, MINI_MOUSE, MOUSE_PLANE, CHEESE_DRONE,
    PUPPY, FRISBEE, BALL_TURRET,
    BEE, HONEY_POT, BEAR_CUB, BEEHIVE,
    BALLOON,
    DUCKLING, YUZU, BUCKET, BABY_CAPY,
    ELITE_MOUSE, ELITE_DOG, ELITE_BEE, ELITE_MONKEY,
    BOSS_MOUSE, BOSS_DOG, BOSS_BEAR, BOSS_CAPY,
    /** 柴犬隊長丟出去的迴力骨頭（打不壞的道具）。 */
    THROWN_BONE,
}

abstract class Enemy(
    var x: Float,
    var y: Float,
    var hp: Float,
    val radius: Float,
    val score: Int,
) {
    val id = nextId++
    var age = 0f
    var alive = true
    var hitFlash = 0f
    var drop: ItemType? = null
    abstract val kind: EnemyKind
    open val isBoss: Boolean get() = false
    /** 菁英怪：比較硬、攻擊比較兇，一定會掉武器膠囊。 */
    open val isElite: Boolean get() = false
    /** Boss 進場中等情況不能被打。 */
    open val invulnerable: Boolean get() = false
    /** 碰到玩家會不會受傷（一般是可以被打的時候；被丟出來的道具雖然打不壞，碰到一樣會痛）。 */
    open val hurtsOnContact: Boolean get() = !invulnerable
    /** 撞到玩家造成的傷害。 */
    open val contactDamage: Float get() = 20f
    /** 自毀俯衝型：撞到玩家就自爆。 */
    open val kamikaze: Boolean get() = false

    /** 還沒顯示成傷害數字的累積傷害（一小段時間合併成一個數字）。 */
    var pendingDamage = 0f
    var pendingTime = 0f
    /** 上一次被肉球閃電打中的時間。 */
    var pawHitTime = -1f

    abstract fun update(w: World, dt: Float)

    /** 飛出畫面就移除（只在進場一段時間後檢查，避免剛生成就被刪）。 */
    protected fun leaveIfOffscreen(w: World) {
        if (age > 1f && (y > w.height + 120f || y < -260f || x < -220f || x > WORLD_WIDTH + 220f)) alive = false
    }

    companion object {
        private var nextId = 1
    }
}

/** 掛著道具的氣球，不會攻擊，打破就掉道具。 */
class Balloon(x: Float, item: ItemType) : Enemy(x, -60f, 26f, 52f, 200) {
    override val kind = EnemyKind.BALLOON
    override val contactDamage = 0f
    private val baseX = x

    init {
        drop = item
    }

    override fun update(w: World, dt: Float) {
        age += dt
        y += 95f * dt
        x = baseX + sin(age * 2f) * 40f
        // 掛著的武器膠囊也會輪流切換類型。
        if (every(age, dt, Item.CYCLE_TIME, offset = Item.CYCLE_TIME)) drop = drop?.let { Item.next(it) }
        leaveIfOffscreen(w)
    }
}

/**
 * 小兵分成兩種：
 * - 自毀俯衝型（[Diver]）：成群出現、不發射子彈，先排隊飛進來，再鎖定玩家衝過去，撞到就自爆。
 *   各種類的俯衝速度有快有慢，同一群裡也有一點差異。
 * - 地面固定型（[GroundShooter]）：跟著地面捲動，只有它們和菁英怪會發射子彈。
 */
abstract class Diver(
    x: Float,
    y: Float,
    hp: Float,
    radius: Float,
    score: Int,
    /** 開始俯衝的時間。 */
    private val diveAt: Float,
    /** 俯衝速度。 */
    private val diveSpeed: Float,
    /** 俯衝時每秒最多轉向的弧度（0 表示直線衝）。 */
    private val homing: Float,
) : Enemy(x, y, hp, radius, score) {
    override val kamikaze = true
    override val contactDamage = KAMIKAZE_DAMAGE
    var diving = false
        private set
    var vx = 0f
        private set
    var vy = 0f
        private set
    /** 俯衝前的預備（畫面用來畫紅色預警光）：0～1。 */
    val windup: Float get() = if (diving) 1f else ((age - (diveAt - WINDUP)) / WINDUP).coerceIn(0f, 1f)

    /** 俯衝前的移動（排隊飛進來）。 */
    protected abstract fun cruise(w: World, dt: Float)

    override fun update(w: World, dt: Float) {
        age += dt
        if (!diving) {
            cruise(w, dt)
            if (age >= diveAt) {
                diving = true
                val a = w.aimAt(x, y)
                vx = cos(a) * diveSpeed
                vy = sin(a).coerceAtLeast(0.25f) * diveSpeed
            }
        } else {
            if (homing > 0f && y < w.player.y - 60f) {
                val want = w.aimAt(x, y)
                val current = kotlin.math.atan2(vy, vx)
                var diff = want - current
                while (diff > HALF_PI * 2f) diff -= TWO_PI
                while (diff < -HALF_PI * 2f) diff += TWO_PI
                val turned = current + diff.coerceIn(-homing * dt, homing * dt)
                vx = cos(turned) * diveSpeed
                vy = sin(turned) * diveSpeed
            }
            x += vx * dt
            y += vy * dt
        }
        leaveIfOffscreen(w)
    }

    companion object {
        const val KAMIKAZE_DAMAGE = 16f
        const val WINDUP = 0.35f
    }
}

/** 地面固定型：跟著地面捲動，在畫面中段以上才開火（不會在畫面外或貼臉時偷射）。 */
abstract class GroundShooter(x: Float, hp: Float, radius: Float, score: Int) : Enemy(x, -60f, hp, radius, score) {
    protected abstract fun fire(w: World, t: Float, dt: Float)

    /** 開火的節奏從進入畫面後開始算。 */
    private var fireTime = 0f

    override fun update(w: World, dt: Float) {
        age += dt
        y += World.SCROLL_SPEED * dt
        if (y > 40f && y < w.height * 0.72f) {
            fireTime += dt
            fire(w, fireTime, dt)
        }
        leaveIfOffscreen(w)
    }

}

// ───────────── 第 1 關：老鼠 ─────────────

/** 一路蛇行下來的老鼠，慢慢鎖定玩家衝過去（慢速、會追）。 */
class MouseRunner(x: Float, private val wiggle: Float = 1f) :
    Diver(x, -50f, 24f, 40f, 100, diveAt = 1.2f + (x % 5f) * 0.08f, diveSpeed = 300f, homing = 1.4f) {
    override val kind = EnemyKind.MOUSE_RUNNER
    private val baseX = x

    override fun cruise(w: World, dt: Float) {
        y += 200f * dt
        x = baseX + sin(age * 4f) * 30f * wiggle
    }
}

/** 鼠王叫來的小老鼠：一出來就快速衝刺（快速、直線）。 */
class MiniMouse(x: Float, y: Float, private val dir: Float) :
    Diver(x, y, 14f, 32f, 50, diveAt = 0.45f, diveSpeed = 560f, homing = 0f) {
    override val kind = EnemyKind.MINI_MOUSE

    override fun cruise(w: World, dt: Float) {
        y += 120f * dt
        x += dir * 160f * dt
    }
}

/** 從側邊繞弧線飛進來的老鼠玩具飛機，接著高速直線俯衝（快速）。 */
class MousePlane(private val fromLeft: Boolean, private val startY: Float) :
    Diver(if (fromLeft) -60f else WORLD_WIDTH + 60f, startY, 40f, 48f, 150, diveAt = 1.6f, diveSpeed = 620f, homing = 0.3f) {
    override val kind = EnemyKind.MOUSE_PLANE
    val facing: Float get() = if (fromLeft) 1f else -1f

    override fun cruise(w: World, dt: Float) {
        x += facing * 300f * dt
        y = startY + sin(age * 1.6f) * 90f + age * 40f
    }
}

/** 地上的起司輪砲台：一邊轉一邊放出一圈起司（慢速彈）。 */
class CheeseDrone(x: Float, @Suppress("UNUSED_PARAMETER") stopY: Float = 0f) : GroundShooter(x, 90f, 56f, 300) {
    override val kind = EnemyKind.CHEESE_DRONE
    val spin: Float get() = age * 2.5f

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 1.8f, offset = 0.6f)) w.ring(x, y, 10, 160f, age * 0.7f, BulletStyle.CHEESE, Tint.CHEESE)
    }
}

// ───────────── 第 2 關：狗狗 ─────────────

/** 坐在草地上丟骨頭（拋物線）的小狗。 */
class Puppy(x: Float, @Suppress("UNUSED_PARAMETER") stopY: Float = 0f) : GroundShooter(x, 45f, 48f, 180) {
    override val kind = EnemyKind.PUPPY

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 1.7f, offset = 0.5f)) {
            for (side in listOf(-1f, 1f)) {
                val dx = (w.player.x - x) / 1.6f + side * 72f
                w.bullet(x, y, dx, -180f, BulletStyle.BONE, Tint.BONE).ay = 330f
            }
        }
    }
}

/** 一邊旋轉一邊橫向切進來、再朝玩家直線飛過去的飛盤（最快）。 */
class Frisbee(private val fromLeft: Boolean, private val startY: Float) :
    Diver(if (fromLeft) -60f else WORLD_WIDTH + 60f, startY, 35f, 46f, 160, diveAt = 1.0f, diveSpeed = 700f, homing = 0f) {
    override val kind = EnemyKind.FRISBEE
    val spin: Float get() = age * 9f

    override fun cruise(w: World, dt: Float) {
        x += (if (fromLeft) 1f else -1f) * 320f * dt
        y = startY + age * 30f
    }
}

/** 跟著地面捲動的網球發射台，三連發瞄準射擊（快速彈）。 */
class BallTurret(x: Float) : GroundShooter(x, 70f, 50f, 220) {
    override val kind = EnemyKind.BALL_TURRET
    var aim = HALF_PI

    override fun update(w: World, dt: Float) {
        aim = w.aimAt(x, y)
        super.update(w, dt)
    }

    override fun fire(w: World, t: Float, dt: Float) {
        val c = t % 2.4f
        if (t > 0.6f && (crossed(c, dt, 0.2f) || crossed(c, dt, 0.45f) || crossed(c, dt, 0.7f))) {
            w.shoot(x + cos(aim) * 40f, y + sin(aim) * 40f, aim, 400f, BulletStyle.ORB, Tint.TENNIS)
        }
    }
}

// ───────────── 第 3 關：熊熊 ─────────────

/** 先蛇行，再鎖定玩家高速俯衝的蜜蜂（快速、微追）。 */
class Bee(x: Float, private val delay: Float) :
    Diver(x, -40f, 12f, 30f, 80, diveAt = 1.2f + delay, diveSpeed = 520f, homing = 0.6f) {
    override val kind = EnemyKind.BEE
    private val baseX = x

    override fun cruise(w: World, dt: Float) {
        y += 200f * dt
        x = baseX + sin(age * 5f) * 50f
    }
}

/** 放在樹根上的蜂蜜罐，不時滴下蜂蜜；被打破會噴一圈蜂蜜。 */
class HoneyPot(x: Float) : GroundShooter(x, 120f, 58f, 320) {
    override val kind = EnemyKind.HONEY_POT

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 1.9f, offset = 0.5f)) {
            for (k in -2..2) {
                w.bullet(x + k * 12f, y + 30f, k * 54f, 90f, BulletStyle.HONEY, Tint.HONEY).ay = 90f
            }
        }
    }

    fun burst(w: World) = w.ring(x, y, 8, 170f, 0f, BulletStyle.HONEY, Tint.HONEY)
}

/** 坐在木頭上的小熊，扇形射擊。 */
class BearCub(x: Float) : GroundShooter(x, 80f, 58f, 260) {
    override val kind = EnemyKind.BEAR_CUB

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 2f, offset = 0.6f)) w.fan(x, y, w.aimAt(x, y), 5, 60f * DEG, 230f, BulletStyle.RICE, Tint.ORANGE)
    }
}

/** 熊熊大王丟出來的蜂巢：落下後爆開成蜜蜂彈。 */
class Beehive(x: Float, y: Float, private val vx: Float) : Enemy(x, y, 30f, 40f, 100) {
    override val kind = EnemyKind.BEEHIVE
    private var vy = -200f

    override fun update(w: World, dt: Float) {
        age += dt
        vy += 420f * dt
        x += vx * dt
        y += vy * dt
        if (age > 1.3f) {
            w.fan(x, y, w.aimAt(x, y), 7, 100f * DEG, 220f, BulletStyle.STAR, Tint.STAR)
            alive = false
            w.puff(x, y, Tint.HONEY)
        }
    }
}

// ───────────── 第 4 關：卡皮巴拉的溫泉 ─────────────

/** 頭上頂著小毛巾的小鴨子，排成一排游下來，慢慢追著玩家撞（慢速、很會追）。 */
class Duckling(x: Float, private val delay: Float) :
    Diver(x, -50f - delay * 200f, 16f, 30f, 90, diveAt = 1.0f + delay, diveSpeed = 240f, homing = 2.2f) {
    override val kind = EnemyKind.DUCKLING
    private val baseX = x

    override fun cruise(w: World, dt: Float) {
        y += 200f * dt
        x = baseX + sin(age * 3f + delay * 6f) * 36f
    }
}

/** 漂在河面上的橘子，一邊轉一邊爆出一圈橘子籽。 */
class YuzuDrone(x: Float, @Suppress("UNUSED_PARAMETER") stopY: Float = 0f) : GroundShooter(x, 40f, 40f, 160) {
    override val kind = EnemyKind.YUZU
    val spin: Float get() = age * 3f

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 1.7f, offset = 0.6f)) w.ring(x, y, 10, 190f, age, BulletStyle.SEED, Tint.ORANGE)
    }
}

/** 跟著地面捲動的檜木水桶（裡面躲著小青蛙），往玩家潑三坨熱水。 */
class BucketTurret(x: Float) : GroundShooter(x, 70f, 42f, 220) {
    override val kind = EnemyKind.BUCKET

    override fun fire(w: World, t: Float, dt: Float) {
        if (t > 0.6f && every(t, dt, 2.2f, offset = 0.6f)) {
            for (k in -1..1) {
                val dx = (w.player.x - x) / 1.5f + k * 84f
                w.bullet(x, y - 10f, dx, -230f, BulletStyle.HONEY, Tint.ORANGE).ay = 360f
            }
        }
    }
}

/** 套著游泳圈漂下來的小卡皮巴拉，慢慢吐出扇形泡泡。 */
class BabyCapy(x: Float) : GroundShooter(x, 90f, 50f, 260) {
    override val kind = EnemyKind.BABY_CAPY

    override fun update(w: World, dt: Float) {
        x += sin(age * 1.2f) * 30f * dt
        super.update(w, dt)
    }

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 2.1f, offset = 0.6f)) w.fan(x, y + 20f, w.aimAt(x, y), 5, 70f * DEG, 150f, BulletStyle.BUBBLE, Tint.CYAN)
    }
}

// ───────────── 菁英怪 ─────────────

abstract class Elite(x: Float, hp: Float, radius: Float, score: Int, item: ItemType) : Enemy(x, -90f, hp, radius, score) {
    override val isElite = true
    override val contactDamage = 30f

    init {
        drop = item
    }
}

/** 起司騎士：左右巡邏，扇形種子彈＋起司圈。 */
class EliteMouse(x: Float, private val stopY: Float = 330f) : Elite(x, 420f, 64f, 2000, ItemType.SPREAD) {
    override val kind = EnemyKind.ELITE_MOUSE
    private val baseX = x

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.6f -> y = approach(y, stopY, 2.5f, dt)
            age < 11f -> {
                x = baseX + sin((age - 1.6f) * 0.9f) * 160f
                if (every(age, dt, 1.4f, offset = 1.8f)) w.fan(x, y + 40f, w.aimAt(x, y), 5, 44f * DEG, 300f, BulletStyle.SEED, Tint.SEED)
                if (every(age, dt, 3f, offset = 2.6f)) w.ring(x, y, 16, 200f, age, BulletStyle.CHEESE, Tint.CHEESE)
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 牧羊犬教官：吹哨叫小狗來幫忙，丟回力鏢骨頭，叫聲變成音符圈。 */
class EliteDog(x: Float, private val stopY: Float = 320f) : Elite(x, 480f, 66f, 2400, ItemType.HOMING) {
    override val kind = EnemyKind.ELITE_DOG

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.6f -> y = approach(y, stopY, 2.5f, dt)
            age < 12f -> {
                x = approach(x, w.player.x.coerceIn(150f, WORLD_WIDTH - 150f), 0.5f, dt)
                if (every(age, dt, 1.1f, offset = 1.8f)) {
                    for (side in listOf(-1f, 1f)) {
                        w.shoot(x, y + 40f, HALF_PI + side * 55f * DEG, 280f, BulletStyle.BONE, Tint.BONE).turn = -side * 1.4f
                    }
                }
                if (every(age, dt, 2.6f, offset = 2.4f)) w.ring(x, y, 20, 190f, age, BulletStyle.NOTE, Tint.NOTE)
                if (crossed(age, dt, 3f) || crossed(age, dt, 7.5f)) {
                    w.spawn(Puppy((x - 180f).coerceIn(80f, WORLD_WIDTH - 80f), y + 80f))
                    w.spawn(Puppy((x + 180f).coerceIn(80f, WORLD_WIDTH - 80f), y + 80f))
                }
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 蜂后：繞 8 字飛行，召喚蜜蜂，三向星星彈。 */
class EliteBee(x: Float) : Elite(x, 380f, 60f, 2200, ItemType.FOCUS) {
    override val kind = EnemyKind.ELITE_BEE
    private val baseX = x

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.4f -> y = approach(y, 300f, 2.5f, dt)
            age < 11f -> {
                val t = age - 1.4f
                x = baseX + sin(t * 0.9f) * 220f
                y = 300f + sin(t * 1.8f) * 70f
                if (every(age, dt, 0.9f, offset = 1.8f)) w.fan(x, y + 30f, w.aimAt(x, y), 3, 24f * DEG, 320f, BulletStyle.STAR, Tint.STAR)
                if (every(age, dt, 1.8f, offset = 2.2f)) {
                    w.spawn(Bee(x - 60f, 0.2f).also { it.y = y })
                    w.spawn(Bee(x + 60f, 0.4f).also { it.y = y })
                }
            }
            else -> y += 260f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 溫泉猴老大（雪猴）：在幾個點之間跳來跳去，丟雪球、叫小鴨子來幫忙。 */
class EliteMonkey(x: Float) : Elite(x, 450f, 64f, 2400, ItemType.SONIC) {
    override val kind = EnemyKind.ELITE_MONKEY
    private var hopFromX = x
    private var hopToX = x
    private var hopT = 1f

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.5f -> y = approach(y, 320f, 2.5f, dt)
            age < 12f -> {
                if (every(age, dt, 1.8f, offset = 1.5f)) {
                    hopFromX = x
                    hopToX = 150f + w.random.nextFloat() * (WORLD_WIDTH - 300f)
                    hopT = 0f
                }
                if (hopT < 1f) {
                    hopT = (hopT + dt / 0.5f).coerceAtMost(1f)
                    x = hopFromX + (hopToX - hopFromX) * hopT
                    y = 320f - sin(hopT * HALF_PI * 2f) * 90f
                    if (hopT >= 1f) w.ring(x, y, 14, 190f, age, BulletStyle.BIG_ORB, Tint.CYAN)
                }
                if (every(age, dt, 0.9f, offset = 2f)) w.fan(x, y + 30f, w.aimAt(x, y), 3, 26f * DEG, 300f, BulletStyle.ORB, Tint.CYAN)
                if (crossed(age, dt, 5f)) for (i in 0 until 4) w.spawn(Duckling(200f + i * 200f, i * 0.12f))
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}

internal fun approach(current: Float, target: Float, rate: Float, dt: Float): Float =
    current + (target - current) * minOf(1f, rate * dt)

internal fun near(a: Float, b: Float, eps: Float) = abs(a - b) < eps
