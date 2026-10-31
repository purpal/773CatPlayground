package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*

/** Art routing for Nova; encounter behavior is isolated by NovaEncounters. */
object NovaSector {
    fun background(stage: Int) = when(stage) { 2 -> "background"; 3 -> "background_bear"; 4 -> "background_capy"; 5 -> "background_rabbit"; 6 -> "background_penguin"; 7 -> "background_koala"; 8 -> "background_otter"; 9 -> "background_panda"; 10 -> "background_sloth"; 11 -> "background_raccoon"; 12 -> "background_monkey"; else -> null }
    fun pilot(stage: Int) = when(stage) { 2 -> "pilot_dog"; 3 -> "pilot_bear"; 4 -> "pilot_capy"; 5 -> "pilot_rabbit"; 6 -> "pilot_penguin"; 7 -> "pilot_koala"; 8 -> "pilot_otter"; 9 -> "pilot_panda"; 10 -> "pilot_sloth"; 11 -> "pilot_raccoon"; 12 -> "pilot_monkey"; else -> null }
    fun preview(stage: Int) = when(stage) { 2 -> "boss"; 3 -> "BEAR_HEALTHY"; 4 -> "CAPY_HEALTHY"; 5 -> "RABBIT_BASE"; 6 -> "PENGUIN_BASE"; 7 -> "KOALA_BASE"; 8 -> "OTTER_BASE"; 9 -> "PANDA_BASE"; 10 -> "SLOTH_BASE"; 11 -> "RACCOON_BASE"; 12 -> "MONKEY_BASE"; else -> null }
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
        5 -> when(style) {
            BulletStyle.RICE, BulletStyle.SEED, BulletStyle.HONEY -> "RABBIT_BOLT"
            BulletStyle.STAR -> "RABBIT_STAR"
            else -> "RABBIT_ORB"
        }
        6 -> when(style) {
            BulletStyle.ICE, BulletStyle.RICE -> "PENGUIN_ICE"
            BulletStyle.FISH, BulletStyle.SEED -> "PENGUIN_TORPEDO"
            else -> "PENGUIN_ORB"
        }
        7 -> when(style) { BulletStyle.SEED, BulletStyle.RICE -> "KOALA_BOLT"; else -> "KOALA_ORB" }
        8 -> when(style) { BulletStyle.FISH, BulletStyle.RICE -> "OTTER_TORPEDO"; else -> "OTTER_ORB" }
        9 -> when(style) { BulletStyle.BAMBOO, BulletStyle.RICE -> "PANDA_DART"; else -> "PANDA_ORB" }
        10 -> when(style) { BulletStyle.LETTER, BulletStyle.RICE -> "SLOTH_DART"; else -> "SLOTH_ORB" }
        11 -> when(style) { BulletStyle.CARD, BulletStyle.RICE -> "RACCOON_DART"; else -> "RACCOON_ORB" }
        12 -> when(style) { BulletStyle.RICE, BulletStyle.SEED -> "MONKEY_DART"; else -> "MONKEY_ORB" }
        else -> null
    }
    fun assets(stage: Int): List<String> = when(stage) {
        3 -> listOf("BEAR_HEALTHY","BEAR_ALERT","BEAR_CRITICAL","BEAR_DISARMED","DIPPER_HEAD","HONEY_TANK_DROP","CLAW_ACTIVE","pilot_bear")
        4 -> listOf("CAPY_HEALTHY","CAPY_ARMORED","CAPY_CRITICAL","CAPY_BUOY_DROP","WATER_CANNON","WATER_ACTIVE","pilot_capy")
        5 -> listOf("RABBIT_BASE", "RABBIT_TRANSITION", "RABBIT_DEPLOYED", "RABBIT_EMITTER", "RABBIT_SHOCK", "pilot_rabbit")
        6 -> listOf("PENGUIN_BASE", "PENGUIN_TRANSITION", "PENGUIN_DEPLOYED", "PENGUIN_THAW", "pilot_penguin")
        7 -> listOf("KOALA_BASE", "KOALA_TRANSITION", "KOALA_DEPLOYED", "KOALA_HATCH", "KOALA_PULSE", "pilot_koala")
        8 -> listOf("OTTER_BASE", "OTTER_TRANSITION", "OTTER_DEPLOYED", "OTTER_PORT", "OTTER_STARBOARD", "OTTER_HATCH", "OTTER_WAKE", "pilot_otter")
        9 -> listOf("PANDA_BASE", "PANDA_TRANSITION", "PANDA_DEPLOYED", "PANDA_CORE", "PANDA_SHOCK", "pilot_panda")
        10 -> listOf("SLOTH_BASE", "SLOTH_TRANSITION", "SLOTH_DEPLOYED", "SLOTH_CORE", "SLOTH_FIELD", "pilot_sloth")
        11 -> listOf("RACCOON_BASE", "RACCOON_TRANSITION", "RACCOON_DEPLOYED", "RACCOON_HOLOGRAM", "RACCOON_SNARE", "pilot_raccoon")
        12 -> listOf("MONKEY_BASE", "MONKEY_TRANSITION", "MONKEY_DEPLOYED", "MONKEY_CORE", "MONKEY_TOTEM", "pilot_monkey")
        else -> emptyList()
    } + BulletStyle.entries.mapNotNull { projectile(stage,it) }.distinct()
}
