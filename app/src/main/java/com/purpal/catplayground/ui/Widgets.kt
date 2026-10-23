package com.purpal.catplayground.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx

/** 圓角膠囊按鈕，按下會有點擊音效。 */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = PlayColors.Card,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val sound = LocalSound.current
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) {
                sound.play(Sfx.CLICK)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
