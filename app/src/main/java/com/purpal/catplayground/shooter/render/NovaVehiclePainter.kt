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
import kotlin.math.sin

private val iceBlue = Color(0xFF75DFFF)
private val warningRed = Color(0xFFFF6B45)
private val flashWhite = ColorFilter.tint(Color.White, BlendMode.SrcIn)
private val warningDash = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))

internal fun DrawScope.drawNovaIcePylon(e: NovaIcePylon, w: World, sprites: SpriteCache) {
    if (!e.alive) return
    val thaw = ((e.age - (e.lifetime - .6f)) / .6f).coerceIn(0f, 1f)
    val center = Offset(e.x, e.y)
    if (e.age < NovaIcePylon.WARN) {
        drawCircle(warningRed.copy(alpha=.12f), e.radius, center)
        drawCircle(warningRed, e.radius, center, style=Stroke(3f,pathEffect=warningDash))
        drawArc(warningRed, -90f, 360f * e.age / NovaIcePylon.WARN, false,
            Offset(e.x-65f,e.y-65f),Size(130f,130f),style=Stroke(5f))
    }
    val grow = (e.age / NovaIcePylon.WARN).coerceIn(.25f, 1f)
    drawSprite(sprites.enemy(e.kind,0),e.x,e.y,scale=grow,alpha=(1f-thaw)*if(e.active)1f else .45f)
    if(e.active) drawCircle(iceBlue, e.radius, center, style=Stroke(3f))
    if(e.hitFlash>0f) drawSprite(sprites.enemy(e.kind,0),e.x,e.y,alpha=.4f,colorFilter=flashWhite)
    if(thaw>0f) drawSprite(sprites.novaSprite("PENGUIN_THAW",150f),e.x,e.y,scale=1f+thaw,alpha=1f-thaw)
}

private fun DrawScope.landingMark(x:Float,y:Float,r:Float,number:Int,progress:Float,sprites:SpriteCache,secondary:Boolean=false) {
    val c=if(secondary) warningRed.copy(alpha=.45f) else warningRed
    drawCircle(c.copy(alpha=if(secondary).05f else .12f),r,Offset(x,y))
    drawCircle(c,r,Offset(x,y),style=Stroke(4f,pathEffect=warningDash))
    drawArc(c,-90f,360f*progress.coerceIn(0f,1f),false,Offset(x-r,y-r),Size(r*2,r*2),style=Stroke(7f))
    drawSprite(sprites.digit(number),x,y,scale=1.1f,colorFilter=ColorFilter.tint(c,BlendMode.SrcIn))
}

internal fun DrawScope.drawNovaVehicle(b: NovaVehicleBoss,w:World,sprites:SpriteCache,flash:Boolean) {
    val defeated=w.phase==Phase.BOSS_DEFEATED
    val rabbit=b as? NovaRabbitBoss
    val penguin=b as? NovaPenguinBoss
    val live=!defeated && !b.transforming
    if(live && rabbit!=null && rabbit.motion in listOf(NovaRabbitBoss.Motion.LOCK,NovaRabbitBoss.Motion.FLIGHT)) {
        landingMark(rabbit.targetX,rabbit.targetY,rabbit.landingRadius,rabbit.jumpNumber,
            if(rabbit.motion==NovaRabbitBoss.Motion.FLIGHT)1f else rabbit.clock/NovaRabbitBoss.WARN,sprites)
        if(rabbit.form==1 && rabbit.jumpNumber==1) landingMark(rabbit.nextX,rabbit.nextY,rabbit.landingRadius,2,0f,sprites,true)
    }
    if(penguin!=null) {
        // Wake gives the naval silhouette a clear surface connection, without extra collisions.
        for(side in listOf(-1f,1f)) drawOval(iceBlue.copy(alpha=.13f),Offset(b.x+side*120f-45f,b.y-240f),Size(90f,280f))
        if(live && penguin.clock<4.4f) {
            val gap=Offset(penguin.gapX,maxOf(620f,w.height*.49f))
            drawLine(iceBlue.copy(alpha=.5f),gap-Offset(0f,90f),gap+Offset(0f,90f),5f,pathEffect=warningDash)
            if(b.form==1) {
                val second=Offset(penguin.secondGapX,maxOf(820f,w.height*.64f))
                drawLine(iceBlue.copy(alpha=.4f),gap,second,4f,pathEffect=warningDash)
            }
        }
    } else {
        drawOval(Color.Black.copy(alpha=.45f),Offset(b.x-145f,b.y-90f),Size(290f,200f))
        if(live && rabbit?.motion==NovaRabbitBoss.Motion.FLIGHT) {
            val jet=sin(rabbit.flight * HALF_PI * 2f).coerceAtLeast(0f)
            for(side in listOf(-1f,1f)) drawSprite(sprites.novaSprite("RABBIT_EMITTER",100f),b.x+side*95f,b.y+100f,scale=.5f+jet,alpha=.7f)
        }
    }
    val width=if(rabbit!=null)450f else 540f
    val lift=if(live && rabbit?.motion==NovaRabbitBoss.Motion.FLIGHT)sin(rabbit.flight*HALF_PI*2f)*.12f else 0f
    val art=sprites.novaSprite(b.artId,width)
    if(b.transforming) {
        val t=(1f-b.armorTime/Boss.ARMOR_TIME).coerceIn(0f,1f)
        // Mechanical intermediate pose, cross-faded on both ends instead of a sprite pop.
        drawSprite(sprites.novaSprite("${b.artPrefix}_BASE",width),b.x,b.y,alpha=(1f-t*2f).coerceIn(0f,1f))
        drawSprite(art,b.x,b.y,alpha=1f-kotlin.math.abs(t*2f-1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_DEPLOYED",width),b.x,b.y,alpha=(t*2f-1f).coerceIn(0f,1f))
        drawCircle(iceBlue.copy(alpha=.6f),140f+t*160f,Offset(b.x,b.y),style=Stroke(5f))
    } else drawSprite(art,b.x,b.y,scale=1f+lift)
    if(flash || defeated && (w.time*8f).toInt()%2==0) drawSprite(art,b.x,b.y,scale=1f+lift,alpha=.35f,colorFilter=flashWhite)
    val cooling=live && (rabbit?.cooling==true || penguin?.cooling==true)
    if(cooling) {
        val pulse=.6f+sin(w.time*8f)*.2f
        drawCircle(iceBlue.copy(alpha=pulse),42f,Offset(b.x,b.y+45f),style=Stroke(6f))
        drawLine(iceBlue,Offset(b.x-60f,b.y+45f),Offset(b.x-35f,b.y+45f),5f)
        drawLine(iceBlue,Offset(b.x+35f,b.y+45f),Offset(b.x+60f,b.y+45f),5f)
    }
    if(live && rabbit?.cooling==true && rabbit.clock<.6f) {
        drawSprite(sprites.novaSprite("RABBIT_SHOCK",rabbit.landingRadius*2f),b.x,b.y,
            scale=1f+rabbit.clock*1.8f,alpha=(1f-rabbit.clock/.6f)*.75f)
    }
}
