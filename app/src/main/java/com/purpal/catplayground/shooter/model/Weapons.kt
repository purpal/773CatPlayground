package com.purpal.catplayground.shooter.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 玩家的主武器與副武器，等級 1 開始（陣列以 level - 1 取值）。 */
object Weapons {

    private val spreadCount = intArrayOf(3, 5, 7, 7, 9)
    private val spreadAngle = floatArrayOf(18f, 28f, 38f, 46f, 56f)
    private val beamWidth = floatArrayOf(26f, 34f, 44f, 54f, 66f)
    private val beamDps = floatArrayOf(150f, 210f, 280f, 350f, 430f)
    private val pawRadius = floatArrayOf(20f, 24f, 28f, 32f, 36f)
    private val pawDamage = floatArrayOf(26f, 32f, 38f, 44f, 52f)
    private val clawStreams = intArrayOf(1, 1, 2, 2, 3)
    private val clawDamage = floatArrayOf(5f, 6f, 6f, 7f, 7f)

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
                // 黑貓專武：高射速爪擊。
                val streams = clawStreams[level]
                for (i in 0 until streams) {
                    val offset = (i - (streams - 1) / 2f) * 18f + w.jitter(4f)
                    w.playerShots += PlayerShot(x + offset, y, 0f, -1800f, clawDamage[level] * power, 9f, ShotStyle.CLAW)
                }
                p.fireTimer = 0.05f
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
