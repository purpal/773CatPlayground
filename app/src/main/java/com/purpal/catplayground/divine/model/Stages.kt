package com.purpal.catplayground.divine.model

/** 第幾秒在哪條車道出一隻敵人；lane 為 -1 表示隨機。 */
class Spawn(val at: Float, val kind: EnemyKind, val lane: Int)

class StageDef(
    val number: Int,
    val title: String,
    val intro: String,
    /** 敵人血量倍率。 */
    val hpScale: Float,
    val spawns: List<Spawn>,
    val boss: Boolean = false,
) {
    /** 第一次過關的金幣；重玩只有 [REPLAY_COINS]。 */
    val firstClearCoins: Int get() = 80 + number * 20
}

/** 寫波次用的小工具：時間軸往後推，沿路放敵人。 */
class WaveBuilder {
    private val out = ArrayList<Spawn>()
    private var t = 0f

    fun wait(seconds: Float) {
        t += seconds
    }

    /** 同一時間在指定車道各出一隻；不給車道就隨機一條。 */
    fun spawn(kind: EnemyKind, vararg lanes: Int) {
        if (lanes.isEmpty()) out += Spawn(t, kind, -1) else for (l in lanes) out += Spawn(t, kind, l)
    }

    /** 連續出 [count] 隻，每隻間隔 [gap] 秒；時間軸跟著往後。 */
    fun stream(kind: EnemyKind, count: Int, gap: Float, lane: Int = -1) {
        repeat(count) {
            out += Spawn(t, kind, lane)
            t += gap
        }
    }

    fun build(): List<Spawn> = out.sortedBy { it.at }
}

private fun waves(block: WaveBuilder.() -> Unit): List<Spawn> = WaveBuilder().apply(block).build()

/** 第一章「幽冥鬼域」：5 關加 Boss 九尾妖狐。 */
object DivineStages {
    const val REPLAY_COINS = 25

    private val gate = StageDef(
        1, "鬼門關", "鬼門大開，小鬼成群結隊溜了出來。把神靈拖到車道上擋住牠們！", 1f,
        waves {
            wait(4f); spawn(EnemyKind.IMP, 2)
            wait(7f); spawn(EnemyKind.IMP, 1, 3)
            wait(8f); stream(EnemyKind.IMP, 4, 3f)
            wait(5f); spawn(EnemyKind.IMP, 0, 2, 4)
            wait(9f); stream(EnemyKind.IMP, 6, 2.2f)
            wait(5f); spawn(EnemyKind.ZOMBIE, 2); spawn(EnemyKind.IMP, 1, 3)
            wait(9f); spawn(EnemyKind.IMP, 0, 1, 2, 3, 4)
        },
    )

    private val graves = StageDef(
        2, "亂葬崗", "殭屍從墳裡跳出來了。牠們走得慢，但是皮很厚。", 1f,
        waves {
            wait(4f); spawn(EnemyKind.IMP, 1, 3)
            wait(7f); spawn(EnemyKind.ZOMBIE, 2)
            wait(7f); stream(EnemyKind.IMP, 5, 2.4f)
            wait(4f); spawn(EnemyKind.ZOMBIE, 0, 4)
            wait(10f); stream(EnemyKind.IMP, 4, 1.8f); spawn(EnemyKind.ZOMBIE, 1, 3)
            wait(10f); spawn(EnemyKind.IMP, 0, 1, 2, 3, 4)
            wait(7f); spawn(EnemyKind.ZOMBIE, 0, 2, 4); stream(EnemyKind.IMP, 5, 1.6f)
            wait(8f); spawn(EnemyKind.ZOMBIE, 1, 2, 3)
        },
    )

    private val foxwood = StageDef(
        3, "妖狐林", "狐妖跑得飛快，蝙蝠精會從頭上飛過去——只有遠程的神靈打得到。", 1.1f,
        waves {
            wait(4f); spawn(EnemyKind.FOX, 2)
            wait(6f); spawn(EnemyKind.FOX, 0, 4)
            wait(6f); spawn(EnemyKind.BAT, 1, 3)
            wait(6f); stream(EnemyKind.IMP, 5, 2f); spawn(EnemyKind.FOX, 2)
            wait(5f); stream(EnemyKind.BAT, 4, 2.5f)
            wait(4f); spawn(EnemyKind.ZOMBIE, 1, 3); spawn(EnemyKind.FOX, 0, 2, 4)
            wait(9f); stream(EnemyKind.FOX, 6, 1.5f)
            wait(5f); spawn(EnemyKind.BAT, 0, 1, 2, 3, 4)
            wait(8f); spawn(EnemyKind.ZOMBIE, 0, 2, 4); stream(EnemyKind.FOX, 5, 1.4f); spawn(EnemyKind.BAT, 1, 3)
        },
    )

    private val boneridge = StageDef(
        4, "白骨嶺", "骷髏弓手躲在遠處放箭，前排要夠硬，或是用法術直接清掉。", 1.2f,
        waves {
            wait(4f); spawn(EnemyKind.IMP, 1, 3)
            wait(6f); spawn(EnemyKind.SKELETON, 2)
            wait(7f); spawn(EnemyKind.SKELETON, 0, 4); stream(EnemyKind.IMP, 4, 2f)
            wait(5f); spawn(EnemyKind.ZOMBIE, 1, 3); spawn(EnemyKind.BAT, 2)
            wait(9f); stream(EnemyKind.SKELETON, 5, 2.2f)
            wait(4f); stream(EnemyKind.FOX, 5, 1.5f); spawn(EnemyKind.ZOMBIE, 2)
            wait(8f); spawn(EnemyKind.SKELETON, 0, 1, 2, 3, 4)
            wait(7f); spawn(EnemyKind.ZOMBIE, 0, 2, 4); stream(EnemyKind.BAT, 5, 1.8f)
            wait(6f); spawn(EnemyKind.SKELETON, 1, 3); stream(EnemyKind.IMP, 8, 1.2f)
        },
    )

    private val palace = StageDef(
        5, "閻羅殿前", "牛頭一斧頭掃過整條車道。別把神靈全排在同一條路上！", 1.3f,
        waves {
            wait(4f); stream(EnemyKind.IMP, 4, 2f)
            wait(4f); spawn(EnemyKind.ZOMBIE, 1, 3)
            wait(8f); spawn(EnemyKind.OXHEAD, 2)
            wait(6f); stream(EnemyKind.FOX, 5, 1.6f); spawn(EnemyKind.SKELETON, 0, 4)
            wait(8f); spawn(EnemyKind.BAT, 0, 2, 4); spawn(EnemyKind.ZOMBIE, 1, 3)
            wait(9f); spawn(EnemyKind.OXHEAD, 0, 4); stream(EnemyKind.IMP, 6, 1.5f)
            wait(10f); spawn(EnemyKind.SKELETON, 1, 2, 3); stream(EnemyKind.FOX, 6, 1.3f)
            wait(8f); spawn(EnemyKind.OXHEAD, 1, 3); spawn(EnemyKind.ZOMBIE, 0, 2, 4); stream(EnemyKind.BAT, 5, 1.6f)
        },
    )

    private val ninetail = StageDef(
        6, "九尾妖狐", "幽冥鬼域的主人現身了。牠會放狐火燒整條車道，快被打敗時還會化成妖霧換邊。", 1.3f,
        waves {
            wait(4f); stream(EnemyKind.IMP, 4, 2f)
            wait(4f); spawn(EnemyKind.FOX, 0, 4)
            wait(8f); spawn(EnemyKind.NINETAIL, 2)
            wait(12f); spawn(EnemyKind.ZOMBIE, 0, 4)
            wait(14f); spawn(EnemyKind.SKELETON, 0, 4); spawn(EnemyKind.BAT, 2)
            wait(14f); spawn(EnemyKind.ZOMBIE, 0, 4); stream(EnemyKind.IMP, 4, 1.6f)
            wait(14f); spawn(EnemyKind.BAT, 0, 2, 4); spawn(EnemyKind.SKELETON, 1, 3)
            wait(14f); spawn(EnemyKind.OXHEAD, 0); stream(EnemyKind.FOX, 4, 1.5f)
        },
        boss = true,
    )

    val all: List<StageDef> = listOf(gate, graves, foxwood, boneridge, palace, ninetail)

    fun get(number: Int): StageDef? = all.firstOrNull { it.number == number }
}
