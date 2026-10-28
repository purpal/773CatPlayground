package com.purpal.catplayground.nova

import androidx.compose.foundation.background
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
    Row(modifier.fillMaxWidth().background(NovaInk).statusBarsPadding().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("$stage  /  $title",color=NovaCyan,fontSize=11.sp)
            Text("${hud.score.toString().padStart(8,'0')}  CR",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
            Text("喵甲 ${hud.armor}%   ${hud.mainWeapon.label} Lv${hud.mainLevel}",color=NovaGold,fontSize=12.sp)
            Text(if(hud.subWeapon==SubWeapon.NONE)"副掛載待命"else"${hud.subWeapon.label} Lv${hud.subLevel}",color=NovaCyan,fontSize=11.sp)
        }
        TextButton(onClick=onPause){Text("暫停",color=NovaCyan)}
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
fun NovaStageOverlays(world:World,phase:Phase,overdrive:Boolean,bestScore:Int,hasNext:Boolean,onNext:()->Unit,onRestart:()->Unit,onExit:()->Unit) {
    var line by remember(world,phase){mutableIntStateOf(0)}
    val blocking=phase==Phase.OUTRO_DIALOG || phase==Phase.CONTINUE || phase==Phase.CLEAR
    val visible=blocking || phase==Phase.INTRO || phase==Phase.WARNING || overdrive
    if(!visible)return
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(blocking).86f else .35f)),contentAlignment=Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(24.dp).background(NovaPanel,RoundedCornerShape(12.dp)).padding(20.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when {
                overdrive -> {
                    Text("核心超載",color=NovaGold,fontSize=32.sp,fontWeight=FontWeight.Black)
                    NovaAsset(if(world.player.cat==CatType.ORANGE)"ship_orange"else"ship_black",Modifier.size(150.dp))
                    Text(if(world.player.cat==CatType.ORANGE)"猛虎反應爐啟動"else"黑豹相位核心啟動",color=NovaCyan)
                }
                phase==Phase.INTRO -> {
                    Text("戰區同步",color=NovaCyan,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("${world.stage.country.name}｜${world.stage.title}",color=Color.White,fontSize=18.sp)
                    Text("按住畫面下方自動射擊\n拖動控制機體・快速甩動啟動特技",color=NovaGold,fontSize=14.sp)
                }
                phase==Phase.WARNING -> {
                    Text("敵方旗艦接近",color=Color(0xFFFF6378),fontSize=30.sp,fontWeight=FontWeight.Black)
                    if(world.stage.number==2)NovaAsset("boss",Modifier.size(160.dp))
                    Text(world.stage.bossName,color=Color.White,fontSize=24.sp)
                    Text("追蹤預警線，保持中央避讓通道",color=NovaGold,fontSize=13.sp)
                }
                phase==Phase.OUTRO_DIALOG -> {
                    val lines=world.stage.outro
                    val current=lines.getOrNull(line)
                    if(current!=null) {
                        Text(if(current.speaker==Speaker.CAT)world.player.cat.shortName else world.stage.bossName,color=NovaGold,fontWeight=FontWeight.Bold)
                        Text(current.text,color=Color.White,fontSize=16.sp)
                    }
                    NovaButton("繼續通訊") { if(line+1>=lines.size)world.dialogFinished()else line++ }
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
                    NovaButton(if(hasNext)"返回地球儀・下一站"else"完成遠征",onClick=if(hasNext)onNext else onExit)
                    NovaButton("返回航路",onClick=onExit)
                }
            }
        }
    }
}
