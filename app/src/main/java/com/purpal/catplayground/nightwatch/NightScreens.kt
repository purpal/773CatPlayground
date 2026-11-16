package com.purpal.catplayground.nightwatch

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.ArtImages
import com.purpal.catplayground.ui.FitText
import com.purpal.catplayground.ui.PillButton

internal val NightGold=Color(0xFFFFD578)
internal val NightInk=Color(0xFF0C1628)
internal val NightPanel=Color(0xFF1B2B43)
internal val NightMint=Color(0xFF76E3D2)
internal fun God.color()= when(this) {
    God.ZEUS->Color(0xFFFFDB63); God.POSEIDON->Color(0xFF64DFFF); God.NEZHA->Color(0xFFFF754F)
    God.ATHENA->Color(0xFFFFB9BB); God.WIND->Color(0xFFA8ED8C); God.EARTH->Color(0xFFEAC488)
}

/** Only the fifth game's preferences are touched. Best stars never regress. */
class NightStore(context: Context) {
    private val prefs=context.getSharedPreferences("cat_playground_nightwatch",Context.MODE_PRIVATE)
    fun stars(stage: Int)=prefs.getInt("stars_$stage",0)
    fun win(stage: Int, stars: Int) {prefs.edit().putInt("stars_$stage",maxOf(stars(stage),stars)).apply()}
    var team: List<God>
        get() = prefs.getString("team",null)?.split(",")?.mapNotNull { s->God.entries.find { it.name==s } }?.distinct()?.takeIf { it.size==4 }
            ?: listOf(God.ZEUS,God.POSEIDON,God.NEZHA,God.ATHENA)
        set(value) { if(value.distinct().size==4) prefs.edit().putString("team",value.joinToString(",") { it.name }).apply() }
    var quietFx: Boolean
        get()=prefs.getBoolean("quiet_fx",false)
        set(value) {prefs.edit().putBoolean("quiet_fx",value).apply()}
}

@Composable
private fun NightText(text: String, color: Color=Color.White, size: Int=14) {
    Text(text,color=color,fontSize=size.sp,lineHeight=(size+6).sp)
}
@Composable
private fun NightPanel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) {
    Column(modifier.background(NightPanel,RoundedCornerShape(18.dp)).border(1.dp,Color.White.copy(alpha=.12f),RoundedCornerShape(18.dp)).padding(14.dp), content=content)
}
@Composable
fun NightMenuScreen(onBack: ()->Unit, onPlay: (Int,List<God>)->Unit) {
    val context=LocalContext.current
    val store=remember {NightStore(context)}
    var team by remember {mutableStateOf(store.team)}
    var help by remember {mutableStateOf(false)}
    var quiet by remember {mutableStateOf(store.quietFx)}
    val stars=NightStage.all.sumOf { store.stars(it.id) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF213C58),NightInk))).systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            PillButton("返回",onClick=onBack)
            Spacer(Modifier.weight(1f))
            PillButton("玩法") {help=true}
        }
        NightText("NIGHTWATCH / 第五款獨立遊戲",NightMint,12)
        Text("喵神守夜",fontSize=38.sp,fontWeight=FontWeight.Black,color=NightGold)
        NightText("最後一座罐罐神殿",Color.White,19)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
            ArtImage(God.ZEUS.art,Modifier.weight(1f).height(130.dp))
            ArtImage(God.POSEIDON.art,Modifier.weight(1f).height(130.dp).padding(top=15.dp))
            ArtImage(God.NEZHA.art,Modifier.weight(1f).height(130.dp).padding(top=15.dp))
        }
        NightPanel(Modifier.fillMaxWidth()) {
            NightText("神社復興　$stars / 9 星",NightGold,18)
            NightText(when {stars>=7->"長明神殿已點亮。六位神貓的故事，將由你繼續守護。";stars>=4->"修好屋頂，開張罐罐食堂。夜裡終於有暖呼呼的晚餐。";stars>0->"修好鳥居，點亮第一盞燈。被遺忘的神話開始回家。";else->"人類忘記了神話。今晚，六隻貓決定替世界守住晚餐。"})
        }
        NightText("出戰神貓　${team.size} / 4",NightGold,19)
        NightText("點選移出或加入。每隻貓本局只能召喚一次，可抱著神座搬家。",Color(0xFFB1C4DB),13)
        for(row in God.entries.chunked(3)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach { god ->
                    val picked=god in team
                    Column(Modifier.weight(1f).background(if(picked) Color(0xFF30435A) else NightPanel,RoundedCornerShape(16.dp))
                        .border(if(picked) 2.dp else 1.dp,if(picked) god.color() else Color(0xFF37445A),RoundedCornerShape(16.dp))
                        .clickable {team=if(picked) team-god else if(team.size<4) team+god else team; if(team.size==4) store.team=team}.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        ArtImage(god.art,Modifier.fillMaxWidth().height(100.dp),god.title)
                        NightText("${if(picked) "✓ " else ""}${god.title}",god.color(),15)
                        NightText("${god.cost} 供品",Color.White,12)
                    }
                }
            }
        }
        NightPanel(Modifier.fillMaxWidth()) {
            for(god in team) NightText("${god.title}｜${god.role}",god.color(),13)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Switch(checked=quiet,onCheckedChange={quiet=it;store.quietFx=it})
                Spacer(Modifier.width(8.dp)); NightText("柔和特效（減少光暈與粒子）",size=13)
            }
        }
        NightText("選擇守夜地點",NightGold,20)
        NightStage.all.forEach { stage ->
            val unlocked=stage.id==0 || store.stars(stage.id-1)>0
            NightPanel(Modifier.fillMaxWidth()) {
                NightText("0${stage.id+1}　${stage.name}",if(unlocked) NightGold else Color.Gray,21)
                NightText("${stage.subtitle}　${"★".repeat(store.stars(stage.id))}${"☆".repeat(3-store.stars(stage.id))}",NightMint)
                NightText("頭目：${stage.boss} · 10 波攻勢",size=13)
                Spacer(Modifier.height(8.dp))
                NightText(stage.description,Color(0xFFBECCDC),13)
                Spacer(Modifier.height(12.dp))
                PillButton(if(!unlocked) "通過前一關解鎖" else if(team.size!=4) "請選滿四位神貓" else "開始守夜",Modifier.fillMaxWidth(),NightGold,NightInk,enabled=unlocked && team.size==4) {store.team=team;onPlay(stage.id,team.toList())}
            }
        }
        NightText("小提醒：雷＋水會連鎖；風＋火能擴散；浪潮會沖掉火焰。",NightMint,13)
        Spacer(Modifier.height(12.dp))
    }
    if(help) AlertDialog(onDismissRequest={help=false},containerColor=NightPanel,title={NightText("今晚怎麼守？",NightGold,22)},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            NightText("① 選四位神貓。點下方貓卡，再點地圖發光圓座召喚。")
            NightText("② 點已佔用神座：選擇兩條升級分支，或花 15 供品換到空座。")
            NightText("③ 準備好後按「迎敵」。神貓自動攻擊，擊倒敵人獲得供品。")
            NightText("④ 波次結束選一項祝福，再重新布防。戰鬥中也能召喚、升級、換座。")
            NightText("⑤ 選中已上場的貓，按「神諭」施放大招；全隊共用冷卻。地圖機關可破頭目護罩。")
            NightText("⑥ 石獅不能擊退，水妖在積水加速，報時鬼加速同伴。第十波打倒頭目並清空敵軍即獲勝。")
            NightText("耐久 17 以上三星、10 以上二星，其餘一星。中途離開不保存戰局；過關星星永久保存。",NightMint,13)
        }
    },confirmButton={TextButton(onClick={help=false}) {Text("開始布防",color=NightGold)}})
}

@Composable
fun NightGameScreen(stageId: Int, team: List<God>, onExit: ()->Unit) {
    val context=LocalContext.current
    val store=remember {NightStore(context)}
    var attempt by remember {mutableIntStateOf(0)}
    val battle=remember(stageId,attempt) {NightBattle(NightStage.all[stageId.coerceIn(0,2)],team)}
    var frame by remember {mutableIntStateOf(0)}
    var hud by remember {mutableIntStateOf(0)}
    var selected by remember(attempt) {mutableStateOf<God?>(team.first())}
    var seatIndex by remember(attempt) {mutableIntStateOf(-1)}
    var moving by remember(attempt) {mutableIntStateOf(-1)}
    var paused by remember {mutableStateOf(false)}
    var quit by remember {mutableStateOf(false)}
    var speed by remember {mutableIntStateOf(1)}
    var quiet by remember {mutableStateOf(store.quietFx)}
    var upgradeBranch by remember {mutableStateOf<Int?>(null)}
    var feedback by remember {mutableStateOf<String?>(null)}
    val sound=LocalSound.current
    val activity=context as? ComponentActivity
    DisposableEffect(activity) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_STOP) paused=true}
        activity?.lifecycle?.addObserver(observer)
        onDispose {activity?.lifecycle?.removeObserver(observer)}
    }
    BackHandler {paused=true;quit=true}
    LaunchedEffect(battle) {
        var last=withFrameNanos {it}
        var lastPhase=battle.phase
        var lastHud=0L
        var lastSound=0f
        while(true) {
            val now=withFrameNanos {it}; val dt=((now-last)/1_000_000_000f).coerceIn(0f,.05f); last=now
            if(!paused && !quit && upgradeBranch==null) repeat(speed) {battle.tick(dt)}
            if(lastPhase!=battle.phase) {
                when(battle.phase) {
                    NightPhase.WON->{store.win(stageId,battle.stars);sound.play(Sfx.WIN)}
                    NightPhase.LOST->sound.play(Sfx.PLAYER_HIT)
                    NightPhase.BLESSING->sound.play(Sfx.RANK_UP)
                    else->Unit
                }
                lastPhase=battle.phase
            }
            if(!paused && !quiet && battle.time-lastSound>.3f && battle.effects.lastOrNull()?.let {it.kind=="hit" && it.life>.38f}==true) {
                sound.play(if(battle.effects.last().god==God.ZEUS) Sfx.LASER else Sfx.SHOT)
                lastSound=battle.time
            }
            if(now-lastHud>100_000_000L) {hud++;lastHud=now}
            frame++
        }
    }
    // Observe the mutable simulation once per rendered frame.
    @Suppress("UNUSED_VARIABLE") val observed=hud
    val assets=remember { (God.entries.map {it.art}+Foe.entries.map {it.art}+(0..5).map {"nw_foe_$it"}+(0..2).map {"nw_boss_$it"}).distinct().associateWith {ArtImages.get(context,it)} }
    val seat=battle.seats.find {it.index==seatIndex}
    fun act(ok: Boolean) {feedback=if(ok) null else "供品不足或位置無法使用";sound.play(if(ok) Sfx.SELECT else Sfx.INVALID);frame++;hud++}
    Column(Modifier.fillMaxSize().background(NightInk).systemBarsPadding().padding(horizontal=8.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            PillButton("Ⅱ") {paused=true}
            Column(Modifier.weight(1f)) {
                FitText(battle.stage.name,color=NightGold,fontWeight=FontWeight.Bold,fontSize=17.sp)
                FitText("第 ${battle.wave} / 10 夜　耐久 ${battle.hp}/20",color=Color.White,fontSize=13.sp)
            }
            NightText("供品 ${battle.offerings}",NightGold,16)
            PillButton("${speed}×") {speed=if(speed==1) 2 else 1}
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF172B3D),RoundedCornerShape(16.dp))) {
            // Fit the logical map without distorting it; input uses the identical transform.
            Canvas(Modifier.fillMaxSize().pointerInput(battle,selected,moving,paused,quit) {
                detectTapGestures { tap ->
                    if(paused || quit || battle.phase !in listOf(NightPhase.PREPARE,NightPhase.FIGHT)) return@detectTapGestures
                    val scale=minOf(size.width/1000f,size.height/1200f)
                    val px=(tap.x-(size.width-1000*scale)/2)/scale
                    val py=(tap.y-(size.height-1200*scale)/2)/scale
                    val index=battle.stage.seats.indices.minByOrNull {battle.stage.seats[it].distance(Point(px,py))} ?: return@detectTapGestures
                    if(battle.stage.seats[index].distance(Point(px,py))>80) {seatIndex=-1;return@detectTapGestures}
                    val existing=battle.seats.find {it.index==index}
                    if(moving>=0 && existing==null) {act(battle.move(moving,index));moving=-1;seatIndex=index}
                    else if(existing!=null) {seatIndex=index;selected=existing.god;moving=-1;feedback=null}
                    else selected?.let {act(battle.place(it,index));if(battle.seats.any {s->s.index==index}) seatIndex=index}
                }
            }) {
                @Suppress("UNUSED_VARIABLE") val redraw=frame
                drawNightBattle(battle,assets,seatIndex,moving,quiet)
            }
            if(battle.phase==NightPhase.PREPARE) Text(if(battle.wave==0) "日暮・點亮神座" else "黎明・重新布防",color=NightGold,fontSize=14.sp,modifier=Modifier.align(Alignment.TopCenter).padding(7.dp))
        }
        FitText(feedback ?: if(moving>=0) "搬家中：點一個空神座（${battle.moveCost} 供品），或再點貓卡取消" else battle.notice,
            color=if(feedback!=null) Color(0xFFFFA28F) else NightMint,fontSize=12.sp,modifier=Modifier.fillMaxWidth().padding(vertical=5.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            team.forEach { god ->
                val placed=battle.seats.any {it.god==god}
                Column(Modifier.weight(1f).background(if(selected==god) Color(0xFF364761) else NightPanel,RoundedCornerShape(12.dp))
                    .border(1.dp,if(selected==god) god.color() else Color.Transparent,RoundedCornerShape(12.dp))
                    .clickable {selected=god;seatIndex=battle.seats.find {it.god==god}?.index ?: -1;moving=-1;feedback=null}.padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    ArtImage(god.art,Modifier.height(56.dp).fillMaxWidth(),god.title)
                    FitText(god.title,color=god.color(),fontSize=13.sp)
                    FitText(if(placed) "已就位" else "${god.cost} 供品",color=Color.White,fontSize=11.sp)
                }
            }
        }
        if(seat!=null) {
            Row(Modifier.fillMaxWidth().padding(top=5.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                if(seat.level==1) seat.god.paths.forEachIndexed { branch,name ->
                    PillButton("$name ${seat.upgradeCost}",Modifier.weight(1f),NightPanel,enabled=battle.offerings>=seat.upgradeCost) {upgradeBranch=branch}
                } else PillButton(if(seat.level>=3) "${seat.god.paths[seat.branch]} MAX" else "${seat.god.paths[seat.branch]} ↑ ${seat.upgradeCost}",Modifier.weight(1f),NightPanel,enabled=seat.level<3 && battle.offerings>=seat.upgradeCost) {act(battle.upgrade(seat.index,seat.branch))}
                PillButton("換座 ${battle.moveCost}",background=NightPanel,enabled=battle.offerings>=battle.moveCost) {moving=seat.index}
            }
        } else FitText(selected?.role ?: "選擇一位神貓",color=NightGold,fontSize=12.sp,modifier=Modifier.padding(vertical=5.dp))
        Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            if(battle.phase==NightPhase.PREPARE) {
                PillButton("迎敵 ▶",Modifier.weight(1f),NightGold,NightInk,enabled=battle.seats.isNotEmpty()) {act(battle.startWave())}
            } else {
                PillButton(if(battle.mechanism>0) "機關 ${battle.mechanism.toInt()+1}s" else battle.stage.mechanism,Modifier.weight(1f),NightPanel,enabled=battle.phase==NightPhase.FIGHT && battle.mechanism<=0) {act(battle.activateMechanism());sound.play(Sfx.SONIC)}
            }
            PillButton(if(battle.ultimate>0) "神諭 ${battle.ultimate.toInt()+1}s" else "${selected?.title ?: "神貓"}神諭",Modifier.weight(1f),Color(0xFF57355F),enabled=battle.phase==NightPhase.FIGHT && battle.ultimate<=0 && battle.seats.any {it.god==selected}) {selected?.let {act(battle.invoke(it));sound.play(Sfx.BLAST)}}
        }
    }
    val branch=upgradeBranch
    if(branch!=null && seat!=null && !paused && !quit) AlertDialog(onDismissRequest={upgradeBranch=null},containerColor=NightPanel,
        title={NightText("${seat.god.title}・${seat.god.paths[branch]}",NightGold,22)},
        text={NightText("${seat.god.branchHelp[branch]}\n花費 ${seat.upgradeCost} 供品，分支選定後本局不再更換。")},
        confirmButton={TextButton(onClick={act(battle.upgrade(seat.index,branch));upgradeBranch=null}) {Text("確認升級",color=NightGold)}},
        dismissButton={TextButton(onClick={upgradeBranch=null}) {Text("返回",color=Color.White)}})
    if(battle.phase==NightPhase.BLESSING && !paused && !quit) AlertDialog(onDismissRequest={},containerColor=NightPanel,title={NightText("黎明的祝福",NightGold,24)},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            NightText("第 ${battle.wave} 夜守住了！選一項本局永久效果。",size=13)
            battle.choices.forEach { boon ->
                Column(Modifier.fillMaxWidth().background(Color(0xFF30415B),RoundedCornerShape(12.dp)).clickable {battle.choose(boon.id);seatIndex=-1;feedback=null;frame++;sound.play(Sfx.POWER_UP)}.padding(12.dp)) {
                    NightText(boon.title,NightGold,18);NightText(boon.text,size=13)
                }
            }
        }
    },confirmButton={})
    if(battle.phase==NightPhase.WON || battle.phase==NightPhase.LOST) AlertDialog(onDismissRequest={},containerColor=NightPanel,title={NightText(if(battle.phase==NightPhase.WON) "守夜成功！" else "天亮前，再試一次",NightGold,25)},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(battle.stars>0) NightText("★".repeat(battle.stars)+"☆".repeat(3-battle.stars),NightGold,30)
            NightText(battle.notice)
            NightText("守住 ${battle.wave} 夜 · 擊退 ${battle.kills} 隻 · 耐久 ${battle.hp}/20")
            if(battle.phase==NightPhase.WON) NightText(if(stageId<2) "星星已保存，下一個守夜地點已開放。" else "三座神社的燈火都回來了。可換隊挑戰三星！",NightMint)
        }
    },confirmButton={TextButton(onClick=onExit) {Text("回神社",color=NightGold)}},dismissButton={TextButton(onClick={attempt++;paused=false;quit=false}) {Text("再次守夜",color=Color.White)}})
    if(paused && !quit && battle.phase !in listOf(NightPhase.WON,NightPhase.LOST)) AlertDialog(onDismissRequest={paused=false},containerColor=NightPanel,title={NightText("夜色暫停",NightGold,23)},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            NightText("四貓一隊。點貓卡再點空座召喚；點神座升級、換座。")
            NightText("雷＋水連鎖，風＋火擴散。機關能讓頭目破防，神諭全隊共用冷卻。")
            NightText("祝福："+blessings.filter {it.id in battle.boons}.joinToString("、") {it.title}.ifEmpty {"尚無"},NightMint,13)
            Row(verticalAlignment=Alignment.CenterVertically) {Switch(quiet,{quiet=it;store.quietFx=it});NightText("柔和特效",size=13)}
        }
    },confirmButton={TextButton(onClick={paused=false}) {Text("繼續守夜",color=NightGold)}},dismissButton={TextButton(onClick={quit=true}) {Text("離開戰局",color=Color.White)}})
    if(quit) AlertDialog(onDismissRequest={quit=false},containerColor=NightPanel,title={NightText("離開本次守夜？",NightGold,22)},text={NightText("已取得的星星會保留，本次尚未完成的戰局不會保存。")},confirmButton={TextButton(onClick=onExit) {Text("離開",color=NightGold)}},dismissButton={TextButton(onClick={quit=false;paused=false}) {Text("繼續",color=Color.White)}})
}
