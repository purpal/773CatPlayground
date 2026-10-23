package com.purpal.catplayground.shooter.model

import kotlin.math.cos
import kotlin.math.sin

/** 玩家的主武器與副武器，等級 1 開始（陣列以 level - 1 取值）。 */
object Weapons {

    private val spreadCount = intArrayOf(3, 5, 7, 7, 9)
    private val spreadAngle = floatArrayOf(18f, 28f, 38f, 46f, 56f)
    private val focusStreams = intArrayOf(1, 2, 3, 4, 5)
    private val focusDamage = floatArrayOf(12f, 10f, 9f, 8.5f, 8f)
    private val pawRadius = floatArrayOf(20f, 24f, 28f, 32f, 36f)
    private val pawDamage = floatArrayOf(26f, 32f, 38f, 44f, 52f)
    private val needleStreams = intArrayOf(1, 1, 2, 2, 3)
    private val needleDamage = floatArrayOf(5f, 6f, 6f, 7f, 7f)

    fun fireMain(w: World, p: Player) {
        val level = p.mainLevel.coerceIn(1, Player.MAX_MAIN_LEVEL) - 1
        val power = if (p.transformed > 0f) 1.5f else 1f
        val x = p.x
        val y = p.y - 40f
        when (p.mainWeapon) {
            MainWeapon.SPREAD -> {
                val count = spreadCount[level]
                val total = spreadAngle[level] * DEG
                for (i in 0 until count) {
                    val a = -HALF_PI - total / 2f + total * i / (count - 1)
                    w.playerShots += PlayerShot(x, y, cos(a) * 1300f, sin(a) * 1300f, (5f + level) * power, 9f, ShotStyle.PELLET)
                }
                p.fireTimer = 0.1f
            }
            MainWeapon.FOCUS -> {
                val streams = focusStreams[level]
                for (i in 0 until streams) {
                    val offset = (i - (streams - 1) / 2f) * 16f
                    w.playerShots += PlayerShot(x + offset, y, 0f, -1500f, focusDamage[level] * power, 10f, ShotStyle.FOCUS)
                }
                p.fireTimer = 0.08f
            }
            MainWeapon.SPECIAL -> if (p.cat == CatType.ORANGE) {
                // 橘貓專武：大尺寸肉球彈，可以貫穿小怪。
                w.playerShots += PlayerShot(x, y - 10f, 0f, -900f, pawDamage[level] * power, pawRadius[level], ShotStyle.PAW, pierce = 2)
                if (level >= 2) {
                    for (side in listOf(-1f, 1f)) {
                        val a = -HALF_PI + side * 14f * DEG
                        w.playerShots += PlayerShot(x + side * 20f, y, cos(a) * 1000f, sin(a) * 1000f, 8f * power, 12f, ShotStyle.PAW)
                    }
                }
                p.fireTimer = 0.17f
            } else {
                // 黑貓專武：高射速爪擊彈。
                val streams = needleStreams[level]
                for (i in 0 until streams) {
                    val offset = (i - (streams - 1) / 2f) * 14f + w.jitter(5f)
                    w.playerShots += PlayerShot(x + offset, y, 0f, -1750f, needleDamage[level] * power, 7f, ShotStyle.NEEDLE)
                }
                p.fireTimer = 0.05f
            }
        }
    }

    fun fireSub(w: World, p: Player) {
        val level = p.subLevel.coerceIn(1, Player.MAX_SUB_LEVEL)
        when (p.subWeapon) {
            SubWeapon.HOMING -> {
                val count = level * 2
                for (i in 0 until count) {
                    val side = if (i % 2 == 0) -1f else 1f
                    val a = -HALF_PI + side * (50f + (i / 2) * 18f) * DEG
                    w.playerShots += PlayerShot(p.x + side * 26f, p.y, cos(a) * 420f, sin(a) * 420f, 10f, 10f, ShotStyle.MISSILE)
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
