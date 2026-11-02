package com.purpal.catplayground.nova

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.purpal.catplayground.shooter.model.*
import com.purpal.catplayground.shooter.render.*
import kotlin.math.*

/** A translucent awakening pulse, like the bosses' expanded totems; never an opaque cut-in. */
fun DrawScope.drawNovaOverdrive(world: World, sprites: SpriteCache) {
    if(world.ultimateCinematic<=0f)return
    val t=(1f-world.ultimateCinematic/World.ULTIMATE_CINEMATIC).coerceIn(0f,1f)
    val vp=viewportFor(size,world.height)
    val origin=Offset(vp.offsetX+world.player.x*vp.scale,vp.offsetY+world.player.y*vp.scale)
    val center=origin+Offset(0f,-size.height*.18f*t)
    val color=if(world.player.cat==CatType.ORANGE)Color(0xFFFFB65C)else Color(0xFFB399FF)
    val alpha=NovaOverdrivePulse.alpha(t)
    val diameter=100f*vp.scale+size.maxDimension*2.2f*(1f-(1f-t).pow(2))
    val radius=diameter*.5f
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha=alpha*.35f),Color.Transparent),center,radius),radius,center)
    drawCircle(color.copy(alpha=alpha*.6f),radius*.68f,center,style=Stroke(3f*vp.scale))
    drawCircle(color.copy(alpha=alpha*.3f),radius*.82f,center,style=Stroke(vp.scale))
    novaImage(sprites.novaTotem(world.player.cat).image,center,diameter,alpha)
}

object NovaOverdrivePulse {
    fun alpha(t:Float):Float {
        val p=t.coerceIn(0f,1f)
        return .48f*(p/.08f).coerceAtMost(1f)*(1f-p).pow(1.5f)
    }
}
