package com.purpal.catplayground.shooter.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Phase
import com.purpal.catplayground.shooter.model.Speaker
import com.purpal.catplayground.shooter.model.World
import com.purpal.catplayground.shooter.render.drawBigCat
import com.purpal.catplayground.shooter.render.drawBossPortrait
import com.purpal.catplayground.shooter.render.drawCatShip
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFFFD54F)

@Composable
private fun Modifier.consumeTouches(): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}

@Composable
fun StageOverlays(
    world: World,
    frame: Long,
    bestScore: Int,
    hasNextStage: Boolean,
    onNextStage: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    frame.let { }
    when (world.phase) {
        Phase.INTRO -> StageTitle(world)
        Phase.WARNING -> Warning(world)
        Phase.BOSS_DIALOG, Phase.OUTRO_DIALOG -> Dialogue(world)
        Phase.CONTINUE -> ContinuePanel(world, onRestart, onExit)
        Phase.CLEAR -> ClearPanel(world, bestScore, hasNextStage, onNextStage, onExit)
        else -> Unit
    }
    if (world.ultimateCinematic > 0f) UltimateCinematic(world)
}

@Composable
private fun StageTitle(world: World) {
    val t = world.phaseTime
    val alpha = (minOf(t / 0.4f, (World.INTRO_TIME - t) / 0.5f)).coerceIn(0f, 1f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                translationX = (1f - alpha) * 60.dp.toPx()
            },
    ) {
        Text("STAGE ${world.stage.number}", style = titleStyle(52.sp))
        Text("${world.stage.animal}的${world.stage.title}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("BOSS：${world.stage.bossName}", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
    }
}

@Composable
private fun Warning(world: World) {
    val blink = (world.phaseTime * 3f).toInt() % 2 == 0
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD50000).copy(alpha = if (blink) 0.55f else 0.3f))
                .padding(vertical = 18.dp),
        ) {
            Text("⚠ WARNING ⚠", style = titleStyle(40.sp, Color.White))
            Text("${world.stage.bossName} 接近中！", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Dialogue(world: World) {
    val sound = LocalSound.current
    val lines = if (world.phase == Phase.BOSS_DIALOG) world.stage.intro else world.stage.outro
    var index by remember(world.phase) { mutableIntStateOf(0) }
    var shownAt by remember(world.phase, index) { mutableLongStateOf(System.currentTimeMillis()) }
    val line = lines.getOrNull(index)
    LaunchedEffect(world.phase, index) { if (line != null) sound.play(Sfx.DIALOG) }
    if (line == null) {
        LaunchedEffect(world.phase) { world.dialogFinished() }
        return
    }
    val elapsed = (System.currentTimeMillis() - shownAt) / 1000f
    val visible = (elapsed * 30f).toInt().coerceAtMost(line.text.length)

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (visible < line.text.length) shownAt = 0L else index++
            },
    ) {
        PillButton(
            "跳過 ⏭",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(top = 64.dp, end = 12.dp),
        ) { index = lines.size }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE1A1033))
                .border(2.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(14.dp),
        ) {
            val isCat = line.speaker == Speaker.CAT
            Canvas(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCat) Color(0xFFFFE0B2) else Color(0xFFE1BEE7)),
            ) {
                withTransform({
                    translate(size.width / 2f, size.height * (if (isCat) 0.6f else 0.62f))
                    val s = if (isCat) size.width / 80f else size.width / 260f
                    scale(s, s, Offset.Zero)
                }) {
                    if (isCat) drawCatShip(world.player.cat, world.time, 0f, transformed = false)
                    else drawBossPortrait(world.boss?.kind ?: return@withTransform, phase = 0, time = world.time)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isCat) world.player.cat.shortName else world.stage.bossName,
                    color = if (isCat) Color(0xFFFFB74D) else Color(0xFFCE93D8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    line.text.take(visible),
                    color = Color.White,
                    fontSize = 17.sp,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                Text("點一下繼續 ▼", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun ContinuePanel(world: World, onRestart: () -> Unit, onExit: () -> Unit) {
    val sound = LocalSound.current
    CenterPanel {
        Text("😿", fontSize = 56.sp)
        Text("${world.player.cat.shortName}被打倒了…", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (world.continuesLeft > 0) {
            Text("要接關嗎？（剩 ${world.continuesLeft} 次）", color = Gold, fontSize = 17.sp)
            Text("接關後分數會歸零、主武器降一級", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            BigButton("💪 接關！", Gold, PlayColors.Night) {
                sound.play(Sfx.POWER_UP)
                world.useContinue()
            }
        } else {
            Text("接關次數用完了，從這一關開頭再挑戰吧！", color = Gold, fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            BigButton("🔄 重新開始本關", Gold, PlayColors.Night, onRestart)
        }
        Spacer(Modifier.height(10.dp))
        BigButton("離開", Color(0xFF3949AB), Color.White, onExit)
    }
}

@Composable
private fun ClearPanel(world: World, bestScore: Int, hasNextStage: Boolean, onNextStage: () -> Unit, onExit: () -> Unit) {
    CenterPanel {
        Text("STAGE CLEAR!", style = titleStyle(40.sp))
        Text("打倒了${world.stage.bossName}！", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("SCORE ${world.score}", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        if (world.score > bestScore) Text("🎉 新紀錄！", color = PlayColors.Mint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        else Text("最高分 $bestScore", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        if (hasNextStage) {
            BigButton("下一關 ▶", Gold, PlayColors.Night, onNextStage)
        } else {
            Text("下一關製作中，敬請期待！", color = Color.White, fontSize = 15.sp)
        }
        Spacer(Modifier.height(10.dp))
        BigButton("📋 關卡選單", Color(0xFF3949AB), Color.White, onExit)
    }
}

@Composable
fun PauseMenu(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    CenterPanel {
        Text("暫停", style = titleStyle(40.sp, Color.White))
        Spacer(Modifier.height(20.dp))
        BigButton("▶ 繼續", Gold, PlayColors.Night, onResume)
        Spacer(Modifier.height(10.dp))
        BigButton("🔄 重新開始本關", Color(0xFF3949AB), Color.White, onRestart)
        Spacer(Modifier.height(10.dp))
        BigButton("離開關卡", Color(0xFF3949AB), Color.White, onExit)
    }
}

@Composable
private fun CenterPanel(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .consumeTouches(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF6A1B9A), Color(0xFF1A1033))))
                .border(2.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) { content() }
    }
}

/** 不用圖層的實心按鈕（避免和勝利畫面一樣的背景消失問題）。 */
@Composable
private fun BigButton(text: String, background: Color, contentColor: Color, onClick: () -> Unit) {
    val sound = LocalSound.current
    Box(
        Modifier
            .width(240.dp)
            .heightIn(min = 52.dp)
            .background(background, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                sound.play(Sfx.CLICK)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun titleStyle(size: androidx.compose.ui.unit.TextUnit, color: Color = Gold) = TextStyle(
    color = color,
    fontSize = size,
    fontWeight = FontWeight.Black,
    shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 6f), 10f),
)

/**
 * 大絕變身動畫：畫面變暗，一條斜斜的光帶從左邊衝進來，
 * 帶子上是老虎／黑豹的大頭，配上「吼——！」和招式名稱，最後一道白光。
 */
@Composable
private fun UltimateCinematic(world: World) {
    val total = World.ULTIMATE_CINEMATIC
    val t = (1f - world.ultimateCinematic / total).coerceIn(0f, 1f)
    val tiger = world.player.cat == CatType.ORANGE
    val bandColor = if (tiger) listOf(Color(0xFFFF6D00), Color(0xFFFFD54F), Color(0xFFFF6D00)) else listOf(Color(0xFF4A148C), Color(0xFFB388FF), Color(0xFF4A148C))
    val slideIn = (t / 0.18f).coerceIn(0f, 1f)
    val slideOut = ((t - 0.82f) / 0.18f).coerceIn(0f, 1f)
    val textPop = ((t - 0.2f) / 0.15f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize().consumeTouches()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.65f * (1f - slideOut)))
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            // 集中線
            for (k in 0 until 36) {
                val a = k * (2 * PI / 36).toFloat() + world.time * 0.6f
                val inner = size.minDimension * 0.35f
                drawLine(
                    Color.White.copy(alpha = 0.25f * (1f - slideOut)),
                    Offset(cx + cos(a) * inner, cy + sin(a) * inner),
                    Offset(cx + cos(a) * size.maxDimension, cy + sin(a) * size.maxDimension),
                    strokeWidth = if (k % 3 == 0) 6f else 2f,
                )
            }
            val bandHeight = size.height * 0.26f
            val slide = (1f - slideIn) * -size.width * 1.5f + slideOut * size.width * 1.5f
            rotate(-10f, Offset(cx, cy)) {
                drawRect(
                    Brush.horizontalGradient(bandColor),
                    topLeft = Offset(-size.width + slide, cy - bandHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width * 3f, bandHeight),
                )
                drawRect(Color.White, Offset(-size.width + slide, cy - bandHeight / 2f - 6f), androidx.compose.ui.geometry.Size(size.width * 3f, 6f))
                drawRect(Color.White, Offset(-size.width + slide, cy + bandHeight / 2f), androidx.compose.ui.geometry.Size(size.width * 3f, 6f))
                val shake = if (t > 0.2f && t < 0.6f) sin(world.time * 120f) * 6f else 0f
                withTransform({
                    translate(cx + slide * 0.6f + shake, cy + bandHeight * 0.05f)
                    val s = bandHeight / 75f
                    scale(s, s, Offset.Zero)
                }) { drawBigCat(world.player.cat, world.time, fierce = true) }
            }
            if (t > 0.9f) drawRect(Color.White.copy(alpha = (t - 0.9f) / 0.1f))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 260.dp)
                .graphicsLayer {
                    val s = 0.4f + textPop * 0.6f + (1f - textPop) * 1.5f
                    scaleX = s
                    scaleY = s
                    alpha = textPop * (1f - slideOut)
                },
        ) {
            Text("吼喔喔喔——！", style = titleStyle(46.sp, Color.White))
            Text(world.player.cat.ultimateName, style = titleStyle(26.sp, if (tiger) Gold else Color(0xFFD1C4E9)))
        }
    }
}
