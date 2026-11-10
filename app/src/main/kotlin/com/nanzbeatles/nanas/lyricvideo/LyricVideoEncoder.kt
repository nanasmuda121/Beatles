/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyricvideo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import com.nanzbeatles.nanas.constants.VideoLyricsCardStyle
import com.nanzbeatles.nanas.constants.VideoLyricsEffectStyle
import com.nanzbeatles.nanas.lyrics.LyricsEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

/**
 * High-performance on-device video encoder for NanzBeatles Lyric Videos.
 * Encodes 1280x720 H.264 video using format-agnostic YUV via Image API and
 * muxes the original local audio into a finalized MP4 file using Mp4ContainerWriter.
 */
object LyricVideoEncoder {

    private const val TAG = "LyricVideoEncoder"
    private const val MIME_TYPE_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC // video/avc
    private const val VIDEO_WIDTH = 1280
    private const val VIDEO_HEIGHT = 720
    private const val VIDEO_FPS = 30
    private const val VIDEO_BITRATE = 4_000_000 // 4 Mbps for crisp 720p HD
    private const val I_FRAME_INTERVAL = 1 // Keyframe every second
    private const val TIMEOUT_US = 10_000L

    data class EncodeConfig(
        val audioFile: File,
        val outputFile: File,
        val coverBitmap: Bitmap?,
        val lyrics: List<LyricsEntry>?,
        val songTitle: String,
        val songArtist: String,
        val startTimeMs: Long,
        val durationMs: Long,
        val amplitudes: FloatArray,
        val caseBitmap: Bitmap? = null,
        val cardStyle: VideoLyricsCardStyle = VideoLyricsCardStyle.KASET,
        val effectStyle: VideoLyricsEffectStyle = VideoLyricsEffectStyle.KINETIC_SHAKE,
        val lyricsScale: Float = 1.0f,
        val lyricsOffsetX: Float = 0f,
        val lyricsOffsetY: Float = 0f,
        val cardScale: Float = 1.0f,
        val cardOffsetX: Float = 0f,
        val cardOffsetY: Float = 0f,
        val cardAlpha: Float = 1.0f,
        val lyricsSpacingScale: Float = 1.0f,
        val showUpcomingLyrics: Boolean = true,
        val hideInactiveLyrics: Boolean = false,
        val lyricsRotation: Float = 0f,
        val cardRotation: Float = 0f,
        val jedagJedugEnabled: Boolean = true,
        val snowEnabled: Boolean = false,
        val waveAmplitudeScale: Float = 1.0f,
        val waveSpeedScale: Float = 1.0f,
        val audioSpeed: Float = 1.0f
    )

    /**
     * Executes the end-to-end rendering and encoding process.
     */
    suspend fun encodeLyricVideo(
        config: EncodeConfig,
        onProgress: (stage: String, progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        val muxSession = Mp4ContainerWriter.Mp4MuxSession(
            outputFile = config.outputFile,
            videoWidth = VIDEO_WIDTH,
            videoHeight = VIDEO_HEIGHT
        )

        try {
            val totalFrames = maxOf(1, ((config.durationMs / 1000.0) * VIDEO_FPS).toInt())
            val frameDurationUs = 1_000_000L / VIDEO_FPS

            // Setup Video Encoder
            val videoFormat = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            val encoder = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO)
            encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val artistHandle = if (config.songArtist.isNotBlank()) {
                val clean = config.songArtist.trim()
                if (clean.startsWith("@")) clean else "@$clean"
            } else {
                "@Beatles"
            }

            val renderer = LyricVideoRenderer(
                width = VIDEO_WIDTH,
                height = VIDEO_HEIGHT,
                caseBitmap = config.caseBitmap,
                brandText = "@BeatlesMusic",
                artistHandle = artistHandle,
                cardStyle = config.cardStyle,
                effectStyle = config.effectStyle,
                clipStartTimeMs = config.startTimeMs,
                lyricsScale = config.lyricsScale,
                lyricsOffsetX = config.lyricsOffsetX,
                lyricsOffsetY = config.lyricsOffsetY,
                cardScale = config.cardScale,
                cardOffsetX = config.cardOffsetX,
                cardOffsetY = config.cardOffsetY,
                cardAlpha = config.cardAlpha,
                lyricsSpacingScale = config.lyricsSpacingScale,
                showUpcomingLyrics = config.showUpcomingLyrics,
                hideInactiveLyrics = config.hideInactiveLyrics,
                lyricsRotation = config.lyricsRotation,
                cardRotation = config.cardRotation,
                jedagJedugEnabled = config.jedagJedugEnabled,
                snowEnabled = config.snowEnabled,
                waveAmplitudeScale = config.waveAmplitudeScale,
                waveSpeedScale = config.waveSpeedScale
            ).apply {
                setCoverBitmap(config.coverBitmap)
            }

            val frameBitmap = Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val argbPixels = IntArray(VIDEO_WIDTH * VIDEO_HEIGHT)

            var encoderOutputFormat: MediaFormat? = null
            var videoSamplesWritten = 0

            val bufferInfo = MediaCodec.BufferInfo()
            var currentFrame = 0
            var isInputEOS = false
            var isOutputEOS = false

            Timber.tag(TAG).d("Starting video encode loop for $totalFrames frames")

            while (!isOutputEOS && coroutineContext.isActive) {
                // Feed input frames
                if (!isInputEOS) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        if (currentFrame >= totalFrames) {
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                currentFrame * frameDurationUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputEOS = true
                        } else {
                            val presentationTimeUs = currentFrame * frameDurationUs
                            val elapsedVideoMs = currentFrame * 1000L / VIDEO_FPS
                            val frameTimeMs = config.startTimeMs + (elapsedVideoMs.toDouble() * config.audioSpeed.toDouble()).toLong()

                            // Lookup amplitude for this frame within the clipped audio segment
                            val amplitude = config.amplitudes.getOrElse(currentFrame) { 0.25f }

                            // Render frame to Bitmap
                            renderer.renderFrame(
                                frameCanvas,
                                frameTimeMs,
                                amplitude,
                                config.lyrics,
                                config.songTitle,
                                config.songArtist
                            )

                            // Copy Bitmap to Image YUV or InputBuffer NV12
                            val inputImage = try { encoder.getInputImage(inputBufferIndex) } catch (e: Exception) { null }
                            frameBitmap.getPixels(argbPixels, 0, VIDEO_WIDTH, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
                            if (inputImage != null) {
                                try {
                                    copyArgbToImageYuv(argbPixels, inputImage, VIDEO_WIDTH, VIDEO_HEIGHT)
                                } finally {
                                    try { inputImage.close() } catch (_: Exception) {}
                                }
                            } else {
                                val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                                if (inputBuffer != null) {
                                    copyArgbToNv12(argbPixels, inputBuffer, VIDEO_WIDTH, VIDEO_HEIGHT)
                                }
                            }

                            val frameBytesSize = (VIDEO_WIDTH * VIDEO_HEIGHT * 3) / 2
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                frameBytesSize,
                                presentationTimeUs,
                                0
                            )

                            currentFrame++
                            val encodeProgress = (currentFrame.toFloat() / totalFrames).coerceIn(0f, 1f)
                            onProgress("Merender video lirik...", 0.40f + 0.45f * encodeProgress)
                        }
                    }
                }

                // Drain output packets
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputFormat = encoder.outputFormat
                    muxSession.setVideoFormat(encoder.outputFormat)
                    Timber.tag(TAG).d("Video encoder output format received: $encoderOutputFormat")
                } else if (outputBufferIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (encodedData != null && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            val csdBytes = ByteArray(bufferInfo.size)
                            encodedData.get(csdBytes)
                            muxSession.feedVideoCodecConfig(csdBytes)
                        }
                        bufferInfo.size = 0
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                    }

                    if (encodedData != null && bufferInfo.size > 0) {
                        if (encoderOutputFormat == null) {
                            encoderOutputFormat = encoder.outputFormat
                            muxSession.setVideoFormat(encoder.outputFormat)
                        }
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        val packetBytes = ByteArray(bufferInfo.size)
                        encodedData.get(packetBytes)
                        val isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                        muxSession.writeVideoSample(packetBytes, bufferInfo.presentationTimeUs, isKeyFrame)
                        videoSamplesWritten++
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            try {
                encoder.stop()
                encoder.release()
            } catch (ignored: Exception) {}

            frameBitmap.recycle()

            if (videoSamplesWritten == 0) {
                error("Perangkat tidak dapat menghasilkan rekaman video. Silakan periksa izin atau coba lagi.")
            }

            onProgress("Menggabungkan audio & video...", 0.88f)

            // Step 4: Append Audio Track to Session and Finalize MP4 Container
            appendAudioTrackToSession(
                muxSession = muxSession,
                audioFile = config.audioFile,
                startTimeMs = config.startTimeMs,
                durationMs = config.durationMs
            )

            val finishedOk = muxSession.finish()
            if (!finishedOk || !config.outputFile.exists() || config.outputFile.length() < 1024L) {
                error("Gagal menulis kontainer MP4 video lirik.")
            }

            onProgress("Selesai!", 1.0f)
            Result.success(config.outputFile)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Encoding failed")
            try { muxSession.release() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    /**
     * Reads the trimmed audio packets from the local audio file and writes them to the Mp4MuxSession.
     */
    private fun appendAudioTrackToSession(
        muxSession: Mp4ContainerWriter.Mp4MuxSession,
        audioFile: File,
        startTimeMs: Long,
        durationMs: Long
    ) {
        if (!audioFile.exists() || audioFile.length() <= 0L) {
            Timber.tag(TAG).w("Audio file missing or empty, finalizing video without audio track")
            return
        }

        val audioExtractor = MediaExtractor()
        var audioSamplesWritten = 0

        try {
            audioExtractor.setDataSource(audioFile.absolutePath)
            val audioTrack = selectFirstTrack(audioExtractor, "audio/")
            if (audioTrack == -1) {
                Timber.tag(TAG).w("No audio track found in ${audioFile.name}")
                return
            }
            audioExtractor.selectTrack(audioTrack)
            val audioFormat = audioExtractor.getTrackFormat(audioTrack)
            muxSession.setAudioFormat(audioFormat)

            val audioTrackDurationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val isAudioPreTrimmed = audioFile.name.startsWith("clip_") ||
                (audioTrackDurationUs > 0L && audioTrackDurationUs <= (durationMs + 5_000L) * 1000L)
            var startUs = if (isAudioPreTrimmed) 0L else startTimeMs * 1000L
            val clipDurationUs = durationMs * 1000L

            if (!isAudioPreTrimmed && startUs > 0L) {
                audioExtractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                while (audioExtractor.sampleTime in 0 until startUs) {
                    if (audioExtractor.sampleTime >= startUs - 12_000L) {
                        break
                    }
                    if (!audioExtractor.advance()) break
                }
                if (audioExtractor.sampleTime < 0L || audioExtractor.sampleTime > startUs + clipDurationUs) {
                    audioExtractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    startUs = 0L
                }
            }

            val audioBuffer = ByteBuffer.allocateDirect(256 * 1024)
            var firstAudioSampleTimeUs = -1L

            while (true) {
                val sampleFlags = audioExtractor.sampleFlags
                if ((sampleFlags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!audioExtractor.advance()) break
                    continue
                }
                audioBuffer.clear()
                val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                val sampleTime = audioExtractor.sampleTime
                if (sampleSize < 0 || sampleTime < 0) break

                if (firstAudioSampleTimeUs == -1L) {
                    firstAudioSampleTimeUs = sampleTime
                }
                val relativePtsUs = maxOf(0L, sampleTime - firstAudioSampleTimeUs)
                if (relativePtsUs > clipDurationUs) break

                audioBuffer.position(0)
                audioBuffer.limit(sampleSize)
                val rawBytes = ByteArray(sampleSize)
                audioBuffer.get(rawBytes)
                muxSession.writeAudioSample(rawBytes, relativePtsUs)
                audioSamplesWritten++

                if (!audioExtractor.advance()) break
            }

            Timber.tag(TAG).d("Appended $audioSamplesWritten audio samples to MP4 session")
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to append audio track to MP4 session")
        } finally {
            try { audioExtractor.release() } catch (_: Exception) {}
        }
    }

    private fun selectFirstTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith(prefix)) return i
        }
        return -1
    }

    /**
     * Converts ARGB pixels to format-agnostic YUV Image planes (NV12 / I420).
     */
    private fun copyArgbToImageYuv(argb: IntArray, image: Image, width: Int, height: Int) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        // Fast Y plane fill
        for (y in 0 until height) {
            val yOffset = y * yRowStride
            val argbRowOffset = y * width
            for (x in 0 until width) {
                val c = argb[argbRowOffset + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuffer.put(yOffset + x, yVal.coerceIn(0, 255).toByte())
            }
        }

        // Subsampled Chroma (U and V)
        for (y in 0 until height / 2) {
            val uRowOffset = y * uRowStride
            val vRowOffset = y * vRowStride
            val argbRowOffset = (y * 2) * width
            for (x in 0 until width / 2) {
                val c = argb[argbRowOffset + (x * 2)]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                uBuffer.put(uRowOffset + (x * uPixelStride), uVal.coerceIn(0, 255).toByte())
                vBuffer.put(vRowOffset + (x * vPixelStride), vVal.coerceIn(0, 255).toByte())
            }
        }
    }

    /**
     * Converts ARGB pixels to standard NV12 ByteBuffer when Image API is not supported.
     */
    private fun copyArgbToNv12(argb: IntArray, buffer: ByteBuffer, width: Int, height: Int) {
        buffer.clear()
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val c = argb[rowOffset + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                buffer.put(yIndex++, yVal.coerceIn(0, 255).toByte())

                if (y % 2 == 0 && x % 2 == 0) {
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    buffer.put(uvIndex++, uVal.coerceIn(0, 255).toByte())
                    buffer.put(uvIndex++, vVal.coerceIn(0, 255).toByte())
                }
            }
        }
    }
}
