package com.purpal.catplayground.nova

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Fixed approved 45-degree view. FX anchors use that frame's source coordinates. */
internal fun DrawScope.novaHangarShip(assets: NovaAssets, key: String, center: Offset, width: Float, breath: Float) {
    val id="turn_${key}_2"
    val bitmap=assets.image(id)
    val anchor=assets.anchor(id)!!
    val scale=width/bitmap.width
    fun at(x: Float,y: Float)=center+Offset((x-anchor.first)*scale,(y-anchor.second)*scale)
    val color=if(key=="orange") Color(0xFFFFA323) else NovaCyan
    val jets=if(key=="orange") listOf(Offset(325f,225f),Offset(350f,254f)) else listOf(Offset(337f,264f),Offset(360f,291f))
    for(jet in jets) {
        val root=at(jet.x,jet.y)
        val tip=root+Offset(width*(.16f+.07f*breath),-width*.045f)
        val normal=Offset(width*.012f,width*.045f)
        val path=Path().apply { moveTo(root.x-normal.x,root.y-normal.y);quadraticTo(tip.x,tip.y-normal.y*.5f,tip.x,tip.y);quadraticTo(tip.x,tip.y+normal.y*.5f,root.x+normal.x,root.y+normal.y);close() }
        drawPath(path,Brush.linearGradient(listOf(color.copy(alpha=.8f*breath),color.copy(alpha=.15f),Color.Transparent),root,tip),blendMode=BlendMode.Plus)
        drawLine(Color.White.copy(alpha=.65f*breath),root,root+(tip-root)*.55f,width*.012f,StrokeCap.Round)
    }
    novaTurn(assets,id,center,width,1f)
    val lights=if(key=="orange") listOf(Offset(118f,270f),Offset(213f,207f),Offset(282f,260f)) else listOf(Offset(141f,268f),Offset(231f,220f),Offset(303f,279f))
    for(light in lights) {
        val p=at(light.x,light.y)
        val radius=width*.035f
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha=.6f*breath),Color.Transparent),p,radius),radius,p,blendMode=BlendMode.Plus)
        drawLine(color.copy(alpha=.35f+.65f*breath),p,p+Offset(width*.025f,-width*.006f),width*.009f,StrokeCap.Round)
    }
}
