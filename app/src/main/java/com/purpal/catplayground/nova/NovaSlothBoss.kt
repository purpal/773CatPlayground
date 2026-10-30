package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

/** Three announced release gates, one field, and no modification to the player's clock. */
class NovaSlothBoss : NovaVehicleBoss(33000f,165f,100000) {
    override val kind=EnemyKind.BOSS_SLOTH
    override val name="時差領航員 閃電"
    override val transformAt=.35f
    override val artPrefix="SLOTH"
    var clock=0f
        private set
    var cycle=0
        private set
    val fieldX get() = 500f
    val fieldY get() = 580f
    val fieldActive get() = !entering && !transforming && clock<10f
    val cooling get() = !transforming && clock>=10f
    override val damageScale get() = if(cooling)1.6f else 1f
    fun releaseAt(gate:Int):Float = 4.4f+(if(cycle%2==0)gate else 2-gate)*1.2f
    fun releaseProgress(gate:Int) = ((clock-releaseAt(gate))/.6f).coerceIn(0f,1f)
    fun gateAngle(gate:Int) = (50f+gate*40f)*DEG
    fun gateAt(bx:Float,by:Float):Int {
        val a=atan2(by-fieldY,bx-fieldX)/DEG
        return when { a<70f -> 0; a<110f -> 1; else -> 2 }
    }
    override fun bulletTimeScale(bx:Float,by:Float):Float {
        if(!fieldActive) return 1f
        val distance=sqrt(dist2(bx,by,fieldX,fieldY))
        if(distance>=FIELD_RADIUS) return 1f
        val release=releaseProgress(gateAt(bx,by))
        val speed=SLOW+((if(form==1)FAST else 1f)-SLOW)*release
        val weight=((FIELD_RADIUS-distance)/40f).coerceIn(0f,1f)
        return 1f+(speed-1f)*weight
    }
    override fun cancelAttack(w:World) { clock=0f }
    override fun beginForm(w:World) { clock=0f }
    override fun combat(w:World,dt:Float) {
        clock+=dt
        // The field stays fixed; small ship drift never moves a boundary onto the player.
        x=approach(x,500f+sin(age*.22f)*55f,2f,dt)
        if(crossed(clock,dt,.1f)) w.cue(SoundCue.WARNING)
        if(crossed(clock,dt,1.4f)||crossed(clock,dt,2.1f)||crossed(clock,dt,2.8f)) {
            for(gate in 0..2) for(offset in if(form==1)listOf(-4f,4f) else listOf(0f)) {
                val a=gateAngle(gate)+offset*DEG
                w.shoot(fieldX+cos(a)*20f,fieldY+sin(a)*20f,a,115f,BulletStyle.LETTER,Tint.PURPLE)
            }
        }
        for(gate in 0..2) if(crossed(clock,dt,releaseAt(gate)-1.4f)) w.cue(SoundCue.WARNING)
        if(clock>=12f) { clock=0f;cycle++ }
    }
    companion object { const val FIELD_RADIUS=280f; const val SLOW=.3f; const val FAST=1.7f }
}

/** A destructible timing lesson: three clearly announced launches instead of an instant volley. */
class NovaTimeBeacon(x:Float,y:Float):Enemy(x,y,120f,48f,180) {
    override val kind=EnemyKind.NOVA_TIME_BEACON
    override val contactDamage=0f
    override fun update(w:World,dt:Float) {
        age+=dt
        for(gate in 0..2) if(crossed(age,dt,1.4f+gate*.8f)) {
            w.shoot(x,y,(50f+gate*40f)*DEG,170f,BulletStyle.LETTER,Tint.PURPLE)
            w.cue(SoundCue.SHOT_HEAVY)
        }
        if(age>4f) alive=false
    }
}
