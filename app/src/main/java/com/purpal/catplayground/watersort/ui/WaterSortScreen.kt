package com.purpal.catplayground.watersort.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayBackground
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.watersort.Praise
import com.purpal.catplayground.watersort.Rank
import com.purpal.catplayground.watersort.Ranks
import com.purpal.catplayground.watersort.model.LevelGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class WinInfo(val praise: String, val rankUp: Rank?)

@Composable
fun WaterSortScreen(
    level: Int,
    cleared: Int,
    onCleared: (Int) -> Unit,
    onNextLevel: () -> Unit,
    onBack: () -> Unit,
) {
    var game by remember(level) { mutableStateOf<WaterSortGame?>(null) }
    LaunchedEffect(level) {
        game = withContext(Dispatchers.Default) { WaterSortGame(level, LevelGenerator.generate(level)) }
    }
    var win by remember(level) { mutableStateOf<WinInfo?>(null) }

    PlayBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val current = game
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PillButton("⬅") { onBack() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("第 $level 關", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("步數 ${current?.moves ?: 0}", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
                }
                PillButton("↩ 復原", enabled = current?.canUndo == true) { current?.undo() }
                Spacer(Modifier.width(8.dp))
                PillButton("🔄") { current?.restart() }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (current == null) {
                    Text("準備關卡中… 🐾", color = Color.White, fontSize = 18.sp)
                } else {
                    key(current) {
                        GameBoard(game = current, cleared = cleared, onCleared = onCleared, onWin = { win = it })
                    }
                }
            }
            Text(
                "點一支試管，再點另一支把水倒過去",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        val finished = game
        val info = win
        if (finished != null && info != null) {
            WinOverlay(
                level = level,
                moves = finished.moves,
                praise = info.praise,
                rankUp = info.rankUp,
                isLastLevel = level >= LevelGenerator.LEVEL_COUNT,
                onNext = onNextLevel,
                onLevels = onBack,
            )
        }
    }
}

@Composable
private fun GameBoard(
    game: WaterSortGame,
    cleared: Int,
    onCleared: (Int) -> Unit,
    onWin: (WinInfo) -> Unit,
) {
    val sound = LocalSound.current
    val scope = rememberCoroutineScope()
    val pourProgress = remember { Animatable(0f) }
    val flash = remember { Animatable(1f) }
    var flashTube by remember { mutableIntStateOf(-1) }

    LaunchedEffect(game.pour) {
        if (game.pour == null) return@LaunchedEffect
        pourProgress.snapTo(0f)
        launch {
            delay(POUR_SOUND_DELAY_MS)
            sound.play(Sfx.POUR)
        }
        pourProgress.animateTo(1f, tween(POUR_DURATION_MS, easing = LinearEasing))
        val completed = game.finishPour()
        if (completed != null) {
            sound.play(Sfx.COMPLETE)
            flashTube = completed
            scope.launch {
                flash.snapTo(0f)
                flash.animateTo(1f, tween(700))
            }
        }
        if (game.board.isSolved()) {
            val rankBefore = Ranks.forCleared(cleared)
            val rankAfter = Ranks.forCleared(maxOf(cleared, game.level))
            val rankUp = rankAfter.takeIf { it != rankBefore }
            onCleared(game.level)
            // finishPour() 把 pour 設回 null 會讓這個 LaunchedEffect 重啟並取消自己，
            // 所以等待後顯示勝利畫面要放在不受影響的 scope 裡。
            scope.launch {
                delay(500)
                sound.play(if (rankUp != null) Sfx.RANK_UP else Sfx.WIN)
                onWin(WinInfo(Praise.random(), rankUp))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        BoardCanvas(
            game = game,
            pourProgress = pourProgress.value,
            flashTube = flashTube,
            flashProgress = flash.value,
            onTap = { index ->
                when (game.tap(index)) {
                    TapResult.SELECTED -> sound.play(Sfx.SELECT)
                    TapResult.DESELECTED -> sound.play(Sfx.CLICK)
                    TapResult.INVALID -> sound.play(Sfx.INVALID)
                    TapResult.POURED, TapResult.IGNORED -> Unit
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
