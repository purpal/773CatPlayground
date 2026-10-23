package com.purpal.catplayground.watersort.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.purpal.catplayground.audio.VICTORY_IMPACT_SEC
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.watersort.Rank
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Gold = Color(0xFFFFD54F)
private val GoldLight = Color(0xFFFFF8E1)
private val GoldDark = Color(0xFFB8860B)
private val BannerTop = Color(0xFF1E3A7A)
private val BannerBottom = Color(0xFF0B1A3A)

// 時間軸（毫秒）：橫幅從畫面外砸下來，落地時閃光、震波、震動，接著依序顯示文字，最後才出現按鈕。
private val SLAM_MS = (VICTORY_IMPACT_SEC * 1000).toFloat()
private val STAR_MS = SLAM_MS + 450f
private val PRAISE_MS = SLAM_MS + 850f
private val RANK_MS = SLAM_MS + 1250f
private const val INTRO_MS = 2600

/** easeOutBack：會稍微衝過頭再回來。 */
private val Overshoot = Easing { x ->
    val c = 2.2f
    val t = x - 1f
    1f + (c + 1f) * t * t * t + c * t * t
}

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
    val timeline = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        timeline.animateTo(INTRO_MS.toFloat(), tween(INTRO_MS, easing = LinearEasing))
    }
    val t = timeline.value
    fun phase(start: Float, duration: Float) = ((t - start) / duration).coerceIn(0f, 1f)

    val loop = rememberInfiniteTransition(label = "victory")
    val rayAngle by loop.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "rays",
    )
    val glow by loop.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )

    val buttonsAt = (if (rankUp != null) RANK_MS + 450f else PRAISE_MS + 500f)
    val buttonsIn = phase(buttonsAt, 350f)
    val slam = phase(0f, SLAM_MS)
    val landed = phase(SLAM_MS, 220f)
    val bannerScale = if (slam < 1f) lerp(3f, 0.9f, slam * slam) else lerp(0.9f, 1f, FastOutSlowInEasing.transform(landed))
    val shake = (1f - phase(SLAM_MS, 380f)).let { if (t < SLAM_MS) 0f else it }
    val flash = if (t < SLAM_MS) 0f else 1f - phase(SLAM_MS, 420f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f * phase(0f, 250f)))
            // 介紹動畫進行中點一下可以直接跳到結尾；同時擋住後面的盤面。
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (timeline.value < INTRO_MS) scope.launch { timeline.snapTo(INTRO_MS.toFloat()) }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (t > SLAM_MS) Confetti(alpha = phase(SLAM_MS, 600f))

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // 內容放在可捲動的置中區塊，按鈕固定在下方，螢幕再矮或字再大都看得到按鈕。
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .graphicsLayer {
                            translationX = sin(t * 0.21f) * 10.dp.toPx() * shake
                            translationY = cos(t * 0.17f) * 8.dp.toPx() * shake
                        },
                ) {
                    VictoryBanner(
                        scale = bannerScale,
                        alpha = phase(0f, 150f),
                        rayAngle = rayAngle,
                        raysAlpha = phase(SLAM_MS, 500f) * glow,
                        shockwave = if (t < SLAM_MS) -1f else phase(SLAM_MS, 650f),
                        sparks = if (t < SLAM_MS) -1f else phase(SLAM_MS, 1100f),
                    )

                    Row(horizontalArrangement = Arrangement.Center) {
                        for (k in 0 until 3) {
                            val pop = Overshoot.transform(phase(STAR_MS + k * 150f, 300f))
                            Text(
                                "⭐",
                                fontSize = if (k == 1) 46.sp else 36.sp,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .graphicsLayer {
                                        scaleX = pop
                                        scaleY = pop
                                        alpha = pop.coerceIn(0f, 1f)
                                        translationY = if (k == 1) -6.dp.toPx() else 0f
                                    },
                            )
                        }
                    }

                    val praiseIn = phase(PRAISE_MS, 350f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                alpha = praiseIn
                                translationY = (1f - praiseIn) * 24.dp.toPx()
                            },
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            praise,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                shadow = Shadow(Gold.copy(alpha = 0.8f), blurRadius = 18f),
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isLastLevel) "全部 $level 關都通過了！" else "第 $level 關完成・$moves 步",
                            color = GoldLight.copy(alpha = 0.85f),
                            fontSize = 16.sp,
                        )
                    }

                    if (rankUp != null) {
                        val rankIn = phase(RANK_MS, 400f)
                        val pop = Overshoot.transform(rankIn)
                        Spacer(Modifier.height(16.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = pop
                                    scaleY = pop
                                    alpha = rankIn
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.verticalGradient(listOf(BannerTop, BannerBottom)))
                                .border(2.dp, Gold.copy(alpha = glow), RoundedCornerShape(18.dp))
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Text("稱號升級！", color = PlayColors.Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${rankUp.emoji} ${rankUp.title}", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .height(56.dp),
            ) {
                if (buttonsIn > 0f) Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            alpha = buttonsIn
                            translationY = (1f - buttonsIn) * 30.dp.toPx()
                        },
                ) {
                    val ready = buttonsIn >= 1f
                    PillButton(
                        "📋 關卡列表",
                        enabled = ready,
                        background = BannerTop,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                    ) { onLevels() }
                    if (!isLastLevel) {
                        Spacer(Modifier.width(12.dp))
                        PillButton(
                            "下一關 ▶",
                            enabled = ready,
                            background = Gold,
                            contentColor = PlayColors.Night,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                        ) { onNext() }
                    }
                }
            }
        }

        if (flash > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flash * flash * 0.75f)),
            )
        }
    }
}

@Composable
private fun VictoryBanner(
    scale: Float,
    alpha: Float,
    rayAngle: Float,
    raysAlpha: Float,
    shockwave: Float,
    sparks: Float,
) {
    val sparkSeeds = remember { Random(9).let { r -> List(40) { r.nextFloat() to r.nextFloat() } } }
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawRays(center, rayAngle, raysAlpha)
            if (shockwave in 0f..0.999f) {
                val eased = FastOutSlowInEasing.transform(shockwave)
                drawCircle(
                    color = Gold.copy(alpha = 1f - shockwave),
                    radius = size.width * 0.75f * eased,
                    center = center,
                    style = Stroke(width = 14.dp.toPx() * (1f - shockwave)),
                )
                drawCircle(
                    color = Color.White.copy(alpha = (1f - shockwave) * 0.6f),
                    radius = size.width * 0.5f * eased,
                    center = center,
                    style = Stroke(width = 6.dp.toPx() * (1f - shockwave)),
                )
            }
            if (sparks in 0f..0.999f) {
                val eased = FastOutSlowInEasing.transform(sparks)
                sparkSeeds.forEachIndexed { k, (speed, size) ->
                    val angle = k * 2f * PI.toFloat() / sparkSeeds.size + speed
                    val distance = this.size.width * (0.25f + 0.55f * speed) * eased
                    drawCircle(
                        color = (if (k % 3 == 0) Color.White else Gold).copy(alpha = 1f - sparks),
                        radius = (2f + size * 4f).dp.toPx() * (1f - sparks * 0.6f),
                        center = center + Offset(cos(angle) * distance, sin(angle) * distance * 0.7f),
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        ) {
            Canvas(Modifier.fillMaxSize()) { drawBannerFrame() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "勝利",
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(GoldLight, Gold, GoldDark)),
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 6f), 8f),
                    ),
                )
                Text(
                    "V I C T O R Y",
                    style = TextStyle(
                        color = Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawRays(center: Offset, angle: Float, alpha: Float) {
    if (alpha <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Gold.copy(alpha = 0.55f * alpha), Gold.copy(alpha = 0f)),
            center = center,
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = center,
    )
    val radius = size.width * 1.2f
    val count = 14
    rotate(angle, pivot = center) {
        for (k in 0 until count) {
            val a0 = k * 2f * PI.toFloat() / count
            val a1 = a0 + PI.toFloat() / count * 0.55f
            val ray = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + cos(a0) * radius, center.y + sin(a0) * radius)
                lineTo(center.x + cos(a1) * radius, center.y + sin(a1) * radius)
                close()
            }
            drawPath(
                ray,
                Brush.radialGradient(
                    listOf(Gold.copy(alpha = 0.35f * alpha), Gold.copy(alpha = 0f)),
                    center = center,
                    radius = radius * 0.6f,
                ),
            )
        }
    }
}

/** 深藍色的六角形橫幅，金邊、左右金色翅膀、上下菱形裝飾。 */
private fun DrawScope.drawBannerFrame() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val halfH = size.height * 0.3f
    val tip = halfH * 0.9f
    // 讓兩側翅膀剛好收在畫面內。
    val wingSpan = tip * 0.4f + halfH * (0.84f + 0.75f) + 4.dp.toPx()
    val halfW = maxOf(size.width / 2f - wingSpan, size.width * 0.2f)
    val gold = Brush.verticalGradient(listOf(GoldLight, Gold, GoldDark), startY = cy - halfH, endY = cy + halfH)

    for (side in listOf(-1f, 1f)) {
        for (k in 0 until 3) {
            val baseX = cx + side * (halfW + tip * 0.4f + k * halfH * 0.42f)
            val reach = halfH * (1.25f - k * 0.25f)
            val feather = Path().apply {
                moveTo(baseX, cy - reach)
                lineTo(baseX + side * halfH * 0.75f, cy)
                lineTo(baseX, cy + reach)
                lineTo(baseX + side * halfH * 0.3f, cy)
                close()
            }
            drawPath(feather, gold, alpha = 1f - k * 0.2f)
        }
    }

    val band = Path().apply {
        moveTo(cx - halfW - tip, cy)
        lineTo(cx - halfW, cy - halfH)
        lineTo(cx + halfW, cy - halfH)
        lineTo(cx + halfW + tip, cy)
        lineTo(cx + halfW, cy + halfH)
        lineTo(cx - halfW, cy + halfH)
        close()
    }
    drawPath(band, Brush.verticalGradient(listOf(BannerTop, BannerBottom), startY = cy - halfH, endY = cy + halfH))
    drawPath(band, gold, style = Stroke(width = 4.dp.toPx()))
    val inset = 7.dp.toPx()
    val inner = Path().apply {
        moveTo(cx - halfW - tip + inset * 1.6f, cy)
        lineTo(cx - halfW + inset * 0.4f, cy - halfH + inset)
        lineTo(cx + halfW - inset * 0.4f, cy - halfH + inset)
        lineTo(cx + halfW + tip - inset * 1.6f, cy)
        lineTo(cx + halfW - inset * 0.4f, cy + halfH - inset)
        lineTo(cx - halfW + inset * 0.4f, cy + halfH - inset)
        close()
    }
    drawPath(inner, Gold.copy(alpha = 0.5f), style = Stroke(width = 1.5f.dp.toPx()))

    for (dir in listOf(-1f, 1f)) {
        val y = cy + dir * (halfH + 2.dp.toPx())
        val d = halfH * 0.38f
        val diamond = Path().apply {
            moveTo(cx, y - d)
            lineTo(cx + d * 0.8f, y)
            lineTo(cx, y + d)
            lineTo(cx - d * 0.8f, y)
            close()
        }
        drawPath(diamond, gold)
        drawPath(diamond, BannerBottom, style = Stroke(width = 1.5f.dp.toPx()))
    }
}

private class ConfettiPiece(random: Random) {
    val x = random.nextFloat()
    val offset = random.nextFloat()
    val speed = 0.6f + random.nextFloat() * 0.8f
    val sway = 0.02f + random.nextFloat() * 0.05f
    val spin = 180f + random.nextFloat() * 540f
    val size = 0.6f + random.nextFloat() * 0.8f
    val color = if (random.nextInt(3) == 0) Gold else TubeColors.palette[random.nextInt(TubeColors.palette.size)]
}

@Composable
private fun Confetti(alpha: Float) {
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
                drawRect(piece.color.copy(alpha = alpha), topLeft = Offset(x - w / 2, y - w), size = Size(w, w * 2f))
            }
        }
    }
}
