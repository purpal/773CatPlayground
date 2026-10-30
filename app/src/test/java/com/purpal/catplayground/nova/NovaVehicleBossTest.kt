package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign

class NovaVehicleBossTest {
    private fun world(stage:Int, hp:Float=1f, height:Float=1800f, cat:CatType=CatType.ORANGE, bout:Int=0):World {
        val w=World(NovaEncounters.stage(Stages.get(stage)!!),cat,height,difficulty=Difficulty.forBout(bout))
        NovaPreview.boss(w,hp)
        return w
    }
    private fun until(w:World,limit:Int=1800,condition:()->Boolean) {
        var n=0
        while(!condition() && n++<limit) { w.update(STEP);w.cues.clear() }
        assertTrue("condition timed out: stage ${w.stage.number}",condition())
    }

    @Test fun novaFactoriesAreIsolatedAndOtherStagesKeepOriginalInstances() {
        assertTrue(Stages.rabbit.createBoss() is RabbitBoss)
        assertTrue(Stages.penguin.createBoss() is PenguinBoss)
        assertTrue(world(5).boss is NovaRabbitBoss)
        assertTrue(world(6).boss is NovaPenguinBoss)
        for(i in 1..13) if(i !in 5..10) assertSame(Stages.get(i),NovaEncounters.stage(Stages.get(i)!!))
        assertEquals(Stages.rabbit.script.events.size,NovaEncounters.stage(Stages.rabbit).script.events.size)
        assertEquals(Stages.penguin.script.events.size+1,NovaEncounters.stage(Stages.penguin).script.events.size)
    }

    @Test fun formsTransformExactlyOnceIncludingLargeDamageAndHealing() {
        for(stage in 5..6) {
            val w=world(stage)
            val b=w.boss as NovaVehicleBoss
            b.hp=b.maxHp*(if(stage==5).401f else .551f)
            repeat(5){w.update(STEP)}
            assertEquals(0,b.form)
            assertFalse(b.transforming)
            b.hp=b.maxHp*.1f
            w.update(STEP)
            assertTrue(b.transforming)
            assertTrue(b.invulnerable)
            assertFalse(b.solid)
            assertTrue(w.lasers.isEmpty())
            assertTrue(w.bullets.isEmpty())
            repeat(110){w.update(STEP)}
            assertEquals(1,b.form)
            assertFalse(b.transforming)
            assertFalse(b.invulnerable)
            assertFalse(b.broken) // not a cosmetic relabeling of the legacy armor state
            assertNull(b.heldProp())
            b.hp=b.maxHp*.95f
            repeat(30){w.update(STEP)}
            b.hp=b.maxHp*.05f
            repeat(180){w.update(STEP)}
            assertEquals(1,b.transformations)
        }
    }

    @Test fun rabbitLocksBothLandingsAndExposesRecoveryWithoutSweepAttacks() {
        for(bout in listOf(0,12)) {
            val w=world(5,.3f,bout=bout)
            val b=w.boss as NovaRabbitBoss
            until(w){b.motion==NovaRabbitBoss.Motion.LOCK}
            val locked=listOf(b.targetX,b.targetY,b.nextX,b.nextY)
            w.player.x=80f;w.player.y=140f
            repeat(30){w.update(STEP)}
            assertEquals(locked,listOf(b.targetX,b.targetY,b.nextX,b.nextY))
            assertEquals(NovaRabbitBoss.Motion.LOCK,b.motion)
            until(w){b.motion==NovaRabbitBoss.Motion.FLIGHT}
            assertFalse(b.solid)
            until(w){b.cooling}
            assertEquals(locked[0],b.x,.01f)
            assertEquals(locked[1],b.y,.01f)
            assertTrue(b.damageScale>1f)
            until(w){b.motion==NovaRabbitBoss.Motion.LOCK && b.jumpNumber==2}
            assertEquals(locked[2],b.targetX,.01f)
            assertEquals(locked[3],b.targetY,.01f)
            assertTrue(w.lasers.isEmpty())
        }
    }

    @Test fun rabbitWarningCoversContactAndLeavesTimeForDamagedOrangeToEscape() {
        for(height in listOf(1200f,1800f,2200f)) {
            val w=world(5,.3f,height)
            val b=w.boss as NovaRabbitBoss
            until(w){b.motion==NovaRabbitBoss.Motion.LOCK}
            w.player.armor=w.player.cat.maxArmor*.05f
            assertTrue(b.landingRadius>=b.radius*.9f)
            assertTrue(w.player.speed*NovaRabbitBoss.WARN>b.landingRadius+w.player.radius+30f)
            assertTrue(b.targetX-b.landingRadius>=-10f)
            assertTrue(b.targetX+b.landingRadius<=WORLD_WIDTH+10f)
            assertTrue(b.targetY<w.playerMaxY)
        }
    }

    @Test fun iceRowsKeepAnOverlappingPassageAndThawBeforeBroadsides() {
        for(height in listOf(1200f,1800f,2200f)) for(bout in listOf(0,12)) {
            val w=world(6,.4f,height,bout=bout)
            val b=w.boss as NovaPenguinBoss
            repeat(1800) {
                w.update(STEP)
                val pylons=w.enemies.filterIsInstance<NovaIcePylon>().filter{it.alive && it.owner===b}
                val gap=(b.gapX+b.secondGapX)/2f
                assertTrue(pylons.all { abs(it.x-gap)>it.radius+w.player.radius+30f })
                if(pylons.any{it.active}) assertTrue("no surprise fire inside blockade",w.bullets.isEmpty())
                assertTrue(w.lasers.isEmpty())
            }
        }
    }

    @Test fun projectorsCanBeDestroyedAndTheirOwnerCancelsThemDuringTransform() {
        val w=world(6)
        val b=w.boss as NovaPenguinBoss
        until(w){w.enemies.any{it is NovaIcePylon && it.alive}}
        val first=w.enemies.filterIsInstance<NovaIcePylon>().first{it.alive}
        w.damageEnemy(first,first.hp+1f)
        assertFalse(first.alive)
        b.hp=b.maxHp*.2f
        w.update(STEP)
        assertTrue(b.transforming)
        assertTrue(w.enemies.filterIsInstance<NovaIcePylon>().none{it.alive && it.owner===b})
    }

    @Test fun killingDuringTransformStillReachesDialogueAndClear() {
        for(stage in 5..6) {
            val w=world(stage)
            val b=w.boss as NovaVehicleBoss
            b.hp=b.maxHp*.1f;w.update(STEP)
            assertTrue(b.transforming)
            w.damageEnemy(b,b.maxHp*2f)
            until(w){w.phase==Phase.OUTRO_DIALOG}
            assertTrue(w.enemies.filterIsInstance<NovaIcePylon>().none{it.alive})
            w.dialogFinished()
            assertEquals(Phase.CLEAR,w.phase)
        }
    }

    @Test fun bothVehiclesCanBeDefeatedWithBaseWeaponsForBothCats() {
        for(stage in 5..6) for(cat in CatType.entries) {
            val w=world(stage,cat=cat)
            w.firing=true
            var t=0f
            while(w.phase==Phase.BOSS && t<360f) {
                val b=w.boss!!
                w.moveX=if(abs(b.x-w.player.x)>20f)sign(b.x-w.player.x) else 0f
                w.update(STEP);w.cues.clear();t+=STEP
            }
            assertEquals("stage $stage $cat after $t",Phase.BOSS_DEFEATED,w.phase)
        }
    }
}
