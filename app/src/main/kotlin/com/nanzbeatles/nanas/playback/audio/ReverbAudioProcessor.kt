package com.nanzbeatles.nanas.playback.audio

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import com.nanzbeatles.nanas.constants.AudioStyleMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Studio DSP Audio Processor for **Normal**, **Speed Up**, and **Slowed + Reverb**:
 * - **Normal**: Zero-overhead bit-exact pass-through once envelopes settle.
 * - **Speed Up**: True 4-point Hermite cubic spline analog varispeed (+20% speed & pitch).
 * - **Slowed + Reverb**: 4-point Hermite cubic spline analog varispeed (0.86x) + LFO-modulated
 *   canonical unity-gain Schroeder concert hall reverb with 32 ms pre-delay and 2-pole silk filter.
 */
@UnstableApi
class ReverbAudioProcessor : AudioProcessor {

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET

    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var workBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    @Volatile
    var enabled: Boolean = false

    @Volatile
    var styleMode: AudioStyleMode = AudioStyleMode.NORMAL

    @Volatile
    private var targetSpeed: Float = 1.0f

    private var currentSpeed: Float = 1.0f
    private var wetEnvelope: Float = 0.0f

    fun setAudioStyleMode(mode: AudioStyleMode) {
        styleMode = mode
        enabled = mode.reverbEnabled
        setTargetSpeed(mode.speed)
    }

    fun setTargetSpeed(speed: Float) {
        targetSpeed = speed.coerceIn(0.5f, 2.0f)
    }

    fun getMediaDuration(playoutDuration: Long): Long {
        val speed = targetSpeed.toDouble()
        if (abs(speed - 1.0) < 0.0005) return playoutDuration
        return (playoutDuration.toDouble() * speed).roundToLong()
    }

    // Mutually prime comb and all-pass tunings at 44100 Hz (eliminates periodic metallic ringing)
    private val baseInputDiffuserTunings = intArrayOf(149, 211)
    private val baseCombTunings = intArrayOf(1117, 1187, 1277, 1361, 1423, 1493, 1559, 1619)
    private val baseAllpassTunings = intArrayOf(557, 443, 347, 227)
    private val baseStereoSpread = 29

    // Lush, dark-velvet concert hall parameters
    private val roomFeedback = 0.82f
    private val hfDamp1 = 0.68f
    private val hfDamp2 = 1.0f - hfDamp1
    private val inputDiffuserFeedback = 0.45f
    private val allpassFeedback = 0.50f
    private val inputGain = 0.042f

    // Dry / Wet stereo cross-mix for Slowed + Reverb
    private val dryLevel = 0.88f
    private val wet1 = 0.26f
    private val wet2 = 0.08f

    // Filter coefficients (computed dynamically per sampleRate)
    private var inHpfCoeff = 0.027f
    private var inLpfCoeff = 0.28f
    private var outLpfCoeff = 0.34f
    private var outHpfCoeff = 0.012f
    private var dryWarmthCoeff = 0.82f

    // 4-point Hermite cubic varispeed resampler state across buffers
    private var fracPos: Double = 0.0
    private var resamplerPrimed = false
    private var xm1L = 0f
    private var x0L = 0f
    private var x1L = 0f
    private var x2L = 0f
    private var xm1R = 0f
    private var x0R = 0f
    private var x1R = 0f
    private var x2R = 0f

    // Pre-delay circular buffer (~32 ms)
    private var preDelayBufferL = FloatArray(0)
    private var preDelayBufferR = FloatArray(0)
    private var preDelayIdx = 0

    // Input & output filter states for Reverb
    private var dryWarmL = 0f
    private var dryWarmR = 0f
    private var inHpfStateL = 0f
    private var inHpfStateR = 0f
    private var inLpf1StateL = 0f
    private var inLpf1StateR = 0f
    private var inLpf2StateL = 0f
    private var inLpf2StateR = 0f
    private var outLpf1StateL = 0f
    private var outLpf1StateR = 0f
    private var outLpf2StateL = 0f
    private var outLpf2StateR = 0f
    private var outDcStateL = 0f
    private var outDcStateR = 0f

    // 2 Input Diffuser All-pass buffers per channel
    private var inDiffBuffersL = Array(2) { FloatArray(0) }
    private var inDiffBuffersR = Array(2) { FloatArray(0) }
    private val inDiffIndicesL = IntArray(2)
    private val inDiffIndicesR = IntArray(2)

    // 8 Comb filter buffers & states per channel
    private var combBuffersL = Array(8) { FloatArray(0) }
    private var combBuffersR = Array(8) { FloatArray(0) }
    private val combIndicesL = IntArray(8)
    private val combIndicesR = IntArray(8)
    private val combFilterStoreL = FloatArray(8)
    private val combFilterStoreR = FloatArray(8)

    // 4 Output All-pass diffuser buffers & base lengths per channel
    private var allpassBuffersL = Array(4) { FloatArray(0) }
    private var allpassBuffersR = Array(4) { FloatArray(0) }
    private val allpassBaseLenL = IntArray(4)
    private val allpassBaseLenR = IntArray(4)
    private val allpassIndicesL = IntArray(4)
    private val allpassIndicesR = IntArray(4)

    // Quadrature LFO state for chorus-modulated all-pass diffusion
    private var lfoPhaseL = 0.0
    private var lfoPhaseR = 1.57079632679 // +90 degrees quadrature offset
    private var lfoIncL = 0.0
    private var lfoIncR = 0.0
    private var lfoDepthSamples = 3.5f

    private var allocatedSampleRate = 0

    private fun allocateDelayLinesIfNeeded(sampleRate: Int) {
        if (sampleRate <= 0) return
        if (allocatedSampleRate == sampleRate && preDelayBufferL.isNotEmpty()) {
            return
        }
        allocatedSampleRate = sampleRate
        val sr = sampleRate.toFloat()
        val scale = sr / 44100f
        val spread = max(3, (baseStereoSpread * scale).roundToInt())

        // Compute sample-rate-accurate filter coefficients for Reverb
        val twoPiOverSr = (2.0 * Math.PI / sampleRate.toDouble()).toFloat()
        inHpfCoeff = (twoPiOverSr * 190f).coerceIn(0.005f, 0.15f)
        inLpfCoeff = (twoPiOverSr * 2550f).coerceIn(0.10f, 0.65f)
        outLpfCoeff = (twoPiOverSr * 3100f).coerceIn(0.12f, 0.70f)
        outHpfCoeff = (twoPiOverSr * 85f).coerceIn(0.002f, 0.08f)
        dryWarmthCoeff = (twoPiOverSr * 9500f).coerceIn(0.55f, 0.92f)

        // LFO modulation rates (~0.48 Hz Left, ~0.59 Hz Right)
        lfoIncL = (2.0 * Math.PI * 0.48) / sampleRate.toDouble()
        lfoIncR = (2.0 * Math.PI * 0.59) / sampleRate.toDouble()
        lfoDepthSamples = 3.8f * scale

        // 32 ms stereo pre-delay
        val preDelayLen = max(32, (0.032f * sampleRate).roundToInt())
        preDelayBufferL = FloatArray(preDelayLen)
        preDelayBufferR = FloatArray(preDelayLen)
        preDelayIdx = 0

        for (i in 0 until 2) {
            val lenL = max(32, (baseInputDiffuserTunings[i] * scale).roundToInt())
            val lenR = max(32, lenL + (spread / 2))
            inDiffBuffersL[i] = FloatArray(lenL)
            inDiffBuffersR[i] = FloatArray(lenR)
            inDiffIndicesL[i] = 0
            inDiffIndicesR[i] = 0
        }

        for (i in 0 until 8) {
            val lenL = max(64, (baseCombTunings[i] * scale).roundToInt())
            val lenR = max(64, lenL + spread)
            combBuffersL[i] = FloatArray(lenL)
            combBuffersR[i] = FloatArray(lenR)
            combIndicesL[i] = 0
            combIndicesR[i] = 0
            combFilterStoreL[i] = 0f
            combFilterStoreR[i] = 0f
        }

        val extraMargin = ceil(lfoDepthSamples + 4f).toInt()
        for (i in 0 until 4) {
            val baseL = max(32, (baseAllpassTunings[i] * scale).roundToInt())
            val baseR = max(32, baseL + spread)
            allpassBaseLenL[i] = baseL
            allpassBaseLenR[i] = baseR
            allpassBuffersL[i] = FloatArray(baseL + extraMargin)
            allpassBuffersR[i] = FloatArray(baseR + extraMargin)
            allpassIndicesL[i] = 0
            allpassIndicesR[i] = 0
        }

        clearDelayLines()
    }

    private fun clearDelayLines() {
        preDelayBufferL.fill(0f)
        preDelayBufferR.fill(0f)
        preDelayIdx = 0
        dryWarmL = 0f
        dryWarmR = 0f
        inHpfStateL = 0f
        inHpfStateR = 0f
        inLpf1StateL = 0f
        inLpf1StateR = 0f
        inLpf2StateL = 0f
        inLpf2StateR = 0f
        outLpf1StateL = 0f
        outLpf1StateR = 0f
        outLpf2StateL = 0f
        outLpf2StateR = 0f
        outDcStateL = 0f
        outDcStateR = 0f

        for (i in 0 until 2) {
            inDiffBuffersL[i].fill(0f)
            inDiffBuffersR[i].fill(0f)
            inDiffIndicesL[i] = 0
            inDiffIndicesR[i] = 0
        }
        for (i in 0 until 8) {
            combBuffersL[i].fill(0f)
            combBuffersR[i].fill(0f)
            combIndicesL[i] = 0
            combIndicesR[i] = 0
            combFilterStoreL[i] = 0f
            combFilterStoreR[i] = 0f
        }
        for (i in 0 until 4) {
            allpassBuffersL[i].fill(0f)
            allpassBuffersR[i].fill(0f)
            allpassIndicesL[i] = 0
            allpassIndicesR[i] = 0
        }

        fracPos = 0.0
        resamplerPrimed = false
        xm1L = 0f
        x0L = 0f
        x1L = 0f
        x2L = 0f
        xm1R = 0f
        x0R = 0f
        x1R = 0f
        x2R = 0f
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        if (inputAudioFormat.channelCount != 1 && inputAudioFormat.channelCount != 2) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        allocateDelayLinesIfNeeded(inputAudioFormat.sampleRate)
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioFormat.NOT_SET
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun softTapeSaturate(x: Float): Float {
        // Stereo-safe soft limiter above 0.90f so peaks never digital-clip
        val absX = abs(x)
        return if (absX <= 0.90f) {
            x
        } else {
            val excess = absX - 0.90f
            val compressed = 0.90f + (0.098f * (excess / (excess + 0.098f)))
            if (x >= 0f) compressed else -compressed
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun readModulatedAllpass(
        buf: FloatArray,
        writeIdx: Int,
        baseDelay: Int,
        modOffset: Float,
    ): Float {
        val bufSize = buf.size
        val totalDelay = (baseDelay.toFloat() + modOffset).coerceIn(4f, (bufSize - 2).toFloat())
        val delayInt = totalDelay.toInt()
        val frac = totalDelay - delayInt.toFloat()

        var readIdx0 = writeIdx - delayInt
        if (readIdx0 < 0) readIdx0 += bufSize
        var readIdx1 = readIdx0 - 1
        if (readIdx1 < 0) readIdx1 += bufSize

        val s0 = buf[readIdx0]
        val s1 = buf[readIdx1]
        return s0 + frac * (s1 - s0)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val remaining = inputBuffer.remaining()
        val mode = styleMode
        val targetWet = if (enabled || mode == AudioStyleMode.SLOWED_REVERB) 1.0f else 0.0f
        val tgtSpeed = targetSpeed

        // Pure zero-overhead pass-through when Normal mode is settled
        if (mode == AudioStyleMode.NORMAL &&
            !enabled &&
            wetEnvelope < 0.0002f &&
            abs(tgtSpeed - 1.0f) < 0.0005f &&
            abs(currentSpeed - 1.0f) < 0.0005f
        ) {
            wetEnvelope = 0f
            currentSpeed = 1.0f
            resamplerPrimed = false
            if (workBuffer.capacity() < remaining) {
                workBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            } else {
                workBuffer.clear()
            }
            workBuffer.put(inputBuffer)
            workBuffer.flip()
            outputBuffer = workBuffer
            return
        }

        allocateDelayLinesIfNeeded(inputAudioFormat.sampleRate)

        val channelCount = inputAudioFormat.channelCount
        val frameSize = 2 * channelCount
        val inFrameCount = remaining / frameSize
        val startPosition = inputBuffer.position()

        val minEffectiveSpeed = minOf(currentSpeed, tgtSpeed).coerceAtLeast(0.5f)
        val maxOutFrames = ceil(inFrameCount.toDouble() / minEffectiveSpeed.toDouble()).toInt() + 16
        val requiredCapacity = maxOutFrames * frameSize
        if (workBuffer.capacity() < requiredCapacity) {
            workBuffer = ByteBuffer.allocateDirect(requiredCapacity).order(ByteOrder.nativeOrder())
        } else {
            workBuffer.clear()
        }

        val srcOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val pdBufL = preDelayBufferL
        val pdBufR = preDelayBufferR
        val pdLen = pdBufL.size
        var pdIdx = preDelayIdx

        var dWarmL = dryWarmL
        var dWarmR = dryWarmR
        var hpfL = inHpfStateL
        var hpfR = inHpfStateR
        var lpf1L = inLpf1StateL
        var lpf1R = inLpf1StateR
        var lpf2L = inLpf2StateL
        var lpf2R = inLpf2StateR
        var oLpf1L = outLpf1StateL
        var oLpf1R = outLpf1StateR
        var oLpf2L = outLpf2StateL
        var oLpf2R = outLpf2StateR
        var oDcL = outDcStateL
        var oDcR = outDcStateR

        var curWet = wetEnvelope
        var curSpeed = currentSpeed
        var phaseL = lfoPhaseL
        var phaseR = lfoPhaseR

        val invShort = 1.0f / 32768.0f
        val twoPi = 2.0 * Math.PI

        for (frame in 0 until inFrameCount) {
            val baseOffset = startPosition + frame * frameSize
            val inSampleL = inputBuffer.getShort(baseOffset).toFloat() * invShort
            val inSampleR = if (channelCount == 2) {
                inputBuffer.getShort(baseOffset + 2).toFloat() * invShort
            } else {
                inSampleL
            }

            if (!resamplerPrimed) {
                xm1L = inSampleL
                x0L = inSampleL
                x1L = inSampleL
                x2L = inSampleL
                xm1R = inSampleR
                x0R = inSampleR
                x1R = inSampleR
                x2R = inSampleR
                fracPos = 0.0
                resamplerPrimed = true
            } else {
                xm1L = x0L
                x0L = x1L
                x1L = x2L
                x2L = inSampleL

                xm1R = x0R
                x0R = x1R
                x1R = x2R
                x2R = inSampleR
            }

            // 4-point, 3rd-order Hermite cubic spline coefficients for [x0, x1]
            val c0L = x0L
            val c1L = 0.5f * (x1L - xm1L)
            val c2L = xm1L - 2.5f * x0L + 2.0f * x1L - 0.5f * x2L
            val c3L = 0.5f * (x2L - xm1L) + 1.5f * (x0L - x1L)

            val c0R = x0R
            val c1R = 0.5f * (x1R - xm1R)
            val c2R = xm1R - 2.5f * x0R + 2.0f * x1R - 0.5f * x2R
            val c3R = 0.5f * (x2R - xm1R) + 1.5f * (x0R - x1R)

            while (fracPos < 1.0) {
                // Fast, click-free envelope glide (~35 ms transition)
                curSpeed += 0.00035f * (tgtSpeed - curSpeed)
                curWet += 0.00045f * (targetWet - curWet)

                val f = fracPos.toFloat()
                val rawDryL = ((c3L * f + c2L) * f + c1L) * f + c0L
                val rawDryR = ((c3R * f + c2R) * f + c1R) * f + c0R

                // Subtle analog silk filter on the resampled signal when slowed
                dWarmL += dryWarmthCoeff * (rawDryL - dWarmL)
                dWarmR += dryWarmthCoeff * (rawDryR - dWarmR)
                val dryL = if (curSpeed < 0.99f) (rawDryL * 0.65f + dWarmL * 0.35f) else rawDryL
                val dryR = if (curSpeed < 0.99f) (rawDryR * 0.65f + dWarmR * 0.35f) else rawDryR

                // =========================================================================
                // MODULATED CONCERT HALL REVERB (Slowed + Reverb)
                // =========================================================================
                var wetSignalL = 0f
                var wetSignalR = 0f

                if (curWet > 0.0001f) {
                    // 1. High-Pass Filter (~190 Hz) so kick/808 sub-bass never muddies the reverb tank
                    hpfL += inHpfCoeff * (dryL - hpfL)
                    hpfR += inHpfCoeff * (dryR - hpfR)
                    val hpL = dryL - hpfL
                    val hpR = dryR - hpfR

                    // 2. 2-Pole Cascaded Warm Low-Pass Filter (~2.55 kHz) for silky sibilance roll-off
                    lpf1L += inLpfCoeff * (hpL - lpf1L)
                    lpf2L += inLpfCoeff * (lpf1L - lpf2L)
                    lpf1R += inLpfCoeff * (hpR - lpf1R)
                    lpf2R += inLpfCoeff * (lpf1R - lpf2R)

                    // 3. Stereo Pre-Delay (~32 ms)
                    val delayedL = pdBufL[pdIdx]
                    val delayedR = pdBufR[pdIdx]
                    pdBufL[pdIdx] = lpf2L * inputGain
                    pdBufR[pdIdx] = lpf2R * inputGain
                    pdIdx++
                    if (pdIdx >= pdLen) pdIdx = 0

                    // Cross-blend stereo excitation for cohesive 3D hall space
                    var diffL = (delayedL * 0.78f) + (delayedR * 0.22f)
                    var diffR = (delayedR * 0.78f) + (delayedL * 0.22f)

                    // 4. 2-Stage Input Diffuser All-Pass (softens percussive attacks into velvet)
                    for (d in 0 until 2) {
                        val dBufL = inDiffBuffersL[d]
                        val dIdxL = inDiffIndicesL[d]
                        val dDelL = dBufL[dIdxL]
                        val vL = diffL - inputDiffuserFeedback * dDelL
                        diffL = dDelL + inputDiffuserFeedback * vL
                        dBufL[dIdxL] = vL
                        val nextDL = dIdxL + 1
                        inDiffIndicesL[d] = if (nextDL >= dBufL.size) 0 else nextDL

                        val dBufR = inDiffBuffersR[d]
                        val dIdxR = inDiffIndicesR[d]
                        val dDelR = dBufR[dIdxR]
                        val vR = diffR - inputDiffuserFeedback * dDelR
                        diffR = dDelR + inputDiffuserFeedback * vR
                        dBufR[dIdxR] = vR
                        val nextDR = dIdxR + 1
                        inDiffIndicesR[d] = if (nextDR >= dBufR.size) 0 else nextDR
                    }

                    var outL = 0f
                    var outR = 0f

                    // 5. 8 Parallel Mutually-Prime Low-Pass Damped Comb Filters per channel
                    for (c in 0 until 8) {
                        val bufL = combBuffersL[c]
                        val idxL = combIndicesL[c]
                        val readL = bufL[idxL]
                        val storeL = (readL * hfDamp2) + (combFilterStoreL[c] * hfDamp1)
                        combFilterStoreL[c] = storeL
                        bufL[idxL] = diffL + (storeL * roomFeedback)
                        val nextIdxL = idxL + 1
                        combIndicesL[c] = if (nextIdxL >= bufL.size) 0 else nextIdxL
                        outL += readL

                        val bufR = combBuffersR[c]
                        val idxR = combIndicesR[c]
                        val readR = bufR[idxR]
                        val storeR = (readR * hfDamp2) + (combFilterStoreR[c] * hfDamp1)
                        combFilterStoreR[c] = storeR
                        bufR[idxR] = diffR + (storeR * roomFeedback)
                        val nextIdxR = idxR + 1
                        combIndicesR[c] = if (nextIdxR >= bufR.size) 0 else nextIdxR
                        outR += readR
                    }

                    // Advance quadrature LFOs for modulated all-pass diffusion
                    phaseL += lfoIncL
                    if (phaseL >= twoPi) phaseL -= twoPi
                    phaseR += lfoIncR
                    if (phaseR >= twoPi) phaseR -= twoPi
                    val modL = sin(phaseL).toFloat() * lfoDepthSamples
                    val modR = sin(phaseR).toFloat() * lfoDepthSamples

                    // 6. 4 Canonical Unity-Gain All-Pass Diffusers per channel (first 2 LFO-modulated)
                    for (a in 0 until 4) {
                        val apBufL = allpassBuffersL[a]
                        val apIdxL = allpassIndicesL[a]
                        val delayedApL = if (a == 0) {
                            readModulatedAllpass(apBufL, apIdxL, allpassBaseLenL[a], modL)
                        } else if (a == 1) {
                            readModulatedAllpass(apBufL, apIdxL, allpassBaseLenL[a], -modR * 0.7f)
                        } else {
                            var rIdx = apIdxL - allpassBaseLenL[a]
                            if (rIdx < 0) rIdx += apBufL.size
                            apBufL[rIdx]
                        }
                        val vApL = outL - (allpassFeedback * delayedApL)
                        outL = delayedApL + (allpassFeedback * vApL)
                        apBufL[apIdxL] = vApL
                        val nextApIdxL = apIdxL + 1
                        allpassIndicesL[a] = if (nextApIdxL >= apBufL.size) 0 else nextApIdxL

                        val apBufR = allpassBuffersR[a]
                        val apIdxR = allpassIndicesR[a]
                        val delayedApR = if (a == 0) {
                            readModulatedAllpass(apBufR, apIdxR, allpassBaseLenR[a], modR)
                        } else if (a == 1) {
                            readModulatedAllpass(apBufR, apIdxR, allpassBaseLenR[a], -modL * 0.7f)
                        } else {
                            var rIdx = apIdxR - allpassBaseLenR[a]
                            if (rIdx < 0) rIdx += apBufR.size
                            apBufR[rIdx]
                        }
                        val vApR = outR - (allpassFeedback * delayedApR)
                        outR = delayedApR + (allpassFeedback * vApR)
                        apBufR[apIdxR] = vApR
                        val nextApIdxR = apIdxR + 1
                        allpassIndicesR[a] = if (nextApIdxR >= apBufR.size) 0 else nextApIdxR
                    }

                    // 7. Post-tank 2-pole warm low-pass filter + sub-sonic DC blocker
                    oLpf1L += outLpfCoeff * (outL - oLpf1L)
                    oLpf2L += outLpfCoeff * (oLpf1L - oLpf2L)
                    oLpf1R += outLpfCoeff * (outR - oLpf1R)
                    oLpf2R += outLpfCoeff * (oLpf1R - oLpf2R)

                    oDcL += outHpfCoeff * (oLpf2L - oDcL)
                    oDcR += outHpfCoeff * (oLpf2R - oDcR)
                    val cleanWetL = oLpf2L - oDcL
                    val cleanWetR = oLpf2R - oDcR

                    wetSignalL = ((cleanWetL * wet1) + (cleanWetR * wet2)) * curWet
                    wetSignalR = ((cleanWetR * wet1) + (cleanWetL * wet2)) * curWet
                }

                val effectiveDryGain = 1.0f - ((1.0f - dryLevel) * curWet)
                val mixedNormL = softTapeSaturate((dryL * effectiveDryGain) + wetSignalL)
                val pcmL = (mixedNormL * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
                workBuffer.putShort(pcmL)

                if (channelCount == 2) {
                    val mixedNormR = softTapeSaturate((dryR * effectiveDryGain) + wetSignalR)
                    val pcmR = (mixedNormR * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
                    workBuffer.putShort(pcmR)
                }

                fracPos += curSpeed.toDouble()
            }

            fracPos -= 1.0
        }

        preDelayIdx = pdIdx
        dryWarmL = dWarmL
        dryWarmR = dWarmR
        inHpfStateL = hpfL
        inHpfStateR = hpfR
        inLpf1StateL = lpf1L
        inLpf1StateR = lpf1R
        inLpf2StateL = lpf2L
        inLpf2StateR = lpf2R
        outLpf1StateL = oLpf1L
        outLpf1StateR = oLpf1R
        outLpf2StateL = oLpf2L
        outLpf2StateR = oLpf2R
        outDcStateL = oDcL
        outDcStateR = oDcR

        wetEnvelope = curWet
        currentSpeed = curSpeed
        lfoPhaseL = phaseL
        lfoPhaseR = phaseR

        inputBuffer.order(srcOrder)
        inputBuffer.position(inputBuffer.limit())
        workBuffer.flip()
        outputBuffer = workBuffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        currentSpeed = targetSpeed
        val mode = styleMode
        wetEnvelope = if (enabled || mode == AudioStyleMode.SLOWED_REVERB) 1.0f else 0.0f
        clearDelayLines()
    }

    override fun reset() {
        flush()
        workBuffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        allocatedSampleRate = 0
    }
}

/**
 * Custom [AudioProcessorChain] that places [ReverbAudioProcessor] at the very end of the pipeline
 * and routes pure varispeed modes (`speed == pitch`, such as "Slowed + Reverb" and "Speed Up")
 * through [ReverbAudioProcessor]'s 4-point Hermite cubic spline resampler instead of
 * [SonicAudioProcessor]'s WSOLA speech time-stretcher.
 */
@UnstableApi
class StudioAudioProcessorChain(
    vararg customProcessors: AudioProcessor,
    private val reverbProcessor: ReverbAudioProcessor,
    private val silenceSkippingAudioProcessor: SilenceSkippingAudioProcessor =
        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
    private val sonicAudioProcessor: SonicAudioProcessor = SonicAudioProcessor(),
) : AudioProcessorChain {

    private val processors: Array<AudioProcessor> = buildList {
        addAll(customProcessors)
        add(silenceSkippingAudioProcessor)
        add(sonicAudioProcessor)
        add(reverbProcessor)
    }.toTypedArray()

    override fun getAudioProcessors(): Array<AudioProcessor> = processors

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
        val speed = playbackParameters.speed
        val pitch = playbackParameters.pitch
        val isPureVarispeed = abs(speed - pitch) < 0.001f

        if (isPureVarispeed) {
            // Bypass WSOLA time-stretching; use smooth 4-point Hermite cubic varispeed in reverbProcessor
            sonicAudioProcessor.setSpeed(1.0f)
            sonicAudioProcessor.setPitch(1.0f)
            reverbProcessor.setTargetSpeed(speed)
        } else {
            reverbProcessor.setTargetSpeed(1.0f)
            sonicAudioProcessor.setSpeed(speed)
            sonicAudioProcessor.setPitch(pitch)
        }
        return playbackParameters
    }

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean {
        silenceSkippingAudioProcessor.setEnabled(skipSilenceEnabled)
        return skipSilenceEnabled
    }

    override fun getMediaDuration(playoutDuration: Long): Long {
        val afterVarispeed = reverbProcessor.getMediaDuration(playoutDuration)
        return if (sonicAudioProcessor.isActive()) {
            sonicAudioProcessor.getMediaDuration(afterVarispeed)
        } else {
            afterVarispeed
        }
    }

    override fun getSkippedOutputFrameCount(): Long {
        return silenceSkippingAudioProcessor.skippedFrames
    }
}
