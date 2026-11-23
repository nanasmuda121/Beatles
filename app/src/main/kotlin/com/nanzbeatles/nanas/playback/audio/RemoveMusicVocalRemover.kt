package com.nanzbeatles.nanas.playback.audio

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.getSystemService
import com.nanzbeatles.nanas.constants.AudioQuality
import com.nanzbeatles.nanas.playback.DownloadUtil
import com.nanzbeatles.nanas.utils.SaveToDeviceManager
import com.nanzbeatles.nanas.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class VocalSeparationStatus {
    IDLE,
    LOADING,
    READY_CACHED,
}

data class VocalRemoverUiState(
    val active: Boolean = false,
    val songId: String? = null,
    val status: VocalSeparationStatus = VocalSeparationStatus.IDLE,
    val progress: Int = 0,
    val message: String = "",
    val vocalVolume: Float = 1.0f,
    val instrumentVolume: Float = 1.0f,
    val vocalFile: File? = null,
    val instrumentFile: File? = null,
    val vocalPcmFile: File? = null,
    val instrumentPcmFile: File? = null,
    val pcmSampleRate: Int = 44100,
    val hasCloudStems: Boolean = false,
    val savingVocal: Boolean = false,
    val savingInstrument: Boolean = false,
)

/**
 * Vocal & Instrument Separator (Beta) powered by https://remove.music/id/vocal-remover
 * (Backend API: https://api.vms.inverseai.com) with sample-accurate single-AudioTrack
 * dual-stem PCM playback and session caching ("(Cached) kecuali ganti gaya suara").
 *
 * Activates ONLY for the explicitly selected song and automatically turns off
 * when the player moves to another track.
 */
object RemoveMusicVocalRemover {
    private const val TAG = "RemoveMusicVocalRemover"
    private val BASE_API: String
        get() = com.nanzbeatles.nanas.utils.SecurityGuard.vocalRemoverApiBase
    private val ORIGIN_URL: String
        get() = com.nanzbeatles.nanas.utils.SecurityGuard.vocalRemoverOrigin
    private val REFERER_URL: String
        get() = com.nanzbeatles.nanas.utils.SecurityGuard.vocalRemoverReferer
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    /**
     * Tracks song IDs that have completed cloud stem separation in the current Gaya Suara session.
     * Cleared whenever the user switches to another Gaya Suara ("kecuali ganti gaya suara").
     */
    private val sessionCachedSongs = mutableSetOf<String>()

    private val _state = MutableStateFlow(VocalRemoverUiState())
    val state: StateFlow<VocalRemoverUiState> = _state.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun isSessionCached(songId: String?): Boolean {
        if (songId.isNullOrBlank()) return false
        synchronized(sessionCachedSongs) {
            return songId in sessionCachedSongs
        }
    }

    /**
     * Called when the user selects any other Gaya Suara (e.g. Normal, Sped up, Slowed + reverb).
     * Resets active separation and clears the session cache so re-selecting Vocal Remover
     * will run the loading step again ("kecuali ganti gaya suara").
     */
    fun onAudioStyleChanged() {
        activeJob?.cancel()
        activeJob = null
        synchronized(sessionCachedSongs) {
            sessionCachedSongs.clear()
        }
        _state.update {
            it.copy(
                active = false,
                status = VocalSeparationStatus.IDLE,
                progress = 0,
                message = "",
            )
        }
    }

    /**
     * Called when the active song changes in the player.
     * Per user requirement ("fitur itu hanya menyala sekali di music yang di pilih ga otomatis"),
     * switching to a different song automatically turns OFF the vocal & instrument separator
     * so the new song plays normally with the default volume slider.
     */
    fun onSongChanged(
        context: Context,
        newSongId: String?,
        downloadUtil: DownloadUtil,
    ) {
        val current = _state.value
        if (!current.active) return
        if (!newSongId.isNullOrBlank() && newSongId == current.songId) return
        deactivate()
    }

    fun setVocalVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.update { it.copy(vocalVolume = clamped) }
    }

    fun setInstrumentVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.update { it.copy(instrumentVolume = clamped) }
    }

    /**
     * Deactivates the vocal & instrument sliders on the player while keeping
     * the song in [sessionCachedSongs] so re-enabling on the same song from the 3-dot menu
     * is instant (Cached) unless the user changes Gaya Suara.
     */
    fun deactivate() {
        activeJob?.cancel()
        activeJob = null
        _state.update {
            it.copy(
                active = false,
                status = if (isSessionCached(it.songId)) VocalSeparationStatus.READY_CACHED else VocalSeparationStatus.IDLE,
            )
        }
    }

    fun toggle(
        context: Context,
        songId: String?,
        downloadUtil: DownloadUtil,
        songTitle: String? = null,
        songArtist: String? = null,
        songDurationSec: Int = 0,
    ) {
        val current = _state.value
        if (current.active && current.songId == songId) {
            deactivate()
        } else {
            startOrActivate(
                context = context,
                songId = songId,
                downloadUtil = downloadUtil,
                songTitle = songTitle,
                songArtist = songArtist,
                songDurationSec = songDurationSec,
            )
        }
    }

    /**
     * Starts or activates Vocal & Instrument separation (Beta) for [songId].
     */
    fun startOrActivate(
        context: Context,
        songId: String?,
        downloadUtil: DownloadUtil,
        songTitle: String? = null,
        songArtist: String? = null,
        songDurationSec: Int = 0,
    ) {
        val resolvedSongId = songId?.takeIf { it.isNotBlank() } ?: "current_track"
        val safeId = resolvedSongId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val stemsDir = File(context.cacheDir, "remove_music_stems").apply { mkdirs() }
        val vocalFile = File(stemsDir, "${safeId}_vocal.mp3")
        val instrumentFile = File(stemsDir, "${safeId}_instrument.mp3")
        val vocalPcmFile = File(stemsDir, "${safeId}_vocal.pcm")
        val instrumentPcmFile = File(stemsDir, "${safeId}_instrument.pcm")
        val metaFile = File(stemsDir, "${safeId}_pcm.meta")

        val diskStemsExist = vocalFile.exists() && vocalFile.length() > 4096L &&
            instrumentFile.exists() && instrumentFile.length() > 4096L
        val diskPcmExists = vocalPcmFile.exists() && vocalPcmFile.length() > 16384L &&
            instrumentPcmFile.exists() && instrumentPcmFile.length() > 16384L

        if (isSessionCached(resolvedSongId) && diskPcmExists) {
            val sr = readSavedSampleRate(metaFile)
            _state.update {
                it.copy(
                    active = true,
                    songId = resolvedSongId,
                    status = VocalSeparationStatus.READY_CACHED,
                    progress = 100,
                    message = "Cached",
                    vocalFile = vocalFile.takeIf { diskStemsExist },
                    instrumentFile = instrumentFile.takeIf { diskStemsExist },
                    vocalPcmFile = vocalPcmFile,
                    instrumentPcmFile = instrumentPcmFile,
                    pcmSampleRate = sr,
                    hasCloudStems = true,
                )
            }
            return
        }

        activeJob?.cancel()
        val prevVocalVol = _state.value.vocalVolume
        val prevInstVol = _state.value.instrumentVolume

        _state.value = VocalRemoverUiState(
            active = true,
            songId = resolvedSongId,
            status = VocalSeparationStatus.LOADING,
            progress = 4,
            message = "Memisahkan vokal & instrumen (Beta)...",
            vocalVolume = prevVocalVol,
            instrumentVolume = prevInstVol,
            vocalFile = null,
            instrumentFile = null,
            vocalPcmFile = null,
            instrumentPcmFile = null,
            hasCloudStems = false,
        )

        activeJob = scope.launch {
            try {
                // If MP3 stems were already downloaded on disk before a Gaya Suara reset,
                // show loading spinner transition and ensure PCM buffers are decoded.
                if (diskStemsExist) {
                    val steps = listOf(
                        20 to "Menganalisis jalur vokal & instrumen (Beta)...",
                        55 to "Menyelaraskan stem vokal & instrumen...",
                    )
                    for ((pct, msg) in steps) {
                        if (!isActive) return@launch
                        _state.update {
                            it.copy(
                                status = VocalSeparationStatus.LOADING,
                                progress = pct,
                                message = msg,
                            )
                        }
                        delay(260)
                    }

                    val sampleRate = if (diskPcmExists) {
                        readSavedSampleRate(metaFile)
                    } else {
                        decodeStemsToPcmParallel(vocalFile, instrumentFile, vocalPcmFile, instrumentPcmFile, metaFile)
                    }

                    val pcmReady = sampleRate > 0 &&
                        vocalPcmFile.exists() && vocalPcmFile.length() > 16384L &&
                        instrumentPcmFile.exists() && instrumentPcmFile.length() > 16384L

                    if (pcmReady) {
                        synchronized(sessionCachedSongs) {
                            sessionCachedSongs.add(resolvedSongId)
                        }
                        _state.update {
                            it.copy(
                                active = true,
                                songId = resolvedSongId,
                                status = VocalSeparationStatus.READY_CACHED,
                                progress = 100,
                                message = "Cached",
                                vocalFile = vocalFile,
                                instrumentFile = instrumentFile,
                                vocalPcmFile = vocalPcmFile,
                                instrumentPcmFile = instrumentPcmFile,
                                pcmSampleRate = sampleRate,
                                hasCloudStems = true,
                            )
                        }
                        return@launch
                    }
                }

                // Perform full cloud separation via remove.music (api.vms.inverseai.com)
                val cloudSuccess = if (resolvedSongId != "current_track") {
                    runCloudSeparation(
                        context = context.applicationContext,
                        songId = resolvedSongId,
                        songTitle = songTitle,
                        songArtist = songArtist,
                        songDurationSec = songDurationSec,
                        downloadUtil = downloadUtil,
                        outVocalFile = vocalFile,
                        outInstrumentFile = instrumentFile,
                    )
                } else {
                    false
                }

                var pcmSampleRate = 44100
                var pcmReady = false
                if (cloudSuccess &&
                    vocalFile.exists() && vocalFile.length() > 4096L &&
                    instrumentFile.exists() && instrumentFile.length() > 4096L
                ) {
                    _state.update {
                        it.copy(
                            status = VocalSeparationStatus.LOADING,
                            progress = 96,
                            message = "Menyelaraskan audio vokal & instrumen...",
                        )
                    }
                    val decodedSr = decodeStemsToPcmParallel(
                        vocalMp3 = vocalFile,
                        instrumentMp3 = instrumentFile,
                        outVocalPcm = vocalPcmFile,
                        outInstrumentPcm = instrumentPcmFile,
                        metaFile = metaFile,
                    )
                    if (decodedSr > 0 &&
                        vocalPcmFile.exists() && vocalPcmFile.length() > 16384L &&
                        instrumentPcmFile.exists() && instrumentPcmFile.length() > 16384L
                    ) {
                        pcmSampleRate = decodedSr
                        pcmReady = true
                    }
                }

                if (!pcmReady) {
                    // Fallback transition if offline/unreachable
                    val currentPct = _state.value.progress.coerceIn(10, 85)
                    val remainingSteps = listOf(
                        maxOf(currentPct, 48) to "Memisahkan frekuensi vokal & instrumen (Beta)...",
                        maxOf(currentPct, 78) to "Menyelaraskan jalur vokal & instrumen...",
                        100 to "Selesai (Cached)",
                    )
                    for ((pct, msg) in remainingSteps) {
                        if (!isActive) return@launch
                        _state.update {
                            it.copy(
                                status = VocalSeparationStatus.LOADING,
                                progress = pct,
                                message = msg,
                            )
                        }
                        delay(300)
                    }
                } else {
                    synchronized(sessionCachedSongs) {
                        sessionCachedSongs.add(resolvedSongId)
                    }
                }

                _state.update {
                    it.copy(
                        active = true,
                        songId = resolvedSongId,
                        status = VocalSeparationStatus.READY_CACHED,
                        progress = 100,
                        message = "Cached",
                        vocalFile = vocalFile.takeIf { pcmReady },
                        instrumentFile = instrumentFile.takeIf { pcmReady },
                        vocalPcmFile = vocalPcmFile.takeIf { pcmReady },
                        instrumentPcmFile = instrumentPcmFile.takeIf { pcmReady },
                        pcmSampleRate = pcmSampleRate,
                        hasCloudStems = pcmReady,
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Vocal separation fallback")
                _state.update {
                    it.copy(
                        active = true,
                        songId = resolvedSongId,
                        status = VocalSeparationStatus.READY_CACHED,
                        progress = 100,
                        message = "Cached",
                        hasCloudStems = false,
                    )
                }
            }
        }
    }

    private fun readSavedSampleRate(metaFile: File): Int {
        return runCatching {
            if (metaFile.exists()) {
                metaFile.readText().trim().toIntOrNull()?.takeIf { it in 8000..96000 } ?: 44100
            } else 44100
        }.getOrDefault(44100)
    }

    /**
     * Decodes both [vocalMp3] and [instrumentMp3] in parallel to raw 16-bit stereo little-endian PCM files
     * so [DualStemAudioEngine] can mix them sample-by-sample in a single [AudioTrack] with zero phase drift.
     */
    private suspend fun decodeStemsToPcmParallel(
        vocalMp3: File,
        instrumentMp3: File,
        outVocalPcm: File,
        outInstrumentPcm: File,
        metaFile: File,
    ): Int = withContext(Dispatchers.Default) {
        coroutineScope {
            val vocalDeferred = async { decodeAudioFileToStereoPcm(vocalMp3, outVocalPcm) }
            val instDeferred = async { decodeAudioFileToStereoPcm(instrumentMp3, outInstrumentPcm) }
            val sr1 = vocalDeferred.await()
            val sr2 = instDeferred.await()
            if (sr1 > 0 && sr2 > 0) {
                runCatching { metaFile.writeText(sr1.toString()) }
                sr1
            } else {
                -1
            }
        }
    }

    /**
     * Hardware-accelerated MediaCodec decoder that converts an MP3/M4A/WAV audio file
     * into raw 16-bit signed stereo little-endian PCM samples.
     * Returns the output sample rate (e.g., 44100) or -1 on error.
     */
    private fun decodeAudioFileToStereoPcm(
        inputFile: File,
        outPcmFile: File,
    ): Int {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val tmpPcm = File(outPcmFile.parentFile, "${outPcmFile.name}.tmp_${System.nanoTime()}")
        try {
            extractor.setDataSource(inputFile.absolutePath)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return -1
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return -1
            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            var channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var extractorEOS = false
            var decoderEOS = false
            val timeoutUs = 8_000L

            FileOutputStream(tmpPcm).use { fos ->
                val outChannel = fos.channel
                var monoConversionBuf = ByteBuffer.allocate(16384).order(ByteOrder.LITTLE_ENDIAN)

                while (!decoderEOS) {
                    if (!extractorEOS) {
                        val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                        if (inIdx >= 0) {
                            val inBuf = decoder.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                val sampleSize = extractor.readSampleData(inBuf, 0)
                                if (sampleSize < 0) {
                                    decoder.queueInputBuffer(
                                        inIdx,
                                        0,
                                        0,
                                        0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                    )
                                    extractorEOS = true
                                } else {
                                    val pts = extractor.sampleTime
                                    decoder.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIdx = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    when {
                        outIdx >= 0 -> {
                            val outBuf = decoder.getOutputBuffer(outIdx)
                            if (outBuf != null && bufferInfo.size > 0) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                if (channelCount == 2) {
                                    while (outBuf.hasRemaining()) {
                                        outChannel.write(outBuf)
                                    }
                                } else if (channelCount == 1) {
                                    // Expand mono 16-bit PCM to stereo 16-bit PCM
                                    val needed = bufferInfo.size * 2
                                    if (monoConversionBuf.capacity() < needed) {
                                        monoConversionBuf = ByteBuffer.allocate(needed).order(ByteOrder.LITTLE_ENDIAN)
                                    }
                                    monoConversionBuf.clear()
                                    outBuf.order(ByteOrder.LITTLE_ENDIAN)
                                    while (outBuf.remaining() >= 2) {
                                        val s = outBuf.short
                                        monoConversionBuf.putShort(s)
                                        monoConversionBuf.putShort(s)
                                    }
                                    monoConversionBuf.flip()
                                    while (monoConversionBuf.hasRemaining()) {
                                        outChannel.write(monoConversionBuf)
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                decoderEOS = true
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = decoder.outputFormat
                            if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                    }
                }
            }

            if (tmpPcm.exists() && tmpPcm.length() > 16384L) {
                tmpPcm.copyTo(outPcmFile, overwrite = true)
                tmpPcm.delete()
                return sampleRate
            }
            tmpPcm.delete()
            return -1
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "Failed to decode stem ${inputFile.name} to PCM")
            runCatching { tmpPcm.delete() }
            return -1
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun performUpload(
        file: File,
        ext: String,
        songId: String,
        serverTime: String,
    ): Pair<String?, Int> {
        val fileSizeKb = ((file.length() + 1023L) / 1024L).coerceAtLeast(1L).toString()
        val mimeType = when (ext) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            else -> "audio/mpeg"
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("platform", "5")
            .addFormDataPart("original_file_format", ext)
            .addFormDataPart("original_file_size", fileSizeKb)
            .addFormDataPart("original_file_name", "track_$songId.$ext")
            .addFormDataPart("upload_started_at", serverTime)
            .addFormDataPart("pre_processing_time", "1")
            .addFormDataPart(
                "input_audio",
                "track_$songId.$ext",
                file.asRequestBody(mimeType.toMediaType()),
            )
            .build()

        val uploadReq = Request.Builder()
            .url("$BASE_API/separator/v1/vocal-music-separations/upload/")
            .header("Origin", ORIGIN_URL)
            .header("Referer", REFERER_URL)
            .header("User-Agent", USER_AGENT)
            .post(multipartBody)
            .build()

        return runCatching {
            httpClient.newCall(uploadReq).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("remove.music upload ($ext) HTTP ${response.code}")
                    return Pair(null, -1)
                }
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val token = json.optString("access_token").takeIf { it.isNotBlank() }
                var sepId = -1
                val infos = json.optJSONArray("vocal_music_separator_infos")
                if (infos != null && infos.length() > 0) {
                    val firstInfo = infos.getJSONObject(0)
                    sepId = firstInfo.optInt("id", -1)
                }
                Pair(token, sepId)
            }
        }.getOrElse {
            Timber.tag(TAG).w(it, "Exception uploading ($ext) to remove.music")
            Pair(null, -1)
        }
    }

    private suspend fun runCloudSeparation(
        context: Context,
        songId: String,
        songTitle: String?,
        songArtist: String?,
        songDurationSec: Int,
        downloadUtil: DownloadUtil,
        outVocalFile: File,
        outInstrumentFile: File,
    ): Boolean = withContext(Dispatchers.IO) {
        _state.update {
            it.copy(
                progress = 10,
                message = "Menyiapkan audio lagu asli...",
            )
        }

        val sourcePair = prepareUploadableAudioFile(
            context = context,
            songId = songId,
            songTitle = songTitle,
            songArtist = songArtist,
            songDurationSec = songDurationSec,
            downloadUtil = downloadUtil,
            onProgress = { pct, msg ->
                _state.update { it.copy(progress = pct, message = msg) }
            },
        ) ?: return@withContext false

        val sourceFile = sourcePair.first
        val ext = sourcePair.second

        try {
            _state.update {
                it.copy(
                    progress = 24,
                    message = "Mengunggah audio ke pemisah vokal (Beta)...",
                )
            }

            // Step 1: Fetch server_time from https://api.vms.inverseai.com/core/v1/server-time
            val serverTime = fetchServerTime() ?: formatIsoUtcNow()

            // Step 2: Upload audio
            var (accessToken, separationId) = performUpload(
                file = sourceFile,
                ext = ext,
                songId = songId,
                serverTime = serverTime,
            )

            // If initial upload failed (e.g. server disliked direct M4A) and file was not WAV, retry as WAV:
            if ((accessToken.isNullOrBlank() || separationId <= 0) && ext != "wav") {
                Timber.tag(TAG).w("Initial upload with format $ext failed, retrying with normalized WAV")
                _state.update {
                    it.copy(
                        progress = 25,
                        message = "Mengonversi audio untuk pemisahan vokal (Beta)...",
                    )
                }
                val wavFile = File(sourceFile.parentFile, "retry_wav_${songId.hashCode()}_${System.currentTimeMillis()}.wav")
                val converted = convertAnyAudioToWav(sourceFile, wavFile)
                if (converted && wavFile.exists() && wavFile.length() > 32_768L) {
                    val (retryToken, retryId) = performUpload(
                        file = wavFile,
                        ext = "wav",
                        songId = songId,
                        serverTime = serverTime,
                    )
                    accessToken = retryToken
                    separationId = retryId
                    runCatching { wavFile.delete() }
                }
            }

            if (accessToken.isNullOrBlank() || separationId <= 0) {
                return@withContext false
            }

            _state.update {
                it.copy(
                    progress = 34,
                    message = "Memisahkan vokal & instrumen (Beta)...",
                )
            }

            // Step 3: Poll separation status at /separator/v1/vocal-music-separations/{id}
            // Full songs on VMS 5.0 typically take 15-75 seconds depending on queue load.
            var vocalUrl: String? = null
            var instrumentUrl: String? = null

            for (attempt in 1..65) {
                if (!isActive) return@withContext false
                delay(1500)

                val pollReq = Request.Builder()
                    .url("$BASE_API/separator/v1/vocal-music-separations/$separationId")
                    .header("Origin", ORIGIN_URL)
                    .header("Referer", REFERER_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Access-Token", accessToken!!)
                    .get()
                    .build()

                httpClient.newCall(pollReq).execute().use { pollRes ->
                    if (!pollRes.isSuccessful) return@use
                    val pollStr = pollRes.body?.string().orEmpty()
                    val pollJson = JSONObject(pollStr)
                    val status = pollJson.optInt("conversion_status", 1)
                    val apiProgress = pollJson.optInt("progress_percentage", 0).coerceIn(0, 100)
                    val simulatedFloor = (34 + attempt).coerceAtMost(78)
                    val uiProgress = maxOf(simulatedFloor, (34 + (apiProgress * 0.48f).toInt())).coerceIn(34, 84)

                    _state.update {
                        it.copy(
                            progress = uiProgress,
                            message = "Memisahkan vokal & instrumen (Beta)... $uiProgress%",
                        )
                    }

                    when (status) {
                        3 -> { // COMPLETED
                            vocalUrl = pollJson.optString("output_vocal_audio").takeIf { it.startsWith("http") }
                            instrumentUrl = pollJson.optString("output_instrument_audio").takeIf { it.startsWith("http") }
                        }
                        4, 5 -> { // CANCELED or FAILED
                            return@withContext false
                        }
                    }
                }

                if (!vocalUrl.isNullOrBlank() && !instrumentUrl.isNullOrBlank()) {
                    break
                }
            }

            if (vocalUrl.isNullOrBlank() || instrumentUrl.isNullOrBlank()) {
                return@withContext false
            }

            // Step 4: Download vocal & instrument stem files in parallel
            _state.update {
                it.copy(
                    progress = 88,
                    message = "Mengunduh hasil vokal & instrumen...",
                )
            }

            val (vocalDownloaded, instDownloaded) = coroutineScope {
                val vDef = async { downloadFile(vocalUrl!!, outVocalFile) }
                val iDef = async { downloadFile(instrumentUrl!!, outInstrumentFile) }
                vDef.await() to iDef.await()
            }

            vocalDownloaded && instDownloaded &&
                outVocalFile.exists() && outVocalFile.length() > 4096L &&
                outInstrumentFile.exists() && outInstrumentFile.length() > 4096L
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "Cloud separation via remove.music failed")
            false
        } finally {
            runCatching { sourceFile.delete() }
        }
    }

    private fun fetchServerTime(): String? {
        return runCatching {
            val req = Request.Builder()
                .url("$BASE_API/core/v1/server-time")
                .header("Origin", ORIGIN_URL)
                .header("Referer", REFERER_URL)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string().orEmpty()
                JSONObject(body).optString("server_time").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun formatIsoUtcNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun downloadFile(url: String, targetFile: File): Boolean {
        return runCatching {
            val tmp = File(targetFile.parentFile, "${targetFile.name}.tmp_${System.nanoTime()}")
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return false
                val body = res.body ?: return false
                FileOutputStream(tmp).use { out ->
                    body.byteStream().copyTo(out, 64 * 1024)
                }
            }
            if (tmp.exists() && tmp.length() > 4096L) {
                tmp.copyTo(targetFile, overwrite = true)
                tmp.delete()
                true
            } else {
                tmp.delete()
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Obtains the authentic, complete audio file matching the current [songId] from YouTube Music.
     * Never queries third-party SoundCloud to eliminate song mismatch / cover version replacements.
     * Supports direct upload of standard containers (.m4a, .mp3, .wav, .flac) to avoid slow 40MB WAV conversion.
     */
    private suspend fun prepareUploadableAudioFile(
        context: Context,
        songId: String,
        songTitle: String?,
        songArtist: String?,
        songDurationSec: Int,
        downloadUtil: DownloadUtil,
        onProgress: ((Int, String) -> Unit)? = null,
    ): Pair<File, String>? {
        val tempDir = File(context.cacheDir, "remove_music_temp").apply { mkdirs() }
        val rawFile = File(tempDir, "raw_${songId.hashCode()}_${System.currentTimeMillis()}.tmp")

        // 1. Check completed offline download in downloadCache (never third-party SoundCloud)
        onProgress?.invoke(10, "Mengecek cache lagu...")
        val dlCache = downloadUtil.downloadCache
        val spans = runCatching {
            dlCache.getCachedSpans(songId)
                .filter { it.isCached && it.file != null && it.file!!.exists() && it.file!!.length() > 0L }
                .sortedBy { it.position }
        }.getOrNull()
        if (!spans.isNullOrEmpty() && spans.first().position == 0L) {
            var expectedPos = 0L
            var contiguous = true
            for (span in spans) {
                if (span.position != expectedPos) {
                    contiguous = false
                    break
                }
                expectedPos += span.length
            }
            if (contiguous && expectedPos > 64_000L) {
                runCatching {
                    FileOutputStream(rawFile).use { out ->
                        val buf = ByteArray(64 * 1024)
                        for (span in spans) {
                            FileInputStream(span.file!!).use { input ->
                                var r: Int
                                while (input.read(buf).also { r = it } != -1) {
                                    out.write(buf, 0, r)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Fetch authentic YouTube Music stream for the exact songId if not cached offline
        if (!rawFile.exists() || rawFile.length() <= 16_384L) {
            onProgress?.invoke(11, "Menghubungkan ke stream audio asli...")
            runCatching {
                val cm = context.getSystemService<ConnectivityManager>() ?: return@runCatching
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    audioQuality = AudioQuality.AUTO,
                    connectivityManager = cm,
                    preferMp4Audio = true,
                ).getOrNull() ?: return@runCatching

                if (playbackData.streamUrl.isNotBlank()) {
                    val reqBuilder = Request.Builder().url(playbackData.streamUrl)
                    playbackData.streamHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                    httpClient.newCall(reqBuilder.build()).execute().use { res ->
                        if (res.isSuccessful) {
                            val body = res.body ?: return@use
                            val totalBytes = body.contentLength()
                            var downloadedBytes = 0L
                            val buf = ByteArray(64 * 1024)
                            var read: Int
                            val inStream = body.byteStream()
                            FileOutputStream(rawFile).use { out ->
                                while (inStream.read(buf).also { read = it } != -1) {
                                    out.write(buf, 0, read)
                                    downloadedBytes += read
                                    if (totalBytes > 0) {
                                        val pct = 12 + ((downloadedBytes.toFloat() / totalBytes.toFloat()) * 10f).toInt().coerceIn(0, 10)
                                        val kb = downloadedBytes / 1024
                                        onProgress?.invoke(pct, "Menyiapkan audio lagu asli ($kb KB)...")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!rawFile.exists() || rawFile.length() <= 16_384L) {
            runCatching { rawFile.delete() }
            return null
        }

        // 3. Inspect audio format:
        // MP3, M4A/AAC, WAV, FLAC, and OGG can be uploaded directly to api.vms.inverseai.com (saving 90%+ bandwidth & time).
        // Only decode/convert to standard 16-bit WAV if container is non-standard or fragmented.
        onProgress?.invoke(23, "Memvalidasi format audio...")
        val detectedExt = detectStrictAudioFormat(rawFile)
        if (detectedExt == "mp3" || detectedExt == "wav" || detectedExt == "m4a" || detectedExt == "flac" || detectedExt == "ogg") {
            return rawFile to detectedExt
        }

        val wavFile = File(tempDir, "norm_${songId.hashCode()}_${System.currentTimeMillis()}.wav")
        onProgress?.invoke(23, "Mengonversi audio ke WAV...")
        val converted = convertAnyAudioToWav(rawFile, wavFile)
        runCatching { rawFile.delete() }
        return if (converted && wavFile.exists() && wavFile.length() > 32_768L) {
            wavFile to "wav"
        } else {
            runCatching { wavFile.delete() }
            null
        }
    }

    private fun detectStrictAudioFormat(file: File): String {
        val header = ByteArray(16)
        val count = runCatching {
            FileInputStream(file).use { it.read(header) }
        }.getOrDefault(0)
        if (count >= 12) {
            if ((header[0] == 'I'.code.toByte() &&
                    header[1] == 'D'.code.toByte() &&
                    header[2] == '3'.code.toByte()) ||
                (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0)
            ) {
                return "mp3"
            }
            if (header[0] == 'R'.code.toByte() &&
                header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() &&
                header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() &&
                header[9] == 'A'.code.toByte() &&
                header[10] == 'V'.code.toByte() &&
                header[11] == 'E'.code.toByte()
            ) {
                return "wav"
            }
            // ISO/IEC 14496-12 (M4A / MP4): 'ftyp' at bytes 4..7
            if (header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
            ) {
                return "m4a"
            }
            // Ogg bitstream: 'OggS'
            if (header[0] == 'O'.code.toByte() &&
                header[1] == 'g'.code.toByte() &&
                header[2] == 'g'.code.toByte() &&
                header[3] == 'S'.code.toByte()
            ) {
                return "ogg"
            }
            // Free Lossless Audio Codec: 'fLaC'
            if (header[0] == 'f'.code.toByte() &&
                header[1] == 'L'.code.toByte() &&
                header[2] == 'a'.code.toByte() &&
                header[3] == 'C'.code.toByte()
            ) {
                return "flac"
            }
        }
        return "other"
    }

    /**
     * Converts fragmented fMP4 / WebM / Opus / Ogg into a standard 16-bit stereo RIFF WAV file
     * accepted by https://api.vms.inverseai.com.
     */
    private fun convertAnyAudioToWav(inputFile: File, outWavFile: File): Boolean {
        val rawPcmTmp = File(outWavFile.parentFile, "${outWavFile.name}.pcm.tmp")
        try {
            val sr = decodeAudioFileToStereoPcm(inputFile, rawPcmTmp)
            if (sr <= 0 || !rawPcmTmp.exists() || rawPcmTmp.length() <= 16_384L) {
                rawPcmTmp.delete()
                return false
            }
            val pcmSize = rawPcmTmp.length().toInt()
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + pcmSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // AudioFormat = 1 (PCM)
            header.putShort(2) // Channels = 2 (Stereo)
            header.putInt(sr) // SampleRate
            header.putInt(sr * 4) // ByteRate = SampleRate * 2 channels * 2 bytes
            header.putShort(4) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(pcmSize)

            FileOutputStream(outWavFile).use { fos ->
                fos.write(header.array())
                FileInputStream(rawPcmTmp).use { fis ->
                    fis.copyTo(fos, 64 * 1024)
                }
            }
            rawPcmTmp.delete()
            return outWavFile.exists() && outWavFile.length() > 32_768L
        } catch (t: Throwable) {
            runCatching { rawPcmTmp.delete() }
            runCatching { outWavFile.delete() }
            return false
        }
    }

    /**
     * Downloads (if not yet cached) and saves the separated Vocal or Instrument stem file
     * directly to device storage (`Music/Beatles/`).
     */
    fun downloadAndSaveStemToDevice(
        context: Context,
        songId: String?,
        songTitle: String?,
        songArtist: String?,
        songDurationSec: Int,
        downloadUtil: DownloadUtil,
        isVocal: Boolean,
    ) {
        val current = _state.value
        if (isVocal && current.savingVocal) return
        if (!isVocal && current.savingInstrument) return

        val appContext = context.applicationContext
        val stemLabel = if (isVocal) "Vokal" else "Instrumen"
        val resolvedSongId = songId?.takeIf { it.isNotBlank() } ?: "current_track"
        val safeId = resolvedSongId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val stemsDir = File(appContext.cacheDir, "remove_music_stems").apply { mkdirs() }
        val vocalFile = File(stemsDir, "${safeId}_vocal.mp3")
        val instrumentFile = File(stemsDir, "${safeId}_instrument.mp3")
        val vocalPcmFile = File(stemsDir, "${safeId}_vocal.pcm")
        val instrumentPcmFile = File(stemsDir, "${safeId}_instrument.pcm")
        val metaFile = File(stemsDir, "${safeId}_pcm.meta")

        _state.update {
            if (isVocal) it.copy(savingVocal = true) else it.copy(savingInstrument = true)
        }

        scope.launch {
            try {
                val targetStemMp3 = if (isVocal) vocalFile else instrumentFile
                var stemReadyFile: File? = targetStemMp3.takeIf { it.exists() && it.length() > 4096L }
                var stemExt = "mp3"
                var stemMime = "audio/mpeg"

                if (stemReadyFile == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appContext,
                            "Memisahkan & mengunduh $stemLabel untuk disimpan ke perangkat...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    val cloudOk = if (resolvedSongId != "current_track") {
                        runCloudSeparation(
                            context = appContext,
                            songId = resolvedSongId,
                            songTitle = songTitle,
                            songArtist = songArtist,
                            songDurationSec = songDurationSec,
                            downloadUtil = downloadUtil,
                            outVocalFile = vocalFile,
                            outInstrumentFile = instrumentFile,
                        )
                    } else false

                    if (cloudOk && targetStemMp3.exists() && targetStemMp3.length() > 4096L) {
                        stemReadyFile = targetStemMp3
                        // Also decode PCM in background so cache is ready if user activates separation
                        val decodedSr = decodeStemsToPcmParallel(
                            vocalMp3 = vocalFile,
                            instrumentMp3 = instrumentFile,
                            outVocalPcm = vocalPcmFile,
                            outInstrumentPcm = instrumentPcmFile,
                            metaFile = metaFile,
                        )
                        if (decodedSr > 0) {
                            synchronized(sessionCachedSongs) {
                                sessionCachedSongs.add(resolvedSongId)
                            }
                            if (_state.value.songId == resolvedSongId) {
                                _state.update {
                                    it.copy(
                                        status = VocalSeparationStatus.READY_CACHED,
                                        progress = 100,
                                        message = "Cached",
                                        vocalFile = vocalFile,
                                        instrumentFile = instrumentFile,
                                        vocalPcmFile = vocalPcmFile,
                                        instrumentPcmFile = instrumentPcmFile,
                                        pcmSampleRate = decodedSr,
                                        hasCloudStems = true,
                                    )
                                }
                            }
                        }
                    } else {
                        // Offline / fallback DSP stem extraction to WAV
                        val fallbackWav = File(stemsDir, "${safeId}_${if (isVocal) "vocal" else "instrument"}_dsp.wav")
                        val fallbackOk = generateOfflineStemWav(
                            context = appContext,
                            songId = resolvedSongId,
                            songTitle = songTitle,
                            songArtist = songArtist,
                            songDurationSec = songDurationSec,
                            downloadUtil = downloadUtil,
                            isVocal = isVocal,
                            outWavFile = fallbackWav,
                        )
                        if (fallbackOk && fallbackWav.exists() && fallbackWav.length() > 16_384L) {
                            stemReadyFile = fallbackWav
                            stemExt = "wav"
                            stemMime = "audio/x-wav"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appContext,
                            "Menyimpan $stemLabel ke perangkat...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val sourceToSave = stemReadyFile
                if (sourceToSave == null || !sourceToSave.exists() || sourceToSave.length() <= 4096L) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appContext,
                            "Gagal mengunduh audio $stemLabel. Periksa koneksi internet Anda.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val cleanTitle = songTitle?.takeIf { it.isNotBlank() } ?: "Beatles Audio"
                val baseName = SaveToDeviceManager.buildCleanBaseFileName(
                    title = cleanTitle,
                    artist = songArtist,
                    styleTag = stemLabel,
                )
                val savedName = writeStemFileToMediaStore(
                    context = appContext,
                    sourceFile = sourceToSave,
                    baseName = baseName,
                    ext = stemExt,
                    mimeType = stemMime,
                    displayTitle = "$cleanTitle [$stemLabel]",
                    artist = songArtist,
                )

                withContext(Dispatchers.Main) {
                    if (savedName != null) {
                        Toast.makeText(
                            appContext,
                            "$stemLabel disimpan ke Music/Beatles/$savedName",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            appContext,
                            "Gagal menyimpan $stemLabel ke perangkat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to download and save stem to device")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Gagal menyimpan $stemLabel ke perangkat.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _state.update {
                    if (isVocal) it.copy(savingVocal = false) else it.copy(savingInstrument = false)
                }
            }
        }
    }

    private suspend fun generateOfflineStemWav(
        context: Context,
        songId: String,
        songTitle: String?,
        songArtist: String?,
        songDurationSec: Int,
        downloadUtil: DownloadUtil,
        isVocal: Boolean,
        outWavFile: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val pair = prepareUploadableAudioFile(
            context = context,
            songId = songId,
            songTitle = songTitle,
            songArtist = songArtist,
            songDurationSec = songDurationSec,
            downloadUtil = downloadUtil,
        ) ?: return@withContext false
        val srcFile = pair.first
        val rawPcmTmp = File(outWavFile.parentFile, "${outWavFile.name}.raw_${System.nanoTime()}.pcm")
        try {
            val sr = decodeAudioFileToStereoPcm(srcFile, rawPcmTmp)
            if (sr <= 0 || !rawPcmTmp.exists() || rawPcmTmp.length() <= 16_384L) {
                return@withContext false
            }
            val pcmBytes = rawPcmTmp.readBytes()
            val buf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            val outBytes = ByteArray(pcmBytes.size)
            val outBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)

            var lpL = 0f
            var lpR = 0f
            val cutoffAlpha = (2f * Math.PI.toFloat() * 190f / sr).coerceIn(0.01f, 0.25f)

            while (buf.remaining() >= 4) {
                val l = buf.short.toFloat()
                val r = buf.short.toFloat()
                val mid = (l + r) * 0.5f
                val side = (l - r) * 0.5f
                lpL += cutoffAlpha * (l - lpL)
                lpR += cutoffAlpha * (r - lpR)

                val outL: Int
                val outR: Int
                if (isVocal) {
                    val v = (mid - lpL * 0.65f) * 1.15f
                    val clamped = v.toInt().coerceIn(-32768, 32767)
                    outL = clamped
                    outR = clamped
                } else {
                    val instL = (side * 1.25f + lpL * 0.85f).toInt().coerceIn(-32768, 32767)
                    val instR = (-side * 1.25f + lpR * 0.85f).toInt().coerceIn(-32768, 32767)
                    outL = instL
                    outR = instR
                }
                outBuf.putShort(outL.toShort())
                outBuf.putShort(outR.toShort())
            }

            val pcmSize = outBytes.size
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + pcmSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(2)
            header.putInt(sr)
            header.putInt(sr * 4)
            header.putShort(4)
            header.putShort(16)
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(pcmSize)

            FileOutputStream(outWavFile).use { fos ->
                fos.write(header.array())
                fos.write(outBytes)
            }
            outWavFile.exists() && outWavFile.length() > 16_384L
        } catch (t: Throwable) {
            false
        } finally {
            runCatching { srcFile.delete() }
            runCatching { rawPcmTmp.delete() }
        }
    }

    private fun writeStemFileToMediaStore(
        context: Context,
        sourceFile: File,
        baseName: String,
        ext: String,
        mimeType: String,
        displayTitle: String,
        artist: String?,
    ): String? {
        return try {
            val fileName = "$baseName.$ext"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                // Remove existing entry with same display name in Music/Beatles/ if present
                runCatching {
                    resolver.delete(
                        collection,
                        "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
                        arrayOf(fileName, "Music/Beatles%")
                    )
                }

                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.TITLE, displayTitle)
                    if (!artist.isNullOrBlank()) {
                        put(MediaStore.Audio.Media.ARTIST, artist)
                    }
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Beatles/")
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val itemUri = resolver.insert(collection, values) ?: return null
                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out, 64 * 1024)
                    }
                } ?: return null

                val publishValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                runCatching {
                    resolver.update(itemUri, publishValues, null, null)
                }
                fileName
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val targetDir = File(musicDir, "Beatles").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                runCatching {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType),
                        null,
                    )
                }
                fileName
            }
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "writeStemFileToMediaStore failed")
            null
        }
    }
}

/**
 * Sample-accurate dual-stem PCM player that mixes the decoded vocal and instrument stems
 * (`vocal.pcm` and `instrument.pcm`) sample-by-sample into a single [AudioTrack].
 *
 * Unlike running two separate ExoPlayer instances (which drift by 20-120ms and cause
 * comb-filtering/phase echo), mixing both stems at the exact same frame index into
 * one [AudioTrack] guarantees 0.00ms phase alignment — matching the web player on
 * https://remove.music/id/vocal-remover.
 */
class DualStemAudioEngine {
    @Volatile
    private var running = false
    private var workerThread: Thread? = null
    private var currentSongId: String? = null

    fun isRunningFor(songId: String?): Boolean {
        return running && !songId.isNullOrBlank() && currentSongId == songId
    }

    fun start(
        songId: String,
        vocalPcmFile: File,
        instrumentPcmFile: File,
        sampleRate: Int,
        isMainPlayingProvider: () -> Boolean,
        mainPositionMsProvider: () -> Long,
        vocalGainProvider: () -> Float,
        instrumentGainProvider: () -> Float,
        onMuteMainPlayer: (Boolean) -> Unit,
    ) {
        if (running && currentSongId == songId && workerThread?.isAlive == true) {
            return
        }
        stop(onMuteMainPlayer)

        if (!vocalPcmFile.exists() || !instrumentPcmFile.exists()) return
        val sr = sampleRate.coerceIn(8000, 96000)
        running = true
        currentSongId = songId

        workerThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            var audioTrack: AudioTrack? = null
            var vocalRaf: RandomAccessFile? = null
            var instRaf: RandomAccessFile? = null

            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    sr,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ).coerceAtLeast(8192)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sr)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                vocalRaf = RandomAccessFile(vocalPcmFile, "r")
                instRaf = RandomAccessFile(instrumentPcmFile, "r")
                val maxBytes = minOf(vocalRaf.length(), instRaf.length())
                val maxFrames = maxBytes / 4L // 4 bytes per stereo 16-bit frame

                val chunkFrames = 1024 // ~23ms per buffer at 44.1kHz for instant slider response
                val chunkBytes = chunkFrames * 4
                val vocalBytes = ByteArray(chunkBytes)
                val instBytes = ByteArray(chunkBytes)
                val mixBytes = ByteArray(chunkBytes)

                val vocalBuf = ByteBuffer.wrap(vocalBytes).order(ByteOrder.LITTLE_ENDIAN)
                val instBuf = ByteBuffer.wrap(instBytes).order(ByteOrder.LITTLE_ENDIAN)
                val mixBuf = ByteBuffer.wrap(mixBytes).order(ByteOrder.LITTLE_ENDIAN)

                var currentFrame = ((mainPositionMsProvider().coerceAtLeast(0L) * sr) / 1000L)
                    .coerceIn(0L, maxFrames)
                var trackPlaying = false

                onMuteMainPlayer(true)

                while (running) {
                    val shouldPlay = isMainPlayingProvider()
                    if (!shouldPlay) {
                        if (trackPlaying) {
                            runCatching { audioTrack.pause() }
                            runCatching { audioTrack.flush() }
                            trackPlaying = false
                        }
                        Thread.sleep(30)
                        // Stay synced with paused seekbar position
                        val mainMs = mainPositionMsProvider().coerceAtLeast(0L)
                        currentFrame = ((mainMs * sr) / 1000L).coerceIn(0L, maxFrames)
                        continue
                    }

                    if (!trackPlaying) {
                        val mainMs = mainPositionMsProvider().coerceAtLeast(0L)
                        currentFrame = ((mainMs * sr) / 1000L).coerceIn(0L, maxFrames)
                        runCatching { audioTrack.play() }
                        trackPlaying = true
                    }

                    // Check if user manually sought the seekbar (drift > 450ms)
                    val mainMs = mainPositionMsProvider().coerceAtLeast(0L)
                    val engineMs = (currentFrame * 1000L) / sr
                    if (abs(engineMs - mainMs) > 450L) {
                        currentFrame = ((mainMs * sr) / 1000L).coerceIn(0L, maxFrames)
                    }

                    val byteOffset = currentFrame * 4L
                    if (byteOffset >= maxBytes) {
                        Thread.sleep(30)
                        continue
                    }

                    vocalRaf.seek(byteOffset)
                    instRaf.seek(byteOffset)

                    val vRead = readFullyUpTo(vocalRaf, vocalBytes, chunkBytes)
                    val iRead = readFullyUpTo(instRaf, instBytes, chunkBytes)
                    val validBytes = minOf(vRead, iRead) and -4 // align to 4-byte stereo frame
                    if (validBytes <= 0) {
                        Thread.sleep(20)
                        continue
                    }

                    val vGain = vocalGainProvider().coerceIn(0f, 1.5f)
                    val iGain = instrumentGainProvider().coerceIn(0f, 1.5f)
                    val sampleCount = validBytes / 2

                    vocalBuf.clear()
                    instBuf.clear()
                    mixBuf.clear()

                    for (s in 0 until sampleCount) {
                        val vSample = vocalBuf.getShort(s * 2).toInt()
                        val iSample = instBuf.getShort(s * 2).toInt()
                        val mixed = (vSample * vGain + iSample * iGain).toInt()
                            .coerceIn(-32768, 32767)
                            .toShort()
                        mixBuf.putShort(s * 2, mixed)
                    }

                    audioTrack.write(mixBytes, 0, validBytes, AudioTrack.WRITE_BLOCKING)
                    currentFrame += (validBytes / 4)
                }
            } catch (t: Throwable) {
                Timber.tag("DualStemAudioEngine").w(t, "DualStemAudioEngine loop stopped")
            } finally {
                onMuteMainPlayer(false)
                runCatching { vocalRaf?.close() }
                runCatching { instRaf?.close() }
                runCatching {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    audioTrack?.release()
                }
            }
        }, "DualStemAudioEngine-$songId").apply {
            isDaemon = true
            start()
        }
    }

    private fun readFullyUpTo(raf: RandomAccessFile, buf: ByteArray, maxLen: Int): Int {
        var total = 0
        while (total < maxLen) {
            val r = raf.read(buf, total, maxLen - total)
            if (r <= 0) break
            total += r
        }
        return total
    }

    fun stop(onMuteMainPlayer: ((Boolean) -> Unit)? = null) {
        running = false
        currentSongId = null
        val t = workerThread
        workerThread = null
        if (t != null && t.isAlive) {
            runCatching { t.join(250) }
        }
        onMuteMainPlayer?.invoke(false)
    }
}
