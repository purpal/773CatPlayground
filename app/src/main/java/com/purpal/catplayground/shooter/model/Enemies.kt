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

// ───────────── 第 1 關：老鼠 ─────────────

class MouseRunner(x: Float, private val wiggle: Float = 1f) : Enemy(x, -50f, 24f, 40f, 100) {
    override val kind = EnemyKind.MOUSE_RUNNER
    private val baseX = x

    override fun update(w: World, dt: Float) {
        age += dt
        y += 170f * dt
        x = baseX + sin(age * 4f) * 30f * wiggle
        if (crossed(age, dt, 1.0f) || crossed(age, dt, 2.6f)) {
            w.aimed(x, y + 20f, 270f, BulletStyle.SEED, Tint.SEED)
        }
        leaveIfOffscreen(w)
    }
}

class MiniMouse(x: Float, y: Float, private val dir: Float) : Enemy(x, y, 14f, 32f, 50) {
    override val kind = EnemyKind.MINI_MOUSE

    override fun update(w: World, dt: Float) {
        age += dt
        y += 230f * dt
        x += dir * 110f * dt
        if (crossed(age, dt, 0.7f)) w.aimed(x, y, 250f, BulletStyle.SEED, Tint.SEED)
        leaveIfOffscreen(w)
    }
}

/** 從側邊繞弧線飛過畫面的老鼠玩具飛機。 */
class MousePlane(private val fromLeft: Boolean, private val startY: Float) :
    Enemy(if (fromLeft) -60f else WORLD_WIDTH + 60f, startY, 40f, 48f, 150) {
    override val kind = EnemyKind.MOUSE_PLANE
    val facing: Float get() = if (fromLeft) 1f else -1f

    override fun update(w: World, dt: Float) {
        age += dt
        x += facing * 260f * dt
        y = startY + sin(age * 1.6f) * 120f + age * 40f
        if (every(age, dt, 1.4f, offset = 0.6f)) {
            w.fan(x, y, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.RICE, Tint.SEED)
        }
        leaveIfOffscreen(w)
    }
}

/** 旋轉的起司輪無人機：停在空中放出一圈起司。 */
class CheeseDrone(x: Float, private val stopY: Float) : Enemy(x, -60f, 90f, 56f, 300) {
    override val kind = EnemyKind.CHEESE_DRONE
    val spin: Float get() = age * 2.5f

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.5f -> y += (stopY - y) * minOf(1f, dt * 3f)
            age < 6f -> if (every(age, dt, 1.2f, offset = 1.6f)) {
                w.ring(x, y, 10, 190f, age * 0.7f, BulletStyle.CHEESE, Tint.CHEESE)
            }
            else -> y += 200f * dt
        }
        leaveIfOffscreen(w)
    }
}

// ───────────── 第 2 關：狗狗 ─────────────

/** 停下來丟骨頭（拋物線）的小狗。 */
class Puppy(x: Float, private val stopY: Float) : Enemy(x, -60f, 45f, 48f, 180) {
    override val kind = EnemyKind.PUPPY

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.2f -> y += (stopY - y) * minOf(1f, dt * 3f)
            age < 4.5f -> if (every(age, dt, 1.3f, offset = 1.4f)) {
                for (side in listOf(-1f, 1f)) {
                    val dx = (w.player.x - x) / 1.6f + side * 60f
                    w.bullet(x, y, dx, -180f, BulletStyle.BONE, Tint.BONE).ay = 330f
                }
            }
            else -> y += 220f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 橫向飛過、一邊旋轉一邊灑出螺旋子彈的飛盤。 */
class Frisbee(private val fromLeft: Boolean, private val startY: Float) :
    Enemy(if (fromLeft) -60f else WORLD_WIDTH + 60f, startY, 35f, 46f, 160) {
    override val kind = EnemyKind.FRISBEE
    val spin: Float get() = age * 9f

    override fun update(w: World, dt: Float) {
        age += dt
        x += (if (fromLeft) 1f else -1f) * 240f * dt
        y = startY + age * 30f
        if (every(age, dt, 0.18f, offset = 0.5f)) {
            w.shoot(x, y, age * 3f, 200f, BulletStyle.STAR, Tint.STAR)
            w.shoot(x, y, age * 3f + HALF_PI * 2f, 200f, BulletStyle.STAR, Tint.STAR)
        }
        leaveIfOffscreen(w)
    }
}

/** 跟著地面捲動的網球發射台，三連發瞄準射擊。 */
class BallTurret(x: Float) : Enemy(x, -60f, 70f, 50f, 220) {
    override val kind = EnemyKind.BALL_TURRET
    var aim = HALF_PI

    override fun update(w: World, dt: Float) {
        age += dt
        y += World.SCROLL_SPEED * dt
        aim = w.aimAt(x, y)
        val t = age % 2.2f
        if (age > 0.8f && (crossed(t, dt, 0.2f) || crossed(t, dt, 0.4f) || crossed(t, dt, 0.6f))) {
            w.shoot(x + cos(aim) * 40f, y + sin(aim) * 40f, aim, 300f, BulletStyle.ORB, Tint.TENNIS)
        }
        leaveIfOffscreen(w)
    }
}

// ───────────── 第 3 關：熊熊 ─────────────

/** 先蛇行，再鎖定玩家方向俯衝的蜜蜂。 */
class Bee(x: Float, private val delay: Float) : Enemy(x, -40f, 12f, 30f, 80) {
    override val kind = EnemyKind.BEE
    private val baseX = x
    private var diveVx = 0f
    private var diveVy = 0f

    override fun update(w: World, dt: Float) {
        age += dt
        if (age < 1.4f + delay) {
            y += 180f * dt
            x = baseX + sin(age * 5f) * 50f
        } else {
            if (diveVy == 0f) {
                val a = w.aimAt(x, y)
                diveVx = cos(a) * 380f
                diveVy = sin(a).coerceAtLeast(0.3f) * 380f
                w.aimed(x, y, 260f, BulletStyle.ORB, Tint.STAR, radius = 8f)
            }
            x += diveVx * dt
            y += diveVy * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 慢慢飄下來、不時滴下蜂蜜的蜂蜜罐；被打破會噴一圈蜂蜜。 */
class HoneyPot(x: Float) : Enemy(x, -60f, 120f, 58f, 320) {
    override val kind = EnemyKind.HONEY_POT

    override fun update(w: World, dt: Float) {
        age += dt
        y += 75f * dt
        if (every(age, dt, 1.6f, offset = 1f)) {
            for (k in -2..2) {
                w.bullet(x + k * 10f, y + 30f, k * 45f, 90f, BulletStyle.HONEY, Tint.HONEY).ay = 90f
            }
        }
        leaveIfOffscreen(w)
    }

    fun burst(w: World) = w.ring(x, y, 8, 170f, 0f, BulletStyle.HONEY, Tint.HONEY)
}

/** 坐在木頭上的小熊，扇形射擊。 */
class BearCub(x: Float) : Enemy(x, -70f, 80f, 58f, 260) {
    override val kind = EnemyKind.BEAR_CUB

    override fun update(w: World, dt: Float) {
        age += dt
        y += 95f * dt
        if (every(age, dt, 1.7f, offset = 1f)) {
            w.fan(x, y, w.aimAt(x, y), 5, 60f * DEG, 230f, BulletStyle.RICE, Tint.ORANGE)
        }
        leaveIfOffscreen(w)
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

/** 頭上頂著小毛巾的小鴨子，排成一排游下來，偶爾吐一顆泡泡。 */
class Duckling(x: Float, private val delay: Float) : Enemy(x, -50f - delay * 200f, 16f, 30f, 90) {
    override val kind = EnemyKind.DUCKLING
    private val baseX = x

    override fun update(w: World, dt: Float) {
        age += dt
        y += 190f * dt
        x = baseX + sin(age * 3f + delay * 6f) * 36f
        if (crossed(age, dt, 1.2f + delay)) w.aimed(x, y + 20f, 240f, BulletStyle.BUBBLE, Tint.CYAN, radius = 11f)
        leaveIfOffscreen(w)
    }
}

/** 一邊轉一邊飄下來的柚子，停下來後爆出一圈柚子籽。 */
class YuzuDrone(x: Float, private val stopY: Float) : Enemy(x, -60f, 40f, 40f, 160) {
    override val kind = EnemyKind.YUZU
    val spin: Float get() = age * 3f

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.3f -> y = approach(y, stopY, 3f, dt)
            age < 4.5f -> if (every(age, dt, 1.4f, offset = 1.6f)) w.ring(x, y, 10, 200f, age, BulletStyle.SEED, Tint.ORANGE)
            else -> y += 220f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 跟著地面捲動的檜木水桶（裡面躲著小青蛙），往玩家潑三坨熱水。 */
class BucketTurret(x: Float) : Enemy(x, -60f, 70f, 42f, 220) {
    override val kind = EnemyKind.BUCKET

    override fun update(w: World, dt: Float) {
        age += dt
        y += World.SCROLL_SPEED * dt
        if (age > 0.8f && every(age, dt, 2f, offset = 1f)) {
            for (k in -1..1) {
                val dx = (w.player.x - x) / 1.5f + k * 70f
                w.bullet(x, y - 10f, dx, -230f, BulletStyle.HONEY, Tint.ORANGE).ay = 360f
            }
        }
        leaveIfOffscreen(w)
    }
}

/** 套著游泳圈漂下來的小卡皮巴拉，慢慢吐出扇形泡泡。 */
class BabyCapy(x: Float) : Enemy(x, -70f, 90f, 50f, 260) {
    override val kind = EnemyKind.BABY_CAPY

    override fun update(w: World, dt: Float) {
        age += dt
        y += 80f * dt
        x += sin(age * 1.2f) * 30f * dt
        if (every(age, dt, 1.9f, offset = 1f)) w.fan(x, y + 20f, w.aimAt(x, y), 5, 70f * DEG, 150f, BulletStyle.BUBBLE, Tint.CYAN)
        leaveIfOffscreen(w)
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
