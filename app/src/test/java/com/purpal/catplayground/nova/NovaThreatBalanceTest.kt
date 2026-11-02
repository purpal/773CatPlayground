package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import java.io.File

class NovaThreatBalanceTest {
    private fun world(stage:Int,cat:CatType=CatType.ORANGE):World {
        val w=World(NovaEncounters.stage(Stages.get(stage)!!),cat,1800f,difficulty=Difficulty.forBout(when(stage){1->12;8->9;10->10;4->11;else->8}),novaBalance=true)
        NovaPreview.boss(w,1f);return w
    }
    @Test fun slothBulletsAccumulateRemainSlowAndSurviveTransition() {
        val w=world(10);val b=w.boss as NovaSlothBoss
        repeat(600){w.update(STEP)}
        val old=w.bullets.filter{it.alive}.toList();assertTrue("reduced density: ${old.size}",old.size in 45..75)
        assertTrue(old.all{hypot(it.vx,it.vy)<30f})
        b.hp=b.maxHp*.3f;w.update(STEP)
        assertTrue(b.transforming);assertTrue(old.all{it in w.bullets&&it.alive})
        repeat(180){w.update(STEP)}
        assertTrue(old.any{it in w.bullets&&it.alive})
        w.damageEnemy(b,b.maxHp*10f);assertTrue(w.bullets.isEmpty())
    }
    @Test fun forceWallClearsPersistentBullets() {
        val w=world(10);w.bullets.clear()
        val wall=SonicWall(500f,1100f,320f,86f*DEG,1000f);w.walls+=wall
        val shot=w.shoot(500f,1090f,HALF_PI,26f,BulletStyle.LETTER,Tint.PURPLE,24f)
        repeat(10){w.update(STEP)}
        assertFalse(shot.alive);assertTrue(wall.hp<wall.maxHp)
    }
    @Test fun novaBeamHitsRearRanksWhileOriginalBeamStopsAtFirstTarget() {
        for(nova in listOf(false,true)) {
            val w=World(Stages.mouse,CatType.ORANGE,1800f,novaBalance=nova)
            val owner=NovaMouseBoss();val front=NovaSwarmMouse(owner,500f,900f,0f);val rear=NovaSwarmMouse(owner,500f,700f,0f)
            w.enemies+=front;w.enemies+=rear;w.player.x=500f;w.player.y=1200f;w.player.mainLevel=5
            val hp=front.hp
            Weapons.updateBeam(w,w.player,.01f)
            assertTrue(front.hp<hp);assertEquals(nova,rear.hp<hp)
        }
    }
    @Test fun saturatedWaveHasDeepRanksAndTransformsCleanThemUp() {
        val w=world(1);val b=w.boss as NovaMouseBoss
        repeat(180){w.update(STEP)}
        val mice=w.enemies.filterIsInstance<NovaSwarmMouse>();assertEquals(96,mice.size)
        assertEquals(48,mice.count{it.x<500f});assertEquals(48,mice.count{it.x>500f})
        b.hp=b.maxHp*.5f;w.update(STEP)
        assertTrue(mice.none{it.alive})
    }
    @Test fun orangeTemporaryArmorIsStrongerAndLastsLongerOnlyInNova() {
        for(nova in listOf(false,true)) {
            val w=World(Stages.dog,CatType.ORANGE,1800f,novaBalance=nova)
            while(w.phase==Phase.INTRO)w.update(STEP)
            w.drag(100f,0f);w.update(STEP)
            assertEquals(w.difficulty.tempArmor*(if(nova)2f else 1f),w.player.tempArmor,.001f)
            assertEquals(if(nova)1.2f else World.DASH_TIME,w.player.tempArmorTime,.001f)
        }
    }
    @Test fun bothCatsCanWinUsingShieldSpaceAndPiercingCounterplay() {
        val out=StringBuilder("stage,cat,result,seconds,hp,armor\n")
        for(stage in listOf(1,10))for(cat in CatType.entries) {
            val w=world(stage,cat);w.godMode=false;w.player.armor=cat.maxArmor
            w.player.mainWeapon=MainWeapon.FOCUS;w.player.mainLevel=5
            w.player.subWeapon=SubWeapon.SONIC;w.player.subLevel=5;w.firing=true
            var time=0f
            while(w.phase==Phase.BOSS&&time<240f) {
                var tx=w.boss!!.x
                var ty=if(stage==10) w.playerMaxY-20f else w.height*.76f
                if(stage==10) {
                    val side=w.bullets.filter{it.alive&&abs(it.vx)>1f&&abs(it.x-w.player.x)<250f}
                    if(side.any{abs(it.y-w.player.y)<80f}) {
                        ty=(1000..1420 step 20).map{it.toFloat()}.filter{yy->side.none{abs(it.y-yy)<100f}}
                            .minByOrNull{abs(it-w.player.y)}?:w.player.y
                    } else ty=w.player.y
                }
                for(l in w.lasers.filter{it.alive}) {
                    if(abs(sin(l.angle))>.01f) {
                        val lx=l.x+(ty-l.y)*cos(l.angle)/sin(l.angle)
                        if(abs(tx-lx)<120f)tx=if(lx<500f)lx+140f else lx-140f
                    }
                }
                w.moveX=if(abs(tx-w.player.x)>10f)sign(tx-w.player.x)else 0f
                w.moveY=if(abs(ty-w.player.y)>10f)sign(ty-w.player.y)*(if(stage==10).25f else 1f)else 0f
                w.update(STEP);w.items.clear();w.cues.clear();time+=STEP
            }
            assertEquals("stage $stage / $cat counterplay",Phase.BOSS_DEFEATED,w.phase)
            out.append("$stage,$cat,${w.phase},$time,${w.boss!!.hpRatio},${w.player.armor}\n")
        }
        File("build/reports/nova-counterplay.csv").apply{parentFile?.mkdirs();writeText(out.toString())}
    }
    @Test fun fullLevelLoadoutSurvey() {
        val out=StringBuilder("stage,cat,main,sub,moving,result,seconds,boss_hp_fraction,armor\n")
        for(stage in 1..13)for(cat in CatType.entries)for(main in MainWeapon.entries)for(sub in listOf(SubWeapon.HOMING,SubWeapon.SONIC))for(moving in listOf(false,true)) {
            if(stage !in listOf(10,1) && !((main==MainWeapon.SPREAD&&sub==SubWeapon.HOMING)||(main==MainWeapon.SPECIAL&&cat==CatType.BLACK&&sub==SubWeapon.SONIC)))continue
            val w=world(stage,cat);w.godMode=false;w.player.armor=cat.maxArmor
            w.player.mainWeapon=main;w.player.mainLevel=5;w.player.subWeapon=sub;w.player.subLevel=5;w.firing=true
            if(cat==CatType.BLACK&&main==MainWeapon.SPECIAL)w.player.y=w.boss!!.y+250f
            var time=0f
            while(w.phase==Phase.BOSS&&time<180f) {
                if(moving) {
                    // Simple reproducible sweep; not a perfect bot or a human skill claim.
                    val target=if(stage==10)w.boss!!.x else 500f+sin(time*.6f)*360f
                    if(stage==10) {
                        val targetY=((if(cat==CatType.BLACK&&main==MainWeapon.SPECIAL)w.boss!!.y+300f else w.height*.72f)+sin(time*.5f)*70f).coerceAtMost(w.playerMaxY-20f)
                        w.moveY=if(abs(targetY-w.player.y)>15f)sign(targetY-w.player.y)else 0f
                    }
                    w.moveX=if(abs(target-w.player.x)>20f)sign(target-w.player.x)else 0f
                }
                w.update(STEP);w.items.clear();w.cues.clear();time+=STEP
            }
            if(stage in listOf(1,10)&&!moving&&((main==MainWeapon.SPREAD&&sub==SubWeapon.HOMING)||(cat==CatType.BLACK&&main==MainWeapon.SPECIAL&&sub==SubWeapon.SONIC))) {
                assertEquals("stage $stage / $cat / $main / $sub must require movement",Phase.PLAYER_DOWN,w.phase)
            }
            out.append("$stage,$cat,$main,$sub,$moving,${w.phase},$time,${w.boss!!.hpRatio},${w.player.armor}\n")
        }
        File("build/reports/nova-balance.csv").apply{parentFile?.mkdirs();writeText(out.toString())}
    }
}
