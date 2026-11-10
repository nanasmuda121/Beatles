/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyricvideo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import timber.log.Timber
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline PCM audio analyzer that extracts a per-frame sub-bass & kick transient envelope
 * for rendering 100% bass-locked animations in live previews and exported lyric videos.
 *
 * Output contract per frame [0.10f..1.0f]:
 * - [0.12f..0.34f]: Smooth musical envelope when NO bass kick is present (smooth zoom only, zero forced shake).
 * - [0.45f..1.00f]: Verified sub-bass / kick drum transient peak (triggers synchronized bass shake, zoom punch, glow & snow burst).
 */
object AudioAmplitudeAnalyzer {

    private const val TAG = "AudioAmplitudeAnalyzer"
    private const val TIMEOUT_US = 5000L

    // Physical sub-bass RMS ceiling floor so vocal-only / acoustic / quiet clips are never boosted into fake kicks
    private const val MIN_REAL_BASS_REF = 0.046f
    private const val MIN_KICK_BASS_RMS = 0.022f

    fun analyzeAmplitudes(
        audioFile: File,
        fps: Int = 30,
        totalDurationMs: Long = 0L,
        onProgress: ((Float) -> Unit)? = null
    ): FloatArray {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            Timber.tag(TAG).w("Audio file not available or empty, using smooth zoom-only fallback")
            return generateProceduralAmplitudes(fps, totalDurationMs)
        }

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(audioFile.absolutePath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            var mimeType = ""

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    mimeType = mime
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                Timber.tag(TAG).w("No audio track found in file, using smooth zoom-only fallback")
                return generateProceduralAmplitudes(fps, totalDurationMs)
            }

            extractor.selectTrack(audioTrackIndex)

            val trackDurationUs = when {
                totalDurationMs > 0L -> totalDurationMs * 1000L
                audioFormat.containsKey(MediaFormat.KEY_DURATION) -> audioFormat.getLong(MediaFormat.KEY_DURATION)
                else -> 30_000_000L
            }

            val estimatedTotalFrames = maxOf(1, ((trackDurationUs / 1_000_000.0) * fps).toInt() + 4)

            codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            var sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            } else 44100
            var channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            } else 2

            // Band-pass filter states:
            // 1) 32 Hz High-Pass filter removes DC offset and sub-audible rumble
            // 2) 4-stage cascaded 100 Hz Low-Pass filter (-24 dB/oct) isolates true kick drum & sub-bass (32–100 Hz)
            var hpLpState = 0.0
            var lp1 = 0.0
            var lp2 = 0.0
            var lp3 = 0.0
            var lp4 = 0.0

            val frameBassSqSum = DoubleArray(estimatedTotalFrames + 60)
            val frameBassPeak = FloatArray(estimatedTotalFrames + 60)
            val frameMidSqSum = DoubleArray(estimatedTotalFrames + 60)
            val frameSampleCounts = IntArray(estimatedTotalFrames + 60)
            var maxFrameReached = 0
            var firstPresentationTimeUs = -1L
            var fallbackSamplesProcessed = 0L

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false

            while (!isOutputEOS) {
                if (!isInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputEOS = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFmt = codec.outputFormat
                    if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
                    }
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                    }
                } else if (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                    }

                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        if (firstPresentationTimeUs < 0L && bufferInfo.presentationTimeUs >= 0L) {
                            firstPresentationTimeUs = bufferInfo.presentationTimeUs
                        }
                        val basePtsUs = if (bufferInfo.presentationTimeUs >= firstPresentationTimeUs && firstPresentationTimeUs >= 0L) {
                            bufferInfo.presentationTimeUs - firstPresentationTimeUs
                        } else {
                            (fallbackSamplesProcessed * 1_000_000L) / sampleRate
                        }

                        val dtSample = 1.0 / sampleRate.toDouble()
                        val hpAlpha = (dtSample / ((1.0 / (2.0 * Math.PI * 32.0)) + dtSample)).coerceIn(0.0005, 0.05)
                        val lpAlpha = (dtSample / ((1.0 / (2.0 * Math.PI * 100.0)) + dtSample)).coerceIn(0.002, 0.20)

                        var chIdx = 0
                        var monoAccum = 0.0
                        var monoSampleInBuf = 0L

                        while (outputBuffer.remaining() >= 2) {
                            val sample = outputBuffer.short.toDouble()
                            monoAccum += sample
                            chIdx++
                            if (chIdx >= channelCount) {
                                val mono = (monoAccum / channelCount) / 32768.0
                                chIdx = 0
                                monoAccum = 0.0

                                // 32 Hz High-Pass filter
                                hpLpState += hpAlpha * (mono - hpLpState)
                                val highPassed = mono - hpLpState

                                // 4-stage cascaded 100 Hz Low-Pass filter (isolates 32–100 Hz kick & sub-bass)
                                lp1 += lpAlpha * (highPassed - lp1)
                                lp2 += lpAlpha * (lp1 - lp2)
                                lp3 += lpAlpha * (lp2 - lp3)
                                lp4 += lpAlpha * (lp3 - lp4)

                                val subBass = lp4
                                val midHigh = highPassed - lp2

                                val samplePtsUs = basePtsUs + ((monoSampleInBuf * 1_000_000L) / sampleRate)
                                val frameIdx = ((samplePtsUs * fps) / 1_000_000L).toInt()
                                monoSampleInBuf++
                                fallbackSamplesProcessed++

                                if (frameIdx in frameBassSqSum.indices) {
                                    frameBassSqSum[frameIdx] += subBass * subBass
                                    frameMidSqSum[frameIdx] += midHigh * midHigh
                                    val absBass = abs(subBass).toFloat()
                                    if (absBass > frameBassPeak[frameIdx]) {
                                        frameBassPeak[frameIdx] = absBass
                                    }
                                    frameSampleCounts[frameIdx]++
                                    if (frameIdx > maxFrameReached) {
                                        maxFrameReached = frameIdx
                                        if (estimatedTotalFrames > 0 && maxFrameReached % 15 == 0) {
                                            onProgress?.invoke(
                                                (maxFrameReached.toFloat() / estimatedTotalFrames).coerceIn(0f, 1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            val totalFrames = maxOf(maxFrameReached + 1, 1)
            if (maxFrameReached <= 0 && frameSampleCounts[0] == 0) {
                return generateProceduralAmplitudes(fps, totalDurationMs)
            }

            val rawBassRms = FloatArray(totalFrames)
            val rawBassPeak = FloatArray(totalFrames)
            val rawMidRms = FloatArray(totalFrames)

            for (i in 0 until totalFrames) {
                val count = frameSampleCounts[i]
                if (count > 0) {
                    rawBassRms[i] = sqrt(frameBassSqSum[i] / count).toFloat()
                    rawBassPeak[i] = frameBassPeak[i]
                    rawMidRms[i] = sqrt(frameMidSqSum[i] / count).toFloat()
                } else if (i > 0) {
                    rawBassRms[i] = rawBassRms[i - 1] * 0.85f
                    rawBassPeak[i] = rawBassPeak[i - 1] * 0.85f
                    rawMidRms[i] = rawMidRms[i - 1] * 0.85f
                }
            }

            // 96th percentile reference with a strict physical minimum floor (MIN_REAL_BASS_REF).
            // If the song or clip has NO real bass (e.g., vocals/acoustic/intro), refMaxBass stays at MIN_REAL_BASS_REF
            // so weak vocal rumble is NEVER amplified into fake 1.0f bass kicks!
            val sortedBass = rawBassRms.sorted()
            val p96Index = ((sortedBass.size - 1) * 0.96).toInt().coerceIn(0, sortedBass.lastIndex)
            val refMaxBass = maxOf(sortedBass[p96Index], MIN_REAL_BASS_REF)

            val normBass = FloatArray(totalFrames) { i ->
                val rmsNorm = (rawBassRms[i] / refMaxBass).coerceIn(0f, 1.15f)
                val peakNorm = (rawBassPeak[i] / (refMaxBass * 1.65f)).coerceIn(0f, 1.15f)
                (rmsNorm * 0.68f + peakNorm * 0.32f).coerceIn(0f, 1f)
            }

            // Stage 1: Compute positive Sub-Bass Spectral Flux (Attack Rise) with Bass-to-Mid Dominance Gating
            val onsetFlux = FloatArray(totalFrames)
            for (i in 0 until totalFrames) {
                // Local background bass floor in preceding window [i-12 .. i-2] (~350ms before impact)
                var sumPrev = 0f
                var countPrev = 0
                for (k in maxOf(0, i - 12) until maxOf(0, i - 1)) {
                    sumPrev += normBass[k]
                    countPrev++
                }
                val localMean = if (countPrev > 0) sumPrev / countPrev else normBass[i]

                val prev1 = if (i >= 1) normBass[i - 1] else normBass[i]
                val prev2 = if (i >= 2) normBass[i - 2] else prev1
                val preValley = minOf(prev1, prev2)

                val stepRise = maxOf(0f, normBass[i] - preValley)
                val aboveLocal = maxOf(0f, normBass[i] - (localMean * 1.14f + 0.045f))

                // Bass-to-mid ratio gate: reject frames dominated by mid/high vocals when sub-bass is weak
                val bassRms = rawBassRms[i]
                val midRms = rawMidRms[i]
                val hasPhysicalBass = bassRms >= MIN_KICK_BASS_RMS && normBass[i] >= 0.28f
                val bassDominatesOrStrong = (bassRms >= midRms * 0.42f) || (normBass[i] >= 0.52f && bassRms >= MIN_KICK_BASS_RMS * 1.35f)

                if (hasPhysicalBass && bassDominatesOrStrong && stepRise >= 0.065f && aboveLocal > 0.015f) {
                    onsetFlux[i] = (stepRise * 1.55f) + (aboveLocal * 1.35f)
                } else {
                    onsetFlux[i] = 0f
                }
            }

            // Normalize positive flux with a physical minimum floor so tiny fluctuations never become full kicks
            val nonZeroFlux = onsetFlux.filter { it > 0.02f }.sorted()
            val refMaxFlux = if (nonZeroFlux.isNotEmpty()) {
                maxOf(nonZeroFlux[((nonZeroFlux.size - 1) * 0.92).toInt()], 0.16f)
            } else {
                1.0f
            }

            // Stage 2: Local Peak-Picking with a 6-frame (~200ms) Refractory Window
            // Guarantees each kick drum fires ONE crisp, 100% synchronized impulse at the exact frame of impact
            // without rapid sub-bass rumble triggering jittery back-to-back shakes.
            val pickedKicks = FloatArray(totalFrames)
            var lastKickFrame = -10
            for (i in 0 until totalFrames) {
                val fluxNorm = (onsetFlux[i] / refMaxFlux).coerceIn(0f, 1f)
                if (fluxNorm < 0.28f) continue

                val prevFlux = if (i > 0) onsetFlux[i - 1] else 0f
                val nextFlux = if (i + 1 < totalFrames) onsetFlux[i + 1] else 0f
                val isLocalPeak = onsetFlux[i] >= prevFlux && onsetFlux[i] >= nextFlux

                if (isLocalPeak && (i - lastKickFrame >= 6)) {
                    val kickPower = (fluxNorm * 0.72f + normBass[i] * 0.28f).coerceIn(0.30f, 1.0f)
                    pickedKicks[i] = kickPower
                    lastKickFrame = i
                }
            }

            // Stage 3: Build final per-frame output following the two-zone contract:
            // - No bass kick -> [0.12f .. 0.34f] (smooth zoom wave only, zero shake)
            // - Verified bass kick -> [0.45f .. 1.00f] (instant attack, cushioned ~100ms decay)
            var kickEnvelope = 0f
            val result = FloatArray(totalFrames) { i ->
                val hit = pickedKicks[i]
                kickEnvelope = if (hit > kickEnvelope) {
                    hit
                } else {
                    kickEnvelope * 0.54f
                }

                val smoothMusicalBody = (0.12f + normBass[i] * 0.22f).coerceIn(0.12f, 0.34f)
                if (kickEnvelope > 0.20f) {
                    (0.44f + kickEnvelope * 0.56f).coerceIn(0.44f, 1.0f)
                } else {
                    smoothMusicalBody
                }
            }

            val detectedKickCount = pickedKicks.count { it > 0f }
            Timber.tag(TAG).d("Analyzed $totalFrames frames: $detectedKickCount verified bass kicks detected")
            return result

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during offline PCM decoding, falling back to smooth zoom-only envelope")
            return generateProceduralAmplitudes(fps, totalDurationMs)
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Smooth zoom-only fallback when an audio stream cannot be decoded offline.
     * Stays strictly within [0.14f..0.28f] (< 0.42f shake threshold) so the video
     * performs a gentle zoom-in/zoom-out without forcing fake shakes.
     */
    fun generateProceduralAmplitudes(fps: Int, durationMs: Long): FloatArray {
        val count = maxOf(30, ((durationMs / 1000.0) * fps).toInt())
        return FloatArray(count) { frame ->
            val t = frame.toDouble() / fps
            val smoothZoomWave = 0.5 + 0.5 * sin(t * 2.2)
            (0.14 + 0.14 * smoothZoomWave).toFloat().coerceIn(0.14f, 0.28f)
        }
    }
}
