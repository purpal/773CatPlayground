package com.purpal.catplayground.nova

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.purpal.catplayground.shooter.model.*
import com.purpal.catplayground.shooter.render.*
import kotlin.math.*

/** Drawn on the game frame clock: the seal originates at the actual ship position. */
fun DrawScope.drawNovaOverdrive(world: World, sprites: SpriteCache) {
    if(world.ultimateCinematic<=0f) return
    val t=(1f-world.ultimateCinematic/World.ULTIMATE_CINEMATIC).coerceIn(0f,1f)
    val vp=viewportFor(size,world.height)
    val origin=Offset(vp.offsetX+world.player.x*vp.scale,vp.offsetY+world.player.y*vp.scale)
    val center=origin+Offset(0f,-size.height*.28f*t)
    val color=if(world.player.cat==CatType.ORANGE) Color(0xFFFFA724) else Color(0xFF9E79FF)
    val appear=(t/.12f).coerceIn(0f,1f)
    val fade=((1f-t)/.12f).coerceIn(0f,1f)
    // Monotonic growth from a ship-sized emblem to beyond every viewport edge.
    val diameter=72f*vp.scale+size.maxDimension*4f*t.pow(2.2f)
    val radius=diameter*.5f
    drawRect(NovaInk.copy(alpha=.65f*appear*fade))
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha=.35f*appear*fade),Color.Transparent),center,radius*1.25f),radius*1.25f,center)
    rotate(-t*55f,center) {
        for(r in listOf(.88f,1.08f,1.18f)) drawCircle(color.copy(alpha=.6f*appear*fade),radius*r,center,style=Stroke(2f+vp.scale*2f))
        // Engraved radial marks suggest an ancient seal waking inside a machine.
        repeat(32) { i ->
            val a=i*2f*PI.toFloat()/32
            val v=Offset(cos(a),sin(a))
            drawLine(color.copy(alpha=.8f*appear*fade),center+v*radius*1.09f,center+v*radius*(if(i%4==0)1.26f else 1.16f),2f+vp.scale)
        }
    }
    val emblem=sprites.novaTotem(world.player.cat).image
    novaImage(emblem,center,diameter,appear*fade)
    // The seal passes the camera; its final pulse leads into the existing blast.
    if(t>.88f) drawRect(color.copy(alpha=((t-.88f)/.12f)*.35f))
}
