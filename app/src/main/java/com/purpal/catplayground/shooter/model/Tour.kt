package com.purpal.catplayground.shooter.model

import kotlin.random.Random

/** 世界巡迴裡的國家：地圖上的經緯度與國旗圖（assets/art 的名稱）。 */
class Country(val name: String, val lon: Float, val lat: Float, val flag: String)

object Countries {
    val TAIWAN = Country("台灣", 121f, 23.7f, "flag_tw")
    val JAPAN = Country("日本", 138.5f, 36.5f, "flag_jp")
    val CANADA = Country("加拿大", -106f, 57f, "flag_ca")
    val UK = Country("英國", -2f, 53.5f, "flag_gb")
    val ANTARCTICA = Country("南極洲", 30f, -78f, "flag_aq")
    val AUSTRALIA = Country("澳洲", 134f, -25f, "flag_au")
    val CHINA = Country("中國", 104f, 34f, "flag_cn")
    val USA = Country("美國", -98f, 39f, "flag_us")
    val CAMBODIA = Country("柬埔寨", 104.9f, 12.6f, "flag_kh")
    val FRANCE = Country("法國", 2.3f, 46.6f, "flag_fr")
    val SINGAPORE = Country("新加坡", 103.8f, 1.35f, "flag_sg")
    val COSTA_RICA = Country("哥斯大黎加", -84f, 10f, "flag_cr")
    val BRAZIL = Country("巴西", -51f, -10f, "flag_br")
    val SWITZERLAND = Country("瑞士", 8.2f, 46.8f, "flag_ch")

    /** 巡迴的出發地。 */
    val HOME = TAIWAN
}

/**
 * 依出場順序決定的強度。level 0 是第一站，8 是隨機池的最後一站，9～11 是三大天王，12 是萌王。
 * 不管抽到哪一隻動物，越後面出場就越強：Boss 血量、子彈速度、攻擊節奏、彈幕密度與小怪血量一起往上調。
 * 預告時間（紅色虛線、預告圈）不會縮短，所以再難也還是看得出來、閃得掉。
 */
class Difficulty private constructor(val level: Int, private val neutral: Boolean) {
    /** Boss 的血量（null 表示用 Boss 自己的設定值）。 */
    val bossHp: Float? get() = if (neutral) null else BOSS_HP[level.coerceIn(0, BOSS_HP.size - 1)]
    /** 敵彈速度倍率。 */
    val bulletSpeed: Float get() = if (neutral) 1f else 0.8f + level * 0.035f
    /** 攻擊節奏倍率（Boss 招式與地面砲台的開火頻率）。 */
    val tempo: Float get() = if (neutral) 1f else 0.82f + level * 0.03f
    /** 扇形／環形彈的顆數倍率。 */
    val density: Float get() = if (neutral) 1f else 0.7f + level * 0.028f
    /** 小怪與菁英怪的血量倍率。 */
    val enemyHp: Float get() = if (neutral) 1f else 0.8f + level * 0.05f
    /** 小橘「肉球護甲」一次給的臨時護甲（一般子彈 12～22 傷害）。 */
    val tempArmor: Float get() = TEMP_ARMOR + level * TEMP_ARMOR_PER_BOUT

    companion object {
        /** 不調整（單元測試、除錯用），所有東西都照設定值。 */
        val NEUTRAL = Difficulty(0, neutral = true)

        fun forBout(index: Int) = Difficulty(index.coerceIn(0, Tour.SIZE - 1), neutral = false)

        const val TEMP_ARMOR = 16f
        const val TEMP_ARMOR_PER_BOUT = 1f

        /** 每一站 Boss 的基本血量（再乘上 Boss 各自的 [Boss.hpFactor]）。 */
        private val BOSS_HP = floatArrayOf(
            14000f, 16500f, 19000f, 21500f, 24000f, 26500f, 29000f, 31500f, 34000f,
            38000f, 40000f, 42000f,
            46000f,
        )
    }
}

/**
 * 世界巡迴：從台灣出發，前 9 站從 9 隻動物裡隨機抽，接著是三大天王（水獺 → 樹懶 → 卡皮巴拉），
 * 最後挑戰萌王鼠鼠。
 */
object Tour {
    /** 前 9 站的隨機池（Stage 的 number）。 */
    val POOL = listOf(2, 3, 5, 6, 7, 9, 11, 12, 13)
    /** 三大天王：水獺、樹懶、卡皮巴拉。 */
    val KINGS = listOf(8, 10, 4)
    /** 萌王鼠鼠。 */
    const val CHAMPION = 1
    const val SIZE = 13
    const val RANDOM_BOUTS = 9

    fun start(cat: CatType, random: Random = Random.Default): TourState =
        TourState(
            cat = cat,
            order = POOL.shuffled(random) + KINGS + CHAMPION,
            cleared = 0,
            revealed = 0,
            loadout = null,
            totalScore = 0,
        )
}

/**
 * 一趟巡迴的進度。[order] 是 13 站依序要挑戰的關卡（前 9 站開局就先洗好，但畫面上一站一站揭曉）。
 * [revealed] 是已經揭曉（抽籤、飛過去）的站數，
 * 比 [cleared] 多 1 表示下一站已經抽好、飛機也到了。[loadout] 是打完上一站時的機體狀態，下一站沿用。
 */
data class TourState(
    val cat: CatType,
    val order: List<Int>,
    val cleared: Int,
    val revealed: Int,
    val loadout: Loadout?,
    val totalScore: Int,
) {
    val finished: Boolean get() = cleared >= order.size
    /** 下一站（從 0 算）。 */
    val nextBout: Int get() = cleared
    val nextStage: Int? get() = order.getOrNull(cleared)

    fun stageAt(bout: Int): StageDef? = order.getOrNull(bout)?.let { Stages.get(it) }

    /** 上一站的國家（第一站從台灣出發）。 */
    fun departure(): Country = stageAt(cleared - 1)?.country ?: Countries.HOME

    /** 下一站已經抽好、飛機也到了。 */
    val nextRevealed: Boolean get() = revealed > cleared

    fun reveal(): TourState = copy(revealed = maxOf(revealed, cleared + 1))

    fun afterClear(score: Int, loadout: Loadout): TourState =
        copy(cleared = cleared + 1, revealed = maxOf(revealed, cleared + 1), loadout = loadout, totalScore = totalScore + score)

    fun encode(): String = listOf(
        cat.name,
        order.joinToString("."),
        cleared,
        revealed,
        loadout?.encode()?.replace(",", "~") ?: "",
        totalScore,
    ).joinToString(";")

    companion object {
        fun decode(text: String?): TourState? {
            var parts = text?.split(";") ?: return null
            // 舊版存檔多一欄 3×3 格子的位置，直接略過
            if (parts.size == 7) parts = parts.filterIndexed { i, _ -> i != 2 }
            if (parts.size != 6) return null
            return runCatching {
                val order = parts[1].split(".").map { it.toInt() }
                require(order.size == Tour.SIZE)
                TourState(
                    cat = CatType.valueOf(parts[0]),
                    order = order,
                    cleared = parts[2].toInt().coerceIn(0, Tour.SIZE),
                    revealed = parts[3].toInt().coerceIn(0, Tour.SIZE),
                    loadout = Loadout.decode(parts[4].replace("~", ",").ifEmpty { null }),
                    totalScore = parts[5].toInt(),
                )
            }.getOrNull()
        }
    }
}
