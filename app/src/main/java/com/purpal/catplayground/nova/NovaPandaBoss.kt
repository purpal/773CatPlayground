package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

/** A reflected trajectory sampled by BOTH warning art and actual motion. */
class NovaRollPath(val startX:Float,val startY:Float,val vx:Float,val vy:Float,val bottom:Float) {
    fun at(t:Float):Pair<Float,Float> = reflect(startX+vx*t, MARGIN, WORLD_WIDTH-MARGIN) to reflect(startY+vy*t, TOP, bottom)
    private fun reflect(value:Float,lo:Float,hi:Float):Float {
        val length=hi-lo
        val p=((value-lo)%(2f*length)+2f*length)%(2f*length)
        return lo+if(p<=length)p else 2f*length-p
    }
    companion object { const val MARGIN=180f; const val TOP=340f }
}

/** Supply denial before transforming; a locked ricochet route followed by a vulnerable stop. */
class NovaPandaBoss : NovaVehicleBoss(34000f,145f,90000) {
    override val kind=EnemyKind.BOSS_PANDA
    override val name="玄竹守備官 波波"
    override val transformAt=.4f
    override val artPrefix="PANDA"
    enum class Motion { SUPPLY, RETURN, WARN, ROLL, COOL }
    var motion=Motion.SUPPLY
        private set
    var clock=0f
        private set
    var supplyUses=0
        private set
    var route:NovaRollPath?=null
        private set
    var supplyBeam:Laser?=null
        private set
    private val nodes=mutableListOf<NovaSupplyCell>()
    val cooling get() = !transforming && (motion==Motion.COOL || motion==Motion.SUPPLY && clock in 4f..6f)
    override val damageScale get() = if(cooling)1.8f else 1f
    override val solid get() = super.solid && motion!=Motion.RETURN
    override val hurtsOnContact get() = solid
    override fun cancelAttack(w:World) {
        nodes.forEach { it.alive=false };nodes.clear()
        supplyBeam?.alive=false;supplyBeam=null;route=null;clock=0f
    }
    override fun beginForm(w:World) { motion=Motion.RETURN;clock=0f }
    override fun combat(w:World,dt:Float) {
        clock+=dt
        if(nodes.any{!it.alive}) supplyBeam?.alive=false
        when(motion) {
            Motion.SUPPLY -> {
                if(crossed(clock,dt,.1f)) {
                    nodes.clear()
                    for(side in listOf(-1f,1f)) NovaSupplyCell(this,500f+side*220f,560f).also { nodes+=it;w.spawn(it) }
                }
                if(crossed(clock,dt,1.6f) && nodes.size==2 && nodes.all{it.alive}) {
                    supplyBeam=Laser(nodes[0].x,560f,0f,440f,30f,1.4f,.65f,Tint.GREEN,22f).also{w.lasers+=it}
                    w.cue(SoundCue.WARNING)
                }
                if(crossed(clock,dt,2.2f)||crossed(clock,dt,3.1f)) {
                    for(k in -1..1) w.shoot(x,y+100f,HALF_PI+k*.4f,220f,BulletStyle.BAMBOO,Tint.GREEN)
                }
                if(crossed(clock,dt,6.4f)) {
                    if(nodes.any{it.alive} && supplyUses<3) { hp=minOf(maxHp,hp+maxHp*.03f);supplyUses++ }
                    nodes.forEach{it.alive=false}
                }
                if(clock>=9f) clock=0f
            }
            Motion.RETURN -> {
                x=approach(x,500f,3f,dt);y=approach(y,400f,3f,dt)
                if(clock>=2f) {
                    val a=angleTo(x,y,w.player.x.coerceIn(200f,800f),maxOf(620f,w.height*.55f))
                    route=NovaRollPath(x,y,cos(a)*440f,sin(a)*440f,maxOf(620f,w.height*.55f))
                    motion=Motion.WARN;clock=0f;w.cue(SoundCue.WARNING)
                }
            }
            Motion.WARN -> if(clock>=WARN) { motion=Motion.ROLL;clock=0f }
            Motion.ROLL -> {
                val point=checkNotNull(route).at(minOf(clock,ROLL))
                x=point.first;y=point.second
                if(clock>=ROLL) { motion=Motion.COOL;clock=0f;w.cue(SoundCue.SHOT_HEAVY) }
            }
            Motion.COOL -> if(clock>=2.2f) { motion=Motion.RETURN;clock=0f }
        }
    }
    companion object { const val WARN=1.4f; const val ROLL=3.8f }
}

class NovaSupplyCell(val owner:NovaPandaBoss?,x:Float,y:Float):Enemy(x,y,170f,50f,200) {
    override val kind=EnemyKind.NOVA_SUPPLY_CELL
    override val contactDamage=0f
    override fun update(w:World,dt:Float) {
        age+=dt
        if(age>=7f || owner!=null && (!owner.alive || owner.hp<=0f || owner.transforming)) alive=false
    }
}
