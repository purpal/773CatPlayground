package com.purpal.catplayground.shooter.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.audio.LocalSound
import com.purpal.catplayground.audio.Sfx
import com.purpal.catplayground.data.AppPrefs
import com.purpal.catplayground.shooter.model.CatType
import com.purpal.catplayground.shooter.model.StageEntry
import com.purpal.catplayground.shooter.model.Stages
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors

@Composable
fun ShooterMenuScreen(
    prefs: AppPrefs,
    cleared: Int,
    carryStage: Int = 0,
    onBack: () -> Unit,
    onPlay: (stage: Int, cat: CatType) -> Unit,
) {
    val sound = LocalSound.current
    var cat by remember { mutableStateOf(runCatching { CatType.valueOf(prefs.shooterCat) }.getOrDefault(CatType.ORANGE)) }
    var ultimateLeft by remember { mutableStateOf(prefs.shooterUltimateOnLeft) }
    var damageNumbers by remember { mutableStateOf(prefs.shooterDamageNumbers) }
    var fxHigh by remember { mutableStateOf(prefs.shooterFxHigh) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFD84315), Color(0xFF6A1B9A), Color(0xFF1A1033)))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillButton("⬅") { onBack() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    FitText("喵星萌王爭霸戰", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    FitText("貓咪要成為最萌的動物！打倒所有對手吧！", color = PlayColors.Sun, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("選擇機體")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (type in CatType.entries) {
                    CatCard(
                        cat = type,
                        selected = cat == type,
                        modifier = Modifier.weight(1f),
                    ) {
                        sound.play(Sfx.SELECT)
                        cat = type
                        prefs.shooterCat = type.name
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("設定")
            Row(verticalAlignment = Alignment.CenterVertically) {
                FitText("大絕按鈕位置", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                for ((label, left) in listOf("左下" to true, "右下" to false)) {
                    PillButton(
                        label,
                        background = if (ultimateLeft == left) PlayColors.Sun else PlayColors.Card,
                        contentColor = if (ultimateLeft == left) PlayColors.Night else Color.White,
                    ) {
                        ultimateLeft = left
                        prefs.shooterUltimateOnLeft = left
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            OptionRow("傷害數字", listOf("顯示" to true, "隱藏" to false), damageNumbers) {
                damageNumbers = it
                prefs.shooterDamageNumbers = it
            }
            Spacer(Modifier.height(8.dp))
            OptionRow("特效品質", listOf("高" to true, "低" to false), fxHigh) {
                fxHigh = it
                prefs.shooterFxHigh = it
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("關卡")
            for (entry in Stages.all) {
                StageRow(entry, cleared, prefs.shooterBest(entry.number), carried = entry.number == carryStage) {
                    onPlay(entry.number, cat)
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OptionRow(title: String, options: List<Pair<String, Boolean>>, current: Boolean, onSelect: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FitText(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        for ((label, value) in options) {
            PillButton(
                label,
                background = if (current == value) PlayColors.Sun else PlayColors.Card,
                contentColor = if (current == value) PlayColors.Night else Color.White,
            ) { onSelect(value) }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    FitText(text, color = PlayColors.Sun, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun CatCard(cat: CatType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    // 選中的貓咪會輕輕彈跳
    val transition = rememberInfiniteTransition(label = "cat")
    val bounce by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "bounce")
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(if (selected) Color(0x33FFD54F) else PlayColors.Card)
            .border(3.dp, if (selected) PlayColors.Sun else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        ArtImage(
            if (cat == CatType.ORANGE) "cat_orange_front" else "cat_black_front",
            Modifier
                .size(96.dp)
                .offset(y = if (selected) (-6 * bounce).dp else 0.dp),
            contentDescription = cat.displayName,
        )
        FitText(cat.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        StatBar("護甲", cat.maxArmor / 160f, Color(0xFFFF8A80))
        StatBar("速度", cat.baseSpeed / 780f, Color(0xFF80D8FF))
        Spacer(Modifier.height(6.dp))
        FitText("專武：${cat.specialName}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        FitText("大絕：${cat.ultimateName}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
private fun StatBar(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        FitText(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(34.dp))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.2f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Composable
private fun StageRow(entry: StageEntry, cleared: Int, best: Int, carried: Boolean, onClick: () -> Unit) {
    val sound = LocalSound.current
    val ready = entry.stage != null
    val unlocked = ready && entry.number <= cleared + 1
    val done = entry.number <= cleared
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (unlocked) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .clickable {
                if (unlocked) {
                    sound.play(Sfx.SELECT)
                    onClick()
                } else {
                    sound.play(Sfx.INVALID)
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val stage = entry.stage
        if (stage != null) {
            // 已完成的關卡用 Boss 頭像（v2 麻糬萌風）
            ArtImage("${stage.bossArt}_m0_0", Modifier.size(44.dp), contentDescription = stage.bossName, alpha = if (unlocked) 1f else 0.5f)
        } else {
            Text(entry.emoji, fontSize = 30.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText(
                "STAGE ${entry.number}　${entry.animal}",
                color = if (unlocked) PlayColors.Sun else Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            FitText(entry.title, color = Color.White.copy(alpha = if (unlocked) 1f else 0.5f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (done && best > 0) FitText("最高分 $best", color = PlayColors.Mint, fontSize = 12.sp)
            if (carried && unlocked) FitText("🎒 上一關的裝備保留中（可用一次）", color = Color(0xFFFFCADA), fontSize = 12.sp)
        }
        FitText(
            when {
                !ready -> "製作中"
                done -> "⭐"
                unlocked -> "▶"
                else -> "🔒"
            },
            color = Color.White.copy(alpha = 0.8f),
            fontSize = if (ready) 22.sp else 13.sp,
        )
    }
}
