package com.purpal.catplayground.watersort.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayBackground
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.watersort.Ranks
import com.purpal.catplayground.watersort.model.LevelGenerator

@Composable
fun LevelSelectScreen(cleared: Int, onBack: () -> Unit, onPlay: (Int) -> Unit) {
    val total = LevelGenerator.LEVEL_COUNT
    val nextLevel = (cleared + 1).coerceAtMost(total)
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (cleared - 10).coerceAtLeast(0))

    PlayBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillButton("⬅") { onBack() }
                Spacer(Modifier.width(12.dp))
                Text("彩虹倒水", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(12.dp))
            RankCard(cleared)
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = if (cleared >= total) "🏆 全部通關！再玩一次第 $total 關" else "▶ 繼續第 $nextLevel 關",
                background = PlayColors.Sun,
                contentColor = PlayColors.Night,
                modifier = Modifier.fillMaxWidth(),
            ) { onPlay(nextLevel) }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                state = gridState,
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(total) { index ->
                    val level = index + 1
                    LevelCell(
                        level = level,
                        state = when {
                            level <= cleared -> CellState.CLEARED
                            level == cleared + 1 -> CellState.CURRENT
                            else -> CellState.LOCKED
                        },
                        onClick = { onPlay(level) },
                    )
                }
            }
        }
    }
}

@Composable
fun RankCard(cleared: Int) {
    val rank = Ranks.forCleared(cleared)
    val next = Ranks.next(cleared)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlayColors.Card)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rank.emoji, fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("目前稱號", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(rank.title, color = PlayColors.Sun, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("$cleared / ${LevelGenerator.LEVEL_COUNT}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            val span = (next.minCleared - rank.minCleared).toFloat()
            LinearProgressIndicator(
                progress = { (cleared - rank.minCleared) / span },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = PlayColors.Mint,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "再過 ${next.minCleared - cleared} 關升級為 ${next.emoji} ${next.title}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
        }
    }
}

private enum class CellState { CLEARED, CURRENT, LOCKED }

@Composable
private fun LevelCell(level: Int, state: CellState, onClick: () -> Unit) {
    val sound = LocalSound.current
    val pulse = if (state == CellState.CURRENT) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "scale",
        )
        value
    } else {
        1f
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clip(shape)
            .background(
                when (state) {
                    CellState.CLEARED -> PlayColors.Mint.copy(alpha = 0.85f)
                    CellState.CURRENT -> PlayColors.Sun
                    CellState.LOCKED -> Color.White.copy(alpha = 0.12f)
                },
            )
            .border(2.dp, Color.White.copy(alpha = if (state == CellState.LOCKED) 0.1f else 0.5f), shape)
            .clickable {
                if (state == CellState.LOCKED) {
                    sound.play(Sfx.INVALID)
                } else {
                    sound.play(Sfx.SELECT)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state == CellState.LOCKED) {
            Text("🔒", fontSize = 18.sp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$level", color = PlayColors.Night, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (state == CellState.CLEARED) Text("⭐", fontSize = 10.sp)
            }
        }
    }
}
