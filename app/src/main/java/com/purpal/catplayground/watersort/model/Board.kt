package com.purpal.catplayground.watersort.model

data class Move(val from: Int, val to: Int)

/** 每支試管由下往上存放顏色編號；`capacity` 是一支試管最多幾層水。 */
data class Board(val tubes: List<List<Int>>, val capacity: Int) {

    val tubeCount: Int get() = tubes.size

    /** 最上面連續同色的層數。 */
    fun topRun(index: Int): Int {
        val tube = tubes[index]
        if (tube.isEmpty()) return 0
        val color = tube.last()
        var run = 0
        for (k in tube.indices.reversed()) {
            if (tube[k] == color) run++ else break
        }
        return run
    }

    fun canPour(from: Int, to: Int): Boolean {
        if (from == to) return false
        val source = tubes[from]
        val target = tubes[to]
        if (source.isEmpty() || target.size >= capacity) return false
        return target.isEmpty() || target.last() == source.last()
    }

    fun pourAmount(from: Int, to: Int): Int =
        if (!canPour(from, to)) 0 else minOf(topRun(from), capacity - tubes[to].size)

    fun pour(from: Int, to: Int): Board {
        val amount = pourAmount(from, to)
        require(amount > 0) { "Cannot pour from $from to $to" }
        val next = tubes.map { it.toMutableList() }
        val color = next[from].last()
        repeat(amount) {
            next[from].removeAt(next[from].lastIndex)
            next[to].add(color)
        }
        return Board(next, capacity)
    }

    fun isComplete(index: Int): Boolean {
        val tube = tubes[index]
        return tube.size == capacity && tube.all { it == tube[0] }
    }

    fun isSolved(): Boolean = tubes.indices.all { tubes[it].isEmpty() || isComplete(it) }

    /** 與試管順序無關的狀態鍵，供解題器去重。 */
    fun key(): String = tubes.map { it.joinToString(",") }.sorted().joinToString("|")
}
