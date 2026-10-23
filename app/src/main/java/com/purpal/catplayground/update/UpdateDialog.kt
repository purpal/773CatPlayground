package com.purpal.catplayground.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purpal.catplayground.ui.PlayColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

private sealed interface UpdateStep {
    data object Ask : UpdateStep
    data class Downloading(val progress: Float) : UpdateStep
    data class Ready(val apk: File) : UpdateStep
    data object Failed : UpdateStep
}

@Composable
fun UpdateDialog(info: UpdateInfo, updater: AppUpdater, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<UpdateStep>(UpdateStep.Ask) }

    fun startDownload() {
        step = UpdateStep.Downloading(0f)
        scope.launch {
            step = try {
                val apk = updater.download(info) { step = UpdateStep.Downloading(it) }
                updater.install(apk)
                UpdateStep.Ready(apk)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                UpdateStep.Failed
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (step !is UpdateStep.Downloading) onDismiss() },
        containerColor = PlayColors.Dusk,
        title = { Text("🎁 有新版本 v${info.version}！", color = PlayColors.Sun, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                when (val current = step) {
                    UpdateStep.Ask -> {
                        Text("目前版本 v${updater.currentVersion}", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        if (info.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                info.notes,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                    is UpdateStep.Downloading -> {
                        Text("下載中… ${(current.progress * 100).toInt()}%", color = Color.White)
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { current.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = PlayColors.Mint,
                        )
                    }
                    is UpdateStep.Ready -> Text(
                        "下載完成！請在系統畫面按「安裝」。\n如果出現「允許安裝應用程式」的設定，請先開啟再回來按「安裝」。",
                        color = Color.White,
                    )
                    UpdateStep.Failed -> Text("下載失敗，請確認網路連線後再試一次。", color = Color.White)
                }
            }
        },
        confirmButton = {
            when (val current = step) {
                UpdateStep.Ask, UpdateStep.Failed -> TextButton(onClick = ::startDownload) {
                    Text(if (current == UpdateStep.Failed) "重試" else "立即更新", color = PlayColors.Sun, fontWeight = FontWeight.Bold)
                }
                is UpdateStep.Ready -> TextButton(onClick = { updater.install(current.apk) }) {
                    Text("安裝", color = PlayColors.Sun, fontWeight = FontWeight.Bold)
                }
                is UpdateStep.Downloading -> Unit
            }
        },
        dismissButton = {
            if (step !is UpdateStep.Downloading) {
                TextButton(onClick = onDismiss) { Text("稍後", color = Color.White) }
            }
        },
    )
}
