package com.purpal.catplayground.shooter.model

enum class StageTheme { CHEESE_BARN, PARK, HONEY_FOREST }

/** 對話的說話者：貓咪本人或該關 Boss。 */
enum class Speaker { CAT, BOSS }

class DialogLine(val speaker: Speaker, val text: String)

/** 依時間排程的出怪表。 */
class StageScript {
    class Event(val time: Float, val action: (World) -> Unit)

    val events = ArrayList<Event>()
    var length = 0f
        private set

    fun at(time: Float, action: (World) -> Unit) {
        events += Event(time, action)
        length = maxOf(length, time)
    }

    /** 每隔 interval 秒出現一次，共 count 次。 */
    fun line(start: Float, count: Int, interval: Float, action: (World, Int) -> Unit) {
        repeat(count) { i -> at(start + i * interval) { w -> action(w, i) } }
    }

    fun sorted(): List<Event> = events.sortedBy { it.time }
}

class StageDef(
    val number: Int,
    val animal: String,
    val title: String,
    val theme: StageTheme,
    val bossName: String,
    val createBoss: () -> Boss,
    val outro: List<DialogLine>,
    val script: StageScript,
)

/** 選關畫面顯示的全部 13 關；`stage` 為 null 表示還在製作中。 */
class StageEntry(val number: Int, val animal: String, val title: String, val emoji: String, val stage: StageDef?)

object Stages {

    private fun script(build: StageScript.() -> Unit) = StageScript().apply(build)

    private fun cat(text: String) = DialogLine(Speaker.CAT, text)
    private fun boss(text: String) = DialogLine(Speaker.BOSS, text)

    val mouse = StageDef(
        number = 1,
        animal = "老鼠",
        title = "起司倉庫",
        theme = StageTheme.CHEESE_BARN,
        bossName = "起司鼠王",
        createBoss = ::MouseBoss,
        outro = listOf(
            boss("吱…你的肉球真的好可愛…起司分你一半…"),
            cat("謝謝招待喵！下一站，出發！"),
        ),
        script = script {
            line(1.5f, 5, 0.5f) { w, i -> w.spawn(MouseRunner(200f + i * 150f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(7f, 4, 0.7f) { w, i -> w.spawn(MousePlane(fromLeft = true, startY = 260f + i * 30f)) }
            line(10f, 6, 0.4f) { w, i -> w.spawn(MouseRunner(820f - i * 120f, wiggle = 1.5f)) }
            at(13f) { it.spawn(CheeseDrone(300f, 360f)); it.spawn(CheeseDrone(700f, 300f)) }
            at(16f) { it.spawn(Balloon(260f, ItemType.HOMING)) }
            line(19f, 4, 0.7f) { w, i -> w.spawn(MousePlane(fromLeft = false, startY = 220f + i * 30f)) }
            line(22f, 8, 0.3f) { w, i -> w.spawn(MouseRunner(100f + (i % 4) * 260f)) }
            at(25f) { it.spawn(Balloon(720f, ItemType.FOCUS)) }
            at(27f) { it.spawn(CheeseDrone(500f, 320f)) }
            line(28f, 3, 1f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 0, startY = 300f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.SPECIAL)) }
            line(35f, 10, 0.25f) { w, i -> w.spawn(MouseRunner(80f + i * 90f, wiggle = 0.5f)) }
            at(39f) { it.spawn(CheeseDrone(250f, 280f)); it.spawn(CheeseDrone(750f, 280f)) }
            at(43f) { it.spawn(Balloon(300f, ItemType.REPAIR)) }
            line(45f, 6, 0.6f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 0, startY = 200f + i * 40f)) }
            at(50f) { it.spawn(Balloon(700f, ItemType.SONIC)) }
            line(52f, 3, 1.2f) { w, i -> w.spawn(CheeseDrone(250f + i * 250f, 260f + i * 40f)) }
            line(57f, 12, 0.22f) { w, i -> w.spawn(MouseRunner(if (i % 2 == 0) 150f + i * 30f else 850f - i * 30f)) }
            at(61f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(63f, 4, 0.8f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 1, startY = 260f)) }
            at(68f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val dog = StageDef(
        number = 2,
        animal = "狗狗",
        title = "汪汪公園",
        theme = StageTheme.PARK,
        bossName = "柴犬隊長",
        createBoss = ::DogBoss,
        outro = listOf(
            boss("嗚汪…我、我只是想被摸摸頭…"),
            cat("摸摸～你也很可愛，但第一名是我喵！"),
        ),
        script = script {
            line(1.5f, 3, 0.8f) { w, i -> w.spawn(Puppy(250f + i * 250f, 300f + (i % 2) * 80f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.FOCUS)) }
            line(6f, 3, 1f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 0, startY = 250f + i * 60f)) }
            line(10f, 3, 1.2f) { w, i -> w.spawn(BallTurret(200f + i * 300f)) }
            at(14f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(15f, 4, 0.6f) { w, i -> w.spawn(Puppy(150f + i * 230f, 260f)) }
            line(20f, 4, 0.8f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + i * 40f)) }
            at(24f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(25f, 4, 0.9f) { w, i -> w.spawn(BallTurret(150f + i * 230f)) }
            line(30f, 6, 0.5f) { w, i -> w.spawn(MouseRunner(120f + i * 150f)) }
            at(34f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(36f, 5, 0.7f) { w, i -> w.spawn(Puppy(100f + i * 200f, 240f + (i % 2) * 120f)) }
            at(41f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(43f, 6, 0.5f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 0, startY = 220f + (i % 3) * 70f)) }
            line(48f, 4, 0.6f) { w, i -> w.spawn(BallTurret(120f + i * 250f)) }
            at(52f) { it.spawn(Balloon(600f, ItemType.SPREAD)) }
            line(54f, 6, 0.6f) { w, i -> w.spawn(Puppy(100f + i * 160f, 280f)) }
            at(60f) { it.spawn(Balloon(500f, ItemType.FOCUS)) }
            line(61f, 8, 0.4f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val bear = StageDef(
        number = 3,
        animal = "熊熊",
        title = "蜂蜜森林",
        theme = StageTheme.HONEY_FOREST,
        bossName = "熊熊大王",
        createBoss = ::BearBoss,
        outro = listOf(
            boss("蜂蜜…分你吃…你贏了…"),
            cat("甜甜的！萌王寶座又近了一步喵！"),
        ),
        script = script {
            line(1.5f, 8, 0.25f) { w, i -> w.spawn(Bee(150f + i * 100f, i * 0.1f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(7f, 2, 1.5f) { w, i -> w.spawn(HoneyPot(300f + i * 400f)) }
            line(10f, 3, 1f) { w, i -> w.spawn(BearCub(200f + i * 300f)) }
            at(14f) { it.spawn(Balloon(700f, ItemType.HOMING)) }
            line(15f, 10, 0.2f) { w, i -> w.spawn(Bee(850f - i * 70f, 0f)) }
            line(19f, 3, 0.8f) { w, i -> w.spawn(HoneyPot(200f + i * 300f)) }
            at(23f) { it.spawn(Balloon(300f, ItemType.SPECIAL)) }
            line(24f, 4, 0.8f) { w, i -> w.spawn(BearCub(150f + i * 230f)) }
            line(29f, 12, 0.18f) { w, i -> w.spawn(Bee(100f + (i % 6) * 160f, (i / 6) * 0.4f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 3, 0.9f) { w, i -> w.spawn(CheeseDrone(250f + i * 250f, 300f)) }
            at(40f) { it.spawn(Balloon(650f, ItemType.SONIC)) }
            line(41f, 4, 0.7f) { w, i -> w.spawn(BearCub(100f + i * 260f)) }
            line(45f, 4, 1f) { w, i -> w.spawn(HoneyPot(150f + i * 230f)) }
            at(50f) { it.spawn(Balloon(400f, ItemType.FOCUS)) }
            line(51f, 16, 0.15f) { w, i -> w.spawn(Bee(80f + (i % 8) * 120f, (i / 8) * 0.5f)) }
            line(56f, 4, 0.6f) { w, i -> w.spawn(Puppy(150f + i * 230f, 260f)) }
            at(60f) { it.spawn(Balloon(600f, ItemType.SPECIAL)) }
            line(62f, 5, 0.6f) { w, i -> w.spawn(BearCub(100f + i * 200f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val all = listOf(
        StageEntry(1, "老鼠", "起司倉庫", "🐭", mouse),
        StageEntry(2, "狗狗", "汪汪公園", "🐶", dog),
        StageEntry(3, "熊熊", "蜂蜜森林", "🐻", bear),
        StageEntry(4, "兔子", "胡蘿蔔田", "🐰", null),
        StageEntry(5, "水豚", "溫泉河畔", "🦫", null),
        StageEntry(6, "企鵝", "冰雪南極", "🐧", null),
        StageEntry(7, "無尾熊", "尤加利樹林", "🐨", null),
        StageEntry(8, "水獺", "清澈溪流", "🦦", null),
        StageEntry(9, "熊貓", "翠綠竹林", "🐼", null),
        StageEntry(10, "樹懶", "雨林樹冠", "🦥", null),
        StageEntry(11, "浣熊", "月夜小鎮", "🦝", null),
        StageEntry(12, "猴子", "叢林遺跡", "🐵", null),
        StageEntry(13, "豬豬", "泥巴農場", "🐷", null),
    )

    fun get(number: Int): StageDef? = all.getOrNull(number - 1)?.stage

    val playableCount: Int get() = all.count { it.stage != null }
}
