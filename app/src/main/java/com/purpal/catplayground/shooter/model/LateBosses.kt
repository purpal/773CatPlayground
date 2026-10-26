package com.purpal.catplayground.shooter.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 第 10 關：樹懶郵差 閃電（樹懶）。兩隻手勾著空中的郵件纜線慢慢滑過去，身邊有一圈「慢動作力場」：
 * 子彈在圈內慢到幾乎不動，飛出圈外就突然恢復全速，彈幕會先在圈裡排好隊再一起衝出來。
 * 本體也慢吞吞的，正是打它的好時機。打到 35% 才「裝甲」（限時專送）：戴上護目鏡、穿上火箭滑板鞋，
 * 拿出快遞火箭筒——一直很慢的樹懶突然變超快：包裹飛彈、預告後的高速橫衝，力場也反轉成圈內子彈變超快。
 */
class SlothBoss : Boss(hp = 34000f, radius = 190f, score = 110000, homeY = 400f) {
    override val kind = EnemyKind.BOSS_SLOTH
    override val name = "樹懶郵差 閃電"
    override val breakAt = 0.35f
    override val breakKind = BreakKind.ARMOR
    override val weapon = HazardStyle.ROCKET
    override val lostProps = intArrayOf(LostProp.POSTCAP)
    override val handX = 150f
    override val handY = 140f

    /** 高速橫衝的那一排高度與預告剩餘秒數（畫面用來畫紅色預告帶）。 */
    var dashY = 0f
        private set
    var dashWarn = 0f
        private set
    private var dashFrom = 0f
    private var dashToX = Float.NaN
    private var dashToY = 0f
    private var dashStep = false
    /** 這一步移動用的速度上限（在 move 裡決定，衝刺結束的那一步也照衝刺的速度算）。 */
    private var speed = 520f
    override val maxMoveSpeed: Float get() = speed

    /** 郵件纜線的高度（畫面用來畫纜線）：兩隻手勾在上面。 */
    val cableY: Float get() = y - 235f
    /** 還勾在纜線上（裝甲後穿著火箭滑板鞋自己飛）。 */
    val onCable: Boolean get() = !broken

    /** 慢慢微笑（裝甲前）→ 限時專送 → 裝甲被打哭。 */
    override fun mood(): Int = when {
        !broken -> 0
        hpRatio > 0.12f -> 1
        else -> 2
    }

    override fun bulletTimeScale(bx: Float, by: Float): Float {
        if (entering || armorTime > 0f) return 1f
        if (dist2(bx, by, x, y) > FIELD_RADIUS * FIELD_RADIUS) return 1f
        return if (broken) FAST_FIELD else SLOW_FIELD
    }

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        // 衝刺被換階段打斷時直接回原位
        if (!dashStep) {
            dashToX = Float.NaN
            dashWarn = 0f
        }
        dashStep = false
        if (dashWarn > 0f) dashWarn -= dt
    }

    override fun move(w: World, dt: Float) {
        speed = when {
            !dashToX.isNaN() -> DASH_SPEED
            broken -> 900f
            else -> 520f
        }
        when {
            !dashToX.isNaN() -> {
                x = dashToX
                y = dashToY
            }
            broken -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * 1.3f) * 300f
                y = approach(y, homeY + sin(moveTime * 2.6f) * 30f, 4f, dt)
            }
            // 慢吞吞地沿著纜線滑
            else -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * 0.22f) * 260f
                y = approach(y, homeY, 2f, dt)
            }
        }
    }

    /** 信件慢慢飄出來，左右輪流轉彎。 */
    private val letterDrift = Attack(4f) { w, t, dt ->
        if (every(t, dt, 0.5f)) {
            val side = if ((t / 0.5f).toInt() % 2 == 0) 1f else -1f
            for (k in -1..1) w.shoot(x, y + 80f, w.aimAt(x, y) + k * 16f * DEG, 220f, BulletStyle.LETTER, Tint.PINK).turn = side * 0.35f
        }
    }
    /** 打哈欠：一圈圈泡泡在力場裡疊成好幾層，再一起散開。 */
    private val yawnRings = Attack(3.6f) { w, t, dt ->
        if (crossed(t, dt, 0.4f) || crossed(t, dt, 1.4f) || crossed(t, dt, 2.4f)) w.ring(x, y + 90f, 18, 170f, t, BulletStyle.BUBBLE, Tint.CYAN)
    }
    /** 慢慢伸爪：預告超長的爪擊，等很久才抓下去。 */
    private val slowClaw = Attack(3.4f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) melee(w, HazardStyle.CLAW, aimFrom(w, 0f, 120f), 70f, warn = 1.5f, active = 0.5f, fromX = 0f, fromY = 120f)
        if (every(t, dt, 1.1f, offset = 0.6f)) w.fan(x, y + 80f, w.aimAt(x, y), 3, 30f * DEG, 240f, BulletStyle.LETTER, Tint.PINK)
    }
    /** 力場螺旋：兩道螺旋在力場裡慢慢捲成一大團，飛出圈外才一起衝出來。 */
    private val fieldSpiral = Attack(4.6f) { w, t, dt ->
        if (every(t, dt, 0.12f)) {
            val a = t * 2.6f
            w.shoot(x, y + 40f, a, 210f, BulletStyle.ORB, Tint.RED)
            w.shoot(x, y + 40f, a + PI.toFloat(), 210f, BulletStyle.ORB, Tint.RED)
        }
    }
    /** 叫樹蛙跳下來。 */
    private val frogs = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(TreeFrog(130f + i * 148f, (i % 3) * 0.12f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 220f, BulletStyle.BIG_ORB, Tint.PINK)
    }
    /** 裝甲後：包裹飛彈左右輪流射出，往中間彎過來。 */
    private val parcelVolley = Attack(3.4f) { w, t, dt ->
        if (every(t, dt, 0.45f)) {
            val side = if ((t / 0.45f).toInt() % 2 == 0) 1f else -1f
            w.shoot(x + handX * side, y + handY, HALF_PI - side * 40f * DEG, 330f, BulletStyle.PARCEL, Tint.ORANGE).turn = side * 1.3f
        }
    }
    /** 裝甲後：先在玩家附近畫出一整排預告，再踩著火箭滑板鞋從畫面一邊高速衝到另一邊。 */
    private val rocketDash = Attack(4.4f) { w, t, dt ->
        dashStep = t < 3.6f
        if (crossed(t, dt, 0.01f)) {
            dashY = (w.player.y - 150f).coerceIn(homeY + 160f, maxOf(homeY + 160f, w.height * 0.7f))
            dashFrom = if (w.player.x > WORLD_WIDTH / 2f) 170f else WORLD_WIDTH - 170f
            dashToX = dashFrom
            dashToY = dashY
            dashWarn = DASH_WARN
            w.cue(SoundCue.SWING)
        }
        if (crossed(t, dt, DASH_WARN)) {
            dashToX = WORLD_WIDTH - dashFrom
            w.cue(SoundCue.MISSILE)
        }
        if (t > DASH_WARN && t < DASH_WARN + 1f && every(t, dt, 0.08f)) w.shoot(x, y, HALF_PI + w.jitter(0.6f), 140f, BulletStyle.ORB, Tint.ORANGE)
        if (crossed(t, dt, 2.4f)) {
            dashToX = WORLD_WIDTH / 2f
            dashToY = homeY
        }
    }
    /** 裝甲後：火箭筒的噴射火焰從玩家的另一側掃過來 70°，玩家那一側的外緣是安全區。 */
    private val jetSweep = Attack(3.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.ROCKET, if (playerLeft) HALF_PI - 55f * DEG else HALF_PI + 55f * DEG, 46f,
                warn = 0.9f, active = 1.4f, extend = 0.2f, sweep = if (playerLeft) 70f * DEG else -70f * DEG, length = 1150f,
            )
        }
        if (every(t, dt, 1.1f, offset = 1f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.PARCEL, Tint.ORANGE)
    }
    /** 裝甲後：反轉的力場——一圈圈子彈在圈內飛超快，出了圈才慢下來。 */
    private val burstRings = Attack(3.4f) { w, t, dt ->
        if (every(t, dt, 0.85f)) w.ring(x, y + 40f, 16, 150f, t, BulletStyle.ORB, Tint.RED)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(letterDrift, slowClaw, fieldSpiral, frogs, yawnRings)
        !broken -> listOf(fieldSpiral, letterDrift, yawnRings, slowClaw)
        phase < 2 -> listOf(parcelVolley, rocketDash, jetSweep, burstRings, frogs)
        else -> listOf(rocketDash, burstRings, parcelVolley, jetSweep)
    }

    companion object {
        const val FIELD_RADIUS = 260f
        const val SLOW_FIELD = 0.3f
        const val FAST_FIELD = 1.7f
        const val DASH_SPEED = 1500f
        const val DASH_WARN = 1.3f
    }
}

/**
 * 第 11 關：月夜怪盜 基德（浣熊）。揹著贓物布袋、拿電漿抓鉤槍，專偷畫面上的道具——沒有道具就偷貓咪的大絕。
 * 布袋可以打：打破會把偷走的東西全部吐出來、多送金幣，怪盜也會慌張一陣子（受到 1.5 倍傷害）。
 * 50% 被打到「卸甲」：抓鉤槍飛走，慌張地變出分身（只有真的那隻在月光下有影子）、翻垃圾桶丟桶蓋、洗手手濺水花。
 */
class RaccoonBoss : Boss(hp = 42000f, radius = 180f, score = 120000, homeY = 420f) {
    override val kind = EnemyKind.BOSS_RACCOON
    override val name = "月夜怪盜 基德"
    override val breakAt = 0.5f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.GRAPPLE
    override val lostWeapon = LostProp.GRAPPLE
    override val lostProps = intArrayOf(LostProp.TOPHAT)
    override val handX = 159f
    override val handY = 144f
    override val maxMoveSpeed = 900f

    /** 揹在背上的布袋（被打破後一段時間才會再拿出新的）。 */
    var bag: LootBag? = null
        private set
    private var bagRespawn = 0f
    /** 偷來的東西（打破布袋會全部吐出來）。 */
    val loot = ArrayList<ItemType>()
    /** 布袋被打破後慌張的剩餘秒數：受到 [PANIC_DAMAGE] 倍傷害。 */
    var panic = 0f
        private set
    override val damageScale: Float get() = if (panic > 0f) PANIC_DAMAGE else 1f
    /** 分身術時真的那隻要站的位置（NaN 表示沒在變分身）。 */
    private var slotX = Float.NaN
    private var decoyStep = false
    private var snatchItem: Item? = null
    private var snatchPlayer = false
    private var snatchLaser: Laser? = null

    override fun update(w: World, dt: Float) {
        if (bag == null && bagRespawn <= 0f && !entering) bag = LootBag(this).also { w.spawn(it) }
        super.update(w, dt)
        bag?.let {
            if (!it.alive) {
                bag = null
                bagRespawn = BAG_RESPAWN
                spill(w, it)
            }
        }
        if (bagRespawn > 0f) bagRespawn -= dt
        if (panic > 0f) panic -= dt
        if (!decoyStep) slotX = Float.NaN
        decoyStep = false
    }

    override fun move(w: World, dt: Float) {
        when {
            !slotX.isNaN() -> x = slotX
            // 慌張得跑來跑去
            panic > 0f -> x = WORLD_WIDTH / 2f + sin(moveTime * 2.4f) * 260f
            else -> x = WORLD_WIDTH / 2f + sin(moveTime * 0.6f) * 200f
        }
        y = approach(y, homeY, 3f, dt)
    }

    /** 布袋破了：偷來的東西和一堆金幣掉出來，怪盜慌張起來。 */
    private fun spill(w: World, b: LootBag) {
        for ((i, type) in loot.withIndex()) w.items += Item(b.x + (i - loot.size / 2f) * 60f, b.y, type)
        repeat(8) { w.items += Item(b.x + w.jitter(90f), b.y + w.jitter(40f), ItemType.COIN) }
        loot.clear()
        panic = PANIC_TIME
        w.sfx(x, y - 150f, SfxText.CRITICAL)
        w.shake(0.3f)
    }

    /**
     * 抓鉤偷東西：瞄準畫面上最近的道具（武器膠囊、修理…）射出抓鉤，抓到就塞進布袋；
     * 沒有道具就抓貓咪，抓中的話偷走一個大絕。
     */
    private val snatch = Attack(3.2f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val item = w.items.filter { it.alive && it.type != ItemType.DATA && it.type != ItemType.COIN }
                .minByOrNull { dist2(it.x, it.y, x, y) }
            snatchItem = item
            snatchPlayer = item == null
            val tx = item?.x ?: w.player.x
            val ty = item?.y ?: w.player.y
            val len = (sqrt(dist2(x + handX, y + handY, tx, ty)) + 60f).coerceIn(300f, 1600f)
            snatchLaser = melee(w, HazardStyle.GRAPPLE, angleTo(x + handX, y + handY, tx, ty), 34f, warn = 0.55f, active = 0.6f, extend = 0.15f, length = len)
        }
        if (crossed(t, dt, 0.75f)) {
            val l = snatchLaser
            val item = snatchItem
            if (l != null && l.alive) {
                if (item != null && item.alive && dist2(item.x, item.y, l.tipX, l.tipY) < SNATCH_REACH * SNATCH_REACH) {
                    item.alive = false
                    loot += item.type
                    w.sparks(l.tipX, l.tipY, 10, 0xFFFF2BD6L)
                    w.cue(SoundCue.ITEM)
                } else if (snatchPlayer && w.player.bombs > 0 &&
                    l.distance2To(w.player.x, w.player.y) < (l.width * 0.4f + w.player.radius + 10f).let { it * it }
                ) {
                    w.player.bombs--
                    loot += ItemType.BOMB
                    w.sparks(w.player.x, w.player.y, 10, 0xFFFF2BD6L)
                }
            }
        }
        if (every(t, dt, 1f, offset = 1.3f)) w.fan(x, y + 60f, w.aimAt(x, y), 3, 30f * DEG, 300f, BulletStyle.CARD, Tint.PINK)
    }
    /** 撲克牌飛刀：扇形連射，旋轉著飛過來。 */
    private val cardFan = Attack(3f) { w, t, dt ->
        if (every(t, dt, 0.6f)) w.fan(x, y + 60f, w.aimAt(x, y), 5, 50f * DEG, 320f, BulletStyle.CARD, Tint.PINK)
    }
    /** 抓鉤朝玩家突刺兩次。 */
    private val grappleThrust = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.3f)) melee(w, HazardStyle.GRAPPLE, aimFrom(w, handX, handY), 40f, warn = 0.7f)
        if (every(t, dt, 1.3f, offset = 0.8f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.CARD, Tint.PINK)
    }
    /** 煙霧彈：一圈圈紫色煙霧慢慢擴散。 */
    private val smokeRing = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 1.1f)) {
            w.ring(x, y + 40f, 14, 150f, t, BulletStyle.BUBBLE, Tint.PURPLE)
            w.puff(x, y + 40f, Tint.PURPLE)
        }
    }
    /** 叫負鼠過來。 */
    private val possums = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(Opossum(130f + i * 148f, (i % 3) * 0.12f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 220f, BulletStyle.BIG_ORB, Tint.PURPLE)
    }
    /** 卸甲後：變出兩隻分身，三隻換位置；只有真的那隻在月光下有影子。分身也會丟撲克牌。 */
    private val decoys = Attack(5.6f) { w, t, dt ->
        decoyStep = t < 5.4f
        if (crossed(t, dt, 0.3f)) {
            val slots = mutableListOf(230f, 500f, 770f).also { it.shuffle(w.random) }
            slotX = slots[0]
            for (k in 1..2) w.spawn(RaccoonDecoy(this, slots[k]))
            w.puff(x, y, Tint.PURPLE)
            w.cue(SoundCue.SWING)
        }
        if (t > 1f && every(t, dt, RaccoonDecoy.FIRE_EVERY, offset = 1f)) w.fan(x, y + 60f, w.aimAt(x, y), 3, 30f * DEG, 300f, BulletStyle.CARD, Tint.PINK)
    }
    /** 卸甲後：翻垃圾桶，往兩邊丟桶蓋迴力鏢。 */
    private val lidBoomerang = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.6f)) {
            for (side in listOf(-1f, 1f)) w.shoot(x + side * 120f, y + 60f, HALF_PI - side * 55f * DEG, 290f, BulletStyle.LID, Tint.PURPLE).turn = side * 1.6f
        }
    }
    /** 卸甲後：慌張地洗手手，濺出一圈圈水花。 */
    private val washSplash = Attack(3.4f) { w, t, dt ->
        if (every(t, dt, 0.8f)) w.ring(x, y + 80f, 12, 170f, t, BulletStyle.BUBBLE, Tint.CYAN)
        if (every(t, dt, 0.8f, offset = 0.4f)) w.fan(x, y + 80f, w.aimAt(x, y), 3, 26f * DEG, 300f, BulletStyle.ORB, Tint.CYAN)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(cardFan, snatch, grappleThrust, possums, smokeRing)
        !broken -> listOf(snatch, grappleThrust, cardFan, smokeRing)
        phase < 2 -> listOf(decoys, lidBoomerang, washSplash, possums)
        else -> listOf(decoys, lidBoomerang, cardFan, washSplash)
    }

    companion object {
        const val BAG_RESPAWN = 12f
        const val PANIC_TIME = 3f
        const val PANIC_DAMAGE = 1.5f
        const val SNATCH_REACH = 160f
    }
}

/** 怪盜揹的贓物布袋：跟著怪盜移動，可以打；打破會把偷來的東西吐出來（由 [RaccoonBoss] 處理）。 */
class LootBag(val boss: RaccoonBoss) : Enemy(boss.x - 140f, boss.y + 40f, HP, 56f, 1500) {
    override val kind = EnemyKind.LOOT_BAG
    override val contactDamage = 0f
    override val invulnerable: Boolean get() = boss.invulnerable
    /** 裡面有偷來的東西（畫面用：鼓起來、露出金幣）。 */
    val full: Boolean get() = boss.loot.isNotEmpty()

    override fun update(w: World, dt: Float) {
        age += dt
        x = boss.x - 140f
        y = boss.y + 40f
        if (!boss.alive) alive = false
    }

    companion object {
        const val HP = 1500f
    }
}

/** 怪盜的分身：長得一模一樣、也會丟撲克牌，但沒有影子；打一下就破，時間到也會消失。 */
class RaccoonDecoy(val boss: RaccoonBoss, private val slotX: Float) : Enemy(boss.x, boss.y, 60f, 150f, 100) {
    override val kind = EnemyKind.RACCOON_DECOY
    override val contactDamage = 0f

    override fun update(w: World, dt: Float) {
        age += dt
        val limit = 900f * dt
        x += (slotX - x).coerceIn(-limit, limit)
        y = boss.y
        if (age > 1f && every(age, dt, FIRE_EVERY, offset = 1f)) w.fan(x, y + 60f, w.aimAt(x, y), 3, 30f * DEG, 300f, BulletStyle.CARD, Tint.PINK)
        if (age > LIFE || !boss.alive) {
            alive = false
            w.puff(x, y, Tint.PURPLE)
        }
    }

    companion object {
        const val LIFE = 5.1f
        const val FIRE_EVERY = 1.2f
    }
}

/**
 * 第 12 關：遺跡猴王 吱吱（猴子）。招牌「有樣學樣」：學玩家現在的主武器——散射就射香蕉散彈、
 * 集中就射香蕉光束、專武就丟巨大香蕉肉球；學的時候盯著玩家站著不動，正好打。
 * 還會盪藤蔓、丟香蕉迴力鏢、撒香蕉皮（停在地上一陣子）。打到 30% 才「裝甲」（猴王覺醒）：
 * 戴上金色頭箍、拿出伸縮電漿棍——伸到整個畫面的橫掃、長距離突刺，還會拔毛變出小分身一起學。
 */
class MonkeyBoss : Boss(hp = 36000f, radius = 190f, score = 130000, homeY = 420f) {
    override val kind = EnemyKind.BOSS_MONKEY
    override val name = "遺跡猴王 吱吱"
    override val breakAt = 0.3f
    override val breakKind = BreakKind.ARMOR
    override val weapon = HazardStyle.RUYI
    override val lostProps = intArrayOf(LostProp.HELMET)
    override val handX = 162f
    override val handY = 150f

    /** 正在模仿的主武器（畫面在頭上畫出那個武器的圖示）；null 表示沒在模仿。 */
    var mimicking: MainWeapon? = null
        private set
    private var mimicStep = false
    /** 抓著藤蔓盪鞦韆中。 */
    var swinging = false
        private set
    private var swingStep = false
    private var swingTime = 0f
    /** 盪鞦韆的藤蔓掛在這個點（畫面用來畫藤蔓）。 */
    val pivotX = WORLD_WIDTH / 2f
    val pivotY: Float get() = homeY - SWING_LENGTH
    private var speed = 520f
    override val maxMoveSpeed: Float get() = speed

    /** 調皮（裝甲前）→ 猴王覺醒 → 裝甲被打哭。 */
    override fun mood(): Int = when {
        !broken -> 0
        hpRatio > 0.12f -> 1
        else -> 2
    }

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        if (!mimicStep) mimicking = null
        mimicStep = false
        swinging = swingStep
        swingStep = false
    }

    override fun move(w: World, dt: Float) {
        speed = if (swinging) 900f else 520f
        when {
            // 學人的時候盯著玩家，站著不動
            mimicking != null -> Unit
            swinging -> {
                swingTime += dt
                val a = sin(swingTime * 1.6f) * 0.85f
                x = pivotX + sin(a) * SWING_LENGTH
                y = pivotY + cos(a) * SWING_LENGTH
            }
            broken -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * 0.9f) * 240f
                y = approach(y, homeY, 3f, dt)
            }
            else -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * 0.6f) * 200f
                y = approach(y, homeY, 3f, dt)
            }
        }
    }

    /** 有樣學樣：照玩家現在的主武器出招。 */
    private val mimic = Attack(3.8f) { w, t, dt ->
        mimicStep = t < 3.6f
        if (crossed(t, dt, 0.01f)) {
            mimicking = w.player.mainWeapon
            w.cue(SoundCue.ITEM)
        }
        when (mimicking) {
            MainWeapon.SPREAD -> if (every(t, dt, 0.55f, offset = 0.4f)) w.fan(x, y + 80f, w.aimAt(x, y), 7, 60f * DEG, 300f, BulletStyle.BANANA, Tint.CHEESE)
            MainWeapon.FOCUS -> if (crossed(t, dt, 0.4f) || crossed(t, dt, 1.9f)) {
                w.lasers += Laser(
                    x, y + 80f, w.aimAt(x, y + 80f), 1800f, 36f, 0.8f, 0.7f, Tint.RED, BEAM_DAMAGE,
                    owner = this, offsetX = 0f, offsetY = 80f,
                )
                w.cue(SoundCue.LASER)
            }
            MainWeapon.SPECIAL -> if (crossed(t, dt, 0.4f) || crossed(t, dt, 2f)) {
                w.aimed(x, y + 80f, 150f, BulletStyle.BIG_ORB, Tint.CHEESE, radius = 34f)
                w.ring(x, y + 80f, 12, 200f, t, BulletStyle.BANANA, Tint.CHEESE)
            }
            null -> Unit
        }
    }
    /** 抓著藤蔓盪過整個畫面，沿路往下丟香蕉。 */
    private val vineSwing = Attack(4.6f) { w, t, dt ->
        swingStep = t < 4.4f
        if (t > 0.6f && every(t, dt, 0.22f)) w.shoot(x, y + 60f, HALF_PI + w.jitter(0.4f), 260f, BulletStyle.BANANA, Tint.CHEESE)
    }
    /** 香蕉迴力鏢：左右兩邊彎回來。 */
    private val bananaBoomerang = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.6f)) {
            for (side in listOf(-1f, 1f)) w.shoot(x + handX * side, y + handY, HALF_PI + side * 50f * DEG, 320f, BulletStyle.BANANA, Tint.CHEESE).turn = -side * 1.9f
        }
    }
    /** 香蕉皮：丟出去會停在地上一陣子，再滑走。 */
    private val bananaPeel = Attack(3.8f) { w, t, dt ->
        if (every(t, dt, 0.3f)) {
            val b = w.shoot(x + w.jitter(200f), y + 60f, HALF_PI + w.jitter(0.5f), 240f, BulletStyle.BANANA, Tint.CHEESE)
            b.freezeAt = 0.4f + w.random.nextFloat() * 0.8f
            b.freezeTime = 2.2f
        }
    }
    /** 叫盪藤小猴下來。 */
    private val swingMonkeys = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 6) w.spawn(SwingMonkey(130f + i * 148f, (i % 3) * 0.12f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 220f, BulletStyle.BIG_ORB, Tint.CHEESE)
    }
    /** 裝甲後：伸縮棍伸到整個畫面，從玩家的另一側掃過來 70°，玩家那一側的外緣是安全區。 */
    private val ruyiSweep = Attack(3.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.RUYI, if (playerLeft) HALF_PI - 55f * DEG else HALF_PI + 55f * DEG, 48f,
                warn = 0.9f, active = 1.4f, extend = 0.25f, sweep = if (playerLeft) 70f * DEG else -70f * DEG, length = 1600f,
            )
        }
        if (every(t, dt, 1.1f, offset = 1f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.BANANA, Tint.CHEESE)
    }
    /** 裝甲後：伸縮棍朝玩家長距離突刺兩次。 */
    private val ruyiThrust = Attack(2.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.4f)) melee(w, HazardStyle.RUYI, aimFrom(w, handX, handY), 50f, warn = 0.7f, length = 1700f, extend = 0.18f)
    }
    /** 裝甲後：拔毛變出兩隻小分身，一起學玩家。 */
    private val clones = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            for (side in listOf(-1f, 1f)) w.spawn(MonkeyClone(this, (x + side * 260f).coerceIn(150f, WORLD_WIDTH - 150f), y + 60f))
            w.puff(x, y, Tint.CHEESE)
        }
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 240f, BulletStyle.BIG_ORB, Tint.CHEESE)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(mimic, bananaBoomerang, vineSwing, bananaPeel, swingMonkeys)
        !broken -> listOf(vineSwing, mimic, bananaPeel, bananaBoomerang)
        phase < 2 -> listOf(ruyiSweep, mimic, clones, ruyiThrust)
        else -> listOf(ruyiSweep, clones, mimic, ruyiThrust, bananaPeel)
    }

    companion object {
        const val SWING_LENGTH = 460f
    }
}

/** 猴王拔毛變出來的小分身：在旁邊晃來晃去，學玩家射香蕉散彈；一段時間後變回毛消失。 */
class MonkeyClone(val boss: MonkeyBoss, x: Float, y: Float) : Enemy(x, y, 400f, 90f, 500) {
    override val kind = EnemyKind.MONKEY_CLONE
    private val baseX = x

    override fun update(w: World, dt: Float) {
        age += dt
        x = baseX + sin(age * 1.4f) * 80f
        if (every(age, dt, 1.6f, offset = 0.8f)) w.fan(x, y + 40f, w.aimAt(x, y), 5, 50f * DEG, 280f, BulletStyle.BANANA, Tint.CHEESE)
        if (age > LIFE || !boss.alive) {
            alive = false
            w.puff(x, y, Tint.CHEESE)
        }
    }

    companion object {
        const val LIFE = 9f
    }
}

/**
 * 法國：松露獵人 佩奇（豬豬）。戴貝雷帽、穿條紋衫、圍紅領巾，拿著電漿法棍：法棍突刺、留安全區的橫掃、
 * 可頌迴力鏢、一圈圈轉出來的馬卡龍。招牌「泥巴坑」：泥巴炸彈先閃預告圈，落地變成泥巴坑，貓咪踩進去速度減半。
 * 還會用鼻子嗅出地底的松露——地上先冒出閃閃發亮的記號，接著松露彈出來炸開一圈。
 * 45% 被打到「卸甲」：法棍飛走，改成在泥巴裡打滾、泥巴噴泉、一口氣嗅出一大片松露。
 */
class PigBoss : Boss(hp = 34000f, radius = 200f, score = 100000, homeY = 430f) {
    override val kind = EnemyKind.BOSS_PIG
    override val name = "松露獵人 佩奇"
    override val breakAt = 0.45f
    override val breakKind = BreakKind.DISARM
    override val weapon = HazardStyle.BAGUETTE
    override val lostWeapon = LostProp.BAGUETTE
    override val lostProps = intArrayOf(LostProp.BERET)
    override val handX = 165f
    override val handY = 150f

    /** 在泥巴裡打滾中。 */
    var rolling = false
        private set
    private var rollStep = false
    /** 用鼻子嗅松露中（畫面用：鼻子發亮）。 */
    var sniffing = false
        private set
    private var sniffStep = false
    private var speed = 520f
    override val maxMoveSpeed: Float get() = speed

    override fun update(w: World, dt: Float) {
        super.update(w, dt)
        rolling = rollStep
        rollStep = false
        sniffing = sniffStep
        sniffStep = false
    }

    override fun move(w: World, dt: Float) {
        speed = if (rolling) 900f else 520f
        when {
            rolling -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * 2.2f) * 320f
                y = approach(y, homeY - 20f, 3f, dt)
            }
            // 嗅松露的時候低頭站著不動
            sniffing -> Unit
            else -> {
                x = WORLD_WIDTH / 2f + sin(moveTime * (if (broken) 0.8f else 0.55f)) * (if (broken) 230f else 190f)
                y = approach(y, homeY, 3f, dt)
            }
        }
    }

    /** 嗅出 count 顆松露：其中一顆在玩家腳下，其餘散在下半部。 */
    private fun plantTruffles(w: World, count: Int) {
        w.spawn(Truffle(w.player.x + w.jitter(60f), w.player.y + w.jitter(40f)))
        repeat(count - 1) {
            val top = y + 300f
            val bottom = maxOf(top + 60f, w.height * 0.78f)
            w.spawn(Truffle(100f + w.random.nextFloat() * (WORLD_WIDTH - 200f), top + w.random.nextFloat() * (bottom - top)))
        }
        w.cue(SoundCue.SWING)
    }

    /** 法棍朝玩家突刺兩次，順便轉出一圈馬卡龍。 */
    private val baguetteThrust = Attack(2.6f) { w, t, dt ->
        if (crossed(t, dt, 0.01f) || crossed(t, dt, 1.3f)) melee(w, HazardStyle.BAGUETTE, aimFrom(w, handX, handY), 42f, warn = 0.7f)
        if (every(t, dt, 1.3f, offset = 0.8f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.MACARON, Tint.PINK)
    }
    /** 法棍從玩家的另一側掃過來 70°，玩家那一側的外緣是安全區。 */
    private val baguetteSweep = Attack(3.8f) { w, t, dt ->
        if (crossed(t, dt, 0.01f)) {
            val playerLeft = w.player.x < x + handX
            melee(
                w, HazardStyle.BAGUETTE, if (playerLeft) HALF_PI - 55f * DEG else HALF_PI + 55f * DEG, 44f,
                warn = 0.9f, active = 1.4f, extend = 0.2f, sweep = if (playerLeft) 70f * DEG else -70f * DEG, length = 1150f,
            )
        }
        if (every(t, dt, 1.1f, offset = 1f)) w.ring(x, y + 40f, 12, 170f, t, BulletStyle.MACARON, Tint.PINK)
    }
    /** 可頌迴力鏢：左右兩邊彎回來。 */
    private val croissants = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.6f)) {
            for (side in listOf(-1f, 1f)) w.shoot(x + side * 130f, y + 100f, HALF_PI + side * 50f * DEG, 320f, BulletStyle.CROISSANT, Tint.CHEESE).turn = -side * 1.9f
        }
    }
    /** 馬卡龍旋轉圈：一圈圈錯開角度轉出來，再補瞄準的三連發。 */
    private val macaronRings = Attack(3.6f) { w, t, dt ->
        if (every(t, dt, 0.9f)) w.ring(x, y + 40f, 14, 170f, t * 1.3f, BulletStyle.MACARON, Tint.PINK)
        if (every(t, dt, 0.9f, offset = 0.45f)) w.fan(x, y + 80f, w.aimAt(x, y), 3, 26f * DEG, 300f, BulletStyle.MACARON, Tint.PINK)
    }
    /** 泥巴炸彈：三個落點先閃預告圈（其中一個在玩家腳下），落地變成泥巴坑。 */
    private val mudBombs = Attack(4f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) {
            w.spawn(MudPuddle(w.player.x, w.player.y))
            repeat(2) {
                w.spawn(MudPuddle(120f + w.random.nextFloat() * (WORLD_WIDTH - 240f), y + 350f + w.random.nextFloat() * (w.height * 0.75f - y - 350f).coerceAtLeast(60f)))
            }
            w.cue(SoundCue.SWING)
        }
        if (every(t, dt, 1.2f, offset = 1.4f)) w.fan(x, y + 80f, w.aimAt(x, y), 3, 30f * DEG, 260f, BulletStyle.CROISSANT, Tint.CHEESE)
    }
    /** 嗅松露：低頭用鼻子嗅，地上冒出閃閃發亮的記號，松露彈出來炸開。 */
    private val truffleHunt = Attack(4.2f) { w, t, dt ->
        sniffStep = t < 1.4f
        if (crossed(t, dt, 0.5f)) plantTruffles(w, if (broken) 6 else 4)
        if (t > 1.6f && every(t, dt, 0.8f, offset = 1.6f)) w.fan(x, y + 80f, w.aimAt(x, y), 4, 36f * DEG, 280f, BulletStyle.MACARON, Tint.PINK)
    }
    /** 卸甲後：在泥巴裡左右打滾，一路往下甩泥巴。 */
    private val mudRoll = Attack(4.2f) { w, t, dt ->
        rollStep = t < 4f
        if (t > 0.4f && every(t, dt, 0.1f)) w.shoot(x, y + 120f, HALF_PI + w.jitter(0.35f), 260f, BulletStyle.ORB, Tint.ORANGE)
    }
    /** 卸甲後：泥巴噴泉，泥巴球往上噴，劃著拋物線落下來。 */
    private val mudFountain = Attack(3.8f) { w, t, dt ->
        if (every(t, dt, 0.3f)) w.bullet(x, y - 40f, (w.player.x - x) / 2.2f + w.jitter(200f), -400f, BulletStyle.BIG_ORB, Tint.ORANGE).ay = 420f
    }
    /** 叫小雞衝過來。 */
    private val chicks = Attack(3f) { w, t, dt ->
        if (crossed(t, dt, 0.3f)) for (i in 0 until 7) w.spawn(Chick(110f + i * 130f, (i % 3) * 0.1f))
        if (every(t, dt, 1f, offset = 0.8f)) w.aimed(x, y + 80f, 220f, BulletStyle.BIG_ORB, Tint.PINK)
    }

    override fun attacks(phase: Int) = when {
        !broken && phase == 0 -> listOf(baguetteThrust, croissants, mudBombs, macaronRings, chicks)
        !broken -> listOf(baguetteSweep, truffleHunt, croissants, mudBombs, macaronRings)
        phase < 2 -> listOf(mudRoll, truffleHunt, mudFountain, mudBombs, chicks)
        else -> listOf(mudRoll, truffleHunt, macaronRings, mudFountain, mudBombs)
    }
}

/** 地底的松露：先冒出閃閃發亮的記號（預告），[WARN] 秒後彈出來炸開一圈子彈。打不到。 */
class Truffle(x: Float, y: Float) : Enemy(x, y, 1f, 60f, 0) {
    override val kind = EnemyKind.TRUFFLE
    override val invulnerable = true
    override val contactDamage = 0f

    override fun update(w: World, dt: Float) {
        age += dt
        if (crossed(age, dt, WARN)) {
            w.ring(x, y, 10, 190f, age, BulletStyle.ORB, Tint.PURPLE)
            w.burst(x, y, 0xFF5D4037L, 10, 260f)
            w.cue(SoundCue.EXPLODE_SMALL)
        }
        if (age > WARN + 0.4f) alive = false
    }

    companion object {
        const val WARN = 1.4f
    }
}

/** 松露獵人的泥巴坑：先閃預告圈，落地濺出一圈泥巴，之後貓咪踩進去會變慢；過一陣子乾掉。 */
class MudPuddle(x: Float, y: Float) : Enemy(x, y, 1f, 95f, 0) {
    override val kind = EnemyKind.MUD_PUDDLE
    override val invulnerable = true
    override val contactDamage = 0f
    /** 已經落地（預告結束）。 */
    val landed: Boolean get() = age >= WARN

    override fun playerSpeedFactor(px: Float, py: Float): Float =
        if (landed && dist2(px, py, x, y) < radius * radius) MUD_SLOW else 1f

    override fun update(w: World, dt: Float) {
        age += dt
        if (crossed(age, dt, WARN)) {
            w.ring(x, y, 8, 160f, age, BulletStyle.ORB, Tint.ORANGE)
            w.burst(x, y, 0xFF8D6E63L, 10, 240f)
            w.cue(SoundCue.EXPLODE_SMALL)
        }
        if (age > LIFE) alive = false
    }

    companion object {
        const val WARN = 1f
        const val LIFE = 7.5f
        const val MUD_SLOW = 0.5f
    }
}
