package com.purpal.catplayground.shooter.model

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 黑貓專武「肉球閃電」的路徑：以機體為中心的貓掌輪廓（掌心＋四顆趾頭，趾頭朝上）。
 * 座標以 1 為單位（實際大小乘上 [PawBoltState.size]），閃電依序繞掌心一圈、再跳到每顆趾頭各繞一圈。
 */
object PawPath {
    private const val PAD_POINTS = 40
    private const val TOE_POINTS = 18

    val xs: FloatArray
    val ys: FloatArray
    /** 第 i 點到第 i+1 點是不是「跳躍」段（不同形狀之間，畫成淡淡的電弧）。 */
    val jump: BooleanArray
    /** 走到第 i 點時累積的路徑比例（0～1）。 */
    val at: FloatArray
    val count: Int

    init {
        val px = ArrayList<Float>()
        val py = ArrayList<Float>()
        val pj = ArrayList<Boolean>()
        fun loop(cx: Float, cy: Float, rx: Float, ry: Float, n: Int, start: Float, widen: Float = 0f) {
            for (i in 0..n) {
                val a = start + i * TWO_PI / n
                // widen：越往下越寬，掌心看起來像圓角三角形
                px += cx + rx * cos(a) * (1f + widen * sin(a))
                py += cy + ry * sin(a)
                pj += i == n
            }
        }
        // 掌心：從頂端出發
        loop(0f, 0.28f, 0.82f, 0.6f, PAD_POINTS, -HALF_PI, widen = 0.14f)
        // 四顆趾頭（由左到右）
        for ((tx, ty, r) in listOf(Triple(-0.98f, -0.52f, 0.3f), Triple(-0.38f, -1.02f, 0.32f), Triple(0.38f, -1.02f, 0.32f), Triple(0.98f, -0.52f, 0.3f))) {
            loop(tx, ty, r, r * 1.12f, TOE_POINTS, HALF_PI)
        }
        count = px.size
        xs = px.toFloatArray()
        ys = py.toFloatArray()
        jump = pj.toBooleanArray()
        // 跳躍段走得比較快（長度只算三分之一），讓閃電大部分時間在描輪廓
        val seg = FloatArray(count)
        var total = 0f
        for (i in 0 until count) {
            val j = (i + 1) % count
            val len = hypot(xs[j] - xs[i], ys[j] - ys[i]) * (if (jump[i]) 0.33f else 1f)
            seg[i] = len
            total += len
        }
        at = FloatArray(count + 1)
        for (i in 0 until count) at[i + 1] = at[i] + seg[i] / total
    }

    /** 路徑比例 t（0～1）所在的線段索引。 */
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

/** 肉球閃電目前的狀態（畫面用）。 */
class PawBoltState {
    var active = false
    var x = 0f
    var y = 0f
    var size = 0f
    /** 閃電頭在路徑上的位置（0～1，一直往上加，取小數部分）。 */
    var progress = 0f
    var headX = 0f
    var headY = 0f
    var level = 0
}
