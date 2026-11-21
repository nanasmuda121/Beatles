package com.nanzbeatles.nanas.eq.data

import kotlinx.serialization.Serializable

/**
 * Represents a single parametric EQ filter/band
 * Supports APO Parametric EQ filters
 */
@Serializable
data class ParametricEQBand(
    val frequency: Double,                      // Center frequency in Hz
    val gain: Double,                           // Gain in dB
    val q: Double = 1.41,                       // Q factor (bandwidth) - default to sqrt(2)
    val filterType: FilterType = FilterType.PK, // Filter type
    val enabled: Boolean = true                 // Whether this band is active
)

/**
 * Represents a complete parametric EQ configuration for a headphone or 10-band graphic EQ,
 * including optional dedicated low-frequency Bass Boost enhancement (in dB).
 */
@Serializable
data class ParametricEQ(
    val preamp: Double,                         // Preamp/gain in dB (to prevent clipping)
    val bands: List<ParametricEQBand>,          // List of EQ bands
    val bassBoostDb: Double = 0.0,              // Dedicated low-frequency Bass Boost in dB (0.0..12.0)
    val metadata: Map<String, String> = emptyMap()  // Additional metadata from file
) {
    companion object {
        const val MAX_BANDS = 20  // Maximum bands supported by the implementation
    }
}