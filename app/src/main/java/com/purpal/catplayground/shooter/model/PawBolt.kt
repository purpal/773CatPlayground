package com.purpal.catplayground.shooter.model

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 黑貓專武「高周波震動掌」的輪廓：以機體為中心的貓掌外輪廓（像貓掌造型的票卡，四顆趾頭在上方鼓起）。
 * 只有輪廓有傷害；掌心與趾頭的肉球只畫成淡淡的光暈。
 * 座標以 1 為單位（實際大小乘上 [PawBoltState.size]），y 往下為正。
 */
object PawPath {
    private const val POINTS = 120

    /** 掌心（大橢圓）。 */
    private const val BODY_Y = 0.15f
    private const val BODY_RX = 0.95f
    private const val BODY_RY = 0.85f

    /** 四顆趾頭的位置與半徑（外輪廓用）。 */
    private val TOES = arrayOf(
        floatArrayOf(-0.78f, -0.55f, 0.36f),
        floatArrayOf(-0.28f, -0.92f, 0.36f),
        floatArrayOf(0.28f, -0.92f, 0.36f),
        floatArrayOf(0.78f, -0.55f, 0.36f),
    )

    /** 內部的肉球（畫成淡淡的光暈）：x, y, 半徑。第一個是掌心肉球。 */
    val PADS = arrayOf(
        floatArrayOf(0f, 0.3f, 0.42f),
        floatArrayOf(-0.72f, -0.48f, 0.19f),
        floatArrayOf(-0.25f, -0.82f, 0.2f),
        floatArrayOf(0.25f, -0.82f, 0.2f),
        floatArrayOf(0.72f, -0.48f, 0.19f),
    )

    val xs: FloatArray
    val ys: FloatArray
    /** 每一點的外法線（輪廓是從中心往外取樣，所以就是方向向量）。 */
    val nx: FloatArray
    val ny: FloatArray
    /** 每一點到中心的距離。 */
    val radius: FloatArray
    /** 保留給畫面使用：外輪廓沒有跳躍段。 */
    val jump: BooleanArray
    /** 走到第 i 點時累積的路徑比例（0～1）。 */
    val at: FloatArray
    val count = POINTS

    /** 從中心往 angle 方向，射線離開一個圓（或橢圓）的距離；沒碰到回傳 0。 */
    private fun exitCircle(dx: Float, dy: Float, cx: Float, cy: Float, rx: Float, ry: Float): Float {
        // 把橢圓縮放成單位圓再解：|(t*dx - cx)/rx, (t*dy - cy)/ry| = 1
        val ax = dx / rx
        val ay = dy / ry
        val bx = -cx / rx
        val by = -cy / ry
        val a = ax * ax + ay * ay
        val b = 2f * (ax * bx + ay * by)
        val c = bx * bx + by * by - 1f
        val disc = b * b - 4f * a * c
        if (disc < 0f) return 0f
        return ((-b + sqrt(disc)) / (2f * a)).coerceAtLeast(0f)
    }

    init {
        xs = FloatArray(POINTS)
        ys = FloatArray(POINTS)
        nx = FloatArray(POINTS)
        ny = FloatArray(POINTS)
        radius = FloatArray(POINTS)
        jump = BooleanArray(POINTS)
        // 從正上方開始順時針繞一圈，每個方向取所有形狀裡最遠的邊界（聯集的外輪廓）
        for (i in 0 until POINTS) {
            val a = -HALF_PI + i * TWO_PI / POINTS
            val dx = cos(a)
            val dy = sin(a)
            var r = exitCircle(dx, dy, 0f, BODY_Y, BODY_RX, BODY_RY)
            for (t in TOES) r = maxOf(r, exitCircle(dx, dy, t[0], t[1], t[2], t[2]))
            xs[i] = dx * r
            ys[i] = dy * r
            nx[i] = dx
            ny[i] = dy
            radius[i] = r
        }
        val seg = FloatArray(POINTS)
        var total = 0f
        for (i in 0 until POINTS) {
            val j = (i + 1) % POINTS
            seg[i] = hypot(xs[j] - xs[i], ys[j] - ys[i])
            total += seg[i]
        }
        at = FloatArray(POINTS + 1)
        for (i in 0 until POINTS) at[i + 1] = at[i] + seg[i] / total
    }

    /** 從中心往 (dx, dy) 方向，輪廓離中心的距離（單位座標，兩個取樣點之間線性內插）。 */
    fun radiusToward(dx: Float, dy: Float): Float {
        val a = kotlin.math.atan2(dy, dx) + HALF_PI
        val f = ((a / TWO_PI) % 1f + 1f) % 1f * POINTS
        val i = f.toInt() % POINTS
        val j = (i + 1) % POINTS
        val t = f - f.toInt()
        return radius[i] + (radius[j] - radius[i]) * t
    }

    /** 路徑比例 t 所在的線段索引。 */
    fun segmentAt(t: Float): Int {
        var lo = 0
        var hi = count - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (at[mid] <= t) lo = mid else hi = mid - 1
        }
        return lo
    }

    /** 路徑比例 t 的位置（單位座標）寫進 out[0], out[1]。 */
    fun point(t: Float, out: FloatArray) {
        val u = ((t % 1f) + 1f) % 1f
        val i = segmentAt(u)
        val j = (i + 1) % count
        val span = (at[i + 1] - at[i]).coerceAtLeast(1e-6f)
        val f = (u - at[i]) / span
        out[0] = xs[i] + (xs[j] - xs[i]) * f
        out[1] = ys[i] + (ys[j] - ys[i]) * f
    }
}

/** 高周波震動掌目前的狀態（畫面用）。 */
class PawBoltState {
    var active = false
    var x = 0f
    var y = 0f
    var size = 0f
    var level = 0
    /** 距離上一次大震波的秒數（畫面用來畫往外擴散的震波）。 */
    var pulseAge = 9f
    /** 大震波的計時。 */
    var pulseTimer = 0f
    /** 震動的相位，用來決定每格殘影往哪邊偏。 */
    var phase = 0f
}
