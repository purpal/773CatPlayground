package com.purpal.catplayground.watersort

data class Rank(val title: String, val emoji: String, val minCleared: Int)

object Ranks {
    val all = listOf(
        Rank("小貓見習生", "🐱", 0),
        Rank("好奇小貓", "😺", 5),
        Rank("倒水小幫手", "💧", 15),
        Rank("調色小貓", "🎨", 30),
        Rank("彩虹學徒", "🌈", 50),
        Rank("魔法貓咪", "✨", 75),
        Rank("水晶貓師", "💎", 100),
        Rank("星空大師", "🌟", 130),
        Rank("傳說貓王", "👑", 165),
        Rank("倒水之神", "🏆", 200),
    )

    fun forCleared(cleared: Int): Rank = all.last { cleared >= it.minCleared }

    fun next(cleared: Int): Rank? = all.firstOrNull { it.minCleared > cleared }
}
