package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.shooter.model.BallTurret
import com.purpal.catplayground.shooter.model.Balloon
import com.purpal.catplayground.shooter.model.Boss
import com.purpal.catplayground.shooter.model.CheeseDrone
import com.purpal.catplayground.shooter.model.Enemy
import com.purpal.catplayground.shooter.model.EnemyKind
import com.purpal.catplayground.shooter.model.Frisbee
import com.purpal.catplayground.shooter.model.ItemType
import com.purpal.catplayground.shooter.model.MousePlane
import kotlin.math.sin

private const val MINION_SCALE = 1.33f

private val Ink = Color(0xFF2B2233)
private val PinkInner = Color(0xFFFFA6C1)
private val BlushPink = Color(0x66FF6F9F)
private val White = Color.White

private fun DrawScope.eyes(dx: Float, y: Float, size: Float, angry: Boolean = false) {
    for (side in listOf(-1f, 1f)) {
        oval(side * dx, y, size, size * 1.15f, Ink)
        circle(side * dx + size * 0.2f, y - size * 0.25f, size * 0.22f, White)
        if (angry) line(side * (dx + size * 0.7f), y - size * 1.1f, side * (dx - size * 0.5f), y - size * 0.7f, Ink, size * 0.25f)
    }
}

private fun DrawScope.whiskers(dx: Float, y: Float, length: Float, color: Color = Ink) {
    for (side in listOf(-1f, 1f)) {
        line(side * dx, y - 3f, side * (dx + length), y - 7f, color, 1.5f)
        line(side * dx, y + 1f, side * (dx + length), y + 2f, color, 1.5f)
    }
}

private fun DrawScope.blush(dx: Float, y: Float, r: Float) {
    circle(-dx, y, r, BlushPink)
    circle(dx, y, r, BlushPink)
}

fun DrawScope.drawEnemy(e: Enemy, time: Float) {
    // 小怪的圖以舊尺寸畫，統一放大（判定半徑也已同步放大）；Boss 本來就夠大。
    val s = when {
        e.isBoss -> 1f
        e.kind == EnemyKind.MINI_MOUSE -> 0.8f * MINION_SCALE
        else -> MINION_SCALE
    }
    at(e.x, e.y, s) {
        when (e.kind) {
            EnemyKind.MOUSE_RUNNER, EnemyKind.MINI_MOUSE -> mouse(time + e.id)
            EnemyKind.MOUSE_PLANE -> mousePlane((e as MousePlane).facing, time)
            EnemyKind.CHEESE_DRONE -> cheeseDrone((e as CheeseDrone).spin)
            EnemyKind.PUPPY -> puppy(time + e.id)
            EnemyKind.FRISBEE -> frisbee((e as Frisbee).spin)
            EnemyKind.BALL_TURRET -> ballTurret((e as BallTurret).aim)
            EnemyKind.BEE -> bee(time + e.id * 0.3f)
            EnemyKind.HONEY_POT -> honeyPot(time)
            EnemyKind.BEAR_CUB -> bearCub(time + e.id)
            EnemyKind.BEEHIVE -> beehive(e.age)
            EnemyKind.BALLOON -> balloon((e as Balloon).drop ?: ItemType.COIN, time)
            EnemyKind.BOSS_MOUSE -> mouseKing((e as Boss).phase, time)
            EnemyKind.BOSS_DOG -> shibaCaptain((e as Boss).phase, time)
            EnemyKind.BOSS_BEAR -> bearKing((e as Boss).phase, time)
        }
        if (e.hitFlash > 0f) circle(0f, 0f, e.radius * 0.9f / s, Color.White.copy(alpha = 0.35f))
    }
}

// ───────────── 小怪 ─────────────

private fun DrawScope.mouse(t: Float) {
    val gray = Color(0xFFB0A8B9)
    val tail = sin(t * 6f) * 6f
    line(0f, -18f, 10f + tail, -40f, PinkInner, 3f)
    circle(-17f, -16f, 12f, gray)
    circle(17f, -16f, 12f, gray)
    circle(-17f, -16f, 7f, PinkInner)
    circle(17f, -16f, 7f, PinkInner)
    circle(0f, 0f, 22f, gray)
    eyes(8f, -3f, 6f)
    circle(0f, 8f, 3.5f, Color(0xFFFF6F91))
    whiskers(6f, 9f, 14f)
    blush(13f, 6f, 3.5f)
}

private fun DrawScope.mousePlane(facing: Float, t: Float) {
    at(0f, 0f) {
        // 飛機：機身、機翼、螺旋槳
        drawRoundRect(Color(0xFFE53935), Offset(-28f, -6f), Size(56f, 22f), CornerRadius(11f))
        drawRoundRect(Color(0xFFFF7043), Offset(-10f, -26f), Size(20f, 62f), CornerRadius(8f))
        val px = facing * 32f
        oval(px, 4f, 6f, 26f * kotlin.math.abs(sin(t * 30f)) + 4f, Color(0xFFECEFF1))
        circle(px, 4f, 3f, Ink)
    }
    at(0f, -8f, 0.7f) { mouse(t) }
}

private fun DrawScope.cheeseDrone(spin: Float) {
    at(0f, 0f, rotationDegrees = spin * 57.3f) {
        circle(0f, 0f, 38f, Color(0xFFFFB300))
        circle(0f, 0f, 33f, Color(0xFFFFD54F))
        for (k in 0 until 6) {
            val a = k * 1.047f
            circle(kotlin.math.cos(a) * 20f, kotlin.math.sin(a) * 20f, 5f + (k % 3), Color(0xFFFFB300))
        }
    }
    eyes(10f, -2f, 6f)
    blush(18f, 6f, 4f)
}

private fun DrawScope.puppy(t: Float) {
    val beige = Color(0xFFF3D3A8)
    val brown = Color(0xFFA9744F)
    val flop = sin(t * 5f) * 8f
    at(-24f, -8f, rotationDegrees = 20f + flop) { oval(0f, 8f, 16f, 30f, brown) }
    at(24f, -8f, rotationDegrees = -20f - flop) { oval(0f, 8f, 16f, 30f, brown) }
    circle(0f, 0f, 26f, beige)
    oval(0f, 10f, 26f, 18f, Color(0xFFFFF3E0))
    eyes(10f, -4f, 6f)
    oval(0f, 5f, 9f, 7f, Ink)
    oval(0f, 17f, 7f, 9f, Color(0xFFFF6F91))
    blush(16f, 8f, 4f)
}

private fun DrawScope.frisbee(spin: Float) {
    at(0f, 0f, rotationDegrees = spin * 57.3f) {
        circle(0f, 0f, 34f, Color(0xFF29B6F6))
        drawCircle(Color(0xFFFFEB3B), 25f, Offset.Zero, style = Stroke(6f))
        drawCircle(Color(0xFFFF4081), 14f, Offset.Zero, style = Stroke(5f))
        line(-30f, 0f, -18f, 0f, White.copy(alpha = 0.7f), 4f)
    }
}

private fun DrawScope.ballTurret(aim: Float) {
    drawRoundRect(Color(0xFF2E7D32), Offset(-34f, -26f), Size(68f, 52f), CornerRadius(14f))
    drawRoundRect(Color(0xFF66BB6A), Offset(-28f, -20f), Size(56f, 40f), CornerRadius(10f))
    at(0f, 0f, rotationDegrees = aim * 57.3f) {
        drawRoundRect(Color(0xFF455A64), Offset(0f, -8f), Size(34f, 16f), CornerRadius(6f))
        circle(36f, 0f, 9f, Color(0xFFC6FF00))
    }
    circle(0f, 0f, 16f, Color(0xFF81C784))
    eyes(6f, -1f, 4f)
}

private fun DrawScope.bee(t: Float) {
    val flap = 0.5f + 0.5f * kotlin.math.abs(sin(t * 40f))
    oval(-14f, -12f, 18f, 24f * flap, White.copy(alpha = 0.7f))
    oval(14f, -12f, 18f, 24f * flap, White.copy(alpha = 0.7f))
    oval(0f, 0f, 30f, 36f, Color(0xFFFFD600))
    line(-13f, -3f, 13f, -3f, Ink, 5f)
    line(-12f, 7f, 12f, 7f, Ink, 5f)
    shape(Shapes.triangle, 0f, 21f, 4f, Ink, rotationDegrees = 180f)
    eyes(6f, -10f, 4f)
}

private fun DrawScope.honeyPot(t: Float) {
    oval(0f, 6f, 76f, 70f, Color(0xFFB9783F))
    drawRoundRect(Color(0xFF8D5524), Offset(-26f, -34f), Size(52f, 14f), CornerRadius(6f))
    oval(0f, -30f, 50f, 16f, Color(0xFFFFB300))
    val drip = 6f + sin(t * 3f) * 4f
    drawRoundRect(Color(0xFFFFB300), Offset(-16f, -30f), Size(10f, 22f + drip), CornerRadius(5f))
    drawRoundRect(Color(0xFFFFB300), Offset(10f, -30f), Size(8f, 14f + drip / 2f), CornerRadius(4f))
    shape(Shapes.heart, 0f, 12f, 12f, Color(0xFFFF8A65))
}

private fun DrawScope.bearCub(t: Float) {
    val brown = Color(0xFF9C6B44)
    drawRoundRect(Color(0xFF7B5232), Offset(-44f, 6f), Size(88f, 28f), CornerRadius(14f))
    circle(-40f, 20f, 13f, Color(0xFFC79A6B))
    circle(40f, 20f, 13f, Color(0xFFC79A6B))
    circle(-20f, -24f, 11f, brown)
    circle(20f, -24f, 11f, brown)
    circle(0f, -6f, 26f, brown)
    oval(0f, 2f, 22f, 15f, Color(0xFFE8C9A0))
    eyes(10f, -10f, 5f)
    oval(0f, -1f, 8f, 6f, Ink)
    blush(16f, 2f, 4f)
    val wave = sin(t * 6f) * 10f
    circle(-30f, -6f + wave, 8f, brown)
}

private fun DrawScope.beehive(age: Float) {
    at(0f, 0f, rotationDegrees = age * 200f) {
        oval(0f, 0f, 56f, 60f, Color(0xFFFFB300))
        for (k in -1..1) line(-24f, k * 14f, 24f, k * 14f, Color(0xFFE65100), 3f)
        circle(0f, 8f, 7f, Ink)
    }
}

private fun DrawScope.balloon(item: ItemType, t: Float) {
    val color = itemColor(item)
    line(0f, 26f, sin(t * 3f) * 4f, 52f, White.copy(alpha = 0.8f), 2f)
    oval(0f, -6f, 62f, 72f, color)
    oval(-12f, -22f, 14f, 22f, White.copy(alpha = 0.45f))
    shape(Shapes.triangle, 0f, 31f, 5f, color, rotationDegrees = 180f)
    at(0f, 66f, 0.55f) { drawItemBadge(item, t) }
}

// ───────────── 道具 ─────────────

fun itemColor(type: ItemType): Color = when (type) {
    ItemType.SPREAD -> Color(0xFF2979FF)
    ItemType.FOCUS -> Color(0xFFFF1744)
    ItemType.SPECIAL -> Color(0xFFFFAB00)
    ItemType.HOMING -> Color(0xFF00C853)
    ItemType.SONIC -> Color(0xFFAA00FF)
    ItemType.REPAIR -> Color(0xFFFF4081)
    ItemType.BOMB -> Color(0xFFFF6D00)
    ItemType.COIN -> Color(0xFFFFC400)
}

/** 道具徽章，中心 (0,0)，約 56 寬。 */
fun DrawScope.drawItemBadge(type: ItemType, t: Float) {
    if (type == ItemType.COIN) {
        circle(0f, 0f, 20f, Color(0xFFFFA000))
        circle(0f, 0f, 16f, Color(0xFFFFD54F))
        fish(0f, 0f, 9f, Color(0xFFFFA000))
        return
    }
    val color = itemColor(type)
    val glow = 0.35f + 0.25f * sin(t * 6f)
    circle(0f, 0f, 38f, color.copy(alpha = glow))
    drawRoundRect(White, Offset(-28f, -28f), Size(56f, 56f), CornerRadius(16f))
    drawRoundRect(color, Offset(-24f, -24f), Size(48f, 48f), CornerRadius(13f))
    when (type) {
        ItemType.SPREAD -> for (k in -1..1) line(0f, 14f, k * 13f, -14f, White, 5f)
        ItemType.FOCUS -> for (k in -1..1) line(k * 9f, 14f, k * 9f, -14f, White, 5f)
        ItemType.SPECIAL -> paw(0f, 2f, 15f, White)
        ItemType.HOMING -> fish(0f, 0f, 14f, White)
        ItemType.SONIC -> for (k in 0..2) drawArc(White, 200f, 140f, false, Offset(-8f - k * 7f, -2f - k * 7f), Size(16f + k * 14f, 16f + k * 14f), style = Stroke(4f))
        ItemType.REPAIR -> shape(Shapes.heart, 0f, 2f, 15f, White)
        ItemType.BOMB -> shape(Shapes.star, 0f, 1f, 17f, White)
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
    val tail = Path().apply {
        moveTo(x + size * 0.45f, y)
        lineTo(x + size, y - size * 0.5f)
        lineTo(x + size, y + size * 0.5f)
        close()
    }
    drawPath(tail, color)
}

// ───────────── Boss ─────────────

private fun DrawScope.crown(y: Float, width: Float) {
    val gold = Color(0xFFFFC107)
    val path = Path().apply {
        moveTo(-width / 2f, y)
        lineTo(-width / 2f, y - width * 0.35f)
        lineTo(-width / 4f, y - width * 0.15f)
        lineTo(0f, y - width * 0.45f)
        lineTo(width / 4f, y - width * 0.15f)
        lineTo(width / 2f, y - width * 0.35f)
        lineTo(width / 2f, y)
        close()
    }
    drawPath(path, gold)
    circle(0f, y - width * 0.12f, width * 0.07f, Color(0xFFE91E63))
    circle(-width / 3f, y - width * 0.08f, width * 0.05f, Color(0xFF29B6F6))
    circle(width / 3f, y - width * 0.08f, width * 0.05f, Color(0xFF29B6F6))
}

private fun DrawScope.mouseKing(phase: Int, t: Float) {
    val gray = Color(0xFFB7AEC2)
    val angry = phase >= 1
    // 起司機甲
    shape(Shapes.wedge, 0f, 70f, 95f, Color(0xFFFFB300), rotationDegrees = 90f, sy = 1.1f)
    shape(Shapes.wedge, 0f, 64f, 82f, Color(0xFFFFD54F), rotationDegrees = 90f, sy = 1.1f)
    for ((hx, hy, r) in listOf(Triple(-30f, 50f, 10f), Triple(25f, 70f, 13f), Triple(-5f, 105f, 8f), Triple(40f, 35f, 7f))) {
        circle(hx, hy, r, Color(0xFFFFB300))
    }
    for (side in listOf(-1f, 1f)) {
        drawRoundRect(Color(0xFF8D6E63), Offset(side * 100f - 14f, 30f), Size(28f, 60f), CornerRadius(10f))
        circle(side * 100f, 92f, 12f, Ink)
    }
    // 尾巴
    line(60f, 10f, 120f + sin(t * 3f) * 15f, -40f, PinkInner, 7f)
    // 頭
    for (side in listOf(-1f, 1f)) {
        circle(side * 72f, -78f, 52f, gray)
        circle(side * 72f, -78f, 33f, PinkInner)
    }
    circle(0f, -20f, 82f, gray)
    crown(-92f, 90f)
    eyes(30f, -30f, 20f, angry)
    circle(0f, 2f, 11f, Color(0xFFFF6F91))
    drawRect(White, Offset(-11f, 20f), Size(10f, 14f))
    drawRect(White, Offset(1f, 20f), Size(10f, 14f))
    whiskers(18f, 6f, 46f)
    blush(52f, 4f, 12f)
    if (angry) {
        for (side in listOf(-1f, 1f)) circle(side * 95f, -10f + sin(t * 8f) * 4f, 9f, White.copy(alpha = 0.6f))
    }
}

private fun DrawScope.shibaCaptain(phase: Int, t: Float) {
    val tan = Color(0xFFE59A4E)
    val cream = Color(0xFFFFF1DC)
    // 身體與項圈
    oval(0f, 80f, 170f, 110f, tan)
    oval(0f, 90f, 100f, 80f, cream)
    drawRoundRect(Color(0xFFE53935), Offset(-60f, 40f), Size(120f, 18f), CornerRadius(9f))
    circle(0f, 64f, 12f, Color(0xFFFFC107))
    for (side in listOf(-1f, 1f)) circle(side * 78f, 112f, 22f, cream)
    // 耳朵
    for (side in listOf(-1f, 1f)) {
        shape(Shapes.triangle, side * 52f, -78f, 34f, tan, rotationDegrees = side * 15f)
        shape(Shapes.triangle, side * 52f, -72f, 20f, cream, rotationDegrees = side * 15f)
    }
    circle(0f, -10f, 86f, tan)
    oval(0f, 22f, 120f, 80f, cream)
    for (side in listOf(-1f, 1f)) oval(side * 30f, -48f, 18f, 10f, cream)
    // 船長帽
    drawRoundRect(Color(0xFF1A237E), Offset(-70f, -112f), Size(140f, 40f), CornerRadius(18f))
    drawRoundRect(Color(0xFF283593), Offset(-80f, -80f), Size(160f, 14f), CornerRadius(7f))
    paw(0f, -94f, 14f, Color(0xFFFFC107))
    // 臉
    if (phase == 0) {
        for (side in listOf(-1f, 1f)) drawArc(Ink, 200f, 140f, false, Offset(side * 32f - 14f, -30f), Size(28f, 22f), style = Stroke(6f))
    } else {
        eyes(32f, -22f, 17f, angry = phase == 2)
    }
    oval(0f, 2f, 26f, 18f, Ink)
    val pant = 10f + sin(t * 10f) * 4f
    oval(0f, 30f + pant / 2f, 22f, 18f + pant, Color(0xFFFF6F91))
    blush(54f, 10f, 12f)
}

private fun DrawScope.bearKing(phase: Int, t: Float) {
    val brown = Color(0xFF8D5B36)
    val light = Color(0xFFE2BE91)
    // 身體與蜂蜜罐
    oval(0f, 90f, 230f, 140f, brown)
    oval(0f, 100f, 130f, 100f, light)
    for (side in listOf(-1f, 1f)) {
        circle(side * 105f, 120f + sin(t * 2f + side) * 6f, 30f, brown)
        for (k in -1..1) circle(side * 105f + k * 10f, 98f, 5f, Ink)
    }
    oval(0f, 130f, 90f, 80f, Color(0xFFB9783F))
    oval(0f, 96f, 70f, 18f, Color(0xFFFFB300))
    // 頭
    for (side in listOf(-1f, 1f)) {
        circle(side * 78f, -88f, 36f, brown)
        circle(side * 78f, -88f, 20f, light)
    }
    circle(0f, -20f, 98f, brown)
    crown(-104f, 100f)
    oval(0f, 18f, 92f, 64f, light)
    eyes(38f, -34f, 18f, angry = phase >= 1)
    oval(0f, 2f, 30f, 22f, Ink)
    drawArc(Ink, 20f, 140f, false, Offset(-22f, 8f), Size(44f, 30f), style = Stroke(5f))
    blush(64f, 0f, 14f)
    if (phase >= 2) {
        // 生氣時嘴邊沾著蜂蜜
        drawRoundRect(Color(0xFFFFB300), Offset(18f, 30f), Size(10f, 24f + sin(t * 4f) * 6f), CornerRadius(5f))
    }
}

/** 對話框用的 Boss 頭像。 */
fun DrawScope.drawBossPortrait(kind: EnemyKind, phase: Int, time: Float) {
    when (kind) {
        EnemyKind.BOSS_MOUSE -> mouseKing(phase, time)
        EnemyKind.BOSS_DOG -> shibaCaptain(phase, time)
        EnemyKind.BOSS_BEAR -> bearKing(phase, time)
        else -> Unit
    }
}
