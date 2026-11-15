package com.purpal.catplayground.divine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.purpal.catplayground.divine.model.CardDef
import com.purpal.catplayground.divine.model.CardType
import com.purpal.catplayground.divine.model.Faction
import com.purpal.catplayground.divine.model.Rarity
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText

internal object DivineColors {
    val Gold = Color(0xFFFFD24A)
    val GoldDark = Color(0xFF8A4B0C)
    val Parchment = Color(0xFFFFF3D6)
    val Ink = Color(0xFF3A2414)
    val Incense = Color(0xFFFF9A3A)
    val Night = Color(0xFF150B2E)
    val Panel = Color(0xFF2A1A52)

    fun rarity(r: Rarity): List<Color> = when (r) {
        Rarity.COMMON -> listOf(Color(0xFFE6ECF5), Color(0xFF8A93A8))
        Rarity.RARE -> listOf(Color(0xFF9FD8FF), Color(0xFF2E6BD6))
        Rarity.EPIC -> listOf(Color(0xFFE0B0FF), Color(0xFF7A2ED6))
        Rarity.LEGEND -> listOf(Color(0xFFFFF6C2), Color(0xFFFFB02A), Color(0xFFE0621A))
    }

    fun faction(f: Faction): List<Color> = when (f) {
        Faction.EAST -> listOf(Color(0xFFA82A3A), Color(0xFF4A0E22))
        Faction.GREEK -> listOf(Color(0xFF3A5AB8), Color(0xFF16224E))
    }

    fun type(t: CardType): Color = when (t) {
        CardType.CREATURE -> Color(0xFFFFD24A)
        CardType.SPELL -> Color(0xFFFF7A5A)
        CardType.ENCHANT -> Color(0xFFB88AFF)
        CardType.EQUIP -> Color(0xFF7FD0FF)
    }
}

/** 攻擊力／生命的顯示：整數不帶小數點。 */
internal fun statText(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)

/**
 * 一張卡。大小跟著寬度縮放（所有尺寸都以寬度的 1/100 為單位）。
 * [compact]：手牌用的精簡版，只有費用、圖、名字與攻擊／生命。
 */
@Composable
fun DivineCardView(card: CardDef, modifier: Modifier = Modifier, compact: Boolean = false, dim: Boolean = false, highlight: Boolean = false) {
    BoxWithConstraints(modifier.aspectRatio(if (compact) 100f / 132f else 100f / 142f)) {
        val u = maxWidth / 100f
        val density = LocalDensity.current
        fun sp(units: Float): TextUnit = with(density) { (u * units).toSp() }
        val outer = RoundedCornerShape(u * 7)
        val rarity = DivineColors.rarity(card.rarity)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (dim) 0.55f else 1f }
                .shadow(if (highlight) u * 6 else u * 2, outer, ambientColor = DivineColors.Gold, spotColor = if (highlight) DivineColors.Gold else Color.Black)
                .clip(outer)
                .background(Brush.linearGradient(rarity))
                .padding(u * 2.6f)
                .clip(RoundedCornerShape(u * 5))
                .background(Brush.verticalGradient(DivineColors.faction(card.faction))),
        ) {
            Column(Modifier.fillMaxSize().padding(u * 2.4f)) {
                // 名字（左邊留位置給費用）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(u * 14)
                        .clip(RoundedCornerShape(u * 3))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(start = u * 17, end = u * 2),
                    contentAlignment = Alignment.Center,
                ) {
                    FitText(card.name, color = Color.White, fontSize = sp(if (compact) 9.5f else 9f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(u * 1.6f))
                // 卡圖
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(u * 3))
                        .border(u * 0.9f, Brush.linearGradient(rarity), RoundedCornerShape(u * 3)),
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val image = androidx.compose.runtime.remember(card.id) { com.purpal.catplayground.ui.ArtImages.get(context, "dv_card_${card.id}") }
                    androidx.compose.foundation.Image(image, card.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                if (compact) {
                    Spacer(Modifier.weight(1f))
                } else {
                    // 類型列
                    Row(Modifier.fillMaxWidth().height(u * 10), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(u * 4.5f).clip(CircleShape).background(Brush.linearGradient(rarity)))
                        Spacer(Modifier.width(u * 2))
                        FitText(
                            "${card.faction.label}・${card.type.label}　${card.rarity.label}",
                            color = DivineColors.type(card.type), fontSize = sp(6.2f), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                        )
                    }
                    // 說明
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(u * 3))
                            .background(Brush.verticalGradient(listOf(DivineColors.Parchment, Color(0xFFEBD3A0))))
                            .padding(horizontal = u * 3, vertical = u * 2)
                            .padding(bottom = if (card.unit != null) u * 6 else u * 0),
                    ) {
                        Text(
                            card.text,
                            color = DivineColors.Ink,
                            fontSize = sp(if (card.text.length > 30) 5.6f else 6.3f),
                            lineHeight = sp(if (card.text.length > 30) 7.4f else 8.2f),
                            fontWeight = FontWeight.Medium,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // 費用（香火）
            Orb(card.cost.toString(), u * 17, listOf(Color(0xFFFFE9A0), DivineColors.Incense, Color(0xFFC8421E)), sp(10.5f), Modifier.offset(u * 0.6f, u * 0.6f))
            // 攻擊／生命
            val unit = card.unit
            if (unit != null) {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(u * 1.2f), horizontalArrangement = Arrangement.SpaceBetween) {
                    Orb(statText(unit.atk), u * 16, listOf(Color(0xFFFFC2A0), Color(0xFFE8503A), Color(0xFF8A1E14)), sp(9.5f), label = "攻")
                    if (unit.ranged) {
                        Box(
                            Modifier.align(Alignment.CenterVertically).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = u * 3, vertical = u * 0.8f),
                        ) { FitText("遠程", color = Color(0xFF9FD8FF), fontSize = sp(5.6f), fontWeight = FontWeight.Bold) }
                    }
                    Orb(statText(unit.hp), u * 16, listOf(Color(0xFFC8FFD8), Color(0xFF3FB86A), Color(0xFF14602E)), sp(9.5f), label = "命")
                }
            }
        }
    }
}

/** 圓形的數值徽章（費用、攻擊、生命）。 */
@Composable
private fun Orb(text: String, size: Dp, colors: List<Color>, fontSize: TextUnit, modifier: Modifier = Modifier, label: String? = null) {
    Box(
        modifier
            .size(size)
            .drawBehind {
                drawCircle(Color.Black.copy(alpha = 0.45f), radius = this.size.minDimension / 2f, center = center + Offset(0f, this.size.minDimension * 0.05f))
                drawCircle(Brush.radialGradient(colors, center = Offset(this.size.width * 0.38f, this.size.height * 0.32f), radius = this.size.minDimension * 0.75f))
                drawCircle(Color.White.copy(alpha = 0.85f), radius = this.size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(this.size.minDimension * 0.06f))
                drawCircle(Color.White.copy(alpha = 0.55f), radius = this.size.minDimension * 0.1f, center = Offset(this.size.width * 0.32f, this.size.height * 0.28f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Black, maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 2f), 4f)),
        )
        if (label != null) {
            Text(
                label, color = Color.White.copy(alpha = 0.9f), fontSize = fontSize * 0.42f, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = size * 0.02f),
            )
        }
    }
}

/** 卡背（抽卡包翻牌前）。 */
@Composable
fun DivineCardBack(modifier: Modifier = Modifier) {
    ArtImage("dv_back", modifier.aspectRatio(100f / 142f))
}
