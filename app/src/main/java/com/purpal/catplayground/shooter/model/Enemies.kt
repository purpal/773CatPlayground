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
    BOSS_MOUSE, BOSS_DOG, BOSS_BEAR,
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
    /** Boss 進場中等情況不能被打。 */
    open val invulnerable: Boolean get() = false
    /** 撞到玩家造成的傷害。 */
    open val contactDamage: Float get() = 15f

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

internal fun approach(current: Float, target: Float, rate: Float, dt: Float): Float =
    current + (target - current) * minOf(1f, rate * dt)

internal fun near(a: Float, b: Float, eps: Float) = abs(a - b) < eps
