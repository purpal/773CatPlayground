package com.purpal.catplayground.shooter.ui

import android.os.SystemClock
import android.util.Log
import com.purpal.catplayground.shooter.model.Phase
import com.purpal.catplayground.shooter.model.World

/**
 * 模擬器自動測試用（只在 debug 版的 autotest 模式啟用）：
 * 每 5 秒在 logcat 記錄幀耗時統計，並在固定時間自動放大絕，記下開始的時間點給腳本擷取影格。
 */
internal class AutotestProbe {
    private val frames = ArrayList<Float>()
    private var windowStart = SystemClock.elapsedRealtime()
    private val ultimateAt = floatArrayOf(22f, 40f)
    private var nextUltimate = 0

    fun frame(ms: Float, world: World) {
        if (ms in 0f..1000f) frames += ms
        val now = SystemClock.elapsedRealtime()
        if (now - windowStart >= 5000 && frames.isNotEmpty()) {
            val sorted = frames.sorted()
            val avg = frames.average()
            val p95 = sorted[(sorted.size * 0.95f).toInt().coerceAtMost(sorted.size - 1)]
            val slow = frames.count { it > 20f }
            Log.i(
                TAG,
                "frames=${frames.size} avg=%.1fms p95=%.1fms max=%.1fms slow(>20ms)=%d phase=%s bullets=%d particles=%d enemies=%d"
                    .format(avg, p95, sorted.last(), slow, world.phase, world.bullets.size, world.particles.size, world.enemies.size),
            )
            frames.clear()
            windowStart = now
        }
        val canUltimate = world.phase == Phase.STAGE || world.phase == Phase.BOSS
        if (nextUltimate < ultimateAt.size && world.time >= ultimateAt[nextUltimate] && canUltimate) {
            if (world.player.bombs == 0) world.player.bombs = 1
            if (world.requestUltimate()) Log.i(TAG, "ULTIMATE uptimeMs=${SystemClock.uptimeMillis()}")
            nextUltimate++
        }
    }

    companion object {
        const val TAG = "ShooterPerf"
    }
}
