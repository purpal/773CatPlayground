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
import com.purpal.catplayground.shooter.model.Tour
import com.purpal.catplayground.shooter.model.TourState
import com.purpal.catplayground.ui.ArtImage
import com.purpal.catplayground.ui.FitText
import com.purpal.catplayground.ui.PillButton
import com.purpal.catplayground.ui.PlayColors

@Composable
fun ShooterMenuScreen(
    prefs: AppPrefs,
    tour: TourState?,
    onBack: () -> Unit,
    onNewTour: (cat: CatType) -> Unit,
    onContinueTour: () -> Unit,
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
                    FitText("從台灣出發，環遊世界打倒所有對手，挑戰萌王！", color = PlayColors.Sun, fontSize = 13.sp)
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
            OptionRow("大絕按鈕位置", listOf("左下" to true, "右下" to false), ultimateLeft) {
                ultimateLeft = it
                prefs.shooterUltimateOnLeft = it
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
            SectionTitle("世界巡迴")
            var confirmNew by remember { mutableStateOf(false) }
            val inProgress = tour != null && !tour.finished
            if (tour != null && inProgress) {
                TourCard(tour) {
                    sound.play(Sfx.SELECT)
                    onContinueTour()
                }
                Spacer(Modifier.height(10.dp))
            }
            BigMenuButton(
                when {
                    confirmNew -> "⚠️ 放棄目前進度、重新出發？再按一次確定"
                    inProgress -> "🌏 用${cat.shortName}重新開始巡迴"
                    else -> "🌏 ${cat.shortName}，出發環遊世界！"
                },
                primary = !inProgress,
            ) {
                if (inProgress && !confirmNew) {
                    sound.play(Sfx.SELECT)
                    confirmNew = true
                } else {
                    sound.play(Sfx.SELECT)
                    confirmNew = false
                    onNewTour(cat)
                }
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
                modifier = Modifier.width(76.dp),
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
        StatBar("護甲", cat.maxArmor / 100f, Color(0xFFFF8A80))
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

/** 進行中的巡迴：哪隻貓、打到第幾站、下一站在哪裡。 */
@Composable
private fun TourCard(tour: TourState, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(2.dp, PlayColors.Sun, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        ArtImage(if (tour.cat == CatType.ORANGE) "cat_orange_front" else "cat_black_front", Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText("繼續巡迴：${boutTitle(tour.cleared)}", color = PlayColors.Sun, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val next = if (tour.nextRevealed) tour.stageAt(tour.cleared) else null
            FitText(
                if (next != null) "${next.country.name}・${next.bossName}" else "已打倒 ${tour.cleared} / ${Tour.SIZE} 位對手",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            FitText("總分 ${tour.totalScore}", color = PlayColors.Mint, fontSize = 12.sp)
        }
        FitText("▶", color = Color.White, fontSize = 22.sp)
    }
}

@Composable
private fun BigMenuButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) PlayColors.Sun else PlayColors.Card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FitText(
            text,
            color = if (primary) PlayColors.Night else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
