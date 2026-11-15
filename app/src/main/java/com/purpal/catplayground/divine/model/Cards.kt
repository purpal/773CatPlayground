package com.purpal.catplayground.divine.model

/** 神系（相當於魔法風雲會的顏色）。 */
enum class Faction(val label: String) { EAST("東方"), GREEK("希臘") }

enum class CardType(val label: String) { CREATURE("生物"), SPELL("法術"), ENCHANT("結界"), EQUIP("神器") }

/** 稀有度：同名卡最多放幾張、抽到多的換多少金幣、卡包裡出現的權重。 */
enum class Rarity(val label: String, val maxCopies: Int, val refund: Int, val weight: Int) {
    COMMON("普通", 2, 5, 60),
    RARE("稀有", 2, 15, 28),
    EPIC("史詩", 2, 40, 10),
    LEGEND("傳說", 1, 100, 2),
}

/** 這張牌要拖到哪裡：空格（生物）、一整條車道、全場、或場上的一隻生物。 */
enum class Target { SLOT, LANE, FIELD, CREATURE }

enum class Keyword(val label: String, val help: String) {
    GUARD("守護", "受到的傷害 -1"),
    TRAMPLE("踐踏", "多出來的傷害會打到後面的敵人"),
    SWEEP("橫掃", "同時打到左右車道的敵人"),
    PIERCE("貫穿", "打到車道上射程內的所有敵人"),
    ANTI_AIR("對空", "可以攻擊飛行的妖魔"),
    CHAIN("連鎖", "閃電會跳到左右車道最近的敵人（一半傷害）"),
}

/** 生物的能力值。射程以世界座標計（車道長 1400）；近戰是 [Battle.MELEE_RANGE]。 */
class UnitStats(
    val atk: Float,
    val hp: Float,
    val range: Float = Battle.MELEE_RANGE,
    /** 幾秒攻擊一次。 */
    val interval: Float = 1f,
    val keywords: Set<Keyword> = emptySet(),
    val onEnter: (Battle, Creature) -> Unit = { _, _ -> },
    /** 回傳 true 表示沒死（例如復活）。 */
    val onDeath: (Battle, Creature) -> Boolean = { _, _ -> false },
    val onKill: (Battle, Creature) -> Unit = { _, _ -> },
    val onAttack: (Battle, Creature, Enemy) -> Unit = { _, _, _ -> },
    /** 自己這條車道有敵人死掉。 */
    val onLaneEnemyDeath: (Battle, Creature) -> Unit = { _, _ -> },
) {
    val ranged: Boolean get() = range > Battle.MELEE_RANGE
}

/** 結界：蓋在一條車道上，持續一段時間。 */
class EnchantDef(
    val duration: Float,
    /** 敵人移動速度乘上這個倍率。 */
    val enemySpeed: Float = 1f,
    /** 車道上的敵人每秒受到的傷害。 */
    val burn: Float = 0f,
    val atkBonus: Float = 0f,
    /** 我方生物受到的傷害減少。 */
    val reduction: Float = 0f,
    /** 每一拍我方生物回復的生命。 */
    val regen: Float = 0f,
)

/** 神器：裝在一隻生物身上，跟著牠直到陣亡。 */
class EquipDef(
    val atk: Float = 0f,
    val hp: Float = 0f,
    /** 攻擊間隔乘上這個倍率（越小越快）。 */
    val interval: Float = 1f,
    val keywords: Set<Keyword> = emptySet(),
)

class CardDef(
    val id: String,
    val name: String,
    val faction: Faction,
    val type: CardType,
    val rarity: Rarity,
    /** 需要的香火。 */
    val cost: Int,
    val text: String,
    val target: Target,
    val unit: UnitStats? = null,
    val enchant: EnchantDef? = null,
    val equip: EquipDef? = null,
    /** 衍生物（例如哮天犬）：不能放進牌組，陣亡不進棄牌堆。 */
    val token: Boolean = false,
    /** 法術的效果；lane／row 是拖到的位置（[Target.FIELD] 時沒有意義）。 */
    val onPlay: (Battle, Int, Int) -> Unit = { _, _, _ -> },
) {
    override fun toString() = name
}

/** 第一版的卡池：東方 15 張、希臘 16 張。 */
object Cards {
    private fun creature(
        id: String, name: String, faction: Faction, rarity: Rarity, cost: Int, text: String, unit: UnitStats, token: Boolean = false,
    ) = CardDef(id, name, faction, CardType.CREATURE, rarity, cost, text, Target.SLOT, unit = unit, token = token)

    private fun spell(
        id: String, name: String, faction: Faction, rarity: Rarity, cost: Int, text: String, target: Target, effect: (Battle, Int, Int) -> Unit,
    ) = CardDef(id, name, faction, CardType.SPELL, rarity, cost, text, target, onPlay = effect)

    private fun enchant(id: String, name: String, faction: Faction, rarity: Rarity, cost: Int, text: String, def: EnchantDef) =
        CardDef(id, name, faction, CardType.ENCHANT, rarity, cost, text, Target.LANE, enchant = def)

    private fun equip(id: String, name: String, faction: Faction, rarity: Rarity, cost: Int, text: String, def: EquipDef) =
        CardDef(id, name, faction, CardType.EQUIP, rarity, cost, text, Target.CREATURE, equip = def)

    private const val SHORT = 380f
    private const val MID = 500f
    private const val LONG = 620f

    // ───────────── 東方 ─────────────

    val xiaotian = creature(
        "xiaotian", "哮天犬", Faction.EAST, Rarity.COMMON, 0, "二郎神的神犬，咬得又快又狠。",
        UnitStats(3f, 4f, interval = 0.7f), token = true,
    )

    val tudigong = creature(
        "tudigong", "土地公", Faction.EAST, Rarity.COMMON, 1, "入場：獲得 1 點香火。",
        UnitStats(1f, 4f, onEnter = { b, _ -> b.gainIncense(1) }),
    )
    val menshen = creature(
        "menshen", "門神", Faction.EAST, Rarity.COMMON, 2, "守護。站在最前面擋住妖魔。",
        UnitStats(1f, 9f, keywords = setOf(Keyword.GUARD)),
    )
    val leigong = creature(
        "leigong", "雷公", Faction.EAST, Rarity.COMMON, 3, "遠程、對空。打中的敵人暈眩 0.4 秒。",
        UnitStats(2f, 4f, MID, 1.2f, setOf(Keyword.ANTI_AIR), onAttack = { b, _, e -> b.stun(e, 0.4f) }),
    )
    val nezha = creature(
        "nezha", "哪吒", Faction.EAST, Rarity.RARE, 3, "遠程、連射。入場：火尖槍對這條車道最前面的敵人造成 4 點傷害。",
        UnitStats(2f, 5f, SHORT, 0.6f, setOf(Keyword.ANTI_AIR), onEnter = { b, c ->
            b.frontEnemy(c.lane, air = true)?.let { e ->
                b.fx(FxType.BOLT, c.x, c.y, c.lane, id = "nezha", x2 = e.x, y2 = e.y)
                b.damageEnemy(e, 4f, c)
            }
        }),
    )
    val guanyu = creature(
        "guanyu", "關公", Faction.EAST, Rarity.RARE, 4, "橫掃：青龍偃月刀同時砍到左右車道的敵人。",
        UnitStats(4f, 9f, interval = 1.1f, keywords = setOf(Keyword.SWEEP)),
    )
    val erlang = creature(
        "erlang", "二郎神", Faction.EAST, Rarity.EPIC, 5, "遠程、對空。入場：在這條車道的空格召喚哮天犬（3/4）。",
        UnitStats(4f, 7f, LONG, 1f, setOf(Keyword.ANTI_AIR), onEnter = { b, c -> b.summonToken(xiaotian, c.lane) }),
    )
    val wukong = creature(
        "wukong", "孫悟空", Faction.EAST, Rarity.LEGEND, 6, "踐踏。第一次陣亡時，拔一根毫毛以一半生命復活。",
        UnitStats(6f, 10f, interval = 0.9f, keywords = setOf(Keyword.TRAMPLE), onDeath = { b, c ->
            if (c.revived) false else {
                c.revived = true
                c.hp = c.maxHp / 2f
                b.fx(FxType.REVIVE, c.x, c.y, c.lane)
                true
            }
        }),
    )
    val longwang = creature(
        "longwang", "東海龍王", Faction.EAST, Rarity.EPIC, 7, "遠程、貫穿。入場：降下甘霖，我方所有生物回復 4 點生命。",
        UnitStats(4f, 9f, LONG, 1.3f, setOf(Keyword.PIERCE, Keyword.ANTI_AIR), onEnter = { b, _ -> b.creatures().forEach { b.heal(it, 4f) } }),
    )
    val wuleifu = spell("wuleifu", "五雷符", Faction.EAST, Rarity.COMMON, 2, "對一條車道上的所有敵人造成 4 點傷害。", Target.LANE) { b, lane, _ ->
        b.enemiesIn(lane).forEach { b.damageEnemy(it, 4f) }
    }
    val dingshen = spell("dingshen", "定身咒", Faction.EAST, Rarity.COMMON, 1, "一條車道上的所有敵人暈眩 3 秒。", Target.LANE) { b, lane, _ ->
        b.enemiesIn(lane).forEach { b.stun(it, 3f) }
    }
    val pantao = spell("pantao", "蟠桃", Faction.EAST, Rarity.COMMON, 2, "一隻生物回滿生命，生命上限 +3。", Target.CREATURE) { b, lane, row ->
        b.slots[lane][row]?.let {
            it.maxHp += 3f
            b.heal(it, it.maxHp)
        }
    }
    val bagua = enchant(
        "bagua", "八卦陣", Faction.EAST, Rarity.RARE, 3, "20 秒內，這條車道的敵人移動變慢 30%，每秒受到 1 點傷害。",
        EnchantDef(20f, enemySpeed = 0.7f, burn = 1f),
    )
    val ziqi = enchant(
        "ziqi", "紫氣東來", Faction.EAST, Rarity.EPIC, 4, "25 秒內，這條車道的我方生物攻擊力 +2。",
        EnchantDef(25f, atkBonus = 2f),
    )
    val jingubang = equip(
        "jingubang", "如意金箍棒", Faction.EAST, Rarity.RARE, 3, "攻擊力 +3，獲得踐踏。",
        EquipDef(atk = 3f, keywords = setOf(Keyword.TRAMPLE)),
    )
    val fenghuolun = equip("fenghuolun", "風火輪", Faction.EAST, Rarity.COMMON, 2, "攻擊速度 +50%。", EquipDef(interval = 1f / 1.5f))

    // ───────────── 希臘 ─────────────

    val hermes = creature(
        "hermes", "荷米斯", Faction.GREEK, Rarity.COMMON, 1, "出手很快。入場：抽一張牌。",
        UnitStats(2f, 3f, interval = 0.7f, onEnter = { b, _ -> b.draw() }),
    )
    val artemis = creature(
        "artemis", "阿提米絲", Faction.GREEK, Rarity.COMMON, 2, "遠程、對空。射程很遠的月之弓。",
        UnitStats(2f, 3f, LONG, 1f, setOf(Keyword.ANTI_AIR)),
    )
    val hephaestus = creature(
        "hephaestus", "赫菲斯托斯", Faction.GREEK, Rarity.COMMON, 3, "入場：這條車道的其他我方生物攻擊力 +1。",
        UnitStats(2f, 7f, onEnter = { b, c ->
            b.creaturesIn(c.lane).filter { it !== c }.forEach {
                it.atkBonus += 1f
                b.fx(FxType.BUFF, it.x, it.y, it.lane)
            }
        }),
    )
    val apollo = creature(
        "apollo", "阿波羅", Faction.GREEK, Rarity.RARE, 3, "遠程、對空。每次攻擊都治療這條車道受傷最重的我方生物 1 點。",
        UnitStats(2f, 4f, MID, 1f, setOf(Keyword.ANTI_AIR), onAttack = { b, c, _ ->
            b.creaturesIn(c.lane).filter { it.hp < it.maxHp }.minByOrNull { it.hp / it.maxHp }?.let { b.heal(it, 1f) }
        }),
    )
    val heracles = creature(
        "heracles", "海克力斯", Faction.GREEK, Rarity.RARE, 4, "每打倒一個敵人，攻擊力 +1（最多 +4）。",
        UnitStats(4f, 8f, onKill = { b, c ->
            if (c.growth < 4) {
                c.growth++
                c.atkBonus += 1f
                b.fx(FxType.BUFF, c.x, c.y, c.lane)
            }
        }),
    )
    val athena = creature(
        "athena", "雅典娜", Faction.GREEK, Rarity.EPIC, 4, "守護。這條車道與左右車道的我方生物受到的傷害 -1。",
        UnitStats(3f, 9f, keywords = setOf(Keyword.GUARD)),
    )
    val hades = creature(
        "hades", "黑帝斯", Faction.GREEK, Rarity.EPIC, 5, "這條車道每有敵人死亡，回復 2 點生命、攻擊力 +1（最多 +5）。",
        UnitStats(4f, 8f, onLaneEnemyDeath = { b, c ->
            b.heal(c, 2f)
            if (c.growth < 5) {
                c.growth++
                c.atkBonus += 1f
            }
        }),
    )
    val poseidon = creature(
        "poseidon", "波賽頓", Faction.GREEK, Rarity.EPIC, 6, "遠程、對空。入場：海嘯把這條車道的敵人沖回去，並造成 3 點傷害。",
        UnitStats(4f, 8f, MID, 1.2f, setOf(Keyword.ANTI_AIR), onEnter = { b, c ->
            b.fx(FxType.SPELL, c.x, c.y, c.lane, id = "tsunami")
            b.enemiesIn(c.lane).forEach {
                b.knockback(it, 420f)
                b.damageEnemy(it, 3f, c)
            }
        }),
    )
    val zeus = creature(
        "zeus", "宙斯", Faction.GREEK, Rarity.LEGEND, 7, "遠程、對空、連鎖。入場：天降雷霆，對全場敵人造成 3 點傷害。",
        UnitStats(5f, 8f, 700f, 1.1f, setOf(Keyword.ANTI_AIR, Keyword.CHAIN), onEnter = { b, c ->
            b.fx(FxType.SPELL, c.x, c.y, c.lane, id = "zeus")
            b.enemies.toList().forEach { b.damageEnemy(it, 3f, c) }
        }),
    )
    val thunderbolt = spell("thunderbolt", "雷霆", Faction.GREEK, Rarity.COMMON, 3, "對一條車道上生命最高的敵人造成 10 點傷害，並暈眩 1 秒。", Target.LANE) { b, lane, _ ->
        b.enemiesIn(lane).maxByOrNull { it.hp }?.let {
            b.fx(FxType.BOLT, it.x, -100f, lane, id = "thunderbolt", x2 = it.x, y2 = it.y)
            b.stun(it, 1f)
            b.damageEnemy(it, 10f)
        }
    }
    val olympus = spell("olympus", "奧林帕斯聖火", Faction.GREEK, Rarity.RARE, 5, "對全場所有敵人造成 5 點傷害。", Target.FIELD) { b, _, _ ->
        b.enemies.toList().forEach { b.damageEnemy(it, 5f) }
    }
    val feast = spell("feast", "酒神盛宴", Faction.GREEK, Rarity.RARE, 2, "一條車道的我方生物回復 3 點生命，8 秒內攻擊速度加倍。", Target.LANE) { b, lane, _ ->
        b.creaturesIn(lane).forEach {
            b.heal(it, 3f)
            it.haste = 8f
        }
    }
    val labyrinth = enchant(
        "labyrinth", "迷宮", Faction.GREEK, Rarity.COMMON, 2, "15 秒內，這條車道的敵人移動速度減半。",
        EnchantDef(15f, enemySpeed = 0.5f),
    )
    val sanctuary = enchant(
        "sanctuary", "神盾聖域", Faction.GREEK, Rarity.RARE, 3, "20 秒內，這條車道的我方生物受到的傷害 -1，每一拍回復 1 點生命。",
        EnchantDef(20f, reduction = 1f, regen = 1f),
    )
    val trident = equip(
        "trident", "海神三叉戟", Faction.GREEK, Rarity.RARE, 3, "攻擊力 +2，獲得貫穿。",
        EquipDef(atk = 2f, keywords = setOf(Keyword.PIERCE)),
    )
    val aegis = equip(
        "aegis", "神盾埃癸斯", Faction.GREEK, Rarity.COMMON, 2, "生命 +4，獲得守護。",
        EquipDef(hp = 4f, keywords = setOf(Keyword.GUARD)),
    )

    /** 可以收集、放進牌組的卡（不含衍生物）。 */
    val all: List<CardDef> = listOf(
        tudigong, menshen, leigong, nezha, guanyu, erlang, wukong, longwang, wuleifu, dingshen, pantao, bagua, ziqi, jingubang, fenghuolun,
        hermes, artemis, hephaestus, apollo, heracles, athena, hades, poseidon, zeus, thunderbolt, olympus, feast, labyrinth, sanctuary, trident, aegis,
    )
    val tokens: List<CardDef> = listOf(xiaotian)

    private val byId = (all + tokens).associateBy { it.id }
    fun get(id: String): CardDef? = byId[id]

    /** 一開始送的牌組（24 張）。 */
    val starterDeck: List<CardDef> = listOf(
        tudigong, tudigong, menshen, menshen, leigong, leigong, hermes, hermes, artemis, artemis, hephaestus, hephaestus,
        wuleifu, wuleifu, thunderbolt, thunderbolt, pantao, pantao, dingshen, fenghuolun, labyrinth, nezha, heracles, guanyu,
    )

    const val DECK_SIZE = 24
}
