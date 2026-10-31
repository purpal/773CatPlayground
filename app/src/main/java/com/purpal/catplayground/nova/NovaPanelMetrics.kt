package com.purpal.catplayground.nova

/** Shared source cuts and destination insets keep HUD content inside the actual nine-slice art. */
object NovaPanelMetrics {
    data class Cuts(val side:Int,val top:Int,val bottom:Int)
    data class Insets(val side:Float,val top:Float,val bottom:Float)
    fun cuts(width:Int,height:Int,hud:Boolean) = Cuts(
        (width*if(hud).13f else .11f).toInt(),
        (height*if(hud).36f else .24f).toInt(),
        (height*if(hud).22f else .17f).toInt())
    fun insets(width:Float,bitmapWidth:Int,cuts:Cuts):Insets {
        val scale=(width/bitmapWidth).coerceAtMost(1.6f)
        return Insets(cuts.side*scale,cuts.top*scale,cuts.bottom*scale)
    }
}
