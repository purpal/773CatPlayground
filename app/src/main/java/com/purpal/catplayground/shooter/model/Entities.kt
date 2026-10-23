package com.purpal.catplayground.shooter.model

import kotlin.math.cos
import kotlin.math.sin

class Player(val cat: CatType) {
    var x = WORLD_WIDTH / 2f
    var y = 0f
    var vx = 0f
    var armor = cat.maxArmor
    var mainWeapon = MainWeapon.SPREAD
    var mainLevel = 1
    var subWeapon = SubWeapon.NONE
    var subLevel = 0
    var bombs = START_BOMBS
    var invulnerable = 0f
    /** 大絕變身剩餘秒數；變身中火力提升。 */
    var transformed = 0f
    var fireTimer = 0f
    var subTimer = 0f
    var alive = true
    val radius = 12f

    val armorRatio: Float get() = (armor / cat.maxArmor).coerceIn(0f, 1f)

    /** 中彈越多，護甲越低，速度也跟著變慢。 */
    val speed: Float get() = cat.baseSpeed * (0.55f + 0.45f * armorRatio)

    fun resetLoadout() {
        armor = cat.maxArmor
        mainWeapon = MainWeapon.SPREAD
        mainLevel = 1
        subWeapon = SubWeapon.NONE
        subLevel = 0
        bombs = START_BOMBS
    }

    companion object {
        const val START_BOMBS = 2
        const val MAX_BOMBS = 3
        const val MAX_MAIN_LEVEL = 5
        const val MAX_SUB_LEVEL = 3
    }
}

enum class ShotStyle { PELLET, PAW, CLAW, MISSILE, SONIC }

class PlayerShot(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val damage: Float,
    val radius: Float,
    val style: ShotStyle,
    var pierce: Int = 0,
) {
    var age = 0f
    var alive = true
    var target: Enemy? = null
    /** 貫穿彈已經打過的敵人，避免同一隻被重複扣血。 */
    val hits = HashSet<Int>()
    /** 聲波彈：飛到這個時間就展開成弧形牆。 */
    var burstAt = 0f
    var wallLevel = 0
}

enum class BulletStyle { ORB, BIG_ORB, RICE, CHEESE, BONE, NOTE, HONEY, STAR, SEED }

class EnemyBullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val style: BulletStyle,
    val color: Long,
    val radius: Float,
    val damage: Float,
) {
    var ax = 0f
    var ay = 0f
    /** 每秒轉彎的弧度，讓子彈走弧線（例如回力鏢骨頭）。 */
    var turn = 0f
    var age = 0f
    var alive = true
    /** 可被聲波牆擋下。 */
    var erasable = true
    val spin = (style == BulletStyle.BONE || style == BulletStyle.STAR || style == BulletStyle.CHEESE)
}

/**
 * 貫穿攻擊的外觀：BEAM 是光束，其他是 Boss 用肢體或道具的近身攻擊
 * （叉子突刺、骨頭橫掃、蜂蜜棒重擊、尾巴鞭打、爪擊）。
 */
enum class HazardStyle { BEAM, FORK, BONE, DIPPER, TAIL, CLAW }

/**
 * 雷射／近身攻擊：先閃爍預告，再發射；無法被聲波牆擋下，但大絕可以消掉。
 * 道具類攻擊會在 [extendTime] 內從發射者伸出去，只有伸到的部分有判定。
 */
class Laser(
    var x: Float,
    var y: Float,
    var angle: Float,
    val length: Float,
    val width: Float,
    val warnTime: Float,
    val activeTime: Float,
    val color: Long,
    val damage: Float,
    /** 每秒旋轉的弧度。 */
    val sweep: Float = 0f,
    /** 跟著發射者移動時，相對於發射者的位置。 */
    val owner: Enemy? = null,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val style: HazardStyle = HazardStyle.BEAM,
    val extendTime: Float = 0f,
) {
    var age = 0f
    var alive = true
    val active: Boolean get() = age >= warnTime && age < warnTime + activeTime
    val endX: Float get() = x + cos(angle) * length
    val endY: Float get() = y + sin(angle) * length

    /** 目前伸出去的比例（0～1）：發射時伸出，結束前 0.15 秒收回。 */
    val extension: Float
        get() {
            if (age < warnTime) return 0f
            val out = if (extendTime <= 0f) 1f else ((age - warnTime) / extendTime).coerceAtMost(1f)
            val back = if (extendTime <= 0f) 1f else ((warnTime + activeTime - age) / RETRACT).coerceIn(0f, 1f)
            return minOf(out, back)
        }
    val reach: Float get() = length * extension
    val tipX: Float get() = x + cos(angle) * reach
    val tipY: Float get() = y + sin(angle) * reach

    /** 點到「已伸出部分」的距離平方。 */
    fun distance2To(px: Float, py: Float): Float {
        val dx = tipX - x
        val dy = tipY - y
        if (dx * dx + dy * dy < 1f) return dist2(px, py, x, y)
        val t = (((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
        return dist2(px, py, x + dx * t, y + dy * t)
    }

    companion object {
        const val RETRACT = 0.15f
    }
}

/** 聲波彈展開後的弧形牆，弧的開口朝下、凸面朝向敵人。 */
class SonicWall(val x: Float, val y: Float, val radius: Float, val halfAngle: Float) {
    var age = 0f
    var alive = true
    val life = 2.4f
    val thickness = 26f
    val centerY: Float get() = y + radius

    fun blocks(px: Float, py: Float, r: Float): Boolean {
        val d2 = dist2(px, py, x, centerY)
        val inner = radius - thickness - r
        val outer = radius + thickness + r
        if (d2 < inner * inner || d2 > outer * outer) return false
        val a = angleTo(x, centerY, px, py) + HALF_PI // 正上方為 0
        return kotlin.math.abs(a) <= halfAngle
    }
}

/** 道具。武器膠囊會在同一組類型之間輪流切換（主武器 3 種、副武器 2 種）。 */
class Item(var x: Float, var y: Float, type: ItemType) {
    var type = type
        private set
    var age = 0f
    var alive = true
    var cycleTime = 0f
    /** 糖果的顏色。 */
    var variant = 0
    /** 消彈變成的糖果：先往外彈開，再飛向貓咪。 */
    var vx = 0f
    var vy = 0f
    val radius = when (type) {
        ItemType.COIN -> 20f
        ItemType.CANDY -> 16f
        else -> 30f
    }
    val cycle: List<ItemType>? = when (type) {
        in MAIN_CYCLE -> MAIN_CYCLE
        in SUB_CYCLE -> SUB_CYCLE
        else -> null
    }
    /** 切換到下一種的進度（0～1），畫面用來畫倒數圈。 */
    val cycleProgress: Float get() = cycleTime / CYCLE_TIME

    fun updateCycle(dt: Float) {
        val list = cycle ?: return
        cycleTime += dt
        if (cycleTime >= CYCLE_TIME) {
            cycleTime -= CYCLE_TIME
            type = list[(list.indexOf(type) + 1) % list.size]
        }
    }

    companion object {
        const val CYCLE_TIME = 1.6f
        val MAIN_CYCLE = listOf(ItemType.SPREAD, ItemType.FOCUS, ItemType.SPECIAL)
        val SUB_CYCLE = listOf(ItemType.HOMING, ItemType.SONIC)

        fun next(type: ItemType): ItemType {
            val list = when (type) {
                in MAIN_CYCLE -> MAIN_CYCLE
                in SUB_CYCLE -> SUB_CYCLE
                else -> return type
            }
            return list[(list.indexOf(type) + 1) % list.size]
        }
    }
}

/** Boss 被打掉的道具（畫面用來畫飛出去的樣子）。 */
object LostProp {
    const val CROWN = 1
    const val HAT = 2
    const val POT = 3
    const val FORK = 4
}

/** 畫面上的音效字。 */
object SfxText {
    const val POKO = 0
    const val PAAN = 1
    const val DOON = 2
    const val SWEET = 3
    const val ELITE = 4
}

enum class ParticleKind {
    SPARK, STAR, RING, HEART, SMOKE, FLASH, FIRE,
    /** 彩色紙片，會受重力往下飄。 */
    CONFETTI,
    /** 被打飛的 Boss 道具，tag 為 [LostProp]。 */
    PROP,
    /** 漫畫音效字，tag 為 [SfxText]。 */
    SFX,
    /** 彈跳的分數，tag 為分數。 */
    SCORE,
    /** 命中處開出的小花。 */
    FLOWER,
}

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val life: Float,
    val size: Float,
    val color: Long,
    val kind: ParticleKind,
    val tag: Int = 0,
) {
    var age = 0f
    /** 旋轉角度（度）與每秒轉速，紙片與道具用。 */
    var rotation = 0f
    var spin = 0f
    val progress: Float get() = (age / life).coerceIn(0f, 1f)
}
