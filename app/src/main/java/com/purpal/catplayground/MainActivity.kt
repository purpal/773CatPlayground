package com.purpal.catplayground

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Music
import com.purpal.catplayground.audio.SoundManager
import com.purpal.catplayground.data.AppPrefs
import com.purpal.catplayground.lobby.LobbyScreen
import com.purpal.catplayground.shooter.model.Loadout
import com.purpal.catplayground.shooter.model.Stages
import com.purpal.catplayground.shooter.render.FxSettings
import com.purpal.catplayground.shooter.ui.ShooterGameScreen
import com.purpal.catplayground.shooter.ui.ShooterMenuScreen
import com.purpal.catplayground.ui.BrandSplash
import com.purpal.catplayground.ui.CatPlaygroundTheme
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.update.AppUpdater
import com.purpal.catplayground.update.UpdateDialog
import com.purpal.catplayground.update.UpdateInfo
import com.purpal.catplayground.watersort.ui.LevelSelectScreen
import com.purpal.catplayground.watersort.ui.WaterSortScreen
import kotlinx.coroutines.launch

/** 系統字體縮放的上限。 */
private const val MAX_FONT_SCALE = 1.15f

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var sound: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = AppPrefs(applicationContext)
        sound = SoundManager(applicationContext, prefs)
        val updater = AppUpdater(applicationContext)
        val debug = DebugLaunch.from(this, intent)
        setContent {
            // 系統字體設定得很大時，限制放大倍率，避免文字被擠成兩行或截斷。
            val density = LocalDensity.current
            val capped = Density(density.density, minOf(density.fontScale, MAX_FONT_SCALE))
            CatPlaygroundTheme {
                CompositionLocalProvider(LocalSound provides sound, LocalDensity provides capped) {
                    AppRoot(prefs, updater, debug, onExit = { finish() })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sound.onForeground()
    }

    override fun onStop() {
        sound.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        sound.release()
        super.onDestroy()
    }
}

@Composable
private fun AppRoot(prefs: AppPrefs, updater: AppUpdater, debug: DebugLaunch?, onExit: () -> Unit) {
    val context = LocalContext.current
    val sound = LocalSound.current
    val scope = rememberCoroutineScope()
    var stack by remember { mutableStateOf(debug?.initialStack() ?: listOf<Screen>(Screen.Lobby)) }
    var waterSortCleared by remember { mutableIntStateOf(prefs.waterSortCleared) }
    var shooterCleared by remember { mutableIntStateOf(prefs.shooterCleared) }
    var carryStage by remember { mutableIntStateOf(prefs.shooterCarryStage) }

    /** 取出保留給這一關的機體狀態（只能用一次）。 */
    fun takeCarry(stage: Int): Loadout? {
        if (prefs.shooterCarryStage != stage) return null
        val loadout = Loadout.decode(prefs.shooterCarry)
        prefs.shooterCarry = null
        prefs.shooterCarryStage = 0
        carryStage = 0
        return loadout
    }
    // 啟動時先顯示開發商品牌頁（自動測試時略過）；旋轉螢幕不會重播。
    var showBrand by rememberSaveable { mutableStateOf(debug == null) }
    var confirmExit by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }

    // 自動測試時不跳更新視窗，以免擋住畫面。
    LaunchedEffect(Unit) { if (debug == null) update = updater.check() }

    fun checkForUpdate() {
        scope.launch {
            val found = updater.check()
            if (found != null) {
                update = found
                updateDismissed = false
            } else {
                Toast.makeText(context, "已經是最新版本 v${updater.currentVersion} 👍", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun replaceTop(screen: Screen) {
        stack = stack.dropLast(1) + screen
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    // 射擊遊戲自己管理背景音樂，其他畫面都用大廳音樂。
    LaunchedEffect(stack.last()) {
        if (stack.last() !is Screen.ShooterPlay) sound.playMusic(Music.LOBBY)
    }

    BackHandler(enabled = stack.size > 1) { pop() }
    BackHandler(enabled = stack.size == 1) { confirmExit = true }

    Crossfade(targetState = stack.last(), label = "screen") { screen ->
        when (screen) {
            Screen.Lobby -> LobbyScreen(
                waterSortCleared = waterSortCleared,
                shooterCleared = shooterCleared,
                version = updater.currentVersion,
                onOpen = { push(it) },
                onCheckUpdate = ::checkForUpdate,
            )

            Screen.WaterSortLevels -> LevelSelectScreen(
                cleared = waterSortCleared,
                onBack = { pop() },
                onPlay = { push(Screen.WaterSortPlay(it)) },
            )

            is Screen.WaterSortPlay -> WaterSortScreen(
                level = screen.level,
                cleared = waterSortCleared,
                onCleared = { level ->
                    if (level > waterSortCleared) {
                        waterSortCleared = level
                        prefs.waterSortCleared = level
                    }
                },
                onNextLevel = { replaceTop(Screen.WaterSortPlay(screen.level + 1)) },
                onBack = { pop() },
            )

            Screen.ShooterMenu -> ShooterMenuScreen(
                prefs = prefs,
                cleared = shooterCleared,
                carryStage = carryStage,
                onBack = { pop() },
                onPlay = { stage, cat -> push(Screen.ShooterPlay(stage, cat, takeCarry(stage))) },
            )

            is Screen.ShooterPlay -> {
                FxSettings.damageNumbers = prefs.shooterDamageNumbers
                FxSettings.high = prefs.shooterFxHigh
                ShooterGameScreen(
                    stageNumber = screen.stage,
                    cat = screen.cat,
                    autotest = debug?.autotest == true,
                    ultimateOnLeft = prefs.shooterUltimateOnLeft,
                    bestScore = prefs.shooterBest(screen.stage),
                    loadout = screen.loadout,
                    onCleared = { stage, score, loadout ->
                        prefs.setShooterBest(stage, score)
                        if (stage > shooterCleared) {
                            shooterCleared = stage
                            prefs.shooterCleared = stage
                        }
                        // 破關時的機體狀態先存起來：直接進下一關會用掉，回列表的話下次挑戰下一關時用。
                        if (Stages.get(stage + 1) != null) {
                            prefs.shooterCarry = loadout.encode()
                            prefs.shooterCarryStage = stage + 1
                            carryStage = stage + 1
                        }
                    },
                    onNextStage = { replaceTop(Screen.ShooterPlay(screen.stage + 1, screen.cat, takeCarry(screen.stage + 1))) },
                    onExit = { pop() },
                )
            }
        }
    }

    val pendingUpdate = update
    if (pendingUpdate != null && !updateDismissed && !showBrand && stack.last() == Screen.Lobby) {
        UpdateDialog(pendingUpdate, updater, onDismiss = { updateDismissed = true })
    }

    if (showBrand) BrandSplash(onFinished = { showBrand = false })

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            containerColor = PlayColors.Dusk,
            title = { Text("要離開 773喵遊樂園嗎？😿", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("遊戲進度都已經保存好了。", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = onExit) { Text("離開", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("再玩一下", color = PlayColors.Sun, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
