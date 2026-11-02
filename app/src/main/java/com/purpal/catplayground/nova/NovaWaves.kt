package com.purpal.catplayground.nova

import com.purpal.catplayground.shooter.model.*
import kotlin.math.*

/** Authored entrances and pacing, independent of the original game's schedules. */
object NovaWaves {
    enum class Entry { WEDGE, LEFT, RIGHT, PINCER, REAR, WEAVE, COLUMNS }
    data class Plan(val entry:Entry,val count:Int,val spacing:Float,val lane:Float,val delay:Float)
    val openings=listOf(
        Plan(Entry.PINCER,8,.24f,240f,1.2f), // crown patrol
        Plan(Entry.LEFT,6,.35f,280f,1.8f), // dog interception
        Plan(Entry.WEDGE,5,.5f,450f,2.2f), // heavy bear escort
        Plan(Entry.COLUMNS,6,.65f,300f,1.4f), // thermal pumps
        Plan(Entry.REAR,5,.38f,150f,2f), // rabbit overtaking
        Plan(Entry.RIGHT,8,.2f,210f,1.6f), // ice convoy
        Plan(Entry.WEAVE,4,.85f,650f,2.6f), // canopy patrol
        Plan(Entry.PINCER,6,.7f,420f,1.5f), // paired naval escort
        Plan(Entry.COLUMNS,5,.4f,700f,2.4f), // bamboo defence
        Plan(Entry.WEDGE,4,1f,300f,3f), // slow transport screen
        Plan(Entry.REAR,6,.25f,850f,1.7f), // cargo ambush
        Plan(Entry.WEAVE,7,.3f,350f,1.3f), // echo formation
        Plan(Entry.RIGHT,5,.6f,500f,2.8f), // excavation guards
    )
    private val kinds=listOf(EnemyKind.MOUSE_RUNNER,EnemyKind.PUPPY,EnemyKind.BEE,EnemyKind.DUCKLING,
        EnemyKind.BUNNY,EnemyKind.PENGUIN_SLIDER,EnemyKind.LEAF_STAR,EnemyKind.LEAP_FISH,
        EnemyKind.PANDA_CUB,EnemyKind.TREE_FROG,EnemyKind.OPOSSUM,EnemyKind.SWING_MONKEY,EnemyKind.CHICK)
    fun script(stage:Int):StageScript=StageScript().apply {
        val opening=openings[stage-1]
        val middle=Entry.entries[(stage+2)%Entry.entries.size]
        val later=Entry.entries[(stage+4)%Entry.entries.size]
        fun wave(time:Float,p:Plan) {
            line(time,p.count,p.spacing){w,i->w.spawn(NovaRaider(kinds[stage-1],p.entry,i,p.lane,w.height))}
        }
        wave(opening.delay,opening)
        wave(11f+(stage%3),opening.copy(entry=middle,count=6,lane=750f-opening.lane/2f))
        wave(22f+(stage%4),opening.copy(entry=later,count=7,spacing=.35f))
        wave(39f+(stage%5),opening.copy(entry=if(stage%2==0)Entry.REAR else middle,count=6,lane=stage*59f%700f+150f))
        wave(53f+(stage%3),opening.copy(entry=later,count=8,spacing=.3f))
        val supplies=listOf(ItemType.FOCUS,ItemType.HOMING,ItemType.SPECIAL,ItemType.REPAIR,ItemType.SONIC,ItemType.BOMB)
        for((i,item) in supplies.withIndex())at(6f+i*10f){w->w.spawn(Balloon(220f+(stage*97+i*233)%560,item))}
        at(29f+(stage%4)){w->w.spawn(elite(stage,if(stage%2==0)300f else 700f))}
        at(60f+(stage%3)){w->w.spawn(elite(stage,if(stage%2==0)700f else 300f))}
        at(65f){w->w.spawn(Balloon(500f,ItemType.REPAIR))}
        at(70f+(stage%4)*2f){}
    }
    private fun elite(stage:Int,x:Float):Enemy=when(stage) {
        1->EliteMouse(x);2->EliteDog(x);3->EliteBee(x);4->EliteMonkey(x);5->EliteHare(x)
        6->EliteSeal(x);7->EliteKangaroo(x);8->EliteBeaver(x);9->EliteRedPanda(x)
        10->EliteToucan(x);11->EliteSkunk(x);12->EliteGorilla(x);else->EliteWolf(x)
    }
}

/** Rear craft overtake harmlessly, turn above the player, then fire and descend. */
class NovaRaider(override val kind:EnemyKind,val entry:NovaWaves.Entry,val slot:Int,val lane:Float,height:Float):
    Enemy(500f,-70f,42f,34f,140) {
    private var turned=false
    private var attackAge=0f
    private var shot=false
    val facingDegrees get()=if(entry==NovaWaves.Entry.REAR)180f*(1f-(attackAge/.65f).coerceIn(0f,1f)) else 0f
    override val hurtsOnContact get()=entry!=NovaWaves.Entry.REAR||turned
    init {
        x=when(entry){NovaWaves.Entry.LEFT->-70f;NovaWaves.Entry.RIGHT->1070f
            NovaWaves.Entry.PINCER->if(slot%2==0)-70f else 1070f
            NovaWaves.Entry.REAR->lane
            NovaWaves.Entry.COLUMNS->(lane+(slot%3-1)*190f).coerceIn(100f,900f)
            else->(lane+(slot%5-2)*110f).coerceIn(80f,920f)}
        y=when(entry){NovaWaves.Entry.REAR->height+80f
            NovaWaves.Entry.LEFT,NovaWaves.Entry.RIGHT,NovaWaves.Entry.PINCER->lane+(slot%3)*70f
            else->-70f}
    }
    override fun update(w:World,dt:Float) {
        age+=dt
        if(entry==NovaWaves.Entry.REAR&&!turned) {
            y-=620f*dt
            if(y<minOf(w.player.y-240f,w.height*.42f).coerceAtLeast(220f)){turned=true;attackAge=0f}
        } else {
            attackAge+=dt
            when(entry) {
                NovaWaves.Entry.LEFT->{x+=150f*dt;y+=65f*dt}
                NovaWaves.Entry.RIGHT->{x-=150f*dt;y+=65f*dt}
                NovaWaves.Entry.PINCER->{x+=(if(slot%2==0)140f else -140f)*dt;y+=80f*dt}
                NovaWaves.Entry.WEAVE->{x+=cos(age*1.4f+slot)*140f*dt;y+=165f*dt}
                NovaWaves.Entry.WEDGE->{x+=(if(slot%2==0)-25f else 25f)*dt;y+=155f*dt}
                else->y+=if(entry==NovaWaves.Entry.REAR)180f*dt else 140f*dt
            }
            if(!shot&&attackAge>(if(entry==NovaWaves.Entry.REAR).8f else 1.8f)&&x in 40f..960f&&y<w.player.y-150f) {
                w.aimed(x,y,190f,BulletStyle.ORB,Tint.CYAN);shot=true
            }
        }
        if(age>14f)alive=false
        // Rear arrivals start below the screen; do not cull them during their safe ascent.
        if(entry!=NovaWaves.Entry.REAR||turned)leaveIfOffscreen(w)
    }
}
