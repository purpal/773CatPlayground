package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

/** 以單位大小預先建好的形狀，畫的時候再平移、縮放、旋轉，避免每幀重建。 */
internal object Shapes {
    val star: Path = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 1f else 0.45f
            val a = -Math.PI.toFloat() / 2f + i * Math.PI.toFloat() / 5f
            if (i == 0) moveTo(cos(a) * r, sin(a) * r) else lineTo(cos(a) * r, sin(a) * r)
        }
        close()
    }

    val heart: Path = Path().apply {
        moveTo(0f, 0.9f)
        cubicTo(-1.3f, 0f, -0.9f, -1.1f, 0f, -0.45f)
        cubicTo(0.9f, -1.1f, 1.3f, 0f, 0f, 0.9f)
        close()
    }

    /** 尖端朝 +x 的起司三角。 */
    val wedge: Path = Path().apply {
        moveTo(1f, 0f)
        lineTo(-0.8f, -0.85f)
        lineTo(-0.8f, 0.85f)
        close()
    }

    /** 尖端朝上的水滴。 */
    val drop: Path = Path().apply {
        moveTo(0f, -1.2f)
        cubicTo(0.5f, -0.4f, 0.9f, 0.1f, 0.9f, 0.4f)
        cubicTo(0.9f, 0.95f, -0.9f, 0.95f, -0.9f, 0.4f)
        cubicTo(-0.9f, 0.1f, -0.5f, -0.4f, 0f, -1.2f)
        close()
    }

    /** 尖端朝下的火焰。 */
    val flame: Path = Path().apply {
        moveTo(0f, 1.2f)
        cubicTo(0.7f, 0.4f, 0.8f, -0.3f, 0.5f, -0.8f)
        quadraticTo(0f, -1.1f, -0.5f, -0.8f)
        cubicTo(-0.8f, -0.3f, -0.7f, 0.4f, 0f, 1.2f)
        close()
    }

    /** 尖端朝上的三角形（貓耳）。 */
    val triangle: Path = Path().apply {
        moveTo(0f, -1f)
        lineTo(1f, 1f)
        lineTo(-1f, 1f)
        close()
    }
}

internal inline fun DrawScope.at(
    x: Float,
    y: Float,
    scale: Float = 1f,
    rotationDegrees: Float = 0f,
    crossinline block: DrawScope.() -> Unit,
) {
    withTransform({
        translate(x, y)
        if (rotationDegrees != 0f) rotate(rotationDegrees, Offset.Zero)
        if (scale != 1f) scale(scale, scale, Offset.Zero)
    }) { block() }
}

internal fun DrawScope.shape(path: Path, x: Float, y: Float, size: Float, color: Color, rotationDegrees: Float = 0f, sx: Float = 1f, sy: Float = 1f) {
    withTransform({
        translate(x, y)
        if (rotationDegrees != 0f) rotate(rotationDegrees, Offset.Zero)
        scale(size * sx, size * sy, Offset.Zero)
    }) { drawPath(path, color) }
}

internal fun DrawScope.oval(cx: Float, cy: Float, w: Float, h: Float, color: Color) =
    drawOval(color, topLeft = Offset(cx - w / 2f, cy - h / 2f), size = Size(w, h))

internal fun DrawScope.circle(cx: Float, cy: Float, r: Float, color: Color) = drawCircle(color, r, Offset(cx, cy))

internal fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) =
    drawLine(color, Offset(x1, y1), Offset(x2, y2), width, androidx.compose.ui.graphics.StrokeCap.Round)

internal fun rectOf(cx: Float, cy: Float, w: Float, h: Float) = Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)

internal fun Long.toColor() = Color(this)
