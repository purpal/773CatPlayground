package com.purpal.catplayground.nova

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.data.AppPrefs
import com.purpal.catplayground.shooter.model.*
import com.purpal.catplayground.ui.FitText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val NovaInk = Color(0xFF07121F)
val NovaPanel = Color(0xFF102737)
val NovaCyan = Color(0xFF68E5F5)
val NovaGold = Color(0xFFFFC077)

@Composable
fun NovaBackdrop(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF142F42), NovaInk))).systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

@Composable
fun NovaButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val sound = LocalSound.current
    Button(onClick = { sound.play(Sfx.SELECT); onClick() }, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = NovaInk),
        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NovaAsset(id: String, modifier: Modifier = Modifier, description: String? = null) {
    val assets = NovaAssets.get(LocalContext.current.assets)
    val bitmap = remember(id) { assets.image(id) }
    Image(bitmap, description, modifier, contentScale = ContentScale.Fit)
}

internal fun DrawScope.novaTurn(assets: NovaAssets,id:String,center:Offset,width:Float,alpha:Float) {
    val image=assets.image(id)
    val a=assets.anchor(id) ?: (image.width/2f to image.height/2f)
    val scale=width/image.width
    drawImage(image,dstOffset=IntOffset((center.x-a.first*scale).toInt(),(center.y-a.second*scale).toInt()),dstSize=IntSize(width.toInt(),(image.height*scale).toInt()),alpha=alpha)
}

internal fun DrawScope.novaImage(image: ImageBitmap, center: Offset, width: Float, alpha: Float = 1f) {
    val h = width * image.height / image.width
    drawImage(image, dstOffset = IntOffset((center.x-width/2).toInt(),(center.y-h/2).toInt()),
        dstSize = IntSize(width.toInt().coerceAtLeast(1),h.toInt().coerceAtLeast(1)), alpha = alpha)
}

fun novaName(cat: CatType) = if (cat == CatType.ORANGE) "橘曜｜重裝喵衛" else "玄影｜疾風獵喵"

@Composable
fun NovaMenuScreen(prefs: AppPrefs, tour: TourState?, onBack: () -> Unit, onNewTour: (CatType) -> Unit, onContinue: () -> Unit) {
    var cat by remember { mutableStateOf(runCatching { CatType.valueOf(prefs.shooterCat) }.getOrDefault(CatType.ORANGE)) }
    var confirm by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    val sound = LocalSound.current
    val assets = NovaAssets.get(LocalContext.current.assets)
    val loop = rememberInfiniteTransition(label = "hangar")
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(assets) {
        withContext(Dispatchers.Default) { assets.prewarmHangar() }
        ready = true
    }
    val breath by loop.animateFloat(.35f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "engine breathing")
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF142F42),NovaInk))).systemBarsPadding()) {
        val compact=maxHeight<720.dp
        Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(if(compact)6.dp else 10.dp)) {
            Row(Modifier.fillMaxWidth().height(36.dp),verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick=onBack){Text("返回大廳",color=NovaCyan)}
                Spacer(Modifier.weight(1f))
                TextButton(onClick={settings=true}){Text("艦載設定",color=NovaCyan)}
            }
            Text("喵航特攻隊",fontSize=if(compact)24.sp else 28.sp,fontWeight=FontWeight.Black,color=Color.White)
            Text(if(ready)"FELINE FLIGHT COMMAND / 機庫連線中"else"機庫素材同步中…",color=NovaCyan,fontSize=10.sp)
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                for(type in CatType.entries) {
                    val orange=type==CatType.ORANGE
                    val key=if(orange)"orange"else"black"
                    val accent=if(orange)NovaGold else NovaCyan
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(NovaPanel)
                        .border(if(cat==type)2.dp else 1.dp,if(cat==type)accent else Color(0xFF284656),RoundedCornerShape(12.dp))
                        .clickable {cat=type;prefs.shooterCat=type.name;confirm=false;sound.play(Sfx.SELECT)}.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Canvas(Modifier.weight(1f).fillMaxWidth()) {
                            drawOval(accent.copy(alpha=.08f+.05f*breath),Offset(0f,size.height*.35f),Size(size.width,size.height*.15f))
                            if(!ready)return@Canvas
                            novaHangarShip(assets,key,Offset(size.width*.46f,size.height*.23f),minOf(size.width*1.02f,size.height*1.35f),breath)
                            val pilot=assets.image("pilot_$key")
                            val pilotHeight=size.height*.58f
                            novaImage(pilot,Offset(size.width*.5f,size.height*.70f),pilotHeight*pilot.width/pilot.height)
                            drawLine(accent.copy(alpha=.5f),Offset(8f,size.height-2f),Offset(size.width-8f,size.height-2f),2f)
                        }
                        FitText(novaName(type),color=accent,fontSize=13.sp,fontWeight=FontWeight.Bold)
                        Text("駕駛員・${type.shortName}",color=Color.White.copy(alpha=.65f),fontSize=11.sp)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(NovaPanel,RoundedCornerShape(10.dp)).padding(if(compact)10.dp else 14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(novaName(cat),color=if(cat==CatType.ORANGE)NovaGold else NovaCyan,fontWeight=FontWeight.Bold,fontSize=16.sp)
                Text(if(cat==CatType.ORANGE)"鈦纖厚甲・喵盾增幅：甩動獲得護甲\n重力肉球軌砲：重型貫穿能量彈\n猛虎核心：全域消彈、火力增幅"
                    else "匿蹤輕甲・幽爪相位：甩動無敵位移\n高周波喵爪場：持續震盪壓制\n黑豹核心：全域消彈、火力增幅",color=Color(0xFFD0E2EE),fontSize=if(compact)12.sp else 14.sp,lineHeight=if(compact)19.sp else 22.sp)
                FitText("護甲 ${cat.maxArmor.toInt()}  /  推進 ${cat.baseSpeed.toInt()}  /  判定 ${if(cat==CatType.ORANGE)12 else 9}",color=NovaCyan,fontSize=11.sp)
            }
            Text("世界遠征 / 台灣基地出發",color=NovaCyan,fontSize=12.sp)
            if(tour!=null && !tour.finished) NovaButton("繼續遠征｜第 ${tour.cleared+1} 站・${tour.cat.shortName}",onClick=onContinue)
            NovaButton(if(confirm)"確定重啟遠征？目前進度將被取代"else"${cat.shortName}，啟動全球遠征") {
                if(tour!=null && !tour.finished && !confirm)confirm=true else {confirm=false;onNewTour(cat)}
            }
        }
    }
    if(settings) NovaSettings(prefs) { settings=false }
}

@Composable
private fun NovaSettings(prefs: AppPrefs, onClose: () -> Unit) {
    var left by remember { mutableStateOf(prefs.shooterUltimateOnLeft) }
    var numbers by remember { mutableStateOf(prefs.shooterDamageNumbers) }
    var high by remember { mutableStateOf(prefs.shooterFxHigh) }
    AlertDialog(onDismissRequest=onClose, containerColor=NovaPanel,
        title={Text("艦載控制",color=NovaCyan)},text={
            Column {
                for ((label,value,change) in listOf(
                    Triple("超載按鈕置左",left,{ v:Boolean -> left=v;prefs.shooterUltimateOnLeft=v }),
                    Triple("傷害數據",numbers,{ v:Boolean -> numbers=v;prefs.shooterDamageNumbers=v }),
                    Triple("高品質特效",high,{ v:Boolean -> high=v;prefs.shooterFxHigh=v }))) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(label,Modifier.weight(1f),color=Color.White);Switch(value,change)
                    }
                }
            }
        },confirmButton={TextButton(onClick=onClose){Text("完成",color=NovaCyan)}})
}
