package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test

class NovaBossArtTest {
    @Test fun realBossTransitionsSelectTheMatchingEquipmentAndBody() {
        val cases=listOf(.8f to "BODY_HEALTHY",.5f to "BODY_ALERT",.35f to "BODY_DISARMED",.25f to "BODY_CRITICAL")
        for((hp,art) in cases) {
            val world=World(Stages.dog,CatType.ORANGE,1800f)
            NovaPreview.boss(world,hp)
            val boss=world.boss!!
            assertEquals(Phase.BOSS,world.phase)
            assertEquals(art,NovaBossArt.body(boss.mood(),boss.broken))
            assertEquals(hp>.35f,boss.heldProp()!=null)
            if(hp<=.35f)assertTrue(world.enemies.none { it is ThrownBone })
        }
    }
}
