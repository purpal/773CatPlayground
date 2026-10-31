package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

/** Excavation controls small, temporary areas; every lock leaves the lower flight lane open. */
class NovaPigBoss : NovaVehicleBoss(35000f,155f,130000) {
    override val kind=EnemyKind.BOSS_PIG
    override val name="掘金工程長 哼哼"
    override val transformAt=.45f
    override val artPrefix="PIG"
    var clock=0f; private set
    var cycle=0; private set
    val arms=mutableListOf<NovaDrillArm>()
    val cooling get()=clock>=7f
    override val damageScale get()=if(cooling)1.6f else 1f
    override fun cancelAttack(w:World) {
        w.enemies.filterIsInstance<NovaBore>().filter{it.owner===this}.forEach{it.alive=false}
        arms.forEach{it.alive=false};arms.clear();clock=0f
    }
    override fun onDefeated(w:World)=cancelAttack(w)
    override fun beginForm(w:World) {
        for(side in listOf(-1,1))arms+=NovaDrillArm(this,side).also{w.spawn(it)}
    }
    override fun combat(w:World,dt:Float) {
        clock+=dt
        if(crossed(clock,dt,.1f)) {
            w.spawn(NovaBore(this,0,w.player.x.coerceIn(180f,820f),w.player.y.coerceIn(500f,minOf(w.height*.72f,w.playerMaxY-NovaBore.RADIUS-w.player.radius-40f).coerceAtLeast(500f)),0f))
            if(form==0)w.spawn(NovaBore(this,0,if(cycle%2==0)240f else 760f,w.height*.48f,.7f))
            else for(arm in arms.filter{it.alive})w.spawn(NovaBore(this,arm.side,if(arm.side<0)240f else 760f,w.height*.48f,if(arm.side<0).7f else 1.4f))
            w.cue(SoundCue.WARNING)
        }
        if(clock>=10f){clock=0f;cycle++}
    }
}
class NovaDrillArm(val owner:NovaPigBoss,val side:Int):Enemy(owner.x+side*210f,owner.y+60f,250f,55f,300) {
    override val kind=EnemyKind.NOVA_DRILL_ARM
    override val contactDamage=0f
    override fun onDefeated(w:World) {w.enemies.filterIsInstance<NovaBore>().filter{it.owner===owner&&it.side==side}.forEach{it.alive=false}}
    override fun update(w:World,dt:Float) {age+=dt;x=owner.x+side*210f;y=owner.y+60f;if(owner.hp<=0f||owner.transforming)alive=false}
}
class NovaBore(val owner:NovaPigBoss?,val side:Int,x:Float,y:Float,val delay:Float):Enemy(x,y,1f,RADIUS,0) {
    override val kind=EnemyKind.NOVA_BORE
    override val invulnerable=true
    override val contactDamage=0f
    val erupting get()=age in (WARN+delay)..(WARN+delay+.35f)
    val fieldActive get()=alive && age>=WARN+delay+.35f && age<LIFE+delay
    override fun playerSpeedFactor(px:Float,py:Float)=if(fieldActive&&dist2(px,py,x,y)<RADIUS*RADIUS).7f else 1f
    override fun update(w:World,dt:Float) {
        age+=dt
        if(owner!=null&&(owner.hp<=0f||owner.transforming)){alive=false;return}
        if(crossed(age,dt,WARN+delay)) {
            if(dist2(w.player.x,w.player.y,x,y)<(RADIUS+w.player.radius).pow(2))w.damagePlayer(22f)
            // Debris flies upward, away from the guaranteed lower escape corridor.
            for(i in 0..4)w.shoot(x,y,(-150f+i*30f)*DEG,160f,BulletStyle.RICE,Tint.ORANGE)
            w.cue(SoundCue.SHOT_HEAVY)
        }
        if(age>=LIFE+delay)alive=false
    }
    companion object { const val WARN=1.8f;const val LIFE=5.8f;const val RADIUS=90f }
}

/** Three-stage fortress: readable moving gates, destructible rail batteries, then mobile core. */
class NovaMouseBoss : NovaVehicleBoss(46000f,160f,300000) {
    override val kind=EnemyKind.BOSS_MOUSE
    override val name="王冠司令 米米"
    override val transformAt=.6f
    override val artPrefix="MOUSE"
    var clock=0f;private set
    var cycle=0;private set
    var awakened=false;private set
    var awakenings=0;private set
    var awakeningTime=0f;private set
    var gapX=500f;private set
    val batteries=mutableListOf<NovaRailBattery>()
    private var equipped=false
    val cooling get()=clock>=8f && awakeningTime<=0f
    override val damageScale get()=if(cooling)1.5f else 1f
    override val invulnerable get()=super.invulnerable||awakeningTime>0f
    override val solid get()=super.solid&&awakeningTime<=0f
    private fun clearParts(w:World) {
        w.enemies.filterIsInstance<NovaRailGate>().filter{it.owner===this}.forEach{it.alive=false}
        batteries.forEach{it.alive=false};batteries.clear();equipped=false
    }
    private fun awaken(){if(!awakened){awakened=true;awakenings++}}
    override fun cancelAttack(w:World){clearParts(w);clock=0f;if(hpRatio<=.3f)awaken()}
    override fun beginForm(w:World){if(hpRatio<=.3f)awaken();clock=0f}
    override fun onDefeated(w:World)=clearParts(w)
    override fun combat(w:World,dt:Float) {
        if(form==1&&!awakened&&hpRatio<=.3f){awaken();clearParts(w);w.bullets.clear();w.lasers.clear();clock=0f;awakeningTime=1.6f;w.cue(SoundCue.WARNING);return}
        if(awakeningTime>0f){awakeningTime=(awakeningTime-dt).coerceAtLeast(0f);return}
        if(!awakened&&!equipped){equipped=true;for(side in listOf(-1,1))batteries+=NovaRailBattery(this,side).also{w.spawn(it)}}
        clock+=dt
        if(crossed(clock,dt,.1f)) {
            gapX=floatArrayOf(400f,600f,500f)[cycle%3]
            NovaRailGate.row(w,this,gapX,0f)
            if(form==1)NovaRailGate.row(w,this,gapX,2f)
            w.cue(SoundCue.WARNING)
        }
        // Fixed lanes remain stable during the salvo. Core changes position only after it clears.
        if(clock>=8f)x=approach(x,if(awakened)floatArrayOf(300f,700f,500f)[cycle%3]else 500f,2f,dt)
        if(clock>=maxOf(12f,(w.height+70f-500f)/140f+4f)){clock=0f;cycle++}
    }
}
class NovaRailBattery(val owner:NovaMouseBoss,val side:Int):Enemy(owner.x+side*160f,owner.y+90f,240f,55f,400) {
    override val kind=EnemyKind.NOVA_RAIL_BATTERY
    override val contactDamage=0f
    override fun update(w:World,dt:Float){age+=dt;x=if(owner.form==0)owner.x+side*160f else 500f+side*330f;y=owner.y+90f;if(owner.hp<=0f||owner.transforming||owner.awakened)alive=false}
    override fun onDefeated(w:World){w.enemies.filterIsInstance<NovaRailGate>().filter{it.owner===owner&&it.side==side}.forEach{it.alive=false}}
}
class NovaRailGate(val owner:NovaMouseBoss?,x:Float,val delay:Float):Enemy(x,500f,85f,38f,100) {
    override val kind=EnemyKind.NOVA_RAIL_GATE
    val side=if(x<500f)-1 else 1
    val active get()=age>=WARN+delay
    override val hurtsOnContact get()=active
    override val contactDamage=20f
    override fun update(w:World,dt:Float){
        age+=dt
        if(owner!=null&&(owner.hp<=0f||owner.transforming||owner.awakeningTime>0f)){alive=false;return}
        if(active)y+=140f*dt
        if(y>w.height+70f)alive=false
    }
    companion object {
        const val WARN=1.6f
        const val GAP_HALF=200f
        fun row(w:World,owner:NovaMouseBoss?,gap:Float,delay:Float) {
            for(i in 1..9){val x=i*100f;val side=if(x<500f)-1 else 1
                val disabled=owner!=null&&!owner.awakened&&owner.batteries.any{it.side==side&&!it.alive}
                if(abs(x-gap)>=GAP_HALF&&!disabled)w.spawn(NovaRailGate(owner,x,delay))
            }
        }
    }
}
