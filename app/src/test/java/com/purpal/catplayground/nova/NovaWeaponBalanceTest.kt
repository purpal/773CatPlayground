package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test

class NovaWeaponBalanceTest {
    @Test fun missilesChangeOnlyInNovaAtEveryLevel() {
        for(nova in listOf(false,true))for(level in 1..Player.MAX_SUB_LEVEL) {
            val w=World(Stages.get(2)!!,CatType.ORANGE,1800f,novaBalance=nova)
            w.player.subWeapon=SubWeapon.HOMING;w.player.subLevel=level
            Weapons.fireSub(w,w.player)
            assertEquals(level*2,w.playerShots.size)
            w.playerShots.forEach { assertEquals(if(nova)12.8f else 16f,it.damage,.001f) }
        }
    }
    @Test fun deployedShieldHasTwentyPercentMoreDurabilityOnlyInNova() {
        for(nova in listOf(false,true))for(level in 1..Player.MAX_SUB_LEVEL) {
            val w=World(Stages.get(2)!!,CatType.BLACK,1800f,novaBalance=nova)
            w.player.subWeapon=SubWeapon.SONIC;w.player.subLevel=level
            Weapons.fireSub(w,w.player)
            repeat(30){w.update(STEP)}
            val wall=w.walls.single()
            assertEquals((World.WALL_HP+level*World.WALL_HP_PER_LEVEL)*if(nova)1.2f else 1f,wall.maxHp,.001f)
            assertEquals(wall.maxHp,wall.hp,.001f)
        }
    }
}
