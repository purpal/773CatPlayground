package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class NovaForestBossTest {
    private fun world(stage:Int,hp:Float=1f,height:Float=1800f,cat:CatType=CatType.ORANGE,bout:Int=stage-2):World {
        val w=World(NovaEncounters.stage(Stages.get(stage)!!),cat,height,difficulty=Difficulty.forBout(bout))
        NovaPreview.boss(w,hp);return w
    }
    private fun until(w:World,limit:Int=2400,condition:()->Boolean) {
        var n=0
        while(!condition() && n++<limit) { w.update(STEP);w.cues.clear() }
        assertTrue("stage ${w.stage.number} timed out",condition())
    }

    @Test fun novaFactoriesAndTutorialsDoNotReplaceOriginalGame() {
        assertTrue(Stages.panda.createBoss() is PandaBoss)
        assertTrue(Stages.sloth.createBoss() is SlothBoss)
        assertTrue(world(9).boss is NovaPandaBoss)
        assertTrue(world(10).boss is NovaSlothBoss)
        for(i in 9..10) {
            val original=Stages.get(i)!!;val nova=NovaEncounters.stage(original)
            assertEquals(original.script.events.size+1,nova.script.events.size)
            assertSame(original.country,nova.country)
        }
    }

    @Test fun destroyingSupplyCellsCutsTheirBeamAndPreventsHealing() {
        val w=world(9,.8f)
        val b=w.boss as NovaPandaBoss
        until(w){b.supplyBeam?.alive==true}
        val cells=w.enemies.filterIsInstance<NovaSupplyCell>().filter{it.alive}
        assertEquals(2,cells.size)
        w.damageEnemy(cells[0],10000f)
        w.update(STEP)
        assertFalse(b.supplyBeam!!.alive)
        w.damageEnemy(cells[1],10000f)
        val hp=b.hp
        until(w){b.clock>6.5f}
        assertEquals(hp,b.hp,0f)
        assertEquals(0,b.supplyUses)
    }

    @Test fun unattendedSupplyHealsAtMostThreeTimesAndDoesNotUndoTransformation() {
        val w=world(9,.7f)
        val b=w.boss as NovaPandaBoss
        val hp=b.hp
        until(w,limit=2400){b.supplyUses==3}
        assertEquals(hp+b.maxHp*.09f,b.hp,.1f)
        repeat(1800){w.update(STEP)}
        assertEquals(3,b.supplyUses)
        assertEquals(hp+b.maxHp*.09f,b.hp,.1f)
        b.hp=b.maxHp*.2f;w.update(STEP)
        assertTrue(b.transforming)
        assertTrue(w.enemies.filterIsInstance<NovaSupplyCell>().none{it.alive})
        until(w){b.form==1}
        b.hp=b.maxHp*.9f;repeat(120){w.update(STEP)}
        assertEquals(1,b.form);assertEquals(1,b.transformations)
    }

    @Test fun reflectedRouteIsContinuousAndIndependentOfFramePartition() {
        val path=NovaRollPath(500f,400f,400f,0f,660f)
        assertEquals(820f,path.at(.8f).first,.01f)
        assertEquals(740f,path.at(1f).first,.01f)
        assertEquals(path.at(1f),path.at(.25f+.75f))
        val diagonal=NovaRollPath(500f,400f,330f,290f,660f)
        var last=diagonal.at(0f)
        for(i in 1..380) {
            val next=diagonal.at(i*.01f)
            assertTrue(next.first in 180f..820f);assertTrue(next.second in 340f..660f)
            assertTrue(hypot(next.first-last.first,next.second-last.second)<4.5f)
            last=next
        }
    }

    @Test fun pandaFollowsLockedWarningAndLeavesReachableBottomLaneAtAllHeights() {
        for(height in listOf(1200f,1800f,2200f)) for(bout in listOf(0,12)) {
            val w=world(9,.2f,height=height,bout=bout)
            val b=w.boss as NovaPandaBoss
            until(w){b.motion==NovaPandaBoss.Motion.WARN}
            val route=b.route!!
            w.player.x=70f
            repeat(30){w.update(STEP)}
            assertSame(route,b.route)
            assertEquals(NovaPandaBoss.Motion.WARN,b.motion)
            w.player.armor=w.player.cat.maxArmor*.05f
            assertTrue(w.player.speed*NovaPandaBoss.WARN>b.radius+w.player.radius+30f)
            assertTrue(route.bottom+b.radius+w.player.radius+30f<w.playerMaxY)
            until(w){b.motion==NovaPandaBoss.Motion.ROLL}
            repeat(90) {
                w.update(STEP)
                val expected=route.at(b.clock)
                assertEquals(expected.first,b.x,.02f);assertEquals(expected.second,b.y,.02f)
                assertTrue(w.lasers.isEmpty())
            }
            until(w){b.cooling}
            assertTrue(b.damageScale>1f)
        }
    }

    @Test fun temporalGatesReleaseSequentiallyWithSmoothBoundaryAndReverseOrder() {
        for(hp in listOf(1f,.2f)) {
            val w=world(10,hp)
            val b=w.boss as NovaSlothBoss
            until(w){b.clock in 4.95f..5.1f}
            fun speed(gate:Int,r:Float=100f):Float { val a=b.gateAngle(gate);return b.bulletTimeScale(b.fieldX+cos(a)*r,b.fieldY+sin(a)*r) }
            assertTrue(speed(0)>.9f)
            assertEquals(NovaSlothBoss.SLOW,speed(1),.001f)
            assertEquals(NovaSlothBoss.SLOW,speed(2),.001f)
            assertTrue(abs(speed(0,279.99f)-1f)<.001f)
            assertEquals(1f,speed(0,281f),0f)
            until(w){b.clock>7.45f}
            assertEquals(if(b.form==1)NovaSlothBoss.FAST else 1f,speed(2),.001f)
            until(w){b.cycle==1 && b.clock>5.05f}
            assertTrue(speed(2)>.9f);assertEquals(NovaSlothBoss.SLOW,speed(0),.001f)
        }
    }

    @Test fun enemyBulletsStayInsideUnopenedGatesEvenAtHighestDifficulty() {
        for(hp in listOf(1f,.2f)) {
            val w=world(10,hp,bout=12)
            val b=w.boss as NovaSlothBoss
            while(b.clock<9f) {
                w.update(STEP)
                for(bullet in w.bullets) {
                    val gate=b.gateAt(bullet.x,bullet.y)
                    if(b.clock<b.releaseAt(gate)) assertTrue("unreleased gate $gate at ${b.clock}",
                        dist2(bullet.x,bullet.y,b.fieldX,b.fieldY)<NovaSlothBoss.FIELD_RADIUS.pow(2))
                }
            }
            assertTrue(w.lasers.isEmpty())
        }
    }

    @Test fun timeFieldOnlyChangesEnemyMotionNotPlayerShotOrGameClock() {
        for(cat in CatType.entries) {
            val w=world(10,cat=cat)
            val b=w.boss as NovaSlothBoss
            w.bullets.clear()
            val bullet=w.shoot(b.fieldX,b.fieldY+80f,HALF_PI,100f,BulletStyle.ORB,Tint.PURPLE)
            val shot=PlayerShot(b.fieldX,b.fieldY+100f,0f,-600f,10f,10f,ShotStyle.PELLET)
            w.playerShots+=shot
            w.player.x=b.fieldX;w.player.y=b.fieldY+180f
            w.moveX=1f
            val x=w.player.x;val speed=w.player.speed;val sy=shot.y;val by=bullet.y;val time=w.time
            w.update(STEP)
            assertEquals(speed*STEP,w.player.x-x,.02f)
            assertEquals(-600f*STEP,shot.y-sy,.02f)
            assertEquals(bullet.vy*STEP*NovaSlothBoss.SLOW,bullet.y-by,.02f)
            assertEquals(STEP,w.time-time,.0001f)
        }
    }

    @Test fun transformCancelsFieldAndBothBossesCanDieDuringTransition() {
        for(stage in 9..10) {
            val w=world(stage)
            val b=w.boss as NovaVehicleBoss
            b.hp=b.maxHp*.1f;w.update(STEP)
            assertTrue(b.transforming)
            assertTrue(w.bullets.isEmpty());assertTrue(w.lasers.isEmpty())
            if(b is NovaSlothBoss) assertEquals(1f,b.bulletTimeScale(b.fieldX,b.fieldY+100f),0f)
            w.damageEnemy(b,b.maxHp*10f)
            until(w){w.phase==Phase.OUTRO_DIALOG}
            assertTrue(w.enemies.none{it.alive && it is NovaSupplyCell})
            w.dialogFinished();assertEquals(Phase.CLEAR,w.phase)
        }
    }

    @Test fun bothCatsCanFinishWithInitialWeapons() {
        for(stage in 9..10) for(cat in CatType.entries) {
            val w=world(stage,cat=cat)
            w.firing=true
            var t=0f
            while(w.phase==Phase.BOSS && t<360f) {
                val b=w.boss!!
                w.moveX=if(abs(b.x-w.player.x)>20f)sign(b.x-w.player.x)else 0f
                w.update(STEP);w.cues.clear();t+=STEP
            }
            assertEquals("stage $stage $cat after $t hp=${w.boss!!.hp}",Phase.BOSS_DEFEATED,w.phase)
        }
    }
}
