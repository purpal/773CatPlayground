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
    BUNNY, CARROT_TURRET,
    PENGUIN_SLIDER, SNOWMAN,
    LEAF_STAR, OWL, SNOT_BUBBLE,
    LEAP_FISH, CLAM,
    ELITE_MOUSE, ELITE_DOG, ELITE_BEE, ELITE_MONKEY, ELITE_HARE, ELITE_SEAL, ELITE_KANGAROO, ELITE_BEAVER,
    BOSS_MOUSE, BOSS_DOG, BOSS_BEAR, BOSS_CAPY, BOSS_RABBIT, BOSS_PENGUIN, BOSS_KOALA, BOSS_OTTER,
    /** 水獺姊妹的妹妹嚕嚕（和姊姊共用血量）。 */
    OTTER_TWIN,
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
    /** 身體是實心的（Boss）：撞上去會受損並被推開。 */
    open val solid: Boolean get() = isBoss
    /** 受到的傷害轉給這隻（例如共用血量的雙胞胎 Boss）。 */
    open val damageTarget: Enemy? get() = null
    /** 受到的傷害倍率（例如打瞌睡時防禦變低）。 */
    open val damageScale: Float get() = 1f

    /** 還沒顯示成傷害數字的累積傷害（一小段時間合併成一個數字）。 */
    var pendingDamage = 0f
    var pendingTime = 0f
    /** 上一次被震動掌打中的時間。 */
    var pawHitTime = -1f
    /** 被震動掌打中後抖動的剩餘秒數（畫面用）。 */
    var vibrate = 0f

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

// ───────────── 第 5 關：胡蘿蔔田 ─────────────

/** 跳跳小兔：一蹦一蹦地成群跳下來，接著高速撲向玩家（快速、微追）。 */
class Bunny(x: Float, private val delay: Float) :
    Diver(x, -50f - delay * 160f, 18f, 32f, 90, diveAt = 1.1f + delay, diveSpeed = 580f, homing = 0.5f) {
    override val kind = EnemyKind.BUNNY
    private val baseX = x

    override fun cruise(w: World, dt: Float) {
        // 一蹦一蹦：前進速度跟著跳躍節奏忽快忽慢
        y += (120f + kotlin.math.abs(sin(age * 7f)) * 260f) * dt
        x = baseX + sin(age * 3.5f + delay * 5f) * 24f
    }
}

/** 胡蘿蔔砲台：從土裡冒出來的大胡蘿蔔，三連發瞄準的胡蘿蔔飛彈（快速）＋偶爾一小扇（慢速）。 */
class CarrotTurret(x: Float) : GroundShooter(x, 75f, 46f, 230) {
    override val kind = EnemyKind.CARROT_TURRET

    override fun fire(w: World, t: Float, dt: Float) {
        val c = t % 2.6f
        if (t > 0.6f && (crossed(c, dt, 0.2f) || crossed(c, dt, 0.42f) || crossed(c, dt, 0.64f))) {
            w.aimed(x, y - 30f, 390f, BulletStyle.RICE, Tint.ORANGE)
        }
        if (every(t, dt, 3.4f, offset = 1.8f)) w.fan(x, y - 30f, w.aimAt(x, y), 3, 40f * DEG, 170f, BulletStyle.RICE, Tint.ORANGE)
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

/** 野兔隊長：在田裡蹦來蹦去，落地時炸開一圈星星，空中丟胡蘿蔔扇，還會叫小兔子來。 */
class EliteHare(x: Float) : Elite(x, 460f, 64f, 2400, ItemType.FOCUS) {
    override val kind = EnemyKind.ELITE_HARE
    private var hopFromX = x
    private var hopToX = x
    private var hopT = 1f

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.5f -> y = approach(y, 330f, 2.5f, dt)
            age < 12f -> {
                if (every(age, dt, 1.5f, offset = 1.5f)) {
                    hopFromX = x
                    hopToX = 150f + w.random.nextFloat() * (WORLD_WIDTH - 300f)
                    hopT = 0f
                }
                if (hopT < 1f) {
                    hopT = (hopT + dt / 0.45f).coerceAtMost(1f)
                    x = hopFromX + (hopToX - hopFromX) * hopT
                    y = 330f - sin(hopT * HALF_PI * 2f) * 110f
                    if (hopT >= 1f) w.ring(x, y, 12, 180f, age, BulletStyle.STAR, Tint.STAR)
                }
                if (every(age, dt, 1f, offset = 2f)) w.fan(x, y + 30f, w.aimAt(x, y), 5, 50f * DEG, 300f, BulletStyle.RICE, Tint.ORANGE)
                if (crossed(age, dt, 5f)) for (i in 0 until 5) w.spawn(Bunny(200f + i * 150f, i * 0.12f))
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}

internal fun approach(current: Float, target: Float, rate: Float, dt: Float): Float =
    current + (target - current) * minOf(1f, rate * dt)

internal fun near(a: Float, b: Float, eps: Float) = abs(a - b) < eps

// ───────────── 第 6 關：冰雪南極 ─────────────

/** 滑行小企鵝：肚子貼地成群滑下來，接著高速直線撞向玩家（快速、幾乎不追）。 */
class PenguinSlider(x: Float, private val delay: Float) :
    Diver(x, -50f - delay * 160f, 20f, 34f, 90, diveAt = 1.0f + delay, diveSpeed = 640f, homing = 0.2f) {
    override val kind = EnemyKind.PENGUIN_SLIDER
    private val baseX = x

    override fun cruise(w: World, dt: Float) {
        y += 260f * dt
        x = baseX + sin(age * 2.5f + delay * 4f) * 18f
    }
}

/** 雪人砲台：放出一圈會「急凍」停在空中的冰晶，再丟一顆瞄準的雪球（快速）。 */
class SnowmanTurret(x: Float) : GroundShooter(x, 85f, 50f, 240) {
    override val kind = EnemyKind.SNOWMAN

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 2.4f, offset = 0.6f)) {
            for (k in 0 until 10) {
                val b = w.shoot(x, y, age * 0.5f + k * TWO_PI / 10f, 190f, BulletStyle.ICE, Tint.CYAN)
                b.freezeAt = 0.35f
                b.freezeTime = 0.8f
            }
        }
        if (every(t, dt, 2.4f, offset = 1.5f)) w.aimed(x, y - 20f, 360f, BulletStyle.ORB, Tint.CYAN)
    }
}

// ───────────── 第 7 關：尤加利樹林 ─────────────

/** 葉片手裏劍：轉著飛下來、左右蛇行，再追著玩家衝過去（中速、很會追）。 */
class LeafStar(x: Float, private val delay: Float) :
    Diver(x, -50f - delay * 160f, 14f, 32f, 80, diveAt = 1.1f + delay, diveSpeed = 480f, homing = 1.2f) {
    override val kind = EnemyKind.LEAF_STAR
    private val baseX = x
    val spin: Float get() = age * 10f

    override fun cruise(w: World, dt: Float) {
        y += 220f * dt
        x = baseX + sin(age * 4f + delay * 5f) * 40f
    }
}

/** 樹洞貓頭鷹：躲在樹樁裡，三向葉片（慢速）＋兩連發羽毛（快速）。 */
class OwlTurret(x: Float) : GroundShooter(x, 80f, 50f, 240) {
    override val kind = EnemyKind.OWL

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 2.2f, offset = 0.5f)) w.fan(x, y, w.aimAt(x, y), 3, 40f * DEG, 180f, BulletStyle.LEAF, Tint.GREEN)
        val c = t % 2.2f
        if (t > 1f && (crossed(c, dt, 1.3f) || crossed(c, dt, 1.5f))) w.aimed(x, y, 420f, BulletStyle.RICE, Tint.ORANGE)
    }
}

// ───────────── 第 8 關：清澈溪流 ─────────────

/** 跳躍魚：從溪裡一躍而出、劃著弧線下來，再朝玩家衝過去（快速、微追）。 */
class LeapFish(x: Float, private val delay: Float) :
    Diver(x, -50f - delay * 160f, 18f, 32f, 90, diveAt = 1.0f + delay, diveSpeed = 600f, homing = 0.4f) {
    override val kind = EnemyKind.LEAP_FISH
    private val baseX = x

    override fun cruise(w: World, dt: Float) {
        y += (180f + abs(cos(age * 3f)) * 160f) * dt
        x = baseX + sin(age * 3f + delay * 6f) * 50f
    }
}

/** 蛤蜊砲台：張開殼的時候吐出一圈珍珠泡泡（慢速）和瞄準的貝殼（快速）。 */
class ClamTurret(x: Float) : GroundShooter(x, 90f, 46f, 240) {
    override val kind = EnemyKind.CLAM
    /** 殼張開（畫面用）。 */
    val open: Boolean get() = (age % 2.6f) > 1.4f

    override fun fire(w: World, t: Float, dt: Float) {
        if (every(t, dt, 2.6f, offset = 0.6f)) w.ring(x, y, 8, 150f, age, BulletStyle.BUBBLE, Tint.CYAN)
        if (every(t, dt, 2.6f, offset = 1.2f)) {
            for (k in -1..1) w.shoot(x, y, w.aimAt(x, y) + k * 12f * DEG, 380f, BulletStyle.SHELL, Tint.PINK)
        }
    }
}

// ───────────── 第 6～8 關的菁英怪 ─────────────

/** 海豹教官：在冰上左右滑行，吹哨叫滑行小企鵝，扇形冰晶會在半空中凍住一下。 */
class EliteSeal(x: Float) : Elite(x, 470f, 66f, 2600, ItemType.SPREAD) {
    override val kind = EnemyKind.ELITE_SEAL
    private var vx = 220f

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.5f -> y = approach(y, 320f, 2.5f, dt)
            age < 12f -> {
                x += vx * dt
                if (x < 140f && vx < 0f || x > WORLD_WIDTH - 140f && vx > 0f) vx = -vx
                if (every(age, dt, 1.2f, offset = 1.8f)) {
                    for (k in -2..2) {
                        val b = w.shoot(x, y + 30f, w.aimAt(x, y) + k * 14f * DEG * 1.2f, 280f, BulletStyle.ICE, Tint.CYAN)
                        if (k % 2 == 0) {
                            b.freezeAt = 0.4f
                            b.freezeTime = 0.7f
                        }
                    }
                }
                if (crossed(age, dt, 4f) || crossed(age, dt, 8.5f)) for (i in 0 until 4) w.spawn(PenguinSlider(200f + i * 200f, i * 0.1f))
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 袋鼠拳擊手：跳來跳去，出拳是三連發快拳（快速）、落地震出一圈石子，還會叫葉片手裏劍來。 */
class EliteKangaroo(x: Float) : Elite(x, 480f, 66f, 2600, ItemType.HOMING) {
    override val kind = EnemyKind.ELITE_KANGAROO
    private var hopFromX = x
    private var hopToX = x
    private var hopT = 1f

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.5f -> y = approach(y, 320f, 2.5f, dt)
            age < 12f -> {
                if (every(age, dt, 1.7f, offset = 1.5f)) {
                    hopFromX = x
                    hopToX = 150f + w.random.nextFloat() * (WORLD_WIDTH - 300f)
                    hopT = 0f
                }
                if (hopT < 1f) {
                    hopT = (hopT + dt / 0.5f).coerceAtMost(1f)
                    x = hopFromX + (hopToX - hopFromX) * hopT
                    y = 320f - sin(hopT * HALF_PI * 2f) * 120f
                    if (hopT >= 1f) w.ring(x, y, 12, 170f, age, BulletStyle.ORB, Tint.ORANGE)
                }
                val c = age % 1.7f
                if (age > 2f && (crossed(c, dt, 0.9f) || crossed(c, dt, 1.05f) || crossed(c, dt, 1.2f))) w.aimed(x, y + 30f, 430f, BulletStyle.BIG_ORB, Tint.RED)
                if (crossed(age, dt, 5f)) for (i in 0 until 5) w.spawn(LeafStar(200f + i * 150f, i * 0.12f))
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}

/** 河狸工程師：一邊巡邏一邊丟木頭（拋物線）、撒木屑扇，還會放跳躍魚過來。 */
class EliteBeaver(x: Float) : Elite(x, 500f, 68f, 2600, ItemType.FOCUS) {
    override val kind = EnemyKind.ELITE_BEAVER
    private val baseX = x

    override fun update(w: World, dt: Float) {
        age += dt
        when {
            age < 1.5f -> y = approach(y, 330f, 2.5f, dt)
            age < 12f -> {
                x = baseX + sin((age - 1.5f) * 0.8f) * 200f
                if (every(age, dt, 1.1f, offset = 1.8f)) w.fan(x, y + 30f, w.aimAt(x, y), 4, 40f * DEG, 300f, BulletStyle.RICE, Tint.ORANGE)
                if (every(age, dt, 2.6f, offset = 2.4f)) {
                    for (k in -1..1) {
                        val dx = (w.player.x - x) / 1.6f + k * 120f
                        w.bullet(x, y, dx, -260f, BulletStyle.BIG_ORB, Tint.ORANGE).ay = 380f
                    }
                }
                if (crossed(age, dt, 5f)) for (i in 0 until 5) w.spawn(LeapFish(200f + i * 150f, i * 0.12f))
            }
            else -> y += 240f * dt
        }
        leaveIfOffscreen(w)
    }
}
