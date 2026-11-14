/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.streak

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.nanzbeatles.nanas.constants.EnableLevelGifIconsKey
import com.nanzbeatles.nanas.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class LevelGifItem(
    val filename: String,
    val url: String,
    val isLoop: Boolean,
    val level: Int = 0,
    val fromLevel: Int = 0,
    val toLevel: Int = 0,
)

sealed interface GifDownloadStatus {
    data object Idle : GifDownloadStatus
    data class Downloading(
        val currentFile: String,
        val completed: Int,
        val total: Int,
        val progress: Float,
    ) : GifDownloadStatus
    data object Completed : GifDownloadStatus
    data class Error(val message: String) : GifDownloadStatus
}

object LevelGifManager {
    private const val BASE_URL =
        "https://raw.githubusercontent.com/nanasmuda121/animasi_GIF_transparan/main/gif_transparan"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadStatus = MutableStateFlow<GifDownloadStatus>(GifDownloadStatus.Idle)
    val downloadStatus: StateFlow<GifDownloadStatus> = _downloadStatus.asStateFlow()

    private val _isGifEnabled = MutableStateFlow(false)
    val isGifEnabled: StateFlow<Boolean> = _isGifEnabled.asStateFlow()

    val gifItems: List<LevelGifItem> = buildList {
        // 10 loop GIFs for level icons (level 1 to 10)
        for (i in 1..10) {
            add(
                LevelGifItem(
                    filename = "loop_$i.gif",
                    url = "$BASE_URL/loop_$i.gif",
                    isLoop = true,
                    level = i,
                )
            )
        }
        // 9 up GIFs for level transition animations (1->2, 2->3, ..., 9->10)
        for (i in 1..9) {
            val j = i + 1
            add(
                LevelGifItem(
                    filename = "up_$i-$j.gif",
                    url = "$BASE_URL/up_$i-$j.gif",
                    isLoop = false,
                    fromLevel = i,
                    toLevel = j,
                )
            )
        }
    }

    fun init(context: Context) {
        scope.launch {
            val pref = context.applicationContext.dataStore.data
                .map { it[EnableLevelGifIconsKey] ?: false }
                .first()
            val allPresent = areAllGifsDownloaded(context.applicationContext)
            _isGifEnabled.value = pref && allPresent
        }
    }

    fun getGifDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, "level_gifs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDownloadedCount(context: Context): Int {
        val dir = getGifDir(context)
        return gifItems.count { File(dir, it.filename).let { f -> f.exists() && f.length() > 0 } }
    }

    fun areAllGifsDownloaded(context: Context): Boolean {
        return getDownloadedCount(context) == gifItems.size
    }

    fun getLoopGifFile(context: Context, level: Int): File? {
        if (!_isGifEnabled.value) return null
        val safeLevel = level.coerceIn(1, 10)
        val file = File(getGifDir(context), "loop_$safeLevel.gif")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun getUpGifFile(context: Context, fromLevel: Int, toLevel: Int): File? {
        val dir = getGifDir(context)
        val direct = File(dir, "up_$fromLevel-$toLevel.gif")
        if (direct.exists() && direct.length() > 0) return direct

        val safeTo = toLevel.coerceIn(2, 10)
        val stepFallback = File(dir, "up_${safeTo - 1}-$safeTo.gif")
        if (stepFallback.exists() && stepFallback.length() > 0) return stepFallback

        val anyUp = File(dir, "up_1-2.gif")
        if (anyUp.exists() && anyUp.length() > 0) return anyUp
        return null
    }

    fun getTotalSizeBytes(context: Context): Long {
        val dir = getGifDir(context)
        return gifItems.sumOf {
            val f = File(dir, it.filename)
            if (f.exists()) f.length() else 0L
        }
    }

    fun setGifIconsEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { it[EnableLevelGifIconsKey] = enabled }
            _isGifEnabled.value = enabled && areAllGifsDownloaded(appContext)
        }
    }

    fun deleteGifs(context: Context): Boolean {
        val dir = getGifDir(context)
        var allDeleted = true
        gifItems.forEach {
            val f = File(dir, it.filename)
            if (f.exists() && !f.delete()) {
                allDeleted = false
            }
        }
        setGifIconsEnabled(context, false)
        _downloadStatus.value = GifDownloadStatus.Idle
        return allDeleted
    }

    fun startDownload(context: Context) {
        val appContext = context.applicationContext
        if (_downloadStatus.value is GifDownloadStatus.Downloading) return

        scope.launch {
            val dir = getGifDir(appContext)
            var completed = 0
            val total = gifItems.size

            _downloadStatus.value = GifDownloadStatus.Downloading(
                currentFile = gifItems.first().filename,
                completed = 0,
                total = total,
                progress = 0f,
            )

            try {
                for (item in gifItems) {
                    val targetFile = File(dir, item.filename)
                    // Skip if already completely downloaded
                    if (targetFile.exists() && targetFile.length() > 1000) {
                        completed++
                        _downloadStatus.value = GifDownloadStatus.Downloading(
                            currentFile = item.filename,
                            completed = completed,
                            total = total,
                            progress = completed.toFloat() / total.toFloat(),
                        )
                        continue
                    }

                    _downloadStatus.value = GifDownloadStatus.Downloading(
                        currentFile = item.filename,
                        completed = completed,
                        total = total,
                        progress = completed.toFloat() / total.toFloat(),
                    )

                    val tempFile = File(dir, "${item.filename}.tmp")
                    downloadSingleFile(item.url, tempFile)

                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (targetFile.exists()) targetFile.delete()
                        tempFile.renameTo(targetFile)
                    }

                    completed++
                    _downloadStatus.value = GifDownloadStatus.Downloading(
                        currentFile = item.filename,
                        completed = completed,
                        total = total,
                        progress = completed.toFloat() / total.toFloat(),
                    )
                }

                // Automatically enable GIF icons once all 19 are downloaded!
                setGifIconsEnabled(appContext, true)
                _downloadStatus.value = GifDownloadStatus.Completed
            } catch (e: Exception) {
                Timber.e(e, "Error downloading level GIFs")
                _downloadStatus.value = GifDownloadStatus.Error(e.localizedMessage ?: "Gagal mengunduh GIF")
            }
        }
    }

    private suspend fun downloadSingleFile(urlStr: String, destination: File) = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Beatles-Android")
            }
            conn.connect()
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                throw IllegalStateException("HTTP ${conn.responseCode} saat mengunduh $urlStr")
            }
        } finally {
            conn?.disconnect()
        }
    }
}
