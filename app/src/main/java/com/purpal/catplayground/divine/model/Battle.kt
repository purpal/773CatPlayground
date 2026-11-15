package com.purpal.catplayground.divine.model

import kotlin.math.abs
import kotlin.random.Random

enum class BattlePhase { PLAYING, WON, LOST }

/** 給畫面播放特效與音效用的事件；模擬本身不依賴它。 */
enum class FxType {
    SUMMON, SPELL, ENCHANT, EQUIP, SLASH, SHOT_HIT, BOLT, ENEMY_DIE, CREATURE_DIE, BREACH, BEAT, HEAL, NUMBER, KNOCKBACK,
    TELEGRAPH, FOXFIRE, BOSS_VANISH, BOSS_APPEAR, INCENSE, REVIVE, ARROW, STUN, BUFF, DRAW, ENEMY_HIT_CREATURE,
}

class Fx(
    val type: FxType,
    val x: Float,
    val y: Float,
    val lane: Int = -1,
    val value: Float = 0f,
    /** 卡片或敵人的 id，畫面依它決定特效的樣子。 */
    val id: String = "",
    val x2: Float = 0f,
    val y2: Float = 0f,
)

/** 場上的我方生物。 */
class Creature(val def: CardDef, val lane: Int, val row: Int) {
    val unit: UnitStats = def.unit!!
    val x: Float = Battle.laneX(lane)
    val y: Float = Battle.ROW_Y[row]
    var maxHp = unit.hp
    var hp = unit.hp
    var atkBonus = 0f
    val keywords = HashSet(unit.keywords)
    val equips = ArrayList<CardDef>()
    /** 攻擊間隔的倍率（神器）。 */
    var intervalFactor = 1f
    /** 攻速加倍的剩餘秒數（酒神盛宴）。 */
    var haste = 0f
    var stun = 0f
    var cooldown = 0.5f
    var alive = true
    var revived = false
    /** 成長次數（海克力斯、黑帝斯）。 */
    var growth = 0

    // 畫面用
    var age = 0f
    var attackAge = 9f
    var hitFlash = 0f

    val atk: Float get() = unit.atk + atkBonus
    val antiAir: Boolean get() = Keyword.ANTI_AIR in keywords
}

enum class EnemyKind(
    val id: String,
    val label: String,
    val hp: Float,
    val atk: Float,
    val interval: Float,
    val speed: Float,
    /** 0 表示近戰。 */
    val range: Float = 0f,
    val flying: Boolean = false,
    /** 闖進神殿時扣的血。 */
    val breach: Int = 1,
    /** 攻擊同時打到整條車道的我方生物。 */
    val cleave: Boolean = false,
    /** 左右各多佔幾條車道（Boss）。 */
    val span: Int = 0,
    val radius: Float = 48f,
) {
    IMP("imp", "小鬼", 4f, 1f, 1.2f, 55f),
    ZOMBIE("zombie", "殭屍", 14f, 2f, 1.6f, 30f, breach = 2),
    FOX("fox", "狐妖", 3f, 1f, 0.7f, 110f),
    BAT("bat", "蝙蝠精", 3f, 0f, 1f, 75f, flying = true),
    SKELETON("skeleton", "骷髏弓手", 5f, 1.5f, 1.8f, 45f, range = 300f),
    OXHEAD("oxhead", "牛頭", 45f, 4f, 2f, 26f, breach = 5, cleave = true, radius = 70f),
    NINETAIL("ninetail", "九尾妖狐", 280f, 5f, 1.8f, 12f, breach = 20, cleave = true, span = 1, radius = 150f),
}

class Enemy(val kind: EnemyKind, var lane: Int, hpScale: Float, offset: Float) {
    var x = Battle.laneX(lane) + offset
    var y = Battle.SPAWN_Y
    val maxHp = kind.hp * hpScale
    var hp = maxHp
    var stun = 0f
    var cooldown = kind.interval * 0.5f
    var alive = true
    var invulnerable = false

    // Boss 用
    var fireTimer = 5f
    var summonTimer = 8f
    var vanish = 0f
    var phase = 0

    // 畫面用
    var age = 0f
    var attackAge = 9f
    var hitFlash = 0f
    /** 正在攻擊（停在生物前面）。 */
    var engaged = false

    val isBoss: Boolean get() = kind.span > 0
    fun covers(l: Int): Boolean = abs(l - lane) <= kind.span
}

/** 飛行中的遠程攻擊；打中才算傷害。 */
class Shot(val owner: Creature, var target: Enemy, val damage: Float) {
    var x = owner.x
    var y = owner.y - 50f
    var alive = true
    var age = 0f
}

class Enchant(val def: CardDef, val lane: Int) {
    val spec: EnchantDef = def.enchant!!
    var remaining = spec.duration
    var age = 0f
}

/** Boss 的狐火預告：時間到就燒整條車道。 */
class Telegraph(val lane: Int, var remaining: Float, val damage: Float) {
    val total = remaining
}

/**
 * 一場戰鬥。場地寬 1000、長 1400，分成 5 條車道；妖魔從上面走下來，每條車道下半段有 3 格可以放生物，
 * 最下面是神殿。每 [BEAT] 秒一拍：香火上限 +1 並回滿、抽一張牌。固定時間步進，不依賴畫面，可以直接跑單元測試。
 */
class Battle(val stage: StageDef, deck: List<CardDef>, seed: Int = stage.number) {
    val random = Random(seed)

    var phase = BattlePhase.PLAYING
        private set
    var time = 0f
        private set
    /** 這一拍已經過了幾秒（0～[BEAT]）。 */
    var beatTime = 0f
        private set
    var beats = 0
        private set
    var incense = START_INCENSE
        private set
    var maxIncense = START_INCENSE
        private set
    var templeHp = TEMPLE_HP
        private set

    val hand = ArrayList<CardDef>()
    val drawPile = ArrayList<CardDef>()
    val discard = ArrayList<CardDef>()

    val slots: Array<Array<Creature?>> = Array(LANES) { arrayOfNulls<Creature>(ROWS) }
    val enemies = ArrayList<Enemy>()
    val shots = ArrayList<Shot>()
    val enchants = ArrayList<Enchant>()
    val telegraphs = ArrayList<Telegraph>()
    val events = ArrayList<Fx>()

    private var nextSpawn = 0
    var kills = 0
        private set
    /** 還沒出場的敵人數（畫面顯示進度用）。 */
    val remainingSpawns: Int get() = stage.spawns.size - nextSpawn
    val totalSpawns: Int get() = stage.spawns.size

    init {
        drawPile += deck.shuffled(random)
        repeat(HAND_START) { draw(silent = true) }
    }

    fun fx(type: FxType, x: Float, y: Float, lane: Int = -1, value: Float = 0f, id: String = "", x2: Float = 0f, y2: Float = 0f) {
        if (events.size < MAX_EVENTS) events += Fx(type, x, y, lane, value, id, x2, y2)
    }

    // ───────────────────────── 查詢 ─────────────────────────

    fun creatures(): List<Creature> = slots.flatMap { lane -> lane.filterNotNull() }

    fun creaturesIn(lane: Int): List<Creature> = if (lane in 0 until LANES) slots[lane].filterNotNull() else emptyList()

    fun enemiesIn(lane: Int): List<Enemy> = enemies.filter { it.alive && !it.invulnerable && it.covers(lane) }

    /** 這條車道最靠近神殿的敵人。 */
    fun frontEnemy(lane: Int, air: Boolean): Enemy? = enemiesIn(lane).filter { air || !it.kind.flying }.maxByOrNull { it.y }

    /** 敵人在這條車道（Boss 是好幾條）會先撞到的生物。 */
    private fun blockerFor(e: Enemy): Creature? {
        var best: Creature? = null
        for (lane in 0 until LANES) {
            if (!e.covers(lane)) continue
            for (c in slots[lane]) if (c != null && c.y > e.y - 40f && (best == null || c.y < best.y)) best = c
        }
        return best
    }

    private fun laneEnchants(lane: Int) = enchants.filter { it.lane == lane }

    fun laneAtkBonus(lane: Int): Float = laneEnchants(lane).sumOf { it.spec.atkBonus.toDouble() }.toFloat()

    fun attackOf(c: Creature): Float = c.atk + laneAtkBonus(c.lane)

    private fun enemySpeedFactor(e: Enemy): Float {
        var f = 1f
        for (en in enchants) if (e.covers(en.lane)) f = minOf(f, en.spec.enemySpeed)
        // Boss 身軀龐大，緩速只有一半效果
        return if (e.isBoss) 1f - (1f - f) * 0.5f else f
    }

    /** 我方生物受到傷害時的減免：守護、結界、雅典娜的庇護。 */
    private fun reductionFor(c: Creature): Float {
        var r = if (Keyword.GUARD in c.keywords) 1f else 0f
        r += laneEnchants(c.lane).sumOf { it.spec.reduction.toDouble() }.toFloat()
        val athena = (c.lane - 1..c.lane + 1).any { l -> creaturesIn(l).any { it.def === Cards.athena && it !== c } }
        if (athena) r += 1f
        return r
    }

    // ───────────────────────── 出牌 ─────────────────────────

    fun canAfford(card: CardDef): Boolean = card.cost <= incense

    /** 這張牌能不能打在這個位置（不看香火）。 */
    fun validTarget(card: CardDef, lane: Int, row: Int): Boolean = when (card.target) {
        Target.FIELD -> true
        Target.LANE -> lane in 0 until LANES
        Target.SLOT -> lane in 0 until LANES && row in 0 until ROWS
        Target.CREATURE -> lane in 0 until LANES && row in 0 until ROWS && slots[lane][row].let { c ->
            c != null && (card.type != CardType.EQUIP || (c.equips.size < MAX_EQUIPS && card !in c.equips))
        }
    }

    /** 打出手牌；位置不合法或香火不夠就回傳 false、什麼都不做。 */
    fun play(handIndex: Int, lane: Int, row: Int): Boolean {
        if (phase != BattlePhase.PLAYING) return false
        val card = hand.getOrNull(handIndex) ?: return false
        if (!canAfford(card) || !validTarget(card, lane, row)) return false
        incense -= card.cost
        hand.removeAt(handIndex)
        when (card.type) {
            CardType.CREATURE -> {
                // 格子上已經有生物：舊的退場回到棄牌堆（不觸發陣亡效果）
                slots[lane][row]?.let { dismiss(it) }
                place(card, lane, row)
            }
            CardType.SPELL -> {
                fx(FxType.SPELL, laneX(lane.coerceIn(0, LANES - 1)), ROW_Y[0] - 300f, lane, id = card.id)
                card.onPlay(this, lane, row)
                discard += card
            }
            CardType.ENCHANT -> {
                // 同名結界重新計時
                enchants.removeAll { it.lane == lane && it.def === card }
                enchants += Enchant(card, lane)
                fx(FxType.ENCHANT, laneX(lane), ROW_Y[0], lane, id = card.id)
                discard += card
            }
            CardType.EQUIP -> {
                val c = slots[lane][row]!!
                val spec = card.equip!!
                c.equips += card
                c.atkBonus += spec.atk
                c.maxHp += spec.hp
                c.hp += spec.hp
                c.intervalFactor *= spec.interval
                c.keywords += spec.keywords
                fx(FxType.EQUIP, c.x, c.y, lane, id = card.id)
                discard += card
            }
        }
        return true
    }

    private fun place(card: CardDef, lane: Int, row: Int): Creature {
        val c = Creature(card, lane, row)
        slots[lane][row] = c
        fx(FxType.SUMMON, c.x, c.y, lane, id = card.id)
        c.unit.onEnter(this, c)
        return c
    }

    /** 在車道的空格（從前排找起）放一隻衍生物；沒有空格就不放。 */
    fun summonToken(card: CardDef, lane: Int): Creature? {
        val row = (0 until ROWS).firstOrNull { slots[lane][it] == null } ?: return null
        return place(card, lane, row)
    }

    private fun dismiss(c: Creature) {
        c.alive = false
        slots[c.lane][c.row] = null
        if (!c.def.token) discard += c.def
    }

    fun gainIncense(amount: Int) {
        incense = (incense + amount).coerceAtMost(MAX_INCENSE)
        fx(FxType.INCENSE, 0f, 0f, value = amount.toFloat())
    }

    /** 抽一張牌；牌庫空了就把棄牌堆洗回去。手牌滿了不抽。 */
    fun draw(silent: Boolean = false): Boolean {
        if (hand.size >= HAND_MAX) return false
        if (drawPile.isEmpty()) {
            if (discard.isEmpty()) return false
            drawPile += discard.shuffled(random)
            discard.clear()
        }
        hand += drawPile.removeAt(drawPile.size - 1)
        if (!silent) fx(FxType.DRAW, 0f, 0f)
        return true
    }

    // ───────────────────────── 傷害與治療 ─────────────────────────

    /** 回傳多出來的傷害（踐踏用）。 */
    fun damageEnemy(e: Enemy, amount: Float, by: Creature? = null): Float {
        if (!e.alive || e.invulnerable || amount <= 0f) return 0f
        e.hp -= amount
        e.hitFlash = 0.12f
        fx(FxType.NUMBER, e.x, e.y - e.kind.radius, e.lane, amount)
        if (e.hp > 0f) return 0f
        val over = -e.hp
        e.alive = false
        kills++
        fx(FxType.ENEMY_DIE, e.x, e.y, e.lane, id = e.kind.id)
        if (by != null && by.alive) by.unit.onKill(this, by)
        for (lane in 0 until LANES) if (e.covers(lane)) for (c in creaturesIn(lane)) c.unit.onLaneEnemyDeath(this, c)
        return over
    }

    fun damageCreature(c: Creature, amount: Float) {
        if (!c.alive || amount <= 0f) return
        val dmg = maxOf(amount - reductionFor(c), MIN_DAMAGE)
        c.hp -= dmg
        c.hitFlash = 0.12f
        fx(FxType.ENEMY_HIT_CREATURE, c.x, c.y, c.lane, dmg)
        if (c.hp > 0f) return
        if (c.unit.onDeath(this, c)) return
        c.alive = false
        slots[c.lane][c.row] = null
        if (!c.def.token) discard += c.def
        fx(FxType.CREATURE_DIE, c.x, c.y, c.lane, id = c.def.id)
    }

    fun heal(c: Creature, amount: Float) {
        if (!c.alive || c.hp >= c.maxHp) return
        c.hp = minOf(c.maxHp, c.hp + amount)
        fx(FxType.HEAL, c.x, c.y, c.lane, amount)
    }

    fun stun(e: Enemy, seconds: Float) {
        if (!e.alive || e.invulnerable) return
        // Boss 只吃一半的暈眩
        val s = if (e.isBoss) seconds * 0.5f else seconds
        if (s > e.stun) e.stun = s
        fx(FxType.STUN, e.x, e.y, e.lane, s)
    }

    fun knockback(e: Enemy, distance: Float) {
        if (!e.alive || e.invulnerable) return
        e.y = maxOf(SPAWN_Y, e.y - if (e.isBoss) distance * 0.3f else distance)
        fx(FxType.KNOCKBACK, e.x, e.y, e.lane)
    }

    // ───────────────────────── 更新 ─────────────────────────

    fun update(dt: Float) {
        if (phase != BattlePhase.PLAYING) return
        time += dt
        beatTime += dt
        if (beatTime >= BEAT) {
            beatTime -= BEAT
            beat()
        }
        spawnDue()
        updateEnchants(dt)
        updateTelegraphs(dt)
        for (lane in slots) for (c in lane) if (c != null) updateCreature(c, dt)
        updateShots(dt)
        for (e in enemies.toList()) if (e.alive) updateEnemy(e, dt)
        enemies.removeAll { !it.alive }
        shots.removeAll { !it.alive }
        if (templeHp <= 0) {
            phase = BattlePhase.LOST
        } else if (nextSpawn >= stage.spawns.size && enemies.isEmpty()) {
            phase = BattlePhase.WON
        }
    }

    private fun beat() {
        beats++
        maxIncense = (maxIncense + 1).coerceAtMost(MAX_INCENSE)
        incense = maxOf(incense, maxIncense)
        draw()
        for (en in enchants) if (en.spec.regen > 0f) for (c in creaturesIn(en.lane)) heal(c, en.spec.regen)
        fx(FxType.BEAT, 0f, 0f)
    }

    private fun spawnDue() {
        while (nextSpawn < stage.spawns.size && stage.spawns[nextSpawn].at <= time) {
            val s = stage.spawns[nextSpawn++]
            spawn(s.kind, if (s.lane >= 0) s.lane else random.nextInt(LANES))
        }
    }

    fun spawn(kind: EnemyKind, lane: Int): Enemy {
        val l = lane.coerceIn(kind.span, LANES - 1 - kind.span)
        val offset = if (kind.span > 0) 0f else (random.nextFloat() - 0.5f) * 90f
        val e = Enemy(kind, l, stage.hpScale, offset)
        enemies += e
        return e
    }

    private fun updateEnchants(dt: Float) {
        for (en in enchants) {
            en.remaining -= dt
            en.age += dt
            if (en.spec.burn > 0f) for (e in enemiesIn(en.lane)) burn(e, en.spec.burn * dt)
        }
        enchants.removeAll { it.remaining <= 0f }
    }

    /** 持續傷害：不跳數字、不閃白，死了一樣算擊殺。 */
    private fun burn(e: Enemy, amount: Float) {
        if (e.hp - amount > 0f) e.hp -= amount else damageEnemy(e, amount)
    }

    private fun updateTelegraphs(dt: Float) {
        for (t in telegraphs) {
            t.remaining -= dt
            if (t.remaining <= 0f) {
                fx(FxType.FOXFIRE, laneX(t.lane), ROW_Y[1], t.lane)
                for (c in creaturesIn(t.lane)) damageCreature(c, t.damage)
            }
        }
        telegraphs.removeAll { it.remaining <= 0f }
    }

    private fun updateCreature(c: Creature, dt: Float) {
        c.age += dt
        c.attackAge += dt
        if (c.hitFlash > 0f) c.hitFlash -= dt
        if (c.haste > 0f) c.haste -= dt
        if (c.stun > 0f) {
            c.stun -= dt
            return
        }
        c.cooldown -= dt * (if (c.haste > 0f) 2f else 1f)
        if (c.cooldown > 0f) return
        val target = targetFor(c) ?: return
        c.cooldown = c.unit.interval * c.intervalFactor
        c.attackAge = 0f
        if (c.unit.ranged) {
            shots += Shot(c, target, attackOf(c))
        } else {
            fx(FxType.SLASH, target.x, target.y, c.lane, id = c.def.id)
            hit(c, target, attackOf(c))
        }
    }

    private fun targetFor(c: Creature): Enemy? {
        var best: Enemy? = null
        for (e in enemies) {
            if (!e.alive || e.invulnerable || !e.covers(c.lane)) continue
            if (e.kind.flying && !c.antiAir) continue
            if (e.y < c.y - c.unit.range - e.kind.radius || e.y > c.y + 120f || e.y < -20f) continue
            if (best == null || e.y > best.y) best = e
        }
        return best
    }

    /** 一次攻擊命中：依關鍵字把傷害分給其他敵人。 */
    private fun hit(c: Creature, target: Enemy, dmg: Float) {
        if (Keyword.PIERCE in c.keywords) {
            for (e in enemiesIn(c.lane)) {
                if (e === target || (e.kind.flying && !c.antiAir)) continue
                if (e.y >= c.y - c.unit.range - e.kind.radius && e.y > -20f) damageEnemy(e, dmg, c)
            }
        }
        if (Keyword.SWEEP in c.keywords) {
            for (side in intArrayOf(-1, 1)) {
                for (e in enemiesIn(c.lane + side)) {
                    if (e === target || e.covers(c.lane) || e.kind.flying) continue
                    if (abs(e.y - target.y) < SWEEP_REACH) damageEnemy(e, dmg, c)
                }
            }
        }
        if (Keyword.CHAIN in c.keywords) {
            for (side in intArrayOf(-1, 1)) {
                val e = enemiesIn(c.lane + side).filter { it !== target && !it.covers(c.lane) && it.y > -20f }.maxByOrNull { it.y } ?: continue
                fx(FxType.BOLT, target.x, target.y, c.lane, id = "chain", x2 = e.x, y2 = e.y)
                damageEnemy(e, dmg * 0.5f, c)
            }
        }
        c.unit.onAttack(this, c, target)
        val over = damageEnemy(target, dmg, c)
        if (over > 0f && Keyword.TRAMPLE in c.keywords) {
            val next = enemiesIn(c.lane).filter { it !== target && !it.kind.flying && it.y > target.y - TRAMPLE_REACH }.maxByOrNull { it.y }
            if (next != null) damageEnemy(next, over, c)
        }
    }

    private fun updateShots(dt: Float) {
        for (s in shots) {
            s.age += dt
            if (!s.owner.alive && s.age > 2f) {
                s.alive = false
                continue
            }
            if (!s.target.alive || s.target.invulnerable) {
                // 目標沒了：改打同一條車道最前面的敵人，沒有就消失
                val next = frontEnemy(s.owner.lane, s.owner.antiAir)
                if (next == null) {
                    s.alive = false
                    continue
                }
                s.target = next
            }
            val dx = s.target.x - s.x
            val dy = s.target.y - s.y
            val d = kotlin.math.sqrt(dx * dx + dy * dy)
            val step = SHOT_SPEED * dt
            if (d <= step + 20f) {
                s.alive = false
                fx(FxType.SHOT_HIT, s.target.x, s.target.y, s.owner.lane, id = s.owner.def.id)
                hit(s.owner, s.target, s.damage)
            } else {
                s.x += dx / d * step
                s.y += dy / d * step
            }
        }
    }

    private fun updateEnemy(e: Enemy, dt: Float) {
        e.age += dt
        e.attackAge += dt
        if (e.hitFlash > 0f) e.hitFlash -= dt
        if (e.isBoss && updateBoss(e, dt)) return
        if (e.stun > 0f) {
            e.stun -= dt
            return
        }
        val blocker = if (e.kind.flying) null else blockerFor(e)
        val stopY = when {
            blocker == null -> TEMPLE_Y
            e.kind.range > 0f -> blocker.y - e.kind.range
            else -> blocker.y - CONTACT - (e.kind.radius - 48f)
        }
        if (e.y < stopY) {
            e.engaged = false
            e.y = minOf(stopY, e.y + e.kind.speed * enemySpeedFactor(e) * dt)
            if (blocker == null && e.y >= TEMPLE_Y) breach(e)
            return
        }
        if (blocker == null) {
            breach(e)
            return
        }
        e.engaged = true
        e.cooldown -= dt
        if (e.cooldown > 0f) return
        e.cooldown = e.kind.interval
        e.attackAge = 0f
        if (e.kind.range > 0f) fx(FxType.ARROW, e.x, e.y, e.lane, x2 = blocker.x, y2 = blocker.y)
        if (e.kind.cleave) {
            for (lane in 0 until LANES) if (e.covers(lane)) for (c in creaturesIn(lane)) damageCreature(c, e.kind.atk)
        } else {
            damageCreature(blocker, e.kind.atk)
        }
    }

    private fun breach(e: Enemy) {
        e.alive = false
        templeHp = (templeHp - e.kind.breach).coerceAtLeast(0)
        fx(FxType.BREACH, e.x, TEMPLE_Y, e.lane, e.kind.breach.toFloat())
    }

    /**
     * 九尾妖狐：慢慢往神殿走，一路放狐火（預告後燒整條車道）、叫小狐妖；
     * 血量掉到 2/3 與 1/3 時化成妖霧消失，換一邊從頭再來。回傳 true 表示這一步不照一般敵人處理。
     */
    private fun updateBoss(e: Enemy, dt: Float): Boolean {
        if (e.vanish > 0f) {
            e.vanish -= dt
            if (e.vanish <= 0f) {
                e.invulnerable = false
                e.y = 60f
                fx(FxType.BOSS_APPEAR, e.x, e.y, e.lane)
            }
            return true
        }
        val wanted = if (e.hp <= e.maxHp / 3f) 2 else if (e.hp <= e.maxHp * 2f / 3f) 1 else 0
        if (wanted > e.phase) {
            e.phase = wanted
            e.invulnerable = true
            e.vanish = BOSS_VANISH
            e.stun = 0f
            fx(FxType.BOSS_VANISH, e.x, e.y, e.lane)
            e.lane = if (e.lane <= 2) 3 else 1
            e.x = laneX(e.lane)
            for (lane in 0 until LANES) spawn(if (lane % 2 == 0) EnemyKind.FOX else EnemyKind.IMP, lane)
            return true
        }
        e.fireTimer -= dt
        if (e.fireTimer <= 0f) {
            e.fireTimer = BOSS_FIRE_EVERY - e.phase
            val lanes = (0 until LANES).filter { creaturesIn(it).isNotEmpty() && telegraphs.none { t -> t.lane == it } }
            if (lanes.isNotEmpty()) {
                val lane = lanes[random.nextInt(lanes.size)]
                telegraphs += Telegraph(lane, BOSS_FIRE_WARN, BOSS_FIRE_DAMAGE)
                fx(FxType.TELEGRAPH, laneX(lane), ROW_Y[1], lane, BOSS_FIRE_WARN)
            }
        }
        e.summonTimer -= dt
        if (e.summonTimer <= 0f) {
            e.summonTimer = BOSS_SUMMON_EVERY - e.phase * 2f
            repeat(2) { spawn(EnemyKind.FOX, random.nextInt(LANES)) }
        }
        return false
    }

    companion object {
        const val WIDTH = 1000f
        const val HEIGHT = 1400f
        const val LANES = 5
        const val ROWS = 3
        const val LANE_WIDTH = WIDTH / LANES
        /** 三排格子的位置；第 0 排最前面（最靠近妖魔）。 */
        val ROW_Y = floatArrayOf(880f, 1040f, 1200f)
        const val SPAWN_Y = -60f
        const val TEMPLE_Y = 1320f
        const val MELEE_RANGE = 130f
        /** 近戰敵人停在生物前面多遠。 */
        const val CONTACT = 90f
        const val SWEEP_REACH = 150f
        const val TRAMPLE_REACH = 220f
        const val SHOT_SPEED = 1500f
        /** 減傷後至少還會受到的傷害。 */
        const val MIN_DAMAGE = 0.5f

        const val BEAT = 5f
        const val START_INCENSE = 2
        const val MAX_INCENSE = 10
        const val HAND_START = 5
        const val HAND_MAX = 7
        const val TEMPLE_HP = 20
        const val MAX_EQUIPS = 2
        const val MAX_EVENTS = 400

        const val BOSS_VANISH = 2.5f
        const val BOSS_FIRE_EVERY = 7f
        const val BOSS_FIRE_WARN = 1.6f
        const val BOSS_FIRE_DAMAGE = 4f
        const val BOSS_SUMMON_EVERY = 11f

        fun laneX(lane: Int): Float = lane * LANE_WIDTH + LANE_WIDTH / 2f
    }
}
