package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.nova.*
import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

private val finalGold=Color(0xFFFFBA68)
private val finalCyan=Color(0xFF75DDFF)
private val finalDash=PathEffect.dashPathEffect(floatArrayOf(15f,12f))
internal fun DrawScope.drawNovaFinalPart(e:Enemy,w:World,sprites:SpriteCache):Boolean {
    when(e) {
        is NovaBore -> {
            val progress=(e.age/(NovaBore.WARN+e.delay)).coerceIn(0f,1f)
            val center=Offset(e.x,e.y);val r=NovaBore.RADIUS
            if(e.fieldActive) {
                drawSprite(sprites.novaSprite("PIG_FIELD",r*2),e.x,e.y,alpha=.4f)
                drawCircle(finalCyan.copy(alpha=.2f),r,center)
            } else {
                drawCircle(finalGold.copy(alpha=.07f),r,center)
                drawCircle(finalGold,r,center,style=Stroke(3f,pathEffect=finalDash))
                drawArc(finalGold,-90f,360f*progress,false,center-Offset(r,r),Size(r*2,r*2),style=Stroke(5f))
                drawSprite(sprites.digit(if(e.delay<.1f)1 else if(e.delay<1f)2 else 3),e.x,e.y,scale=.8f)
            }
            if(e.erupting)drawSprite(sprites.novaSprite("PIG_BORE",r*2),e.x,e.y,alpha=.9f)
        }
        is NovaRailGate -> {
            if(!e.active) {
                drawLine(finalCyan.copy(alpha=.2f),Offset(e.x,e.y),Offset(e.x,w.height),3f,pathEffect=finalDash)
                drawCircle(finalGold,46f,Offset(e.x,e.y),style=Stroke(3f,pathEffect=finalDash))
            }
            drawSprite(sprites.novaSprite("MOUSE_BATTERY",76f),e.x,e.y,alpha=if(e.active)1f else .45f)
        }
        is NovaDrillArm, is NovaRailBattery -> {
            val id=if(e is NovaDrillArm)"PIG_ARM"else"MOUSE_BATTERY"
            drawSprite(sprites.novaSprite(id,120f),e.x,e.y)
            val hp=(if(e is NovaDrillArm)250f else 240f)*w.difficulty.enemyHp
            drawArc(finalCyan,-90f,360f*(e.hp/hp).coerceIn(0f,1f),false,Offset(e.x-65,e.y-65),Size(130f,130f),style=Stroke(4f))
        }
        else -> return false
    }
    return true
}
internal fun DrawScope.drawNovaFinalBoss(b:NovaVehicleBoss,w:World,sprites:SpriteCache,flash:Boolean) {
    val mouse=b as? NovaMouseBoss
    val pig=b as? NovaPigBoss
    val width=530f
    val id=if(mouse?.awakened==true)"MOUSE_DEPLOYED" else if(mouse!=null&&b.form==1)"MOUSE_TRANSITION" else b.artId
    if(b.transforming) {
        val t=(1f-b.armorTime/Boss.ARMOR_TIME).coerceIn(0f,1f)
        drawSprite(sprites.novaSprite("${b.artPrefix}_BASE",width),b.x,b.y,alpha=1f-t)
        drawSprite(sprites.novaSprite(if(mouse!=null)"MOUSE_TRANSITION"else"PIG_TRANSITION",width),b.x,b.y,alpha=sin(t*Math.PI.toFloat()))
        drawSprite(sprites.novaSprite(if(mouse!=null&&!mouse.awakened)"MOUSE_TRANSITION"else"${b.artPrefix}_DEPLOYED",width),b.x,b.y,alpha=t)
    } else drawSprite(sprites.novaSprite(id,width),b.x,b.y)
    if(flash)drawSprite(sprites.novaSprite(id,width),b.x,b.y,alpha=.35f,colorFilter=ColorFilter.tint(Color.White,BlendMode.SrcIn))
    if(pig?.cooling==true||mouse?.cooling==true)drawCircle(finalCyan,55f,Offset(b.x,b.y),style=Stroke(4f,pathEffect=finalDash))
    if(mouse!=null&&w.phase!=Phase.BOSS_DEFEATED) {
        if(mouse.awakened) {
            val time=if(b.transforming)b.armorTime/Boss.ARMOR_TIME else mouse.awakeningTime/1.6f
            drawSprite(sprites.novaSprite("MOUSE_TOTEM",if(time>0f)250f else 100f),b.x,b.y-70f,scale=if(time>0f)1f+(1f-time)*5f else 1f,alpha=if(time>0f)time*.65f else .6f)
        }
        if(!b.transforming&&mouse.awakeningTime<=0f&&mouse.clock<3.5f) {
            drawLine(finalCyan.copy(alpha=.35f),Offset(mouse.gapX,560f),Offset(mouse.gapX,w.height),4f,pathEffect=finalDash)
        }
    }
}
