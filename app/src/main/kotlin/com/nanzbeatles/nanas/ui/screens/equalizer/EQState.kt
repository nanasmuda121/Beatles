package com.nanzbeatles.nanas.ui.screens.equalizer

import com.nanzbeatles.nanas.eq.data.BuiltInEQPresets
import com.nanzbeatles.nanas.eq.data.GraphicEQPreset
import com.nanzbeatles.nanas.eq.data.SavedEQProfile

/**
 * UI State for the 10-Band Graphic Equalizer, Bass Boost Enhancer & Custom EQ Screen
 */
data class EQState(
    val isEnabled: Boolean = false,
    val selectedPresetId: String = BuiltInEQPresets.FLAT.id,
    val bandGains: List<Float> = BuiltInEQPresets.FLAT.gainsDb,
    val preampDb: Float = 0f,
    val bassBoostPercent: Int = 0,
    val presets: List<GraphicEQPreset> = BuiltInEQPresets.ALL,
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val importStatus: String? = null,
    val error: String? = null
)