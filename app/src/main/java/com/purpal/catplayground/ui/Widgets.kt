package com.purpal.catplayground.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.ConcurrentHashMap
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
        FitText(text, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 單行文字：放不下時自動縮小字體，不會被擠成兩行或截斷
 * （手機設定超大字體、或螢幕比較窄時都適用）。
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    var scale by remember(text, fontSize) { mutableFloatStateOf(1f) }
    var ready by remember(text, fontSize) { mutableStateOf(false) }
    val size = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize
    Text(
        text,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        color = color,
        fontSize = size * scale,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        style = style,
        onTextLayout = { result ->
            if (result.didOverflowWidth && scale > MIN_TEXT_SCALE) scale *= 0.9f else ready = true
        },
    )
}

private const val MIN_TEXT_SCALE = 0.45f

/** assets/art/ 裡的圖（tools/art 產生），解碼一次後重複使用。 */
object ArtImages {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(context: Context, name: String): ImageBitmap = cache.getOrPut(name) {
        context.assets.open("art/$name.png").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
    }
}

@Composable
fun ArtImage(name: String, modifier: Modifier = Modifier, contentDescription: String? = null, alpha: Float = 1f) {
    val context = LocalContext.current
    val image = remember(name) { ArtImages.get(context, name) }
    Image(image, contentDescription, modifier, contentScale = ContentScale.Fit, alpha = alpha)
}
