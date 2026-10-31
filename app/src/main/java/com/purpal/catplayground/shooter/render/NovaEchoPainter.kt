package com.purpal.catplayground.shooter.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.purpal.catplayground.nova.*
import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

private val echoCyan=Color(0xFF75DEFF)
private val echoGold=Color(0xFFFFBD63)
private val echoDash=PathEffect.dashPathEffect(floatArrayOf(14f,12f))
private val echoFlash=ColorFilter.tint(Color.White,BlendMode.SrcIn)

/** The warning uses the captured target and weapon, never the player's later loadout. */
private fun DrawScope.mimicWarning(s:NovaMimicSnapshot,x:Float,y:Float,replay:Boolean=false) {
    val aim=angleTo(x,y,s.targetX,s.targetY)
    val start=Offset(x,y)
    val length=sqrt(dist2(x,y,s.targetX,s.targetY)).coerceAtLeast(180f)
    fun ray(a:Float,width:Float=3f) {
        drawLine(echoGold.copy(alpha=.65f),start,start+Offset(cos(a),sin(a))*length,width,pathEffect=echoDash)
    }
    when(s.weapon) {
        MainWeapon.SPREAD -> for(k in -(if(replay)1 else 2)..(if(replay)1 else 2)) ray(aim+k*12f*DEG)
        MainWeapon.FOCUS -> { ray(aim,5f);drawCircle(echoGold,24f,Offset(s.targetX,s.targetY),style=Stroke(3f)) }
        MainWeapon.SPECIAL -> if(s.cat==CatType.ORANGE) {
            for(k in -(if(replay)0 else 1)..(if(replay)0 else 1)) {
                val a=aim+k*10f*DEG;ray(a)
                drawCircle(echoGold,if(replay)20f else 28f,start+Offset(cos(a),sin(a))*100f,style=Stroke(3f))
            }
        } else {
            // Open downward arc previews the gap in the radial discharge.
            drawArc(echoGold,120f,300f,false,start-Offset(90f,90f),Size(180f,180f),style=Stroke(4f,pathEffect=echoDash))
        }
    }
}

internal fun DrawScope.drawNovaEchoPart(e:Enemy,w:World,sprites:SpriteCache):Boolean {
    if(e is NovaHologram) {
        drawSprite(sprites.novaSprite("RACCOON_HOLOGRAM",240f),e.x,e.y,alpha=.45f+sin(w.time*16f)*.1f)
        for(i in -4..4) drawLine(echoCyan.copy(alpha=.4f),Offset(e.x-80f,e.y+i*30f),Offset(e.x+80f,e.y+i*30f),2f)
        drawRect(echoCyan.copy(alpha=.6f),Offset(e.x-100f,e.y-150f),Size(200f,300f),style=Stroke(3f,pathEffect=echoDash))
        return true
    }
    if(e !is NovaCargoVault && e !is NovaReplayNode) return false
    val c=if(e is NovaCargoVault)echoCyan else echoGold
    drawSprite(sprites.enemy(e.kind,0),e.x,e.y)
    val maxHp=(if(e is NovaCargoVault)220f else 60f)*w.difficulty.enemyHp
    drawArc(c,-90f,360f*(e.hp/maxHp).coerceIn(0f,1f),false,Offset(e.x-65f,e.y-65f),Size(130f,130f),style=Stroke(4f))
    if(e.hitFlash>0f)drawSprite(sprites.enemy(e.kind,0),e.x,e.y,alpha=.4f,colorFilter=echoFlash)
    if(e is NovaCargoVault) {
        drawCircle(echoGold,8f,Offset(e.x,e.y-35f))
        e.owner?.let { owner ->
            drawLine(echoCyan.copy(alpha=.5f),Offset(e.x,e.y),Offset(owner.x,owner.y),3f,pathEffect=echoDash)
            if(owner.stolenCount>0)drawSprite(sprites.digit(owner.stolenCount.coerceAtMost(9)),e.x,e.y+80f,scale=.7f)
        }
    } else if(e is NovaReplayNode && !e.fired) {
        mimicWarning(e.recorded,e.x,e.y,true)
        drawArc(c,-90f,360f*(e.age/NovaReplayNode.WARN).coerceIn(0f,1f),false,Offset(e.x-80f,e.y-80f),Size(160f,160f),style=Stroke(4f))
    }
    return true
}

internal fun DrawScope.drawNovaEchoBoss(b:NovaVehicleBoss,w:World,sprites:SpriteCache,flash:Boolean) {
    val raccoon=b as? NovaRaccoonBoss
    val monkey=b as? NovaMonkeyBoss
    val live=w.phase!=Phase.BOSS_DEFEATED && !b.transforming
    if(raccoon!=null) {
        // Solid paired tracks and a cargo beacon distinguish the real chassis from scanline fakes.
        for(side in listOf(-1f,1f)) for(i in 0..4) {
            val y=b.y-180f-i*26f+(w.time*30f)%26f
            drawLine(echoGold.copy(alpha=.2f),Offset(b.x+side*72f-15f,y),Offset(b.x+side*72f+15f,y),6f)
        }
        if(live && (raccoon.locking||raccoon.snaring)) {
            val center=Offset(raccoon.lockX,raccoon.lockY)
            drawLine(echoCyan.copy(alpha=.6f),Offset(b.x,b.y+145f),center,3f,pathEffect=echoDash)
            drawCircle(echoGold.copy(alpha=.08f),NovaRaccoonBoss.SNARE_RADIUS,center)
            drawCircle(echoGold,NovaRaccoonBoss.SNARE_RADIUS,center,style=Stroke(4f,pathEffect=echoDash))
            if(raccoon.snaring)drawSprite(sprites.novaSprite("RACCOON_SNARE",180f),center.x,center.y,alpha=.8f)
        }
    }
    if(live && monkey?.scanning==true)monkey.snapshot?.let { mimicWarning(it,monkey.originX,monkey.originY) }
    val width=530f
    if(b.transforming) {
        val t=(1f-b.armorTime/Boss.ARMOR_TIME).coerceIn(0f,1f)
        drawSprite(sprites.novaSprite("${b.artPrefix}_BASE",width),b.x,b.y,alpha=(1f-t*2f).coerceIn(0f,1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_TRANSITION",width),b.x,b.y,alpha=1f-abs(t*2f-1f))
        drawSprite(sprites.novaSprite("${b.artPrefix}_DEPLOYED",width),b.x,b.y,alpha=(t*2f-1f).coerceIn(0f,1f))
    } else drawSprite(sprites.novaSprite(b.artId,width),b.x,b.y)
    if(flash)drawSprite(sprites.novaSprite(b.artId,width),b.x,b.y,alpha=.35f,colorFilter=echoFlash)
    if(live && (raccoon?.cooling==true || monkey?.cooling==true)) {
        if(monkey!=null)drawSprite(sprites.novaSprite("MONKEY_CORE",110f),b.x,b.y,alpha=.85f)
        drawCircle(echoCyan,60f,Offset(b.x,b.y),style=Stroke(4f,pathEffect=echoDash))
    }
    if(monkey?.awakened==true && w.phase!=Phase.BOSS_DEFEATED) {
        val t=when { b.transforming -> 1f-b.armorTime/Boss.ARMOR_TIME; monkey.awakeningTime>0f -> 1f-monkey.awakeningTime/1.2f; else -> -1f }
        if(t>=0f)drawSprite(sprites.novaSprite("MONKEY_TOTEM",250f),b.x,b.y,scale=1f+t*6f,alpha=(1f-t)*.65f)
        else drawSprite(sprites.novaSprite("MONKEY_TOTEM",90f),b.x,b.y-100f,alpha=.65f+sin(w.time*4f)*.15f)
    }
}
