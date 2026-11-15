package com.purpal.catplayground.divine.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Music
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.audio.SoundManager
import com.purpal.catplayground.divine.model.Battle
import com.purpal.catplayground.divine.model.BattlePhase
import com.purpal.catplayground.divine.model.CardDef
import com.purpal.catplayground.divine.model.DivineSave
import com.purpal.catplayground.divine.model.Fx
import com.purpal.catplayground.divine.model.FxType
import com.purpal.catplayground.divine.model.StageDef
import com.purpal.catplayground.divine.model.Target
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** 給 Compose 重組用的快照：只有會影響介面排版的東西，變了才重組。 */
private data class BattleHud(
    val hand: List<CardDef> = emptyList(),
    val incense: Int = 0,
    val maxIncense: Int = 0,
    val templeHp: Int = Battle.TEMPLE_HP,
    val phase: BattlePhase = BattlePhase.PLAYING,
    val deck: Int = 0,
    val discard: Int = 0,
    val remaining: Int = 0,
    val bossHp: Int = -1,
) {
    companion object {
        fun of(b: Battle): BattleHud {
            val boss = b.enemies.firstOrNull { it.isBoss }
            return BattleHud(
                b.hand.toList(), b.incense, b.maxIncense, b.templeHp, b.phase, b.drawPile.size, b.discard.size, b.remainingSpawns,
                if (boss != null) (boss.hp / boss.maxHp * 100f).roundToInt().coerceIn(0, 100) else -1,
            )
        }
    }
}

/** 拖牌的狀態（指標位置是整個畫面的像素座標）。 */
private class Drag(val index: Int, val card: CardDef, val position: Offset, val moved: Boolean)

/** 畫面的排版：戰場畫在哪、縮放多少、手牌區從哪裡開始。pointerInput 與繪圖共用。 */
private class Layout {
    var scale = 1f
    var ox = 0f
    var oy = 0f
    var handTop = 0f
    var cardW = 0f
    var cardH = 0f
    var handLeft = 0f
    var handStep = 0f
    var handCount = 0

    fun worldX(px: Float) = (px - ox) / scale
    fun worldY(py: Float) = (py - oy) / scale

    fun handIndexAt(p: Offset): Int {
        if (p.y < handTop || handCount == 0) return -1
        // 右邊的牌疊在上面，所以從右往左找
        for (i in handCount - 1 downTo 0) {
            val x = handLeft + i * handStep
            if (p.x >= x && p.x <= x + cardW) return i
        }
        return -1
    }
}

/**
 * 一場戰鬥：上面是戰場，下面是手牌。按住手牌可以看大圖，往上拖到戰場放開就打出去。
 * [onWin] 回傳這次拿到的金幣（由外面存檔）。
 */
@Composable
fun DivineBattleScreen(
    stage: StageDef, deck: List<CardDef>, hasNext: Boolean, onWin: (stars: Int) -> Int, onNext: () -> Unit, onExit: () -> Unit,
    /** 畫面自動測試：略過開場說明，由 [DivineBot] 出牌。 */
    autoplay: Boolean = false,
) {
    val sound = LocalSound.current
    val context = LocalContext.current
    val density = LocalDensity.current
    var attempt by remember { mutableIntStateOf(0) }
    val battle = remember(attempt) { Battle(stage, deck, seed = stage.number * 31 + attempt) }
    val fx = remember(attempt) { DivineFx() }
    val sprites = remember { DivineSprites(context) }
    val layout = remember { Layout() }
    var hud by remember(attempt) { mutableStateOf(BattleHud.of(battle)) }
    var frame by remember { mutableIntStateOf(0) }
    var drag by remember(attempt) { mutableStateOf<Drag?>(null) }
    var paused by remember(attempt) { mutableStateOf(false) }
    var intro by remember(attempt) { mutableStateOf(!autoplay) }
    val botThink = remember(attempt) { floatArrayOf(0f) }
    var fast by remember { mutableStateOf(false) }
    var reward by remember(attempt) { mutableIntStateOf(-1) }
    var toast by remember(attempt) { mutableStateOf("") }
    var toastAt by remember(attempt) { mutableStateOf(0f) }
    val clock = remember(attempt) { floatArrayOf(0f) }

    LaunchedEffect(stage.number) { sound.playMusic(if (stage.boss) Music.BOSS else Music.STAGE_RACCOON) }
    BackHandler(enabled = hud.phase == BattlePhase.PLAYING) { paused = !paused }

    LaunchedEffect(attempt) {
        var last = withFrameNanos { it }
        var acc = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            clock[0] += dt
            if (!paused && !intro) {
                acc += dt * (if (fast) 2f else 1f)
                while (acc >= STEP) {
                    acc -= STEP
                    battle.update(STEP)
                    if (autoplay) {
                        botThink[0] -= STEP
                        if (botThink[0] <= 0f) {
                            botThink[0] = 0.6f
                            com.purpal.catplayground.divine.model.DivineBot.playOne(battle)
                        }
                    }
                    playSounds(battle.events, sound)
                    fx.consume(battle.events)
                    battle.events.clear()
                    fx.update(STEP)
                }
                val next = BattleHud.of(battle)
                if (next != hud) {
                    if (next.phase != hud.phase) {
                        if (next.phase == BattlePhase.WON) {
                            sound.play(Sfx.WIN)
                            reward = onWin(DivineSave.starsFor(battle.templeHp))
                        } else if (next.phase == BattlePhase.LOST) {
                            sound.play(Sfx.BOSS_DOWN)
                        }
                        drag = null
                    }
                    hud = next
                }
            }
            frame++
        }
    }

    fun hintFor(d: Drag): DropHint? {
        if (d.position.y >= layout.handTop) return null
        val wx = layout.worldX(d.position.x)
        val wy = layout.worldY(d.position.y)
        if (wx < 0f || wx > Battle.WIDTH) return null
        val lane = (wx / Battle.LANE_WIDTH).toInt().coerceIn(0, Battle.LANES - 1)
        val card = d.card
        val nearest = (0 until Battle.ROWS).minByOrNull { abs(Battle.ROW_Y[it] - wy) } ?: 0
        val row = when (card.target) {
            Target.SLOT -> if (wy >= SLOT_ZONE) nearest else {
                // 拖到車道上半段：自動挑一格（近戰往前排、遠程往後排）
                val order = if (card.unit!!.ranged) listOf(2, 1, 0) else listOf(0, 1, 2)
                order.firstOrNull { battle.slots[lane][it] == null } ?: order.first()
            }
            Target.CREATURE -> if (battle.slots[lane][nearest] != null) nearest else
                (0 until Battle.ROWS).filter { battle.slots[lane][it] != null }.minByOrNull { abs(Battle.ROW_Y[it] - wy) } ?: nearest
            else -> 0
        }
        return DropHint(card, lane, row, battle.canAfford(card) && battle.validTarget(card, lane, row))
    }

    fun release(d: Drag) {
        val hint = hintFor(d) ?: return
        val index = battle.hand.indexOf(d.card).takeIf { battle.hand.getOrNull(d.index) !== d.card } ?: d.index
        if (hint.valid && battle.play(index, hint.lane, hint.row)) {
            hud = BattleHud.of(battle)
        } else {
            sound.play(Sfx.INVALID)
            toast = if (!battle.canAfford(d.card)) "香火不夠（需要 ${d.card.cost}）" else when (d.card.target) {
                Target.CREATURE -> "要拖到一隻神靈身上"
                else -> "這裡不能放"
            }
            toastAt = clock[0]
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(DivineColors.Night)
            .systemBarsPadding(),
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val px = with(density) { 1.dp.toPx() }
        val hudH = 58f * px
        val stripH = 40f * px
        // 手牌：最多 7 張，放不下就互相疊
        val cardW = minOf(w / 4.6f, 104f * px)
        val cardH = cardW * 1.32f
        val handH = cardH + 16f * px
        val handTop = h - handH
        val fieldH = handTop - stripH - hudH
        val scale = minOf(w / Battle.WIDTH, fieldH / Battle.HEIGHT)
        layout.scale = scale
        layout.ox = (w - Battle.WIDTH * scale) / 2f
        layout.oy = hudH + (fieldH - Battle.HEIGHT * scale)
        layout.handTop = handTop
        layout.cardW = cardW
        layout.cardH = cardH
        val n = hud.hand.size
        val step = if (n <= 1) 0f else minOf(cardW + 4f * px, (w - 12f * px - cardW) / (n - 1))
        layout.handStep = step
        layout.handCount = n
        layout.handLeft = (w - (cardW + step * (n - 1).coerceAtLeast(0))) / 2f

        // ── 戰場 ──
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(attempt) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (battle.phase != BattlePhase.PLAYING || paused || intro) return@awaitEachGesture
                        val index = layout.handIndexAt(down.position)
                        val card = battle.hand.getOrNull(index) ?: return@awaitEachGesture
                        sound.play(Sfx.SELECT)
                        var current = Drag(index, card, down.position, false)
                        drag = current
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            current = Drag(index, card, change.position, current.moved || (change.position - down.position).getDistance() > 12f * px)
                            drag = current
                        }
                        drag = null
                        if (battle.phase == BattlePhase.PLAYING) release(current)
                    }
                },
        ) {
            @Suppress("UNUSED_EXPRESSION") frame
            val time = clock[0]
            val shake = fx.shake
            val sx = if (shake > 0f) kotlin.math.sin(time * 90f) * shake * scale else 0f
            val sy = if (shake > 0f) kotlin.math.cos(time * 70f) * shake * scale else 0f
            // 戰場上方多出來的空間：接著鬼門往上延伸的夜空
            drawRect(Brush.verticalGradient(listOf(Color(0xFF05020F), Color(0xFF150B2E)), 0f, layout.oy.coerceAtLeast(1f)), size = androidx.compose.ui.geometry.Size(w, layout.oy.coerceAtLeast(0f) + 2f))
            withTransform({
                translate(layout.ox + sx, layout.oy + sy)
                scale(scale, scale, Offset.Zero)
            }) {
                drawBattle(battle, fx, sprites, time, drag?.let { hintFor(it) })
            }
            if (fx.flash > 0f) drawRect(fx.flashColor.copy(alpha = fx.flash * 0.5f), blendMode = BlendMode.Plus)
            // 神殿快被攻破時，畫面邊緣泛紅
            if (battle.templeHp <= 6 && battle.phase == BattlePhase.PLAYING) {
                val a = 0.25f + kotlin.math.sin(time * 5f) * 0.12f
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFFFF2A2A).copy(alpha = a)), center, size.maxDimension * 0.75f))
            }
        }

        // ── 上方資訊 ──
        TopBar(stage, hud, fast, Modifier.height(with(density) { hudH.toDp() }), onPause = { paused = true }, onFast = { fast = !fast })

        // ── 香火列 ──
        IncenseStrip(
            hud, battle, frame,
            Modifier
                .offset { IntOffset(0, (handTop - stripH).roundToInt()) }
                .fillMaxWidth()
                .height(with(density) { stripH.toDp() }),
        )

        // ── 手牌 ──
        Box(
            Modifier
                .offset { IntOffset(0, handTop.roundToInt()) }
                .fillMaxWidth()
                .height(with(density) { handH.toDp() })
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1A52), Color(0xFF120A26)))),
        )
        val dragging = drag
        hud.hand.forEachIndexed { i, card ->
            val held = dragging != null && dragging.index == i && dragging.card === card
            val inField = held && dragging!!.position.y < handTop
            val affordable = card.cost <= hud.incense
            val x = layout.handLeft + i * step
            DivineCardView(
                card,
                Modifier
                    .zIndex(if (held) 20f else i.toFloat())
                    .offset {
                        if (inField) IntOffset((dragging!!.position.x - cardW * 0.4f).roundToInt(), (dragging.position.y - cardH * 1.05f).roundToInt())
                        else IntOffset(x.roundToInt(), (handTop + 8f * px - (if (held) 18f * px else if (affordable) 6f * px else 0f)).roundToInt())
                    }
                    .width(with(density) { cardW.toDp() })
                    .graphicsLayer {
                        val s = if (inField) 0.8f else if (held) 1.06f else 1f
                        scaleX = s
                        scaleY = s
                        alpha = if (inField) 0.92f else 1f
                    },
                compact = true,
                dim = !affordable,
                highlight = affordable,
            )
        }

        // ── 按住手牌：放大看說明 ──
        if (dragging != null && dragging.position.y >= handTop) {
            val bigW = minOf(w * 0.56f, 250f * px)
            DivineCardView(
                dragging.card,
                Modifier
                    .zIndex(30f)
                    .offset { IntOffset(((w - bigW) / 2f).roundToInt(), (handTop - stripH - bigW * 1.42f - 10f * px).roundToInt()) }
                    .width(with(density) { bigW.toDp() }),
            )
        }

        // ── 提示訊息 ──
        if (toast.isNotEmpty() && clock[0] - toastAt < 1.4f) {
            @Suppress("UNUSED_EXPRESSION") frame
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xDD1A1030))
                    .border(2.dp, Color(0xFFFF7A7A), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) { Text(toast, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }

        // ── 開場說明、暫停、結算 ──
        when {
            intro -> Overlay {
                FitText("第 ${stage.number} 關", color = DivineColors.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                FitText(stage.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                Text(stage.intro, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("每 5 秒一拍：香火回滿、上限 +1，並抽一張牌。", color = Color(0xFFFFD9A0), fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                BigButton("⚔️ 開始降魔", DivineColors.Gold, PlayColors.Night) { intro = false }
            }
            hud.phase == BattlePhase.WON -> Overlay {
                val stars = DivineSave.starsFor(hud.templeHp)
                FitText("降魔成功！", color = DivineColors.Gold, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..3) ArtImage(if (i <= stars) "dv_star_1" else "dv_star_0", Modifier.size(56.dp))
                }
                FitText("神殿剩下 ${hud.templeHp} / ${Battle.TEMPLE_HP}", color = Color.White, fontSize = 15.sp)
                if (reward > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        ArtImage("dv_coin", Modifier.size(30.dp))
                        Spacer(Modifier.width(6.dp))
                        FitText("+$reward 金幣", color = DivineColors.Gold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (hasNext) BigButton("下一關 ▶", DivineColors.Gold, PlayColors.Night, onNext)
                Spacer(Modifier.height(8.dp))
                BigButton("回到神殿", Color(0xFF3949AB), Color.White, onExit)
            }
            hud.phase == BattlePhase.LOST -> Overlay {
                FitText("神殿被攻破了…", color = Color(0xFFFF8A8A), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("換幾張牌、調整站位再試一次吧。前排要擋得住，後排才打得安心。", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                BigButton("🔁 再挑戰一次", DivineColors.Gold, PlayColors.Night) { attempt++ }
                Spacer(Modifier.height(8.dp))
                BigButton("回到神殿", Color(0xFF3949AB), Color.White, onExit)
            }
            paused -> Overlay {
                FitText("暫停", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                BigButton("繼續", DivineColors.Gold, PlayColors.Night) { paused = false }
                Spacer(Modifier.height(8.dp))
                BigButton("🔁 重新開始", Color(0xFF6A3AB8), Color.White) { attempt++ }
                Spacer(Modifier.height(8.dp))
                BigButton("離開", Color(0xFF3949AB), Color.White, onExit)
            }
        }
    }
}

private const val STEP = 1f / 60f
/** 拖到這條線以下才算指定格子；以上只算指定車道。 */
private const val SLOT_ZONE = 790f

@Composable
private fun TopBar(stage: StageDef, hud: BattleHud, fast: Boolean, modifier: Modifier, onPause: () -> Unit, onFast: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xEE0A0518), Color(0x660A0518))))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            FitText("第 ${stage.number} 關　${stage.title}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (hud.bossHp >= 0) {
                Gauge("九尾妖狐", hud.bossHp / 100f, listOf(Color(0xFFFF7ACD), Color(0xFFB03AE0)))
            } else {
                FitText(if (hud.remaining > 0) "還有 ${hud.remaining} 隻妖魔沒出現" else "最後一波！", color = Color(0xFFD0B8FF), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.width(120.dp)) {
            Gauge("神殿 ${hud.templeHp}", hud.templeHp / Battle.TEMPLE_HP.toFloat(), listOf(Color(0xFFFFE27A), Color(0xFFFF8A3A)))
        }
        Spacer(Modifier.width(8.dp))
        PillButton(if (fast) "×2" else "×1", background = if (fast) DivineColors.Gold else PlayColors.Card, contentColor = if (fast) PlayColors.Night else Color.White) { onFast() }
        Spacer(Modifier.width(6.dp))
        PillButton("⏸") { onPause() }
    }
}

@Composable
private fun Gauge(label: String, value: Float, colors: List<Color>) {
    Column {
        FitText(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(9.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(colors)))
        }
    }
}

/** 香火（10 顆珠子）、節拍進度、牌庫張數。 */
@Composable
private fun IncenseStrip(hud: BattleHud, battle: Battle, frame: Int, modifier: Modifier) {
    Row(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF3A2466), Color(0xFF2A1A52))))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtImage("dv_incense", Modifier.size(34.dp))
        Spacer(Modifier.width(4.dp))
        FitText("${hud.incense}/${hud.maxIncense}", color = DivineColors.Gold, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 0 until Battle.MAX_INCENSE) {
                val color = when {
                    i < hud.incense -> Brush.radialGradient(listOf(Color(0xFFFFF3A0), DivineColors.Incense))
                    i < hud.maxIncense -> Brush.radialGradient(listOf(Color(0xFF6A4A3A), Color(0xFF3A2A2A)))
                    else -> Brush.radialGradient(listOf(Color(0xFF2A2140), Color(0xFF1A1230)))
                }
                Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(50)).background(color))
            }
        }
        Spacer(Modifier.width(8.dp))
        // 節拍：一圈跑完就是下一拍
        Canvas(Modifier.size(30.dp)) {
            @Suppress("UNUSED_EXPRESSION") frame
            val p = battle.beatTime / Battle.BEAT
            drawCircle(Color.White.copy(alpha = 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
            drawArc(DivineColors.Gold, -90f, 360f * p, false, style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FitText("牌庫 ${hud.deck}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FitText("棄牌 ${hud.discard}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(Color(0xB30A0518))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(26.dp)
        Column(
            Modifier
                .padding(28.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF4A2E9A), Color(0xFF1E1250))))
                .border(3.dp, DivineColors.Gold, shape)
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
internal fun BigButton(text: String, background: Color, contentColor: Color, onClick: () -> Unit) {
    val sound = LocalSound.current
    Box(
        Modifier
            .width(230.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable {
                sound.play(Sfx.CLICK)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { FitText(text, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp)) }
}

/** 把這一步的事件換成音效；同一種音效一步只播一次。 */
private fun playSounds(events: List<Fx>, sound: SoundManager) {
    if (events.isEmpty()) return
    var played = 0
    fun once(bit: Int, sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (played and (1 shl bit) == 0) {
            played = played or (1 shl bit)
            sound.play(sfx, volume, rate)
        }
    }
    for (e in events) when (e.type) {
        FxType.SUMMON -> once(0, Sfx.POWER_UP)
        FxType.SPELL -> once(1, if (e.id == "olympus" || e.id == "zeus") Sfx.BLAST else Sfx.SONIC)
        FxType.ENCHANT -> once(2, Sfx.SONIC, rate = 0.8f)
        FxType.EQUIP -> once(3, Sfx.ITEM)
        FxType.SLASH -> once(4, Sfx.ENEMY_HIT, 0.5f)
        FxType.SHOT_HIT -> once(5, Sfx.ENEMY_HIT, 0.35f, 1.4f)
        FxType.BOLT -> once(6, Sfx.LASER, 0.8f)
        FxType.ENEMY_DIE -> if (e.id == "ninetail") once(7, Sfx.BOSS_DOWN) else once(8, Sfx.EXPLODE_SMALL, 0.6f)
        FxType.CREATURE_DIE -> once(9, Sfx.PLAYER_HIT)
        FxType.BREACH -> once(10, Sfx.EXPLODE_BIG)
        FxType.BEAT -> once(11, Sfx.SELECT, 0.5f, 0.7f)
        FxType.HEAL -> once(12, Sfx.ITEM, 0.4f, 1.3f)
        FxType.TELEGRAPH -> once(13, Sfx.WARNING)
        FxType.FOXFIRE -> once(14, Sfx.EXPLODE_BIG, 0.8f)
        FxType.BOSS_APPEAR -> once(15, Sfx.ROAR_PANTHER)
        FxType.REVIVE -> once(16, Sfx.RANK_UP)
        else -> Unit
    }
}
