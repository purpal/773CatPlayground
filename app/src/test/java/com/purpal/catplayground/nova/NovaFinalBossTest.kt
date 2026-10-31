package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class NovaFinalBossTest {
    private fun world(stage:Int,hp:Float=1f,cat:CatType=CatType.ORANGE,height:Float=1800f):World {
        val w=World(NovaEncounters.stage(Stages.get(stage)!!),cat,height,difficulty=Difficulty.forBout(12),novaBalance=true)
        NovaPreview.boss(w,hp);return w
    }
    private fun until(w:World,limit:Int=3600,condition:()->Boolean) {
        var n=0;while(!condition()&&n++<limit){w.update(STEP);w.cues.clear()}
        assertTrue("stage ${w.stage.number} timed out",condition())
    }
    @Test fun factoriesAreIsolatedAndEveryTourOpponentAppearsOnce() {
        assertTrue(Stages.pig.createBoss() is PigBoss);assertTrue(Stages.mouse.createBoss() is MouseBoss)
        assertTrue(world(13).boss is NovaPigBoss);assertTrue(world(1).boss is NovaMouseBoss)
        for(stage in listOf(1,13))assertEquals(Stages.get(stage)!!.script.events.size+1,NovaEncounters.stage(Stages.get(stage)!!).script.events.size)
        val tour=NovaTour.start(CatType.BLACK)
        assertEquals((2..13).toList()+1,tour.order)
    }
    @Test fun boresLockPositionsAndLeaveReachableLowerEscapeLane() {
        for(height in listOf(1200f,1800f,2200f))for(cat in CatType.entries) {
            val w=world(13,.3f,cat,height)
            until(w){w.enemies.filterIsInstance<NovaBore>().count{it.alive}==3}
            val bores=w.enemies.filterIsInstance<NovaBore>().filter{it.alive}
            val points=bores.map{it.x to it.y};w.player.x=80f
            repeat(30){w.update(STEP)}
            assertEquals(points,bores.map{it.x to it.y})
            assertTrue(bores.all{it.y+NovaBore.RADIUS+w.player.radius<w.playerMaxY})
            w.player.armor=0f
            assertTrue(w.player.speed*.7f*NovaBore.WARN>NovaBore.RADIUS+w.player.radius)
        }
    }
    @Test fun drillingWarningDoesNoDamageAndFieldExpiresWithoutStacking() {
        val w=world(13);w.godMode=false
        val bore=NovaBore(null,0,500f,1000f,0f);w.spawn(bore)
        w.player.x=500f;w.player.y=1000f;val armor=w.player.armor
        repeat(60){bore.update(w,STEP)}
        assertEquals(armor,w.player.armor,0f);assertEquals(1f,bore.playerSpeedFactor(500f,1000f),0f)
        repeat(75){bore.update(w,STEP)}
        assertTrue(w.player.armor<armor);assertEquals(.7f,bore.playerSpeedFactor(500f,1000f),0f)
        assertEquals(1f,bore.playerSpeedFactor(700f,1000f),0f)
        repeat(300){bore.update(w,STEP)}
        assertFalse(bore.alive);assertEquals(1f,bore.playerSpeedFactor(500f,1000f),0f)
    }
    @Test fun breakingDrillArmCancelsItsBoresAndNextSalvo() {
        val w=world(13,.3f);val boss=w.boss as NovaPigBoss
        until(w){boss.form==1&&boss.arms.size==2}
        val arm=boss.arms.first();w.damageEnemy(arm,10000f)
        assertTrue(w.enemies.filterIsInstance<NovaBore>().none{it.alive&&it.side==arm.side})
        val cycle=boss.cycle;until(w){boss.cycle>cycle&&boss.clock>1f}
        assertEquals(2,w.enemies.filterIsInstance<NovaBore>().count{it.alive})
        assertTrue(w.enemies.filterIsInstance<NovaBore>().none{it.alive&&it.side==arm.side})
    }
    @Test fun activeBoreCountStaysBoundedAndTransformClearsHazards() {
        val w=world(13);val boss=w.boss as NovaPigBoss
        repeat(1800){w.update(STEP);assertTrue(w.enemies.filterIsInstance<NovaBore>().count{it.alive}<=2)}
        boss.hp=boss.maxHp*.4f;w.update(STEP)
        assertTrue(boss.transforming);assertTrue(w.enemies.filterIsInstance<NovaBore>().none{it.alive})
        until(w){boss.form==1}
        repeat(1800){w.update(STEP);assertTrue(w.enemies.filterIsInstance<NovaBore>().count{it.alive}<=3)}
    }
    @Test fun railGatesHaveSafeFixedGapAndCanBeDestroyed() {
        for(height in listOf(1200f,1800f,2200f)) {
            val w=world(1,.45f,height=height);val boss=w.boss as NovaMouseBoss
            until(w){boss.form==1&&w.enemies.filterIsInstance<NovaRailGate>().any{it.alive}}
            val gap=boss.gapX
            val gates=w.enemies.filterIsInstance<NovaRailGate>().filter{it.alive}
            assertTrue(gates.all{abs(it.x-gap)>=NovaRailGate.GAP_HALF})
            assertTrue(gates.all{abs(it.x-gap)-it.radius>w.player.radius+80f})
            val gate=gates.first();w.damageEnemy(gate,10000f);assertFalse(gate.alive)
            w.player.x=gap
            repeat(360){w.update(STEP);assertEquals(gap,boss.gapX,0f)}
        }
    }
    @Test fun destroyingBatteryOpensItsSideNowAndOnNextWave() {
        val w=world(1);val boss=w.boss as NovaMouseBoss
        val battery=boss.batteries.first();w.damageEnemy(battery,10000f)
        assertTrue(w.enemies.filterIsInstance<NovaRailGate>().none{it.alive&&it.side==battery.side})
        until(w){boss.cycle==1&&boss.clock>1f}
        assertTrue(w.enemies.filterIsInstance<NovaRailGate>().none{it.alive&&it.side==battery.side})
    }
    @Test fun finalCoreAwakensOnceAndClearsOldBatteriesAndGates() {
        for(combined in listOf(false,true)) {
            val w=world(1);val boss=w.boss as NovaMouseBoss
            boss.hp=boss.maxHp*if(combined).2f else .5f;w.update(STEP)
            assertTrue(boss.transforming);assertTrue(w.enemies.filterIsInstance<NovaRailGate>().none{it.alive})
            until(w){boss.form==1}
            if(!combined){assertFalse(boss.awakened);boss.hp=boss.maxHp*.2f;w.update(STEP);assertTrue(boss.awakeningTime>0f)}
            until(w){boss.awakened&&boss.awakeningTime<=0f}
            assertTrue(boss.batteries.isEmpty());assertEquals(1,boss.awakenings)
            boss.hp=boss.maxHp*.8f;repeat(60){w.update(STEP)};boss.hp=boss.maxHp*.1f;repeat(60){w.update(STEP)}
            assertEquals(1,boss.awakenings);assertEquals(1,boss.transformations)
        }
    }
    @Test fun bothCatsCanFinishInitialWeaponsAndOutro() {
        for(stage in listOf(1,13))for(cat in CatType.entries) {
            val w=world(stage,cat=cat);w.firing=true;var t=0f
            while(w.phase==Phase.BOSS&&t<360f){val b=w.boss!!;w.moveX=if(abs(b.x-w.player.x)>20f)sign(b.x-w.player.x)else 0f;w.update(STEP);w.cues.clear();t+=STEP}
            assertEquals("stage $stage $cat hp=${w.boss!!.hp}",Phase.BOSS_DEFEATED,w.phase)
            until(w){w.phase==Phase.OUTRO_DIALOG};assertTrue(w.enemies.none{it.alive&&!it.isBoss})
            w.dialogFinished();assertEquals(Phase.CLEAR,w.phase)
        }
    }
    @Test fun defeatDuringTransformationAlwaysClearsParts() {
        for(stage in listOf(1,13)) {
            val w=world(stage);val b=w.boss as NovaVehicleBoss;b.hp=b.maxHp*.1f;w.update(STEP)
            assertTrue(b.transforming);w.damageEnemy(b,b.maxHp*10f)
            until(w){w.phase==Phase.OUTRO_DIALOG};assertTrue(w.enemies.none{it.alive&&!it.isBoss})
        }
    }
}
