package com.purpal.catplayground.nightwatch

import kotlin.math.*
import kotlin.random.Random

data class Point(val x: Float, val y: Float) {
    fun distance(p: Point) = hypot(x - p.x, y - p.y)
}

enum class God(val title: String, val role: String, val cost: Int, val range: Float, val damage: Float, val interval: Float, val paths: List<String>) {
    ZEUS("宙斯", "雷擊連鎖・潮濕增幅", 85, 265f, 24f, 1.05f, listOf("雷王", "雷雲牧者")),
    POSEIDON("波塞頓", "積水減速・浪潮擊退", 75, 235f, 10f, 1.5f, listOf("深海之怒", "永恆潮汐")),
    NEZHA("哪吒", "火帶灼燒・範圍清場", 75, 240f, 15f, .9f, listOf("三昧真火", "風火輪")),
    ATHENA("雅典娜", "盾陣攔截・破甲", 65, 180f, 22f, .85f, listOf("戰爭女神", "不破壁壘")),
    WIND("風神", "聚怪擊退・助燃", 65, 255f, 9f, 1.25f, listOf("暴風眼", "焚風")),
    EARTH("土地公", "土坡緩速・供品生產", 60, 210f, 12f, 1.4f, listOf("金山", "山神"));
    val branchHelp get() = when(this) {
        ZEUS -> listOf("直接雷擊傷害提高 65%，擅長打頭目。", "連鎖額外跳躍 2 次，擅長清除群怪。")
        POSEIDON -> listOf("每次浪潮擊退更遠。", "積水維持更久，浪潮範圍更廣。")
        NEZHA -> listOf("灼燒延長至 7 秒。", "攻速提高 45%，火輪範圍擴大。")
        ATHENA -> listOf("盾擊追加傷害並讓頭目破防。", "同時攔截最多 5 隻敵人。")
        WIND -> listOf("大幅提高擊退距離。", "無需附近火源即可引燃敵軍。")
        EARTH -> listOf("供品收入隨等級提高。", "土坡範圍擴大，區域內敵人速度減半。")
    }
    val art get() = "nw_${name.lowercase()}"
}

data class NightStage(val id: Int, val name: String, val subtitle: String, val boss: String, val mechanism: String, val description: String) {
    val paths: List<List<Point>> = when (id) {
        0 -> listOf(listOf(Point(-40f,160f), Point(320f,160f), Point(320f,470f), Point(750f,470f), Point(750f,820f), Point(500f,820f), Point(500f,1130f)),
            listOf(Point(1040f,300f), Point(560f,300f), Point(560f,680f), Point(230f,680f), Point(230f,980f), Point(500f,980f), Point(500f,1130f)))
        1 -> listOf(listOf(Point(-40f,180f),Point(740f,180f),Point(740f,570f),Point(260f,570f),Point(260f,920f),Point(500f,920f),Point(500f,1130f)),
            listOf(Point(1040f,350f),Point(440f,350f),Point(440f,760f),Point(800f,760f),Point(800f,980f),Point(500f,980f),Point(500f,1130f)))
        else -> listOf(listOf(Point(-40f,200f),Point(320f,200f),Point(320f,760f),Point(750f,760f),Point(750f,980f),Point(500f,980f),Point(500f,1130f)),
            listOf(Point(1040f,200f),Point(680f,200f),Point(680f,480f),Point(220f,480f),Point(220f,980f),Point(500f,980f),Point(500f,1130f)))
    }
    val seats: List<Point> = when(id) {
        0 -> listOf(Point(200f,300f),Point(440f,150f),Point(760f,310f),Point(430f,540f),Point(850f,640f),Point(360f,800f),Point(650f,960f),Point(120f,850f))
        1 -> listOf(Point(200f,320f),Point(580f,240f),Point(850f,450f),Point(340f,450f),Point(600f,640f),Point(140f,750f),Point(640f,880f),Point(360f,1030f))
        else -> listOf(Point(200f,330f),Point(500f,230f),Point(810f,350f),Point(440f,600f),Point(790f,610f),Point(160f,810f),Point(600f,870f),Point(360f,1060f))
    }
    companion object {
        val all = listOf(
            NightStage(0,"雨夜魚市場","雷雨交響曲", "百口饕餮", "水閘・銅鐘", "水閘開啟 8 秒：全路潮濕減速，水妖卻會加速。銅鐘震暈敵軍，讓饕餮張嘴承受雙倍傷害。"),
            NightStage(1,"風鈴竹林","風火大迴旋", "吞夢天狗", "風鈴大陣", "敲響風鈴擊退可移動敵人，吹散天狗護罩。風火搭配能把灼燒擴散到附近敵人。"),
            NightStage(2,"月下古神殿","最後的罐罐", "忘卻龍王", "長明火盆", "點燃火盆灼燒全路、驅散龍王護罩。用水能控場，卻會沖掉敵人身上的火焰。")
        )
    }
}

enum class Foe(val title: String, val health: Float, val speed: Float, val art: String) {
    RAT("餓鬼鼠",55f,65f,"dv_foe_imp"), UMBRELLA("紙傘妖",95f,51f,"dv_foe_skeleton"),
    LION("石獅精",220f,36f,"dv_foe_oxhead"), THIEF("偷供品狸",65f,103f,"dv_foe_fox"),
    CLOCK("報時鬼",110f,48f,"dv_foe_zombie"), WATER("水妖",95f,58f,"dv_foe_bat_0"), BOSS("忘卻之主",2600f,23f,"dv_foe_ninetail")
}

class Invader(val id: Int, val kind: Foe, val route: Int, val maxHp: Float) {
    var hp = maxHp; var distance = 0f; var wet = 0f; var burn = 0f; var stun = 0f; var exposed = 0f
    var p = Point(0f,0f); var hit = 0f
}
class Seat(val god: God, var index: Int) {
    var level = 1; var branch = -1; var cooldown = .3f; var attack = 0f; var sealed = 0f
    val upgradeCost get() = if(level==1) 100 else 150
}

data class Blessing(val id: String, val title: String, val text: String)
val blessings = listOf(
    Blessing("chain","天雷引線","宙斯連鎖額外跳躍 2 次"), Blessing("storm","暴雨將至","潮濕持續時間 +3 秒"),
    Blessing("fire","不滅心火","灼燒傷害提高 60%"), Blessing("wind","順風而行","擊退距離提高 60%"),
    Blessing("range","神目如炬","全體攻擊範圍 +15%"), Blessing("haste","貓爪疾風","全體攻擊速度 +18%"),
    Blessing("power","神威降臨","全體直接傷害 +20%"), Blessing("gold","罐罐豐收","每波額外獲得 35 供品"),
    Blessing("heal","神殿修繕","立即回復 5 點神殿耐久"), Blessing("move","搬家專家","本局換座免費"),
    Blessing("ultimate","九命神諭","神諭冷卻縮短 10 秒"), Blessing("shield","守護誓言","雅典娜可多攔截 2 隻敵人")
)
enum class NightPhase { PREPARE, FIGHT, BLESSING, WON, LOST }
data class NightFx(val from: Point, val to: Point, val god: God?, val kind: String = "hit", var life: Float = .5f, val total: Float = .5f)

/** Deterministic simulation; UI and effects never determine damage or progress. */
class NightBattle(val stage: NightStage, val team: List<God>, seed: Int = stage.id) {
    private val random = Random(seed)
    var phase = NightPhase.PREPARE; private set
    var wave = 0; private set
    val totalWaves = 10
    var hp = 20; private set
    var offerings = 310; private set
    var kills = 0; private set
    var time = 0f; private set
    var ultimate = 0f; private set
    var mechanism = 0f; private set
    var flood = 0f; private set
    val seats = mutableListOf<Seat>()
    val enemies = mutableListOf<Invader>()
    val effects = mutableListOf<NightFx>()
    val boons = mutableSetOf<String>()
    var choices = emptyList<Blessing>(); private set
    var notice = "選擇神貓，再點空神座召喚。準備好後開始守夜。"; private set
    private var remaining = 0
    private var spawnClock = 0f
    private var sequence = 0
    private var earthClock = 0f
    private var bossClock = 12f
    private var bossEscaped = false
    val moveCost get() = if("move" in boons) 0 else 15
    val stars get() = if(phase!=NightPhase.WON) 0 else if(hp>=17) 3 else if(hp>=10) 2 else 1
    val living get() = enemies.filter { it.hp > 0 }
    fun radius(s: Seat) = (s.god.range + if(s.god==God.EARTH && s.branch==1) 65f else 0f)*(if("range" in boons) 1.15f else 1f)
    private fun interactive() = phase==NightPhase.PREPARE || phase==NightPhase.FIGHT
    fun place(god: God, index: Int): Boolean {
        if(!interactive() || god !in team || index !in stage.seats.indices || seats.any { it.index==index || it.god==god } || offerings<god.cost) return false
        offerings -= god.cost; seats += Seat(god,index)
        effects += NightFx(stage.seats[index],stage.seats[index],god,"summon",.8f,.8f)
        notice="${god.title}就位！點選神座可升級或換座。"; return true
    }
    fun move(from: Int, to: Int): Boolean {
        val seat=seats.find { it.index==from } ?: return false
        if(!interactive() || to !in stage.seats.indices || seats.any { it.index==to } || offerings<moveCost) return false
        offerings-=moveCost; val start=stage.seats[from]; seat.index=to
        effects+=NightFx(start,stage.seats[to],seat.god,"move",.7f,.7f)
        notice="${seat.god.title}抱著神座搬家了！"; return true
    }
    fun upgrade(index: Int, branch: Int): Boolean {
        val seat=seats.find { it.index==index } ?: return false
        if(!interactive() || seat.level>=3 || branch !in 0..1 || offerings<seat.upgradeCost) return false
        offerings-=seat.upgradeCost
        if(seat.level==1) seat.branch=branch
        seat.level++; effects+=NightFx(stage.seats[index],stage.seats[index],seat.god,"summon",.8f,.8f)
        notice="${seat.god.title}・${seat.god.paths[seat.branch]} Lv.${seat.level}"; return true
    }
    fun startWave(): Boolean {
        if(phase!=NightPhase.PREPARE || seats.isEmpty()) return false
        wave++; remaining=8+wave*2; spawnClock=0f; phase=NightPhase.FIGHT
        notice=if(wave==totalWaves) "${stage.boss}來襲！用${stage.mechanism}破除護罩。" else "第 $wave 夜・${if(wave>=4) "留意快速的偷供品狸與報時鬼" else "守住最後的罐罐"}"
        return true
    }
    fun choose(id: String): Boolean {
        if(phase!=NightPhase.BLESSING || choices.none { it.id==id }) return false
        boons+=id; if(id=="heal") hp=min(20,hp+5)
        choices=emptyList(); phase=NightPhase.PREPARE; notice="祝福已生效。重新布防，迎接下一夜。"; return true
    }
    fun activateMechanism(): Boolean {
        if(phase!=NightPhase.FIGHT || mechanism>0f) return false
        mechanism=27f
        enemies.filter { it.hp>0 }.forEach {
            it.exposed=5f
            when(stage.id) {
                0 -> { it.stun=2.5f; it.wet=8f }
                1 -> if(it.kind!=Foe.LION && it.kind!=Foe.BOSS) it.distance=max(0f,it.distance-150f)
                else -> it.burn=8f
            }
        }
        if(stage.id==0) flood=8f
        effects+=NightFx(Point(500f,600f),Point(500f,600f),God.entries[listOf(1,4,2)[stage.id]],"nova",1.2f,1.2f)
        notice="${stage.mechanism}啟動！頭目破防 5 秒。"; return true
    }
    fun invoke(god: God): Boolean {
        val seat=seats.find { it.god==god } ?: return false
        if(phase!=NightPhase.FIGHT || ultimate>0) return false
        ultimate=if("ultimate" in boons) 22f else 32f
        living.forEach { e ->
            hit(e,95f+seat.level*30f,god)
            when(god) {
                God.POSEIDON -> {e.wet=9f; push(e,100f)}
                God.NEZHA -> e.burn=8f
                God.WIND -> push(e,180f)
                God.ATHENA -> e.stun=3.5f
                God.EARTH -> e.stun=2f
                else -> e.stun=1f
            }
        }
        if(god==God.EARTH) offerings+=35
        effects+=NightFx(stage.seats[seat.index],Point(500f,600f),god,"nova",1f,1f)
        notice="${god.title}神諭！"; return true
    }
    private fun push(e: Invader, amount: Float) {
        if(e.kind!=Foe.LION && e.kind!=Foe.BOSS) e.distance=max(0f,e.distance-amount*(if("wind" in boons) 1.6f else 1f))
    }
    private fun hit(e: Invader, amount: Float, god: God) {
        val armor=when {
            e.kind==Foe.BOSS && e.exposed<=0 && stage.id>0 -> .65f
            e.kind==Foe.UMBRELLA && god!=God.ZEUS && god!=God.ATHENA -> .55f
            else -> 1f
        }
        e.hp-=amount*armor*(if(e.kind==Foe.BOSS && e.exposed>0) 2f else 1f)
        e.hit=.16f
    }
    fun tick(dt: Float) {
        if(dt<=0) return
        effects.forEach { it.life-=dt }; effects.removeAll { it.life<=0 }
        if(phase!=NightPhase.FIGHT) return
        time+=dt; ultimate=max(0f,ultimate-dt); mechanism=max(0f,mechanism-dt); flood=max(0f,flood-dt)
        spawnClock-=dt
        if(remaining>0 && spawnClock<=0) {
            val kind=if(wave==totalWaves && remaining==1) Foe.BOSS else {
                val pool=Foe.entries.take(min(6,2+(wave+stage.id)/2))
                pool[random.nextInt(pool.size)]
            }
            val scale=1f+(wave-1)*.11f+stage.id*.2f
            val e=Invader(++sequence,kind,sequence%2,kind.health*scale)
            e.p=position(stage.paths[e.route],0f); enemies+=e
            remaining--; spawnClock=if(wave==totalWaves) 1.7f else 2f
        }
        bossClock-=dt
        if(bossClock<=0 && living.any { it.kind==Foe.BOSS }) {
            bossClock=14f
            offerings=max(0,offerings-20)
            living.filter {it.kind==Foe.BOSS && it.exposed<=0}.forEach {boss ->
                seats.filter {stage.seats[it.index].distance(boss.p)<280f}.forEach {it.sealed=2.5f}
                effects+=NightFx(boss.p,boss.p,null,"enemyBlast",1f,1f)
            }
            notice="${stage.boss}吞掉供品並震懾附近神貓！用機關破防阻止震懾。"
        }
        earthClock+=dt
        if(earthClock>=6f) {
            earthClock=0f
            seats.filter { it.god==God.EARTH }.forEach { offerings+=if(it.branch==0) 12*it.level else 7 }
        }
        seats.forEach {it.sealed=max(0f,it.sealed-dt)}
        for(e in enemies) {
            if(e.hp<=0) continue
            e.wet=max(0f,e.wet-dt); e.burn=max(0f,e.burn-dt); e.stun=max(0f,e.stun-dt); e.exposed=max(0f,e.exposed-dt); e.hit=max(0f,e.hit-dt)
            if(flood>0) e.wet=max(e.wet,1f)
            if(e.burn>0) e.hp-=dt*13f*(if("fire" in boons) 1.6f else 1f)
            val guards=seats.filter { it.god==God.ATHENA && it.sealed<=0 && stage.seats[it.index].distance(e.p)<radius(it) }
            val blocked=guards.any { guard ->
                val cap=(if(guard.branch==1) 5 else 2)+(if("shield" in boons) 2 else 0)
                living.filter { stage.seats[guard.index].distance(it.p)<radius(guard) }.sortedByDescending { it.distance }.take(cap).contains(e)
            }
            val earth=seats.filter { it.god==God.EARTH && stage.seats[it.index].distance(e.p)<radius(it) }
            val soil=if(earth.any {it.branch==1}) .5f else if(earth.isNotEmpty()) .7f else 1f
            val haste=living.any { it.kind==Foe.CLOCK && it.id!=e.id && it.p.distance(e.p)<190f }
            val wetSpeed=if(e.wet>0) {if(e.kind==Foe.WATER) 1.45f else .58f} else 1f
            if(e.stun<=0 && !blocked) e.distance+=e.kind.speed*dt*wetSpeed*soil*(if(haste) 1.3f else 1f)
            val path=stage.paths[e.route]; e.p=position(path,e.distance)
            if(e.distance>=length(path)) {
                hp=max(0,hp-if(e.kind==Foe.BOSS) 8 else 1)
                if(e.kind==Foe.BOSS) bossEscaped=true
                if(e.kind==Foe.THIEF) offerings=max(0,offerings-25)
                e.hp=0f; effects+=NightFx(e.p,e.p,null,"breach",.6f,.6f)
            }
        }
        for(s in seats) {
            s.attack=max(0f,s.attack-dt); s.cooldown-=dt
            if(s.cooldown>0 || s.sealed>0) continue
            val origin=stage.seats[s.index]
            val targets=living.filter { origin.distance(it.p)<=radius(s) }.sortedBy { length(stage.paths[it.route])-it.distance }
            val first=targets.firstOrNull() ?: continue
            s.cooldown=s.god.interval/(if("haste" in boons) 1.18f else 1f)/(if(s.branch==1 && s.god==God.NEZHA) 1.45f else 1f)
            s.attack=.25f
            var damage=s.god.damage*(1f+(s.level-1)*.65f)*(if("power" in boons) 1.2f else 1f)
            if(s.branch==0 && s.god==God.ZEUS) damage*=1.65f
            val victims=when(s.god) {
                God.ZEUS -> listOf(first)+living.filter { it.id!=first.id && it.p.distance(first.p)<220f }.take(1+(if(first.wet>0) 2 else 0)+(if("chain" in boons) 2 else 0)+(if(s.branch==1) 2 else 0))
                God.NEZHA,God.WIND,God.POSEIDON -> targets.filter { it.p.distance(first.p)<if(s.branch==1) 170f else 110f }
                else -> listOf(first)
            }
            var previous=origin
            victims.forEach { e ->
                hit(e,damage,s.god)
                when(s.god) {
                    God.POSEIDON -> {e.wet=(if(s.branch==1) 7f else 4f)+(if("storm" in boons) 3f else 0f); e.burn=0f; push(e,if(s.branch==0) 45f else 20f)}
                    God.NEZHA -> e.burn=if(s.branch==0) 7f else 4f
                    God.WIND -> {push(e,if(s.branch==0) 65f else 35f); if(s.branch==1 || living.any { it.burn>0 && it.p.distance(e.p)<210f }) e.burn=4f}
                    God.ATHENA -> if(s.branch==0) {e.exposed=2f; hit(e,damage*.4f,s.god)}
                    else -> Unit
                }
                effects+=NightFx(previous,e.p,s.god)
                if(s.god==God.ZEUS) previous=e.p
            }
        }
        val dead=enemies.filter { it.hp<=0 }
        dead.forEach { e ->
            // Breaches do not grant kill income.
            if(e.distance<length(stage.paths[e.route])) {kills++; offerings+=if(e.kind==Foe.BOSS) 80 else 6; effects+=NightFx(e.p,e.p,null,"poof",.45f,.45f)}
        }
        enemies.removeAll { it.hp<=0 }
        if(effects.size>180) effects.subList(0,effects.size-180).clear()
        if(hp<=0 || bossEscaped) {phase=NightPhase.LOST; notice=if(bossEscaped) "頭目吞掉了最後的罐罐！下次把機關和神諭留給牠。" else "神殿被忘卻之霧吞沒了。換個陣容再試一次！"}
        else if(remaining==0 && enemies.isEmpty()) {
            if(wave==totalWaves) {phase=NightPhase.WON; notice="守夜成功！神話與晚餐都保住了。"}
            else {
                offerings+=80+(if("gold" in boons) 35 else 0)
                choices=blessings.filter { it.id !in boons }.shuffled(random).take(3)
                phase=NightPhase.BLESSING; notice="黎明將至，選擇一項祝福。"
            }
        }
    }
    companion object {
        fun length(path: List<Point>): Float = path.zipWithNext().sumOf { (a,b)->a.distance(b).toDouble() }.toFloat()
        fun position(path: List<Point>, distance: Float): Point {
            var left=distance.coerceAtLeast(0f)
            for((a,b) in path.zipWithNext()) {
                val size=a.distance(b)
                if(left<=size) {val t=left/size; return Point(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t)}
                left-=size
            }
            return path.last()
        }
    }
}
