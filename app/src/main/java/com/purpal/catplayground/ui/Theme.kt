package com.purpal.catplayground.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object PlayColors {
    val Night = Color(0xFF1B1446)
    val Dusk = Color(0xFF3A2A8C)
    val Berry = Color(0xFF7B3FC4)
    val Sun = Color(0xFFFFC94A)
    val Mint = Color(0xFF5BE3C4)
    val Card = Color(0x33FFFFFF)
    val CardStrong = Color(0x55FFFFFF)
}

@Composable
fun CatPlaygroundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PlayColors.Sun,
            onPrimary = PlayColors.Night,
            secondary = PlayColors.Mint,
            background = PlayColors.Night,
            surface = PlayColors.Dusk,
            onSurface = Color.White,
            onBackground = Color.White,
        ),
        content = content,
    )
}

@Composable
fun PlayBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PlayColors.Berry, PlayColors.Dusk, PlayColors.Night))),
        content = content,
    )
}
