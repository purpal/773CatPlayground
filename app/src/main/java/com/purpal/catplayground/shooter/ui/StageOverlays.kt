package com.purpal.catplayground.shooter.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.Phase
import com.purpal.catplayground.shooter.model.Speaker
import com.purpal.catplayground.shooter.model.StageTheme
import com.purpal.catplayground.shooter.model.World
import com.purpal.catplayground.shooter.render.drawBossPortrait
import com.purpal.catplayground.shooter.render.drawCatShip
import com.purpal.catplayground.ui.PlayColors
import kotlinx.coroutines.delay

private val Gold = Color(0xFFFFD54F)

@Composable
private fun Modifier.consumeTouches(): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}

@Composable
fun StageOverlays(
    world: World,
    phase: Phase,
    bestScore: Int,
    hasNextStage: Boolean,
    onNextStage: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    when (phase) {
        Phase.INTRO -> StageTitle(world)
        Phase.WARNING -> Warning(world)
        Phase.OUTRO_DIALOG -> Dialogue(world)
        Phase.CONTINUE -> ContinuePanel(world, onRestart, onExit)
        Phase.CLEAR -> ClearPanel(world, bestScore, hasNextStage, onNextStage, onExit)
        else -> Unit
    }
}

private fun bossKind(theme: StageTheme) = when (theme) {
    StageTheme.CHEESE_BARN -> EnemyKind.BOSS_MOUSE
    StageTheme.PARK -> EnemyKind.BOSS_DOG
    StageTheme.HONEY_FOREST -> EnemyKind.BOSS_BEAR
}

@Composable
private fun StageTitle(world: World) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(400))
        delay(((World.INTRO_TIME - 0.9f) * 1000).toLong())
        alpha.animateTo(0f, tween(500))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha.value
                translationX = (1f - alpha.value) * 60.dp.toPx()
            },
    ) {
        Text("STAGE ${world.stage.number}", style = titleStyle(52.sp))
        Text("${world.stage.animal}的${world.stage.title}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(
            "按住畫面下方：自動射擊\n手指移動：貓咪跟著移動",
            color = Color.White,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * Boss 警告：上下兩條捲動的黃黑警示帶、閃爍的紅色 WARNING，
 * 中間是關卡、Boss 名字與頭像。
 */
@Composable
private fun Warning(world: World) {
    val loop = rememberInfiniteTransition(label = "warning")
    val stripe by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing)), label = "stripe")
    val blink by loop.animateFloat(0.35f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "blink")
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }

    Box(Modifier.fillMaxSize()) {
        // 四周泛紅
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    0.55f to Color.Transparent,
                    1f to Color(0xFFFF1744).copy(alpha = 0.55f * blink),
                    center = center,
                    radius = size.maxDimension * 0.7f,
                ),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleY = enter.value.coerceIn(0f, 1.2f)
                    alpha = enter.value.coerceIn(0f, 1f)
                },
        ) {
            HazardStripe(stripe)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE6140A26))
                    .padding(vertical = 14.dp),
            ) {
                Text("WARNING", style = titleStyle(54.sp, Color(0xFFFF1744).copy(alpha = blink), Color.White))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        Modifier
                            .size(84.dp)
                            .background(Color(0xFF3A1D5C), CircleShape)
                            .border(3.dp, Gold, CircleShape)
                            .clip(CircleShape),
                    ) {
                        withTransform({
                            translate(size.width / 2f, size.height * 0.62f)
                            val s = size.width / 230f
                            scale(s, s, Offset.Zero)
                        }) { drawBossPortrait(bossKind(world.stage.theme), phase = 1, time = world.time) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("STAGE ${world.stage.number}　BOSS", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(world.stage.bossName, style = titleStyle(34.sp, Color.White))
                        Text("接近中！", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HazardStripe(1f - stripe)
        }
    }
}

@Composable
private fun HazardStripe(offset: Float) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        drawRect(Color(0xFFFFD600))
        val w = size.height * 1.6f
        clipRect {
            var x = -w * 2 + offset * w * 2
            while (x < size.width + w) {
                val path = Path().apply {
                    moveTo(x, size.height)
                    lineTo(x + w * 0.5f, 0f)
                    lineTo(x + w, 0f)
                    lineTo(x + w * 0.5f, size.height)
                    close()
                }
                drawPath(path, Color(0xFF1A1033))
                x += w
            }
        }
    }
}

/** 打倒 Boss 之後的簡短對話。 */
@Composable
private fun Dialogue(world: World) {
    val sound = LocalSound.current
    val lines = world.stage.outro
    var index by remember { mutableIntStateOf(0) }
    var visible by remember(index) { mutableIntStateOf(0) }
    val line = lines.getOrNull(index)
    if (line == null) {
        LaunchedEffect(Unit) { world.dialogFinished() }
        return
    }
    LaunchedEffect(index) {
        sound.play(Sfx.DIALOG)
        while (visible < line.text.length) {
            delay(33)
            visible++
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (visible < line.text.length) visible = line.text.length else index++
            },
    ) {
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
                    translate(size.width / 2f, size.height * (if (isCat) 0.62f else 0.64f))
                    val s = if (isCat) size.width / 90f else size.width / 250f
                    scale(s, s, Offset.Zero)
                }) {
                    if (isCat) drawCatShip(world.player.cat, 0f, 0f, transformed = false)
                    else drawBossPortrait(bossKind(world.stage.theme), phase = 0, time = 0f)
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
                Text(line.text.take(visible), color = Color.White, fontSize = 17.sp, modifier = Modifier.heightIn(min = 48.dp))
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
                .background(Brush.verticalGradient(listOf(Color(0xFF6A1B9A), Color(0xFF1A1033))), RoundedCornerShape(28.dp))
                .border(2.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) { content() }
    }
}

/** 不用圖層的實心按鈕（避免背景消失的問題）。 */
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

private fun titleStyle(size: TextUnit, color: Color = Gold, shadow: Color = Color.Black) = TextStyle(
    color = color,
    fontSize = size,
    fontWeight = FontWeight.Black,
    shadow = Shadow(shadow.copy(alpha = 0.7f), Offset(0f, 6f), 10f),
)
