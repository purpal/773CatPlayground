package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp

/**
 * 寶可夢風格的「卡通著色」小工具：粗深色描邊、右下方的陰影、左上方的高光。
 * 這些只在建立精靈圖（[SpriteCache]）或選單預覽時畫，不會每幀執行。
 */
internal object Toon {
    val Outline = Color(0xFF2A1A2E)
    const val WIDTH = 3.2f
}

internal fun Color.shade(amount: Float = 0.3f) = lerp(this, Color(0xFF3A2050), amount)
internal fun Color.light(amount: Float = 0.45f) = lerp(this, Color.White, amount)

internal fun ovalPath(cx: Float, cy: Float, w: Float, h: Float) = Path().apply { addOval(Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)) }

internal fun circlePath(cx: Float, cy: Float, r: Float) = ovalPath(cx, cy, r * 2f, r * 2f)

internal fun roundRectPath(cx: Float, cy: Float, w: Float, h: Float, r: Float) =
    Path().apply { addRoundRect(RoundRect(Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f), CornerRadius(r))) }

internal fun polyPath(vararg points: Float) = Path().apply {
    moveTo(points[0], points[1])
    var i = 2
    while (i < points.size) {
        lineTo(points[i], points[i + 1])
        i += 2
    }
    close()
}

/** 圓角的三角形耳朵：尖端 (tx,ty)，底邊從 (lx,ly) 到 (rx,ry)。 */
internal fun earPath(tx: Float, ty: Float, lx: Float, ly: Float, rx: Float, ry: Float) = Path().apply {
    moveTo(lx, ly)
    quadraticTo((lx + tx) / 2f - (ry - ty) * 0.08f, (ly + ty) / 2f, tx, ty)
    quadraticTo((rx + tx) / 2f + (ry - ty) * 0.08f, (ry + ty) / 2f, rx, ry)
    close()
}

/**
 * 畫一塊卡通著色的形狀：底色 → 右下陰影 → 左上高光 → 描邊。
 * shadeDx/shadeDy 是陰影偏移量占形狀寬高的比例。
 */
internal fun DrawScope.blob(
    path: Path,
    fill: Color,
    shade: Color? = fill.shade(),
    highlight: Boolean = true,
    outline: Color = Toon.Outline,
    width: Float = Toon.WIDTH,
    shadeDx: Float = 0.1f,
    shadeDy: Float = 0.14f,
) {
    val b = path.getBounds()
    if (shade != null) {
        drawPath(path, shade)
        clipPath(path) { translate(-b.width * shadeDx, -b.height * shadeDy) { drawPath(path, fill) } }
    } else {
        drawPath(path, fill)
    }
    if (highlight) {
        clipPath(path) {
            drawOval(
                fill.light(0.55f).copy(alpha = 0.75f),
                topLeft = Offset(b.left + b.width * 0.16f, b.top + b.height * 0.1f),
                size = androidx.compose.ui.geometry.Size(b.width * 0.32f, b.height * 0.2f),
            )
        }
    }
    if (width > 0f) drawPath(path, outline, style = Stroke(width, join = StrokeJoin.Round))
}

internal fun DrawScope.inkLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = Toon.WIDTH, color: Color = Toon.Outline) =
    drawLine(color, Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)

/** 動畫風的大眼睛：深色眼珠、虹膜漸層、兩個白色高光。 */
internal fun DrawScope.animeEye(cx: Float, cy: Float, w: Float, h: Float, iris: Color, slit: Boolean = false, look: Float = 0f) {
    val eye = ovalPath(cx, cy, w, h)
    drawPath(eye, Color(0xFF1C1228))
    clipPath(eye) {
        drawOval(iris, topLeft = Offset(cx - w * 0.42f + look, cy - h * 0.05f), size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.6f))
        drawOval(iris.light(0.35f), topLeft = Offset(cx - w * 0.3f + look, cy + h * 0.18f), size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.3f))
        if (slit) drawOval(Color(0xFF120A18), topLeft = Offset(cx - w * 0.1f + look, cy - h * 0.42f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.84f))
    }
    drawPath(eye, Toon.Outline, style = Stroke(Toon.WIDTH * 0.8f))
    drawCircle(Color.White, w * 0.2f, Offset(cx + w * 0.12f + look, cy - h * 0.22f))
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.09f, Offset(cx - w * 0.15f + look, cy + h * 0.2f))
}

/** 生氣的眼睛：上緣斜切的眼睛加粗眉。 */
internal fun DrawScope.angryEye(cx: Float, cy: Float, w: Float, h: Float, iris: Color, side: Float) {
    animeEye(cx, cy, w, h, iris)
    val lid = polyPath(cx - w * 0.7f, cy - h * 0.7f, cx + w * 0.7f, cy - h * 0.7f, cx + w * 0.7f * side, cy - h * 0.15f)
    drawPath(lid, Toon.Outline)
}

internal fun DrawScope.blush(cx: Float, cy: Float, w: Float) {
    drawOval(Color(0x66FF5C8A), topLeft = Offset(cx - w / 2f, cy - w * 0.28f), size = androidx.compose.ui.geometry.Size(w, w * 0.56f))
}

/** 「ω」形的貓嘴。 */
internal fun DrawScope.catMouth(cx: Float, cy: Float, w: Float, color: Color = Toon.Outline) {
    val path = Path().apply {
        moveTo(cx - w, cy - w * 0.2f)
        quadraticTo(cx - w * 0.5f, cy + w * 0.7f, cx, cy)
        quadraticTo(cx + w * 0.5f, cy + w * 0.7f, cx + w, cy - w * 0.2f)
    }
    drawPath(path, color, style = Stroke(Toon.WIDTH * 0.7f, cap = StrokeCap.Round))
}

/** 張開的嘴（帶舌頭）。 */
internal fun DrawScope.openMouth(cx: Float, cy: Float, w: Float, h: Float) {
    val mouth = Path().apply {
        moveTo(cx - w / 2f, cy)
        quadraticTo(cx, cy - h * 0.15f, cx + w / 2f, cy)
        quadraticTo(cx + w * 0.3f, cy + h, cx, cy + h)
        quadraticTo(cx - w * 0.3f, cy + h, cx - w / 2f, cy)
        close()
    }
    drawPath(mouth, Color(0xFF5A1630))
    clipPath(mouth) { drawOval(Color(0xFFFF6F91), topLeft = Offset(cx - w * 0.3f, cy + h * 0.45f), size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.7f)) }
    drawPath(mouth, Toon.Outline, style = Stroke(Toon.WIDTH * 0.8f, join = StrokeJoin.Round))
}
