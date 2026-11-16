package com.purpal.catplayground.nightwatch

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.*

private val labelPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);textAlign=Paint.Align.CENTER}
private fun DrawScope.label(text: String,x: Float,y: Float,color: Color=NightGold,size: Float=25f) {
    labelPaint.color=color.toArgb();labelPaint.textSize=size
    drawContext.canvas.nativeCanvas.drawText(text,x,y,labelPaint)
}
private fun DrawScope.sprite(image: ImageBitmap?,x: Float,y: Float,width: Float,alpha: Float=1f) {
    if(image==null) return
    val h=width*image.height/image.width
    drawImage(image,dstOffset=IntOffset((x-width/2).toInt(),(y-h).toInt()),dstSize=IntSize(width.toInt(),h.toInt()),alpha=alpha)
}
private fun Point.o()=Offset(x,y)
private fun DrawScope.glow(p: Offset,color: Color,radius: Float,alpha: Float=.4f) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha=alpha),color.copy(alpha=0f)),p,radius),radius,p)
}
private fun DrawScope.road(points: List<Point>,color: Color,width: Float) {
    val path=Path().apply {moveTo(points.first().x,points.first().y);points.drop(1).forEach {lineTo(it.x,it.y)}}
    drawPath(path,color,style=Stroke(width,cap=StrokeCap.Round,join=StrokeJoin.Round))
}

internal fun DrawScope.drawNightBattle(b: NightBattle,art: Map<String,ImageBitmap>,selected: Int,moving: Int,quiet: Boolean) {
    val scale=min(size.width/1000f,size.height/1200f)
    val left=(size.width-1000f*scale)/2;val top=(size.height-1200f*scale)/2
    clipRect {
        translate(left,top) {scale(scale,scale,Offset.Zero) {
            val palettes=listOf(listOf(Color(0xFF102D40),Color(0xFF214A56)),listOf(Color(0xFF102E2F),Color(0xFF24493D)),listOf(Color(0xFF262840),Color(0xFF3D3655)))
            val colors=palettes[b.stage.id]
            drawRect(Brush.verticalGradient(colors,0f,1200f),size=Size(1000f,1200f))
            // Stone paving and rain glints; fixed layout so nothing shifts during combat.
            for(row in 0..19) for(col in 0..12) {
                val x=col*90f+(row%2)*45f;val y=row*68f
                drawRoundRect(Color.White.copy(alpha=.025f),Offset(x,y),Size(82f,60f),androidx.compose.ui.geometry.CornerRadius(8f))
            }
            for(i in 0..25) {
                val x=(i*173%1000).toFloat(); val y=(i*233%1150).toFloat()
                if(!quiet) glow(Offset(x,y),if(b.stage.id==1) NightMint else Color(0xFF83B8FB),42f,.07f)
                drawCircle(NightMint.copy(alpha=.15f+.1f*sin(b.time+i)),2.5f,Offset(x,y))
            }
            for(path in b.stage.paths) {
                road(path,Color(0xFF071722),100f)
                road(path,if(b.flood>0) Color(0xFF276F89) else Color(0xFF536579),76f)
                road(path,if(b.flood>0) Color(0xFF67D8F7).copy(alpha=.25f) else Color(0xFF8091A0).copy(alpha=.16f),47f)
                for((a,c) in path.zipWithNext()) {
                    val steps=(a.distance(c)/90f).toInt().coerceAtLeast(1)
                    for(i in 1..steps) {
                        val f=i.toFloat()/(steps+1)
                        val p=Offset(a.x+(c.x-a.x)*f,a.y+(c.y-a.y)*f)
                        val d=if(abs(a.x-c.x)>abs(a.y-c.y)) Offset(0f,21f) else Offset(21f,0f)
                        drawLine(Color(0xFF253F51),p-d,p+d,3f)
                    }
                }
            }
            // Location-specific scenery stays outside the traversal lanes.
            when(b.stage.id) {
                0 -> for(i in 0..3) {
                    val x=90f+i*245f
                    drawRoundRect(Color(0xFF21323E),Offset(x-65f,35f),Size(130f,63f),androidx.compose.ui.geometry.CornerRadius(8f))
                    val roof=Path().apply {moveTo(x-85,35f);lineTo(x,5f);lineTo(x+85,35f);close()}
                    drawPath(roof,if(i%2==0) Color(0xFF934B50) else Color(0xFF3F7D86))
                    drawLine(Color(0xFFDBAA72),Offset(x-65,40f),Offset(x+65,40f),5f)
                    label("魚",x,86f,Color(0xFFB9D7D8),26f)
                }
                1 -> for(i in 0..10) {
                    val x=20f+(i*97%950)
                    drawLine(Color(0xFF467E5A),Offset(x,20f),Offset(x-20,100f),9f)
                    for(k in 0..2) drawLine(Color(0xFF76A269),Offset(x-k*6,35f+k*23),Offset(x+25,20f+k*23),7f)
                }
                else -> for(i in 0..4) {
                    val x=80f+i*210f
                    drawRect(Color(0xFF847D94),Offset(x-15,25f),Size(30f,75f))
                    drawRect(Color(0xFFBAB1BE),Offset(x-25,20f),Size(50f,12f))
                    drawRect(Color(0xFF545065),Offset(x-25,100f),Size(50f,15f))
                }
            }
            // Warm lanterns frame the cold moonlit battlefield.
            for(i in 0..5) {
                val x=if(i%2==0) 42f else 958f;val y=250f+(i/2)*330f
                if(!quiet) glow(Offset(x,y),NightGold,80f,.2f)
                drawLine(Color(0xFF191D29),Offset(x,y-35),Offset(x,y+20),7f)
                drawRoundRect(Color(0xFFDC945A),Offset(x-13,y-22),Size(26f,35f),androidx.compose.ui.geometry.CornerRadius(8f))
                drawLine(NightGold,Offset(x-8,y-10),Offset(x+8,y-10),3f)
            }
            // The final canned-food shrine.
            val shrine=Offset(500f,1145f)
            if(!quiet) glow(shrine,NightGold,110f,.2f)
            drawRoundRect(Color(0xFF37334C),Offset(415f,1085f),Size(170f,90f),androidx.compose.ui.geometry.CornerRadius(13f))
            drawRect(Color(0xFFAC7162),Offset(425f,1070f),Size(15f,105f));drawRect(Color(0xFFAC7162),Offset(560f,1070f),Size(15f,105f))
            drawLine(Color(0xFFDD9B78),Offset(395f,1060f),Offset(605f,1060f),18f)
            drawLine(Color(0xFFDD9B78),Offset(410f,1082f),Offset(590f,1082f),9f)
            drawOval(NightGold,Offset(473f,1120f),Size(54f,19f));drawRect(Color(0xFFAC8055),Offset(473f,1130f),Size(54f,30f))
            label("罐",500f,1155f,NightGold,23f)
            drawRoundRect(Color(0xFF14212F),Offset(415f,1180f),Size(170f,12f),androidx.compose.ui.geometry.CornerRadius(6f))
            drawRoundRect(if(b.hp<7) Color(0xFFFF7777) else NightMint,Offset(415f,1180f),Size(170f*b.hp/20f,12f),androidx.compose.ui.geometry.CornerRadius(6f))
            // Unoccupied altars and selected attack range.
            b.stage.seats.forEachIndexed { index,p ->
                val seat=b.seats.find {it.index==index}
                if(index==selected && seat!=null) {
                    val r=b.radius(seat)
                    drawCircle(seat.god.color().copy(alpha=.06f),r,p.o())
                    drawCircle(seat.god.color().copy(alpha=.35f),r,p.o(),style=Stroke(2f))
                }
                drawOval(Color.Black.copy(alpha=.35f),Offset(p.x-65,p.y-21),Size(130f,54f))
                drawOval(Color(0xFF344C60),Offset(p.x-58,p.y-30),Size(116f,48f))
                drawOval(if(index==selected || (moving>=0 && seat==null)) NightGold else Color(0xFF7193A8),Offset(p.x-58,p.y-30),Size(116f,48f),style=Stroke(if(index==selected) 5f else 2f))
                if(seat==null) label("＋",p.x,p.y+7,if(moving>=0) NightGold else Color(0xFFA2BDCF),36f)
            }
            // Depth ordering lets the cats sit naturally among the enemies.
            val drawables=(b.seats.map {s->Triple(b.stage.seats[s.index].y, s, null as Invader?)}+
                b.enemies.map {e->Triple(e.p.y,null as Seat?,e)}).sortedBy {it.first}
            drawables.forEach { (_,s,e) ->
                if(s!=null) {
                    val p=b.stage.seats[s.index];val leap=if(s.attack>0) sin(s.attack/.25f*PI).toFloat()*10 else 0f
                    sprite(art[s.god.art],p.x,p.y+5-leap,175f,if(s.sealed>0) .5f else 1f)
                    if(s.sealed>0) label("震懾",p.x,p.y-140,Color(0xFFFF9EB0),23f)
                    if(s.attack>0 && !quiet) glow(Offset(p.x,p.y-75),s.god.color(),70f,s.attack)
                    label("${s.god.title} ${"•".repeat(s.level)}",p.x,p.y+38,s.god.color(),22f)
                }
                if(e!=null) {
                    val p=e.p;val boss=e.kind==Foe.BOSS;val w=if(boss) 200f else 105f
                    val bob=sin(b.time*8+e.id)*3
                    drawOval(Color.Black.copy(alpha=.3f),Offset(p.x-w*.35f,p.y-9),Size(w*.7f,20f))
                    if(e.wet>0) drawOval(Color(0xFF5BCDF1).copy(alpha=.45f),Offset(p.x-46,p.y-13),Size(92f,22f))
                    sprite(art[if(boss) "nw_boss_${b.stage.id}" else "nw_foe_${e.kind.ordinal}"] ?: art[e.kind.art],p.x,p.y+bob,w,if(e.hit>0) .65f else 1f)
                    if(e.burn>0) {
                        glow(Offset(p.x,p.y-30),Color(0xFFFF722D),48f,.3f)
                        for(i in 0..2) {
                            val drift=(b.time*55+i*20)%60
                            drawCircle(if(i%2==0) NightGold else Color(0xFFFF6A43),5f,Offset(p.x-17+i*17,p.y-drift))
                        }
                    }
                    if(e.stun>0) drawArc(NightGold,b.time*150,270f,false,Offset(p.x-27,p.y-w+2),Size(54f,20f),style=Stroke(5f))
                    val bar=if(boss) 145f else 65f
                    drawRect(Color(0xFF18232C),Offset(p.x-bar/2,p.y-w+10),Size(bar,8f))
                    drawRect(if(boss) Color(0xFFFF6E87) else NightMint,Offset(p.x-bar/2,p.y-w+10),Size(bar*(e.hp/e.maxHp).coerceIn(0f,1f),8f))
                    if(boss) label(b.stage.boss,p.x,p.y-w,Color(0xFFFFB4D1),26f)
                }
            }
            b.effects.forEach { fx ->
                val t=(1-fx.life/fx.total).coerceIn(0f,1f)
                val alpha=(1-t).coerceIn(0f,1f)
                val color=fx.god?.color() ?: Color.White
                val a=fx.from.o();val z=fx.to.o()
                when(fx.kind) {
                    "nova" -> {
                        val center=Offset(500f,600f);val radius=100+t*650
                        if(!quiet) glow(center,color,radius,.15f*alpha)
                        drawCircle(color.copy(alpha=alpha),radius,center,style=Stroke(10f*(1-t)+1))
                        drawCircle(Color.White.copy(alpha=alpha*.65f),radius*.82f,center,style=Stroke(3f))
                        for(i in 0..if(quiet) 5 else 15) {
                            val angle=i*PI/8
                            val end=center+Offset(cos(angle).toFloat(),sin(angle).toFloat())*radius
                            drawLine(color.copy(alpha=alpha),center+(end-center)*.75f,end,5f)
                        }
                        label("${fx.god?.title}・神力解放",500f,570f-t*45,color.copy(alpha=alpha),42f)
                    }
                    "summon" -> {
                        drawCircle(color.copy(alpha=alpha),40f+t*90,z,style=Stroke(5f))
                        drawLine(color.copy(alpha=alpha),z,z-Offset(0f,160f*(1-t)),7f)
                    }
                    "move" -> {drawLine(color.copy(alpha=alpha),a,z,7f);drawCircle(color.copy(alpha=alpha),35f+t*40,z,style=Stroke(4f))}
                    "poof" -> for(i in 0..5) {
                        val angle=i*PI/3
                        drawCircle(NightGold.copy(alpha=alpha),5f, z+Offset(cos(angle).toFloat(),sin(angle).toFloat())*(15+t*40))
                    }
                    "enemyBlast" -> drawCircle(Color(0xFFFF6688).copy(alpha=alpha),40+t*240,z,style=Stroke(12f*alpha+1))
                    "breach" -> drawCircle(Color(0xFFFF6677).copy(alpha=alpha),30+t*90,z,style=Stroke(8f))
                    else -> {
                        when(fx.god) {
                            God.ZEUS -> {
                                val path=Path().apply {moveTo(a.x,a.y-70);for(i in 1..6) {val f=i/7f;lineTo(a.x+(z.x-a.x)*f+(if(i%2==0) 18 else -18),a.y-70+(z.y-a.y+40)*f)};lineTo(z.x,z.y-30)}
                                if(!quiet) drawPath(path,color.copy(alpha=alpha*.2f),style=Stroke(24f))
                                drawPath(path,color.copy(alpha=alpha),style=Stroke(8f));drawPath(path,Color.White.copy(alpha=alpha),style=Stroke(3f))
                            }
                            God.POSEIDON -> {
                                drawCircle(color.copy(alpha=alpha*.8f),20f+t*80,z,style=Stroke(7f));drawCircle(Color.White.copy(alpha=alpha*.6f),10+t*60,z,style=Stroke(3f))
                                drawLine(color.copy(alpha=alpha*.55f),a-Offset(0f,55f),z,10f)
                            }
                            God.NEZHA -> {
                                val end=a+(z-a)*min(1f,t*3f)-Offset(0f,35f)
                                drawLine(Color(0xFFFA583C).copy(alpha=alpha),a-Offset(0f,50f),end,14f)
                                drawCircle(NightGold.copy(alpha=alpha),12f,end)
                                drawCircle(Color(0xFFFF7745).copy(alpha=alpha),15+t*50,z-Offset(0f,25f),style=Stroke(8f))
                            }
                            God.WIND -> {
                                drawArc(color.copy(alpha=alpha),t*320,250f,false,z-Offset(60f+t*35,45f+t*20),Size(120+t*70,90+t*40),style=Stroke(7f))
                                drawLine(color.copy(alpha=alpha*.4f),a-Offset(0f,60f),z-Offset(0f,20f),5f)
                            }
                            God.ATHENA -> {
                                drawLine(color.copy(alpha=alpha),z-Offset(25f,65f),z+Offset(25f,-5f),8f)
                                drawLine(Color.White.copy(alpha=alpha),z+Offset(25f,-65f),z-Offset(25f,5f),5f)
                            }
                            else -> {drawCircle(color.copy(alpha=alpha),25+t*35,z,style=Stroke(5f))}
                        }
                        if(!quiet) glow(z-Offset(0f,25f),color,65f,.3f*alpha)
                    }
                }
            }
            if(b.flood>0 && !quiet) for(i in 0..24) {
                val x=(i*137%1000).toFloat();val y=(i*211+b.time*260)%1200
                drawLine(Color(0xFF7AD2EA).copy(alpha=.22f),Offset(x,y),Offset(x-7,y+28),2f)
            }
        }}
    }
}
