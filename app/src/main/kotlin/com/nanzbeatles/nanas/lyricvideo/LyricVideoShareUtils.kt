/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyricvideo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nanzbeatles.nanas.R
import android.net.ConnectivityManager
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import com.nanzbeatles.innertube.FallbackAudioResolver
import java.nio.ByteBuffer
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.constants.AudioQuality
import com.nanzbeatles.nanas.constants.AudioQualityKey
import com.nanzbeatles.nanas.constants.AudioStyleMode
import com.nanzbeatles.nanas.constants.AudioStyleModeKey
import com.nanzbeatles.nanas.constants.VideoLyricsCardStyle
import com.nanzbeatles.nanas.constants.VideoLyricsCardStyleKey
import com.nanzbeatles.nanas.constants.VideoLyricsEffectStyle
import com.nanzbeatles.nanas.constants.VideoLyricsEffectStyleKey
import com.nanzbeatles.nanas.constants.VideoLyricsHideInactiveKey
import com.nanzbeatles.nanas.constants.VideoLyricsJedagJedugKey
import com.nanzbeatles.nanas.constants.VideoLyricsShowUpcomingKey
import com.nanzbeatles.nanas.constants.VideoLyricsSnowKey
import com.nanzbeatles.nanas.constants.VideoLyricsWaveAmplitudePercentKey
import com.nanzbeatles.nanas.constants.VideoLyricsWaveSpeedPercentKey
import com.nanzbeatles.nanas.lyrics.LyricsEntry
import com.nanzbeatles.nanas.models.MediaMetadata
import com.nanzbeatles.nanas.playback.audio.ReverbAudioProcessor
import androidx.media3.common.audio.AudioProcessor
import com.nanzbeatles.nanas.utils.ShareUtils
import com.nanzbeatles.nanas.utils.YTPlayerUtils
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.enumPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object LyricVideoShareUtils {

    private const val TAG = "LyricVideoShareUtils"
    private val clipMutex = Mutex()

    /**
     * Checks if the given local file is a valid, readable AAC audio file that MediaExtractor and MediaMuxer can handle.
     */
    fun isValidAudioFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4 * 1024L) return false
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var hasAacAudio = false
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) ||
                    mime.contains("mp4a", ignoreCase = true) ||
                    mime.startsWith("audio/mp4", ignoreCase = true)) {
                    hasAacAudio = true
                    break
                }
            }
            hasAacAudio
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Audio file validation failed for: ${file.absolutePath}")
            false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
        }
    }

    private fun extractClipFromExtractor(
        extractor: MediaExtractor,
        targetFile: File,
        startTimeMs: Long,
        durationMs: Long
    ): Boolean {
        try {
            var audioTrack = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrack == -1 || audioFormat == null) return false
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            if (!mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) &&
                !mime.contains("mp4a", ignoreCase = true) &&
                !mime.startsWith("audio/mp4", ignoreCase = true)
            ) {
                return false
            }

            extractor.selectTrack(audioTrack)
            targetFile.parentFile?.mkdirs()

            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            } else 44100
            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
            } else 2
            val csd0 = Mp4ContainerWriter.extractFormatCsd0(audioFormat)

            val startUs = startTimeMs * 1000L
            val targetDurationUs = durationMs * 1000L

            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                while (extractor.sampleTime in 0 until startUs) {
                    if (extractor.sampleTime >= startUs - 12_000L) {
                        break
                    }
                    extractor.advance()
                }
            }

            val buffer = ByteBuffer.allocateDirect(256 * 1024)
            val packets = ArrayList<Mp4ContainerWriter.AudioPacket>(1024)
            var firstSampleTimeUs = -1L
            var lastPtsUs = -1L

            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0L) break

                val sampleFlags = extractor.sampleFlags
                if ((sampleFlags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    extractor.advance()
                    continue
                }

                if (firstSampleTimeUs == -1L) {
                    firstSampleTimeUs = sampleTime
                }
                val relativePtsUs = (sampleTime - firstSampleTimeUs).coerceAtLeast(0L)
                if (relativePtsUs > targetDurationUs) break

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize <= 0) break

                val bytes = ByteArray(sampleSize)
                buffer.position(0)
                buffer.get(bytes, 0, sampleSize)

                var pts = relativePtsUs
                if (pts <= lastPtsUs) {
                    pts = lastPtsUs + ((1_000_000L * 1024L) / sampleRate)
                }
                lastPtsUs = pts

                packets.add(Mp4ContainerWriter.AudioPacket(bytes, pts))
                extractor.advance()
            }

            // Require at least 10 valid audio packets and reasonable duration
            val minRequiredDurationUs = minOf(targetDurationUs / 2L, 1_500_000L)
            if (packets.size < 10 || lastPtsUs < minRequiredDurationUs) {
                return false
            }

            return Mp4ContainerWriter.writeAudioOnlyM4a(
                outputFile = targetFile,
                sampleRate = sampleRate,
                channelCount = channelCount,
                csd0 = csd0,
                packets = packets
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "extractClipFromExtractor direct remux failed")
            return false
        }
    }

    /**
     * Transcodes any audio format (Opus, Vorbis, WebM, MP3, FLAC, AAC) to standard MP4 AAC
     * using MediaCodec decoder and encoder, optionally applying [AudioStyleMode]
     * (Speed Up or Slowed + Reverb) via [ReverbAudioProcessor].
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun transcodeAudioToAac(
        sourceFile: File,
        targetFile: File,
        startTimeMs: Long,
        durationMs: Long,
        audioStyleMode: AudioStyleMode = AudioStyleMode.NORMAL
    ): Boolean {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null

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

            val reverbProcessor = if (audioStyleMode != AudioStyleMode.NORMAL) {
                ReverbAudioProcessor().apply {
                    setAudioStyleMode(audioStyleMode)
                    configure(AudioProcessor.AudioFormat(sampleRate, channelCount, C.ENCODING_PCM_16BIT))
                    flush()
                }
            } else null

            val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 192000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            targetFile.parentFile?.mkdirs()
            var encoderOutputFormat: MediaFormat? = null
            var csd0Bytes: ByteArray? = null

            val inputDurationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            var startUs = if (inputDurationUs in 1L..(startTimeMs * 1000L)) {
                0L
            } else {
                startTimeMs * 1000L
            }
            val targetDurationUs = durationMs * 1000L
            var endUs = startUs + targetDurationUs

            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                // If seeking failed or jumped to EOF (common on concatenated/VBR MP3 without Xing TOC),
                // fall back to reading from 0L so we still reach the requested timestamp.
                if (extractor.sampleTime < 0L) {
                    extractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                }
            }

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
            val encodedPackets = ArrayList<Mp4ContainerWriter.AudioPacket>(1024)

            val timeoutUs = 5000L
            val startNs = System.nanoTime()
            var lastActivityNs = startNs

            while (!encoderDone) {
                val nowNs = System.nanoTime()
                if (nowNs - lastActivityNs > 20_000_000_000L) {
                    Timber.tag(TAG).e("transcodeAudioToAac watchdog: idle for > 20 seconds, aborting")
                    return false
                }
                if (nowNs - startNs > 180_000_000_000L) {
                    Timber.tag(TAG).e("transcodeAudioToAac watchdog: total time exceeded 180 seconds, aborting")
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
                            if (sampleSize < 0 || (sampleTime > endUs && sampleTime > 0)) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorDone = true
                            } else {
                                // Always pass 0 for flags (extractor.sampleFlags uses MediaExtractor constants, not MediaCodec)
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, maxOf(0L, sampleTime), 0)
                                extractor.advance()
                            }
                            lastActivityNs = System.nanoTime()
                        }
                    }
                }

                // 2. Drain decoder -> optional ReverbAudioProcessor -> chunked into AAC encoder
                val currentPending = pendingPcmBuffer
                if ((currentPending == null || !currentPending.hasRemaining()) && !decoderDone) {
                    val decOutIdx = decoder.dequeueOutputBuffer(decoderBufferInfo, timeoutUs)
                    if (decOutIdx >= 0) {
                        lastActivityNs = System.nanoTime()
                        val decBuf = decoder.getOutputBuffer(decOutIdx)
                        val isEos = (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        if (decBuf != null && decoderBufferInfo.size > 0 && decoderBufferInfo.presentationTimeUs >= startUs) {
                            decBuf.position(decoderBufferInfo.offset)
                            decBuf.limit(decoderBufferInfo.offset + decoderBufferInfo.size)
                            if (reverbProcessor != null) {
                                reverbProcessor.queueInput(decBuf)
                                pendingPcmBuffer = reverbProcessor.getOutput()
                            } else {
                                val copy = ByteBuffer.allocateDirect(decoderBufferInfo.size)
                                copy.put(decBuf)
                                copy.flip()
                                pendingPcmBuffer = copy
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

                // 3. Drain encoder into memory packet list
                val encOutIdx = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
                if (encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    lastActivityNs = System.nanoTime()
                    val fmt = encoder.outputFormat
                    encoderOutputFormat = fmt
                    if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
                    }
                    if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                    }
                    csd0Bytes = Mp4ContainerWriter.extractFormatCsd0(fmt) ?: csd0Bytes
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
                        val packetBytes = ByteArray(encoderBufferInfo.size)
                        encOutBuf.get(packetBytes)

                        var pts = maxOf(0L, encoderBufferInfo.presentationTimeUs)
                        if (pts <= lastAudioPtsUs) {
                            pts = lastAudioPtsUs + ((1_000_000L * 1024L) / sampleRate)
                        }
                        lastAudioPtsUs = pts
                        encodedPackets.add(Mp4ContainerWriter.AudioPacket(packetBytes, pts))
                    }
                    encoder.releaseOutputBuffer(encOutIdx, false)
                }
            }

            if (encodedPackets.size < 5) {
                return false
            }

            val finalCsd0 = csd0Bytes ?: encoderOutputFormat?.let { Mp4ContainerWriter.extractFormatCsd0(it) }
            return Mp4ContainerWriter.writeAudioOnlyM4a(
                outputFile = targetFile,
                sampleRate = sampleRate,
                channelCount = channelCount,
                csd0 = finalCsd0,
                packets = encodedPackets
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "transcodeAudioToAac failed")
            return false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (ignored: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (ignored: Exception) {}
        }
    }

    private fun extractClipFromFile(
        sourceFile: File,
        targetFile: File,
        startTimeMs: Long,
        durationMs: Long
    ): Boolean {
        // 1. Try direct remux if audio is already AAC (fastest)
        val extractor = MediaExtractor()
        val directOk = try {
            extractor.setDataSource(sourceFile.absolutePath)
            extractClipFromExtractor(extractor, targetFile, startTimeMs, durationMs)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "extractClipFromExtractor direct remux failed")
            false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
        }

        if (directOk && isValidAudioFile(targetFile)) {
            return true
        }

        // 2. Transcode to standard AAC via MediaCodec (supports Opus, WebM, MP3, etc.)
        Timber.tag(TAG).d("Falling back to MediaCodec transcode to AAC")
        return transcodeAudioToAac(sourceFile, targetFile, startTimeMs, durationMs)
    }

    private fun extractClipFromStream(
        context: Context,
        streamUrl: String,
        headers: Map<String, String>,
        targetFile: File,
        startTimeMs: Long,
        durationMs: Long
    ): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, Uri.parse(streamUrl), headers)
            extractClipFromExtractor(extractor, targetFile, startTimeMs, durationMs)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "extractClipFromStream failed")
            false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
        }
    }

    /**
     * Resolves strictly the selected audio range (startTimeMs to startTimeMs + durationMs)
     * and applies [audioStyleMode] (Normal, Speed Up, or Slowed + Reverb) if active.
     */
    suspend fun resolveAudioClip(
        context: Context,
        mediaId: String,
        startTimeMs: Long,
        durationMs: Long,
        downloadCache: SimpleCache? = null,
        playerCache: SimpleCache? = null,
        audioStyleMode: AudioStyleMode = AudioStyleMode.NORMAL,
        onProgress: ((String, Float) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        clipMutex.withLock {
            val audioDir = File(context.cacheDir, "temp_lyric_audio").apply { mkdirs() }
            val sourceDurationMs = (durationMs.toDouble() * audioStyleMode.speed.toDouble())
                .toLong()
                .coerceAtLeast(1000L)

            val styledClipFile = File(
                audioDir,
                "clip_${mediaId}_${startTimeMs}_${durationMs}_${audioStyleMode.name}.m4a"
            )
            if (styledClipFile.exists() && isValidAudioFile(styledClipFile)) {
                Timber.tag(TAG).d("Reusing existing valid styled audio clip: ${styledClipFile.absolutePath}")
                return@withLock styledClipFile
            }

            val rawClipFile = if (audioStyleMode == AudioStyleMode.NORMAL) {
                styledClipFile
            } else {
                File(audioDir, "clip_${mediaId}_${startTimeMs}_${sourceDurationMs}_RAW.m4a")
            }

            if (!rawClipFile.exists() || !isValidAudioFile(rawClipFile)) {
                onProgress?.invoke("Menyiapkan segmen audio...", 0.05f)

                var rawExtracted = false

                // 0. Check FallbackAudioResolver local disk cache first (complete file already downloaded)
                FallbackAudioResolver.getCachedStreamFile(mediaId)?.let { fallbackFile ->
                    onProgress?.invoke("Menyiapkan audio dari cache...", 0.10f)
                    if (extractClipFromFile(fallbackFile, rawClipFile, startTimeMs, sourceDurationMs) &&
                        isValidAudioFile(rawClipFile)
                    ) {
                        Timber.tag(TAG).d("Audio clip extracted from FallbackAudioResolver cache: ${rawClipFile.length()} bytes")
                        rawExtracted = true
                    }
                }

                // 1. Check playerCache and downloadCache (offline / in-memory cache)
                val activeCaches = listOfNotNull(downloadCache, playerCache)
                for (cache in activeCaches) {
                    if (rawExtracted) break
                    val matchingKey = cache.keys.firstOrNull { it == mediaId || it.contains(mediaId) }
                    if (matchingKey != null) {
                        val tempOffline = File(audioDir, "temp_cache_${mediaId}_${System.currentTimeMillis()}.tmp")
                        if (copyFromCache(cache, matchingKey, tempOffline)) {
                            if (tempOffline.exists() && tempOffline.length() > 4 * 1024L) {
                                onProgress?.invoke("Menyiapkan audio dari cache...", 0.10f)
                                val ok = extractClipFromFile(tempOffline, rawClipFile, startTimeMs, sourceDurationMs)
                                tempOffline.delete()
                                if (ok && isValidAudioFile(rawClipFile)) {
                                    Timber.tag(TAG).d("Audio clip extracted from cache: ${rawClipFile.length()} bytes")
                                    rawExtracted = true
                                    break
                                }
                            } else {
                                tempOffline.delete()
                            }
                        }
                    }
                }

                if (!rawExtracted) {
                    // 2. Stream online directly according to selected duration
                    val connectivityManager = context.getSystemService<ConnectivityManager>()
                        ?: error("ConnectivityManager tidak tersedia")

                    onProgress?.invoke("Menghubungkan ke stream audio...", 0.08f)
                    val playbackData = YTPlayerUtils.playerResponseForPlayback(
                        videoId = mediaId,
                        audioQuality = AudioQuality.HIGH,
                        connectivityManager = connectivityManager,
                        preferMp4Audio = true
                    ).getOrThrow()

                    // 3. Extract clip directly from stream without downloading full audio
                    val durSec = sourceDurationMs / 1000L
                    onProgress?.invoke("Mengunduh audio segmen ($durSec detik)...", 0.12f)
                    val streamSuccess = try {
                        extractClipFromStream(
                            context = context,
                            streamUrl = playbackData.streamUrl,
                            headers = playbackData.streamHeaders,
                            targetFile = rawClipFile,
                            startTimeMs = startTimeMs,
                            durationMs = sourceDurationMs
                        )
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Direct stream extraction failed, falling back to temp download")
                        false
                    }

                    if (!streamSuccess || !isValidAudioFile(rawClipFile)) {
                        // 4. Fallback: Download temp audio with generous timeout and streaming buffer
                        onProgress?.invoke("Mengunduh audio...", 0.14f)
                        val client = OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(180, TimeUnit.SECONDS)
                            .build()

                        val requestBuilder = Request.Builder().url(playbackData.streamUrl)
                        playbackData.streamHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                        val request = requestBuilder.build()
                        val tempDownloadFile = File(audioDir, "temp_dl_${mediaId}_${System.currentTimeMillis()}.tmp")

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) error("Gagal mengunduh audio: HTTP ${response.code}")
                            val body = response.body ?: error("Empty audio response body")
                            val totalBytes = body.contentLength()
                            var downloadedBytes = 0L
                            FileOutputStream(tempDownloadFile).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                val inStream = body.byteStream()
                                var read: Int
                                while (inStream.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    downloadedBytes += read
                                    if (totalBytes > 0) {
                                        val prog = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                                        onProgress?.invoke("Mengunduh audio (${(prog * 100).toInt()}%)...", 0.14f + 0.08f * prog)
                                    }
                                }
                            }
                        }

                        val extractSuccess = extractClipFromFile(tempDownloadFile, rawClipFile, startTimeMs, sourceDurationMs)
                        tempDownloadFile.delete()

                        if (!extractSuccess || !isValidAudioFile(rawClipFile)) {
                            rawClipFile.delete()
                            error("Gagal mengekstrak segmen audio yang dipilih")
                        }
                    }
                }
            }

            if (audioStyleMode == AudioStyleMode.NORMAL) {
                return@withLock rawClipFile
            }

            onProgress?.invoke("Menerapkan efek ${audioStyleMode.label}...", 0.18f)
            val styleApplied = transcodeAudioToAac(
                sourceFile = rawClipFile,
                targetFile = styledClipFile,
                startTimeMs = 0L,
                durationMs = sourceDurationMs + 2000L,
                audioStyleMode = audioStyleMode
            )
            if (styleApplied && isValidAudioFile(styledClipFile)) {
                Timber.tag(TAG).d("Styled audio clip (${audioStyleMode.label}) ready: ${styledClipFile.length()} bytes")
                return@withLock styledClipFile
            }

            Timber.tag(TAG).w("Styled transcode fallback to raw clip for ${audioStyleMode.label}")
            rawClipFile
        }
    }

    private fun copyFromCache(cache: SimpleCache, key: String, destFile: File): Boolean {
        return try {
            val dataSource = CacheDataSource.Factory()
                .setCache(cache)
                .createDataSource()

            val dataSpec = DataSpec.Builder().setKey(key).build()
            val totalBytes = dataSource.open(dataSpec)

            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                while (dataSource.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            dataSource.close()
            destFile.exists() && destFile.length() > 0
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to copy audio from cache")
            destFile.delete()
            false
        }
    }

    /**
     * Loads cover art bitmap via Coil.
     */
    suspend fun loadCoverBitmap(context: Context, thumbnailUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isNullOrBlank()) return@withContext null
        try {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap()
            } else null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not load cover bitmap")
            null
        }
    }

    /**
     * Generates a complete lyric video file, automatically applying the active [AudioStyleMode]
     * (Normal, Speed Up, or Slowed + Reverb).
     */
    suspend fun generateLyricVideo(
        context: Context,
        mediaMetadata: MediaMetadata,
        lyrics: List<LyricsEntry>?,
        startTimeMs: Long,
        durationMs: Long,
        cardStyle: VideoLyricsCardStyle? = null,
        effectStyle: VideoLyricsEffectStyle? = null,
        jedagJedugEnabled: Boolean? = null,
        snowEnabled: Boolean? = null,
        waveAmplitudeScale: Float? = null,
        waveSpeedScale: Float? = null,
        audioStyleMode: AudioStyleMode? = null,
        downloadCache: SimpleCache? = null,
        playerCache: SimpleCache? = null,
        onProgress: (stage: String, progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        try {
            val prefs = context.dataStore.data.first()
            val effectiveAudioStyle = audioStyleMode ?: prefs[AudioStyleModeKey]?.let {
                runCatching { AudioStyleMode.valueOf(it) }.getOrNull()
            } ?: AudioStyleMode.NORMAL

            onProgress("Menyiapkan segmen audio (${effectiveAudioStyle.label})...", 0.05f)
            val audioFile = resolveAudioClip(
                context = context,
                mediaId = mediaMetadata.id,
                startTimeMs = startTimeMs,
                durationMs = durationMs,
                downloadCache = downloadCache,
                playerCache = playerCache,
                audioStyleMode = effectiveAudioStyle,
                onProgress = onProgress
            )

            onProgress("Menganalisis amplitudo musik...", 0.22f)
            val amplitudes = AudioAmplitudeAnalyzer.analyzeAmplitudes(
                audioFile = audioFile,
                fps = 30,
                totalDurationMs = durationMs
            ) { progress ->
                onProgress("Menganalisis amplitudo musik...", 0.22f + 0.15f * progress)
            }

            onProgress("Memuat gambar cover...", 0.38f)
            val coverBitmap = loadCoverBitmap(context, mediaMetadata.thumbnailUrl)
            val caseBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.lyric_video_cd_case)
            } catch (e: Exception) {
                null
            }

            val outputDir = File(context.cacheDir, "lyric_videos").apply { mkdirs() }
            val outputFile = File(outputDir, "Beatles_${mediaMetadata.id}_${System.currentTimeMillis()}.mp4")

            val styleStr = prefs[VideoLyricsCardStyleKey] ?: VideoLyricsCardStyle.KASET.name
            val effectiveCardStyle = cardStyle ?: try {
                VideoLyricsCardStyle.valueOf(styleStr)
            } catch (e: Exception) {
                VideoLyricsCardStyle.KASET
            }
            val effectStr = prefs[VideoLyricsEffectStyleKey] ?: VideoLyricsEffectStyle.KINETIC_SHAKE.name
            val effectiveEffectStyle = effectStyle ?: try {
                VideoLyricsEffectStyle.valueOf(effectStr)
            } catch (e: Exception) {
                VideoLyricsEffectStyle.KINETIC_SHAKE
            }
            val showUpcoming = prefs[VideoLyricsShowUpcomingKey] ?: true
            val hideInactive = prefs[VideoLyricsHideInactiveKey] ?: (!showUpcoming)
            val effectiveJedagJedug = jedagJedugEnabled ?: (prefs[VideoLyricsJedagJedugKey] ?: true)
            val effectiveSnow = snowEnabled ?: (prefs[VideoLyricsSnowKey] ?: false)
            val effectiveWaveAmp = waveAmplitudeScale ?: ((prefs[VideoLyricsWaveAmplitudePercentKey] ?: 100) / 100f)
            val effectiveWaveSpeed = waveSpeedScale ?: ((prefs[VideoLyricsWaveSpeedPercentKey] ?: 100) / 100f)

            val config = LyricVideoEncoder.EncodeConfig(
                audioFile = audioFile,
                outputFile = outputFile,
                coverBitmap = coverBitmap,
                lyrics = lyrics,
                songTitle = mediaMetadata.title,
                songArtist = mediaMetadata.artists.joinToString { it.name },
                startTimeMs = startTimeMs,
                durationMs = durationMs,
                amplitudes = amplitudes,
                caseBitmap = caseBitmap,
                cardStyle = effectiveCardStyle,
                effectStyle = effectiveEffectStyle,
                lyricsScale = 1.0f,
                lyricsOffsetX = 0f,
                lyricsOffsetY = 0f,
                cardScale = 1.0f,
                cardOffsetX = 0f,
                cardOffsetY = 0f,
                cardAlpha = 1.0f,
                lyricsSpacingScale = 1.0f,
                showUpcomingLyrics = !hideInactive && showUpcoming,
                hideInactiveLyrics = hideInactive,
                lyricsRotation = 0f,
                cardRotation = 0f,
                jedagJedugEnabled = effectiveJedagJedug,
                snowEnabled = effectiveSnow,
                waveAmplitudeScale = effectiveWaveAmp,
                waveSpeedScale = effectiveWaveSpeed,
                audioSpeed = effectiveAudioStyle.speed
            )

            LyricVideoEncoder.encodeLyricVideo(config, onProgress)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to generate lyric video")
            Result.failure(e)
        }
    }

    /**
     * Shares the rendered lyric video MP4 file to a selected platform or system chooser.
     */
    fun shareLyricVideo(
        context: Context,
        videoFile: File,
        songTitle: String,
        songArtist: String,
        platform: ShareUtils.SharePlatform = ShareUtils.SharePlatform.GENERIC
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            videoFile
        )

        val shareText = "🎵 $songTitle - $songArtist\nVideo lirik dibuat dengan Beatles"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (platform.packageName != null) {
                setPackage(platform.packageName)
            }
        }

        try {
            if (platform == ShareUtils.SharePlatform.GENERIC) {
                context.startActivity(Intent.createChooser(intent, "Bagikan Video Lirik lewat"))
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback to generic chooser if specific app isn't installed
            if (platform != ShareUtils.SharePlatform.GENERIC) {
                shareLyricVideo(context, videoFile, songTitle, songArtist, ShareUtils.SharePlatform.GENERIC)
            }
        }
    }

    /**
     * Saves the generated lyric video MP4 directly to the device Gallery (Movies/NanzBeatles).
     */
    fun saveVideoToGallery(
        context: Context,
        videoFile: File,
        songTitle: String,
        songArtist: String
    ): Uri? {
        if (!videoFile.exists() || videoFile.length() == 0L) return null
        return try {
            val safeTitle = songTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
            val fileName = "Beatles_${safeTitle}_${System.currentTimeMillis()}.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Beatles")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { outStream ->
                    videoFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val moviesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Beatles"
                ).apply { mkdirs() }
                val targetFile = File(moviesDir, fileName)
                videoFile.copyTo(targetFile, overwrite = true)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save video to gallery")
            null
        }
    }

    /**
     * Uploads the generated lyric video MP4 to filebin.net (Primary #1 provider) with automatic fallbacks
     * (tmpfiles.org, litterbox.catbox.moe, and uguu.se) and returns the public temporary share URL.
     */
    suspend fun uploadVideoToTempFiles(videoFile: File): Result<String> = withContext(Dispatchers.IO) {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Berkas video tidak ditemukan"))
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        // 1. Primary (#1 Provider): filebin.net API (POST https://filebin.net/{bin}/{filename})
        val compactUploadName = "beatles_${System.currentTimeMillis() % 1_000_000L}.mp4"
        try {
            val randomSuffix = java.util.UUID.randomUUID().toString().replace("-", "").take(8).lowercase()
            val binId = "beatles$randomSuffix"
            val targetUrl = "https://filebin.net/$binId/$compactUploadName"
            val rawBody = videoFile.asRequestBody("application/octet-stream".toMediaType())

            val request = Request.Builder()
                .url(targetUrl)
                .header("Accept", "application/json")
                .header("cid", "beatles-${System.currentTimeMillis()}")
                .header("User-Agent", "Mozilla/5.0 (Android; BeatlesMusic)")
                .post(rawBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful || response.code == 201) {
                    val resolvedUrl = runCatching {
                        val json = JSONObject(bodyStr)
                        val returnedBin = json.optJSONObject("bin")?.optString("id").orEmpty().ifBlank { binId }
                        val returnedFile = json.optJSONObject("file")?.optString("filename").orEmpty().ifBlank { compactUploadName }
                        "https://filebin.net/$returnedBin/$returnedFile"
                    }.getOrDefault(targetUrl)
                    return@withContext Result.success(resolvedUrl)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "filebin.net upload failed, trying tmpfiles.org fallback")
        }

        val mediaType = "video/mp4".toMediaType()
        val fileBody = videoFile.asRequestBody(mediaType)

        // 2. Fallback 1: tmpfiles.org API
        try {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", videoFile.name, fileBody)
                .build()

            val request = Request.Builder()
                .url("https://tmpfiles.org/api/v1/upload")
                .header("User-Agent", "Mozilla/5.0 (Android; BeatlesMusic)")
                .post(multipart)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    val rawUrl = json.optJSONObject("data")?.optString("url").orEmpty()
                    if (rawUrl.isNotBlank()) {
                        val httpsUrl = rawUrl.replaceFirst("http://", "https://")
                        return@withContext Result.success(httpsUrl)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "tmpfiles.org upload failed, trying litterbox fallback")
        }

        // 3. Fallback 2: litterbox.catbox.moe (24h temp file host)
        try {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("time", "24h")
                .addFormDataPart("fileToUpload", videoFile.name, fileBody)
                .build()

            val request = Request.Builder()
                .url("https://litterbox.catbox.moe/resources/internals/api.php")
                .header("User-Agent", "Mozilla/5.0 (Android; BeatlesMusic)")
                .post(multipart)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()?.trim().orEmpty()
                if (response.isSuccessful && bodyStr.startsWith("https://")) {
                    return@withContext Result.success(bodyStr)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "litterbox upload failed, trying uguu.se fallback")
        }

        // 4. Fallback 3: uguu.se temp file host
        try {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("files[]", videoFile.name, fileBody)
                .build()

            val request = Request.Builder()
                .url("https://uguu.se/upload")
                .header("User-Agent", "Mozilla/5.0 (Android; BeatlesMusic)")
                .post(multipart)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    val filesArr = json.optJSONArray("files")
                    val url = filesArr?.optJSONObject(0)?.optString("url").orEmpty()
                    if (url.startsWith("http")) {
                        return@withContext Result.success(url)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "uguu.se upload failed")
        }

        Result.failure(Exception("Gagal mengunggah video ke filebin.net / Temp Files. Periksa koneksi internet Anda."))
    }
}
