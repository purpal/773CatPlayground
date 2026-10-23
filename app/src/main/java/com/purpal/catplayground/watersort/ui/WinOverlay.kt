package com.purpal.catplayground.watersort.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.watersort.Rank
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WinOverlay(
    level: Int,
    moves: Int,
    praise: String,
    rankUp: Rank?,
    isLastLevel: Boolean,
    onNext: () -> Unit,
    onLevels: () -> Unit,
) {
    val cardScale = remember { Animatable(0.3f) }
    val stars = List(3) { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        cardScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    LaunchedEffect(Unit) {
        delay(250)
        for (star in stars) {
            star.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
    val dance = rememberInfiniteTransition(label = "dance")
    val wiggle by dance.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "wiggle",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // 吃掉點擊，避免點到後面的盤面。
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Confetti()
        Column(
            modifier = Modifier
                .padding(28.dp)
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(PlayColors.Berry, PlayColors.Dusk)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (rankUp != null) "🎉" else "😻",
                fontSize = 72.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = wiggle * 12f
                    translationY = -kotlin.math.abs(wiggle) * 10.dp.toPx()
                },
            )
            Row(horizontalArrangement = Arrangement.Center) {
                stars.forEach { star ->
                    Text(
                        "⭐",
                        fontSize = 36.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = star.value
                            scaleY = star.value
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(praise, color = PlayColors.Sun, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                if (isLastLevel) "全部 $level 關都通過了！" else "第 $level 關完成！用了 $moves 步",
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )
            if (rankUp != null) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PlayColors.Card)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("稱號升級！", color = PlayColors.Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${rankUp.emoji} ${rankUp.title}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row {
                PillButton("📋 關卡") { onLevels() }
                if (!isLastLevel) {
                    Spacer(Modifier.width(12.dp))
                    PillButton("下一關 ▶", background = PlayColors.Sun, contentColor = PlayColors.Night) { onNext() }
                }
            }
        }
    }
}

private class ConfettiPiece(random: Random) {
    val x = random.nextFloat()
    val offset = random.nextFloat()
    val speed = 0.6f + random.nextFloat() * 0.8f
    val sway = 0.02f + random.nextFloat() * 0.05f
    val spin = 180f + random.nextFloat() * 540f
    val size = 0.6f + random.nextFloat() * 0.8f
    val color = TubeColors.palette[random.nextInt(TubeColors.palette.size)]
}

@Composable
private fun Confetti() {
    val pieces = remember { Random(System.nanoTime()).let { r -> List(90) { ConfettiPiece(r) } } }
    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "time",
    )
    Canvas(Modifier.fillMaxSize()) {
        val unit = size.minDimension / 40f
        for (piece in pieces) {
            val progress = (time * piece.speed + piece.offset) % 1f
            val y = (progress * 1.2f - 0.1f) * size.height
            val x = (piece.x + sin((progress * 6f + piece.offset * 10f).toDouble()).toFloat() * piece.sway) * size.width
            val w = unit * piece.size
            rotate(progress * piece.spin, pivot = Offset(x, y)) {
                drawRect(piece.color, topLeft = Offset(x - w / 2, y - w), size = Size(w, w * 2f))
            }
        }
    }
}
