package com.purpal.catplayground

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.Context
import com.purpal.catplayground.shooter.model.CatType

/**
 * 只在 debug 版有效的啟動參數，給模擬器自動測試用（見 scripts/emulator-smoke.sh）：
 *   --es open shooter_menu|shooter_play --ei stage 1 --es cat ORANGE --ez autotest true
 * autotest 會讓射擊遊戲無敵、自動放大絕，並在 logcat（ShooterPerf）記錄每幀耗時。
 */
data class DebugLaunch(val open: String, val stage: Int, val cat: CatType, val autotest: Boolean) {

    fun initialStack(): List<Screen> = when (open) {
        "shooter_menu" -> listOf(Screen.Lobby, Screen.ShooterMenu)
        "shooter_play" -> listOf(Screen.Lobby, Screen.ShooterMenu, Screen.ShooterPlay(stage, cat))
        else -> listOf(Screen.Lobby)
    }

    companion object {
        fun from(context: Context, intent: Intent?): DebugLaunch? {
            val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (!debuggable || intent == null || !intent.hasExtra("open")) return null
            return DebugLaunch(
                open = intent.getStringExtra("open") ?: "",
                stage = intent.getIntExtra("stage", 1),
                cat = runCatching { CatType.valueOf(intent.getStringExtra("cat") ?: "") }.getOrDefault(CatType.ORANGE),
                autotest = intent.getBooleanExtra("autotest", false),
            )
        }
    }
}
