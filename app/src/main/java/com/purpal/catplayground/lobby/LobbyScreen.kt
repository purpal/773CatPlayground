package com.purpal.catplayground.lobby

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.Screen
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayBackground
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.watersort.Ranks
import com.purpal.catplayground.watersort.model.LevelGenerator

@Composable
fun LobbyScreen(
    waterSortCleared: Int,
    version: String,
    onOpen: (Screen) -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val sound = LocalSound.current
    PlayBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton(if (sound.musicOn) "🎵" else "🔇") { sound.toggleMusic() }
                Spacer(Modifier.width(8.dp))
                PillButton(if (sound.sfxOn) "🔊" else "🔈") { sound.toggleSfx() }
            }
            Spacer(Modifier.height(12.dp))
            BouncingCat()
            Text(
                "773喵遊樂園",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                "小7 的遊戲大廳 🐾",
                color = PlayColors.Sun,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(28.dp))

            for (game in GameRegistry.games) {
                GameCard(
                    game = game,
                    status = statusFor(game.id, waterSortCleared),
                    onClick = { onOpen(game.start) },
                )
                Spacer(Modifier.height(16.dp))
            }
            ComingSoonCard()
            Spacer(Modifier.height(24.dp))
            Text(
                "版本 v$version　·　檢查更新",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        sound.play(Sfx.CLICK)
                        onCheckUpdate()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

private fun statusFor(id: String, waterSortCleared: Int): String? = when (id) {
    GameRegistry.WATER_SORT -> {
        val rank = Ranks.forCleared(waterSortCleared)
        "${rank.emoji} ${rank.title}　已過 $waterSortCleared / ${LevelGenerator.LEVEL_COUNT} 關"
    }
    else -> null
}

@Composable
private fun BouncingCat() {
    val transition = rememberInfiniteTransition(label = "cat")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce",
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "😸",
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer {
                translationY = -bounce * 16.dp.toPx()
                rotationZ = (bounce - 0.5f) * 10f
            },
        )
    }
}

@Composable
private fun GameCard(game: GameEntry, status: String?, onClick: () -> Unit) {
    val sound = LocalSound.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(game.colors))
            .clickable {
                sound.play(Sfx.SELECT)
                onClick()
            }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PlayColors.CardStrong),
            contentAlignment = Alignment.Center,
        ) {
            Text(game.emoji, fontSize = 36.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(game.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(game.description, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            if (status != null) {
                Spacer(Modifier.height(6.dp))
                Text(status, color = PlayColors.Sun, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("▶", color = Color.White, fontSize = 24.sp)
    }
}

@Composable
private fun ComingSoonCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, PlayColors.CardStrong, RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("🎁 更多遊戲即將推出…", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
    }
}
