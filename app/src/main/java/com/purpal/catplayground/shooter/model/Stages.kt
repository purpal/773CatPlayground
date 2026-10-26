package com.purpal.catplayground.shooter.model

enum class StageTheme { CHEESE_BARN, PARK, HONEY_FOREST, ONSEN, CARROT_FIELD, ICE_FIELD, EUCALYPTUS, STREAM, BAMBOO, RAINFOREST, NIGHT_TOWN, RUINS, MUD_FARM }

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
    /** 世界巡迴裡這一關所在的國家。 */
    val country: Country,
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
        title = "阿爾卑斯起司城堡",
        theme = StageTheme.CHEESE_BARN,
        country = Countries.SWITZERLAND,
        bossName = "萌王 鼠鼠",
        bossArt = "boss_mouse",
        createBoss = ::MouseBoss,
        outro = listOf(
            boss("吱…萌王的紙皇冠…（雙手遞過來）你的肉球是全世界最可愛的…這個位子給你坐…"),
            cat("繞了地球一大圈…我、我是新的萌王了喵？！謝謝大家陪我一路打過來！"),
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
        title = "櫻花公園",
        theme = StageTheme.PARK,
        country = Countries.JAPAN,
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
        title = "楓葉蜂蜜森林",
        theme = StageTheme.HONEY_FOREST,
        country = Countries.CANADA,
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
        title = "亞馬遜溫泉",
        theme = StageTheme.ONSEN,
        country = Countries.BRAZIL,
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
        title = "英式菜園",
        theme = StageTheme.CARROT_FIELD,
        country = Countries.UK,
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

    val penguin = StageDef(
        number = 6,
        animal = "企鵝",
        title = "冰雪南極",
        theme = StageTheme.ICE_FIELD,
        country = Countries.ANTARCTICA,
        bossName = "企鵝艦長 冰冰",
        bossArt = "boss_penguin",
        createBoss = ::PenguinBoss,
        outro = listOf(
            boss("嗚…艦長的帽子飛走了…（吸鼻子）你、你的肚子滑起來一定也很快吧…"),
            cat("我是用跑的喵！南極好冷，下一站要找暖暖的地方！"),
        ),
        script = script {
            line(1.5f, 10, 0.12f) { w, i -> w.spawn(PenguinSlider(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(SnowmanTurret(220f + i * 280f)) }
            line(9f, 6, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 0, startY = 240f + (i / 2) * 40f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(PenguinSlider(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(YuzuDrone(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(SnowmanTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteSeal(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(Duckling(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(PenguinSlider(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(SnowmanTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteSeal(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(BallTurret(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(PenguinSlider(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteHare(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val koala = StageDef(
        number = 7,
        animal = "無尾熊",
        title = "尤加利樹林",
        theme = StageTheme.EUCALYPTUS,
        country = Countries.AUSTRALIA,
        bossName = "無尾熊長老 尤加",
        bossArt = "boss_koala",
        createBoss = ::KoalaBoss,
        outro = listOf(
            boss("呼啊…（揉眼睛）吵醒老人家睡午覺…算了，你很可愛，原諒你…Zzz…"),
            cat("長老晚安喵～我也有點想睡了…不行，再撐一下！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(LeafStar(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(OwlTurret(220f + i * 280f)) }
            line(9f, 8, 0.25f) { w, i -> w.spawn(Bee(150f + i * 100f, i * 0.1f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(LeafStar(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(BearCub(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(OwlTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteKangaroo(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(Bunny(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(LeafStar(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(OwlTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 1, startY = 200f + (i / 2) * 40f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteKangaroo(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(HoneyPot(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(LeafStar(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteBee(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val otter = StageDef(
        number = 8,
        animal = "水獺",
        title = "新加坡河畔",
        theme = StageTheme.STREAM,
        country = Countries.SINGAPORE,
        bossName = "水獺姊妹 啾啾＆嚕嚕",
        bossArt = "boss_otter",
        createBoss = ::OtterBoss,
        outro = listOf(
            boss("啾啾：嗚嗚…我們的貝殼髮夾…　嚕嚕：姊姊不哭，我們一起牽手手…"),
            cat("（也想牽手…）咳，萌王是我的喵！下一站，出發！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(LeapFish(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(ClamTurret(220f + i * 280f)) }
            line(9f, 10, 0.15f) { w, i -> w.spawn(Duckling(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(LeapFish(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(BucketTurret(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(ClamTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteBeaver(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(PenguinSlider(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(LeapFish(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(ClamTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteBeaver(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(BabyCapy(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(LeapFish(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteMonkey(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val panda = StageDef(
        number = 9,
        animal = "熊貓",
        title = "翠綠竹林",
        theme = StageTheme.BAMBOO,
        country = Countries.CHINA,
        bossName = "熊貓師父 波波",
        bossArt = "boss_panda",
        createBoss = ::PandaBoss,
        outro = listOf(
            boss("嗚哇…鬍子是黏上去的啦…師父其實才三歲…（吸鼻子）竹子分你一根…"),
            cat("難怪黑眼圈這麼重喵，原來是熬夜在練裝大人！下一站，出發！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(PandaCub(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(SteamerTurret(220f + i * 280f)) }
            line(9f, 10, 0.14f) { w, i -> w.spawn(LeafStar(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(PandaCub(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(BearCub(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(SteamerTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteRedPanda(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(Bunny(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(PandaCub(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(SteamerTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 1, startY = 200f + (i / 2) * 40f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteRedPanda(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(OwlTurret(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(PandaCub(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteKangaroo(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val sloth = StageDef(
        number = 10,
        animal = "樹懶",
        title = "雨林樹冠",
        theme = StageTheme.RAINFOREST,
        country = Countries.COSTA_RICA,
        bossName = "樹懶郵差 閃電",
        bossArt = "boss_sloth",
        createBoss = ::SlothBoss,
        outro = listOf(
            boss("嗚…信、信件送不到了…（慢慢地）你…好…快…喵…"),
            cat("你剛剛明明也超快的喵！快點去送信啦～下一站！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(TreeFrog(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(PitcherTurret(220f + i * 280f)) }
            line(9f, 10, 0.14f) { w, i -> w.spawn(LeafStar(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(TreeFrog(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(OwlTurret(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(PitcherTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteToucan(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(LeafStar(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(TreeFrog(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(PitcherTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(Bee(150f + i * 100f, i * 0.1f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteToucan(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(OwlTurret(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(TreeFrog(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteMonkey(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val raccoon = StageDef(
        number = 11,
        animal = "浣熊",
        title = "紐約月夜屋頂",
        theme = StageTheme.NIGHT_TOWN,
        country = Countries.USA,
        bossName = "月夜怪盜 基德",
        bossArt = "boss_raccoon",
        createBoss = ::RaccoonBoss,
        outro = listOf(
            boss("嗚嗚…怪盜的帽子飛走了…偷來的東西都還你啦…"),
            cat("偷東西是不對的喵！不過那頂帽子還滿帥的…下一站！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(Opossum(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(TrashCanTurret(220f + i * 280f)) }
            line(9f, 10, 0.14f) { w, i -> w.spawn(Bunny(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(Opossum(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(BallTurret(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(TrashCanTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteSkunk(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(Bunny(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(Opossum(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(TrashCanTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(MousePlane(fromLeft = i % 2 == 1, startY = 200f + (i / 2) * 40f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteSkunk(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(BallTurret(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(Opossum(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteMouse(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val monkey = StageDef(
        number = 12,
        animal = "猴子",
        title = "吳哥叢林遺跡",
        theme = StageTheme.RUINS,
        country = Countries.CAMBODIA,
        bossName = "遺跡猴王 吱吱",
        bossArt = "boss_monkey",
        createBoss = ::MonkeyBoss,
        outro = listOf(
            boss("吱…學不來…你的肉球太可愛了，學不來啦…香蕉分你一根…"),
            cat("學我是沒用的喵！下一站，出發！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(SwingMonkey(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(TotemTurret(220f + i * 280f)) }
            line(9f, 10, 0.14f) { w, i -> w.spawn(TreeFrog(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(SwingMonkey(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(BearCub(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(TotemTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteGorilla(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(TreeFrog(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(SwingMonkey(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(TotemTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteGorilla(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(BearCub(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(SwingMonkey(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteToucan(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val pig = StageDef(
        number = 13,
        animal = "豬豬",
        title = "薰衣草農場",
        theme = StageTheme.MUD_FARM,
        country = Countries.FRANCE,
        bossName = "松露獵人 佩奇",
        bossArt = "boss_pig",
        createBoss = ::PigBoss,
        outro = listOf(
            boss("嗚哇…貝雷帽被打飛了…（吸鼻子）剛挖到的松露分你一顆…"),
            cat("香香的！不過我還是比較想要小魚乾喵～下一站，出發！"),
        ),
        script = script {
            line(1.5f, 10, 0.14f) { w, i -> w.spawn(Chick(140f + (i % 5) * 180f, (i / 5) * 0.3f)) }
            at(5f) { it.spawn(Balloon(500f, ItemType.SPREAD)) }
            line(6f, 3, 1.1f) { w, i -> w.spawn(CowTurret(220f + i * 280f)) }
            line(9f, 10, 0.14f) { w, i -> w.spawn(Duckling(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(12f) { it.spawn(Balloon(300f, ItemType.HOMING)) }
            line(13f, 12, 0.12f) { w, i -> w.spawn(Chick(860f - (i % 6) * 140f, (i / 6) * 0.3f)) }
            line(17f, 3, 1f) { w, i -> w.spawn(CarrotTurret(200f + i * 300f)) }
            at(21f) { it.spawn(Balloon(650f, ItemType.SPECIAL)) }
            line(22f, 4, 0.8f) { w, i -> w.spawn(CowTurret(150f + i * 230f)) }
            at(26f) { it.spawn(EliteWolf(500f)) }
            line(30f, 10, 0.15f) { w, i -> w.spawn(Duckling(150f + (i % 5) * 175f, (i / 5) * 0.3f)) }
            at(33f) { it.spawn(Balloon(500f, ItemType.REPAIR)) }
            line(35f, 14, 0.1f) { w, i -> w.spawn(Chick(100f + (i % 7) * 133f, (i / 7) * 0.3f)) }
            at(39f) { it.spawn(Balloon(350f, ItemType.SONIC)) }
            line(40f, 4, 0.7f) { w, i -> w.spawn(CowTurret(120f + i * 250f)) }
            line(44f, 8, 0.25f) { w, i -> w.spawn(Frisbee(fromLeft = i % 2 == 1, startY = 200f + (i % 4) * 50f)) }
            at(49f) { it.spawn(Balloon(600f, ItemType.FOCUS)) }
            at(51f) { it.spawn(EliteWolf(300f)) }
            line(55f, 3, 1f) { w, i -> w.spawn(CarrotTurret(250f + i * 250f)) }
            line(58f, 16, 0.1f) { w, i -> w.spawn(Chick(80f + (i % 8) * 120f, (i / 8) * 0.3f)) }
            at(62f) { it.spawn(EliteHare(700f)) }
            at(67f) { it.spawn(Balloon(400f, ItemType.BOMB)); it.spawn(Balloon(600f, ItemType.REPAIR)) }
            at(72f) { }
        },
    )

    val all = listOf(
        StageEntry(1, "老鼠", "阿爾卑斯起司城堡", "🐭", mouse),
        StageEntry(2, "狗狗", "櫻花公園", "🐶", dog),
        StageEntry(3, "熊熊", "楓葉蜂蜜森林", "🐻", bear),
        StageEntry(4, "卡皮巴拉", "亞馬遜溫泉", "♨️", capybara),
        StageEntry(5, "兔子", "英式菜園", "🐰", rabbit),
        StageEntry(6, "企鵝", "冰雪南極", "🐧", penguin),
        StageEntry(7, "無尾熊", "尤加利樹林", "🐨", koala),
        StageEntry(8, "水獺", "新加坡河畔", "🦦", otter),
        StageEntry(9, "熊貓", "翠綠竹林", "🐼", panda),
        StageEntry(10, "樹懶", "雨林樹冠", "🦥", sloth),
        StageEntry(11, "浣熊", "紐約月夜屋頂", "🦝", raccoon),
        StageEntry(12, "猴子", "吳哥叢林遺跡", "🐵", monkey),
        StageEntry(13, "豬豬", "薰衣草農場", "🐷", pig),
    )

    fun get(number: Int): StageDef? = all.getOrNull(number - 1)?.stage

    val playableCount: Int get() = all.count { it.stage != null }
}
