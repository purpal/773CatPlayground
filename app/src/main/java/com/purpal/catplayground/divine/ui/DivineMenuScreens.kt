package com.purpal.catplayground.divine.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.divine.model.CardDef
import com.purpal.catplayground.divine.model.CardType
import com.purpal.catplayground.divine.model.Cards
import com.purpal.catplayground.divine.model.DivineSave
import com.purpal.catplayground.divine.model.DivineStages
import com.purpal.catplayground.divine.model.Faction
import com.purpal.catplayground.divine.model.PackResult
import com.purpal.catplayground.divine.model.StageDef
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 存檔：一行字串放在自己的 SharedPreferences 裡。 */
class DivineStore(context: Context) {
    private val prefs = context.getSharedPreferences("cat_playground_divine", Context.MODE_PRIVATE)

    fun load(): DivineSave = DivineSave.decode(prefs.getString("save", null)) ?: DivineSave.fresh()

    fun save(value: DivineSave) = prefs.edit().putString("save", value.encode()).apply()
}

private val Backdrop = Brush.verticalGradient(listOf(Color(0xFF5A2E8A), Color(0xFF2A1650), Color(0xFF0E0824)))

@Composable
private fun Header(title: String, subtitle: String, coins: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        PillButton("⬅") { onBack() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            FitText(subtitle, color = DivineColors.Gold, fontSize = 13.sp)
        }
        CoinChip(coins)
    }
}

@Composable
private fun CoinChip(coins: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.4f)).border(1.5.dp, DivineColors.Gold, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtImage("dv_coin", Modifier.size(24.dp))
        Spacer(Modifier.width(5.dp))
        FitText("$coins", color = DivineColors.Gold, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

// ───────────────────────── 神殿（主選單） ─────────────────────────

@Composable
fun DivineMenuScreen(save: DivineSave, onBack: () -> Unit, onPlay: (Int) -> Unit, onDeck: () -> Unit, onShop: () -> Unit) {
    val sound = LocalSound.current
    Box(Modifier.fillMaxSize().background(Backdrop)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Header("喵神封魔錄", "組牌請神・五路降魔", save.coins, onBack)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuTile("🃏 牌組", "${save.deck.size} / ${Cards.DECK_SIZE} 張", Modifier.weight(1f), warn = !save.deckReady, onClick = onDeck)
                MenuTile("🎁 抽卡", "一包 ${DivineSave.PACK_COST} 金幣", Modifier.weight(1f), warn = false, glow = save.coins >= DivineSave.PACK_COST, onClick = onShop)
            }
            Spacer(Modifier.height(14.dp))
            FitText("第一章　幽冥鬼域", color = DivineColors.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                for (stage in DivineStages.all) {
                    StageRow(stage, save.stars[stage.number] ?: 0, save.unlocked(stage.number)) {
                        if (!save.deckReady) {
                            sound.play(Sfx.INVALID)
                            onDeck()
                        } else {
                            sound.play(Sfx.SELECT)
                            onPlay(stage.number)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MenuTile(title: String, subtitle: String, modifier: Modifier, warn: Boolean, glow: Boolean = false, onClick: () -> Unit) {
    val sound = LocalSound.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF6A4AD0), Color(0xFF3A2488))))
            .border(2.dp, if (warn) Color(0xFFFF7A7A) else if (glow) DivineColors.Gold else Color.White.copy(alpha = 0.3f), shape)
            .clickable {
                sound.play(Sfx.SELECT)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        FitText(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        FitText(if (warn) "$subtitle（要湊滿才能出戰）" else subtitle, color = if (warn) Color(0xFFFFB0B0) else Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun StageRow(stage: StageDef, stars: Int, unlocked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (stage.boss) Brush.horizontalGradient(listOf(Color(0xFF8A1E5A), Color(0xFF3A1466))) else Brush.horizontalGradient(listOf(Color(0xFF3A2A7A), Color(0xFF241650))))
            .border(2.dp, if (stage.boss && unlocked) Color(0xFFFF7ACD) else Color.White.copy(alpha = if (unlocked) 0.3f else 0.1f), shape)
            .graphicsLayer { alpha = if (unlocked) 1f else 0.45f }
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            if (unlocked) ArtImage(if (stage.boss) "dv_foe_ninetail" else STAGE_ICONS[stage.number] ?: "dv_foe_imp", Modifier.fillMaxSize())
            else Text("🔒", fontSize = 28.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            FitText(if (stage.boss) "BOSS　${stage.title}" else "第 ${stage.number} 關　${stage.title}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            FitText(if (unlocked) (if (stars == 0) "首次過關 +${stage.firstClearCoins} 金幣" else "再次挑戰 +${DivineStages.REPLAY_COINS} 金幣") else "打倒前一關後開放", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Row { for (i in 1..3) ArtImage(if (i <= stars) "dv_star_1" else "dv_star_0", Modifier.size(24.dp)) }
    }
}

private val STAGE_ICONS = mapOf(1 to "dv_foe_imp", 2 to "dv_foe_zombie", 3 to "dv_foe_fox", 4 to "dv_foe_skeleton", 5 to "dv_foe_oxhead")

// ───────────────────────── 牌組 ─────────────────────────

private enum class DeckFilter(val label: String) { ALL("全部"), DECK("牌組中"), EAST("東方"), GREEK("希臘") }

@Composable
fun DivineDeckScreen(save: DivineSave, onChange: (DivineSave) -> Unit, onBack: () -> Unit) {
    val sound = LocalSound.current
    var filter by remember { mutableStateOf(DeckFilter.ALL) }
    var detail by remember { mutableStateOf<CardDef?>(null) }
    val cards = remember(filter, save.deck) {
        Cards.all.filter {
            when (filter) {
                DeckFilter.ALL -> true
                DeckFilter.DECK -> save.inDeck(it) > 0
                DeckFilter.EAST -> it.faction == Faction.EAST
                DeckFilter.GREEK -> it.faction == Faction.GREEK
            }
        }.sortedWith(compareBy({ it.cost }, { it.type.ordinal }, { it.name }))
    }
    Box(Modifier.fillMaxSize().background(Backdrop)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Header("牌組", "點卡片看說明、加進或移出牌組", save.coins, onBack)
            Spacer(Modifier.height(10.dp))
            // 張數與費用分布
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Column {
                    FitText("${save.deck.size} / ${Cards.DECK_SIZE}", color = if (save.deckReady) PlayColors.Mint else Color(0xFFFF8A8A), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    val deck = save.deckCards
                    FitText("生物 ${deck.count { it.type == CardType.CREATURE }}　其他 ${deck.count { it.type != CardType.CREATURE }}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Spacer(Modifier.width(14.dp))
                CostCurve(save.deckCards, Modifier.weight(1f).height(46.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (f in DeckFilter.entries) {
                    PillButton(f.label, background = if (filter == f) DivineColors.Gold else PlayColors.Card, contentColor = if (filter == f) PlayColors.Night else Color.White) { filter = f }
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cards, key = { it.id }) { card ->
                    val owned = save.count(card)
                    val inDeck = save.inDeck(card)
                    Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        sound.play(Sfx.SELECT)
                        detail = card
                    }) {
                        DivineCardView(card, Modifier.fillMaxWidth(), dim = owned == 0, highlight = inDeck > 0)
                        if (owned == 0) {
                            Box(Modifier.align(Alignment.Center).clip(RoundedCornerShape(50)).background(Color(0xCC0A0518)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                FitText("尚未獲得", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(2.dp).clip(RoundedCornerShape(50))
                                    .background(if (inDeck > 0) PlayColors.Mint else Color(0xCC0A0518)).padding(horizontal = 7.dp, vertical = 2.dp),
                            ) { FitText("$inDeck/${minOf(owned, card.rarity.maxCopies)}", color = if (inDeck > 0) PlayColors.Night else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
        val shown = detail
        if (shown != null) {
            CardDetail(shown, save, onClose = { detail = null }, onAdd = {
                sound.play(Sfx.ITEM)
                onChange(save.addToDeck(shown))
            }, onRemove = {
                sound.play(Sfx.CLICK)
                onChange(save.removeFromDeck(shown))
            })
        }
    }
}

/** 牌組的費用分布（0～7+ 的長條）。 */
@Composable
private fun CostCurve(deck: List<CardDef>, modifier: Modifier) {
    val counts = IntArray(8)
    for (c in deck) counts[c.cost.coerceAtMost(7)]++
    val most = counts.max().coerceAtLeast(1)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..7) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height((30 * counts[i] / most).dp.coerceAtLeast(2.dp)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE9A0), DivineColors.Incense))),
                )
                FitText(if (i == 7) "7+" else "$i", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CardDetail(card: CardDef, save: DivineSave, onClose: () -> Unit, onAdd: () -> Unit, onRemove: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC0A0518)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DivineCardView(card, Modifier.width(270.dp))
            Spacer(Modifier.height(10.dp))
            val unit = card.unit
            if (unit != null && unit.keywords.isNotEmpty()) {
                Column(Modifier.width(270.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xCC1E1250)).padding(10.dp)) {
                    for (k in unit.keywords) Text("【${k.label}】${k.help}", color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
            val owned = save.count(card)
            val inDeck = save.inDeck(card)
            FitText(if (owned == 0) "尚未獲得（抽卡包取得）" else "擁有 $owned 張　牌組裡 $inDeck 張（最多 ${card.rarity.maxCopies}）", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("－ 移出", enabled = inDeck > 0, background = Color(0xFF8A3A5A)) { onRemove() }
                PillButton("＋ 放入牌組", enabled = save.canAdd(card), background = DivineColors.Gold, contentColor = PlayColors.Night) { onAdd() }
                PillButton("關閉") { onClose() }
            }
        }
    }
}

// ───────────────────────── 抽卡 ─────────────────────────

@Composable
fun DivineShopScreen(save: DivineSave, onChange: (DivineSave) -> Unit, onBack: () -> Unit) {
    val sound = LocalSound.current
    var result by remember { mutableStateOf<PackResult?>(null) }
    var revealed by remember { mutableIntStateOf(0) }
    var opening by remember { mutableIntStateOf(0) }
    val random = remember { Random(System.nanoTime()) }

    LaunchedEffect(opening) {
        val r = result ?: return@LaunchedEffect
        revealed = 0
        delay(350)
        for (i in r.cards.indices) {
            revealed = i + 1
            sound.play(if (r.cards[i].rarity.ordinal >= 2) Sfx.RANK_UP else Sfx.ITEM)
            delay(if (r.cards[i].rarity.ordinal >= 2) 700 else 380)
        }
    }

    Box(Modifier.fillMaxSize().background(Backdrop)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Header("抽卡", "每包 ${DivineSave.PACK_SIZE} 張，最後一張保底稀有以上", save.coins, onBack)
            Spacer(Modifier.height(10.dp))
            val r = result
            if (r == null) {
                Spacer(Modifier.weight(1f))
                ArtImage("dv_pack", Modifier.size(260.dp))
                Spacer(Modifier.height(8.dp))
                Text("超過持有上限的卡會自動換成金幣。", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(1f))
            } else {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    for (row in listOf(r.cards.indices.take(3), r.cards.indices.drop(3))) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                            for (i in row) PackCard(r.cards[i], r.duplicates[i], i < revealed, Modifier.width(112.dp))
                        }
                    }
                    if (revealed >= r.cards.size && r.refund > 0) {
                        FitText("重複的卡換回 ${r.refund} 金幣", color = DivineColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                }
            }
            val busy = r != null && revealed < r.cards.size
            val canBuy = save.coins >= DivineSave.PACK_COST && !busy
            BigButton(
                if (save.coins >= DivineSave.PACK_COST) "🎁 開一包（${DivineSave.PACK_COST} 金幣）" else "金幣不夠（要 ${DivineSave.PACK_COST}）",
                if (canBuy) DivineColors.Gold else Color.White.copy(alpha = 0.2f), if (canBuy) PlayColors.Night else Color.White,
            ) {
                if (!canBuy) return@BigButton
                val opened = save.openPack(random) ?: return@BigButton
                sound.play(Sfx.POWER_UP)
                result = opened
                opening++
                onChange(opened.save)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** 卡包裡的一張卡：先是卡背，輪到它時翻過來。 */
@Composable
private fun PackCard(card: CardDef, duplicate: Boolean, shown: Boolean, modifier: Modifier) {
    val flip = remember(card, shown) { Animatable(if (shown) 0f else 1f) }
    LaunchedEffect(shown) { if (shown) { flip.snapTo(1f); flip.animateTo(0f, tween(320)) } }
    Box(modifier) {
        if (!shown) {
            DivineCardBack(Modifier.fillMaxWidth())
        } else {
            Box(Modifier.graphicsLayer {
                rotationY = flip.value * 90f
                val s = 1f + (1f - flip.value) * 0.0f
                scaleX = s
                scaleY = s
                cameraDistance = 12f * density
            }) {
                DivineCardView(card, Modifier.fillMaxWidth(), highlight = card.rarity.ordinal >= 2)
                if (duplicate) {
                    Box(Modifier.align(Alignment.Center).clip(RoundedCornerShape(50)).background(Color(0xDD0A0518)).border(1.5.dp, DivineColors.Gold, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        FitText("已滿 → +${card.rarity.refund}", color = DivineColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private val circle = CircleShape
