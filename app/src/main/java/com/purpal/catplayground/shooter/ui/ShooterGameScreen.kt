package com.purpal.catplayground.shooter.ui

import androidx.activity.compose.BackHandler
import com.purpal.catplayground.nova.drawNovaOverdrive
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.Loadout
import com.purpal.catplayground.shooter.model.MainWeapon
import com.purpal.catplayground.shooter.model.Phase
import com.purpal.catplayground.shooter.model.STEP
import com.purpal.catplayground.shooter.model.SoundCue
import com.purpal.catplayground.shooter.model.StageTheme
import com.purpal.catplayground.shooter.model.Stages
import com.purpal.catplayground.shooter.model.Difficulty
import com.purpal.catplayground.shooter.model.Tour
import com.purpal.catplayground.shooter.model.SubWeapon
import com.purpal.catplayground.shooter.model.WORLD_WIDTH
import com.purpal.catplayground.shooter.model.World
import com.purpal.catplayground.shooter.render.SpriteCache
import com.purpal.catplayground.shooter.render.drawUltimateCinematic
import com.purpal.catplayground.shooter.render.drawWorld
import com.purpal.catplayground.shooter.render.viewportFor
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** 觸控板式操作：記錄手指位置與尚未套用的移動量（螢幕像素）。 */
private class TouchPad {
    var pointer: PointerId? = null
    var x = 0f
    var y = 0f
    var pendingDx = 0f
    var pendingDy = 0f
    val active: Boolean get() = pointer != null
}

/** HUD 需要的數值；只有數值改變時才觸發重組，避免每幀重組造成卡頓。 */
@Immutable
data class HudState(
    val phase: Phase = Phase.INTRO,
    val score: Int = 0,
    val armor: Int = 100,
    val speed: Int = 100,
    val mainWeapon: MainWeapon = MainWeapon.SPREAD,
    val mainLevel: Int = 1,
    val subWeapon: SubWeapon = SubWeapon.NONE,
    val subLevel: Int = 0,
    val bombs: Int = 0,
    val bossName: String? = null,
    val bossHp: Int = 0,
    val continuesLeft: Int = 0,
) {
    companion object {
        fun of(w: World): HudState {
            val p = w.player
            val boss = w.boss?.takeIf { !it.entering && (w.phase == Phase.BOSS || w.phase == Phase.BOSS_DEFEATED) }
            return HudState(
                phase = w.phase,
                score = w.score,
                armor = (p.armorRatio * 100).toInt(),
                speed = (p.speed / p.cat.baseSpeed * 100).toInt(),
                mainWeapon = p.mainWeapon,
                mainLevel = p.mainLevel,
                subWeapon = p.subWeapon,
                subLevel = p.subLevel,
                bombs = p.bombs,
                bossName = boss?.name,
                bossHp = ((boss?.hpRatio ?: 0f) * 200).toInt(),
                continuesLeft = w.continuesLeft,
            )
        }
    }
}

@Composable
fun ShooterGameScreen(
    stageNumber: Int,
    cat: CatType,
    autotest: Boolean = false,
    nova: Boolean = false,
    novaBossHp: Float? = null,
    novaOutro: Boolean = false,
    ultimateOnLeft: Boolean,
    bestScore: Int,
    loadout: Loadout? = null,
    /** 世界巡迴的第幾站（從 0 算），決定強度；null 表示照關卡的設定值打。 */
    bout: Int? = null,
    onCleared: (stage: Int, score: Int, loadout: Loadout) -> Unit,
    onNextStage: () -> Unit,
    onExit: () -> Unit,
) {
    var novaReady by remember { mutableStateOf(!nova) }
    val stage = remember(stageNumber, nova) {
        val original = Stages.get(stageNumber)!!
        if (nova) com.purpal.catplayground.nova.NovaEncounters.stage(original) else original
    }
    val difficulty = remember(bout) { bout?.let { Difficulty.forBout(it) } ?: Difficulty.NEUTRAL }
    val boutLabel = if (bout != null) { if(nova) "第 ${bout+1} 站" else boutTitle(bout) } else "STAGE ${stage.number}"
    val sound = LocalSound.current
    var attempt by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    var hud by remember { mutableStateOf(HudState()) }
    val touch = remember { TouchPad() }
    val textMeasurer = rememberTextMeasurer()

    BackHandler { paused = !paused }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) paused = true }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // HUD 的高度依實際字體量一次（之後內容結構固定、高度不變），量好之前先不開始遊戲。
    var hudPx by remember { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val density = LocalDensity.current
        if (hudPx == 0) {
            if (nova) com.purpal.catplayground.nova.NovaHud(hud,boutLabel,stage.title,Modifier.onSizeChanged { if(it.height>0)hudPx=it.height },onPause={})
            else ShooterHud(hud, boutLabel, stage.title, Modifier.onSizeChanged { if (it.height > 0) hudPx = it.height }, onPause = {})
            return@BoxWithConstraints
        }
        // 上方是 HUD，下方才是遊戲畫面，避免 HUD 擋住 Boss。
        val hudHeight = with(density) { hudPx.toDp() }
        val gameHeightPx = (constraints.maxHeight - hudPx).toFloat()
        val worldHeight = (WORLD_WIDTH * gameHeightPx / constraints.maxWidth.toFloat()).coerceIn(1400f, 2300f)
        val pxPerUnit = minOf(constraints.maxWidth / WORLD_WIDTH, gameHeightPx / worldHeight)
        val context = LocalContext.current
        val sprites = remember(pxPerUnit,nova,stageNumber) { SpriteCache(pxPerUnit, context.assets, if(nova)com.purpal.catplayground.nova.NovaAssets.get(context.assets) else null, stageNumber) }
        val world = remember(attempt, stageNumber, worldHeight) {
            // 巡迴中重新開始這一站時，一樣從打完上一站時的機體狀態開始。
            World(
                stage, cat, worldHeight, seed = stageNumber * 31 + attempt, loadout = if (attempt == 0 || bout != null) loadout else null,
                difficulty = difficulty, bout = if (bout != null) bout + 1 else stage.number,
            )
                .also {
                    if(nova && novaOutro) com.purpal.catplayground.nova.NovaPreview.outro(it)
                    if(nova && novaBossHp!=null) com.purpal.catplayground.nova.NovaPreview.boss(it,novaBossHp)
                    it.godMode = autotest || (nova && novaBossHp!=null)
                    // 自動測試直接拿專武，好在截圖裡看到專武特效
                    if (autotest) {
                        it.player.mainWeapon = com.purpal.catplayground.shooter.model.MainWeapon.SPECIAL
                        it.player.mainLevel = 3
                    }
                }
        }

        LaunchedEffect(sprites, cat, stageNumber) {
            withContext(Dispatchers.Default) { sprites.prewarm(cat, kindsFor(stageNumber)) }
            novaReady = true
        }

        LaunchedEffect(world) {
            var last = withFrameNanos { it }
            var accumulator = 0f
            var lastPhase = world.phase
            val perf = if (autotest) AutotestProbe() else null
            while (true) {
                val now = withFrameNanos { it }
                perf?.frame((now - last) / 1_000_000f, world)
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                if (!paused && world.running && novaReady) {
                    // 手指移動量直接換算成世界座標，第一個物理步就套用，跟手不延遲。
                    world.drag(touch.pendingDx / pxPerUnit, touch.pendingDy / pxPerUnit)
                    world.firing = touch.active
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
                touch.pendingDx = 0f
                touch.pendingDy = 0f
                playCues(world, sound)
                sound.playMusic(musicFor(world))
                val next = HudState.of(world)
                if (next != hud) hud = next
                if (world.phase != lastPhase) {
                    if (world.phase == Phase.CLEAR) {
                        sound.play(Sfx.WIN)
                        onCleared(stageNumber, world.score, world.loadout())
                    }
                    lastPhase = world.phase
                }
                frame++
            }
        }

        // 遊戲畫面（只有這裡每幀重畫，不經過重組）
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(top = hudHeight),
        ) {
            if (frame < 0 || !novaReady) return@Canvas // 讀取 frame 讓畫布每幀重畫
            drawWorld(world, viewportFor(size, world.height), sprites)
            if (touch.active && world.ultimateCinematic <= 0f) {
                drawCircle(Color.White.copy(alpha = 0.18f), 46.dp.toPx(), Offset(touch.x, touch.y))
                drawCircle(Color.White.copy(alpha = 0.5f), 46.dp.toPx(), Offset(touch.x, touch.y), style = Stroke(3.dp.toPx()))
                drawCircle(Color(0xFFFFD54F).copy(alpha = 0.8f), 10.dp.toPx(), Offset(touch.x, touch.y))
            }
            if (nova) drawNovaOverdrive(world, sprites) else drawUltimateCinematic(world, sprites, textMeasurer)
        }

        // 觸控板：按住下半部任何地方開始射擊，手指移動多少，貓咪就跟著移動多少。
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = hudHeight)
                .pointerInput(world) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (touch.pointer == null && change.pressed && !change.previousPressed &&
                                    change.position.y > size.height * 0.3f
                                ) {
                                    touch.pointer = change.id
                                    touch.x = change.position.x
                                    touch.y = change.position.y
                                } else if (change.id == touch.pointer) {
                                    if (change.pressed) {
                                        touch.pendingDx += change.position.x - touch.x
                                        touch.pendingDy += change.position.y - touch.y
                                        touch.x = change.position.x
                                        touch.y = change.position.y
                                    } else {
                                        touch.pointer = null
                                    }
                                }
                            }
                        }
                    }
                },
        )

        if (nova) com.purpal.catplayground.nova.NovaHud(hud,boutLabel,stage.title,onPause={paused=true})
        else ShooterHud(hud, boutLabel, stage.title, onPause = { paused = true })
        BossBar(hud, Modifier.padding(top = hudHeight))
        UltimateButton(
            cat = cat,
            bombs = hud.bombs,
            onPress = { world.requestUltimate() },
            modifier = Modifier
                .align(if (ultimateOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(20.dp),
        )

        if (nova) {
            if (!novaReady) Box(Modifier.fillMaxSize().background(Color(0xFF07121F)),contentAlignment=Alignment.Center) { Text("艦載系統同步中…",color=Color(0xFF68E5F5)) }
            else com.purpal.catplayground.nova.NovaStageOverlays(world,hud.phase,bestScore,if(bout!=null)bout+1<Tour.SIZE else Stages.get(stageNumber+1)!=null,onNextStage,{attempt++;paused=false},onExit)
        } else StageOverlays(
            world = world,
            phase = hud.phase,
            bestScore = bestScore,
            hasNextStage = if (bout != null) bout + 1 < Tour.SIZE else Stages.get(stageNumber + 1) != null,
            onNextStage = onNextStage,
            onRestart = {
                attempt++
                paused = false
            },
            onExit = onExit,
        )

        if (paused) {
            if(nova) com.purpal.catplayground.nova.NovaPause({paused=false},{attempt++;paused=false},onExit)
            else PauseMenu(
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

private fun kindsFor(stage: Int): List<EnemyKind> = when (stage) {
    1 -> listOf(
        EnemyKind.MOUSE_RUNNER, EnemyKind.MINI_MOUSE, EnemyKind.MOUSE_PLANE, EnemyKind.CHEESE_DRONE, EnemyKind.BALLOON, EnemyKind.ELITE_MOUSE,
        EnemyKind.BOSS_MOUSE, EnemyKind.MOUSETRAP, EnemyKind.ROLLING_CHEESE,
    )
    2 -> listOf(
        EnemyKind.PUPPY, EnemyKind.FRISBEE, EnemyKind.BALL_TURRET, EnemyKind.MOUSE_RUNNER, EnemyKind.BALLOON,
        EnemyKind.ELITE_DOG, EnemyKind.ELITE_MOUSE, EnemyKind.BOSS_DOG, EnemyKind.THROWN_BONE,
    )
    3 -> listOf(
        EnemyKind.BEE, EnemyKind.HONEY_POT, EnemyKind.BEAR_CUB, EnemyKind.BEEHIVE, EnemyKind.CHEESE_DRONE, EnemyKind.PUPPY,
        EnemyKind.BALLOON, EnemyKind.ELITE_BEE, EnemyKind.ELITE_DOG, EnemyKind.BOSS_BEAR,
    )
    5 -> listOf(
        EnemyKind.BUNNY, EnemyKind.CARROT_TURRET, EnemyKind.MOUSE_PLANE, EnemyKind.PUPPY, EnemyKind.DUCKLING, EnemyKind.FRISBEE,
        EnemyKind.BABY_CAPY, EnemyKind.BALLOON, EnemyKind.ELITE_HARE, EnemyKind.ELITE_MONKEY, EnemyKind.BOSS_RABBIT,
    )
    6 -> listOf(
        EnemyKind.PENGUIN_SLIDER, EnemyKind.SNOWMAN, EnemyKind.MOUSE_PLANE, EnemyKind.YUZU, EnemyKind.DUCKLING, EnemyKind.FRISBEE,
        EnemyKind.BALL_TURRET, EnemyKind.BALLOON, EnemyKind.ELITE_SEAL, EnemyKind.ELITE_HARE, EnemyKind.BOSS_PENGUIN,
    )
    7 -> listOf(
        EnemyKind.LEAF_STAR, EnemyKind.OWL, EnemyKind.BEE, EnemyKind.BEAR_CUB, EnemyKind.BUNNY, EnemyKind.MOUSE_PLANE,
        EnemyKind.HONEY_POT, EnemyKind.BALLOON, EnemyKind.ELITE_KANGAROO, EnemyKind.ELITE_BEE, EnemyKind.BOSS_KOALA,
    )
    8 -> listOf(
        EnemyKind.LEAP_FISH, EnemyKind.CLAM, EnemyKind.DUCKLING, EnemyKind.BUCKET, EnemyKind.PENGUIN_SLIDER, EnemyKind.FRISBEE,
        EnemyKind.BABY_CAPY, EnemyKind.BALLOON, EnemyKind.ELITE_BEAVER, EnemyKind.ELITE_MONKEY, EnemyKind.BOSS_OTTER, EnemyKind.OTTER_TWIN,
    )
    9 -> listOf(
        EnemyKind.PANDA_CUB, EnemyKind.STEAMER, EnemyKind.LEAF_STAR, EnemyKind.BEAR_CUB, EnemyKind.BUNNY, EnemyKind.MOUSE_PLANE,
        EnemyKind.OWL, EnemyKind.BALLOON, EnemyKind.ELITE_RED_PANDA, EnemyKind.ELITE_KANGAROO, EnemyKind.BOSS_PANDA, EnemyKind.BAMBOO_SHOOT,
    )
    10 -> listOf(
        EnemyKind.TREE_FROG, EnemyKind.PITCHER, EnemyKind.LEAF_STAR, EnemyKind.OWL, EnemyKind.BEE, EnemyKind.BALLOON,
        EnemyKind.ELITE_TOUCAN, EnemyKind.ELITE_MONKEY, EnemyKind.BOSS_SLOTH,
    )
    11 -> listOf(
        EnemyKind.OPOSSUM, EnemyKind.TRASH_CAN, EnemyKind.BUNNY, EnemyKind.BALL_TURRET, EnemyKind.MOUSE_PLANE, EnemyKind.BALLOON,
        EnemyKind.ELITE_SKUNK, EnemyKind.ELITE_MOUSE, EnemyKind.BOSS_RACCOON, EnemyKind.LOOT_BAG,
    )
    12 -> listOf(
        EnemyKind.SWING_MONKEY, EnemyKind.TOTEM, EnemyKind.TREE_FROG, EnemyKind.BEAR_CUB, EnemyKind.FRISBEE, EnemyKind.BALLOON,
        EnemyKind.ELITE_GORILLA, EnemyKind.ELITE_TOUCAN, EnemyKind.BOSS_MONKEY,
    )
    13 -> listOf(
        EnemyKind.CHICK, EnemyKind.COW, EnemyKind.DUCKLING, EnemyKind.CARROT_TURRET, EnemyKind.FRISBEE, EnemyKind.BALLOON,
        EnemyKind.ELITE_WOLF, EnemyKind.ELITE_HARE, EnemyKind.BOSS_PIG,
    )
    else -> listOf(
        EnemyKind.DUCKLING, EnemyKind.YUZU, EnemyKind.BUCKET, EnemyKind.BABY_CAPY, EnemyKind.BALLOON,
        EnemyKind.ELITE_MONKEY, EnemyKind.ELITE_BEE, EnemyKind.BOSS_CAPY,
    )
}

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
            SoundCue.BEAM -> sound.play(Sfx.BEAM, 0.35f)
            SoundCue.DATA -> sound.play(Sfx.ITEM, 0.35f, 1.3f + random.nextFloat() * 0.3f)
            SoundCue.SWING -> sound.play(Sfx.SONIC, 0.7f, 0.6f)
            SoundCue.BREAK -> sound.play(Sfx.EXPLODE_BIG, 1f, 0.8f)
            SoundCue.ARMOR_UP -> sound.play(Sfx.POWER_UP, 1f, 0.7f)
        }
    }
    world.cues.clear()
}

private fun musicFor(world: World): Music = when (world.phase) {
    Phase.WARNING, Phase.BOSS, Phase.BOSS_DEFEATED -> Music.BOSS
    Phase.PLAYER_DOWN, Phase.CONTINUE -> if (world.boss != null) Music.BOSS else stageMusic(world)
    else -> stageMusic(world)
}

private fun stageMusic(world: World): Music = when (world.stage.theme) {
    StageTheme.CHEESE_BARN -> Music.STAGE_MOUSE
    StageTheme.PARK -> Music.STAGE_DOG
    StageTheme.HONEY_FOREST -> Music.STAGE_BEAR
    StageTheme.ONSEN -> Music.STAGE_CAPY
    StageTheme.CARROT_FIELD -> Music.STAGE_RABBIT
    StageTheme.ICE_FIELD -> Music.STAGE_PENGUIN
    StageTheme.EUCALYPTUS -> Music.STAGE_KOALA
    StageTheme.STREAM -> Music.STAGE_OTTER
    StageTheme.BAMBOO -> Music.STAGE_PANDA
    StageTheme.RAINFOREST -> Music.STAGE_SLOTH
    StageTheme.NIGHT_TOWN -> Music.STAGE_RACCOON
    StageTheme.RUINS -> Music.STAGE_MONKEY
    StageTheme.MUD_FARM -> Music.STAGE_PIG
}

// ───────────── HUD ─────────────

@Composable
internal fun ShooterHud(hud: HudState, stage: String, title: String, modifier: Modifier = Modifier, onPause: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1033), Color(0xFF2D1B4E))))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左邊：關卡、分數、護甲；速度不再顯示（兩隻貓的差別在判定點與特技（閃身步／金鐘罩），不在數值）。
            Column(Modifier.weight(1f)) {
                FitText("$stage　$title", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FitText("SCORE ${hud.score.toString().padStart(7, '0')}", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Gauge("護甲", hud.armor / 100f, Color(0xFFFF5252), Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(10.dp))
            // 主武器、副武器一樣大；副武器那一格永遠佔位（沒有副武器時透明），HUD 高度才不會變。
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip("${hud.mainWeapon.label} Lv${hud.mainLevel}", Color(0xFFFF9100))
                val hasSub = hud.subWeapon != SubWeapon.NONE
                Chip(if (hasSub) "${hud.subWeapon.label} Lv${hud.subLevel}" else "", Color(0xFF7C4DFF), visible = hasSub)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .pointerInput(Unit) { detectTapGestures { onPause() } },
                contentAlignment = Alignment.Center,
            ) { Text("⏸", fontSize = 20.sp, color = Color.White) }
        }
    }
}

/** Boss 血條，疊在遊戲畫面最上方。 */
@Composable
internal fun BossBar(hud: HudState, modifier: Modifier = Modifier) {
    val name = hud.bossName ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        FitText(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(max = 120.dp))
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
                    .fillMaxWidth(hud.bossHp / 200f)
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
        FitText(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
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

/** 武器等級標籤：固定大小，主副武器一樣大。 */
@Composable
private fun Chip(text: String, color: Color, visible: Boolean = true) {
    Box(
        Modifier
            .size(width = 112.dp, height = 24.dp)
            .background(if (visible) color.copy(alpha = 0.85f) else Color.Transparent, RoundedCornerShape(50))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (visible) FitText(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun UltimateButton(cat: CatType, bombs: Int, onPress: () -> Unit, modifier: Modifier) {
    val ready = bombs > 0
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            Modifier
                .size(59.dp)
                .pointerInput(onPress) { detectTapGestures(onPress = { onPress() }) },
            contentAlignment = Alignment.Center,
        ) {
            // 貓吼剪影按鈕：橘貓是橘金色、黑貓是紫色
            if (com.purpal.catplayground.DebugFlags.plainHud) {
                Box(
                    Modifier
                        .size(55.dp)
                        .background(if (ready) Color(0xFFFF6D00).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(if (cat == CatType.ORANGE) "🐯" else "🐈‍⬛", fontSize = 27.sp) }
            } else ArtImage(
                if (cat == CatType.ORANGE) "ult_btn_orange" else "ult_btn_black",
                Modifier.size(59.dp),
                contentDescription = cat.ultimateName,
                alpha = if (ready) 1f else 0.4f,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(12.dp)
                        .background(if (i < bombs) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.25f), CircleShape),
                )
            }
        }
    }
}
