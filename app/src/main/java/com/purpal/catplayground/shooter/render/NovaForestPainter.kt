package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.nova.*
import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

private val jade=Color(0xFF75F4B1)
private val violet=Color(0xFFC593FF)
private val amber=Color(0xFFFFBE66)
private val forestDash=PathEffect.dashPathEffect(floatArrayOf(15f,12f))
private val forestFlash=ColorFilter.tint(Color.White,BlendMode.SrcIn)

internal fun DrawScope.drawNovaForestPart(e:Enemy,w:World,sprites:SpriteCache):Boolean {
    if(e !is NovaSupplyCell && e !is NovaTimeBeacon) return false
    val c=if(e is NovaSupplyCell)jade else violet
    drawSprite(sprites.enemy(e.kind,0),e.x,e.y)
    val maxHp=(if(e is NovaSupplyCell)170f else 120f)*w.difficulty.enemyHp
    drawArc(c,-90f,360f*(e.hp/maxHp).coerceIn(0f,1f),false,Offset(e.x-65f,e.y-65f),Size(130f,130f),style=Stroke(4f))
    if(e.hitFlash>0f) drawSprite(sprites.enemy(e.kind,0),e.x,e.y,alpha=.4f,colorFilter=forestFlash)
    if(e is NovaSupplyCell) {
        e.owner?.let { drawLine(jade.copy(alpha=.3f),Offset(e.x,e.y),Offset(it.x,it.y),3f,pathEffect=forestDash) }
        drawArc(amber,-90f,360f*(e.age/6.4f).coerceIn(0f,1f),false,Offset(e.x-75f,e.y-75f),Size(150f,150f),style=Stroke(2f))
    } else for(gate in 0..2) {
        val a=(50f+gate*40f)*DEG
        val start=Offset(e.x,e.y)+Offset(cos(a),sin(a))*60f
        val end=Offset(e.x,e.y)+Offset(cos(a),sin(a))*150f
        if(e.age<1.4f+gate*.8f) {
            drawLine(amber.copy(alpha=.7f),start,end,3f,pathEffect=forestDash)
            drawSprite(sprites.digit(gate+1),end.x,end.y,scale=.55f)
        }
    }
    return true
}

private fun DrawScope.drawTemporalField(b:NovaSlothBoss,w:World,sprites:SpriteCache) {
    if(!b.fieldActive || w.phase==Phase.BOSS_DEFEATED) return
    val center=Offset(b.fieldX,b.fieldY)
    val r=NovaSlothBoss.FIELD_RADIUS
    drawCircle(violet.copy(alpha=.035f),r,center)
    drawSprite(sprites.novaSprite("SLOTH_FIELD",r*2f),b.fieldX,b.fieldY,alpha=.16f)
    drawCircle(violet.copy(alpha=.5f),r,center,style=Stroke(3f,pathEffect=forestDash))
    for(gate in 0..2) {
        val a=b.gateAngle(gate)
        val dir=Offset(cos(a),sin(a))
        val position=center+dir*(r+24f)
        val released=b.releaseProgress(gate)
        val remaining=b.releaseAt(gate)-b.clock
        val warning=remaining in 0f..1.4f
        val c=if(released>0f)if(b.form==1)amber else jade else if(warning)amber else violet.copy(alpha=.5f)
        drawArc(c,a/DEG-14f,28f,false,center-Offset(r,r),Size(r*2f,r*2f),style=Stroke(if(released>0f)9f else 4f))
        if(released<=0f) {
            val order=if(b.cycle%2==0)gate+1 else 3-gate
            drawSprite(sprites.digit(order),position.x,position.y,scale=.7f,colorFilter=ColorFilter.tint(c,BlendMode.SrcIn))
            if(warning) drawCircle(c,22f+(remaining/1.4f)*20f,position,style=Stroke(2f))
        } else {
            val side=Offset(-dir.y,dir.x)
            val tip=position+dir*(8f+sin(w.time*8f)*5f)
            drawLine(c,tip-dir*18f+side*12f,tip,4f)
            drawLine(c,tip-dir*18f-side*12f,tip,4f)
        }
    }
}

internal fun DrawScope.drawNovaForestBoss(b:NovaVehicleBoss,w:World,sprites:SpriteCache,flash:Boolean) {
    val panda=b as? NovaPandaBoss
    val sloth=b as? NovaSlothBoss
    val live=w.phase!=Phase.BOSS_DEFEATED && !b.transforming
    if(sloth!=null) {
        drawTemporalField(sloth,w,sprites)
        if(b.form==0) for(side in listOf(-1f,1f)) drawLine(violet.copy(alpha=.35f),Offset(b.x+side*110f,b.y-70f),Offset(b.x+side*170f,-80f),5f)
    }
    if(panda!=null) {
        drawOval(Color.Black.copy(alpha=.4f),Offset(b.x-150f,b.y-85f),Size(300f,200f))
        val route=panda.route
        if(live && route!=null && panda.motion in listOf(NovaPandaBoss.Motion.WARN,NovaPandaBoss.Motion.ROLL)) {
            val path=Path()
            for(i in 0..76) {
                val(x,y)=route.at(i*NovaPandaBoss.ROLL/76f)
                if(i==0)path.moveTo(x,y)else path.lineTo(x,y)
            }
            drawPath(path,amber.copy(alpha=.055f),style=Stroke(b.radius*2f))
            drawPath(path,amber.copy(alpha=.65f),style=Stroke(4f,pathEffect=forestDash))
            val end=route.at(NovaPandaBoss.ROLL)
            drawCircle(amber.copy(alpha=.7f),b.radius,Offset(end.first,end.second),style=Stroke(3f,pathEffect=forestDash))
        }
    }
    val width=if(panda!=null && b.form==1)440f else 530f
    val rotation=if(live && panda?.motion==NovaPandaBoss.Motion.ROLL)panda.clock*260f else 0f
    if(b.transforming) {
        val t=(1f-b.armorTime/Boss.ARMOR_TIME).coerceIn(0f,1f)
        drawSprite(sprites.novaSprite("${b.artPrefix}_BASE",530f),b.x,b.y,alpha=(1f-t*2f).coerceIn(0f,1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_TRANSITION",530f),b.x,b.y,alpha=1f-abs(t*2f-1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_DEPLOYED",if(panda!=null)440f else 530f),b.x,b.y,alpha=(t*2f-1f).coerceIn(0f,1f))
    } else drawSprite(sprites.novaSprite(b.artId,width),b.x,b.y,rotationDegrees=rotation)
    if(flash) drawSprite(sprites.novaSprite(b.artId,width),b.x,b.y,rotationDegrees=rotation,alpha=.35f,colorFilter=forestFlash)
    if(live && (panda?.cooling==true || sloth?.cooling==true)) {
        drawSprite(sprites.novaSprite("${b.artPrefix}_CORE",120f),b.x,b.y,alpha=.85f+sin(w.time*7f)*.15f)
        drawCircle(if(panda!=null)jade else violet,75f,Offset(b.x,b.y),style=Stroke(4f,pathEffect=forestDash))
    }
    if(live && panda?.motion==NovaPandaBoss.Motion.COOL && panda.clock<.6f) {
        drawSprite(sprites.novaSprite("PANDA_SHOCK",280f),b.x,b.y,scale=1f+panda.clock*2f,alpha=1f-panda.clock/.6f)
    }
}
