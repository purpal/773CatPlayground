package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Art routing only: original encounter timing, collision and damage stay in World. */
object NovaSector {
    fun background(stage: Int) = when(stage) { 2 -> "background"; 3 -> "background_bear"; 4 -> "background_capy"; else -> null }
    fun pilot(stage: Int) = when(stage) { 2 -> "pilot_dog"; 3 -> "pilot_bear"; 4 -> "pilot_capy"; else -> null }
    fun preview(stage: Int) = when(stage) { 2 -> "boss"; 3 -> "BEAR_HEALTHY"; 4 -> "CAPY_HEALTHY"; else -> null }
    fun body(kind: EnemyKind, mood: Int, broken: Boolean, phase: Int = mood): String? = when(kind) {
        EnemyKind.BOSS_DOG -> NovaBossArt.body(mood,broken)
        EnemyKind.BOSS_BEAR -> when { broken -> "BEAR_DISARMED"; mood==2 -> "BEAR_CRITICAL"; mood==1 -> "BEAR_ALERT"; else -> "BEAR_HEALTHY" }
        EnemyKind.BOSS_CAPY -> when { !broken -> "CAPY_HEALTHY"; phase==2 -> "CAPY_CRITICAL"; else -> "CAPY_ARMORED" }
        else -> null
    }
    fun projectile(stage: Int, style: BulletStyle): String? = when(stage) {
        3 -> when(style) {
            BulletStyle.HONEY, BulletStyle.RICE, BulletStyle.STAR -> "BEAR.BulletStyle.${style.name}"
            BulletStyle.ORB, BulletStyle.BIG_ORB -> "BEAR.BulletStyle.ORB"
            else -> null
        }
        4 -> when(style) {
            BulletStyle.BUBBLE, BulletStyle.SEED, BulletStyle.BIG_ORB, BulletStyle.HONEY -> "CAPY.BulletStyle.${style.name}"
            BulletStyle.ORB -> "CAPY.BulletStyle.BUBBLE"
            BulletStyle.STAR -> "BEAR.BulletStyle.STAR" // visiting bee escort
            else -> null
        }
        else -> null
    }
    fun assets(stage: Int): List<String> = when(stage) {
        3 -> listOf("BEAR_HEALTHY","BEAR_ALERT","BEAR_CRITICAL","BEAR_DISARMED","DIPPER_HEAD","HONEY_TANK_DROP","CLAW_ACTIVE","pilot_bear")
        4 -> listOf("CAPY_HEALTHY","CAPY_ARMORED","CAPY_CRITICAL","CAPY_BUOY_DROP","WATER_CANNON","WATER_ACTIVE","pilot_capy")
        else -> emptyList()
    } + BulletStyle.entries.mapNotNull { projectile(stage,it) }.distinct()
}
