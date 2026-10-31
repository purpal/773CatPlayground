package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

/** Freight raider: visible theft locks, a destructible vault, and identifiable fake trucks. */
class NovaRaccoonBoss : NovaVehicleBoss(34000f,155f,110000) {
    override val kind=EnemyKind.BOSS_RACCOON
    override val name="夜匣指揮官 基德"
    override val transformAt=.5f
    override val artPrefix="RACCOON"
    var clock=0f
        private set
    var cycle=0
        private set
    var vault:NovaCargoVault?=null
        private set
    private var vaultDelay=0f
    private var vulnerable=0f
    private val loot=mutableListOf<ItemType>()
    private val returned=mutableSetOf<Item>()
    val stolenCount get()=loot.size
    var lockX=500f
        private set
    var lockY=1000f
        private set
    private var itemTarget:Item?=null
    var stealingPlayer=false
        private set
    private var lockArmed=false
    private var snareFired=false
    val locking get()=lockArmed && clock in 1f..2.5f && vault?.alive==true
    val snaring get()=snareFired && clock in 2.5f..2.85f && vault?.alive==true
    val cooling get()=vulnerable>0f
    override val damageScale get()=if(cooling)1.8f else 1f
    private var destination=500f
    fun releaseLoot(w:World,broken:Boolean) {
        loot.forEachIndexed { i,type ->
            Item(x+(i-(loot.size-1)/2f)*65f,y+190f,type).also { w.items+=it;returned+=it }
        }
        loot.clear()
        if(broken) { vulnerable=3f;vaultDelay=12f;lockArmed=false;snareFired=false;itemTarget=null;stealingPlayer=false }
    }
    override fun onDefeated(w:World) { releaseLoot(w,false);clearDecoys(w) }
    private fun clearDecoys(w:World) { w.enemies.filterIsInstance<NovaHologram>().filter{it.owner===this}.forEach{it.alive=false} }
    override fun cancelAttack(w:World) {
        clearDecoys(w);lockArmed=false;snareFired=false;itemTarget=null;stealingPlayer=false;clock=0f
        // Cargo survives the transformation, so stolen resources cannot disappear with the art.
    }
    override fun beginForm(w:World) { clock=0f }
    override fun combat(w:World,dt:Float) {
        clock+=dt;vulnerable=(vulnerable-dt).coerceAtLeast(0f);vaultDelay=(vaultDelay-dt).coerceAtLeast(0f)
        returned.removeAll{!it.alive || it !in w.items}
        if((vault?.alive!=true || w.enemies.none{it===vault}) && vaultDelay<=0f) {
            vault=NovaCargoVault(this,x,y+145f).also{w.spawn(it)}
        }
        if(crossed(clock,dt,.1f)) {
            destination=if(form==1)floatArrayOf(250f,750f,500f)[cycle%3] else 500f
            clearDecoys(w)
            if(form==1) for(slot in listOf(250f,500f,750f)) if(slot!=destination)w.spawn(NovaHologram(this,slot))
        }
        // Finish moving before the lock; the genuine vehicle always keeps its cargo beacon.
        if(clock<1f || clock>7f) x=approach(x,destination,5f,dt)
        if(crossed(clock,dt,1f) && vault?.alive==true) {
            lockArmed=true;snareFired=false
            itemTarget=w.items.filter{it.alive && it.type !in listOf(ItemType.COIN,ItemType.DATA) && it !in returned}
                .minByOrNull{dist2(it.x,it.y,x,y)}
            stealingPlayer=itemTarget==null
            lockX=itemTarget?.x ?: w.player.x;lockY=itemTarget?.y ?: w.player.y
            w.cue(SoundCue.WARNING)
        }
        // Item lock visibly follows a floating pickup; player lock stays at the old position.
        if(locking) itemTarget?.takeIf{it.alive}?.let{lockX=it.x;lockY=it.y}
        if(crossed(clock,dt,2.5f) && lockArmed && vault?.alive==true) {
            lockArmed=false;snareFired=true
            val item=itemTarget
            if(item!=null && item.alive && item in w.items) { loot+=item.type;item.alive=false;w.items.remove(item) }
            else if(stealingPlayer && w.player.bombs>0 && w.player.invulnerable<=0f &&
                dist2(w.player.x,w.player.y,lockX,lockY)<(SNARE_RADIUS+w.player.radius).pow(2)) {
                w.player.bombs--;loot+=ItemType.BOMB
                w.damagePlayer(16f)
            }
            itemTarget=null;stealingPlayer=false
        }
        if(clock in 4f..6f && every(clock,dt,.9f,offset=4f)) {
            for(k in -1..1) w.shoot(x,y+90f,w.aimAt(x,y+90f)+k*.25f,240f,BulletStyle.CARD,Tint.CYAN)
        }
        if(clock>=10f) {clock=0f;cycle++;snareFired=false;lockArmed=false}
    }
    companion object { const val SNARE_RADIUS=90f }
}

class NovaCargoVault(val owner:NovaRaccoonBoss?,x:Float,y:Float):Enemy(x,y,220f,55f,250) {
    override val kind=EnemyKind.NOVA_CARGO_VAULT
    override val contactDamage=0f
    override val invulnerable get()=owner?.let{it.entering||it.transforming} ?: false
    override fun onDefeated(w:World) { if(!released){released=true;owner?.releaseLoot(w,true)} }
    private var released=false
    override fun update(w:World,dt:Float) {
        age+=dt
        if(owner==null) { if(age>6f)alive=false;return }
        x=owner.x;y=owner.y+145f
        if(!owner.alive || owner.hp<=0f)alive=false
    }
}

class NovaHologram(val owner:NovaRaccoonBoss,val slot:Float):Enemy(owner.x,owner.y,40f,100f,0) {
    override val kind=EnemyKind.NOVA_HOLOGRAM
    override val contactDamage=0f
    override fun update(w:World,dt:Float) {
        age+=dt;x=approach(x,slot,5f,dt)
        if(!owner.alive||owner.hp<=0f||owner.transforming||age>9.5f)alive=false
    }
}
