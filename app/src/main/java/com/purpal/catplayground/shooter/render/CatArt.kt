package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.shooter.model.CatType
import kotlin.math.sin

private class CatPalette(
    val fur: Color,
    val furShade: Color,
    val stripe: Color,
    val belly: Color,
    val iris: Color,
    val slit: Boolean,
    val innerEar: Color,
)

private val orange = CatPalette(Color(0xFFFFA445), Color(0xFFE0782A), Color(0xFFB4501E), Color(0xFFFFF4E2), Color(0xFF8A5A2B), false, Color(0xFFFF9DB5))
private val black = CatPalette(Color(0xFF3A3452), Color(0xFF221E33), Color(0xFF221E33), Color(0xFFF2EEFF), Color(0xFFFFD43B), true, Color(0xFFFF8FB0))
private val tiger = CatPalette(Color(0xFFFF9A1F), Color(0xFFD9680E), Color(0xFF1E1410), Color(0xFFFFFBF2), Color(0xFF7CD13A), true, Color(0xFFFFF4E2))
private val panther = CatPalette(Color(0xFF2B2640), Color(0xFF15121F), Color(0xFF6E5BD6), Color(0xFF2B2640), Color(0xFFFFE135), true, Color(0xFF6E5BD6))

/**
 * 玩家機體，中心 (0,0)，大約 90 寬。
 * frame 0～3 控制推進火焰與尾巴的擺動（精靈圖會各畫一張）。
 */
fun DrawScope.drawCatSprite(cat: CatType, frame: Int, beast: Boolean) {
    val p = when {
        beast && cat == CatType.ORANGE -> tiger
        beast -> panther
        cat == CatType.ORANGE -> orange
        else -> black
    }
    val wave = sin(frame * 1.57f)
    jetFlames(frame, beast, cat)
    tail(p, wave, stripes = cat == CatType.ORANGE || beast)
    // 身體、手、肚子、腳
    blob(ovalPath(0f, 26f, 50f, 42f), p.fur, p.furShade)
    blob(ovalPath(0f, 30f, 28f, 24f), p.belly, p.belly.shade(0.12f), highlight = false, width = 0f)
    for (side in listOf(-1f, 1f)) {
        blob(ovalPath(side * 23f, 24f, 14f, 18f), p.fur, p.furShade)
        blob(ovalPath(side * 12f, 45f, 18f, 12f), p.belly, p.belly.shade(0.15f), highlight = false)
    }
    head(p, cat, beast)
}

/** 選單或對話框直接畫的版本（會擺動）。 */
fun DrawScope.drawCatShip(cat: CatType, time: Float, bank: Float, transformed: Boolean) {
    at(0f, 0f, rotationDegrees = bank) { drawCatSprite(cat, ((time * 12f).toInt()) % 4, transformed) }
}

/** 大絕動畫裡的大頭（老虎／黑豹在咆哮）。 */
fun DrawScope.drawBigCat(cat: CatType, time: Float, fierce: Boolean) {
    drawCatSprite(cat, ((time * 12f).toInt()) % 4, beast = fierce)
}

private fun DrawScope.jetFlames(frame: Int, beast: Boolean, cat: CatType) {
    val length = 1f + (frame % 2) * 0.25f
    val outer = when {
        !beast -> Color(0xFFFF8A1F)
        cat == CatType.ORANGE -> Color(0xFFFF5A1F)
        else -> Color(0xFF8E5BFF)
    }
    for (side in listOf(-1f, 1f)) {
        shape(Shapes.flame, side * 12f, 62f, 11f, outer.copy(alpha = 0.55f), sy = 1.9f * length)
        shape(Shapes.flame, side * 12f, 58f, 8f, outer, sy = 1.5f * length)
        shape(Shapes.flame, side * 12f, 55f, 4.5f, Color(0xFFFFF7C2), sy = 1.4f * length)
    }
}

private fun DrawScope.tail(p: CatPalette, wave: Float, stripes: Boolean) {
    val tip = 8f * wave
    val path = Path().apply {
        moveTo(16f, 34f)
        cubicTo(42f, 34f, 46f, 6f, 34f + tip, -10f)
    }
    drawPath(path, Toon.Outline, style = Stroke(15f, cap = StrokeCap.Round))
    drawPath(path, p.fur, style = Stroke(9.5f, cap = StrokeCap.Round))
    if (stripes) {
        inkLine(38f, 20f, 46f, 22f, 4f, p.stripe)
        inkLine(38f, 6f, 45f, 3f, 4f, p.stripe)
    }
}

private fun DrawScope.head(p: CatPalette, cat: CatType, beast: Boolean) {
    // 耳朵
    for (side in listOf(-1f, 1f)) {
        blob(earPath(side * 27f, -54f, side * 36f, -18f, side * 8f, -34f), p.fur, p.furShade, highlight = false)
        blob(earPath(side * 25f, -46f, side * 30f, -24f, side * 14f, -33f), p.innerEar, null, highlight = false, width = 0f)
    }
    // 老虎的臉頰毛
    if (beast) {
        for (side in listOf(-1f, 1f)) {
            blob(polyPath(side * 30f, -14f, side * 48f, -4f, side * 34f, 2f, side * 46f, 10f, side * 28f, 12f), p.belly, p.belly.shade(0.15f), highlight = false)
        }
    }
    blob(ovalPath(0f, -8f, 74f, 64f), p.fur, p.furShade, shadeDx = 0.08f, shadeDy = 0.1f)
    // 嘴邊白毛
    if (cat == CatType.ORANGE || beast) blob(ovalPath(0f, 8f, 36f, 22f), p.belly, p.belly.shade(0.1f), highlight = false, width = 0f)
    // 斑紋
    if (cat == CatType.ORANGE || (beast && cat == CatType.ORANGE)) {
        for (k in -1..1) {
            val x = k * 9f
            drawPath(polyPath(x - 3f, -40f, x + 3f, -40f, x, -27f + kotlin.math.abs(k) * 4f), p.stripe)
        }
        for (side in listOf(-1f, 1f)) {
            inkLine(side * 36f, -10f, side * 27f, -8f, 3.5f, p.stripe)
            inkLine(side * 36f, -2f, side * 28f, -1f, 3.5f, p.stripe)
        }
    }
    if (beast && cat == CatType.BLACK) {
        // 黑豹的紫色光澤
        drawArc(p.stripe.copy(alpha = 0.8f), 200f, 70f, false, Offset(-30f, -36f), androidx.compose.ui.geometry.Size(60f, 50f), style = Stroke(3.5f))
    }
    // 眼睛
    for (side in listOf(-1f, 1f)) {
        if (beast) angryEye(side * 14f, -8f, 15f, 15f, p.iris, side)
        else animeEye(side * 14f, -8f, 15f, 19f, p.iris, slit = p.slit)
    }
    // 鼻子、嘴
    drawPath(polyPath(-4f, 2f, 4f, 2f, 0f, 6f), Color(0xFFFF6F91))
    if (beast) {
        openMouth(0f, 9f, 18f, 12f)
        drawPath(polyPath(-7f, 9f, -3f, 9f, -5f, 15f), Color.White)
        drawPath(polyPath(3f, 9f, 7f, 9f, 5f, 15f), Color.White)
    } else {
        catMouth(0f, 8f, 5f)
        blush(-22f, 6f, 10f)
        blush(22f, 6f, 10f)
    }
    val whisker = if (cat == CatType.BLACK) Color(0xFFD9D4F0) else Toon.Outline.copy(alpha = 0.6f)
    for (side in listOf(-1f, 1f)) {
        inkLine(side * 18f, 4f, side * 40f, 0f, 1.6f, whisker)
        inkLine(side * 18f, 8f, side * 41f, 9f, 1.6f, whisker)
    }
}
