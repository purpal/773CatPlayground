package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.ItemType

/*
 * 寶可夢風格的敵人與 Boss。所有函式都以 (0,0) 為中心、用最終的世界尺寸作畫，
 * 由 SpriteCache 畫成圖片後再貼到畫面上。frame 是動畫格（翅膀、螺旋槳、尾巴）。
 */

private val MouseGray = Color(0xFFBDB4CF)
private val PinkEar = Color(0xFFFFA6C1)
private val NosePink = Color(0xFFFF6F91)

internal fun DrawScope.drawEnemyArt(kind: EnemyKind, frame: Int, phase: Int, item: ItemType?) {
    when (kind) {
        EnemyKind.MOUSE_RUNNER, EnemyKind.MINI_MOUSE -> mouse(frame)
        EnemyKind.MOUSE_PLANE -> mousePlane(frame)
        EnemyKind.CHEESE_DRONE -> cheeseFace()
        EnemyKind.PUPPY -> puppy(frame)
        EnemyKind.FRISBEE -> frisbee()
        EnemyKind.BALL_TURRET -> tennisTurret()
        EnemyKind.BEE -> bee(frame)
        EnemyKind.HONEY_POT -> honeyPot(frame)
        EnemyKind.BEAR_CUB -> bearCub(frame)
        EnemyKind.BEEHIVE -> beehive()
        EnemyKind.BALLOON -> balloon(item ?: ItemType.COIN)
        EnemyKind.BOSS_MOUSE -> mouseKing(phase, frame)
        EnemyKind.BOSS_DOG -> shibaCaptain(phase, frame)
        EnemyKind.BOSS_BEAR -> bearKing(phase, frame)
    }
}

// ───────────── 小怪 ─────────────

private fun DrawScope.mouse(frame: Int) {
    val tail = Path().apply {
        moveTo(0f, -20f)
        cubicTo(18f, -40f, -14f + frame * 10f, -52f, 10f + frame * 8f, -62f)
    }
    drawPath(tail, Toon.Outline, style = Stroke(7f, cap = StrokeCap.Round))
    drawPath(tail, PinkEar, style = Stroke(3.5f, cap = StrokeCap.Round))
    for (side in listOf(-1f, 1f)) {
        blob(circlePath(side * 25f, -20f, 18f), MouseGray)
        blob(circlePath(side * 25f, -19f, 11f), PinkEar, null, highlight = false, width = 0f)
    }
    blob(ovalPath(0f, 2f, 64f, 58f), MouseGray)
    blob(ovalPath(0f, 14f, 34f, 22f), Color(0xFFF1ECF7), null, highlight = false, width = 0f)
    animeEye(-11f, -2f, 11f, 15f, Color(0xFF3C2A6E))
    animeEye(11f, -2f, 11f, 15f, Color(0xFF3C2A6E))
    blob(ovalPath(0f, 9f, 9f, 7f), NosePink, highlight = false, width = 2f)
    drawRect(Color.White, Offset(-4f, 13f), Size(8f, 7f))
    drawRect(Toon.Outline, Offset(-4f, 13f), Size(8f, 7f), style = Stroke(1.5f))
    inkLine(0f, 13f, 0f, 20f, 1.2f)
    blush(-21f, 9f, 9f)
    blush(21f, 9f, 9f)
    for (side in listOf(-1f, 1f)) {
        inkLine(side * 10f, 9f, side * 32f, 5f, 1.3f)
        inkLine(side * 10f, 12f, side * 32f, 14f, 1.3f)
    }
}

private fun DrawScope.mousePlane(frame: Int) {
    // 飛機朝右（往左飛時在畫面上翻轉）
    blob(roundRectPath(-4f, 6f, 30f, 92f, 12f), Color(0xFFFF6B5B))
    blob(roundRectPath(0f, 10f, 92f, 30f, 15f), Color(0xFFE53935))
    blob(roundRectPath(-40f, 8f, 16f, 34f, 6f), Color(0xFFFF6B5B))
    blob(circlePath(44f, 10f, 8f), Color(0xFF9E9E9E))
    val blade = if (frame == 0) Size(6f, 40f) else Size(6f, 16f)
    drawOval(Color(0xCCECEFF1), Offset(50f - blade.width / 2f, 10f - blade.height / 2f), blade)
    at(0f, -16f, 0.62f) {
        mouse(frame)
        // 飛行員護目鏡
        drawRoundRect(Color(0xFF6D4C41), Offset(-26f, -12f), Size(52f, 9f), androidx.compose.ui.geometry.CornerRadius(4f))
    }
}

/** 起司輪無人機的臉（輪子本身另外畫、會旋轉）。 */
private fun DrawScope.cheeseFace() {
    animeEye(-13f, -4f, 12f, 16f, Color(0xFF6D3A00))
    animeEye(13f, -4f, 12f, 16f, Color(0xFF6D3A00))
    catMouth(0f, 10f, 5f)
    blush(-24f, 8f, 10f)
    blush(24f, 8f, 10f)
}

internal fun DrawScope.cheeseWheel() {
    blob(circlePath(0f, 0f, 54f), Color(0xFFFFB300), Color(0xFFE08A00))
    blob(circlePath(0f, 0f, 45f), Color(0xFFFFD54F), Color(0xFFFFC107), width = 2f)
    for ((x, y, r) in listOf(Triple(-28f, -14f, 7f), Triple(24f, -26f, 6f), Triple(30f, 18f, 8f), Triple(-20f, 30f, 6f), Triple(2f, -38f, 4f))) {
        blob(circlePath(x, y, r), Color(0xFFF2A000), null, highlight = false, width = 1.5f)
    }
}

private fun DrawScope.puppy(frame: Int) {
    val beige = Color(0xFFF2CF9E)
    val brown = Color(0xFFA0623B)
    val flop = if (frame == 0) 0f else 8f
    blob(ovalPath(0f, 32f, 44f, 26f), beige)
    at(-30f, -6f, rotationDegrees = 22f + flop) { blob(ovalPath(0f, 12f, 20f, 36f), brown) }
    at(30f, -6f, rotationDegrees = -22f - flop) { blob(ovalPath(0f, 12f, 20f, 36f), brown) }
    blob(ovalPath(0f, 0f, 66f, 58f), beige)
    blob(ovalPath(0f, 12f, 34f, 24f), Color(0xFFFFF6E8), null, highlight = false, width = 0f)
    animeEye(-12f, -5f, 11f, 15f, Color(0xFF5D3A1A))
    animeEye(12f, -5f, 11f, 15f, Color(0xFF5D3A1A))
    blob(ovalPath(0f, 7f, 12f, 8f), Color(0xFF2B1B1B), highlight = true, width = 1.5f)
    openMouth(0f, 13f, 12f, 9f)
    blush(-22f, 8f, 10f)
    blush(22f, 8f, 10f)
}

internal fun DrawScope.frisbee() {
    blob(circlePath(0f, 0f, 46f), Color(0xFF29B6F6), Color(0xFF0288D1))
    drawCircle(Color(0xFFFFEB3B), 34f, Offset.Zero, style = Stroke(7f))
    drawCircle(Color(0xFFFF4081), 20f, Offset.Zero, style = Stroke(6f))
    shape(Shapes.star, 0f, 0f, 10f, Color.White)
    drawCircle(Toon.Outline, 46f, Offset.Zero, style = Stroke(Toon.WIDTH))
}

private fun DrawScope.tennisTurret() {
    blob(roundRectPath(0f, 6f, 92f, 64f, 18f), Color(0xFF43A047), Color(0xFF2E7D32))
    blob(circlePath(0f, 0f, 34f), Color(0xFFD4E157), Color(0xFFAFB42B))
    clipPath(circlePath(0f, 0f, 34f)) {
        drawArc(Color.White, -60f, 120f, false, Offset(-70f, -30f), Size(60f, 60f), style = Stroke(5f))
        drawArc(Color.White, 120f, 120f, false, Offset(10f, -30f), Size(60f, 60f), style = Stroke(5f))
    }
    animeEye(-11f, -2f, 10f, 13f, Color(0xFF33691E))
    animeEye(11f, -2f, 10f, 13f, Color(0xFF33691E))
    catMouth(0f, 12f, 4f)
}

/** 網球砲台的砲管，朝 +x，另外依瞄準角度旋轉。 */
internal fun DrawScope.turretBarrel() {
    blob(roundRectPath(30f, 0f, 44f, 20f, 8f), Color(0xFF78909C), Color(0xFF546E7A))
    blob(circlePath(54f, 0f, 11f), Color(0xFFD4E157), Color(0xFFAFB42B))
}

private fun DrawScope.bee(frame: Int) {
    val wing = if (frame == 0) 30f else 16f
    for (side in listOf(-1f, 1f)) {
        blob(ovalPath(side * 18f, -16f, 24f, wing), Color(0xCCFFFFFF), null, highlight = false, width = 2f)
    }
    inkLine(-6f, -24f, -12f, -36f, 2f)
    inkLine(6f, -24f, 12f, -36f, 2f)
    blob(circlePath(-12f, -36f, 3.5f), Toon.Outline, null, highlight = false, width = 0f)
    blob(circlePath(12f, -36f, 3.5f), Toon.Outline, null, highlight = false, width = 0f)
    val body = ovalPath(0f, 2f, 42f, 46f)
    blob(body, Color(0xFFFFD600), Color(0xFFFFA000))
    clipPath(body) {
        drawRect(Color(0xFF2B1B1B), Offset(-30f, 6f), Size(60f, 7f))
        drawRect(Color(0xFF2B1B1B), Offset(-30f, 17f), Size(60f, 6f))
    }
    drawPath(body, Toon.Outline, style = Stroke(Toon.WIDTH))
    drawPath(polyPath(-4f, 24f, 4f, 24f, 0f, 32f), Toon.Outline)
    animeEye(-8f, -6f, 9f, 12f, Color(0xFF4E342E))
    animeEye(8f, -6f, 9f, 12f, Color(0xFF4E342E))
    blush(-15f, 2f, 7f)
    blush(15f, 2f, 7f)
}

private fun DrawScope.honeyPot(frame: Int) {
    blob(ovalPath(0f, 10f, 86f, 78f), Color(0xFFC07A3C), Color(0xFF8D5524))
    blob(roundRectPath(0f, -30f, 62f, 16f, 7f), Color(0xFF8D5524))
    blob(ovalPath(0f, -36f, 54f, 18f), Color(0xFFFFB300), Color(0xFFFF8F00), highlight = false)
    val drip = if (frame == 0) 0f else 7f
    blob(roundRectPath(-16f, -22f + drip / 2f, 11f, 24f + drip, 5f), Color(0xFFFFB300), null, highlight = false, width = 2f)
    blob(roundRectPath(14f, -26f, 9f, 14f, 4f), Color(0xFFFFB300), null, highlight = false, width = 2f)
    animeEye(-14f, 8f, 11f, 14f, Color(0xFF4E2A10))
    animeEye(14f, 8f, 11f, 14f, Color(0xFF4E2A10))
    catMouth(0f, 22f, 4f)
    blush(-26f, 18f, 10f)
    blush(26f, 18f, 10f)
}

private fun DrawScope.bearCub(frame: Int) {
    val brown = Color(0xFF9C6A43)
    blob(roundRectPath(0f, 32f, 112f, 32f, 15f), Color(0xFF7B5232), Color(0xFF5D3A1E))
    for (side in listOf(-1f, 1f)) {
        blob(circlePath(side * 52f, 32f, 15f), Color(0xFFD7A876), Color(0xFFB98552))
        drawCircle(Color(0xFF8D5B32), 8f, Offset(side * 52f, 32f), style = Stroke(2f))
    }
    for (side in listOf(-1f, 1f)) {
        blob(circlePath(side * 24f, -32f, 13f), brown)
        blob(circlePath(side * 24f, -31f, 7f), Color(0xFFE8C9A0), null, highlight = false, width = 0f)
    }
    blob(ovalPath(0f, -6f, 64f, 56f), brown)
    // 額頭上的月牙
    drawPath(
        Path().apply {
            moveTo(-9f, -26f)
            quadraticTo(0f, -14f, 9f, -26f)
            quadraticTo(0f, -20f, -9f, -26f)
            close()
        },
        Color(0xFFFFE082),
    )
    blob(ovalPath(0f, 6f, 30f, 20f), Color(0xFFEFD2AB), null, highlight = false, width = 0f)
    animeEye(-12f, -6f, 10f, 13f, Color(0xFF3E2210))
    animeEye(12f, -6f, 10f, 13f, Color(0xFF3E2210))
    blob(ovalPath(0f, 3f, 11f, 7f), Color(0xFF2B1B1B), width = 1.2f)
    catMouth(0f, 9f, 4f)
    blush(-20f, 6f, 9f)
    blush(20f, 6f, 9f)
    val wave = if (frame == 0) 0f else -10f
    blob(ovalPath(-36f, 2f + wave, 16f, 20f), brown)
}

internal fun DrawScope.beehive() {
    val path = Path().apply {
        moveTo(-34f, 26f)
        cubicTo(-40f, -20f, -18f, -40f, 0f, -40f)
        cubicTo(18f, -40f, 40f, -20f, 34f, 26f)
        close()
    }
    blob(path, Color(0xFFFFB300), Color(0xFFE65100))
    clipPath(path) { for (k in 0..3) drawRect(Color(0xFFE65100).copy(alpha = 0.7f), Offset(-40f, -28f + k * 14f), Size(80f, 3.5f)) }
    drawPath(path, Toon.Outline, style = Stroke(Toon.WIDTH))
    blob(ovalPath(0f, 12f, 16f, 12f), Color(0xFF3E2210), highlight = false, width = 2f)
}

private fun DrawScope.balloon(item: ItemType) {
    val color = itemColor(item)
    val string = Path().apply {
        moveTo(0f, 34f)
        cubicTo(8f, 48f, -8f, 56f, 0f, 66f)
    }
    drawPath(string, Toon.Outline, style = Stroke(2f))
    blob(ovalPath(0f, -8f, 76f, 86f), color, color.shade(0.25f))
    drawPath(polyPath(-6f, 36f, 6f, 36f, 0f, 30f), color)
    at(0f, 82f, 0.62f) { drawItemBadge(item, 0f) }
}

// ───────────── 道具 ─────────────

fun itemColor(type: ItemType): Color = when (type) {
    ItemType.SPREAD -> Color(0xFF2979FF)
    ItemType.FOCUS -> Color(0xFF00B8D4)
    ItemType.SPECIAL -> Color(0xFFFFAB00)
    ItemType.HOMING -> Color(0xFF00C853)
    ItemType.SONIC -> Color(0xFFAA00FF)
    ItemType.REPAIR -> Color(0xFFFF4081)
    ItemType.BOMB -> Color(0xFFFF6D00)
    ItemType.COIN -> Color(0xFFFFC400)
}

/** 道具徽章，中心 (0,0)，約 60 寬。 */
fun DrawScope.drawItemBadge(type: ItemType, t: Float) {
    if (type == ItemType.COIN) {
        blob(circlePath(0f, 0f, 20f), Color(0xFFFFC400), Color(0xFFFF8F00))
        fish(0f, 0f, 9f, Color(0xFFFF8F00))
        return
    }
    val color = itemColor(type)
    blob(roundRectPath(0f, 0f, 58f, 58f, 17f), Color.White, Color(0xFFE0E0E0), highlight = false)
    blob(roundRectPath(0f, 0f, 46f, 46f, 13f), color, color.shade(0.25f), width = 0f)
    when (type) {
        ItemType.SPREAD -> for (k in -1..1) inkLine(0f, 14f, k * 13f, -14f, 5f, Color.White)
        ItemType.FOCUS -> {
            drawRoundRect(Color.White, Offset(-6f, -16f), Size(12f, 32f), androidx.compose.ui.geometry.CornerRadius(6f))
            drawRoundRect(color.light(0.6f), Offset(-12f, -12f), Size(24f, 4f), androidx.compose.ui.geometry.CornerRadius(2f))
        }
        ItemType.SPECIAL -> paw(0f, 2f, 15f, Color.White)
        ItemType.HOMING -> fish(0f, 0f, 14f, Color.White)
        ItemType.SONIC -> for (k in 0..2) drawArc(Color.White, 200f, 140f, false, Offset(-8f - k * 7f, -2f - k * 7f), Size(16f + k * 14f, 16f + k * 14f), style = Stroke(4f))
        ItemType.REPAIR -> shape(Shapes.heart, 0f, 2f, 15f, Color.White)
        ItemType.BOMB -> shape(Shapes.star, 0f, 1f, 17f, Color.White)
        ItemType.COIN -> Unit
    }
}

internal fun DrawScope.paw(x: Float, y: Float, size: Float, color: Color) {
    oval(x, y + size * 0.35f, size * 1.15f, size * 0.95f, color)
    for ((dx, dy) in listOf(-0.62f to -0.25f, -0.22f to -0.62f, 0.22f to -0.62f, 0.62f to -0.25f)) {
        circle(x + dx * size, y + dy * size, size * 0.27f, color)
    }
}

internal fun DrawScope.fish(x: Float, y: Float, size: Float, color: Color) {
    oval(x - size * 0.15f, y, size * 1.5f, size * 0.9f, color)
    drawPath(polyPath(x + size * 0.45f, y, x + size, y - size * 0.5f, x + size, y + size * 0.5f), color)
}

// ───────────── Boss ─────────────

private fun DrawScope.crown(y: Float, width: Float) {
    val path = polyPath(
        -width / 2f, y, -width / 2f, y - width * 0.35f, -width / 4f, y - width * 0.15f,
        0f, y - width * 0.48f, width / 4f, y - width * 0.15f, width / 2f, y - width * 0.35f, width / 2f, y,
    )
    blob(path, Color(0xFFFFCA28), Color(0xFFFF8F00))
    blob(circlePath(0f, y - width * 0.13f, width * 0.08f), Color(0xFFE91E63), width = 2f)
    blob(circlePath(-width / 3f, y - width * 0.08f, width * 0.055f), Color(0xFF29B6F6), width = 2f)
    blob(circlePath(width / 3f, y - width * 0.08f, width * 0.055f), Color(0xFF29B6F6), width = 2f)
}

private fun DrawScope.bossEyes(dx: Float, y: Float, size: Float, iris: Color, phase: Int) {
    for (side in listOf(-1f, 1f)) {
        if (phase >= 1) angryEye(side * dx, y, size, size * 1.1f, iris, side)
        else animeEye(side * dx, y, size, size * 1.3f, iris)
    }
}

private fun DrawScope.mouseKing(phase: Int, frame: Int) {
    // 紅色披風
    blob(polyPath(-90f, 20f, 90f, 20f, 130f, 150f, -130f, 150f), Color(0xFFD32F2F), Color(0xFF8E1B1B), highlight = false)
    // 尾巴
    val tail = Path().apply {
        moveTo(60f, 60f)
        cubicTo(150f, 60f, 120f + frame * 20f, -40f, 160f + frame * 10f, -60f)
    }
    drawPath(tail, Toon.Outline, style = Stroke(14f, cap = StrokeCap.Round))
    drawPath(tail, PinkEar, style = Stroke(8f, cap = StrokeCap.Round))
    // 起司機甲身體
    blob(polyPath(-95f, 30f, 95f, 30f, 0f, 160f), Color(0xFFFFCA28), Color(0xFFFF9800))
    for ((x, y, r) in listOf(Triple(-36f, 56f, 11f), Triple(28f, 70f, 14f), Triple(-4f, 110f, 9f), Triple(46f, 40f, 7f))) {
        blob(circlePath(x, y, r), Color(0xFFF2A000), null, highlight = false, width = 2f)
    }
    for (side in listOf(-1f, 1f)) {
        blob(roundRectPath(side * 108f, 66f, 34f, 70f, 12f), Color(0xFF8D6E63), Color(0xFF5D4037))
        blob(circlePath(side * 108f, 104f, 14f), Color(0xFF37474F))
    }
    // 頭
    for (side in listOf(-1f, 1f)) {
        blob(circlePath(side * 76f, -80f, 56f), MouseGray)
        blob(circlePath(side * 76f, -78f, 36f), PinkEar, null, highlight = false, width = 0f)
    }
    blob(ovalPath(0f, -18f, 184f, 164f), MouseGray, shadeDx = 0.06f, shadeDy = 0.08f)
    blob(ovalPath(0f, 18f, 96f, 60f), Color(0xFFF1ECF7), null, highlight = false, width = 0f)
    crown(-96f, 96f)
    bossEyes(36f, -28f, 30f, Color(0xFF4A2C82), phase)
    blob(ovalPath(0f, 4f, 24f, 18f), NosePink, width = 2.5f)
    if (frame == 0 || phase == 0) {
        drawRect(Color.White, Offset(-12f, 18f), Size(24f, 18f))
        drawRect(Toon.Outline, Offset(-12f, 18f), Size(24f, 18f), style = Stroke(2.5f))
        inkLine(0f, 18f, 0f, 36f, 2f)
    } else {
        openMouth(0f, 16f, 40f, 26f)
    }
    blush(-60f, 8f, 26f)
    blush(60f, 8f, 26f)
    for (side in listOf(-1f, 1f)) {
        inkLine(side * 28f, 6f, side * 92f, -6f, 2.5f)
        inkLine(side * 28f, 14f, side * 94f, 16f, 2.5f)
    }
}

private fun DrawScope.shibaCaptain(phase: Int, frame: Int) {
    val tan = Color(0xFFE8964A)
    val cream = Color(0xFFFFF1DC)
    blob(ovalPath(0f, 88f, 190f, 120f), tan)
    blob(ovalPath(0f, 98f, 110f, 86f), cream, null, highlight = false, width = 0f)
    blob(roundRectPath(0f, 46f, 136f, 22f, 11f), Color(0xFFE53935))
    blob(circlePath(0f, 66f, 14f), Color(0xFFFFCA28))
    for (side in listOf(-1f, 1f)) blob(ovalPath(side * 84f, 126f, 50f, 40f), cream)
    for (side in listOf(-1f, 1f)) {
        blob(earPath(side * 64f, -122f, side * 92f, -50f, side * 26f, -82f), tan)
        blob(earPath(side * 60f, -108f, side * 78f, -60f, side * 36f, -80f), cream, null, highlight = false, width = 0f)
    }
    blob(ovalPath(0f, -12f, 190f, 172f), tan, shadeDx = 0.06f, shadeDy = 0.08f)
    blob(ovalPath(0f, 26f, 134f, 92f), cream, null, highlight = false, width = 0f)
    for (side in listOf(-1f, 1f)) blob(ovalPath(side * 34f, -54f, 22f, 12f), cream, null, highlight = false, width = 0f)
    // 船長帽
    blob(roundRectPath(0f, -110f, 150f, 48f, 20f), Color(0xFF1A237E), Color(0xFF0D1250))
    blob(roundRectPath(0f, -84f, 172f, 16f, 8f), Color(0xFF283593))
    paw(0f, -112f, 16f, Color(0xFFFFCA28))
    if (phase == 0) {
        for (side in listOf(-1f, 1f)) {
            drawArc(Toon.Outline, 200f, 140f, false, Offset(side * 36f - 17f, -36f), Size(34f, 26f), style = Stroke(7f, cap = StrokeCap.Round))
        }
    } else {
        bossEyes(36f, -26f, 26f, Color(0xFF5D3A1A), phase)
    }
    blob(ovalPath(0f, 2f, 30f, 20f), Color(0xFF2B1B1B), width = 2f)
    openMouth(0f, 16f, 34f, if (frame == 0) 24f else 34f)
    blush(-62f, 14f, 26f)
    blush(62f, 14f, 26f)
}

private fun DrawScope.bearKing(phase: Int, frame: Int) {
    val brown = Color(0xFF8D5B36)
    val light = Color(0xFFE6C496)
    blob(ovalPath(0f, 96f, 250f, 150f), brown)
    blob(ovalPath(0f, 106f, 140f, 108f), light, null, highlight = false, width = 0f)
    for (side in listOf(-1f, 1f)) {
        val bob = if (frame == 0) 0f else 8f
        blob(circlePath(side * 116f, 126f + bob * side, 34f), brown)
        for (k in -1..1) drawPath(polyPath(side * 116f + k * 12f - 4f, 98f, side * 116f + k * 12f + 4f, 98f, side * 116f + k * 12f, 86f), Color.White)
    }
    // 蜂蜜罐肚子
    blob(ovalPath(0f, 140f, 100f, 86f), Color(0xFFC07A3C), Color(0xFF8D5524))
    blob(ovalPath(0f, 102f, 80f, 22f), Color(0xFFFFB300), Color(0xFFFF8F00), highlight = false)
    for (side in listOf(-1f, 1f)) {
        blob(circlePath(side * 84f, -94f, 40f), brown)
        blob(circlePath(side * 84f, -92f, 22f), light, null, highlight = false, width = 0f)
    }
    blob(ovalPath(0f, -22f, 214f, 188f), brown, shadeDx = 0.06f, shadeDy = 0.08f)
    crown(-110f, 104f)
    blob(ovalPath(0f, 22f, 104f, 72f), light, null, highlight = false, width = 0f)
    bossEyes(42f, -36f, 28f, Color(0xFF3E2210), phase)
    blob(ovalPath(0f, 4f, 36f, 26f), Color(0xFF2B1B1B), width = 2.5f)
    if (phase >= 1) openMouth(0f, 22f, 44f, if (frame == 0) 26f else 34f) else catMouth(0f, 22f, 10f)
    blush(-72f, 2f, 30f)
    blush(72f, 2f, 30f)
    if (phase >= 2) blob(roundRectPath(28f, 46f, 12f, 30f, 6f), Color(0xFFFFB300), null, highlight = false, width = 2f)
}

/** 對話框、WARNING 畫面用的 Boss 頭像（直接畫，不經過快取）。 */
fun DrawScope.drawBossPortrait(kind: EnemyKind, phase: Int, time: Float) {
    drawEnemyArt(kind, ((time * 4f).toInt()) % 2, phase, null)
}
