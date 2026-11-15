package com.purpal.catplayground.divine.model

import kotlin.random.Random

/** 一包卡的內容：抽到的卡，以及超過持有上限、換成金幣的部分。 */
class PackResult(val cards: List<CardDef>, val refund: Int, val save: DivineSave) {
    /** 每張卡是不是多出來被換成金幣的。 */
    var duplicates: List<Boolean> = emptyList()
        internal set
}

/**
 * 玩家的進度：金幣、擁有的卡、牌組、每一關拿到的星星。不可變，每次變動回傳新的一份，
 * 用 [encode]／[decode] 存成一行字串。
 */
data class DivineSave(
    val coins: Int,
    /** 卡片 id → 張數。 */
    val owned: Map<String, Int>,
    /** 牌組裡的卡片 id（可以重複）。 */
    val deck: List<String>,
    /** 關卡編號 → 星星數（1～3）。 */
    val stars: Map<Int, Int>,
) {
    fun count(card: CardDef): Int = owned[card.id] ?: 0

    fun inDeck(card: CardDef): Int = deck.count { it == card.id }

    val deckCards: List<CardDef> get() = deck.mapNotNull { Cards.get(it) }

    val deckReady: Boolean get() = deck.size == Cards.DECK_SIZE

    /** 已經過關的最高關卡。 */
    val cleared: Int get() = stars.keys.maxOrNull() ?: 0

    fun unlocked(stage: Int): Boolean = stage <= cleared + 1

    fun canAdd(card: CardDef): Boolean = deck.size < Cards.DECK_SIZE && inDeck(card) < minOf(count(card), card.rarity.maxCopies)

    fun addToDeck(card: CardDef): DivineSave = if (canAdd(card)) copy(deck = deck + card.id) else this

    fun removeFromDeck(card: CardDef): DivineSave {
        val i = deck.lastIndexOf(card.id)
        return if (i < 0) this else copy(deck = deck.filterIndexed { index, _ -> index != i })
    }

    /** 過關：記下最好的星星數，並發金幣（第一次過關比較多）。回傳新的進度與這次拿到的金幣。 */
    fun afterWin(stage: StageDef, earnedStars: Int): Pair<DivineSave, Int> {
        val first = stage.number !in stars
        val reward = if (first) stage.firstClearCoins else DivineStages.REPLAY_COINS
        val best = maxOf(stars[stage.number] ?: 0, earnedStars)
        return copy(coins = coins + reward, stars = stars + (stage.number to best)) to reward
    }

    /** 買一包卡；金幣不夠回傳 null。 */
    fun openPack(random: Random): PackResult? {
        if (coins < PACK_COST) return null
        var next = copy(coins = coins - PACK_COST)
        val cards = ArrayList<CardDef>()
        val dup = ArrayList<Boolean>()
        var refund = 0
        repeat(PACK_SIZE) { i ->
            // 最後一張保底稀有以上
            val card = rollCard(random, minRarity = if (i == PACK_SIZE - 1) Rarity.RARE else Rarity.COMMON)
            cards += card
            if (next.count(card) >= card.rarity.maxCopies) {
                refund += card.rarity.refund
                dup += true
            } else {
                next = next.copy(owned = next.owned + (card.id to next.count(card) + 1))
                dup += false
            }
        }
        next = next.copy(coins = next.coins + refund)
        return PackResult(cards, refund, next).also { it.duplicates = dup }
    }

    fun encode(): String = listOf(
        coins.toString(),
        owned.entries.joinToString(",") { "${it.key}:${it.value}" },
        deck.joinToString(","),
        stars.entries.joinToString(",") { "${it.key}:${it.value}" },
    ).joinToString("|")

    companion object {
        const val PACK_COST = 100
        const val PACK_SIZE = 5
        const val START_COINS = 100

        /** 新玩家：一副起始牌組，加上夠開第一包卡的金幣。 */
        fun fresh(): DivineSave {
            val deck = Cards.starterDeck
            return DivineSave(START_COINS, deck.groupingBy { it.id }.eachCount(), deck.map { it.id }, emptyMap())
        }

        fun decode(text: String?): DivineSave? {
            val parts = text?.split("|") ?: return null
            if (parts.size != 4) return null
            return runCatching {
                fun pairs(s: String) = s.split(",").filter { it.isNotEmpty() }.map { it.split(":").let { p -> p[0] to p[1].toInt() } }
                val owned = pairs(parts[1]).filter { Cards.get(it.first)?.token == false }.toMap()
                val deck = parts[2].split(",").filter { owned.containsKey(it) }
                DivineSave(parts[0].toInt(), owned, deck, pairs(parts[3]).associate { it.first.toInt() to it.second })
            }.getOrNull()
        }

        private fun rollCard(random: Random, minRarity: Rarity): CardDef {
            val pool = Rarity.entries.filter { it >= minRarity }
            var roll = random.nextInt(pool.sumOf { it.weight })
            val rarity = pool.first { r ->
                roll -= r.weight
                roll < 0
            }
            val cards = Cards.all.filter { it.rarity == rarity }
            return cards[random.nextInt(cards.size)]
        }

        /** 依神殿剩下的血量給星星。 */
        fun starsFor(templeHp: Int): Int = when {
            templeHp >= Battle.TEMPLE_HP * 0.8f -> 3
            templeHp >= Battle.TEMPLE_HP * 0.4f -> 2
            else -> 1
        }
    }
}
