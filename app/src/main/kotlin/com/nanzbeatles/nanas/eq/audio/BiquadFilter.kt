package com.nanzbeatles.nanas.eq.audio

import com.nanzbeatles.nanas.eq.data.FilterType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Biquad filter implementation for 10-band Graphic & Parametric EQ
 * Supports peaking (PK), low-shelf (LSC), and high-shelf (HSC) filters
 * Based on Robert Bristow-Johnson's Audio EQ Cookbook.
 * Supports glitch-free in-place coefficient updates without clearing filter history.
 */
class BiquadFilter(
    private val sampleRate: Int,
    frequency: Double,
    gain: Double,
    q: Double = 1.41,
    filterType: FilterType = FilterType.PK
) {
    @Volatile
    var currentFrequency: Double = frequency
        private set

    @Volatile
    var currentGain: Double = gain
        private set

    @Volatile
    var currentQ: Double = q
        private set

    @Volatile
    var currentFilterType: FilterType = filterType
        private set

    // Normalized filter coefficients
    @Volatile
    private var a1 = 0.0
    @Volatile
    private var a2 = 0.0
    @Volatile
    private var b0 = 1.0
    @Volatile
    private var b1 = 0.0
    @Volatile
    private var b2 = 0.0

    // State variables for filtering (per channel)
    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0

    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    init {
        calculateCoefficients(currentFrequency, currentGain, currentQ, currentFilterType)
    }

    /**
     * Update filter parameters in-place without resetting delay states (x1, x2, y1, y2)
     * to prevent clicks/pops while dragging 10-band EQ sliders in real time.
     */
    fun updateParameters(
        frequency: Double = currentFrequency,
        gain: Double = currentGain,
        q: Double = currentQ,
        filterType: FilterType = currentFilterType
    ) {
        if (frequency == currentFrequency &&
            gain == currentGain &&
            q == currentQ &&
            filterType == currentFilterType
        ) {
            return
        }
        currentFrequency = frequency
        currentGain = gain
        currentQ = q
        currentFilterType = filterType
        calculateCoefficients(frequency, gain, q, filterType)
    }

    /**
     * Calculate biquad filter coefficients based on filter type
     * Based on Robert Bristow-Johnson's Audio EQ Cookbook
     */
    private fun calculateCoefficients(
        frequency: Double,
        gain: Double,
        q: Double,
        filterType: FilterType
    ) {
        if (kotlin.math.abs(gain) < 0.001) {
            // Unity pass-through when gain is 0 dB
            b0 = 1.0
            b1 = 0.0
            b2 = 0.0
            a1 = 0.0
            a2 = 0.0
            return
        }
        when (filterType) {
            FilterType.PK -> calculatePeakingCoefficients(frequency, gain, q)
            FilterType.LSC -> calculateLowShelfCoefficients(frequency, gain)
            FilterType.HSC -> calculateHighShelfCoefficients(frequency, gain)
            else -> calculatePeakingCoefficients(frequency, gain, q)
        }
    }

    /**
     * Calculate peaking EQ coefficients (PK)
     * Boosts or cuts around a center frequency
     */
    private fun calculatePeakingCoefficients(frequency: Double, gain: Double, q: Double) {
        val A = 10.0.pow(gain / 40.0)
        val safeFreq = frequency.coerceIn(10.0, (sampleRate * 0.49).coerceAtLeast(20.0))
        val safeQ = q.coerceAtLeast(0.1)
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val alpha = sinOmega / (2.0 * safeQ)

        val rawB0 = 1.0 + alpha * A
        val rawB1 = -2.0 * cosOmega
        val rawB2 = 1.0 - alpha * A
        val rawA0 = 1.0 + alpha / A
        val rawA1 = -2.0 * cosOmega
        val rawA2 = 1.0 - alpha / A

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    /**
     * Calculate low-shelf coefficients (LSC)
     * Boosts or cuts frequencies below the cutoff frequency
     */
    private fun calculateLowShelfCoefficients(frequency: Double, gain: Double) {
        val A = 10.0.pow(gain / 40.0)
        val safeFreq = frequency.coerceIn(10.0, (sampleRate * 0.49).coerceAtLeast(20.0))
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val S = 1.0
        val alpha = sinOmega / 2.0 * sqrt((A + 1.0 / A) * (1.0 / S - 1.0) + 2.0)
        val sqrtA = sqrt(A)

        val aPlusOne = A + 1.0
        val aMinusOne = A - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        val rawB0 = A * (aPlusOne - aMinusOne * cosOmega + twoSqrtAAlpha)
        val rawB1 = 2.0 * A * (aMinusOne - aPlusOne * cosOmega)
        val rawB2 = A * (aPlusOne - aMinusOne * cosOmega - twoSqrtAAlpha)
        val rawA0 = aPlusOne + aMinusOne * cosOmega + twoSqrtAAlpha
        val rawA1 = -2.0 * (aMinusOne + aPlusOne * cosOmega)
        val rawA2 = aPlusOne + aMinusOne * cosOmega - twoSqrtAAlpha

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    /**
     * Calculate high-shelf coefficients (HSC)
     * Boosts or cuts frequencies above the cutoff frequency
     */
    private fun calculateHighShelfCoefficients(frequency: Double, gain: Double) {
        val A = 10.0.pow(gain / 40.0)
        val safeFreq = frequency.coerceIn(10.0, (sampleRate * 0.49).coerceAtLeast(20.0))
        val omega = 2.0 * PI * safeFreq / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val S = 1.0
        val alpha = sinOmega / 2.0 * sqrt((A + 1.0 / A) * (1.0 / S - 1.0) + 2.0)
        val sqrtA = sqrt(A)

        val aPlusOne = A + 1.0
        val aMinusOne = A - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        val rawB0 = A * (aPlusOne + aMinusOne * cosOmega + twoSqrtAAlpha)
        val rawB1 = -2.0 * A * (aMinusOne + aPlusOne * cosOmega)
        val rawB2 = A * (aPlusOne + aMinusOne * cosOmega - twoSqrtAAlpha)
        val rawA0 = aPlusOne - aMinusOne * cosOmega + twoSqrtAAlpha
        val rawA1 = 2.0 * (aMinusOne - aPlusOne * cosOmega)
        val rawA2 = aPlusOne - aMinusOne * cosOmega - twoSqrtAAlpha

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    /**
     * Process a single sample (mono)
     */
    fun processSample(input: Double): Double {
        val lb0 = b0
        val lb1 = b1
        val lb2 = b2
        val la1 = a1
        val la2 = a2

        val output = lb0 * input + lb1 * x1L + lb2 * x2L - la1 * y1L - la2 * y2L

        x2L = x1L
        x1L = input
        y2L = y1L
        y1L = output

        return output
    }

    /**
     * Process stereo samples (left and right channels)
     */
    fun processStereo(inputLeft: Double, inputRight: Double): Pair<Double, Double> {
        val lb0 = b0
        val lb1 = b1
        val lb2 = b2
        val la1 = a1
        val la2 = a2

        val outputLeft = lb0 * inputLeft + lb1 * x1L + lb2 * x2L - la1 * y1L - la2 * y2L
        x2L = x1L
        x1L = inputLeft
        y2L = y1L
        y1L = outputLeft

        val outputRight = lb0 * inputRight + lb1 * x1R + lb2 * x2R - la1 * y1R - la2 * y2R
        x2R = x1R
        x1R = inputRight
        y2R = y1R
        y1R = outputRight

        return Pair(outputLeft, outputRight)
    }

    /**
     * Reset filter state (clears history)
     */
    fun reset() {
        x1L = 0.0
        x2L = 0.0
        y1L = 0.0
        y2L = 0.0
        x1R = 0.0
        x2R = 0.0
        y1R = 0.0
        y2R = 0.0
    }
}