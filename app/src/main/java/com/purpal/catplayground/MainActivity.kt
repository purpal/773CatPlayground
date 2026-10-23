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
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Music
import com.purpal.catplayground.audio.SoundManager
import com.purpal.catplayground.data.AppPrefs
import com.purpal.catplayground.lobby.LobbyScreen
import com.purpal.catplayground.shooter.ui.ShooterGameScreen
import com.purpal.catplayground.shooter.ui.ShooterMenuScreen
import com.purpal.catplayground.ui.CatPlaygroundTheme
import com.purpal.catplayground.ui.PlayColors
import com.purpal.catplayground.update.AppUpdater
import com.purpal.catplayground.update.UpdateDialog
import com.purpal.catplayground.update.UpdateInfo
import com.purpal.catplayground.watersort.ui.LevelSelectScreen
import com.purpal.catplayground.watersort.ui.WaterSortScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var sound: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = AppPrefs(applicationContext)
        sound = SoundManager(applicationContext, prefs)
        val updater = AppUpdater(applicationContext)
        setContent {
            CatPlaygroundTheme {
                CompositionLocalProvider(LocalSound provides sound) {
                    AppRoot(prefs, updater, onExit = { finish() })
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
private fun AppRoot(prefs: AppPrefs, updater: AppUpdater, onExit: () -> Unit) {
    val context = LocalContext.current
    val sound = LocalSound.current
    val scope = rememberCoroutineScope()
    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Lobby)) }
    var waterSortCleared by remember { mutableIntStateOf(prefs.waterSortCleared) }
    var shooterCleared by remember { mutableIntStateOf(prefs.shooterCleared) }
    var confirmExit by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { update = updater.check() }

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
                onBack = { pop() },
                onPlay = { stage, cat -> push(Screen.ShooterPlay(stage, cat)) },
            )

            is Screen.ShooterPlay -> ShooterGameScreen(
                stageNumber = screen.stage,
                cat = screen.cat,
                ultimateOnLeft = prefs.shooterUltimateOnLeft,
                bestScore = prefs.shooterBest(screen.stage),
                onCleared = { stage, score ->
                    prefs.setShooterBest(stage, score)
                    if (stage > shooterCleared) {
                        shooterCleared = stage
                        prefs.shooterCleared = stage
                    }
                },
                onNextStage = { replaceTop(Screen.ShooterPlay(screen.stage + 1, screen.cat)) },
                onExit = { pop() },
            )
        }
    }

    val pendingUpdate = update
    if (pendingUpdate != null && !updateDismissed && stack.last() == Screen.Lobby) {
        UpdateDialog(pendingUpdate, updater, onDismiss = { updateDismissed = true })
    }

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
