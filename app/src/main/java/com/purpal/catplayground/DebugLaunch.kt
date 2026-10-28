package com.purpal.catplayground

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.Context
import com.purpal.catplayground.shooter.model.CatType

/**
 * 只在 debug 版有效的啟動參數，給模擬器自動測試用（見 scripts/emulator-smoke.sh）：
 *   --es open shooter_menu|shooter_play --ei stage 1 --es cat ORANGE --ez autotest true
 * autotest 會讓射擊遊戲無敵、自動放大絕，並在 logcat（ShooterPerf）記錄每幀耗時。
 * probe 可關掉部分繪圖功能來找模擬器當掉的原因（見 [DebugFlags]）。
 */
data class DebugLaunch(val open: String, val stage: Int, val cat: CatType, val autotest: Boolean, val bossHp: Float? = null) {

    fun initialStack(): List<Screen> = when (open) {
        "nova_menu" -> listOf(Screen.Lobby, Screen.NovaMenu)
        "nova_tour" -> listOf(Screen.Lobby, Screen.NovaMenu, Screen.NovaTour)
        "nova_boss" -> listOf(Screen.Lobby, Screen.NovaMenu, Screen.NovaPlay(stage, cat))
        "nova_play" -> listOf(Screen.Lobby, Screen.NovaMenu, Screen.NovaPlay(stage, cat))
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
                bossHp = if(intent.getStringExtra("open")=="nova_boss") intent.getFloatExtra("hp_ratio",.3f) else null,
            ).also { DebugFlags.apply(intent.getStringExtra("probe") ?: "") }
        }
    }
}
