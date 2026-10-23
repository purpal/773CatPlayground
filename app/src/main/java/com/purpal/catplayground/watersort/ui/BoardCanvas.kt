package com.purpal.catplayground.watersort.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import com.purpal.catplayground.watersort.model.Board
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private const val LIFT = 0.16f          // 選取時抬高的比例（以試管高度計）
private const val HEADROOM = 0.1f       // 試管頂端留空的比例
private const val TILT_DEGREES = 72f

// 倒水動畫的三個階段：移過去並傾斜、倒水、回到原位。
const val POUR_DURATION_MS = 1000
private const val MOVE_END = 0.3f
private const val POUR_END = 0.75f
const val POUR_SOUND_DELAY_MS = (POUR_DURATION_MS * MOVE_END).toLong()

class BoardLayout(val rects: List<Rect>, val tubeWidth: Float, val tubeHeight: Float) {
    fun hitTest(point: Offset): Int? = rects.indexOfFirst { rect ->
        point.x in (rect.left - tubeWidth * 0.3f)..(rect.right + tubeWidth * 0.3f) &&
            point.y in (rect.top - tubeHeight * 0.35f)..(rect.bottom + tubeHeight * 0.1f)
    }.takeIf { it >= 0 }
}

fun computeBoardLayout(count: Int, size: Size): BoardLayout {
    val rows = if (count <= 6) 1 else 2
    val perRow = ceil(count / rows.toFloat()).toInt()
    var tubeWidth = minOf(size.width / perRow * 0.68f, size.width * 0.16f)
    var tubeHeight = tubeWidth * 3.5f
    val topMargin = 0.6f
    val rowGap = 0.7f
    val needed = tubeHeight * (topMargin + rows + (rows - 1) * rowGap)
    if (needed > size.height) {
        val scale = size.height / needed
        tubeWidth *= scale
        tubeHeight *= scale
    }
    val totalHeight = tubeHeight * (topMargin + rows + (rows - 1) * rowGap)
    val top = (size.height - totalHeight) / 2f + tubeHeight * topMargin
    val slot = size.width / perRow
    val rects = (0 until count).map { i ->
        val row = i / perRow
        val inRow = if (row == rows - 1) count - perRow * (rows - 1) else perRow
        val column = i % perRow
        val rowStart = (size.width - slot * inRow) / 2f
        val cx = rowStart + slot * (column + 0.5f)
        val y = top + row * tubeHeight * (1f + rowGap)
        Rect(cx - tubeWidth / 2f, y, cx + tubeWidth / 2f, y + tubeHeight)
    }
    return BoardLayout(rects, tubeWidth, tubeHeight)
}

@Composable
fun BoardCanvas(
    game: WaterSortGame,
    pourProgress: Float,
    flashTube: Int,
    flashProgress: Float,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val board = game.pour?.before ?: game.board
    val lifts = List(board.tubeCount) { i ->
        animateFloatAsState(if (game.selected == i) 1f else 0f, label = "lift$i").value
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(canvasSize, board.tubeCount) {
        computeBoardLayout(board.tubeCount, Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()))
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(layout) {
                detectTapGestures { point -> layout.hitTest(point)?.let(onTap) }
            },
    ) {
        if (canvasSize == IntSize.Zero) return@Canvas
        val stroke = layout.tubeWidth * 0.07f
        val pour = game.pour

        board.tubes.indices.forEach { i ->
            if (pour != null && i == pour.from) return@forEach
            val rect = layout.rects[i].translate(0f, -lifts[i] * LIFT * layout.tubeHeight)
            val (layers, fill) = when {
                pour != null && i == pour.to -> {
                    val frac = pourFraction(pourProgress)
                    (board.tubes[i] + List(pour.amount) { pour.color }) to board.tubes[i].size + pour.amount * frac
                }
                else -> board.tubes[i] to board.tubes[i].size.toFloat()
            }
            drawTube(rect, layers, fill, board.capacity, stroke, selected = game.selected == i, complete = board.isComplete(i))
            if (i == flashTube && flashProgress in 0f..0.999f) drawSparkle(rect, flashProgress)
        }

        if (pour != null) drawPouringTube(board, pour, layout, pourProgress, stroke)
    }
}

private fun pourFraction(progress: Float): Float =
    ((progress - MOVE_END) / (POUR_END - MOVE_END)).coerceIn(0f, 1f)

private fun ease(t: Float): Float = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))

private fun DrawScope.drawPouringTube(board: Board, pour: Pour, layout: BoardLayout, progress: Float, stroke: Float) {
    val w = layout.tubeWidth
    val h = layout.tubeHeight
    val home = layout.rects[pour.from].center
    val lifted = home - Offset(0f, LIFT * h)
    val target = layout.rects[pour.to]
    val direction = if (target.center.x >= home.x) 1f else -1f
    val tilt = TILT_DEGREES * direction
    val radians = tilt * PI.toFloat() / 180f
    // 讓傾斜後的管口剛好對準目標試管上方。
    val mouthTarget = Offset(target.center.x, target.top - h * 0.3f)
    val pourCenter = mouthTarget - Offset(h / 2f * sin(radians), -h / 2f * cos(radians))

    val center: Offset
    val angle: Float
    when {
        progress < MOVE_END -> {
            val t = ease(progress / MOVE_END)
            center = androidx.compose.ui.geometry.lerp(lifted, pourCenter, t)
            angle = tilt * t
        }
        progress < POUR_END -> {
            center = pourCenter
            angle = tilt
        }
        else -> {
            val t = ease((progress - POUR_END) / (1f - POUR_END))
            center = androidx.compose.ui.geometry.lerp(pourCenter, home, t)
            angle = tilt * (1f - t)
        }
    }

    val frac = pourFraction(progress)
    val source = board.tubes[pour.from]
    val rect = Rect(center.x - w / 2f, center.y - h / 2f, center.x + w / 2f, center.y + h / 2f)

    if (progress > MOVE_END && progress < POUR_END) {
        val targetFill = board.tubes[pour.to].size + pour.amount * frac
        val unit = h * (1f - HEADROOM) / board.capacity
        val surfaceY = target.bottom - stroke - targetFill * unit
        val streamWidth = w * 0.16f * sin(PI.toFloat() * frac).coerceAtLeast(0.35f)
        drawLine(
            color = TubeColors.palette[pour.color],
            start = mouthTarget,
            end = Offset(mouthTarget.x, surfaceY),
            strokeWidth = streamWidth,
            cap = StrokeCap.Round,
        )
    }

    rotate(angle, pivot = center) {
        drawTube(rect, source, source.size - pour.amount * frac, board.capacity, stroke, selected = true, complete = false)
    }
}

private fun tubePath(rect: Rect): Path = Path().apply {
    val r = rect.width / 2f
    moveTo(rect.left, rect.top)
    lineTo(rect.right, rect.top)
    lineTo(rect.right, rect.bottom - r)
    arcTo(Rect(rect.left, rect.bottom - 2 * r, rect.right, rect.bottom), 0f, 180f, false)
    close()
}

private fun DrawScope.drawTube(
    rect: Rect,
    layers: List<Int>,
    fill: Float,
    capacity: Int,
    stroke: Float,
    selected: Boolean,
    complete: Boolean,
) {
    val path = tubePath(rect)
    drawPath(path, Color.White.copy(alpha = 0.10f))
    val unit = rect.height * (1f - HEADROOM) / capacity
    clipPath(path) {
        for (k in layers.indices) {
            val amount = (fill - k).coerceIn(0f, 1f)
            if (amount <= 0f) break
            val bottom = rect.bottom - k * unit
            val top = bottom - amount * unit
            val color = TubeColors.palette[layers[k]]
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(lerp(color, Color.Black, 0.15f), color, lerp(color, Color.White, 0.25f), color),
                    startX = rect.left,
                    endX = rect.right,
                ),
                topLeft = Offset(rect.left, top),
                // 多畫一點點避免層與層之間出現細縫。
                size = Size(rect.width, bottom - top + if (k == 0) stroke else 1f),
            )
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(rect.left + rect.width * 0.2f, rect.top + rect.height * 0.08f),
            size = Size(rect.width * 0.12f, rect.height * 0.72f),
            cornerRadius = CornerRadius(rect.width * 0.06f),
        )
    }
    val outline = when {
        complete -> Color(0xFFFFD54F)
        selected -> Color.White
        else -> Color.White.copy(alpha = 0.7f)
    }
    drawPath(path, outline, style = Stroke(stroke))
    drawRoundRect(
        color = outline,
        topLeft = Offset(rect.left - rect.width * 0.1f, rect.top - stroke),
        size = Size(rect.width * 1.2f, stroke * 2.2f),
        cornerRadius = CornerRadius(stroke),
    )
}

/** 試管裝滿同一色時的閃光：擴散的光圈和星星。 */
private fun DrawScope.drawSparkle(rect: Rect, progress: Float) {
    val center = Offset(rect.center.x, rect.top)
    val alpha = 1f - progress
    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = alpha * 0.8f),
        radius = rect.width * lerp(0.3f, 1.6f, progress),
        center = center,
        style = Stroke(rect.width * 0.08f),
    )
    for (k in 0 until 8) {
        val a = k * PI.toFloat() / 4f
        val distance = rect.width * lerp(0.4f, 1.4f, progress)
        val point = center + Offset(cos(a) * distance, sin(a) * distance)
        drawCircle(Color.White.copy(alpha = alpha), radius = rect.width * 0.07f * (1f - progress * 0.5f), center = point)
    }
}
