package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.shooter.model.CatType
import kotlin.math.sin

private class CatLook(
    val fur: Color,
    val dark: Color,
    val belly: Color,
    val iris: Color,
    val pupilSlit: Boolean,
)

private val orangeLook = CatLook(Color(0xFFFFA64D), Color(0xFFE0712A), Color(0xFFFFE6C7), Color(0xFF5A3A22), pupilSlit = false)
private val blackLook = CatLook(Color(0xFF2E2A3B), Color(0xFF15131C), Color(0xFF4B4562), Color(0xFFFFD54F), pupilSlit = true)
private val tigerLook = CatLook(Color(0xFFFFA726), Color(0xFF1E1A16), Color(0xFFFFF8EC), Color(0xFF7CB342), pupilSlit = true)
private val pantherLook = CatLook(Color(0xFF1C1B2E), Color(0xFF3D3A78), Color(0xFF2A2847), Color(0xFFFFEB3B), pupilSlit = true)

private val Pink = Color(0xFFFF8FB1)
private val Blush = Color(0x66FF6F9F)

/**
 * 玩家的貓咪機體（正面朝向玩家），中心在 (0,0)，大約 80 寬。
 * transformed 為 true 時畫成老虎／黑豹。
 */
fun DrawScope.drawCatShip(cat: CatType, time: Float, bank: Float, transformed: Boolean) {
    if (transformed) {
        val aura = if (cat == CatType.ORANGE) Color(0xFFFFC107) else Color(0xFF9C6BFF)
        val pulse = 1f + sin(time * 10f) * 0.08f
        drawCircle(
            Brush.radialGradient(listOf(aura.copy(alpha = 0.6f), aura.copy(alpha = 0f)), Offset.Zero, 90f * pulse),
            90f * pulse,
            Offset.Zero,
        )
        at(0f, 0f, scale = 1.3f, rotationDegrees = bank) { drawBigCat(cat, time, fierce = true) }
        return
    }
    at(0f, 0f, rotationDegrees = bank) { drawCatBody(if (cat == CatType.ORANGE) orangeLook else blackLook, cat == CatType.ORANGE, time) }
}

/** 大絕畫面上的老虎／黑豹大頭（同樣以 (0,0) 為中心，約 80 寬，用 scale 放大）。 */
fun DrawScope.drawBigCat(cat: CatType, time: Float, fierce: Boolean) {
    val look = if (cat == CatType.ORANGE) tigerLook else pantherLook
    drawCatBody(look, stripes = cat == CatType.ORANGE, time = time, fierce = fierce, tiger = true)
}

private fun DrawScope.drawCatBody(look: CatLook, stripes: Boolean, time: Float, fierce: Boolean = false, tiger: Boolean = false) {
    // 腳底的推進火焰
    val flicker = 1f + sin(time * 40f) * 0.15f
    for (side in listOf(-1f, 1f)) {
        shape(Shapes.flame, side * 13f, 50f, 11f, Color(0xFFFF9100), sy = 1.3f * flicker)
        shape(Shapes.flame, side * 13f, 47f, 6f, Color(0xFFFFF59D), sy = 1.2f * flicker)
    }
    // 尾巴
    val sway = sin(time * 4f) * 6f
    line(16f, 26f, 32f + sway, 6f, look.fur, 9f)
    line(32f + sway, 6f, 26f + sway, -14f, look.fur, 9f)
    if (stripes) line(30f + sway, 0f, 34f + sway, 4f, look.dark, 4f)
    // 身體與肚子
    oval(0f, 22f, 54f, 46f, look.fur)
    oval(0f, 26f, 30f, 28f, look.belly)
    if (stripes) {
        line(-24f, 16f, -16f, 20f, look.dark, 4f)
        line(24f, 16f, 16f, 20f, look.dark, 4f)
    }
    // 前腳
    for (side in listOf(-1f, 1f)) {
        circle(side * 13f, 42f, 8f, look.fur)
        line(side * 13f - 3f, 46f, side * 13f - 3f, 49f, look.dark, 1.5f)
        line(side * 13f + 3f, 46f, side * 13f + 3f, 49f, look.dark, 1.5f)
    }
    // 耳朵
    for (side in listOf(-1f, 1f)) {
        shape(Shapes.triangle, side * 19f, -36f, 13f, look.fur, rotationDegrees = side * 18f)
        shape(Shapes.triangle, side * 19f, -33f, 7f, if (tiger && !stripes) look.dark else Pink, rotationDegrees = side * 18f)
    }
    // 頭
    circle(0f, -8f, 31f, look.fur)
    if (tiger) oval(0f, 6f, 40f, 24f, look.belly)
    if (stripes) {
        line(0f, -37f, 0f, -28f, look.dark, 5f)
        line(-9f, -35f, -7f, -27f, look.dark, 4f)
        line(9f, -35f, 7f, -27f, look.dark, 4f)
        line(-30f, -8f, -22f, -6f, look.dark, 4f)
        line(30f, -8f, 22f, -6f, look.dark, 4f)
        if (tiger) {
            line(-29f, 0f, -21f, 1f, look.dark, 3.5f)
            line(29f, 0f, 21f, 1f, look.dark, 3.5f)
        }
    }
    // 眼睛
    for (side in listOf(-1f, 1f)) {
        val ex = side * 11f
        if (fierce) {
            oval(ex, -9f, 13f, 11f, look.iris)
            oval(ex, -9f, 3.5f, 10f, Color.Black)
            line(ex - side * 8f, -19f, ex + side * 6f, -14f, look.dark.takeIf { stripes } ?: Color.Black, 3.5f)
        } else {
            oval(ex, -8f, 13f, 16f, look.iris)
            if (look.pupilSlit) oval(ex, -8f, 4f, 13f, Color.Black)
            circle(ex + 2.5f, -12f, 3f, Color.White)
            circle(ex - 2.5f, -5f, 1.5f, Color.White)
        }
    }
    // 鼻子、嘴巴、鬍鬚、腮紅
    shape(Shapes.triangle, 0f, 2f, 3.5f, Pink, rotationDegrees = 180f)
    drawArc(look.dark, 0f, 180f, false, Offset(-7f, 2f), androidx.compose.ui.geometry.Size(7f, 6f), style = Stroke(1.8f))
    drawArc(look.dark, 0f, 180f, false, Offset(0f, 2f), androidx.compose.ui.geometry.Size(7f, 6f), style = Stroke(1.8f))
    if (fierce) {
        shape(Shapes.triangle, -4f, 11f, 2.5f, Color.White, rotationDegrees = 180f)
        shape(Shapes.triangle, 4f, 11f, 2.5f, Color.White, rotationDegrees = 180f)
    }
    val whisker = if (look == blackLook || look == pantherLook) Color(0xFFBDBDBD) else Color(0xFF6D4C41)
    for (side in listOf(-1f, 1f)) {
        line(side * 15f, 2f, side * 34f, -2f, whisker, 1.3f)
        line(side * 15f, 5f, side * 35f, 5f, whisker, 1.3f)
        line(side * 15f, 8f, side * 33f, 12f, whisker, 1.3f)
        if (!fierce) circle(side * 19f, 5f, 5f, Blush)
    }
}
