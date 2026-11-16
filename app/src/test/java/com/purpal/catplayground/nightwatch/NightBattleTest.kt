package com.purpal.catplayground.nightwatch

import org.junit.Assert.*
import org.junit.Test

class NightBattleTest {
    private fun battle()=NightBattle(NightStage.all[0],God.entries.take(4))
    @Test fun oneCatCannotOccupyTwoSeatsAndFailedActionsDoNotSpend() {
        val b=battle()
        assertTrue(b.place(God.ZEUS,0))
        val money=b.offerings
        assertFalse(b.place(God.ZEUS,1));assertFalse(b.place(God.WIND,2));assertFalse(b.place(God.ATHENA,0))
        assertEquals(money,b.offerings);assertEquals(1,b.seats.size)
    }
    @Test fun relocatingPreservesUpgradeAndRejectsOccupiedSeat() {
        val b=battle();b.place(God.ZEUS,0);b.place(God.ATHENA,1)
        assertTrue(b.upgrade(0,1))
        val s=b.seats.first();val money=b.offerings
        assertFalse(b.move(0,1));assertEquals(money,b.offerings)
        assertTrue(b.move(0,2));assertSame(s,b.seats.first());assertEquals(2,s.level);assertEquals(1,s.branch)
        assertEquals(money-15,b.offerings)
    }
    @Test fun cannotLaunchWithoutDefendersAndCannotSpendDuringBlessing() {
        val b=battle();assertFalse(b.startWave());b.place(God.ZEUS,3);assertTrue(b.startWave());assertFalse(b.startWave())
        assertEquals(1,b.wave)
    }
    @Test fun lightningJumpsFurtherWhenTargetIsWet() {
        fun run(wet: Boolean): Int {
            val b=battle();b.place(God.ZEUS,0);b.startWave()
            for(i in 0..3) b.enemies+=Invader(100+i,Foe.LION,0,500f).apply {distance=210f+i*12; p=NightBattle.position(b.stage.paths[0],distance);this.wet=if(wet) 4f else 0f}
            repeat(8) {b.tick(.05f)}
            return b.enemies.count {it.id>=100 && it.hp<500}
        }
        assertTrue(run(true)>run(false))
    }
    @Test fun pauseTimeIsOwnedByCallerAndPreparationDoesNotAdvanceEnemies() {
        val b=battle();b.place(God.ZEUS,0);b.tick(10f)
        assertEquals(0f,b.time,0f);assertEquals(0,b.enemies.size)
    }
    @Test fun mechanismExposesBossAndSharesCooldown() {
        val b=battle();b.place(God.ZEUS,0);b.startWave()
        val boss=Invader(99,Foe.BOSS,0,3000f);b.enemies+=boss
        assertTrue(b.activateMechanism());assertTrue(boss.exposed>0);assertTrue(boss.stun>0)
        assertFalse(b.activateMechanism())
    }
    @Test fun stoneLionCannotBePushedByWindMechanism() {
        val b=NightBattle(NightStage.all[1],God.entries.take(4));b.place(God.ZEUS,0);b.startWave()
        val lion=Invader(99,Foe.LION,0,500f).apply {distance=300f};val rat=Invader(100,Foe.RAT,0,50f).apply {distance=300f}
        b.enemies+=lion;b.enemies+=rat;b.activateMechanism()
        assertEquals(300f,lion.distance,0f);assertEquals(150f,rat.distance,0f)
    }
    @Test fun waterExtinguishesFire() {
        val b=battle();b.place(God.POSEIDON,0);b.startWave()
        val e=Invader(100,Foe.LION,0,500f).apply {distance=230f;p=NightBattle.position(b.stage.paths[0],distance);burn=8f}
        b.enemies+=e;repeat(8) {b.tick(.05f)}
        assertTrue(e.wet>0);assertEquals(0f,e.burn,0f)
    }
    @Test fun ultimateNeedsDeployedCatAndUsesTeamCooldown() {
        val b=battle();b.place(God.ZEUS,0);b.place(God.ATHENA,1);b.startWave()
        assertFalse(b.invoke(God.POSEIDON));assertTrue(b.invoke(God.ZEUS));assertFalse(b.invoke(God.ATHENA));assertTrue(b.ultimate>0)
    }
    @Test fun breachedBossCostsEightLivesAndNeverGrantsKillIncome() {
        val b=battle();b.place(God.ZEUS,0);b.startWave()
        val money=b.offerings
        b.enemies+=Invader(200,Foe.BOSS,0,4000f).apply {distance=NightBattle.length(b.stage.paths[0])-.1f;p=b.stage.paths[0].last()}
        b.tick(.05f)
        assertEquals(12,b.hp);assertEquals(NightPhase.LOST,b.phase)
        assertEquals(0,b.kills);assertEquals(money,b.offerings)
        val time=b.time;b.tick(5f);assertEquals(time,b.time,0f)
    }
    @Test fun baselineStrategyCanFinishEveryMap() {
        for(stage in NightStage.all) {
            val b=NightBattle(stage,listOf(God.ZEUS,God.POSEIDON,God.ATHENA,God.NEZHA))
            val sites=when(stage.id) {0->listOf(3,4,5,6);1->listOf(4,2,6,3);else->listOf(3,4,6,5)}
            b.team.forEachIndexed {i,g->b.place(g,sites[i])}
            var frames=0
            while(b.phase!=NightPhase.WON && b.phase!=NightPhase.LOST && frames<50000) {
                when(b.phase) {
                    NightPhase.PREPARE->{b.seats.sortedBy {it.level}.forEach {b.upgrade(it.index,if(it.god==God.ZEUS) 1 else 0)};b.startWave()}
                    NightPhase.BLESSING->b.choose(b.choices.minBy { listOf("chain","power","haste","range","fire","heal","gold","storm","ultimate","shield","wind","move").indexOf(it.id) }.id)
                    NightPhase.FIGHT->{if(b.living.any {it.kind==Foe.BOSS} || b.living.size>9) b.activateMechanism();if(b.living.size>7 || b.living.any {it.kind==Foe.BOSS}) b.invoke(God.ZEUS);b.tick(.05f)}
                    else->Unit
                }
                frames++
            }
            println("NIGHTWATCH balance stage=${stage.id} phase=${b.phase} wave=${b.wave} hp=${b.hp} kills=${b.kills} frames=$frames")
            assertEquals("stage ${stage.id}: hp=${b.hp}, wave=${b.wave}",NightPhase.WON,b.phase)
        }
    }
}
