package com.purpal.catplayground.shooter.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.shooter.model.Countries
import com.purpal.catplayground.shooter.model.Country
import com.purpal.catplayground.shooter.model.Tour
import com.purpal.catplayground.shooter.model.TourState
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.ArtImages
import com.purpal.catplayground.ui.FitText
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.hypot

/** 地圖圖片（tools/art/tour.py）的範圍：經度 -180～180，緯度 84～-86。 */
private const val MAP_LAT_TOP = 84f
private const val MAP_LAT_BOTTOM = -86f
private const val MAP_ASPECT = 1600f / 756f
private val Gold = Color(0xFFFFD54F)

/** 抽籤 → 揭曉 → 搭飛機過去 → 可以出發。 */
private enum class TourStep { ROULETTE, REVEAL, FLY, READY }

/**
 * 世界巡迴：上方是世界地圖（小飛機從上一站飛到下一站），下方依序是萌王 1 格、三大天王 3 格、
 * 前 9 站的 3×3 格子。格子一開始都是問號；前 9 站每次在剩下的問號裡抽籤，抽中的格子換成這一站的對手。
 */
@Composable
fun TourScreen(
    tour: TourState,
    onRevealed: (TourState) -> Unit,
    onFight: () -> Unit,
    onNewTour: () -> Unit,
    onBack: () -> Unit,
) {
    val sound = LocalSound.current
    val bout = tour.cleared
    var step by remember(bout) {
        mutableStateOf(
            when {
                tour.finished || tour.nextRevealed -> TourStep.READY
                bout < Tour.RANDOM_BOUTS -> TourStep.ROULETTE
                else -> TourStep.REVEAL
            },
        )
    }
    var highlight by remember(bout) { mutableIntStateOf(-1) }
    val flight = remember(bout) { Animatable(if (tour.nextRevealed || tour.finished) 1f else 0f) }

    LaunchedEffect(bout) {
        if (tour.finished || tour.nextRevealed) return@LaunchedEffect
        if (bout < Tour.RANDOM_BOUTS) {
            // 在還是問號的格子之間跳來跳去，越跳越慢，最後停在抽中的那一格
            val remaining = (0 until Tour.RANDOM_BOUTS).filter { box -> tour.boxes.indexOf(box) >= bout }
            val target = tour.boxes[bout]
            val steps = 16 + remaining.size
            val start = Math.floorMod(remaining.indexOf(target) - (steps - 1), remaining.size)
            var wait = 55L
            for (i in 0 until steps) {
                highlight = remaining[(start + i) % remaining.size]
                sound.play(Sfx.SELECT)
                delay(wait)
                wait = (wait * 1.1f).toLong()
            }
            highlight = target
        }
        step = TourStep.REVEAL
        sound.play(Sfx.POWER_UP)
        delay(800)
        step = TourStep.FLY
        sound.play(Sfx.MISSILE)
        flight.animateTo(1f, tween(2300, easing = FastOutSlowInEasing))
        step = TourStep.READY
        onRevealed(tour.reveal())
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2E7DD1), Color(0xFF6A1B9A), Color(0xFF1A1033)))),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 96.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PillButton("⬅") { onBack() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    FitText("世界巡迴", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    FitText(
                        if (tour.finished) "全部 ${Tour.SIZE} 站制霸！" else "第 ${bout + 1} / ${Tour.SIZE} 站　${tour.cat.shortName}出征中",
                        color = PlayColors.Sun,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            WorldMap(tour, flight.value, showDestination = step != TourStep.ROULETTE)
            Spacer(Modifier.height(10.dp))
            NextBoutCard(tour, step)
            Spacer(Modifier.height(12.dp))
            Bracket(tour, step, highlight)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE1A1033))))
                .systemBarsPadding()
                .padding(16.dp),
        ) {
            if (tour.finished) {
                TourButton("🌏 再巡迴一次", enabled = true, onClick = onNewTour)
            } else {
                TourButton(if (step == TourStep.READY) "✈️ 出發挑戰！" else "飛行中…", enabled = step == TourStep.READY, onClick = onFight)
            }
        }
    }
}

/** 地圖上的位置（等距圓柱投影）。 */
private fun DrawScope.mapPos(c: Country) =
    Offset((c.lon + 180f) / 360f * size.width, (MAP_LAT_TOP - c.lat) / (MAP_LAT_TOP - MAP_LAT_BOTTOM) * size.height)

/** 兩國之間往上拱起的航線（二次貝茲曲線）的控制點。 */
private fun arcControl(a: Offset, b: Offset): Offset {
    val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    return Offset(mid.x, mid.y - hypot(b.x - a.x, b.y - a.y) * 0.28f)
}

private fun bezier(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(u * u * a.x + 2f * u * t * c.x + t * t * b.x, u * u * a.y + 2f * u * t * c.y + t * t * b.y)
}

@Composable
private fun WorldMap(tour: TourState, flight: Float, showDestination: Boolean) {
    val context = LocalContext.current
    val map = remember { ArtImages.get(context, "tour_map") }
    val plane = remember { ArtImages.get(context, "tour_plane") }
    val flags = remember { HashMap<String, ImageBitmap>() }
    fun flagOf(c: Country) = flags.getOrPut(c.flag) { ArtImages.get(context, c.flag) }
    val pulse by rememberInfiniteTransition(label = "pin").animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    val routePath = remember { Path() }

    val visited = remember(tour.cleared) { listOf(Countries.HOME) + (0 until tour.cleared).mapNotNull { tour.stageAt(it)?.country } }
    val from = tour.departure()
    val to = if (tour.finished) null else tour.stageAt(tour.cleared)?.country

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(MAP_ASPECT)
            .clip(RoundedCornerShape(16.dp))
            .border(3.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
    ) {
        drawImage(map, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        // 已經飛過的航線
        for (i in 1 until visited.size) {
            val a = mapPos(visited[i - 1])
            val b = mapPos(visited[i])
            val c = arcControl(a, b)
            routePath.reset()
            routePath.moveTo(a.x, a.y)
            routePath.quadraticTo(c.x, c.y, b.x, b.y)
            drawPath(routePath, Color.White.copy(alpha = 0.85f), style = Stroke(3f, pathEffect = dash, cap = StrokeCap.Round))
        }
        val flagH = size.height * 0.07f
        fun drawFlag(c: Country, scale: Float) {
            val img = flagOf(c)
            val h = flagH * scale
            val w = h * img.width / img.height
            val p = mapPos(c)
            drawLine(Color(0xFF6B4C52), p, Offset(p.x, p.y - h * 1.2f), 2.5f)
            drawImage(img, dstOffset = IntOffset((p.x).toInt(), (p.y - h * 1.2f - h).toInt()), dstSize = IntSize(w.toInt(), h.toInt()))
            drawCircle(Color.White, 4f, p)
            drawCircle(Color(0xFF6B4C52), 4f, p, style = Stroke(1.5f))
        }
        for (c in visited) drawFlag(c, 0.8f)
        if (to != null && showDestination) {
            val a = mapPos(from)
            val b = mapPos(to)
            val c = arcControl(a, b)
            routePath.reset()
            routePath.moveTo(a.x, a.y)
            routePath.quadraticTo(c.x, c.y, b.x, b.y)
            drawPath(routePath, Gold, style = Stroke(3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), -pulse * 42f), cap = StrokeCap.Round))
            drawCircle(Color(0xFFFF3D5A).copy(alpha = 1f - pulse), 8f + pulse * 22f, b, style = Stroke(3f))
            drawFlag(to, 1.15f)
        }
        // 小飛機
        val a = mapPos(from)
        val p: Offset
        val heading: Offset
        if (to != null && showDestination) {
            val b = mapPos(to)
            val c = arcControl(a, b)
            val t = flight.coerceIn(0f, 1f)
            p = bezier(a, c, b, t)
            val ahead = bezier(a, c, b, (t + 0.02f).coerceAtMost(1f))
            val behind = bezier(a, c, b, (t - 0.02f).coerceAtLeast(0f))
            heading = Offset(ahead.x - behind.x, ahead.y - behind.y)
        } else {
            p = a
            heading = Offset(1f, 0f)
        }
        val left = heading.x < 0f
        val deg = Math.toDegrees(atan2(heading.y.toDouble(), kotlin.math.abs(heading.x).toDouble().coerceAtLeast(0.001))).toFloat()
        val pw = size.width * 0.09f
        val ph = pw * plane.height / plane.width
        withTransform({
            translate(p.x, p.y - ph * 0.3f)
            if (left) scale(-1f, 1f, Offset.Zero)
            rotate(deg, Offset.Zero)
        }) {
            drawImage(plane, dstOffset = IntOffset((-pw / 2f).toInt(), (-ph / 2f).toInt()), dstSize = IntSize(pw.toInt(), ph.toInt()))
        }
    }
}

@Composable
private fun NextBoutCard(tour: TourState, step: TourStep) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (tour.finished) {
            Text("🏆", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                FitText("${tour.cat.shortName}是新一屆的喵星萌王！", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                FitText("巡迴總分 ${tour.totalScore}", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            return@Row
        }
        val bout = tour.cleared
        val stage = tour.stageAt(bout) ?: return@Row
        val shown = step != TourStep.ROULETTE
        if (shown) {
            ArtImage(stage.country.flag, Modifier.height(36.dp).width(54.dp))
        } else {
            Text("❓", fontSize = 30.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText(boutTitle(bout), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            if (shown) {
                FitText("${stage.country.name}・${stage.title}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                FitText(stage.bossName, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            } else {
                FitText("抽籤中…", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 第幾站的稱呼：前 9 站、三大天王、萌王決戰。 */
internal fun boutTitle(bout: Int): String = when {
    bout < Tour.RANDOM_BOUTS -> "第 ${bout + 1} 站"
    bout < Tour.SIZE - 1 -> "三大天王 ${bout - Tour.RANDOM_BOUTS + 1}"
    else -> "萌王決戰"
}

@Composable
private fun Bracket(tour: TourState, step: TourStep, highlight: Int) {
    val bout = tour.cleared
    // 第 i 站的格子現在要不要顯示對手
    fun shown(i: Int) = i < tour.revealed || (i == bout && step != TourStep.ROULETTE)

    Label("👑 萌王")
    BoutBox(tour, Tour.SIZE - 1, shown(Tour.SIZE - 1), current = bout == Tour.SIZE - 1, glow = false, size = 88.dp)
    Spacer(Modifier.height(10.dp))
    Label("⚔️ 三大天王")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (k in 0 until 3) {
            val i = Tour.RANDOM_BOUTS + k
            BoutBox(tour, i, shown(i), current = bout == i, glow = false, size = 78.dp)
        }
    }
    Spacer(Modifier.height(10.dp))
    Label("🌏 世界各地的挑戰者")
    for (row in 0 until 3) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            for (col in 0 until 3) {
                val box = row * 3 + col
                val i = tour.boxes.indexOf(box)
                BoutBox(tour, i, shown(i), current = bout == i && step != TourStep.ROULETTE, glow = step == TourStep.ROULETTE && box == highlight, size = 72.dp)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    FitText(text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
}

/** 一格對手：問號、這一站的對手（金框跳動）、或已經打倒的對手（哭哭臉＋勾勾）。 */
@Composable
private fun BoutBox(tour: TourState, bout: Int, shown: Boolean, current: Boolean, glow: Boolean, size: Dp) {
    val stage = tour.stageAt(bout)
    val cleared = bout < tour.cleared
    val bounce by rememberInfiniteTransition(label = "box").animateFloat(1f, 1.07f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "bounce")
    val shape = RoundedCornerShape(16.dp)
    val border = when {
        glow -> Color.White
        current -> Gold
        cleared -> PlayColors.Mint
        else -> Color.White.copy(alpha = 0.35f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val s = if (current || glow) bounce else 1f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(
                when {
                    glow -> Color(0xFFFFE082)
                    current -> Color(0x55FFD54F)
                    else -> Color.White.copy(alpha = 0.12f)
                },
            )
            .border(3.dp, border, shape),
    ) {
        if (!shown || stage == null) {
            Text("?", color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Black)
        } else {
            ArtImage("${stage.bossArt}_m${if (cleared) 2 else 0}_0", Modifier.size(size * 0.9f), contentDescription = stage.bossName, alpha = if (cleared) 0.75f else 1f)
            ArtImage(stage.country.flag, Modifier.align(Alignment.BottomEnd).padding(4.dp).height(size * 0.22f).width(size * 0.33f))
            if (cleared) Text("✔", color = PlayColors.Mint, fontSize = (size.value * 0.3f).sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

@Composable
private fun TourButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val sound = LocalSound.current
    Box(
        Modifier
            .width(260.dp)
            .heightIn(min = 56.dp)
            .background(if (enabled) Gold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (enabled) {
                    sound.play(Sfx.CLICK)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        FitText(text, color = if (enabled) PlayColors.Night else Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
    }
}
