package com.purpal.catplayground.shooter.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 玩家的主武器與副武器，等級 1 開始（陣列以 level - 1 取值）。 */
object Weapons {

    private val spreadCount = intArrayOf(3, 5, 7, 7, 9)
    private val spreadAngle = floatArrayOf(18f, 28f, 38f, 46f, 56f)
    private val beamWidth = floatArrayOf(32f, 44f, 58f, 74f, 94f)
    private val beamDps = floatArrayOf(150f, 210f, 280f, 350f, 430f)
    private val pawRadius = floatArrayOf(20f, 24f, 28f, 32f, 36f)
    private val pawDamage = floatArrayOf(26f, 32f, 38f, 44f, 52f)
    /** 肉球閃電：輪廓大小、每秒繞幾圈、每次命中的傷害、閃電頭的判定半徑。 */
    private val pawBoltSize = floatArrayOf(90f, 104f, 118f, 132f, 148f)
    private val pawBoltLaps = floatArrayOf(0.9f, 1.15f, 1.4f, 1.7f, 2f)
    private val pawBoltDamage = floatArrayOf(24f, 30f, 36f, 44f, 52f)
    private val pawBoltReach = floatArrayOf(30f, 34f, 38f, 42f, 46f)
    private val point = FloatArray(2)

    private fun level(p: Player) = p.mainLevel.coerceIn(1, Player.MAX_MAIN_LEVEL) - 1

    private fun power(p: Player) = if (p.transformed > 0f) 1.5f else 1f

    fun fireMain(w: World, p: Player) {
        val level = level(p)
        val power = power(p)
        val x = p.x
        val y = p.y - 46f
        when (p.mainWeapon) {
            MainWeapon.SPREAD -> {
                val count = spreadCount[level]
                val total = spreadAngle[level] * DEG
                for (i in 0 until count) {
                    val a = -HALF_PI - total / 2f + total * i / (count - 1)
                    w.playerShots += PlayerShot(x, y, cos(a) * 1400f, sin(a) * 1400f, (5f + level) * power, 10f, ShotStyle.PELLET)
                }
                p.fireTimer = 0.1f
            }
            MainWeapon.FOCUS -> Unit // 光束在 updateBeam 處理
            MainWeapon.SPECIAL -> if (p.cat == CatType.ORANGE) {
                // 橘貓專武：巨大肉球砲，可以貫穿小怪。
                w.playerShots += PlayerShot(x, y - 10f, 0f, -950f, pawDamage[level] * power, pawRadius[level], ShotStyle.PAW, pierce = 2)
                if (level >= 2) {
                    for (side in listOf(-1f, 1f)) {
                        val a = -HALF_PI + side * 14f * DEG
                        w.playerShots += PlayerShot(x + side * 22f, y, cos(a) * 1050f, sin(a) * 1050f, 8f * power, 13f, ShotStyle.PAW)
                    }
                }
                p.fireTimer = 0.17f
            } else {
                // 黑貓專武：肉球閃電，在 updatePawBolt 處理
                p.fireTimer = 0.2f
            }
        }
    }

    /**
     * 集中型：從貓咪射出的連續光束，打到最近的敵人就停在那裡並持續造成傷害。
     * 回傳被打中的敵人（給特效用）。
     */
    fun updateBeam(w: World, p: Player, dt: Float): Enemy? {
        val level = level(p)
        val width = beamWidth[level]
        val bottom = p.y - 46f
        var target: Enemy? = null
        for (e in w.enemies) {
            if (!e.alive || e.invulnerable || e.y > bottom || e.y < -40f) continue
            if (abs(e.x - p.x) > e.radius * 0.8f + width / 2f) continue
            if (target == null || e.y > target.y) target = e
        }
        w.beam.active = true
        w.beam.x = p.x
        w.beam.bottom = bottom
        w.beam.width = width
        w.beam.top = target?.let { it.y + it.radius * 0.4f } ?: -60f
        if (target != null) w.damageEnemy(target, beamDps[level] * power(p) * dt)
        return target
    }

    /**
     * 黑貓專武：沿著貓掌輪廓繞著機體跑的閃電。等級越高繞得越快、範圍越大、傷害越高。
     * 只有閃電頭有判定；同一隻敵人每 [PAW_HIT_COOLDOWN] 秒最多被打一次。回傳這一步打中的敵人。
     */
    fun updatePawBolt(w: World, p: Player, dt: Float): Enemy? {
        val level = level(p)
        val b = w.pawBolt
        b.active = true
        b.level = level
        b.x = p.x
        b.y = p.y
        b.size = pawBoltSize[level]
        b.progress = (b.progress + pawBoltLaps[level] * dt) % 1000f
        PawPath.point(b.progress, point)
        b.headX = p.x + point[0] * b.size
        b.headY = p.y + point[1] * b.size
        val reach = pawBoltReach[level]
        var hit: Enemy? = null
        for (e in w.enemies) {
            if (!e.alive || e.invulnerable || e is Balloon) continue
            if (w.time - e.pawHitTime < PAW_HIT_COOLDOWN) continue
            val r = e.radius + reach
            if (dist2(b.headX, b.headY, e.x, e.y) > r * r) continue
            e.pawHitTime = w.time
            val crit = w.random.nextFloat() < World.CRIT_CHANCE
            val damage = pawBoltDamage[level] * power(p)
            w.damageEnemy(e, if (crit) damage * 2f else damage, crit)
            hit = e
        }
        return hit
    }

    const val PAW_HIT_COOLDOWN = 0.08f

    fun fireSub(w: World, p: Player) {
        val level = p.subLevel.coerceIn(1, Player.MAX_SUB_LEVEL)
        when (p.subWeapon) {
            SubWeapon.HOMING -> {
                val count = level * 2
                for (i in 0 until count) {
                    val side = if (i % 2 == 0) -1f else 1f
                    val a = -HALF_PI + side * (50f + (i / 2) * 18f) * DEG
                    w.playerShots += PlayerShot(p.x + side * 26f, p.y, cos(a) * 420f, sin(a) * 420f, 10f, 12f, ShotStyle.MISSILE)
                }
                p.subTimer = 0.6f
                w.cue(SoundCue.MISSILE)
            }
            SubWeapon.SONIC -> {
                val shot = PlayerShot(p.x, p.y - 40f, 0f, -900f, 0f, 16f, ShotStyle.SONIC)
                shot.burstAt = 0.26f
                shot.wallLevel = level
                w.playerShots += shot
                p.subTimer = 1.5f
            }
            SubWeapon.NONE -> Unit
        }
    }
}

/** 集中型光束目前的狀態（畫面用）。 */
class BeamState {
    var active = false
    var x = 0f
    var top = 0f
    var bottom = 0f
    var width = 0f
}
