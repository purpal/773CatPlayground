package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign

class NovaHarborBossTest {
    private fun world(stage:Int, hp:Float=1f, cat:CatType=CatType.ORANGE, height:Float=1800f, bout:Int=5):World {
        val w=World(NovaEncounters.stage(Stages.get(stage)!!),cat,height,difficulty=Difficulty.forBout(bout))
        NovaPreview.boss(w,hp)
        return w
    }
    private fun until(w:World, limit:Int=1800, condition:()->Boolean) {
        var n=0
        while(!condition() && n++<limit) { w.update(STEP); w.cues.clear() }
        assertTrue("stage ${w.stage.number} timed out",condition())
    }

    @Test fun factoriesAreNovaOnlyAndTutorialsAreAddedWithoutReplacingWaves() {
        assertTrue(Stages.koala.createBoss() is KoalaBoss)
        assertTrue(Stages.otter.createBoss() is OtterBoss)
        assertTrue(world(7).boss is NovaKoalaBoss)
        assertTrue(world(8).boss is NovaOtterBoss)
        for(stage in listOf(Stages.koala,Stages.otter)) {
            val nova=NovaEncounters.stage(stage)
            assertEquals(stage.script.events.size+1,nova.script.events.size)
            assertSame(stage.country,nova.country)
            assertSame(stage.outro,nova.outro)
        }
    }

    @Test fun destroyingCapacitorCancelsDischargeButLeavingItChargesAndLocksAim() {
        val w=world(7)
        val b=w.boss as NovaKoalaBoss
        until(w){w.enemies.filterIsInstance<NovaCapacitor>().count { it.alive && it.age<1f }==2}
        val nodes=w.enemies.filterIsInstance<NovaCapacitor>().filter { it.alive }
        assertEquals(2,nodes.size)
        val killed=nodes.first(); val live=nodes.last()
        w.damageEnemy(killed,10000f)
        until(w){live.warning}
        val aim=live.aim
        w.player.x=50f
        repeat(60){w.update(STEP)}
        assertEquals(aim,live.aim,0f)
        assertFalse(live.discharged)
        until(w){live.discharged}
        assertFalse(killed.discharged)
        assertTrue(w.bullets.any { it.style==BulletStyle.ORB })
        assertTrue(w.lasers.isEmpty())
    }

    @Test fun koalaWindowsDoubleDamageAndAlternateSideAfterAnchoring() {
        val w=world(7)
        val b=w.boss as NovaKoalaBoss
        until(w){b.cooling}
        var hp=b.hp; w.damageEnemy(b,100f); assertEquals(200f,hp-b.hp,.01f)
        b.hp=b.maxHp*.2f
        until(w){b.form==1 && b.cooling}
        val hatch=w.enemies.filterIsInstance<NovaKoalaHatch>().first{it.alive}
        assertFalse(hatch.invulnerable)
        hp=b.hp; w.damageEnemy(b,100f); assertEquals(100f,hp-b.hp,.01f)
        hp=b.hp; w.damageEnemy(hatch,100f); assertEquals(200f,hp-b.hp,.01f)
        val x=hatch.x
        until(w){!b.cooling}
        assertTrue(hatch.invulnerable)
        until(w){b.cooling}
        assertTrue((x-b.x)*(hatch.x-b.x)<0f)
        assertEquals(1,w.enemies.filterIsInstance<NovaCapacitor>().count{it.alive})
    }

    @Test fun sonarBuoyCanBeRemovedBeforeItsSingleVolley() {
        val w=world(8)
        val b=w.boss as NovaOtterBoss
        until(w){w.enemies.any { it is NovaSonarBuoy && it.alive && it.warning }}
        val buoy=w.enemies.filterIsInstance<NovaSonarBuoy>().first{it.alive}
        w.damageEnemy(buoy,10000f)
        w.bullets.clear()
        until(w){b.clock>4f}
        assertTrue(w.bullets.none{it.style==BulletStyle.BUBBLE})
        assertTrue(w.bullets.any{it.style==BulletStyle.FISH})
    }

    @Test fun sistersShareHealthDiveOnlyBrieflyAndSwapWithoutTeleportingBack() {
        for(bout in listOf(0,12)) {
            val w=world(8,bout=bout)
            val b=w.boss as NovaOtterBoss
            assertEquals(2,b.hulls.size)
            val left=b.hulls.first{it.side<0};val right=b.hulls.first{it.side>0}
            val hp=b.hp
            w.damageEnemy(left,100f);w.damageEnemy(right,100f)
            assertEquals(200f,hp-b.hp,.01f)
            until(w){b.diving}
            assertTrue(b.hulls.all{it.invulnerable && !it.solid})
            var last=left.x
            until(w){
                assertTrue("smooth submerged movement",abs(left.x-last)<20f);last=left.x
                !b.diving
            }
            assertTrue(left.x>right.x)
            repeat(70){w.update(STEP)}
            assertTrue(left.x>right.x)
            assertTrue(b.hulls.none{it.invulnerable})
        }
    }

    @Test fun torpedoSourceAlternatesScreenSidesAfterSubmergedSwap() {
        val w=world(8)
        val b=w.boss as NovaOtterBoss
        val first=w.bullets.first{it.style==BulletStyle.FISH}.x
        until(w){b.cycle==1 && w.bullets.any { it.style==BulletStyle.FISH && it.age<STEP*2f }}
        val next=w.bullets.first{it.style==BulletStyle.FISH && it.age<STEP*2f}.x
        assertTrue(first<500f)
        assertTrue(next>500f)
    }

    @Test fun oneAreaBlastHitsTheSharedLifeOnlyOnceAndPiercingCannotDoubleHit() {
        val w=world(8)
        val b=w.boss as NovaOtterBoss
        w.enemies.removeAll{it is NovaSonarBuoy}
        w.player.bombs=1
        val hp=b.hp
        assertTrue(w.requestUltimate())
        repeat(300) { w.playerShots.clear();w.update(STEP) }
        assertEquals(700f,hp-b.hp,.1f)
        // A single oversized piercing projectile overlaps both hulls; crit is at most 2x.
        val before=b.hp
        w.playerShots.clear()
        w.playerShots+=PlayerShot(b.x,b.y,0f,0f,100f,400f,ShotStyle.PAW,pierce=5)
        w.update(STEP)
        assertTrue(before-b.hp in 100f..200.1f)
    }

    @Test fun blackCatAreaPulseAndTickUseOneSharedDamageIdentity() {
        val w=world(8,cat=CatType.BLACK)
        val b=w.boss as NovaOtterBoss
        w.enemies.removeAll { it is NovaSonarBuoy }
        w.player.x=500f;w.player.y=650f
        // Place both real hulls inside one area band, then compare with one hull alone.
        b.hulls.forEach { it.x=500f;it.y=400f }
        w.pawBolt.pulseTimer=Weapons.VIBRO_PULSE
        val hp=b.hp
        Weapons.updateVibroPaw(w,w.player,STEP)
        val pulse=hp-b.hp
        assertTrue("single pulse, allowing one crit: $pulse",pulse in listOf(40.5f,81f))
        w.pawBolt.pulseTimer=0f;b.pawHitTime=-1f
        val before=b.hp
        Weapons.updateVibroPaw(w,w.player,STEP)
        assertTrue(before-b.hp in listOf(27f,54f))
        val after=b.hp
        Weapons.updateVibroPaw(w,w.player,STEP)
        assertEquals(after,b.hp,0f)
    }

    @Test fun transformationsCancelPartsAndNeverRepeatEvenAfterHealing() {
        for(stage in 7..8) {
            val w=world(stage)
            val b=w.boss as NovaVehicleBoss
            val old=w.enemies.filter{it is NovaCapacitor || it is NovaSonarBuoy}
            b.hp=b.maxHp*.05f;w.update(STEP)
            assertTrue(b.transforming)
            assertTrue(old.none{it.alive})
            assertTrue(w.bullets.isEmpty());assertTrue(w.lasers.isEmpty())
            if(b is NovaOtterBoss) assertTrue(b.hulls.all{it.invulnerable && !it.solid})
            until(w){b.form==1 && !b.transforming}
            assertFalse(b.broken)
            b.hp=b.maxHp*.9f;repeat(5){w.update(STEP)}
            b.hp=b.maxHp*.1f;repeat(150){w.update(STEP)}
            assertEquals(1,b.transformations)
        }
    }

    @Test fun dockedHullExposesOneSideAndConnectorCannotBlockBottomEscape() {
        for(height in listOf(1200f,1800f,2200f)) {
            val w=world(8,.3f,height=height)
            val b=w.boss as NovaOtterBoss
            until(w){w.lasers.isNotEmpty()}
            val beam=w.lasers.first()
            assertTrue(beam.warnTime>=1.2f)
            assertTrue(beam.y+beam.width < w.playerMaxY-100f)
            until(w){b.cooling}
            val weak=b.hulls.first{it.side == -b.activeSide}
            val other=b.hulls.first{it.side == b.activeSide}
            var hp=b.hp;w.damageEnemy(weak,100f);assertEquals(180f,hp-b.hp,.01f)
            hp=b.hp;w.damageEnemy(other,100f);assertEquals(100f,hp-b.hp,.01f)
        }
    }

    @Test fun killingEitherFormCleansPartsAndReachesDialogueWithSingleReward() {
        for(stage in 7..8) for(hp in listOf(1f,.1f)) {
            val w=world(stage,hp)
            val b=w.boss!!
            val score=w.score
            w.damageEnemy(b,b.maxHp*10f)
            assertEquals(score+b.score,w.score)
            w.damageEnemy(b,b.maxHp*10f)
            assertEquals(score+b.score,w.score)
            until(w){w.phase==Phase.OUTRO_DIALOG}
            assertTrue(w.enemies.none{it.alive && (it is NovaOtterHull || it is NovaSonarBuoy || it is NovaCapacitor || it is NovaKoalaHatch)})
            w.dialogFinished();assertEquals(Phase.CLEAR,w.phase)
        }
    }

    @Test fun bothCatsCanFinishBothEncountersWithBaseWeapons() {
        for(stage in 7..8) for(cat in CatType.entries) {
            val w=world(stage,cat=cat,bout=stage-2)
            w.firing=true
            var t=0f
            while(w.phase==Phase.BOSS && t<360f) {
                val boss=w.boss!!
                val target=if(boss is NovaOtterBoss)boss.hulls.firstOrNull{!it.invulnerable} ?: boss else boss
                w.moveX=if(abs(target.x-w.player.x)>20f)sign(target.x-w.player.x) else 0f
                w.update(STEP);w.cues.clear();t+=STEP
            }
            assertEquals("stage $stage $cat after $t hp=${w.boss!!.hp} form=${(w.boss as NovaVehicleBoss).form}",Phase.BOSS_DEFEATED,w.phase)
        }
    }
}
