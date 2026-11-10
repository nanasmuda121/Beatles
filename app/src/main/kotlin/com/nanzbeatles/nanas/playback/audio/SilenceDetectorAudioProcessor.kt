/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.playback.audio

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight PCM pass-through processor that detects long stretches of near-silence
 * and tracks real-time sub-bass (35–105 Hz) kick drum transients for zero-latency preview sync.
 */
@UnstableApi
@Suppress("DEPRECATION")
class SilenceDetectorAudioProcessor(
    private val minSilenceDurationUs: Long = 2_000_000L,
    private val silenceThreshold: Int = 256,
    private val onLongSilence: () -> Unit,
) : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    @Volatile
    var instantModeEnabled: Boolean = false

    @Volatile
    private var consecutiveSilentFrames: Long = 0

    @Volatile
    private var inSilence: Boolean = false

    private var notifiedThisSilence = false

    // Real-time sub-bass filter states (32 Hz HPF + 100 Hz 4th-order cascaded LPF)
    private var hpLpState = 0.0
    private var lp1 = 0.0
    private var lp2 = 0.0
    private var lp3 = 0.0
    private var lp4 = 0.0
    private var prevSubBassRms = 0f
    private var localBassAvg = 0.02f
    private var runningPeakBass = 0.06f
    private var kickHoldEnvelope = 0f
    private var lastKickTimeMs = 0L

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        resetBassFilterState()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            outputBuffer = EMPTY_BUFFER
            return
        }

        if (sampleRate > 0 && channelCount > 0) {
            analyzePcmBuffer(inputBuffer)
        } else {
            clearSilenceState()
        }

        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }

    private fun analyzePcmBuffer(inputBuffer: ByteBuffer) {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val frameCount = inputBuffer.remaining() / 2 / channelCount
        if (frameCount <= 0) return
        val basePosition = inputBuffer.position()

        val dt = 1.0 / sampleRate.toDouble()
        val hpAlpha = (dt / ((1.0 / (2.0 * Math.PI * 32.0)) + dt)).coerceIn(0.0005, 0.05)
        val lpAlpha = (dt / ((1.0 / (2.0 * Math.PI * 100.0)) + dt)).coerceIn(0.002, 0.20)

        var bassSumSq = 0.0
        var midSumSq = 0.0

        repeat(frameCount) { frameIndex ->
            var framePeak = 0
            var monoAccum = 0
            repeat(channelCount) { channelIndex ->
                val sampleIndex = basePosition + (frameIndex * channelCount + channelIndex) * 2
                val sampleShort = inputBuffer.getShort(sampleIndex).toInt()
                val sampleAbs = abs(sampleShort)
                if (sampleAbs > framePeak) {
                    framePeak = sampleAbs
                }
                monoAccum += sampleShort
            }

            // 1. Silence detection (when enabled)
            if (instantModeEnabled) {
                if (framePeak < silenceThreshold) {
                    consecutiveSilentFrames++
                    val silentDurationUs = (consecutiveSilentFrames * 1_000_000L) / sampleRate
                    if (silentDurationUs >= minSilenceDurationUs) {
                        inSilence = true
                        if (!notifiedThisSilence) {
                            notifiedThisSilence = true
                            onLongSilence()
                        }
                    }
                } else {
                    clearSilenceState()
                }
            } else if (inSilence || consecutiveSilentFrames != 0L) {
                clearSilenceState()
            }

            // 2. Real-time 32–100 Hz sub-bass isolation
            val mono = (monoAccum.toDouble() / channelCount) / 32768.0
            hpLpState += hpAlpha * (mono - hpLpState)
            val highPassed = mono - hpLpState

            lp1 += lpAlpha * (highPassed - lp1)
            lp2 += lpAlpha * (lp1 - lp2)
            lp3 += lpAlpha * (lp2 - lp3)
            lp4 += lpAlpha * (lp3 - lp4)

            val subBass = lp4
            val midHigh = highPassed - lp2
            bassSumSq += subBass * subBass
            midSumSq += midHigh * midHigh
        }

        val bassRms = sqrt(bassSumSq / frameCount).toFloat()
        val midRms = sqrt(midSumSq / frameCount).toFloat()

        // Adaptive peak ceiling with absolute physical floor (prevents quiet/vocal-only boosting)
        runningPeakBass = maxOf(0.048f, runningPeakBass * 0.996f, bassRms)
        val normBass = (bassRms / runningPeakBass).coerceIn(0f, 1f)

        val rise = (normBass - prevSubBassRms).coerceAtLeast(0f)
        val aboveAvg = (normBass - (localBassAvg * 1.15f + 0.045f)).coerceAtLeast(0f)
        prevSubBassRms = normBass
        localBassAvg = (localBassAvg * 0.86f) + (normBass * 0.14f)

        val nowMs = SystemClock.elapsedRealtime()
        val hasRealBassEnergy = bassRms >= 0.025f && normBass >= 0.28f && (bassRms >= midRms * 0.42f || normBass >= 0.52f)
        val isKickAttack = hasRealBassEnergy && rise >= 0.07f && aboveAvg > 0.02f && (nowMs - lastKickTimeMs >= 125L)

        if (isKickAttack) {
            val kickStrength = ((rise * 2.1f) + (aboveAvg * 1.6f) + (normBass * 0.35f)).coerceIn(0.25f, 1.0f)
            kickHoldEnvelope = maxOf(kickHoldEnvelope, kickStrength)
            lastKickTimeMs = nowMs
        } else {
            kickHoldEnvelope *= 0.62f
        }

        // Encode into two-zone contract:
        // [0.10..0.34] = smooth zoom envelope (no bass kick)
        // [0.45..1.00] = verified bass kick impact
        val baseZoomEnv = (0.12f + normBass * 0.22f).coerceIn(0.12f, 0.34f)
        realtimeBassAmplitude = if (kickHoldEnvelope > 0.16f) {
            (0.44f + kickHoldEnvelope * 0.56f).coerceIn(0.44f, 1.0f)
        } else {
            baseZoomEnv
        }
        lastRealtimeUpdateMs = nowMs
    }

    private fun resetBassFilterState() {
        hpLpState = 0.0
        lp1 = 0.0
        lp2 = 0.0
        lp3 = 0.0
        lp4 = 0.0
        prevSubBassRms = 0f
        localBassAvg = 0.02f
        runningPeakBass = 0.06f
        kickHoldEnvelope = 0f
    }

    private fun clearSilenceState() {
        consecutiveSilentFrames = 0
        inSilence = false
        notifiedThisSilence = false
    }

    fun resetTracking() {
        clearSilenceState()
        resetBassFilterState()
    }

    fun isCurrentlySilent(): Boolean = inSilence

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

    @Deprecated("Deprecated in AudioProcessor")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        clearSilenceState()
        resetBassFilterState()
    }

    @Deprecated("Deprecated in AudioProcessor")
    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

        @Volatile
        var realtimeBassAmplitude: Float = 0.18f
            private set

        @Volatile
        var lastRealtimeUpdateMs: Long = 0L
            private set
    }
}
