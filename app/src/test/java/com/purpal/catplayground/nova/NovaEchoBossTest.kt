package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class NovaEchoBossTest {
    private fun world(stage:Int,hp:Float=1f,cat:CatType=CatType.ORANGE,height:Float=1800f):World {
        val w=World(NovaEncounters.stage(Stages.get(stage)!!),cat,height,difficulty=Difficulty.forBout(stage-2))
        NovaPreview.boss(w,hp);return w
    }
    private fun until(w:World,limit:Int=2400,condition:()->Boolean) {
        var n=0
        while(!condition() && n++<limit){w.update(STEP);w.cues.clear()}
        assertTrue("stage ${w.stage.number} timed out",condition())
    }
    @Test fun originalFactoriesAndWavesArePreserved() {
        assertTrue(Stages.get(11)!!.createBoss() is RaccoonBoss)
        assertTrue(Stages.get(12)!!.createBoss() is MonkeyBoss)
        for(stage in 11..12) {
            val original=Stages.get(stage)!!;val nova=NovaEncounters.stage(original)
            assertSame(original.country,nova.country)
            assertEquals(original.script.events.size+1,nova.script.events.size)
        }
    }
    @Test fun cargoReturnsStolenPickupExactlyOnceEvenAcrossTransformation() {
        val w=world(11);val b=w.boss as NovaRaccoonBoss
        until(w){b.clock<.5f};b.releaseLoot(w,false)
        w.items.clear();val item=Item(800f,800f,ItemType.REPAIR);w.items+=item
        until(w){b.stolenCount==1}
        assertFalse(item.alive)
        val vault=b.vault!!
        b.hp=b.maxHp*.4f;w.update(STEP)
        assertTrue(b.transforming);assertTrue(vault.alive);assertTrue(vault.invulnerable)
        until(w){b.form==1}
        w.items.clear()
        w.damageEnemy(vault,10000f)
        assertEquals(0,b.stolenCount);assertTrue(b.cooling)
        assertEquals(1,w.items.count{it.type==ItemType.REPAIR})
        vault.onDefeated(w)
        assertEquals(1,w.items.count{it.type==ItemType.REPAIR})
    }
    @Test fun stolenPickupCannotAlsoBeCollectedDuringTheSameFrame() {
        val w=world(11);val b=w.boss as NovaRaccoonBoss
        until(w){b.clock<.5f};b.releaseLoot(w,false);w.items.clear()
        val item=Item(800f,800f,ItemType.REPAIR);w.items+=item
        until(w){b.clock<2.5f && b.clock+STEP>=2.5f}
        w.player.armor=20f;item.x=w.player.x;item.y=w.player.y
        w.update(STEP)
        assertEquals(1,b.stolenCount);assertFalse(item in w.items)
        assertEquals(20f,w.player.armor,0f)
    }
    @Test fun bossDeathAlsoReturnsCargo() {
        val w=world(11);val b=w.boss as NovaRaccoonBoss
        until(w){b.clock<.5f};b.releaseLoot(w,false);w.items.clear();w.items+=Item(800f,800f,ItemType.BOMB)
        until(w){b.stolenCount==1}
        w.items.clear();w.damageEnemy(b,b.maxHp*10f)
        assertEquals(1,w.items.count{it.type==ItemType.BOMB});assertEquals(0,b.stolenCount)
    }
    @Test fun escapingMagnetOrBreakingVaultCancelsTheft() {
        for(breakVault in listOf(false,true)) {
            val w=world(11);val b=w.boss as NovaRaccoonBoss
            until(w){b.clock<.5f};b.releaseLoot(w,false);w.items.clear();w.player.bombs=3
            until(w){b.locking && b.stealingPlayer}
            val lock=b.lockX
            if(breakVault)w.damageEnemy(b.vault!!,10000f)else w.player.x=if(lock>500f)80f else 920f
            until(w){b.clock>2.9f}
            assertEquals(3,w.player.bombs);assertEquals(0,b.stolenCount)
        }
    }
    @Test fun remainingInMagnetLosesOneChargeWhichCanBeRecovered() {
        val w=world(11);val b=w.boss as NovaRaccoonBoss
        until(w){b.clock<.5f};b.releaseLoot(w,false);w.items.clear();w.player.bombs=3;w.player.invulnerable=0f
        until(w){b.locking && b.stealingPlayer}
        until(w){b.clock>2.9f}
        assertEquals(2,w.player.bombs);assertEquals(1,b.stolenCount)
        w.damageEnemy(b.vault!!,10000f)
        assertEquals(1,w.items.count{it.type==ItemType.BOMB})
    }
    @Test fun hologramsAreDestructibleAndCannotCauseContactDamage() {
        val w=world(11,.4f);val b=w.boss as NovaRaccoonBoss
        until(w){w.enemies.filterIsInstance<NovaHologram>().any{it.alive}}
        val fakes=w.enemies.filterIsInstance<NovaHologram>().filter{it.alive}
        assertEquals(2,fakes.size)
        for(fake in fakes) { assertEquals(0f,fake.contactDamage,0f);w.damageEnemy(fake,10000f);assertFalse(fake.alive) }
        assertTrue(b.alive)
    }
    @Test fun snapshotKeepsWeaponCatAndTargetUntilFiring() {
        for(cat in CatType.entries)for(weapon in MainWeapon.entries) {
            val w=world(12,cat=cat);val b=w.boss as NovaMonkeyBoss
            until(w){b.clock<.05f};w.player.mainWeapon=weapon
            until(w){b.scanning}
            val recorded=b.snapshot!!;val target=w.player.x
            w.player.mainWeapon=if(weapon==MainWeapon.FOCUS)MainWeapon.SPREAD else MainWeapon.FOCUS
            w.player.x=80f;w.bullets.clear();w.lasers.clear()
            until(w){b.clock>1.7f}
            assertSame(recorded,b.snapshot);assertEquals(weapon,recorded.weapon);assertEquals(cat,recorded.cat)
            assertEquals(target,recorded.targetX,0f)
            if(weapon==MainWeapon.FOCUS)assertEquals(1,w.lasers.size)else assertTrue(w.bullets.isNotEmpty())
        }
    }
    @Test fun breakingReplayBeforeShotOrDuringLaserCancelsItsAttack() {
        for(afterFire in listOf(false,true)) {
            val w=world(12,.2f)
            val node=NovaReplayNode(null,NovaMimicSnapshot(MainWeapon.FOCUS,CatType.BLACK,500f,1500f),500f,650f)
            w.spawn(node)
            if(afterFire){until(w){node.fired};assertTrue(w.lasers.any{it.owner===node&&it.alive})}
            w.damageEnemy(node,10000f)
            repeat(240){w.update(STEP)}
            assertTrue(w.lasers.none{it.owner===node&&it.alive})
            if(!afterFire)assertFalse(node.fired)
        }
    }
    @Test fun transformationAndAwakeningRunOnceForSeparateOrCombinedThresholds() {
        for(combined in listOf(false,true)) {
            val w=world(12);val b=w.boss as NovaMonkeyBoss
            b.hp=b.maxHp*if(combined).2f else .28f;w.update(STEP)
            assertTrue(b.transforming)
            until(w){b.form==1}
            if(!combined){assertFalse(b.awakened);b.hp=b.maxHp*.2f;w.update(STEP);assertTrue(b.awakeningTime>0f)}
            until(w){b.awakeningTime<=0f && b.awakened}
            assertEquals(1,b.transformations);assertEquals(1,b.awakenings)
            until(w){w.enemies.filterIsInstance<NovaReplayNode>().any{it.alive}}
            b.hp=b.maxHp*.8f;repeat(180){w.update(STEP)};b.hp=b.maxHp*.1f;repeat(180){w.update(STEP)}
            assertEquals(1,b.transformations);assertEquals(1,b.awakenings)
        }
    }
    @Test fun defeatDuringTransformReachesOutroAndClearsComponents() {
        for(stage in 11..12) {
            val w=world(stage);val b=w.boss as NovaVehicleBoss
            b.hp=b.maxHp*.1f;w.update(STEP);assertTrue(b.transforming)
            w.damageEnemy(b,b.maxHp*10f)
            until(w){w.phase==Phase.OUTRO_DIALOG}
            assertTrue(w.enemies.none{it.alive&&!it.isBoss})
            w.dialogFinished();assertEquals(Phase.CLEAR,w.phase)
        }
    }
    @Test fun bothCatsCanFinishWithInitialWeapons() {
        for(stage in 11..12)for(cat in CatType.entries) {
            val w=world(stage,cat=cat)
            w.firing=true;var time=0f
            while(w.phase==Phase.BOSS && time<360f) {
                val boss=w.boss!!;w.moveX=if(abs(boss.x-w.player.x)>20f)sign(boss.x-w.player.x)else 0f
                w.update(STEP);w.cues.clear();time+=STEP
            }
            assertEquals("stage $stage $cat $time hp=${w.boss!!.hp}",Phase.BOSS_DEFEATED,w.phase)
        }
    }
}
