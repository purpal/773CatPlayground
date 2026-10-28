package com.purpal.catplayground.nova

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.shooter.model.*
import com.purpal.catplayground.ui.ArtImage
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun NovaTourScreen(tour: TourState, onRevealed: (TourState)->Unit, onFight: ()->Unit, onNewTour: ()->Unit, onBack: ()->Unit) {
    val sound=LocalSound.current
    val key=tour.order to tour.cleared
    var step by remember(key) { mutableIntStateOf(if(tour.nextRevealed || tour.finished) 3 else 0) }
    var rolling by remember(key) { mutableIntStateOf(tour.nextStage ?: 2) }
    val flight=remember(key) { Animatable(if(tour.nextRevealed || tour.finished) 1f else 0f) }
    LaunchedEffect(key) {
        if(tour.nextRevealed || tour.finished) return@LaunchedEffect
        val remaining=(tour.cleared until minOf(Tour.RANDOM_BOUTS,tour.order.size)).map { tour.order[it] }
        if(remaining.size>1) repeat(12) { i ->
            rolling=remaining[i%remaining.size];sound.play(Sfx.SELECT);delay(70L+i*15)
        }
        rolling=tour.nextStage ?: 2;step=1;sound.play(Sfx.POWER_UP);delay(650)
        step=2
        flight.animateTo(1f,tween(3800,easing=LinearEasing))
        step=3;onRevealed(tour.reveal())
    }
    val destination=if(tour.finished) Countries.HOME else tour.stageAt(tour.cleared)?.country ?: Countries.HOME
    val opponent=Stages.get(if(step==0)rolling else tour.nextStage ?: 2)
    NovaBackdrop {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onBack){Text("返回機庫",color=NovaCyan)}
            Spacer(Modifier.weight(1f));Text("${minOf(tour.cleared+1,Tour.SIZE)} / ${Tour.SIZE}",color=NovaGold)
        }
        Text("軌道航路指揮台",fontSize=27.sp,color=Color.White,fontWeight=FontWeight.Black)
        Text("${tour.departure().name}  →  ${if(step==0)"目標辨識中" else destination.name}",color=NovaCyan,fontSize=14.sp)
        Globe(tour.departure(),destination,flight.value,step>0,tour.cat)
        Column(Modifier.fillMaxWidth().background(NovaPanel,RoundedCornerShape(12.dp)).border(1.dp,NovaCyan.copy(alpha=.3f),RoundedCornerShape(12.dp)).padding(16.dp)) {
            Text(when(step){0->"敵方訊號掃描";1->"航路已鎖定";2->"躍遷航行中";else->if(tour.finished)"全球遠征完成" else "已抵達戰區"},color=NovaGold,fontSize=12.sp)
            if(!tour.finished && opponent!=null) Row(verticalAlignment=Alignment.CenterVertically) {
                if(opponent.number==2) NovaAsset("boss",Modifier.size(92.dp),"柴犬隊長巡航艦")
                else ArtImage("${opponent.bossArt}_m0_0",Modifier.size(92.dp),opponent.bossName)
                Column(Modifier.weight(1f).padding(start=12.dp)) {
                    Text(opponent.bossName,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                    Text("${opponent.country.name}｜${opponent.title}",color=NovaCyan,fontSize=13.sp)
                    Text("駕駛員 ${tour.cat.shortName}  ·  累計 ${tour.totalScore}",color=Color(0xFFB0C8D8),fontSize=12.sp)
                }
            }
        }
        Text("遠征航誌",color=NovaCyan,fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            for(i in tour.order.indices) Column(Modifier.width(88.dp).background(if(i==tour.cleared)Color(0xFF204456)else NovaPanel,RoundedCornerShape(8.dp)).padding(10.dp)) {
                Text("${i+1}".padStart(2,'0'),color=if(i<tour.cleared)NovaGold else NovaCyan,fontWeight=FontWeight.Bold)
                Text(if(i<tour.revealed || i<tour.cleared) tour.stageAt(i)?.country?.name ?: "未知" else "未辨識",color=Color.White,fontSize=12.sp)
            }
        }
        if(tour.finished) NovaButton("重新展開遠征",onClick=onNewTour)
        else NovaButton(when(step){0->"掃描中…";1,2->"航行中…";else->"進入戰區"},step==3,onFight)
    }
}

@Composable
private fun Globe(from:Country,to:Country,flight:Float,revealed:Boolean,cat:CatType) {
    val context=LocalContext.current
    val assets=NovaAssets.get(context.assets)
    // Reuse only the geographic mask from the map, rendered as a shaded 3D point globe.
    val land=remember {
        val map=context.assets.open("art/tour_map.png").use { BitmapFactory.decodeStream(it) }
        buildList {
            for(lat in -81..81 step 3) for(lon in -180 until 180 step 3) {
                val x=((lon+180)/360f*(map.width-1)).toInt()
                val y=((84-lat)/170f*(map.height-1)).toInt().coerceIn(0,map.height-1)
                val rgb=map.getPixel(x,y);val red=rgb shr 16 and 255;val green=rgb shr 8 and 255;val blue=rgb and 255
                if(green>blue+10 && green>red) add(GlobeMath.point(lon.toDouble(),lat.toDouble()))
            }
        }.also { map.recycle() }
    }
    val a=remember(from){GlobeMath.point(from.lon.toDouble(),from.lat.toDouble())}
    val b=remember(to){GlobeMath.point(to.lon.toDouble(),to.lat.toDouble())}
    val loop=rememberInfiniteTransition(label="globe")
    val pulse by loop.animateFloat(0f,1f,infiniteRepeatable(tween(1600,easing=LinearEasing)),label="pulse")
    Canvas(Modifier.fillMaxWidth().heightIn(min=240.dp,max=330.dp).aspectRatio(1.2f).background(NovaInk,RoundedCornerShape(16.dp))) {
        val center=Offset(size.width/2,size.height/2)
        val r=min(size.width,size.height)*.425f
        val camera=GlobeMath.route(a,b,if(revealed)flight*.85 else 0.0)
        fun p(v:GlobeMath.Vec):Pair<Offset,Float> {
            val q=GlobeMath.project(v,camera)
            return center+Offset(q.x*r,q.y*r) to q.depth
        }
        drawCircle(NovaCyan.copy(alpha=.06f),r*1.08f,center)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF214D67),Color(0xFF07192B)),center-Offset(r*.3f,r*.3f),r*1.5f),r,center)
        for(v in land) {
            val (o,z)=p(v)
            if(z>0f) drawCircle(Color(0xFF57C9BD).copy(alpha=.2f+.75f*z),r*.014f,o)
        }
        fun grid(points:List<GlobeMath.Vec>) {
            val path=Path();var pen=false
            for(v in points) {val(o,z)=p(v);if(z>0){if(pen)path.lineTo(o.x,o.y)else path.moveTo(o.x,o.y);pen=true}else pen=false}
            drawPath(path,NovaCyan.copy(alpha=.15f),style=Stroke(1f))
        }
        for(lat in -60..60 step 30) grid((-180..180 step 3).map{GlobeMath.point(it.toDouble(),lat.toDouble())})
        for(lon in -180 until 180 step 30) grid((-90..90 step 3).map{GlobeMath.point(lon.toDouble(),it.toDouble())})
        drawCircle(NovaCyan.copy(alpha=.5f),r,center,style=Stroke(2f))
        val(fromPoint,fromZ)=p(a)
        if(fromZ>0)drawCircle(NovaCyan,4.dp.toPx(),fromPoint)
        if(revealed) {
            val path=Path();var pen=false
            for(i in 0..80) {
                val(o,z)=p(GlobeMath.route(a,b,i/80.0))
                if(z>0){if(pen)path.lineTo(o.x,o.y)else path.moveTo(o.x,o.y);pen=true}else pen=false
            }
            drawPath(path,NovaGold.copy(alpha=.5f),style=Stroke(2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,8f))))
            val(o,z)=p(b)
            if(z>0) {
                drawCircle(NovaGold,4.dp.toPx(),o)
                drawCircle(NovaGold.copy(alpha=1-pulse),(.02f+pulse*.07f)*r,o,style=Stroke(2f))
            }
            val pos=GlobeMath.route(a,b,flight.toDouble())
            val next=GlobeMath.route(a,b,(flight+.01f).coerceAtMost(1f).toDouble())
            val(plane,depth)=p(pos);val(ahead,_)=p(next)
            if(depth>0) {
                val angle=if(flight>=.999f)0f else Math.toDegrees(atan2((ahead.y-plane.y).toDouble(),(ahead.x-plane.x).toDouble())).toFloat()+90
                rotate(angle,plane) {novaImage(assets.image(if(cat==CatType.ORANGE)"ship_orange"else"ship_black"),plane,28.dp.toPx())}
            }
        }
    }
}
