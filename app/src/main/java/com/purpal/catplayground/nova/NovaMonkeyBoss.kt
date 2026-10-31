package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

data class NovaMimicSnapshot(val weapon:MainWeapon,val cat:CatType,val targetX:Float,val targetY:Float) {
    val symbol:Int get()=when(weapon){MainWeapon.SPREAD->1;MainWeapon.FOCUS->2;MainWeapon.SPECIAL->if(cat==CatType.ORANGE)3 else 4}
    fun fire(w:World,x:Float,y:Float,replay:Boolean=false,owner:Enemy?=null) {
        val aim=angleTo(x,y,targetX,targetY)
        when(weapon) {
            MainWeapon.SPREAD -> for(k in -(if(replay)1 else 2)..(if(replay)1 else 2))
                w.shoot(x,y,aim+k*12f*DEG,230f,BulletStyle.RICE,Tint.ORANGE)
            MainWeapon.FOCUS -> w.lasers.add(Laser(x,y,aim,(sqrt(dist2(x,y,targetX,targetY))+180f).coerceAtLeast(500f),
                if(replay)26f else 38f,1.25f,if(replay).4f else .6f,Tint.ORANGE,if(replay)16f else 24f,owner=owner))
            MainWeapon.SPECIAL -> if(cat==CatType.ORANGE) {
                for(k in -(if(replay)0 else 1)..(if(replay)0 else 1))
                    w.shoot(x,y,aim+k*10f*DEG,220f,BulletStyle.BIG_ORB,Tint.ORANGE,if(replay)20f else 28f)
            } else {
                val count=if(replay)8 else 12
                for(i in 0 until count) {
                    val a=i*TWO_PI/count
                    if(abs(a-HALF_PI)>.5f)w.shoot(x,y,a,170f,BulletStyle.ORB,Tint.ORANGE)
                }
            }
        }
        w.cue(SoundCue.SHOT_HEAVY)
    }
}

/** Locks a weapon snapshot; the awakened totem adds one destructible delayed replay. */
class NovaMonkeyBoss : NovaVehicleBoss(35000f,165f,120000) {
    override val kind=EnemyKind.BOSS_MONKEY
    override val name="擬神駕駛員 吱吱"
    override val transformAt=.3f
    override val artPrefix="MONKEY"
    var clock=0f
        private set
    var cycle=0
        private set
    var snapshot:NovaMimicSnapshot?=null
        private set
    var originX=500f
        private set
    var originY=500f
        private set
    var awakened=false
        private set
    var awakenings=0
        private set
    var awakeningTime=0f
        private set
    val scanning get()=snapshot!=null && clock in .1f..1.6f
    val cooling get()=clock>=8f && !transforming && awakeningTime<=0f
    override val damageScale get()=if(cooling)1.6f else 1f
    override val invulnerable get()=super.invulnerable || awakeningTime>0f
    override val solid get()=super.solid && awakeningTime<=0f
    private fun cancelReplay(w:World) { w.enemies.filterIsInstance<NovaReplayNode>().filter{it.owner===this}.forEach{it.cancel(w)} }
    private fun awaken() { if(!awakened){awakened=true;awakenings++} }
    override fun cancelAttack(w:World) {
        cancelReplay(w);clock=0f;snapshot=null
        if(hpRatio<=.25f)awaken() // one hit crossing both gates uses the existing transformation
    }
    override fun beginForm(w:World) { if(hpRatio<=.25f)awaken();clock=0f }
    override fun onDefeated(w:World) { cancelReplay(w) }
    override fun combat(w:World,dt:Float) {
        if(form==1 && !awakened && hpRatio<=.25f) {
            awaken();cancelReplay(w);w.bullets.clear();w.lasers.clear();snapshot=null;clock=0f;awakeningTime=1.2f;w.cue(SoundCue.WARNING);return
        }
        if(awakeningTime>0f) {awakeningTime=(awakeningTime-dt).coerceAtLeast(0f);return}
        clock+=dt
        if(crossed(clock,dt,.1f)) {
            snapshot=NovaMimicSnapshot(w.player.mainWeapon,w.player.cat,w.player.x,w.player.y)
            originX=x+if(form==1)if(cycle%2==0)-85f else 85f else 0f;originY=y+100f
            w.cue(SoundCue.WARNING)
        }
        if(crossed(clock,dt,1.6f)) snapshot?.fire(w,originX,originY)
        if(awakened && crossed(clock,dt,3.2f)) snapshot?.let {
            w.spawn(NovaReplayNode(this,it,x+if(cycle%2==0)-220f else 220f,y+180f))
            w.cue(SoundCue.WARNING)
        }
        if(clock>=8f) x=approach(x,500f+sin(age*.35f)*80f,2f,dt)
        if(clock>=10f){clock=0f;cycle++}
    }
}

class NovaReplayNode(val owner:NovaMonkeyBoss?,val recorded:NovaMimicSnapshot,x:Float,y:Float):Enemy(x,y,60f,50f,200) {
    override val kind=EnemyKind.NOVA_REPLAY_NODE
    override val contactDamage=0f
    var fired=false
        private set
    fun cancel(w:World) { alive=false;w.lasers.filter{it.owner===this}.forEach{it.alive=false} }
    override fun onDefeated(w:World)=cancel(w)
    override fun update(w:World,dt:Float) {
        age+=dt
        if(owner!=null && (!owner.alive||owner.hp<=0f||owner.transforming)) {cancel(w);return}
        if(crossed(age,dt,WARN)) {recorded.fire(w,x,y,true,this);fired=true}
        if(age>=5f)cancel(w)
    }
    companion object {const val WARN=2.8f}
}
