package com.nanzbeatles.nanas.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import com.nanzbeatles.innertube.FallbackAudioResolver
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalDownloadUtil
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AudioQuality
import com.nanzbeatles.nanas.constants.AudioStyleMode
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.eq.audio.CustomEqualizerAudioProcessor
import com.nanzbeatles.nanas.eq.data.EQProfileRepository
import com.nanzbeatles.nanas.eq.data.ParametricEQ
import com.nanzbeatles.nanas.lyricvideo.Mp4ContainerWriter
import com.nanzbeatles.nanas.playback.DownloadUtil
import com.nanzbeatles.nanas.playback.audio.ReverbAudioProcessor
import com.nanzbeatles.nanas.ui.component.Material3MenuItemData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * Result states for the "Simpan ke perangkat" (Save to Device) feature.
 */
sealed class SaveToDeviceResult {
    data class Success(
        val fileName: String,
        val relativePath: String = "Music/Beatles",
        val styleLabel: String? = null,
        val eqApplied: Boolean = false,
    ) : SaveToDeviceResult()

    data class AlreadyExists(val fileName: String) : SaveToDeviceResult()
    data object MissingSourceFile : SaveToDeviceResult()
    data object StorageFull : SaveToDeviceResult()
    data object PermissionDenied : SaveToDeviceResult()
    data class Error(val reason: String) : SaveToDeviceResult()
}

object SaveToDeviceManager {

    private const val TAG = "SaveToDeviceManager"
    private const val TARGET_RELATIVE_PATH = "Music/Beatles/"
    private const val TARGET_SUBFOLDER = "Beatles"
    private val ILLEGAL_FILENAME_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val MULTIPLE_SPACES = Regex("""\s+""")

    /**
     * Checks whether the song has a completed offline download or cached stream available.
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
     * Cleans illegal filesystem characters and appends style/EQ tag when applied.
     */
    fun buildCleanBaseFileName(
        title: String,
        artist: String?,
        styleTag: String? = null,
    ): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist?.trim().orEmpty()
        val rawBase = if (cleanArtist.isNotEmpty() && !cleanTitle.startsWith(cleanArtist, ignoreCase = true)) {
            "$cleanArtist - $cleanTitle"
        } else {
            cleanTitle
        }
        val withStyle = if (!styleTag.isNullOrBlank()) {
            "$rawBase [$styleTag]"
        } else {
            rawBase
        }
        val cleaned = withStyle
            .replace(ILLEGAL_FILENAME_CHARS, "_")
            .replace(MULTIPLE_SPACES, " ")
            .trim()
            .trim('.')
            .trim()

        val fallback = if (cleaned.isEmpty()) "Beatles_Audio" else cleaned
        return if (fallback.length > 140) fallback.take(140).trim().trim('.') else fallback
    }

    /**
     * Determines original extension and MIME type using Room's FormatEntity and file magic bytes.
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
     * Extracts source audio from cache (downloadCache, playerCache, FallbackAudioResolver)
     * or fetches online stream as resilient fallback.
     */
    private suspend fun extractSourceToTemp(
        context: Context,
        songId: String,
        downloadUtil: DownloadUtil,
        onProgress: ((String, Float) -> Unit)? = null,
    ): File? = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "export_temp").apply { mkdirs() }
        val tempFile = File(tempDir, "source_${songId.hashCode()}_${System.currentTimeMillis()}.tmp")

        // 1. Read from downloadCache or playerCache
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
                if (copyFromSimpleCacheSpans(cache, matchingKey, tempFile) &&
                    tempFile.exists() &&
                    tempFile.length() > 1024L
                ) {
                    return@withContext tempFile
                }
                if (copyFromSimpleCacheViaDataSource(cache, matchingKey, tempFile) &&
                    tempFile.exists() &&
                    tempFile.length() > 1024L
                ) {
                    return@withContext tempFile
                }
            }
        }

        // 2. Check FallbackAudioResolver local disk file
        runCatching { FallbackAudioResolver.getCachedStreamFile(songId) }.getOrNull()?.let { fallbackFile ->
            if (fallbackFile.exists() && fallbackFile.length() > 1024L) {
                runCatching {
                    fallbackFile.copyTo(tempFile, overwrite = true)
                    if (tempFile.exists() && tempFile.length() > 1024L) {
                        return@withContext tempFile
                    }
                }
            }
        }

        // 3. Resilient fallback: fetch audio stream online if not yet cached
        runCatching {
            val connectivityManager = context.getSystemService<ConnectivityManager>()
            if (connectivityManager != null) {
                onProgress?.invoke("Mengunduh stream audio...", 0.05f)
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    audioQuality = AudioQuality.HIGH,
                    connectivityManager = connectivityManager,
                    preferMp4Audio = true,
                ).getOrNull()

                if (playbackData != null && playbackData.streamUrl.isNotBlank()) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(25, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .build()

                    val reqBuilder = Request.Builder().url(playbackData.streamUrl)
                    playbackData.streamHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                    client.newCall(reqBuilder.build()).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body
                            if (body != null) {
                                val totalBytes = body.contentLength()
                                var downloadedBytes = 0L
                                FileOutputStream(tempFile).use { output ->
                                    val buffer = ByteArray(64 * 1024)
                                    val inStream = body.byteStream()
                                    var read: Int
                                    while (inStream.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                        downloadedBytes += read
                                        if (totalBytes > 0) {
                                            val fraction = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                                            onProgress?.invoke("Mengunduh audio (${(fraction * 100).toInt()}%)...", fraction * 0.25f)
                                        }
                                    }
                                }
                                if (tempFile.exists() && tempFile.length() > 1024L) {
                                    return@withContext tempFile
                                }
                            }
                        }
                    }
                }
            }
        }

        runCatching { tempFile.delete() }
        null
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
            stat.availableBytes > (requiredBytes + 3L * 1024L * 1024L)
        } catch (t: Throwable) {
            true
        }
    }

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
     * Transcodes full-length audio file to high-bitrate AAC (.m4a),
     * applying [AudioStyleMode] (analog varispeed, reverb, 8D) and [ParametricEQ] (10-band + bass boost).
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun transcodeAudioWithDsp(
        sourceFile: File,
        targetFile: File,
        audioStyleMode: AudioStyleMode = AudioStyleMode.NORMAL,
        parametricEQ: ParametricEQ? = null,
        onProgress: ((String, Float) -> Unit)? = null,
    ): Boolean {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        val tempWritingFile = File(targetFile.parentFile, "${targetFile.name}.mux_${System.currentTimeMillis()}.tmp")

        try {
            extractor.setDataSource(sourceFile.absolutePath)
            var audioTrack = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    inputFormat = format
                    break
                }
            }
            if (audioTrack == -1 || inputFormat == null) return false
            extractor.selectTrack(audioTrack)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return false
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            } else 44100
            var channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
            } else 2

            val pcmFormat = AudioProcessor.AudioFormat(sampleRate, channelCount, C.ENCODING_PCM_16BIT)

            val eqProcessor = if (parametricEQ != null) {
                CustomEqualizerAudioProcessor().apply {
                    applyProfile(parametricEQ)
                    configure(pcmFormat)
                    flush()
                }
            } else null

            val reverbProcessor = if (audioStyleMode != AudioStyleMode.NORMAL) {
                ReverbAudioProcessor().apply {
                    setAudioStyleMode(audioStyleMode)
                    configure(pcmFormat)
                    flush()
                }
            } else null

            val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 256000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            targetFile.parentFile?.mkdirs()
            tempWritingFile.parentFile?.mkdirs()

            muxer = MediaMuxer(tempWritingFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1

            val totalDurationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()
            var extractorDone = false
            var decoderDone = false
            var encoderEosQueued = false
            var encoderDone = false
            var lastAudioPtsUs = -1L
            var totalEncodedPcmFrames = 0L
            val frameSize = (2 * channelCount).coerceAtLeast(2)
            var pendingPcmBuffer: ByteBuffer? = null

            var csd0Bytes: ByteArray? = null
            val fallbackPackets = ArrayList<Mp4ContainerWriter.AudioPacket>()

            val timeoutUs = 5000L
            val startNs = System.nanoTime()
            var lastActivityNs = startNs

            val styleName = audioStyleMode.label
            val effectDesc = buildString {
                if (audioStyleMode != AudioStyleMode.NORMAL) append(styleName)
                if (parametricEQ != null) {
                    if (isNotEmpty()) append(" + ")
                    append("EQ")
                }
            }.ifBlank { "efek suara" }

            while (!encoderDone) {
                val nowNs = System.nanoTime()
                if (nowNs - lastActivityNs > 35_000_000_000L) {
                    Timber.tag(TAG).e("transcodeAudioWithDsp: idle > 35s, aborting")
                    return false
                }
                if (nowNs - startNs > 360_000_000_000L) {
                    Timber.tag(TAG).e("transcodeAudioWithDsp: exceeded 360s, aborting")
                    return false
                }

                // 1. Feed extractor into decoder
                if (!extractorDone) {
                    val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            inBuf.clear()
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            val sampleTime = extractor.sampleTime
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorDone = true
                            } else {
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, maxOf(0L, sampleTime), 0)
                                extractor.advance()
                                if (totalDurationUs > 0L) {
                                    val prog = (sampleTime.toFloat() / totalDurationUs.toFloat()).coerceIn(0f, 1f)
                                    onProgress?.invoke("Menerapkan $effectDesc (${(prog * 100).toInt()}%)...", prog)
                                }
                            }
                            lastActivityNs = System.nanoTime()
                        }
                    }
                }

                // 2. Drain decoder -> EQ -> Reverb -> chunk into encoder
                val currentPending = pendingPcmBuffer
                if ((currentPending == null || !currentPending.hasRemaining()) && !decoderDone) {
                    val decOutIdx = decoder.dequeueOutputBuffer(decoderBufferInfo, timeoutUs)
                    if (decOutIdx >= 0) {
                        lastActivityNs = System.nanoTime()
                        val decBuf = decoder.getOutputBuffer(decOutIdx)
                        val isEos = (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        if (decBuf != null && decoderBufferInfo.size > 0) {
                            decBuf.position(decoderBufferInfo.offset)
                            decBuf.limit(decoderBufferInfo.offset + decoderBufferInfo.size)

                            var currentBuf: ByteBuffer = decBuf
                            if (eqProcessor != null && eqProcessor.isActive) {
                                eqProcessor.queueInput(currentBuf)
                                currentBuf = eqProcessor.getOutput()
                            }
                            if (reverbProcessor != null && currentBuf.hasRemaining()) {
                                reverbProcessor.queueInput(currentBuf)
                                currentBuf = reverbProcessor.getOutput()
                            }
                            if (currentBuf === decBuf) {
                                val copy = ByteBuffer.allocateDirect(decoderBufferInfo.size).order(ByteOrder.nativeOrder())
                                copy.put(decBuf)
                                copy.flip()
                                pendingPcmBuffer = copy
                            } else {
                                pendingPcmBuffer = currentBuf
                            }
                        }

                        decoder.releaseOutputBuffer(decOutIdx, false)
                        if (isEos) {
                            decoderDone = true
                        }
                    }
                }

                val activePcm = pendingPcmBuffer
                if (activePcm != null && activePcm.hasRemaining()) {
                    val encInIdx = encoder.dequeueInputBuffer(timeoutUs)
                    if (encInIdx >= 0) {
                        val encInBuf = encoder.getInputBuffer(encInIdx)
                        val ptsUs = (totalEncodedPcmFrames * 1_000_000L) / sampleRate
                        if (encInBuf != null) {
                            encInBuf.clear()
                            val maxBytes = (minOf(activePcm.remaining(), encInBuf.remaining()) / frameSize) * frameSize
                            if (maxBytes > 0) {
                                val oldLimit = activePcm.limit()
                                activePcm.limit(activePcm.position() + maxBytes)
                                encInBuf.put(activePcm)
                                activePcm.limit(oldLimit)

                                totalEncodedPcmFrames += (maxBytes / frameSize).toLong()
                                encoder.queueInputBuffer(encInIdx, 0, maxBytes, ptsUs, 0)
                                lastActivityNs = System.nanoTime()
                            } else {
                                pendingPcmBuffer = null
                                encoder.queueInputBuffer(encInIdx, 0, 0, ptsUs, 0)
                                lastActivityNs = System.nanoTime()
                            }
                        } else {
                            pendingPcmBuffer = null
                            encoder.queueInputBuffer(encInIdx, 0, 0, ptsUs, 0)
                            lastActivityNs = System.nanoTime()
                        }
                    }
                } else if (decoderDone && !encoderEosQueued) {
                    val encInIdx = encoder.dequeueInputBuffer(timeoutUs)
                    if (encInIdx >= 0) {
                        val ptsUs = (totalEncodedPcmFrames * 1_000_000L) / sampleRate
                        encoder.queueInputBuffer(encInIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        encoderEosQueued = true
                        lastActivityNs = System.nanoTime()
                    }
                }

                // 3. Drain encoder to MediaMuxer
                val encOutIdx = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
                if (encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    lastActivityNs = System.nanoTime()
                    val fmt = encoder.outputFormat
                    if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
                    }
                    if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                    }
                    csd0Bytes = Mp4ContainerWriter.extractFormatCsd0(fmt) ?: csd0Bytes
                    try {
                        muxerTrackIndex = muxer.addTrack(fmt)
                        muxer.start()
                        muxerStarted = true
                    } catch (t: Throwable) {
                        Timber.tag(TAG).w(t, "MediaMuxer start failed, will fallback to Mp4ContainerWriter")
                    }
                } else if (encOutIdx >= 0) {
                    lastActivityNs = System.nanoTime()
                    val encOutBuf = encoder.getOutputBuffer(encOutIdx)
                    if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (encOutBuf != null && encoderBufferInfo.size > 0 && csd0Bytes == null) {
                            encOutBuf.position(encoderBufferInfo.offset)
                            encOutBuf.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                            csd0Bytes = Mp4ContainerWriter.copyByteBuffer(encOutBuf)
                        }
                        encoderBufferInfo.size = 0
                    }
                    if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encoderDone = true
                    }
                    if (encOutBuf != null && encoderBufferInfo.size > 0) {
                        encOutBuf.position(encoderBufferInfo.offset)
                        encOutBuf.limit(encoderBufferInfo.offset + encoderBufferInfo.size)

                        var pts = maxOf(0L, encoderBufferInfo.presentationTimeUs)
                        if (pts <= lastAudioPtsUs) {
                            pts = lastAudioPtsUs + ((1_000_000L * 1024L) / sampleRate)
                        }
                        lastAudioPtsUs = pts
                        encoderBufferInfo.presentationTimeUs = pts

                        if (muxerStarted && muxerTrackIndex >= 0) {
                            try {
                                muxer.writeSampleData(muxerTrackIndex, encOutBuf, encoderBufferInfo)
                            } catch (t: Throwable) {
                                Timber.tag(TAG).w(t, "MediaMuxer writeSampleData failed")
                                muxerStarted = false
                            }
                        }

                        if (!muxerStarted) {
                            encOutBuf.position(encoderBufferInfo.offset)
                            val packetBytes = ByteArray(encoderBufferInfo.size)
                            encOutBuf.get(packetBytes)
                            fallbackPackets.add(Mp4ContainerWriter.AudioPacket(packetBytes, pts))
                        }
                    }
                    encoder.releaseOutputBuffer(encOutIdx, false)
                }
            }

            var muxerFinishedOk = false
            if (muxerStarted) {
                try {
                    muxer.stop()
                    muxerFinishedOk = tempWritingFile.exists() && tempWritingFile.length() > 1024L
                } catch (t: Throwable) {
                    Timber.tag(TAG).w(t, "MediaMuxer stop failed")
                }
            }
            runCatching { muxer.release() }
            muxer = null

            if (muxerFinishedOk) {
                tempWritingFile.renameTo(targetFile)
                return targetFile.exists() && targetFile.length() > 1024L
            }

            // Fallback: write using pure Mp4ContainerWriter
            if (fallbackPackets.size >= 2) {
                val ok = Mp4ContainerWriter.writeAudioOnlyM4a(
                    outputFile = targetFile,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    csd0 = csd0Bytes,
                    packets = fallbackPackets,
                )
                tempWritingFile.delete()
                return ok && targetFile.exists() && targetFile.length() > 1024L
            }

            tempWritingFile.delete()
            return false
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "transcodeAudioWithDsp failed")
            tempWritingFile.delete()
            return false
        } finally {
            runCatching { extractor.release() }
            runCatching { decoder?.stop(); decoder?.release() }
            runCatching { encoder?.stop(); encoder?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }
    }

    /**
     * Copies the song to `Music/Beatles/` with optional [AudioStyleMode] and [ParametricEQ] DSP processing.
     */
    suspend fun saveSongToDevice(
        context: Context,
        songId: String,
        title: String,
        artist: String?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        audioStyleMode: AudioStyleMode = AudioStyleMode.NORMAL,
        applyEq: Boolean = false,
        onProgress: ((String, Float) -> Unit)? = null,
    ): SaveToDeviceResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        var tempSourceFile: File? = null
        var tempTranscodedFile: File? = null

        try {
            // 1. Storage permission check on Android 9 and below
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val granted = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    return@withContext SaveToDeviceResult.PermissionDenied
                }
            }

            // 2. Extract source file
            onProgress?.invoke("Menyiapkan file audio...", 0.05f)
            tempSourceFile = extractSourceToTemp(appContext, songId, downloadUtil, onProgress)
                ?: return@withContext SaveToDeviceResult.MissingSourceFile

            val sourceLength = tempSourceFile.length()
            if (sourceLength <= 0L) {
                return@withContext SaveToDeviceResult.MissingSourceFile
            }

            if (!hasEnoughFreeSpace(appContext, sourceLength)) {
                return@withContext SaveToDeviceResult.StorageFull
            }

            // 3. Determine if DSP transcode is required
            val activeParametricEq = if (applyEq) {
                EQProfileRepository.loadActiveParametricEQ(appContext)
            } else null

            val needsDsp = audioStyleMode != AudioStyleMode.NORMAL || activeParametricEq != null

            val styleTag = buildString {
                if (audioStyleMode != AudioStyleMode.NORMAL) {
                    append(audioStyleMode.label)
                }
                if (activeParametricEq != null) {
                    if (isNotEmpty()) append(" + ")
                    append("EQ")
                }
            }.ifBlank { null }

            val cleanBaseName = buildCleanBaseFileName(title = title, artist = artist, styleTag = styleTag)

            val publicMusicDir = runCatching {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            }.getOrNull()
            val legacyTargetDir = if (publicMusicDir != null) {
                File(publicMusicDir, TARGET_SUBFOLDER)
            } else {
                File(Environment.getExternalStorageDirectory(), "Music/$TARGET_SUBFOLDER")
            }

            val finalFileToCopy: File
            val finalExt: String
            val finalMimeType: String

            if (needsDsp) {
                // Transcode with effects to standard high-quality M4A
                val tempDir = File(appContext.cacheDir, "dsp_export_temp").apply { mkdirs() }
                val styledTarget = File(tempDir, "styled_${songId.hashCode()}_${System.currentTimeMillis()}.m4a")

                val transcodeOk = transcodeAudioWithDsp(
                    sourceFile = tempSourceFile,
                    targetFile = styledTarget,
                    audioStyleMode = audioStyleMode,
                    parametricEQ = activeParametricEq,
                    onProgress = onProgress,
                )

                if (!transcodeOk || !styledTarget.exists() || styledTarget.length() <= 1024L) {
                    styledTarget.delete()
                    return@withContext SaveToDeviceResult.Error("Gagal menerapkan efek suara pada lagu.")
                }

                tempTranscodedFile = styledTarget
                finalFileToCopy = styledTarget
                finalExt = "m4a"
                finalMimeType = "audio/mp4"
            } else {
                // Direct fast copy of original file
                val formatEntity = runCatching { database.format(songId).firstOrNull() }.getOrNull()
                val (detectedExt, detectedMime) = detectExtensionAndMime(
                    sourceFile = tempSourceFile,
                    dbMimeType = formatEntity?.mimeType,
                    dbCodecs = formatEntity?.codecs,
                )
                finalFileToCopy = tempSourceFile
                finalExt = detectedExt
                finalMimeType = detectedMime
            }

            onProgress?.invoke("Menyimpan ke Music/Beatles...", 0.92f)

            // 4. Save to destination
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appContext.contentResolver
                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val candidates = buildMediaStoreCandidates(finalExt, finalMimeType)

                // Check for duplicates
                for ((candExt, _) in candidates) {
                    val candidateName = "$cleanBaseName.$candExt"
                    val candidateLegacyFile = File(legacyTargetDir, candidateName)
                    if (fileAlreadyExistsInMediaStore(appContext, candidateName) ||
                        (runCatching { candidateLegacyFile.exists() && candidateLegacyFile.length() > 0L }.getOrDefault(false))
                    ) {
                        return@withContext SaveToDeviceResult.AlreadyExists(candidateName)
                    }
                }

                var insertedUri: Uri? = null
                var finalFileName = "$cleanBaseName.$finalExt"
                var finalMime = finalMimeType
                var lastInsertError: Throwable? = null

                for ((candExt, candMime) in candidates) {
                    val candidateName = "$cleanBaseName.$candExt"
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, candidateName)
                        val displayTitle = if (styleTag != null) "$title [$styleTag]" else title
                        put(MediaStore.Audio.Media.TITLE, displayTitle.ifBlank { cleanBaseName })
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
                            finalMime = candMime
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
                        FileInputStream(finalFileToCopy).use { input ->
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
                            arrayOf(finalMime),
                            null,
                        )
                    }

                    return@withContext SaveToDeviceResult.Success(
                        fileName = finalFileName,
                        styleLabel = audioStyleMode.label.takeIf { audioStyleMode != AudioStyleMode.NORMAL },
                        eqApplied = activeParametricEq != null,
                    )
                } catch (t: Throwable) {
                    runCatching { resolver.delete(itemUri, null, null) }
                    if (isStorageOutOfSpaceException(t)) {
                        return@withContext SaveToDeviceResult.StorageFull
                    }
                    return@withContext SaveToDeviceResult.Error(
                        t.localizedMessage ?: "Terjadi kesalahan saat menulis file ke perangkat."
                    )
                }
            } else {
                // Android 9 and below
                val finalFileName = "$cleanBaseName.$finalExt"
                val legacyTargetFile = File(legacyTargetDir, finalFileName)

                if (legacyTargetFile.exists() && legacyTargetFile.length() > 0L) {
                    return@withContext SaveToDeviceResult.AlreadyExists(finalFileName)
                }

                if (!legacyTargetDir.exists() && !legacyTargetDir.mkdirs()) {
                    return@withContext SaveToDeviceResult.Error("Gagal membuat folder Music/Beatles di penyimpanan perangkat.")
                }

                try {
                    FileOutputStream(legacyTargetFile).use { output ->
                        FileInputStream(finalFileToCopy).use { input ->
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
                            arrayOf(finalMimeType),
                            null,
                        )
                    }

                    return@withContext SaveToDeviceResult.Success(
                        fileName = finalFileName,
                        styleLabel = audioStyleMode.label.takeIf { audioStyleMode != AudioStyleMode.NORMAL },
                        eqApplied = activeParametricEq != null,
                    )
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
            runCatching { tempTranscodedFile?.delete() }
        }
    }
}

/**
 * Material 3 Dialog to configure audio style and EQ options before saving to device.
 */
@Composable
fun SaveToDeviceOptionsDialog(
    songTitle: String,
    artist: String?,
    currentAudioStyle: AudioStyleMode,
    isEqActive: Boolean,
    activeEqSummary: String?,
    onDismissRequest: () -> Unit,
    onConfirmSave: (AudioStyleMode, Boolean) -> Unit,
) {
    val initialHasEffects = currentAudioStyle != AudioStyleMode.NORMAL || isEqActive
    var saveModeWithEffects by remember { mutableStateOf(initialHasEffects) }
    var selectedStyle by remember { mutableStateOf(currentAudioStyle) }
    var applyEq by remember { mutableStateOf(isEqActive) }

    val computedStyleTag = remember(saveModeWithEffects, selectedStyle, applyEq) {
        if (!saveModeWithEffects) null
        else buildString {
            if (selectedStyle != AudioStyleMode.NORMAL) append(selectedStyle.label)
            if (applyEq) {
                if (isNotEmpty()) append(" + ")
                append("EQ")
            }
        }.ifBlank { null }
    }

    val previewFileName = remember(songTitle, artist, computedStyleTag) {
        val base = SaveToDeviceManager.buildCleanBaseFileName(songTitle, artist, computedStyleTag)
        "$base.m4a"
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Simpan ke Perangkat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Subtitle
                Text(
                    text = "$songTitle • ${artist ?: "Beatles"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Mode 1: Audio Asli
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (!saveModeWithEffects) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { saveModeWithEffects = false },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = !saveModeWithEffects,
                            onClick = { saveModeWithEffects = false },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Asli",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Simpan file asli tanpa efek suara tambahan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Mode 2: Dengan Gaya Suara & EQ
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (saveModeWithEffects) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { saveModeWithEffects = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = saveModeWithEffects,
                            onClick = { saveModeWithEffects = true },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dengan Gaya Suara & EQ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Menerapkan efek gaya suara & equalizer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Expanded settings when "Dengan Gaya Suara & EQ" is selected
                if (saveModeWithEffects) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Pilih Gaya Suara",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        // Horizontal Chip Row for AudioStyleMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AudioStyleMode.entries.forEach { mode ->
                                val isSelected = selectedStyle == mode
                                val iconRes = when (mode) {
                                    AudioStyleMode.NORMAL -> R.drawable.music_note
                                    AudioStyleMode.SPEED_UP -> R.drawable.speed
                                    AudioStyleMode.SLOWED_REVERB -> R.drawable.graphic_eq
                                    AudioStyleMode.SUPER_SLOWED -> R.drawable.graphic_eq
                                    AudioStyleMode.NIGHTCORE -> R.drawable.speed
                                    AudioStyleMode.LOFI -> R.drawable.equalizer
                                    AudioStyleMode.EIGHT_D -> R.drawable.graphic_eq
                                    AudioStyleMode.CONCERT -> R.drawable.equalizer
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStyle = mode },
                                    label = { Text(text = mode.label, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                )
                            }
                        }

                        // Equalizer & Bass Boost Card with Switch
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Terapkan Equalizer & Bass Boost",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (!activeEqSummary.isNullOrBlank()) {
                                            "Profil aktif: $activeEqSummary"
                                        } else {
                                            "Pengaturan equalizer flat/nonaktif"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (applyEq) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                                Switch(
                                    checked = applyEq,
                                    onCheckedChange = { applyEq = it },
                                    modifier = Modifier.testTag("save_dialog_eq_switch"),
                                )
                            }
                        }
                    }
                }

                // File preview surface
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Tersimpan di: Music/Beatles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = previewFileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalStyle = if (saveModeWithEffects) selectedStyle else AudioStyleMode.NORMAL
                    val finalEq = if (saveModeWithEffects) applyEq else false
                    onConfirmSave(finalStyle, finalEq)
                },
                modifier = Modifier.testTag("save_dialog_confirm_button"),
            ) {
                Text(text = "Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Batal")
            }
        },
    )
}

/**
 * Controller managing the save action lifecycle: options dialog, permission gating,
 * transcode progress, and result alerts.
 */
class SaveToDeviceController(
    private val onTriggerSaveAction: () -> Unit,
) {
    fun onTriggerSave() {
        onTriggerSaveAction()
    }
}

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
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()

    val currentSongId by rememberUpdatedState(songId)
    val currentTitle by rememberUpdatedState(title)
    val currentArtist by rememberUpdatedState(artist)
    val currentOnDismiss by rememberUpdatedState(onFinishedDismissMenu)

    val activeAudioStyle by (playerConnection?.service?.audioStyleMode
        ?: MutableStateFlow(AudioStyleMode.NORMAL)).collectAsState()

    val activeEqSummary = remember { EQProfileRepository.getActiveEqSummaryLabel(context) }
    val isEqActive = remember { EQProfileRepository.loadActiveParametricEQ(context) != null }

    var showOptionsDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var progressStage by remember { mutableStateOf("Menyiapkan...") }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var saveResult by remember { mutableStateOf<SaveToDeviceResult?>(null) }

    var pendingStyle by remember { mutableStateOf(AudioStyleMode.NORMAL) }
    var pendingEq by remember { mutableStateOf(false) }

    val executeSave: (AudioStyleMode, Boolean) -> Unit = { styleMode, eqEnabled ->
        if (!isSaving) {
            isSaving = true
            progressStage = "Menyiapkan file audio..."
            progressFraction = 0f
            scope.launch {
                val res = try {
                    SaveToDeviceManager.saveSongToDevice(
                        context = context,
                        songId = currentSongId,
                        title = currentTitle,
                        artist = currentArtist,
                        database = database,
                        downloadUtil = downloadUtil,
                        audioStyleMode = styleMode,
                        applyEq = eqEnabled,
                        onProgress = { stage, fraction ->
                            progressStage = stage
                            progressFraction = fraction
                        },
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
            currentExecuteSave(pendingStyle, pendingEq)
        } else {
            saveResult = SaveToDeviceResult.PermissionDenied
        }
    }

    val requestSaveWithConfig: (AudioStyleMode, Boolean) -> Unit = { style, eq ->
        pendingStyle = style
        pendingEq = eq
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                currentExecuteSave(style, eq)
            } else {
                runCatching {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }.onFailure {
                    saveResult = SaveToDeviceResult.PermissionDenied
                }
            }
        } else {
            currentExecuteSave(style, eq)
        }
    }

    // 1. Configuration Dialog (Audio Asli vs Gaya Suara & EQ)
    if (showOptionsDialog) {
        SaveToDeviceOptionsDialog(
            songTitle = currentTitle,
            artist = currentArtist,
            currentAudioStyle = activeAudioStyle,
            isEqActive = isEqActive,
            activeEqSummary = activeEqSummary,
            onDismissRequest = { showOptionsDialog = false },
            onConfirmSave = { chosenStyle, chosenEq ->
                showOptionsDialog = false
                requestSaveWithConfig(chosenStyle, chosenEq)
            },
        )
    }

    // 2. Loading & Transcode Progress Dialog
    if (isSaving) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                        )
                        Text(
                            text = progressStage,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (progressFraction > 0f) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                        )
                    }
                }
            }
        }
    }

    // 3. Result AlertDialog
    saveResult?.let { result ->
        val isSuccess = result is SaveToDeviceResult.Success
        val dialogTitle = when (result) {
            is SaveToDeviceResult.Success -> "Berhasil Disimpan"
            is SaveToDeviceResult.AlreadyExists -> "File Sudah Ada"
            is SaveToDeviceResult.StorageFull -> "Penyimpanan Penuh"
            is SaveToDeviceResult.PermissionDenied -> "Izin Penyimpanan Ditolak"
            is SaveToDeviceResult.MissingSourceFile -> "File Audio Tidak Ditemukan"
            is SaveToDeviceResult.Error -> "Gagal Menyimpan"
        }
        val dialogMessage = when (result) {
            is SaveToDeviceResult.Success -> {
                val effectsDesc = buildString {
                    if (result.styleLabel != null) append("Gaya Suara: ${result.styleLabel}")
                    if (result.eqApplied) {
                        if (isNotEmpty()) append(" • ")
                        append("Equalizer Aktif")
                    }
                }.ifBlank { "Audio Asli" }
                "File musik tersimpan di folder Music/Beatles\n\nNama file: ${result.fileName}\nEfek: $effectsDesc"
            }
            is SaveToDeviceResult.AlreadyExists ->
                "File musik sudah ada di Music/Beatles (${result.fileName}). Tidak dibuat salinan duplikat."
            is SaveToDeviceResult.StorageFull ->
                "Gagal menyimpan lagu karena ruang penyimpanan perangkat penuh. Kosongkan sebagian ruang penyimpanan lalu coba lagi."
            is SaveToDeviceResult.PermissionDenied ->
                "Izin akses penyimpanan ditolak. Berikan izin penyimpanan agar aplikasi dapat menyimpan file lagu ke folder Music/Beatles."
            is SaveToDeviceResult.MissingSourceFile ->
                "File audio tidak dapat diunduh atau diproses. Pastikan koneksi internet aktif lalu coba lagi."
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
        SaveToDeviceController(onTriggerSaveAction = { showOptionsDialog = true })
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
            Text(text = "Simpan ke folder Music/Beatles dengan gaya suara & EQ")
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
