package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class NovaCombatPresentationTest {
    @Test fun thirteenOpeningsDifferAndSupplyAndElitesRemainAvailable() {
        val signatures=mutableSetOf<String>()
        for(stage in 1..13) {
            val original=Stages.get(stage)!!
            val script=NovaEncounters.stage(original).script
            val w=World(original,CatType.ORANGE,1800f)
            for(e in script.sorted().filter{it.time<5f})e.action(w)
            signatures+=w.enemies.joinToString { "${it.x},${it.y},${(it as? NovaRaider)?.entry}" }
            assertTrue(w.enemies.any{it is NovaRaider})
            w.enemies.clear()
            script.sorted().forEach{it.action(w)}
            assertEquals(2,w.enemies.count{it.isElite})
            assertTrue(w.enemies.filterIsInstance<Balloon>().map{it.drop}.containsAll(listOf(ItemType.REPAIR,ItemType.SONIC,ItemType.HOMING)))
            assertTrue(script.length in 70f..76f)
        }
        assertEquals(13,signatures.size)
    }
    @Test fun rearEnemiesOvertakeWithoutContactThenTurnAndFireFromAhead() {
        for(height in listOf(1200f,1800f,2200f)) {
            val w=World(Stages.dog,CatType.ORANGE,height);w.player.y=height*.8f
            val e=NovaRaider(EnemyKind.PUPPY,NovaWaves.Entry.REAR,0,500f,height)
            repeat(30){e.update(w,STEP)}
            assertFalse(e.hurtsOnContact);assertTrue(w.bullets.isEmpty());assertTrue(e.alive)
            var frames=0
            while(w.bullets.isEmpty()&&frames++<600)e.update(w,STEP)
            assertTrue(e.hurtsOnContact);assertTrue(e.y<w.player.y)
            assertTrue(w.bullets.isNotEmpty());assertEquals(0f,e.facingDegrees,.01f)
            repeat(900){e.update(w,STEP)};assertFalse(e.alive)
        }
    }
    @Test fun mouseWingsReaimAfterPlayerMovesAndExpire() {
        val w=World(Stages.mouse,CatType.ORANGE,1800f);val boss=NovaMouseBoss()
        val m=NovaSwarmMouse(boss,100f,300f,220f)
        w.player.x=150f;w.player.y=1400f
        repeat(90){m.update(w,STEP)}
        w.player.x=900f
        val x=m.x;repeat(120){m.update(w,STEP)}
        assertTrue("must chase the new player location",m.x>x+100f)
        w.player.x=100f;val right=m.x
        repeat(180){m.update(w,STEP)}
        assertTrue("must retarget again",m.x<right)
        repeat(900){m.update(w,STEP)};assertFalse(m.alive)
    }
    @Test fun slothSpacingBlocksEvenBlackCatWithoutOverlappingArtworkHeavily() {
        val w=World(NovaEncounters.stage(Stages.sloth),CatType.BLACK,1800f,novaBalance=true)
        NovaPreview.boss(w,1f)
        while(w.bullets.isEmpty())w.update(STEP)
        val row=w.bullets.sortedBy{it.x}
        assertEquals(15,row.size)
        for((left,right) in row.zipWithNext()) {
            val spacing=right.x-left.x
            assertTrue(spacing<(left.radius+right.radius)*.75f+2*w.player.radius)
            assertTrue(spacing>(left.radius+right.radius)*.95f)
        }
        assertTrue(row.first().x<40f+row.first().radius)
        assertTrue(row.last().x>960f-row.last().radius)
    }
    @Test fun overdriveRemainsTranslucentAndFadesCompletely() {
        assertEquals(0f,NovaOverdrivePulse.alpha(0f),0f)
        assertEquals(0f,NovaOverdrivePulse.alpha(1f),0f)
        assertTrue((0..100).all{NovaOverdrivePulse.alpha(it/100f) in 0f.. .48f})
        assertTrue(NovaOverdrivePulse.alpha(.8f)<NovaOverdrivePulse.alpha(.3f))
    }
}
