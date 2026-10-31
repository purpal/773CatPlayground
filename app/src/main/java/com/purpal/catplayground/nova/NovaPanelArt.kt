package com.purpal.catplayground.nova

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*

/** Nine-slice artwork preserves metal corners; the HUD reserves the same corners for content. */
@Composable
fun NovaPanelArt(id:String,modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit) {
    val assets=NovaAssets.get(LocalContext.current.assets)
    val bitmap=remember(id){assets.image(id)}
    val hud=id=="UI_HUD"
    val cuts=remember(bitmap,hud){NovaPanelMetrics.cuts(bitmap.width,bitmap.height,hud)}
    val density=LocalDensity.current
    BoxWithConstraints(modifier.background(NovaInk)) {
        val insets=NovaPanelMetrics.insets(with(density){maxWidth.toPx()},bitmap.width,cuts)
        Canvas(Modifier.matchParentSize()) {
            val edge=NovaPanelMetrics.insets(size.width,bitmap.width,cuts)
            // Also tolerate a short non-HUD panel without reversing its center slice.
            val fit=minOf(1f,size.height/(edge.top+edge.bottom).coerceAtLeast(1f))
            val sx=intArrayOf(0,cuts.side,bitmap.width-cuts.side,bitmap.width)
            val sy=intArrayOf(0,cuts.top,bitmap.height-cuts.bottom,bitmap.height)
            val dx=intArrayOf(0,edge.side.toInt(),(size.width-edge.side).toInt(),size.width.toInt())
            val dy=intArrayOf(0,(edge.top*fit).toInt(),(size.height-edge.bottom*fit).toInt(),size.height.toInt())
            for(y in 0..2)for(x in 0..2) {
                val w=dx[x+1]-dx[x];val h=dy[y+1]-dy[y]
                if(w>0 && h>0) drawImage(bitmap,srcOffset=IntOffset(sx[x],sy[y]),srcSize=IntSize(sx[x+1]-sx[x],sy[y+1]-sy[y]),dstOffset=IntOffset(dx[x],dy[y]),dstSize=IntSize(w,h))
            }
        }
        if(hud) {
            val padding=with(density){PaddingValues(start=insets.side.toDp()+4.dp,end=insets.side.toDp()+4.dp,
                top=insets.top.toDp()+4.dp,bottom=insets.bottom.toDp()+4.dp)}
            Box(Modifier.fillMaxWidth().padding(padding).clipToBounds(),content=content)
        } else content()
    }
}
