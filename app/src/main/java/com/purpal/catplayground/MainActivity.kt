package com.purpal.catplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.SoundManager
import com.purpal.catplayground.data.AppPrefs
import com.purpal.catplayground.lobby.LobbyScreen
import com.purpal.catplayground.ui.CatPlaygroundTheme
import com.purpal.catplayground.watersort.ui.LevelSelectScreen
import com.purpal.catplayground.watersort.ui.WaterSortScreen

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var sound: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = AppPrefs(applicationContext)
        sound = SoundManager(prefs)
        setContent {
            CatPlaygroundTheme {
                CompositionLocalProvider(LocalSound provides sound) {
                    AppRoot(prefs)
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
private fun AppRoot(prefs: AppPrefs) {
    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Lobby)) }
    var waterSortCleared by remember { mutableIntStateOf(prefs.waterSortCleared) }

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun replaceTop(screen: Screen) {
        stack = stack.dropLast(1) + screen
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    Crossfade(targetState = stack.last(), label = "screen") { screen ->
        when (screen) {
            Screen.Lobby -> LobbyScreen(
                waterSortCleared = waterSortCleared,
                onOpen = { push(it) },
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
        }
    }
}
