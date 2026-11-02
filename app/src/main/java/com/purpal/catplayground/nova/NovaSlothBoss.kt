package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

/** Persistent slow occupation rounds; force walls carve space, transformations do not reset it. */
class NovaSlothBoss : NovaVehicleBoss(33000f,165f,100000) {
    override val preserveBulletsOnTransform=true
    override val hpFactor=1.25f
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
    val cooling get() = !transforming && clock>=10f
    override val damageScale get() = if(cooling)1.6f else 1f
    override fun bulletTimeScale(bx:Float,by:Float)=1f
    override fun cancelAttack(w:World) { clock=0f }
    override fun beginForm(w:World) { clock=0f }
    override fun combat(w:World,dt:Float) {
        clock+=dt
        // Slow horizontal drift keeps the firing source readable.
        x=approach(x,500f+sin(age*.22f)*55f,2f,dt)
        if(every(clock,dt,2f,offset=.15f)) {
            val row=(clock/2f).toInt()+cycle*6
            val startY=minOf(w.player.y-110f,y+120f)
            for(i in 0..19) {
                val bx=25f+i*50f+if(row%2==0)0f else 25f
                w.shoot(bx,startY,HALF_PI,26f/w.difficulty.bulletSpeed,BulletStyle.LETTER,Tint.PURPLE,24f).also{it.wallDamageScale=.25f}
            }
            if(form==1) for(side in listOf(-1f,1f)) {
                w.shoot(if(side<0)70f else 930f,w.player.y,if(side<0)0f else PI.toFloat(),22f/w.difficulty.bulletSpeed,BulletStyle.ORB,Tint.PURPLE,28f).also{it.wallDamageScale=.25f}
            }
            w.cue(SoundCue.SHOT_HEAVY)
        }
        if(clock>=12f){clock=0f;cycle++}
    }

}

/** A destructible preview of the persistent slow occupation rounds. */
class NovaTimeBeacon(x:Float,y:Float):Enemy(x,y,120f,48f,180) {
    override val kind=EnemyKind.NOVA_TIME_BEACON
    override val contactDamage=0f
    override fun update(w:World,dt:Float) {
        age+=dt
        for(gate in 0..2) if(crossed(age,dt,1.4f+gate*.8f)) {
            w.shoot(x,y,(50f+gate*40f)*DEG,26f/w.difficulty.bulletSpeed,BulletStyle.LETTER,Tint.PURPLE).also{it.wallDamageScale=.25f}
            w.cue(SoundCue.SHOT_HEAVY)
        }
        if(age>4f) alive=false
    }
}
