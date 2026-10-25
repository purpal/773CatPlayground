package com.purpal.catplayground.shooter.model

enum class StageTheme { CHEESE_BARN, PARK, HONEY_FOREST, ONSEN, CARROT_FIELD }

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
    /** 選關、警告畫面用的 Boss 頭像（assets/art 的名稱，不含心情與影格）。 */
    val bossArt: String,
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
        bossArt = "boss_mouse",
        createBoss = ::MouseBoss,
        outro = listOf(
            boss("吱…你的肉球真的好可愛…起司分你一半…"),
            cat("謝謝招待喵！下一站，出發！"),
        ),
        script = script {
            line(1.5f, 10, 0.12f) { w, i -> w.spawn(MouseRunner(140f + (i % 5) * 180f + (i / 5) * 90f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(7f, 6, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = true, startY = 240f + i * 30f)) }
            line(10f, 10, 0.15f) { w, i -> w.spawn(MouseRunner(860f - (i % 5) * 170f, wiggle = 1.5f)) }
            at(13f) { it.spawn(CheeseDrone(300f, 360f)); it.spawn(CheeseDrone(700f, 300f)) }
            at(16f) { it.spawn(Balloon(260f, ItemType.HOMING)) }
            line(19f, 6, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = false, startY = 220f + i * 30f)) }
            line(22f, 12, 0.12f) { w, i -> w.spawn(MouseRunner(100f + (i % 4) * 260f + (i / 4) * 40f)) }
            at(25f) { it.spawn(Balloon(720f, ItemType.FOCUS)) }
            at(27f) { it.spawn(CheeseDrone(500f, 320f)) }
            line(28f, 6, 0.4f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 0, startY = 280f + (i / 2) * 30f)) }
            at(30f) { it.spawn(EliteMouse(500f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.SPECIAL)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(MouseRunner(80f + (i % 10) * 90f, wiggle = 0.5f)) }
            at(39f) { it.spawn(CheeseDrone(250f, 280f)); it.spawn(CheeseDrone(750f, 280f)) }
            at(43f) { it.spawn(Balloon(300f, ItemType.REPAIR)) }
            line(45f, 8, 0.3f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 0, startY = 200f + (i / 2) * 40f)) }
            at(50f) { it.spawn(Balloon(700f, ItemType.SONIC)) }
            line(52f, 3, 1.2f) { w, i -> w.spawn(CheeseDrone(250f + i * 250f, 260f + i * 40f)) }
            line(57f, 16, 0.12f) { w, i -> w.spawn(MouseRunner(if (i % 2 == 0) 150f + i * 25f else 850f - i * 25f)) }
            at(55f) { it.spawn(EliteMouse(300f, 300f)) }
            at(61f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(63f, 8, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 1, startY = 240f + (i / 2) * 25f)) }
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
        bossArt = "boss_dog",
        createBoss = ::DogBoss,
        outro = listOf(
            boss("嗚汪…我、我只是想被摸摸頭…"),
            cat("摸摸～你也很可愛，但第一名是我喵！"),
        ),
        script = script {
            line(1.5f, 3, 0.8f) { w, i -> w.spawn(Puppy(250f + i * 250f, 300f + (i % 2) * 80f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.FOCUS)) }
            line(6f, 6, 0.3f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 0, startY = 230f + (i / 2) * 60f)) }
            line(10f, 3, 1.2f) { w, i -> w.spawn(BallTurret(200f + i * 300f)) }
            at(14f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(15f, 4, 0.6f) { w, i -> w.spawn(Puppy(150f + i * 230f, 260f)) }
            line(20f, 8, 0.25f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i / 2) * 40f)) }
            at(24f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(25f, 4, 0.9f) { w, i -> w.spawn(BallTurret(150f + i * 230f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(MouseRunner(120f + (i % 5) * 190f)) }
            at(31f) { it.spawn(EliteDog(500f)) }
            at(34f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(36f, 5, 0.7f) { w, i -> w.spawn(Puppy(100f + i * 200f, 240f + (i % 2) * 120f)) }
            at(41f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(43f, 10, 0.2f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 0, startY = 220f + (i % 3) * 70f)) }
            line(48f, 4, 0.6f) { w, i -> w.spawn(BallTurret(120f + i * 250f)) }
            at(52f) { it.spawn(Balloon(600f, ItemType.SPREAD)) }
            line(54f, 6, 0.6f) { w, i -> w.spawn(Puppy(100f + i * 160f, 280f)) }
            at(57f) { it.spawn(EliteMouse(700f)) }
            at(60f) { it.spawn(Balloon(500f, ItemType.FOCUS)) }
            line(61f, 12, 0.18f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
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
        bossArt = "boss_bear",
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
            at(21f) { it.spawn(EliteBee(500f)) }
            at(23f) { it.spawn(Balloon(300f, ItemType.SPECIAL)) }
            line(24f, 4, 0.8f) { w, i -> w.spawn(BearCub(150f + i * 230f)) }
            line(29f, 12, 0.18f) { w, i -> w.spawn(Bee(100f + (i % 6) * 160f, (i / 6) * 0.4f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 3, 0.9f) { w, i -> w.spawn(CheeseDrone(250f + i * 250f, 300f)) }
            at(40f) { it.spawn(Balloon(650f, ItemType.SONIC)) }
            line(41f, 4, 0.7f) { w, i -> w.spawn(BearCub(100f + i * 260f)) }
            line(45f, 4, 1f) { w, i -> w.spawn(HoneyPot(150f + i * 230f)) }
            at(46f) { it.spawn(EliteBee(400f)) }
            at(50f) { it.spawn(Balloon(400f, ItemType.FOCUS)) }
            line(51f, 16, 0.15f) { w, i -> w.spawn(Bee(80f + (i % 8) * 120f, (i / 8) * 0.5f)) }
            line(56f, 4, 0.6f) { w, i -> w.spawn(Puppy(150f + i * 230f, 260f)) }
            at(60f) { it.spawn(Balloon(600f, ItemType.SPECIAL)) }
            line(62f, 5, 0.6f) { w, i -> w.spawn(BearCub(100f + i * 200f)) }
            at(63f) { it.spawn(EliteDog(500f, 300f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val capybara = StageDef(
        number = 4,
        animal = "卡皮巴拉",
        title = "溫泉河畔",
        theme = StageTheme.ONSEN,
        bossName = "溫泉大王 卡皮",
        bossArt = "boss_capy",
        createBoss = ::CapyBoss,
        outro = listOf(
            boss("（淡定地把護目鏡推上去）…嗯，你也一起來泡一下吧。"),
            cat("呼～好舒服…等等，我是來比萌的喵！"),
        ),
        script = script {
            line(1.5f, 10, 0.15f) { w, i -> w.spawn(Duckling(150f + (i % 5) * 175f, (i / 5) * 0.3f + (i % 5) * 0.05f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1f) { w, i -> w.spawn(YuzuDrone(250f + i * 250f, 300f + (i % 2) * 80f)) }
            line(10f, 3, 1.2f) { w, i -> w.spawn(BucketTurret(200f + i * 300f)) }
            at(13f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(14f, 2, 1.5f) { w, i -> w.spawn(BabyCapy(300f + i * 400f)) }
            line(18f, 12, 0.15f) { w, i -> w.spawn(Duckling(850f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            at(22f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(23f, 4, 0.8f) { w, i -> w.spawn(YuzuDrone(150f + i * 230f, 280f)) }
            at(27f) { it.spawn(EliteMonkey(500f)) }
            line(31f, 3, 1f) { w, i -> w.spawn(BucketTurret(150f + i * 350f)) }
            at(34f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(36f, 3, 1.2f) { w, i -> w.spawn(BabyCapy(200f + i * 300f)) }
            at(40f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(41f, 10, 0.2f) { w, i -> w.spawn(Duckling(100f + (i % 5) * 200f, (i / 5) * 0.4f)) }
            line(45f, 4, 0.7f) { w, i -> w.spawn(YuzuDrone(120f + i * 250f, 260f + (i % 2) * 60f)) }
            at(50f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(52f) { it.spawn(EliteMonkey(300f)) }
            line(56f, 4, 0.8f) { w, i -> w.spawn(BucketTurret(120f + i * 250f)) }
            line(60f, 3, 1f) { w, i -> w.spawn(BabyCapy(250f + i * 250f)) }
            at(63f) { it.spawn(EliteBee(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val rabbit = StageDef(
        number = 5,
        animal = "兔子",
        title = "胡蘿蔔田",
        theme = StageTheme.CARROT_FIELD,
        bossName = "兔兔隊長 蘿蔔",
        bossArt = "boss_rabbit",
        createBoss = ::RabbitBoss,
        outro = listOf(
            boss("嗚嗚…兔子急了也會咬人，可是你更兇喵…胡蘿蔔分你一根…"),
            cat("謝謝！不過我比較想要小魚乾喵～下一站！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(Bunny(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(CarrotTurret(220f + i * 280f)) }
            line(9f, 6, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 0, startY = 240f + (i / 2) * 40f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(Bunny(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(Puppy(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(CarrotTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteHare(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(Duckling(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(Bunny(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(CarrotTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteHare(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(BabyCapy(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(Bunny(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteMonkey(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val all = listOf(
        StageEntry(1, "老鼠", "起司倉庫", "🐭", mouse),
        StageEntry(2, "狗狗", "汪汪公園", "🐶", dog),
        StageEntry(3, "熊熊", "蜂蜜森林", "🐻", bear),
        StageEntry(4, "卡皮巴拉", "溫泉河畔", "♨️", capybara),
        StageEntry(5, "兔子", "胡蘿蔔田", "🐰", rabbit),
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
