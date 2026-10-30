package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.nova.*
import com.purpal.catplayground.shooter.model.*
import kotlin.math.cos
import kotlin.math.sin

private val mint = Color(0xFF9CFFC8)
private val aqua = Color(0xFF6CEAFF)
private val warning = Color(0xFFFFAE57)
private val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
private val white = ColorFilter.tint(Color.White, BlendMode.SrcIn)

/** Returns true for Nova-owned parts so they never fall through to the original art. */
internal fun DrawScope.drawNovaHarborPart(e: Enemy, w: World, sprites: SpriteCache): Boolean {
    if (e is NovaOtterHull || e is NovaKoalaHatch) return true // drawn with their owner
    val capacitor = e as? NovaCapacitor
    val buoy = e as? NovaSonarBuoy
    if (capacitor == null && buoy == null) return false
    val tint = if (capacitor != null) mint else aqua
    val center = Offset(e.x, e.y)
    val charging = capacitor?.warning ?: buoy!!.warning
    val aim = capacitor?.aim ?: buoy!!.aim
    if (charging) {
        val end = center + Offset(cos(aim), sin(aim)) * 430f
        drawLine(warning.copy(alpha=.65f), center, end, 4f, pathEffect=dash)
        drawCircle(warning, e.radius + 10f, center, style=Stroke(3f, pathEffect=dash))
    }
    capacitor?.owner?.let { drawLine(mint.copy(alpha=.25f), Offset(it.x,it.y), center, 3f, pathEffect=dash) }
    val scale = if (capacitor != null && capacitor.discharged) .7f else 1f
    drawSprite(sprites.enemy(e.kind,0),e.x,e.y,scale=scale)
    drawArc(tint,-90f,360f*(e.hp / if(capacitor!=null)180f*w.difficulty.enemyHp else 120f*w.difficulty.enemyHp).coerceIn(0f,1f),false,
        center-Offset(61f,61f),Size(122f,122f),style=Stroke(4f))
    if(e.hitFlash>0f) drawSprite(sprites.enemy(e.kind,0),e.x,e.y,alpha=.4f,colorFilter=white)
    if(capacitor?.discharged==true) drawSprite(sprites.novaSprite("KOALA_PULSE",150f),e.x,e.y,
        scale=1f+(e.age-NovaCapacitor.FIRE)*3f,alpha=(1f-(e.age-NovaCapacitor.FIRE)/.7f).coerceIn(0f,1f))
    if(buoy!=null) drawCircle(aqua.copy(alpha=.3f),45f+(e.age%1f)*50f,center,style=Stroke(2f))
    return true
}

private fun DrawScope.weakHatch(sprites:SpriteCache,id:String,x:Float,y:Float,time:Float) {
    drawSprite(sprites.novaSprite(id,105f),x,y,alpha=.85f+sin(time*8f)*.15f)
    val c=if(id.startsWith("KOALA"))mint else aqua
    for(side in listOf(-1f,1f)) {
        val px=x+side*66f
        drawLine(c,Offset(px,y-40f),Offset(px,y+40f),4f)
        drawLine(c,Offset(px,y-40f),Offset(px-side*17f,y-40f),4f)
        drawLine(c,Offset(px,y+40f),Offset(px-side*17f,y+40f),4f)
    }
}

internal fun DrawScope.drawNovaHarborBoss(b:NovaVehicleBoss,w:World,sprites:SpriteCache,flash:Boolean) {
    val live=w.phase!=Phase.BOSS_DEFEATED && !b.transforming
    val koala=b as? NovaKoalaBoss
    val otter=b as? NovaOtterBoss
    val width=if(koala!=null)570f else 680f
    if(koala!=null) {
        drawOval(Color.Black.copy(alpha=.4f),Offset(b.x-200f,b.y-145f),Size(400f,310f))
        if(b.form==0) for(side in listOf(-1f,1f)) {
            drawLine(mint.copy(alpha=.25f),Offset(b.x+side*165f,b.y-100f),Offset(b.x+side*235f,-80f),5f)
        }
    }
    if(otter!=null) {
        for(side in listOf(-1f,1f)) {
            val hx=otter.hullX(side)
            drawOval(aqua.copy(alpha=.12f),Offset(hx-55f,b.y-260f),Size(110f,340f))
        }
        if(live && otter.diving) {
            // Both the current trail and the fixed resurfacing destinations stay visible.
            drawLine(aqua.copy(alpha=.5f),Offset(b.x-235f,b.y),Offset(b.x+235f,b.y),4f,pathEffect=dash)
            for(side in listOf(-1f,1f)) {
                drawSprite(sprites.novaSprite("OTTER_WAKE",190f),b.x+side*235f,b.y,alpha=.65f)
                drawCircle(aqua,100f,Offset(b.x+side*235f,b.y),style=Stroke(3f,pathEffect=dash))
            }
        }
        if(live && otter.clock in .2f..1.6f) {
            val hx=otter.batteryX(otter.activeSide)
            for(dx in listOf(-60f,60f)) drawLine(warning.copy(alpha=.55f),Offset(hx+dx,b.y+100f),Offset(hx+dx,w.height),3f,pathEffect=dash)
        }
    }
    if(b.transforming) {
        val t=(1f-b.armorTime/Boss.ARMOR_TIME).coerceIn(0f,1f)
        if(otter!=null) {
            val separation=235f-85f*t
            for(side in listOf(-1f,1f)) drawSprite(sprites.novaSprite(if(side<0)"OTTER_PORT"else"OTTER_STARBOARD",230f),
                b.x+side*separation,b.y,alpha=(1f-t*1.5f).coerceIn(0f,1f))
        } else drawSprite(sprites.novaSprite("${b.artPrefix}_BASE",width),b.x,b.y,alpha=(1f-t*2f).coerceIn(0f,1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_TRANSITION",width),b.x,b.y,alpha=1f-kotlin.math.abs(t*2f-1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_DEPLOYED",width),b.x,b.y,alpha=(t*2f-1f).coerceIn(0f,1f))
        drawCircle(aqua.copy(alpha=.45f),180f+t*170f,Offset(b.x,b.y),style=Stroke(4f))
    } else if(otter!=null && b.form==0) {
        for(side in listOf(-1f,1f)) {
            val id=if(side<0)"OTTER_PORT"else"OTTER_STARBOARD"
            val alpha=if(otter.diving).25f else 1f
            drawSprite(sprites.novaSprite(id,230f),otter.hullX(side),b.y,alpha=alpha)
            if(flash || otter.hulls.any { it.side==side && it.hitFlash>0f }) drawSprite(sprites.novaSprite(id,230f),otter.hullX(side),b.y,alpha=.35f,colorFilter=white)
        }
    } else {
        drawSprite(sprites.novaSprite(b.artId,width),b.x,b.y)
        if(flash || otter?.hulls?.any { it.hitFlash>0f }==true) drawSprite(sprites.novaSprite(b.artId,width),b.x,b.y,alpha=.35f,colorFilter=white)
    }
    if(live && koala?.cooling==true) {
        val hx=b.x+if(b.form==1)koala.weakSide*205f else 0f
        weakHatch(sprites,"KOALA_HATCH",hx,b.y+55f,w.time)
    }
    if(live && otter?.cooling==true) weakHatch(sprites,"OTTER_HATCH",otter.hullX(-otter.activeSide),b.y+55f,w.time)
}
