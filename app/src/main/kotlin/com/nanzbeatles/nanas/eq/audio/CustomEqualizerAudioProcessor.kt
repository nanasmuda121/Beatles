package com.nanzbeatles.nanas.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.nanzbeatles.nanas.eq.data.FilterType
import com.nanzbeatles.nanas.eq.data.ParametricEQ
import com.nanzbeatles.nanas.eq.data.ParametricEQBand
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow

/**
 * Media3 [AudioProcessor] implementation for the 10-band graphic equalizer, dedicated Bass Boost
 * low-frequency enhancer, and parametric EQ.
 * Processes 16-bit PCM audio in real time through a cascaded bank of [BiquadFilter] stages
 * with in-place coefficient updates and soft-knee headroom limiting.
 */
@UnstableApi
class CustomEqualizerAudioProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false

    @Volatile
    private var equalizerEnabled = false

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    @Volatile
    private var filters: List<BiquadFilter> = emptyList()

    // Dedicated 3-stage Bass Boost filter bank (85 Hz Low-Shelf + 60 Hz Sub-Punch Peak + 240 Hz Anti-Mud Dip)
    @Volatile
    private var bassBoostFilters: List<BiquadFilter> = emptyList()

    @Volatile
    private var bassBoostDb: Double = 0.0

    @Volatile
    private var preampGain: Double = 1.0
    private var pendingProfile: ParametricEQ? = null

    companion object {
        private const val TAG = "CustomEqualizerAudioProcessor"
        private const val BASS_SHELF_FREQ_HZ = 85.0
        private const val BASS_PUNCH_FREQ_HZ = 60.0
        private const val BASS_MUD_CLEAN_FREQ_HZ = 240.0
        private val EMPTY_BUFFER: ByteBuffer =
            ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    /**
     * Apply an EQ profile (10-band graphic preset + Bass Boost or custom parametric profile).
     * Updates existing biquad filters in-place when band topology matches so live slider
     * adjustments are completely click-free.
     */
    @Synchronized
    fun applyProfile(parametricEQ: ParametricEQ) {
        if (sampleRate == 0) {
            Timber.tag(TAG)
                .d("Audio processor not configured yet. Storing profile as pending with ${parametricEQ.bands.size} bands")
            pendingProfile = parametricEQ
            return
        }

        val clampedBassDb = parametricEQ.bassBoostDb.coerceIn(0.0, 15.0)
        bassBoostDb = clampedBassDb
        // Automatic -0.22 dB per +1 dB Bass Boost headroom compensation to keep lows tight and clean
        val effectivePreampDb = parametricEQ.preamp - (clampedBassDb * 0.22)
        preampGain = 10.0.pow(effectivePreampDb / 20.0)

        updateOrCreateFilters(parametricEQ.bands)
        updateOrCreateBassBoostFilters(clampedBassDb)
        equalizerEnabled = true

        Timber.tag(TAG)
            .d("Applied EQ profile with ${filters.size} bands, ${parametricEQ.preamp} dB preamp, and $clampedBassDb dB bass boost")
    }

    /**
     * Disable the equalizer (bit-exact passthrough)
     */
    @Synchronized
    fun disable() {
        equalizerEnabled = false
        bassBoostDb = 0.0
        preampGain = 1.0
        pendingProfile = null
        Timber.tag(TAG).d("Equalizer disabled")
    }

    /**
     * Check if equalizer is enabled
     */
    fun isEnabled(): Boolean = equalizerEnabled

    /**
     * Reuses existing [BiquadFilter] instances in-place if the active band count matches,
     * preserving filter state history for zero-glitch slider dragging and preset switching.
     */
    private fun updateOrCreateFilters(bands: List<ParametricEQBand>) {
        if (sampleRate == 0) return

        val nyquist = sampleRate / 2.0
        val activeBands = bands.filter { it.enabled && it.frequency < nyquist }
        val currentFilters = filters

        if (currentFilters.size == activeBands.size && currentFilters.isNotEmpty()) {
            for (i in activeBands.indices) {
                val band = activeBands[i]
                currentFilters[i].updateParameters(
                    frequency = band.frequency,
                    gain = band.gain,
                    q = band.q,
                    filterType = band.filterType
                )
            }
        } else {
            filters = activeBands.map { band ->
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = band.frequency,
                    gain = band.gain,
                    q = band.q,
                    filterType = band.filterType
                )
            }
        }
    }

    /**
     * Updates or creates the 3-stage dedicated Bass Boost filter topology in-place:
     * 1. 85 Hz Low-Shelf (`Q = 0.78`) for deep sub-bass extension (+75% of bassBoostDb)
     * 2. 60 Hz Peaking (`Q = 1.15`) for chest-thumping kick/808 punch (+55% of bassBoostDb)
     * 3. 240 Hz Peaking Dip (`Q = 1.41`) to prevent lower-mid mud (-15% of bassBoostDb)
     */
    private fun updateOrCreateBassBoostFilters(boostDb: Double) {
        if (sampleRate == 0) return
        val shelfGain = boostDb * 0.75
        val punchGain = boostDb * 0.55
        val mudCutGain = -boostDb * 0.15

        val currentBassFilters = bassBoostFilters
        if (currentBassFilters.size == 3) {
            currentBassFilters[0].updateParameters(
                frequency = BASS_SHELF_FREQ_HZ,
                gain = shelfGain,
                q = 0.78,
                filterType = FilterType.LSC
            )
            currentBassFilters[1].updateParameters(
                frequency = BASS_PUNCH_FREQ_HZ,
                gain = punchGain,
                q = 1.15,
                filterType = FilterType.PK
            )
            currentBassFilters[2].updateParameters(
                frequency = BASS_MUD_CLEAN_FREQ_HZ,
                gain = mudCutGain,
                q = 1.41,
                filterType = FilterType.PK
            )
        } else {
            bassBoostFilters = listOf(
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = BASS_SHELF_FREQ_HZ,
                    gain = shelfGain,
                    q = 0.78,
                    filterType = FilterType.LSC
                ),
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = BASS_PUNCH_FREQ_HZ,
                    gain = punchGain,
                    q = 1.15,
                    filterType = FilterType.PK
                ),
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = BASS_MUD_CLEAN_FREQ_HZ,
                    gain = mudCutGain,
                    q = 1.41,
                    filterType = FilterType.PK
                )
            )
        }
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        val sampleRateChanged = sampleRate != inputAudioFormat.sampleRate
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        if (sampleRateChanged) {
            filters = emptyList()
            bassBoostFilters = emptyList()
        }

        pendingProfile?.let { profile ->
            val clampedBassDb = profile.bassBoostDb.coerceIn(0.0, 15.0)
            bassBoostDb = clampedBassDb
            val effectivePreampDb = profile.preamp - (clampedBassDb * 0.22)
            preampGain = 10.0.pow(effectivePreampDb / 20.0)
            updateOrCreateFilters(profile.bands)
            updateOrCreateBassBoostFilters(clampedBassDb)
            equalizerEnabled = true
            pendingProfile = null
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    @Suppress("NOTHING_TO_INLINE")
    private inline fun softLimitSample(x: Double): Double {
        // Transparent below 0.90 (-0.9 dBFS); soft-knee limiting above 0.90 to prevent digital clipping on +12 dB boosts
        val absX = abs(x)
        return if (absX <= 0.90) {
            x
        } else {
            val excess = absX - 0.90
            val limited = 0.90 + (0.098 * (excess / (excess + 0.098)))
            if (x >= 0.0) limited else -limited
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val activeFilters = filters
        val activeBassFilters = if (bassBoostDb > 0.01) bassBoostFilters else emptyList()
        if (!equalizerEnabled || (activeFilters.isEmpty() && activeBassFilters.isEmpty())) {
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return

            if (outputBuffer.capacity() < remaining) {
                outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            } else {
                outputBuffer.clear()
            }
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return

        if (outputBuffer === EMPTY_BUFFER || outputBuffer === inputBuffer || outputBuffer.capacity() < inputSize) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        if (encoding == C.ENCODING_PCM_16BIT) {
            processAudioBuffer16Bit(inputBuffer, outputBuffer, activeFilters, activeBassFilters, preampGain)
        } else {
            outputBuffer.put(inputBuffer)
        }

        outputBuffer.flip()
    }

    private fun processAudioBuffer16Bit(
        input: ByteBuffer,
        output: ByteBuffer,
        activeFilters: List<BiquadFilter>,
        activeBassFilters: List<BiquadFilter>,
        gain: Double
    ) {
        val sampleCount = input.remaining() / 2
        val filterCount = activeFilters.size
        val bassFilterCount = activeBassFilters.size

        when (channelCount) {
            1 -> {
                repeat(sampleCount) {
                    var processed = input.getShort().toDouble() / 32768.0
                    for (i in 0 until filterCount) {
                        processed = activeFilters[i].processSample(processed)
                    }
                    for (i in 0 until bassFilterCount) {
                        processed = activeBassFilters[i].processSample(processed)
                    }
                    processed = softLimitSample(processed * gain)
                    val outputSample = (processed * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    output.putShort(outputSample)
                }
            }
            2 -> {
                repeat(sampleCount / 2) {
                    var left = input.getShort().toDouble() / 32768.0
                    var right = input.getShort().toDouble() / 32768.0

                    for (i in 0 until filterCount) {
                        val pair = activeFilters[i].processStereo(left, right)
                        left = pair.first
                        right = pair.second
                    }
                    for (i in 0 until bassFilterCount) {
                        val pair = activeBassFilters[i].processStereo(left, right)
                        left = pair.first
                        right = pair.second
                    }

                    left = softLimitSample(left * gain)
                    right = softLimitSample(right * gain)

                    val outL = (left * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    val outR = (right * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    output.putShort(outL)
                    output.putShort(outR)
                }
            }
            else -> {
                repeat(sampleCount) {
                    output.putShort(input.getShort())
                }
            }
        }
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer.remaining() == 0
    }

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        filters.forEach { it.reset() }
        bassBoostFilters.forEach { it.reset() }
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        inputBuffer = EMPTY_BUFFER
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        filters.forEach { it.reset() }
        bassBoostFilters.forEach { it.reset() }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
