package com.purpal.catplayground.shooter.render

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.World
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BackOut = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val QuickIn = CubicBezierEasing(0.6f, 0f, 1f, 0.6f)

private fun span(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)

/**
 * 大絕動畫（螢幕座標，畫在遊戲畫布裡，不經過重組，所以會很順）：
 * 畫面變暗、集中線 → 漫畫分格般的斜光帶彈進來，寫實漫畫風的猛虎／黑豹大頭衝出格子咆哮、
 * 震動、放出衝擊環 → 「吼～！」與招式名稱彈出 → 光帶飛走 → 白光，接著世界裡的衝擊波開始擴散。
 * 平常是軟萌的麻糬貓，這裡刻意用寫實畫風做出變身的反差。
 */
fun DrawScope.drawUltimateCinematic(w: World, sprites: SpriteCache, text: TextMeasurer) {
    if (w.ultimateCinematic <= 0f) return
    val t = (1f - w.ultimateCinematic / World.ULTIMATE_CINEMATIC).coerceIn(0f, 1f)
    val tiger = w.player.cat == CatType.ORANGE
    val main = if (tiger) Color(0xFFFF8F00) else Color(0xFF7C4DFF)
    val accent = if (tiger) Color(0xFFFFE082) else Color(0xFFD1C4E9)
    val leave = QuickIn.transform(span(t, 0.8f, 0.94f))
    val presence = FastOutSlowInEasing.transform(span(t, 0f, 0.12f)) * (1f - leave)

    // 背景變暗＋旋轉的集中線
    drawRect(Color(0xFF0A0614).copy(alpha = 0.72f * presence))
    val cx = size.width / 2f
    val cy = size.height * 0.4f
    rotate(t * 25f, Offset(cx, cy)) {
        for (k in 0 until 40) {
            val a = k * (2f * PI.toFloat() / 40f)
            val inner = size.minDimension * (0.28f + (k % 3) * 0.05f)
            drawLine(
                accent.copy(alpha = 0.35f * presence),
                Offset(cx + cos(a) * inner, cy + sin(a) * inner),
                Offset(cx + cos(a) * size.maxDimension, cy + sin(a) * size.maxDimension),
                strokeWidth = if (k % 4 == 0) 7f else 2.5f,
                blendMode = BlendMode.Plus,
            )
        }
    }

    // 斜光帶
    val enter = BackOut.transform(span(t, 0.04f, 0.22f))
    val slide = (1f - enter) * -size.width * 1.4f + leave * size.width * 1.6f
    val bandHeight = size.height * 0.28f
    rotate(-9f, Offset(cx, cy)) {
        val left = -size.width + slide
        drawRect(Brush.verticalGradient(listOf(main.shade(0.3f), main, accent, main, main.shade(0.3f)), cy - bandHeight / 2f, cy + bandHeight / 2f), Offset(left, cy - bandHeight / 2f), Size(size.width * 3f, bandHeight))
        // 漫畫分格的框線：外黑內白
        drawRect(Color(0xFF140D12), Offset(left, cy - bandHeight / 2f - 22f), Size(size.width * 3f, 14f))
        drawRect(Color(0xFF140D12), Offset(left, cy + bandHeight / 2f + 8f), Size(size.width * 3f, 14f))
        drawRect(Color.White, Offset(left, cy - bandHeight / 2f - 8f), Size(size.width * 3f, 8f))
        drawRect(Color.White, Offset(left, cy + bandHeight / 2f), Size(size.width * 3f, 8f))
        // 光帶上流動的光條
        for (k in 0 until 6) {
            val x = ((t * 3000f + k * 260f) % (size.width * 1.6f)) - size.width * 0.3f + slide
            drawRect(Color.White.copy(alpha = 0.18f), Offset(x, cy - bandHeight / 2f), Size(40f, bandHeight), blendMode = BlendMode.Plus)
        }

        // 咆哮的大頭
        val roar = span(t, 0.26f, 0.62f)
        val shake = if (roar > 0f && roar < 1f) sin(t * 400f) * 9f else 0f
        // 大頭比光帶還高，衝出格子外
        val faceSize = minOf(bandHeight * 1.55f, size.width * 0.95f) * (1f + 0.06f * sin(roar * PI.toFloat()))
        val faceX = cx + slide * 0.55f + shake
        val faceY = cy - bandHeight * 0.02f
        if (roar > 0f) {
            for (ring in 0..2) {
                val r = span(t, 0.28f + ring * 0.1f, 0.6f + ring * 0.1f)
                if (r in 0.001f..0.999f) {
                    drawCircle(Color.White.copy(alpha = 1f - r), bandHeight * (0.3f + r * 1.4f), Offset(faceX, faceY), style = Stroke(16f * (1f - r) + 2f), blendMode = BlendMode.Plus)
                }
            }
        }
        val face = sprites.ultimate(w.player.cat)
        val k = faceSize / face.image.width
        withTransform({
            translate(faceX, faceY)
            rotate(9f, Offset.Zero) // 抵銷光帶的傾斜，讓臉正對鏡頭
            scale(k, k, Offset.Zero)
        }) {
            drawImage(face.image, Offset(-face.image.width / 2f, -face.image.height / 2f))
        }
    }

    // 文字
    val pop = BackOut.transform(span(t, 0.26f, 0.4f))
    if (pop > 0f) {
        val roarSprite = sprites.roar()
        val roarWidth = size.width * 0.62f
        val roarHeight = roarWidth * roarSprite.image.height / roarSprite.image.width
        val nameText = text.measure(
            w.player.cat.ultimateName,
            TextStyle(color = accent, fontSize = 26.sp, fontWeight = FontWeight.Black, shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 5f), 6f)),
        )
        val textY = cy + bandHeight * 0.62f
        val scale = 2.2f - pop * 1.2f
        withTransform({
            translate(cx, textY + roarHeight / 2f)
            scale(scale * roarWidth / roarSprite.image.width, scale * roarWidth / roarSprite.image.width, Offset.Zero)
        }) {
            drawImage(
                roarSprite.image,
                Offset(-roarSprite.image.width / 2f, -roarSprite.image.height / 2f),
                alpha = pop.coerceIn(0f, 1f) * (1f - leave),
            )
        }
        val nameIn = span(t, 0.36f, 0.48f)
        drawText(
            nameText,
            topLeft = Offset(cx - nameText.size.width / 2f + (1f - nameIn) * 120f, textY + roarHeight + 6f),
            alpha = nameIn * (1f - leave),
        )
    }

    // OVERDRIVE：HUD 解鎖軌道砲（琥珀色框＋色差字），接著天降光柱
    val hud = span(t, 0.55f, 0.66f)
    if (hud > 0f) {
        val ov = sprites.overdrive()
        val ovW = size.width * 0.8f
        val ovH = ovW * ov.image.height / ov.image.width
        val oy = size.height * 0.12f
        val a = hud * (1f - span(t, 0.86f, 0.94f))
        val amber = Color(0xFFFFB300)
        val bx = size.width * 0.06f
        val bw = size.width * 0.88f
        val bh = ovH * 1.5f
        val c = 26f
        // 四個角的 L 形框
        for (corner in 0 until 4) {
            val sx = if (corner % 2 == 0) 1f else -1f
            val sy = if (corner < 2) 1f else -1f
            val x0 = if (sx > 0f) bx else bx + bw
            val y0 = if (sy > 0f) oy else oy + bh
            drawLine(amber.copy(alpha = a), Offset(x0, y0), Offset(x0 + sx * c, y0), 5f)
            drawLine(amber.copy(alpha = a), Offset(x0, y0), Offset(x0, y0 + sy * c), 5f)
        }
        val jitter = if (hud < 1f) sin(t * 300f) * 8f else 0f
        withTransform({
            translate(size.width / 2f + jitter, oy + bh / 2f)
            scale(ovW / ov.image.width, ovW / ov.image.width, Offset.Zero)
        }) {
            drawImage(ov.image, Offset(-ov.image.width / 2f, -ov.image.height / 2f), alpha = a)
        }
    }

    // 收尾白光
    val flash = span(t, 0.88f, 1f)
    if (flash > 0f) drawRect(Color.White.copy(alpha = flash))
}
