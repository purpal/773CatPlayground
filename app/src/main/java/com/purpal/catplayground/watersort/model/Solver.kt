package com.purpal.catplayground.watersort.model

/** 深度優先搜尋解題器，用來確認產生的關卡一定有解。 */
object Solver {

    fun solve(start: Board, maxNodes: Int = 200_000): List<Move>? {
        val visited = HashSet<String>()
        val path = ArrayList<Move>()
        var nodes = 0

        fun dfs(board: Board): Boolean {
            if (board.isSolved()) return true
            if (++nodes > maxNodes) return false
            if (!visited.add(board.key())) return false
            for (move in candidateMoves(board)) {
                path.add(move)
                if (dfs(board.pour(move.from, move.to))) return true
                path.removeAt(path.lastIndex)
                if (nodes > maxNodes) return false
            }
            return false
        }

        return if (dfs(start)) path.toList() else null
    }

    private fun candidateMoves(board: Board): List<Move> {
        val scored = ArrayList<Pair<Move, Int>>()
        for (from in board.tubes.indices) {
            val source = board.tubes[from]
            if (source.isEmpty() || board.isComplete(from)) continue
            val run = board.topRun(from)
            val uniform = run == source.size
            var triedEmpty = false
            for (to in board.tubes.indices) {
                if (!board.canPour(from, to)) continue
                val score = if (board.tubes[to].isEmpty()) {
                    // 整支同色倒進空管沒有意義；多支空管彼此等價，只試一支。
                    if (uniform || triedEmpty) continue
                    triedEmpty = true
                    0
                } else {
                    2 + if (board.pourAmount(from, to) == run) 1 else 0
                }
                scored.add(Move(from, to) to score)
            }
        }
        scored.sortByDescending { it.second }
        return scored.map { it.first }
    }
}
