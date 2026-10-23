package com.purpal.catplayground.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val apkUrl: String, val notes: String)

/** 從 GitHub Releases 檢查新版、下載 APK 並交給系統安裝。 */
class AppUpdater(private val context: Context) {

    val currentVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

    /** 有比目前更新的版本時回傳它；沒有或網路失敗時回傳 null。 */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open(LATEST_RELEASE_URL)
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val tag = json.getString("tag_name")
                if (!Versions.isNewer(tag, currentVersion)) return@runCatching null
                val assets = json.getJSONArray("assets")
                val apk = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith(".apk") }
                    ?: return@runCatching null
                UpdateInfo(
                    version = tag.removePrefix("v"),
                    apkUrl = apk.getString("browser_download_url"),
                    notes = Versions.releaseNotes(json.optString("body")),
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val file = File(dir, "773CatPlayground-${info.version}.apk")
        val connection = open(info.apkUrl)
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        file
    }

    /** 開啟系統安裝畫面；第一次會請使用者允許這個 App 安裝應用程式。 */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            instanceFollowRedirects = true
        }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/purpal/773CatPlayground/releases/latest"
    }
}
