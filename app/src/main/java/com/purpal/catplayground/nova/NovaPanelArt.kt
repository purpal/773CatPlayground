package com.purpal.catplayground.nova

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Nine-slice artwork preserves metal corners while the live content changes size. */
@Composable
fun NovaPanelArt(id: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val assets=NovaAssets.get(LocalContext.current.assets)
    val bitmap=remember(id) { assets.image(id) }
    Box(modifier.background(NovaInk)) {
        Canvas(Modifier.matchParentSize()) {
            val scale=(size.width/bitmap.width).coerceAtMost(1.6f)
            val left=(bitmap.width*.11f).toInt()
            val top=(bitmap.height*.24f).toInt()
            val bottom=(bitmap.height*.17f).toInt()
            val sx=intArrayOf(0,left,bitmap.width-left,bitmap.width)
            val sy=intArrayOf(0,top,bitmap.height-bottom,bitmap.height)
            val dx=intArrayOf(0,(left*scale).toInt(),(size.width-left*scale).toInt(),size.width.toInt())
            val dy=intArrayOf(0,(top*scale).toInt(),(size.height-bottom*scale).toInt(),size.height.toInt())
            for(y in 0..2)for(x in 0..2) {
                val w=dx[x+1]-dx[x];val h=dy[y+1]-dy[y]
                if(w>0 && h>0) drawImage(bitmap,srcOffset=IntOffset(sx[x],sy[y]),srcSize=IntSize(sx[x+1]-sx[x],sy[y+1]-sy[y]),dstOffset=IntOffset(dx[x],dy[y]),dstSize=IntSize(w,h))
            }
        }
        content()
    }
}
