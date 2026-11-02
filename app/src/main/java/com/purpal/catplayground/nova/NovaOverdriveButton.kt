package com.purpal.catplayground.nova

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.ui.ArtImage

/** Mechanical housing preserves the familiar cat/mouth-cannon emblem and its large touch target. */
@Composable
fun NovaOverdriveButton(cat:CatType,bombs:Int,onPress:()->Unit,modifier:Modifier=Modifier) {
    val ready=bombs>0
    val accent=if(cat==CatType.ORANGE)Color(0xFFFFB65C)else Color(0xFFB399FF)
    Column(modifier, horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).clickable(enabled=ready,role=Role.Button,onClickLabel="核心超載",onClick=onPress),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cut=size.width*.18f
                val frame=Path().apply {
                    moveTo(cut,2f);lineTo(size.width-cut,2f);lineTo(size.width-2f,cut)
                    lineTo(size.width-2f,size.height-cut);lineTo(size.width-cut,size.height-2f)
                    lineTo(cut,size.height-2f);lineTo(2f,size.height-cut);lineTo(2f,cut);close()
                }
                drawPath(frame,Brush.linearGradient(listOf(Color(0xFF243A50),NovaInk)))
                drawPath(frame,accent.copy(alpha=if(ready).9f else .25f),style=Stroke(2.dp.toPx()))
                drawCircle(accent.copy(alpha=if(ready).13f else .03f),size.width*.37f)
                for(side in listOf(-1,1)) {
                    val x=if(side<0)size.width*.1f else size.width*.9f
                    drawLine(NovaCyan.copy(alpha=if(ready).8f else .2f),Offset(x,size.height*.37f),Offset(x,size.height*.63f),2.dp.toPx())
                }
                drawArc(accent.copy(alpha=.5f),210f,120f,false,Offset(size.width*.17f,size.height*.17f),Size(size.width*.66f,size.height*.66f),style=Stroke(1.dp.toPx()))
            }
            ArtImage(if(cat==CatType.ORANGE)"ult_btn_orange"else"ult_btn_black",Modifier.size(53.dp),"核心超載，剩餘 $bombs 次",if(ready).95f else .3f)
        }
        Row(Modifier.padding(top=4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            repeat(3){i->Box(Modifier.size(18.dp,5.dp).background(if(i<bombs)accent else Color(0xFF243A50),RoundedCornerShape(1.dp)).border(.5.dp,accent.copy(alpha=.4f),RoundedCornerShape(1.dp)))}
        }
        Text("核心超載",color=if(ready)NovaCyan else Color(0xFF718296),fontSize=10.sp)
    }
}
