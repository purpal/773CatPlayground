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

enum class ShotStyle { PELLET, FOCUS, PAW, NEEDLE, MISSILE, SONIC }

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

/** 雷射：先閃爍預告，再發射；無法被聲波牆擋下，但大絕可以消掉。 */
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
) {
    var age = 0f
    var alive = true
    val active: Boolean get() = age >= warnTime && age < warnTime + activeTime
    val endX: Float get() = x + cos(angle) * length
    val endY: Float get() = y + sin(angle) * length

    /** 點到雷射線段的距離平方。 */
    fun distance2To(px: Float, py: Float): Float {
        val dx = endX - x
        val dy = endY - y
        val t = (((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
        return dist2(px, py, x + dx * t, y + dy * t)
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

class Item(var x: Float, var y: Float, val type: ItemType) {
    var age = 0f
    var alive = true
    val radius = if (type == ItemType.COIN) 20f else 30f
}

enum class ParticleKind { SPARK, STAR, RING, HEART, SMOKE, FLASH }

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val life: Float,
    val size: Float,
    val color: Long,
    val kind: ParticleKind,
) {
    var age = 0f
    val progress: Float get() = (age / life).coerceIn(0f, 1f)
}
