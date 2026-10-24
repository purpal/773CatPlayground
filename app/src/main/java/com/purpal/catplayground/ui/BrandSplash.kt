package com.purpal.catplayground.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.R
import kotlinx.coroutines.delay

/** 開發商品牌色（logo 主色）與略暗的白底。 */
val BrandBlue = Color(0xFF0088FE)
val BrandBackground = Color(0xFFF2F3F5)

private val RoundedFont = FontFamily(Font(R.font.nunito_extrabold, FontWeight.ExtraBold))

/** 啟動時的開發商品牌頁：淡入、停留一下再淡出；點一下可以跳過。 */
@Composable
fun BrandSplash(onFinished: () -> Unit) {
    // 整頁在淡出前都不透明，蓋住後面已經載入的大廳；淡入只作用在 logo 與文字。
    val alpha = remember { Animatable(1f) }
    val content = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        content.animateTo(1f, tween(450))
        delay(1100)
        alpha.animateTo(0f, tween(500))
        onFinished()
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha.value)
            .background(BrandBackground)
            .pointerInput(Unit) { detectTapGestures { onFinished() } },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                this.alpha = content.value
                val s = 0.92f + 0.08f * content.value
                scaleX = s
                scaleY = s
            },
        ) {
            ArtImage("brand_logo", Modifier.size(150.dp), contentDescription = "Zuipower Studio")
            Spacer(Modifier.height(20.dp))
            Text("Zuipower Studio", color = BrandBlue, fontFamily = RoundedFont, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        }
    }
}
