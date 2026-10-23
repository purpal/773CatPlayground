package com.purpal.catplayground.watersort

import kotlin.random.Random

object Praise {
    private val phrases = listOf(
        "太厲害了！",
        "完美過關！",
        "小7 真聰明！",
        "你是倒水天才！",
        "漂亮的一手！",
        "喵～好棒棒！",
        "顏色排得整整齊齊！",
        "腦袋轉得好快！",
        "這關難不倒你！",
        "超級無敵厲害！",
        "貓咪都為你拍手！",
        "一滴都沒有灑出來！",
        "彩虹都被你整理好了！",
        "繼續保持，你好強！",
        "真是聰明的小腦袋！",
        "哇！你做到了！",
        "比魔法還神奇！",
        "高手中的高手！",
        "這就是大師的實力！",
        "喵喵大讚！",
        "你讓水都乖乖聽話了！",
        "太神啦，下一關也沒問題！",
    )
    private var last = -1

    fun random(): String {
        var index: Int
        do {
            index = Random.nextInt(phrases.size)
        } while (index == last)
        last = index
        return phrases[index]
    }
}
