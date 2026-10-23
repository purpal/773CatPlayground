package com.purpal.catplayground.shooter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Music
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.audio.SoundManager
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.Phase
import com.purpal.catplayground.shooter.model.STEP
import com.purpal.catplayground.shooter.model.SoundCue
import com.purpal.catplayground.shooter.model.StageTheme
import com.purpal.catplayground.shooter.model.Stages
import com.purpal.catplayground.shooter.model.SubWeapon
import com.purpal.catplayground.shooter.model.WORLD_WIDTH
import com.purpal.catplayground.shooter.model.World
import com.purpal.catplayground.shooter.render.drawWorld
import com.purpal.catplayground.shooter.render.viewportFor
import kotlin.math.hypot
import kotlin.random.Random

/** 虛擬搖桿狀態（螢幕座標），不放進 Compose state：每幀都會重畫。 */
private class Joystick {
    var pointer: PointerId? = null
    var baseX = 0f
    var baseY = 0f
    var knobX = 0f
    var knobY = 0f
    val active: Boolean get() = pointer != null
}

@Composable
fun ShooterGameScreen(
    stageNumber: Int,
    cat: CatType,
    ultimateOnLeft: Boolean,
    bestScore: Int,
    onCleared: (stage: Int, score: Int) -> Unit,
    onNextStage: () -> Unit,
    onExit: () -> Unit,
) {
    val stage = remember(stageNumber) { Stages.get(stageNumber)!! }
    val sound = LocalSound.current
    var attempt by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    val joystick = remember { Joystick() }

    BackHandler { paused = !paused }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) paused = true }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val density = LocalDensity.current
        // 上方是固定高度的 HUD，下方才是遊戲畫面，避免 HUD 擋住 Boss。
        val hudHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + HUD_HEIGHT
        val gameHeightPx = constraints.maxHeight - with(density) { hudHeight.toPx() }
        val worldHeight = (WORLD_WIDTH * gameHeightPx / constraints.maxWidth.toFloat()).coerceIn(1400f, 2300f)
        val world = remember(attempt, stageNumber, worldHeight) { World(stage, cat, worldHeight, seed = stageNumber * 31 + attempt) }
        val knobRadius = with(density) { 56.dp.toPx() }

        LaunchedEffect(world) {
            var last = withFrameNanos { it }
            var accumulator = 0f
            var lastPhase = world.phase
            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                if (!paused && world.running) {
                    applyJoystick(world, joystick, knobRadius)
                    accumulator += dt
                    var steps = 0
                    while (accumulator >= STEP && steps < 4) {
                        world.update(STEP)
                        accumulator -= STEP
                        steps++
                    }
                    if (steps == 4) accumulator = 0f
                } else {
                    accumulator = 0f
                }
                playCues(world, sound)
                sound.playMusic(musicFor(world))
                if (world.phase != lastPhase) {
                    if (world.phase == Phase.CLEAR) {
                        sound.play(Sfx.WIN)
                        onCleared(stageNumber, world.score)
                    }
                    lastPhase = world.phase
                }
                frame++
            }
        }

        // 遊戲畫面
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(top = hudHeight),
        ) {
            frame.let { }
            val view = viewportFor(size, world.height)
            drawWorld(world, view)
            if (world.phase == Phase.STAGE || world.phase == Phase.BOSS || world.phase == Phase.WARNING) {
                drawJoystick(joystick, knobRadius, size.width / 2f, size.height - knobRadius * 2.2f)
            }
        }

        // 搖桿輸入：按住下半部任何地方，搖桿底座就出現在手指下。
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(world) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (joystick.pointer == null && change.pressed && !change.previousPressed &&
                                    change.position.y > size.height * 0.45f
                                ) {
                                    joystick.pointer = change.id
                                    joystick.baseX = change.position.x
                                    joystick.baseY = change.position.y
                                    joystick.knobX = change.position.x
                                    joystick.knobY = change.position.y
                                } else if (change.id == joystick.pointer) {
                                    if (change.pressed) {
                                        joystick.knobX = change.position.x
                                        joystick.knobY = change.position.y
                                    } else {
                                        joystick.pointer = null
                                    }
                                }
                            }
                        }
                    }
                },
        )

        val frameTick = frame
        ShooterHud(world, frameTick, Modifier.height(hudHeight), onPause = { paused = true })
        BossBar(world, frameTick, Modifier.padding(top = hudHeight))
        UltimateButton(
            world = world,
            frame = frameTick,
            modifier = Modifier
                .align(if (ultimateOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(20.dp),
        )

        StageOverlays(
            world = world,
            frame = frameTick,
            bestScore = bestScore,
            hasNextStage = Stages.get(stageNumber + 1) != null,
            onNextStage = onNextStage,
            onRestart = {
                attempt++
                paused = false
            },
            onExit = onExit,
        )

        if (paused) {
            PauseMenu(
                onResume = { paused = false },
                onRestart = {
                    attempt++
                    paused = false
                },
                onExit = onExit,
            )
        }
    }
}

private fun applyJoystick(world: World, joystick: Joystick, radius: Float) {
    if (!joystick.active) {
        world.moveX = 0f
        world.moveY = 0f
        world.firing = false
        return
    }
    var dx = (joystick.knobX - joystick.baseX) / radius
    var dy = (joystick.knobY - joystick.baseY) / radius
    val length = hypot(dx, dy)
    if (length > 1f) {
        dx /= length
        dy /= length
    }
    if (length < 0.12f) {
        dx = 0f
        dy = 0f
    }
    world.moveX = dx
    world.moveY = dy
    world.firing = true
}

private val HUD_HEIGHT = 104.dp

private val random = Random(0)

private fun playCues(world: World, sound: SoundManager) {
    for (cue in world.cues) {
        when (cue) {
            SoundCue.SHOT -> sound.play(Sfx.SHOT, 0.35f, 0.95f + random.nextFloat() * 0.1f)
            SoundCue.SHOT_HEAVY -> sound.play(Sfx.SHOT_HEAVY, 0.5f)
            SoundCue.ENEMY_HIT -> sound.play(Sfx.ENEMY_HIT, 0.4f, 0.9f + random.nextFloat() * 0.2f)
            SoundCue.EXPLODE_SMALL -> sound.play(Sfx.EXPLODE_SMALL, 0.7f, 0.9f + random.nextFloat() * 0.25f)
            SoundCue.EXPLODE_BIG -> sound.play(Sfx.EXPLODE_BIG)
            SoundCue.ITEM -> sound.play(Sfx.ITEM, 0.6f)
            SoundCue.POWER_UP -> sound.play(Sfx.POWER_UP)
            SoundCue.PLAYER_HIT -> sound.play(Sfx.PLAYER_HIT)
            SoundCue.ROAR -> sound.play(if (world.player.cat == CatType.ORANGE) Sfx.ROAR_TIGER else Sfx.ROAR_PANTHER)
            SoundCue.BOMB_BLAST -> sound.play(Sfx.BLAST)
            SoundCue.WARNING -> sound.play(Sfx.WARNING)
            SoundCue.BOSS_DOWN -> sound.play(Sfx.BOSS_DOWN)
            SoundCue.SONIC -> sound.play(Sfx.SONIC, 0.6f)
            SoundCue.MISSILE -> sound.play(Sfx.MISSILE, 0.4f)
            SoundCue.LASER -> sound.play(Sfx.LASER, 0.7f)
        }
    }
    world.cues.clear()
}

private fun musicFor(world: World): Music = when (world.phase) {
    Phase.WARNING, Phase.BOSS_DIALOG, Phase.BOSS, Phase.BOSS_DEFEATED -> Music.BOSS
    Phase.PLAYER_DOWN, Phase.CONTINUE -> if (world.boss != null) Music.BOSS else stageMusic(world)
    else -> stageMusic(world)
}

private fun stageMusic(world: World): Music = when (world.stage.theme) {
    StageTheme.CHEESE_BARN -> Music.STAGE_MOUSE
    StageTheme.PARK -> Music.STAGE_DOG
    StageTheme.HONEY_FOREST -> Music.STAGE_BEAR
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJoystick(j: Joystick, radius: Float, idleX: Float, idleY: Float) {
    val bx = if (j.active) j.baseX else idleX
    val by = if (j.active) j.baseY else idleY
    var kx = if (j.active) j.knobX - bx else 0f
    var ky = if (j.active) j.knobY - by else 0f
    val length = hypot(kx, ky)
    if (length > radius) {
        kx = kx / length * radius
        ky = ky / length * radius
    }
    val alpha = if (j.active) 0.55f else 0.25f
    drawCircle(Color.White.copy(alpha = alpha * 0.35f), radius * 1.15f, Offset(bx, by))
    drawCircle(Color.White.copy(alpha = alpha), radius * 1.15f, Offset(bx, by), style = Stroke(4f))
    drawCircle(Color(0xFFFFD54F).copy(alpha = alpha + 0.2f), radius * 0.48f, Offset(bx + kx, by + ky))
    drawCircle(Color.White.copy(alpha = alpha + 0.2f), radius * 0.2f, Offset(bx + kx - radius * 0.12f, by + ky - radius * 0.12f))
}

// ───────────── HUD ─────────────

@Composable
internal fun ShooterHud(world: World, frame: Long, modifier: Modifier = Modifier, onPause: () -> Unit) {
    frame.let { }
    val p = world.player
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1033), Color(0xFF2D1B4E))))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "STAGE ${world.stage.number}　${world.stage.title}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("SCORE ${world.score.toString().padStart(7, '0')}", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .pointerInput(Unit) { detectTapGestures { onPause() } },
                contentAlignment = Alignment.Center,
            ) { Text("⏸", fontSize = 20.sp, color = Color.White) }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gauge("護甲", p.armorRatio, Color(0xFFFF5252), Modifier.weight(1f))
            Gauge("速度", p.speed / p.cat.baseSpeed, Color(0xFF40C4FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("${p.mainWeapon.label} Lv${p.mainLevel}", Color(0xFFFF9100))
            if (p.subWeapon != SubWeapon.NONE) Chip("${p.subWeapon.label} Lv${p.subLevel}", Color(0xFF7C4DFF))
        }
    }
}

/** Boss 血條，疊在遊戲畫面最上方。 */
@Composable
internal fun BossBar(world: World, frame: Long, modifier: Modifier = Modifier) {
    frame.let { }
    val boss = world.boss ?: return
    if (boss.entering || (world.phase != Phase.BOSS && world.phase != Phase.BOSS_DEFEATED)) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(boss.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(boss.hpRatio)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF1744)),
            )
        }
    }
}

@Composable
private fun Gauge(label: String, value: Float, color: Color, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
internal fun UltimateButton(world: World, frame: Long, modifier: Modifier) {
    frame.let { }
    val p = world.player
    val ready = p.bombs > 0
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(if (ready) Color(0xFFFF6D00).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.4f))
                .border(3.dp, Color.White.copy(alpha = if (ready) 0.9f else 0.3f), CircleShape)
                .pointerInput(world) { detectTapGestures(onPress = { world.requestUltimate() }) },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (p.cat == CatType.ORANGE) "🐯" else "🐆", fontSize = 38.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (i < p.bombs) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.25f)),
                )
            }
        }
    }
}
