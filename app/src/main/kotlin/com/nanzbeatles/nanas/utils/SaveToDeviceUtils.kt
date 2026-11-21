package com.nanzbeatles.nanas.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import com.nanzbeatles.innertube.FallbackAudioResolver
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalDownloadUtil
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.playback.DownloadUtil
import com.nanzbeatles.nanas.ui.component.Material3MenuItemData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Result states for the "Simpan ke perangkat" (Save to Device) feature.
 */
sealed class SaveToDeviceResult {
    data class Success(val fileName: String, val relativePath: String = "Music/Beatles") : SaveToDeviceResult()
    data class AlreadyExists(val fileName: String) : SaveToDeviceResult()
    data object MissingSourceFile : SaveToDeviceResult()
    data object StorageFull : SaveToDeviceResult()
    data object PermissionDenied : SaveToDeviceResult()
    data class Error(val reason: String) : SaveToDeviceResult()
}

object SaveToDeviceManager {

    private const val TARGET_RELATIVE_PATH = "Music/Beatles/"
    private const val TARGET_SUBFOLDER = "Beatles"
    private val ILLEGAL_FILENAME_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val MULTIPLE_SPACES = Regex("""\s+""")

    /**
     * Checks whether the song has a completed offline download available to be saved.
     */
    fun isOfflineFileAvailable(
        songId: String,
        download: Download?,
        downloadUtil: DownloadUtil?,
        isDownloadedInDb: Boolean = false,
    ): Boolean {
        if (songId.isBlank()) return false
        if (download?.state == Download.STATE_COMPLETED || isDownloadedInDb) return true

        val dlCache = downloadUtil?.downloadCache
        if (dlCache != null) {
            val spans = runCatching { dlCache.getCachedSpans(songId) }.getOrNull()
            if (!spans.isNullOrEmpty() && spans.any { it.isCached && (it.file?.length() ?: 0L) > 0L }) {
                return true
            }
        }
        val fallback = runCatching { FallbackAudioResolver.getCachedStreamFile(songId) }.getOrNull()
        return fallback != null && fallback.exists() && fallback.length() > 1024L
    }

    /**
     * Cleans illegal filesystem characters from the song title & artist and trims length safely.
     */
    fun buildCleanBaseFileName(title: String, artist: String?): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist?.trim().orEmpty()
        val rawBase = if (cleanArtist.isNotEmpty() && !cleanTitle.startsWith(cleanArtist, ignoreCase = true)) {
            "$cleanArtist - $cleanTitle"
        } else {
            cleanTitle
        }
        val cleaned = rawBase
            .replace(ILLEGAL_FILENAME_CHARS, "_")
            .replace(MULTIPLE_SPACES, " ")
            .trim()
            .trim('.')
            .trim()

        val fallback = if (cleaned.isEmpty()) "Beatles_Audio" else cleaned
        return if (fallback.length > 120) fallback.take(120).trim().trim('.') else fallback
    }

    /**
     * Determines the original extension and MIME type using Room's FormatEntity and file magic bytes.
     */
    private fun detectExtensionAndMime(
        sourceFile: File,
        dbMimeType: String?,
        dbCodecs: String?,
    ): Pair<String, String> {
        val header = ByteArray(16)
        val readCount = runCatching {
            FileInputStream(sourceFile).use { it.read(header) }
        }.getOrDefault(0)

        if (readCount >= 12) {
            // 1. ISO Base Media / MP4 / M4A ("ftyp" at byte 4..7)
            if (header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
            ) {
                return "m4a" to "audio/mp4"
            }
            // 2. WebM / Matroska EBML header (0x1A 0x45 0xDF 0xA3)
            if (header[0] == 0x1A.toByte() &&
                header[1] == 0x45.toByte() &&
                header[2] == 0xDF.toByte() &&
                header[3] == 0xA3.toByte()
            ) {
                return "webm" to "audio/webm"
            }
            // 3. Ogg container ("OggS")
            if (header[0] == 'O'.code.toByte() &&
                header[1] == 'g'.code.toByte() &&
                header[2] == 'g'.code.toByte() &&
                header[3] == 'S'.code.toByte()
            ) {
                return "ogg" to "audio/ogg"
            }
            // 4. FLAC ("fLaC")
            if (header[0] == 'f'.code.toByte() &&
                header[1] == 'L'.code.toByte() &&
                header[2] == 'a'.code.toByte() &&
                header[3] == 'C'.code.toByte()
            ) {
                return "flac" to "audio/flac"
            }
            // 5. WAV ("RIFF" ... "WAVE")
            if (header[0] == 'R'.code.toByte() &&
                header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() &&
                header[3] == 'F'.code.toByte()
            ) {
                return "wav" to "audio/x-wav"
            }
            // 6. MP3 ("ID3" or frame sync 0xFF 0xFB / 0xFF 0xF3 / 0xFF 0xF2)
            if ((header[0] == 'I'.code.toByte() &&
                    header[1] == 'D'.code.toByte() &&
                    header[2] == '3'.code.toByte()) ||
                (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0)
            ) {
                return "mp3" to "audio/mpeg"
            }
        }

        // Fallback to FormatEntity mimeType from database
        val mime = dbMimeType?.lowercase()?.trim().orEmpty()
        val codecs = dbCodecs?.lowercase()?.trim().orEmpty()
        return when {
            mime.contains("mp4") || mime.contains("m4a") || codecs.contains("mp4a") -> "m4a" to "audio/mp4"
            mime.contains("mpeg") || mime.contains("mp3") -> "mp3" to "audio/mpeg"
            mime.contains("ogg") || codecs.contains("vorbis") -> "ogg" to "audio/ogg"
            mime.contains("opus") -> "opus" to "audio/ogg"
            mime.contains("flac") -> "flac" to "audio/flac"
            mime.contains("wav") -> "wav" to "audio/x-wav"
            mime.contains("webm") -> "webm" to "audio/webm"
            else -> "m4a" to "audio/mp4"
        }
    }

    /**
     * Builds prioritized candidate (extension, mimeType) pairs for MediaStore insertion.
     * On Android 10+ (Scoped Storage), AOSP MediaProvider maps `.webm` to `video/webm` in MimeUtils
     * and throws IllegalArgumentException when inserting into MediaStore.Audio.Media ("Music/Beatles/").
     * By trying the original extension first and gracefully falling back to audio-compatible
     * container extensions (.ogg / .opus / .m4a) if MediaProvider rejects `.webm`, we preserve
     * the original extension whenever supported while preventing MediaProvider crashes.
     */
    private fun buildMediaStoreCandidates(ext: String, mimeType: String): List<Pair<String, String>> {
        val primary = ext to mimeType
        return if (ext.equals("webm", ignoreCase = true)) {
            listOf(
                primary,
                "ogg" to "audio/ogg",
                "opus" to "audio/ogg",
                "m4a" to "audio/mp4",
            )
        } else {
            listOf(
                primary,
                "m4a" to "audio/mp4",
                "mp3" to "audio/mpeg",
            ).distinct()
        }
    }

    /**
     * Extracts the offline cached song from Media3 SimpleCache (downloadCache / playerCache)
     * or FallbackAudioResolver into a temporary file ready for export.
     */
    private fun extractOfflineSourceToTemp(
        context: Context,
        songId: String,
        downloadUtil: DownloadUtil,
    ): File? {
        val tempDir = File(context.cacheDir, "export_temp").apply { mkdirs() }
        val tempFile = File(tempDir, "offline_${songId.hashCode()}_${System.currentTimeMillis()}.tmp")

        // 1. Read from downloadCache (primary offline storage) or playerCache
        val caches = listOf(downloadUtil.downloadCache, downloadUtil.playerCache)
        for (cache in caches) {
            val directSpans = runCatching { cache.getCachedSpans(songId) }.getOrNull()
            val candidateKeys = buildList {
                if (!directSpans.isNullOrEmpty()) {
                    add(songId)
                }
                val extraKey = runCatching {
                    cache.keys.toList().firstOrNull { it == songId || it.contains(songId) }
                }.getOrNull()
                if (extraKey != null && extraKey !in this) {
                    add(extraKey)
                }
            }

            for (matchingKey in candidateKeys) {
                // First concatenate sorted CacheSpans directly from disk (fast & never requires URI resolution)
                if (copyFromSimpleCacheSpans(cache, matchingKey, tempFile) &&
                    tempFile.exists() &&
                    tempFile.length() > 1024L
                ) {
                    return tempFile
                }

                // Fallback: read via CacheDataSource for contiguous stream reconstruction
                if (copyFromSimpleCacheViaDataSource(cache, matchingKey, tempFile) &&
                    tempFile.exists() &&
                    tempFile.length() > 1024L
                ) {
                    return tempFile
                }
            }
        }

        // 2. Check FallbackAudioResolver local disk file
        runCatching { FallbackAudioResolver.getCachedStreamFile(songId) }.getOrNull()?.let { fallbackFile ->
            if (fallbackFile.exists() && fallbackFile.length() > 1024L) {
                runCatching {
                    fallbackFile.copyTo(tempFile, overwrite = true)
                    if (tempFile.exists() && tempFile.length() > 1024L) {
                        return tempFile
                    }
                }
            }
        }

        runCatching { tempFile.delete() }
        return null
    }

    private fun copyFromSimpleCacheViaDataSource(
        cache: SimpleCache,
        key: String,
        destFile: File,
    ): Boolean {
        val dataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(null)
            .createDataSource()
        return try {
            val dataSpec = DataSpec.Builder()
                .setUri(key.toUri())
                .setKey(key)
                .build()
            dataSource.open(dataSpec)
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (dataSource.read(buffer, 0, buffer.size).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            destFile.exists() && destFile.length() > 1024L
        } catch (t: Throwable) {
            runCatching { destFile.delete() }
            false
        } finally {
            runCatching { dataSource.close() }
        }
    }

    private fun copyFromSimpleCacheSpans(
        cache: SimpleCache,
        key: String,
        destFile: File,
    ): Boolean {
        return try {
            val spans = cache.getCachedSpans(key)
                .filter { it.isCached && it.file != null && it.file!!.exists() && it.file!!.length() > 0L }
                .sortedBy { it.position }

            if (spans.isEmpty()) return false

            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                for (span in spans) {
                    val spanFile = span.file ?: continue
                    if (!spanFile.exists()) continue
                    FileInputStream(spanFile).use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                    }
                }
                output.flush()
            }
            destFile.exists() && destFile.length() > 1024L
        } catch (t: Throwable) {
            runCatching { destFile.delete() }
            false
        }
    }

    private fun isStorageOutOfSpaceException(t: Throwable?): Boolean {
        var cur = t
        while (cur != null) {
            if (cur is ErrnoException && cur.errno == OsConstants.ENOSPC) return true
            val msg = cur.message?.lowercase().orEmpty()
            if (msg.contains("enospc") || msg.contains("no space left") || msg.contains("disk full")) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    private fun hasEnoughFreeSpace(context: Context, requiredBytes: Long): Boolean {
        return try {
            val candidates = listOfNotNull(
                runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) }.getOrNull(),
                runCatching { Environment.getExternalStorageDirectory() }.getOrNull(),
                context.getExternalFilesDir(null),
                context.filesDir,
            )
            val targetDir = candidates.firstOrNull { it.exists() } ?: return true
            val stat = StatFs(targetDir.absolutePath)
            // Require at least requiredBytes + 2 MB safety margin
            stat.availableBytes > (requiredBytes + 2L * 1024L * 1024L)
        } catch (t: Throwable) {
            true
        }
    }

    /**
     * Checks if [fileName] already exists inside `Music/Beatles/` on Android 10+ via MediaStore.
     */
    private fun fileAlreadyExistsInMediaStore(context: Context, fileName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val resolver = context.applicationContext.contentResolver
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.SIZE,
            )
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val relPathIdx = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    val relPath = if (relPathIdx >= 0) cursor.getString(relPathIdx).orEmpty() else ""
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 1L
                    val inBeatlesFolder = relPath.equals("Music/Beatles/", ignoreCase = true) ||
                        relPath.equals("Music/Beatles", ignoreCase = true) ||
                        relPath.replace('\\', '/').trim('/').equals("Music/Beatles", ignoreCase = true)
                    if (inBeatlesFolder && size > 0L) {
                        return@use true
                    }
                }
                false
            } ?: false
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Copies the offline downloaded song to `Music/Beatles/` on [Dispatchers.IO].
     *
     * - **Android 10+ (API 29+)**: Uses `MediaStore.Audio.Media` with
     *   `RELATIVE_PATH = "Music/Beatles/"` and `IS_PENDING = 1 -> 0` (no storage permission needed).
     * - **Android 9 and below (API <= 28)**: Requires `WRITE_EXTERNAL_STORAGE` and copies to
     *   `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)/Beatles/`.
     * - Triggers `MediaScannerConnection.scanFile` so the track appears immediately in gallery & other music players.
     */
    suspend fun saveDownloadedSongToDevice(
        context: Context,
        songId: String,
        title: String,
        artist: String?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
    ): SaveToDeviceResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        var tempSourceFile: File? = null
        try {
            // 1. Check permission on Android 9 and below
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val granted = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    return@withContext SaveToDeviceResult.PermissionDenied
                }
            }

            // 2. Extract source offline file from internal cache
            tempSourceFile = extractOfflineSourceToTemp(appContext, songId, downloadUtil)
                ?: return@withContext SaveToDeviceResult.MissingSourceFile

            val sourceLength = tempSourceFile.length()
            if (sourceLength <= 0L) {
                return@withContext SaveToDeviceResult.MissingSourceFile
            }

            // 3. Check available storage space before copying
            if (!hasEnoughFreeSpace(appContext, sourceLength)) {
                return@withContext SaveToDeviceResult.StorageFull
            }

            // 4. Determine original extension & MIME type
            val formatEntity = runCatching { database.format(songId).firstOrNull() }.getOrNull()
            val (detectedExt, detectedMimeType) = detectExtensionAndMime(
                sourceFile = tempSourceFile,
                dbMimeType = formatEntity?.mimeType,
                dbCodecs = formatEntity?.codecs,
            )

            // 5. Sanitize filename
            val cleanBaseName = buildCleanBaseFileName(title = title, artist = artist)
            val publicMusicDir = runCatching {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            }.getOrNull()
            val legacyTargetDir = if (publicMusicDir != null) {
                File(publicMusicDir, TARGET_SUBFOLDER)
            } else {
                File(Environment.getExternalStorageDirectory(), "Music/$TARGET_SUBFOLDER")
            }

            // 6. Copy file to Music/Beatles/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appContext.contentResolver
                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val candidates = buildMediaStoreCandidates(detectedExt, detectedMimeType)

                // Check if any candidate filename already exists in Music/Beatles/ to prevent duplicates
                for ((candExt, _) in candidates) {
                    val candidateName = "$cleanBaseName.$candExt"
                    val candidateLegacyFile = File(legacyTargetDir, candidateName)
                    if (fileAlreadyExistsInMediaStore(appContext, candidateName) ||
                        (runCatching { candidateLegacyFile.exists() && candidateLegacyFile.length() > 0L }.getOrDefault(false))
                    ) {
                        return@withContext SaveToDeviceResult.AlreadyExists(candidateName)
                    }
                }

                // Insert into MediaStore using the first candidate accepted by Android's MediaProvider
                var insertedUri: Uri? = null
                var finalFileName = "$cleanBaseName.$detectedExt"
                var finalMimeType = detectedMimeType
                var lastInsertError: Throwable? = null

                for ((candExt, candMime) in candidates) {
                    val candidateName = "$cleanBaseName.$candExt"
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, candidateName)
                        put(MediaStore.Audio.Media.TITLE, title.ifBlank { cleanBaseName })
                        if (!artist.isNullOrBlank()) {
                            put(MediaStore.Audio.Media.ARTIST, artist)
                        }
                        put(MediaStore.Audio.Media.MIME_TYPE, candMime)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, TARGET_RELATIVE_PATH)
                        put(MediaStore.Audio.Media.IS_MUSIC, 1)
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }

                    try {
                        val uri = resolver.insert(collection, values)
                        if (uri != null) {
                            insertedUri = uri
                            finalFileName = candidateName
                            finalMimeType = candMime
                            break
                        }
                    } catch (t: Throwable) {
                        lastInsertError = t
                        if (isStorageOutOfSpaceException(t)) {
                            return@withContext SaveToDeviceResult.StorageFull
                        }
                    }
                }

                val itemUri = insertedUri ?: return@withContext SaveToDeviceResult.Error(
                    lastInsertError?.localizedMessage ?: "Gagal membuat entri MediaStore untuk Music/Beatles."
                )

                try {
                    val outStream = resolver.openOutputStream(itemUri)
                        ?: throw IOException("Gagal membuka OutputStream MediaStore.")
                    outStream.use { output ->
                        FileInputStream(tempSourceFile).use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                            output.flush()
                        }
                    }

                    val publishValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    runCatching {
                        resolver.update(itemUri, publishValues, null, null)
                    }

                    val legacyTargetFile = File(legacyTargetDir, finalFileName)
                    runCatching {
                        MediaScannerConnection.scanFile(
                            appContext,
                            arrayOf(legacyTargetFile.absolutePath),
                            arrayOf(finalMimeType),
                            null,
                        )
                    }

                    return@withContext SaveToDeviceResult.Success(fileName = finalFileName)
                } catch (t: Throwable) {
                    // Clean up incomplete pending entry on failure
                    runCatching { resolver.delete(itemUri, null, null) }
                    if (isStorageOutOfSpaceException(t)) {
                        return@withContext SaveToDeviceResult.StorageFull
                    }
                    return@withContext SaveToDeviceResult.Error(
                        t.localizedMessage ?: "Terjadi kesalahan saat menulis file ke perangkat."
                    )
                }
            } else {
                // Android 9 (Pie) and below: copy directly to Environment.DIRECTORY_MUSIC/Beatles/
                val finalFileName = "$cleanBaseName.$detectedExt"
                val legacyTargetFile = File(legacyTargetDir, finalFileName)

                if (legacyTargetFile.exists() && legacyTargetFile.length() > 0L) {
                    return@withContext SaveToDeviceResult.AlreadyExists(finalFileName)
                }

                if (!legacyTargetDir.exists() && !legacyTargetDir.mkdirs()) {
                    return@withContext SaveToDeviceResult.Error("Gagal membuat folder Music/Beatles di penyimpanan perangkat.")
                }

                try {
                    FileOutputStream(legacyTargetFile).use { output ->
                        FileInputStream(tempSourceFile).use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                            output.flush()
                        }
                    }

                    runCatching {
                        MediaScannerConnection.scanFile(
                            appContext,
                            arrayOf(legacyTargetFile.absolutePath),
                            arrayOf(detectedMimeType),
                            null,
                        )
                    }

                    return@withContext SaveToDeviceResult.Success(fileName = finalFileName)
                } catch (t: Throwable) {
                    runCatching { legacyTargetFile.delete() }
                    if (isStorageOutOfSpaceException(t)) {
                        return@withContext SaveToDeviceResult.StorageFull
                    }
                    return@withContext SaveToDeviceResult.Error(
                        t.localizedMessage ?: "Terjadi kesalahan saat menyalin file ke Music/Beatles."
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (isStorageOutOfSpaceException(t)) {
                return@withContext SaveToDeviceResult.StorageFull
            }
            return@withContext SaveToDeviceResult.Error(
                t.localizedMessage ?: "Terjadi kesalahan tak terduga saat menyimpan file musik."
            )
        } finally {
            runCatching { tempSourceFile?.delete() }
        }
    }
}

/**
 * Asynchronously checks whether the song has an offline file ready to be saved to device,
 * without blocking UI composition on disk I/O.
 */
@Composable
fun rememberIsOfflineFileAvailable(
    songId: String,
    download: Download?,
    isDownloadedInDb: Boolean = false,
): Boolean {
    val downloadUtil = LocalDownloadUtil.current
    val immediateDownloaded = download?.state == Download.STATE_COMPLETED || isDownloadedInDb
    val asyncAvailable by produceState(
        initialValue = immediateDownloaded,
        key1 = songId,
        key2 = download?.state,
        key3 = isDownloadedInDb,
    ) {
        value = if (immediateDownloaded) {
            true
        } else {
            withContext(Dispatchers.IO) {
                SaveToDeviceManager.isOfflineFileAvailable(
                    songId = songId,
                    download = download,
                    downloadUtil = downloadUtil,
                    isDownloadedInDb = isDownloadedInDb,
                )
            }
        }
    }
    return immediateDownloaded || asyncAvailable
}

/**
 * State holder for triggering "Simpan ke perangkat" from any three-dot Song Menu,
 * managing runtime permission on Android <= 9, showing the short loading dialog,
 * and displaying the final result AlertDialog.
 */
class SaveToDeviceController(
    private val onTriggerSaveAction: () -> Unit,
) {
    fun onTriggerSave() {
        onTriggerSaveAction()
    }
}

@Composable
fun rememberSaveToDeviceController(
    songId: String,
    title: String,
    artist: String?,
    onFinishedDismissMenu: (() -> Unit)? = null,
): SaveToDeviceController {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val scope = rememberCoroutineScope()

    val currentSongId by rememberUpdatedState(songId)
    val currentTitle by rememberUpdatedState(title)
    val currentArtist by rememberUpdatedState(artist)
    val currentOnDismiss by rememberUpdatedState(onFinishedDismissMenu)

    var isSaving by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<SaveToDeviceResult?>(null) }

    val executeSave: () -> Unit = {
        if (!isSaving) {
            isSaving = true
            scope.launch {
                val res = try {
                    SaveToDeviceManager.saveDownloadedSongToDevice(
                        context = context,
                        songId = currentSongId,
                        title = currentTitle,
                        artist = currentArtist,
                        database = database,
                        downloadUtil = downloadUtil,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    SaveToDeviceResult.Error(
                        t.localizedMessage ?: "Terjadi kesalahan saat menyimpan lagu ke perangkat."
                    )
                }
                isSaving = false
                saveResult = res
            }
        }
    }

    val currentExecuteSave by rememberUpdatedState(executeSave)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            currentExecuteSave()
        } else {
            saveResult = SaveToDeviceResult.PermissionDenied
        }
    }

    val triggerSave: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                currentExecuteSave()
            } else {
                runCatching {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }.onFailure {
                    saveResult = SaveToDeviceResult.PermissionDenied
                }
            }
        } else {
            currentExecuteSave()
        }
    }

    val currentTriggerSave by rememberUpdatedState(triggerSave)

    // 1. Short Loading Dialog while copying on Dispatchers.IO
    if (isSaving) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Menyimpan ke Music/Beatles...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    // 2. Result AlertDialog (Success / Duplicate / Error)
    saveResult?.let { result ->
        val isSuccess = result is SaveToDeviceResult.Success
        val dialogTitle = when (result) {
            is SaveToDeviceResult.Success -> "Berhasil Disimpan"
            is SaveToDeviceResult.AlreadyExists -> "File Sudah Ada"
            is SaveToDeviceResult.StorageFull -> "Penyimpanan Penuh"
            is SaveToDeviceResult.PermissionDenied -> "Izin Penyimpanan Ditolak"
            is SaveToDeviceResult.MissingSourceFile -> "File Offline Tidak Ditemukan"
            is SaveToDeviceResult.Error -> "Gagal Menyimpan"
        }
        val dialogMessage = when (result) {
            is SaveToDeviceResult.Success ->
                "File musik tersimpan di Music/Beatles\n\nNama file: ${result.fileName}"
            is SaveToDeviceResult.AlreadyExists ->
                "File musik sudah ada di Music/Beatles (${result.fileName}). Tidak dibuat salinan duplikat."
            is SaveToDeviceResult.StorageFull ->
                "Gagal menyimpan lagu karena ruang penyimpanan perangkat penuh. Kosongkan sebagian ruang penyimpanan lalu coba lagi."
            is SaveToDeviceResult.PermissionDenied ->
                "Izin akses penyimpanan ditolak. Berikan izin penyimpanan agar aplikasi dapat menyimpan file lagu ke folder Music/Beatles."
            is SaveToDeviceResult.MissingSourceFile ->
                "File unduhan offline tidak ditemukan atau rusak di penyimpanan internal aplikasi. Silakan unduh ulang lagu ini terlebih dahulu."
            is SaveToDeviceResult.Error ->
                "Gagal menyimpan file musik ke Music/Beatles: ${result.reason}"
        }

        AlertDialog(
            onDismissRequest = {
                saveResult = null
                if (isSuccess) {
                    currentOnDismiss?.invoke()
                }
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = dialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveResult = null
                        if (isSuccess) {
                            currentOnDismiss?.invoke()
                        }
                    },
                    modifier = Modifier.testTag("save_to_device_alert_ok"),
                ) {
                    Text(
                        text = "Oke",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        )
    }

    return remember {
        SaveToDeviceController(onTriggerSaveAction = { currentTriggerSave() })
    }
}

/**
 * Reusable Material 3 menu item for "Simpan ke perangkat" inside three-dot song menus.
 */
@Composable
fun buildSaveToDeviceMenuItem(
    controller: SaveToDeviceController,
): Material3MenuItemData {
    return Material3MenuItemData(
        title = {
            Text(text = "Simpan ke perangkat")
        },
        description = {
            Text(text = "Salin file musik offline ke folder publik Music/Beatles")
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = "Simpan ke perangkat",
                modifier = Modifier.size(24.dp),
            )
        },
        onClick = {
            controller.onTriggerSave()
        },
    )
}
