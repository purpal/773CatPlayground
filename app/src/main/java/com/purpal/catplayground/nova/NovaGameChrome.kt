package com.purpal.catplayground.nova

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.purpal.catplayground.shooter.model.*
import com.purpal.catplayground.shooter.ui.HudState

@Composable
fun NovaHud(hud: HudState, stage: String, title: String, modifier: Modifier = Modifier, onPause: ()->Unit) {
    Box(modifier.fillMaxWidth().background(NovaInk).statusBarsPadding()) {
    NovaPanelArt("UI_HUD",Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("$stage  /  $title",color=NovaCyan,fontSize=11.sp)
            Text("${hud.score.toString().padStart(8,'0')}  CR",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FitText("喵甲 ${hud.armor}%",Modifier.weight(.8f),color=NovaGold,fontSize=11.sp)
                FitText("主武 Lv${hud.mainLevel}",Modifier.weight(1f),color=NovaGold,fontSize=11.sp)
                FitText(if(hud.subWeapon==SubWeapon.NONE)"副武 —" else "副武 Lv${hud.subLevel}",Modifier.weight(1f),color=NovaCyan,fontSize=11.sp)
            }
        }
        TextButton(onClick=onPause){Text("暫停",color=NovaCyan)}
    }
    }
    }
}

@Composable
fun NovaPause(onResume:()->Unit,onRestart:()->Unit,onExit:()->Unit) {
    AlertDialog(onDismissRequest=onResume,containerColor=NovaPanel,
        title={Text("航控暫停",color=NovaCyan)},text={Text("機體已進入待命狀態",color=Color.White)},
        confirmButton={TextButton(onClick=onResume){Text("繼續航行",color=NovaCyan)}},
        dismissButton={Row{TextButton(onClick=onRestart){Text("重啟本戰",color=NovaGold)};TextButton(onClick=onExit){Text("撤回航路",color=Color.White)}}})
}

@Composable
fun NovaStageOverlays(world:World,phase:Phase,bestScore:Int,hasNext:Boolean,onNext:()->Unit,onRestart:()->Unit,onExit:()->Unit) {
    var line by remember(world,phase){mutableIntStateOf(0)}
    val blocking=phase==Phase.OUTRO_DIALOG || phase==Phase.CONTINUE || phase==Phase.CLEAR
    val visible=blocking || phase==Phase.INTRO || phase==Phase.WARNING
    if(!visible)return
    val lines = remember(world) { novaOutro(world.stage, world.player.cat) }
    val tapAnywhere = when(phase) {
        Phase.OUTRO_DIALOG -> Modifier.clickable(onClickLabel="繼續通訊") {
            if(line+1>=lines.size)world.dialogFinished()else line++
        }
        Phase.CLEAR -> Modifier.clickable(onClickLabel="返回航路") { if(hasNext)onNext()else onExit() }
        else -> Modifier
    }
    Box(Modifier.fillMaxSize().then(tapAnywhere).background(Color.Black.copy(alpha=if(blocking).86f else .35f)),contentAlignment=Alignment.Center) {
        NovaPanelArt("UI_DIALOG",Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=28.dp,vertical=32.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when {
                phase==Phase.INTRO -> {
                    Text("戰區同步",color=NovaCyan,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("${world.stage.country.name}｜${world.stage.title}",color=Color.White,fontSize=18.sp)
                    Text("按住畫面下方自動射擊\n拖動控制機體・快速甩動啟動特技",color=NovaGold,fontSize=14.sp)
                }
                phase==Phase.WARNING -> {
                    Text("敵方旗艦接近",color=Color(0xFFFF6378),fontSize=30.sp,fontWeight=FontWeight.Black)
                    NovaSector.preview(world.stage.number)?.let { NovaAsset(it,Modifier.size(160.dp)) }
                    Text(world.stage.bossName,color=Color.White,fontSize=24.sp)
                    Text("追蹤預警線，保持中央避讓通道",color=NovaGold,fontSize=13.sp)
                }
                phase==Phase.OUTRO_DIALOG -> {
                    val current=lines[line]
                    val catSpeaking=current.speaker==Speaker.CAT
                    Text("戰後通訊  /  ${line+1}・${lines.size}",color=NovaCyan,fontSize=12.sp)
                    if(catSpeaking) NovaAsset(if(world.player.cat==CatType.ORANGE)"pilot_orange"else"pilot_black",Modifier.height(150.dp))
                    else if(NovaSector.pilot(world.stage.number)!=null) NovaAsset(NovaSector.pilot(world.stage.number)!!,Modifier.size(160.dp),"${world.stage.bossName}駕駛員")
                    else ArtImage("${world.stage.bossArt}_m0_0",Modifier.size(150.dp),world.stage.bossName)
                    Text(if(catSpeaking)world.player.cat.shortName else world.stage.bossName,color=NovaGold,fontWeight=FontWeight.Bold)
                    Text(current.text + if(line<lines.lastIndex) " ▼" else "",color=Color.White,fontSize=17.sp,lineHeight=27.sp)
                    Text("輕觸任意處繼續",color=NovaCyan.copy(alpha=.65f),fontSize=11.sp)
                }
                phase==Phase.CONTINUE -> {
                    Text("機體失聯",color=NovaGold,fontSize=28.sp)
                    Text("剩餘救援 ${world.continuesLeft} 次",color=Color.White)
                    if(world.continuesLeft>0)NovaButton("緊急回收・接續作戰"){world.useContinue()}
                    NovaButton("重啟本戰",onClick=onRestart)
                    NovaButton("撤回航路",onClick=onExit)
                }
                phase==Phase.CLEAR -> {
                    Text("戰區解除",color=NovaCyan,fontSize=30.sp,fontWeight=FontWeight.Black)
                    Text("戰果 ${world.score} CR",color=NovaGold,fontSize=22.sp)
                    if(world.score>bestScore)Text("航誌最高紀錄更新",color=NovaCyan)
                    Text(if(hasNext)"輕觸任意處返回航線選擇"else"遠征完成・輕觸任意處返回航線選擇",color=NovaCyan,fontSize=14.sp)
                }
            }
        }
    }
    }
}
