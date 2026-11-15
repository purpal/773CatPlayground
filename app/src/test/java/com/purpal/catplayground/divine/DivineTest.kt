package com.purpal.catplayground.divine

import com.purpal.catplayground.divine.model.Battle
import com.purpal.catplayground.divine.model.BattlePhase
import com.purpal.catplayground.divine.model.CardDef
import com.purpal.catplayground.divine.model.CardType
import com.purpal.catplayground.divine.model.Cards
import com.purpal.catplayground.divine.model.DivineBot
import com.purpal.catplayground.divine.model.DivineSave
import com.purpal.catplayground.divine.model.DivineStages
import com.purpal.catplayground.divine.model.EnemyKind
import com.purpal.catplayground.divine.model.Keyword
import com.purpal.catplayground.divine.model.Rarity
import com.purpal.catplayground.divine.model.Spawn
import com.purpal.catplayground.divine.model.StageDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DivineTest {
    private val step = DivineBot.STEP

    /** 沒有敵人的空關卡，方便單獨測卡片效果。 */
    private val empty = StageDef(99, "測試", "", 1f, listOf(Spawn(99999f, EnemyKind.IMP, 0)))

    private fun battle(vararg hand: CardDef): Battle {
        val b = Battle(empty, hand.toList() + List(10) { Cards.tudigong })
        b.hand.clear()
        b.hand += hand
        repeat(8) { b.gainIncense(1) }
        b.events.clear()
        return b
    }

    private fun run(b: Battle, seconds: Float) {
        var t = 0f
        while (t < seconds) {
            b.update(step)
            t += step
        }
    }

    @Test
    fun cardPoolIsConsistent() {
        assertEquals(31, Cards.all.size)
        assertEquals(Cards.all.size, Cards.all.map { it.id }.toSet().size)
        for (c in Cards.all) {
            assertFalse(c.token)
            assertEquals("${c.name} has unit stats iff creature", c.type == CardType.CREATURE, c.unit != null)
            assertEquals(c.type == CardType.ENCHANT, c.enchant != null)
            assertEquals(c.type == CardType.EQUIP, c.equip != null)
            assertTrue(c.cost in 1..7)
        }
        assertEquals(Cards.DECK_SIZE, Cards.starterDeck.size)
        for ((card, n) in Cards.starterDeck.groupingBy { it }.eachCount()) assertTrue(n <= card.rarity.maxCopies)
    }

    @Test
    fun beatRefillsIncenseAndDrawsACard() {
        val b = Battle(empty, Cards.starterDeck)
        assertEquals(Battle.HAND_START, b.hand.size)
        assertEquals(Battle.START_INCENSE, b.incense)
        val card = b.hand.indexOfFirst { it.cost <= 2 && it.type == CardType.CREATURE }
        if (card >= 0) assertTrue(b.play(card, 2, 0))
        val handBefore = b.hand.size
        run(b, Battle.BEAT + 0.1f)
        assertEquals(1, b.beats)
        assertEquals(Battle.START_INCENSE + 1, b.maxIncense)
        assertEquals(b.maxIncense, b.incense)
        assertEquals(handBefore + 1, b.hand.size)
        run(b, Battle.BEAT * 12)
        assertEquals(Battle.MAX_INCENSE, b.maxIncense)
        assertEquals(Battle.HAND_MAX, b.hand.size)
    }

    @Test
    fun cardsNeedIncenseAndAValidTarget() {
        val b = Battle(empty, List(24) { Cards.zeus })
        assertFalse("zeus costs 7", b.play(0, 2, 0))
        assertEquals(Battle.HAND_START, b.hand.size)
        val e = battle(Cards.jingubang, Cards.menshen)
        assertFalse("equipment needs a creature", e.play(0, 1, 0))
        assertTrue(e.play(1, 1, 0))
        assertTrue(e.play(0, 1, 0))
        val c = e.slots[1][0]!!
        assertEquals(4f, e.attackOf(c), 0f)
        assertTrue(Keyword.TRAMPLE in c.keywords)
    }

    @Test
    fun meleeBlocksAndRangedShootsFromBehind() {
        val b = battle(Cards.menshen, Cards.artemis)
        assertTrue(b.play(0, 2, 0))
        assertTrue(b.play(0, 2, 2))
        val imp = b.spawn(EnemyKind.IMP, 2)
        run(b, 40f)
        assertFalse("the imp died before reaching the temple", imp.alive)
        assertEquals(Battle.TEMPLE_HP, b.templeHp)
        // 沒人擋的車道：小鬼走到神殿，扣血
        val open = b.spawn(EnemyKind.IMP, 0)
        run(b, 40f)
        assertFalse(open.alive)
        assertEquals(Battle.TEMPLE_HP - 1, b.templeHp)
    }

    @Test
    fun onlyAntiAirHitsFlyingEnemies() {
        val b = battle(Cards.guanyu)
        assertTrue(b.play(0, 1, 0))
        b.spawn(EnemyKind.BAT, 1)
        run(b, 30f)
        assertEquals("the bat flew past the melee blocker", Battle.TEMPLE_HP - 1, b.templeHp)
        val c = battle(Cards.artemis)
        assertTrue(c.play(0, 1, 2))
        c.spawn(EnemyKind.BAT, 1)
        run(c, 30f)
        assertEquals(Battle.TEMPLE_HP, c.templeHp)
    }

    @Test
    fun spellsAndEnterEffects() {
        val b = battle(Cards.wuleifu, Cards.dingshen, Cards.tudigong, Cards.hermes, Cards.erlang)
        val a = b.spawn(EnemyKind.ZOMBIE, 3)
        val c = b.spawn(EnemyKind.IMP, 3)
        val other = b.spawn(EnemyKind.IMP, 0)
        run(b, 1f)
        assertTrue(b.play(0, 3, 0))
        assertEquals(10f, a.hp, 0.01f)
        assertFalse("4 damage kills an imp", c.alive)
        assertEquals(other.maxHp, other.hp, 0f)
        val y = a.y
        assertTrue(b.play(0, 3, 0))
        run(b, 2f)
        assertEquals("stunned enemies do not move", y, a.y, 0.01f)
        val incense = b.incense
        assertTrue(b.play(0, 1, 0))
        assertEquals("tudigong refunds its own cost", incense, b.incense)
        val hand = b.hand.size
        assertTrue(b.play(0, 1, 1))
        assertEquals("hermes draws a card", hand, b.hand.size)
        assertTrue(b.play(b.hand.indexOf(Cards.erlang), 4, 2))
        assertEquals("erlang brings his dog", Cards.xiaotian, b.slots[4][0]?.def)
    }

    @Test
    fun wukongRevivesOnceAndEnchantmentsExpire() {
        val b = battle(Cards.wukong, Cards.bagua)
        assertTrue(b.play(0, 2, 0))
        val monkey = b.slots[2][0]!!
        b.damageCreature(monkey, 99f)
        assertTrue(monkey.alive)
        assertEquals(monkey.maxHp / 2f, monkey.hp, 0.01f)
        b.damageCreature(monkey, 99f)
        assertFalse(monkey.alive)
        assertNull(b.slots[2][0])
        assertTrue("dead creatures go to the discard pile", Cards.wukong in b.discard)

        assertTrue(b.play(0, 2, 0))
        assertEquals(1, b.enchants.size)
        val zombie = b.spawn(EnemyKind.ZOMBIE, 2)
        run(b, 5f)
        assertTrue("burning", zombie.hp < zombie.maxHp)
        assertTrue("slowed", zombie.y < Battle.SPAWN_Y + EnemyKind.ZOMBIE.speed * 5f * 0.75f)
        run(b, 20f)
        assertTrue(b.enchants.isEmpty())
    }

    @Test
    fun bossVanishesTwiceAndSummonsHelp() {
        val b = battle()
        val boss = b.spawn(EnemyKind.NINETAIL, 2)
        run(b, 1f)
        b.damageEnemy(boss, boss.maxHp * 0.4f)
        run(b, 0.1f)
        assertTrue(boss.invulnerable)
        assertEquals(1, boss.phase)
        assertTrue("minions cover the retreat", b.enemies.size > 1)
        run(b, Battle.BOSS_VANISH + 0.2f)
        assertFalse(boss.invulnerable)
        assertNotNull(b.enemies.firstOrNull { it.isBoss })
    }

    @Test
    fun starterDeckClearsTheEarlyStages() {
        for (number in 1..3) {
            for (seed in 1..4) {
                val b = DivineBot.run(Battle(DivineStages.get(number)!!, Cards.starterDeck, seed))
                assertEquals("stage $number seed $seed", BattlePhase.WON, b.phase)
                assertTrue("stage $number took ${b.time}s", b.time < 240f)
            }
        }
    }

    /** 每一關都有解：一副好牌組交給自動玩家，大多數時候打得過（包含 Boss）。 */
    @Test
    fun everyStageIsBeatableWithAGoodDeck() {
        val good = with(Cards) {
            listOf(wukong, zeus, guanyu, guanyu, menshen, menshen, athena, erlang, nezha, nezha, artemis, artemis, apollo, heracles,
                hermes, hermes, tudigong, tudigong, wuleifu, wuleifu, thunderbolt, olympus, bagua, sanctuary)
        }
        assertEquals(Cards.DECK_SIZE, good.size)
        for (stage in DivineStages.all) {
            val wins = (1..6).count { seed -> DivineBot.run(Battle(stage, good, seed)).phase == BattlePhase.WON }
            assertTrue("stage ${stage.number}: $wins/6 wins", wins >= 4)
        }
    }

    @Test
    fun savesPacksAndDeckRules() {
        val fresh = DivineSave.fresh()
        assertTrue(fresh.deckReady)
        assertEquals(fresh, DivineSave.decode(fresh.encode()))
        assertNull(DivineSave.decode("garbage"))
        assertTrue(fresh.unlocked(1))
        assertFalse(fresh.unlocked(2))

        val (won, reward) = fresh.afterWin(DivineStages.get(1)!!, 2)
        assertEquals(DivineStages.get(1)!!.firstClearCoins, reward)
        assertEquals(2, won.stars[1])
        assertTrue(won.unlocked(2))
        val (again, replay) = won.afterWin(DivineStages.get(1)!!, 3)
        assertEquals(DivineStages.REPLAY_COINS, replay)
        assertEquals(3, again.stars[1])

        var save = again.copy(coins = DivineSave.PACK_COST * 60)
        val random = Random(5)
        repeat(60) {
            val pack = save.openPack(random)!!
            assertEquals(DivineSave.PACK_SIZE, pack.cards.size)
            assertTrue("last card is rare or better", pack.cards.last().rarity >= Rarity.RARE)
            save = pack.save
        }
        for (c in Cards.all) assertTrue("${c.name} within the copy limit", save.count(c) <= c.rarity.maxCopies)
        assertNull(save.copy(coins = 10).openPack(random))
        assertEquals(save, DivineSave.decode(save.encode()))

        // 牌組：同名卡不超過持有數與上限，滿 24 張不能再加
        val removed = fresh.removeFromDeck(Cards.tudigong)
        assertEquals(Cards.DECK_SIZE - 1, removed.deck.size)
        assertFalse(removed.canAdd(Cards.zeus))
        assertTrue(removed.canAdd(Cards.tudigong))
        assertFalse(removed.addToDeck(Cards.tudigong).canAdd(Cards.tudigong))
    }
}
