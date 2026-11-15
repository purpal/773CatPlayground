package com.purpal.catplayground.divine.model

/** 自動玩家：規則很單純，大約是「會玩但不精」的程度。單元測試用它抓平衡，畫面自動測試也用它出牌。 */
object DivineBot {
    const val STEP = 1f / 60f

    /** 打完一場（最多 [maxSeconds] 秒），回傳結束時的戰鬥。 */
    fun run(b: Battle, maxSeconds: Float = 400f): Battle {
        var think = 0f
        while (b.phase == BattlePhase.PLAYING && b.time < maxSeconds) {
            b.update(STEP)
            b.events.clear()
            think -= STEP
            if (think <= 0f) {
                think = 0.5f
                while (playOne(b)) Unit
            }
        }
        return b
    }

    private fun threat(b: Battle, lane: Int): Float = b.enemiesIn(lane).sumOf { (it.hp * (0.5f + it.y.coerceAtLeast(0f) / Battle.HEIGHT)).toDouble() }.toFloat()

    /** 打出一張現在打得出來的牌；沒有就回傳 false。 */
    fun playOne(b: Battle): Boolean {
        val order = b.hand.indices.sortedByDescending { b.hand[it].cost }
        for (i in order) {
            val card = b.hand[i]
            if (!b.canAfford(card)) continue
            val spot = choose(b, card) ?: continue
            if (b.play(i, spot.first, spot.second)) return true
        }
        return false
    }

    private fun choose(b: Battle, card: CardDef): Pair<Int, Int>? {
        val lanes = (0 until Battle.LANES)
        when (card.type) {
            CardType.CREATURE -> {
                val unit = card.unit!!
                val rows = if (unit.ranged) listOf(2, 1, 0) else listOf(0, 1, 2)
                // 先補有敵人卻沒人擋的車道，再補人最少的
                val lane = lanes.filter { l -> rows.any { b.slots[l][it] == null } }
                    .maxByOrNull { l -> threat(b, l) * 10f - b.creaturesIn(l).size * 3f + (if (b.creaturesIn(l).isEmpty()) 5f else 0f) } ?: return null
                val row = rows.first { b.slots[lane][it] == null }
                // 遠程不要單獨站在沒有前排的車道最前面
                return lane to row
            }
            CardType.SPELL -> return when (card) {
                Cards.wuleifu -> lanes.filter { b.enemiesIn(it).size >= (if (b.incense >= b.maxIncense - 1) 1 else 2) }.maxByOrNull { b.enemiesIn(it).size }?.let { it to 0 }
                Cards.dingshen -> lanes.filter { l -> b.enemiesIn(l).count { it.engaged } >= 2 }.maxByOrNull { threat(b, it) }?.let { it to 0 }
                Cards.thunderbolt -> lanes.filter { l -> b.enemiesIn(l).any { it.hp >= 6f } }.maxByOrNull { l -> b.enemiesIn(l).maxOf { it.hp } }?.let { it to 0 }
                Cards.olympus -> if (b.enemies.size >= 5) 0 to 0 else null
                Cards.feast -> lanes.filter { b.creaturesIn(it).size >= 2 && b.enemiesIn(it).isNotEmpty() }.maxByOrNull { threat(b, it) }?.let { it to 0 }
                Cards.pantao -> b.creatures().filter { it.hp < it.maxHp * 0.5f }.maxByOrNull { it.maxHp }?.let { it.lane to it.row }
                else -> null
            }
            CardType.ENCHANT -> return lanes.filter { b.enemiesIn(it).size >= 2 && b.creaturesIn(it).isNotEmpty() && b.enchants.none { e -> e.lane == it } }
                .maxByOrNull { threat(b, it) }?.let { it to 0 }
            CardType.EQUIP -> {
                val spec = card.equip!!
                val pool = b.creatures().filter { b.validTarget(card, it.lane, it.row) }
                val pick = if (spec.hp > 0f) pool.filter { !it.unit.ranged }.maxByOrNull { it.maxHp } else pool.maxByOrNull { it.atk }
                return pick?.let { it.lane to it.row }
            }
        }
    }
}
