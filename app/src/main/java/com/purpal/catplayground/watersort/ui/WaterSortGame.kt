package com.purpal.catplayground.watersort.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.purpal.catplayground.watersort.model.Board

/** 正在進行的倒水動作；動畫期間盤面仍顯示 [before]。 */
data class Pour(val from: Int, val to: Int, val color: Int, val amount: Int, val before: Board) {
    val after: Board get() = before.pour(from, to)
}

enum class TapResult { SELECTED, DESELECTED, POURED, INVALID, IGNORED }

/** 一局倒水遊戲的狀態：盤面、選取、復原紀錄與進行中的倒水動畫。 */
class WaterSortGame(val level: Int, private val initial: Board) {
    var board by mutableStateOf(initial)
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var pour by mutableStateOf<Pour?>(null)
        private set
    private val history = mutableStateListOf<Board>()

    val moves: Int get() = history.size
    val canUndo: Boolean get() = history.isNotEmpty() && pour == null

    fun tap(index: Int): TapResult {
        if (pour != null || board.isSolved()) return TapResult.IGNORED
        val current = selected
        if (current == null) {
            if (board.tubes[index].isEmpty() || board.isComplete(index)) return TapResult.INVALID
            selected = index
            return TapResult.SELECTED
        }
        if (current == index) {
            selected = null
            return TapResult.DESELECTED
        }
        if (board.canPour(current, index)) {
            pour = Pour(current, index, board.tubes[current].last(), board.pourAmount(current, index), board)
            selected = null
            return TapResult.POURED
        }
        selected = if (board.tubes[index].isEmpty() || board.isComplete(index)) null else index
        return TapResult.INVALID
    }

    /** 倒水動畫結束時呼叫，回傳剛被填滿的試管（若有）。 */
    fun finishPour(): Int? {
        val p = pour ?: return null
        history.add(p.before)
        board = p.after
        pour = null
        return p.to.takeIf { board.isComplete(it) }
    }

    fun undo() {
        if (!canUndo) return
        board = history.removeAt(history.lastIndex)
        selected = null
    }

    fun restart() {
        if (pour != null) return
        history.clear()
        board = initial
        selected = null
    }
}
