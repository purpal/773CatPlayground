package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test

class NovaSectorTest {
    @Test fun bearDisarmsAtQuarterHealthWhileCapyAddsArmorAtHalf() {
        val cases=listOf(
            Triple(3,.8f,"BEAR_HEALTHY"),Triple(3,.4f,"BEAR_ALERT"),Triple(3,.25f,"BEAR_DISARMED"),
            Triple(4,.8f,"CAPY_HEALTHY"),Triple(4,.5f,"CAPY_ARMORED"),Triple(4,.25f,"CAPY_CRITICAL"),Triple(4,.2f,"CAPY_CRITICAL"))
        for((stage,hp,expected) in cases) {
            val world=World(Stages.get(stage)!!,CatType.ORANGE,1800f)
            NovaPreview.boss(world,hp)
            val b=world.boss!!
            assertEquals(expected,NovaSector.body(b.kind,b.mood(),b.broken,b.phase))
            if(stage==3) assertEquals(if(hp>.25f)HazardStyle.DIPPER else null,b.heldProp())
            else assertEquals(if(hp<=.5f)HazardStyle.WATER else null,b.heldProp())
        }
    }
    @Test fun newArtCoversActualBossProjectilesAndSpecialAttackCycles() {
        for((stage,hp,weapon) in listOf(Triple(3,.8f,HazardStyle.DIPPER),Triple(3,.2f,HazardStyle.CLAW),Triple(4,.4f,HazardStyle.WATER))) {
            val world=World(Stages.get(stage)!!,CatType.BLACK,1800f)
            NovaPreview.boss(world,hp)
            val hazards=mutableSetOf<HazardStyle>()
            val bullets=mutableSetOf<BulletStyle>()
            repeat(1800) {
                world.update(STEP)
                hazards.addAll(world.lasers.map { it.style })
                bullets.addAll(world.bullets.map { it.style })
            }
            assertTrue("stage $stage hp $hp: $hazards",weapon in hazards)
            assertTrue(bullets.isNotEmpty())
            assertTrue("stage $stage missing projectiles: $bullets",bullets.all { NovaSector.projectile(stage,it)!=null })
        }
    }
}
