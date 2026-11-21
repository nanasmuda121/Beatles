package com.nanzbeatles.nanas.eq.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard 10-band ISO Graphic Equalizer center frequencies (Hz) and compact labels.
 */
object GraphicEQBands {
    val FREQUENCIES_HZ: List<Double> = listOf(
        31.0,
        62.0,
        125.0,
        250.0,
        500.0,
        1000.0,
        2000.0,
        4000.0,
        8000.0,
        16000.0
    )

    val LABELS: List<String> = listOf(
        "31",
        "62",
        "125",
        "250",
        "500",
        "1k",
        "2k",
        "4k",
        "8k",
        "16k"
    )

    const val MIN_GAIN_DB = -12f
    const val MAX_GAIN_DB = 12f
    const val MAX_BASS_BOOST_DB = 12f
    const val BAND_COUNT = 10

    fun bassBoostPercentToDb(percent: Int): Float {
        return (percent.coerceIn(0, 100) / 100f) * MAX_BASS_BOOST_DB
    }

    fun buildParametricBands(gainsDb: List<Float>): List<ParametricEQBand> {
        return FREQUENCIES_HZ.mapIndexed { index, freq ->
            val gain = gainsDb.getOrElse(index) { 0f }.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB).toDouble()
            val filterType = when (index) {
                0 -> FilterType.LSC
                FREQUENCIES_HZ.lastIndex -> FilterType.HSC
                else -> FilterType.PK
            }
            ParametricEQBand(
                frequency = freq,
                gain = gain,
                q = 1.41,
                filterType = filterType,
                enabled = true
            )
        }
    }
}

/**
 * Built-in 10-band Graphic Equalizer preset definition.
 */
data class GraphicEQPreset(
    val id: String,
    val name: String,
    val gainsDb: List<Float>,
    val preampDb: Float = 0f
)

object BuiltInEQPresets {
    const val CUSTOM_PRESET_ID = "preset_custom"

    val FLAT = GraphicEQPreset(
        id = "preset_flat",
        name = "Flat",
        gainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )
    val ROCK = GraphicEQPreset(
        id = "preset_rock",
        name = "Rock",
        gainsDb = listOf(5.0f, 4.0f, 3.0f, -1.5f, -2.0f, -0.5f, 2.5f, 4.0f, 5.0f, 5.5f)
    )
    val POP = GraphicEQPreset(
        id = "preset_pop",
        name = "Pop",
        gainsDb = listOf(-1.0f, 1.5f, 3.5f, 4.5f, 3.0f, 0.0f, -1.0f, 1.5f, 2.5f, 2.0f)
    )
    val JAZZ = GraphicEQPreset(
        id = "preset_jazz",
        name = "Jazz",
        gainsDb = listOf(3.5f, 2.5f, 1.5f, 2.0f, -1.5f, -1.5f, 0.0f, 1.5f, 3.0f, 4.0f)
    )
    val CLASSICAL = GraphicEQPreset(
        id = "preset_classical",
        name = "Classical",
        gainsDb = listOf(4.5f, 3.5f, 2.5f, 1.0f, -1.5f, -1.5f, 0.0f, 2.0f, 3.5f, 4.5f)
    )
    val HIP_HOP = GraphicEQPreset(
        id = "preset_hiphop",
        name = "Hip-Hop",
        gainsDb = listOf(5.5f, 5.0f, 3.0f, 1.0f, -1.0f, -1.0f, 1.5f, 1.0f, 2.5f, 3.5f)
    )
    val ELECTRONIC = GraphicEQPreset(
        id = "preset_electronic",
        name = "Electronic",
        gainsDb = listOf(5.0f, 4.5f, 1.5f, 0.0f, -2.0f, 1.5f, 1.0f, 2.5f, 4.5f, 5.0f)
    )
    val BASS_BOOST = GraphicEQPreset(
        id = "preset_bass_boost",
        name = "Bass Boost",
        gainsDb = listOf(6.5f, 5.5f, 4.0f, 1.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    )
    val VOCAL_BOOST = GraphicEQPreset(
        id = "preset_vocal_boost",
        name = "Vocal",
        gainsDb = listOf(-2.0f, -1.5f, -1.0f, 1.5f, 4.0f, 5.0f, 4.5f, 2.5f, 0.5f, -1.0f)
    )
    val ACOUSTIC = GraphicEQPreset(
        id = "preset_acoustic",
        name = "Acoustic",
        gainsDb = listOf(3.5f, 3.0f, 1.5f, 0.5f, 1.5f, 1.5f, 2.5f, 3.0f, 3.0f, 2.0f)
    )
    val RNB = GraphicEQPreset(
        id = "preset_rnb",
        name = "R&B",
        gainsDb = listOf(3.5f, 6.0f, 4.5f, 1.0f, -2.0f, -1.0f, 2.0f, 2.5f, 3.0f, 3.5f)
    )
    val TREBLE_BOOST = GraphicEQPreset(
        id = "preset_treble_boost",
        name = "Treble",
        gainsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 2.5f, 4.5f, 5.5f, 6.5f)
    )

    val ALL: List<GraphicEQPreset> = listOf(
        FLAT,
        ROCK,
        POP,
        JAZZ,
        CLASSICAL,
        HIP_HOP,
        ELECTRONIC,
        BASS_BOOST,
        VOCAL_BOOST,
        ACOUSTIC,
        RNB,
        TREBLE_BOOST
    )

    fun findById(id: String): GraphicEQPreset? = ALL.find { it.id == id }
}

/**
 * Saved EQ Profile with metadata
 */
@Serializable
data class SavedEQProfile(
    val id: String,                       // Unique identifier
    val name: String,                     // Display name
    val deviceModel: String,              // e.g., "Rock (10-Band)" or "Sony WH-1000XM4"
    val bands: List<ParametricEQBand>,    // EQ bands
    val preamp: Double = 0.0,             // Preamp gain in dB
    val bassBoostDb: Double = 0.0,        // Dedicated low-frequency Bass Boost in dB (0.0..12.0)
    val isCustom: Boolean = false,        // Whether this is a custom imported/saved profile
    val isActive: Boolean = false,        // Whether this profile is currently active
    val addedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Repository for managing 10-band Graphic Equalizer state, Bass Boost control, presets, and custom EQ profiles.
 */
@Singleton
class EQProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nanosonic_eq_profiles",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _profiles = MutableStateFlow<List<SavedEQProfile>>(emptyList())
    val profiles: StateFlow<List<SavedEQProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<SavedEQProfile?>(null)
    val activeProfile: StateFlow<SavedEQProfile?> = _activeProfile.asStateFlow()

    private val _isEqEnabled = MutableStateFlow(false)
    val isEqEnabled: StateFlow<Boolean> = _isEqEnabled.asStateFlow()

    private val _selectedPresetId = MutableStateFlow(BuiltInEQPresets.FLAT.id)
    val selectedPresetId: StateFlow<String> = _selectedPresetId.asStateFlow()

    private val _bandGains = MutableStateFlow(BuiltInEQPresets.FLAT.gainsDb)
    val bandGains: StateFlow<List<Float>> = _bandGains.asStateFlow()

    private val _preampDb = MutableStateFlow(0f)
    val preampDb: StateFlow<Float> = _preampDb.asStateFlow()

    private val _bassBoostPercent = MutableStateFlow(0)
    val bassBoostPercent: StateFlow<Int> = _bassBoostPercent.asStateFlow()

    companion object {
        private const val PREFS_NAME = "nanosonic_eq_profiles"
        private const val KEY_PROFILES = "eq_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_GRAPHIC_EQ_ENABLED = "graphic_eq_enabled"
        private const val KEY_GRAPHIC_EQ_PRESET_ID = "graphic_eq_preset_id"
        private const val KEY_GRAPHIC_EQ_GAINS = "graphic_eq_gains"
        private const val KEY_GRAPHIC_EQ_PREAMP = "graphic_eq_preamp"
        private const val KEY_GRAPHIC_EQ_BASS_BOOST = "graphic_eq_bass_boost_percent"
        private const val GRAPHIC_ACTIVE_PROFILE_ID = "graphic_eq_10band_active"

        private val staticJson = Json {
            ignoreUnknownKeys = true
        }

        /**
         * Reads the currently active 10-band Equalizer / custom profile and Bass Boost from
         * SharedPreferences so offline/background pipelines (such as VideoLyrics export)
         * apply the exact same Equalizer and Bass Boost settings as live playback.
         */
        fun loadActiveParametricEQ(context: Context): ParametricEQ? {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean(KEY_GRAPHIC_EQ_ENABLED, false)
                val bassPercent = prefs.getInt(KEY_GRAPHIC_EQ_BASS_BOOST, 0).coerceIn(0, 100)
                val bassDb = GraphicEQBands.bassBoostPercentToDb(bassPercent).toDouble()

                val activeCustomId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
                if (!activeCustomId.isNullOrBlank()) {
                    val profilesJson = prefs.getString(KEY_PROFILES, null)
                    val loadedProfiles = if (profilesJson != null) {
                        runCatching { staticJson.decodeFromString<List<SavedEQProfile>>(profilesJson) }
                            .getOrDefault(emptyList())
                    } else emptyList()

                    val customActive = loadedProfiles.find { it.id == activeCustomId }
                    if (customActive != null) {
                        return ParametricEQ(
                            preamp = customActive.preamp,
                            bands = customActive.bands,
                            bassBoostDb = if (bassDb > 0.0) bassDb else customActive.bassBoostDb
                        )
                    }
                }

                if (!enabled && bassPercent <= 0) {
                    return null
                }

                val presetId = prefs.getString(KEY_GRAPHIC_EQ_PRESET_ID, BuiltInEQPresets.FLAT.id)
                    ?: BuiltInEQPresets.FLAT.id
                val gainsCsv = prefs.getString(KEY_GRAPHIC_EQ_GAINS, null)
                val loadedGains = if (!enabled) {
                    BuiltInEQPresets.FLAT.gainsDb
                } else if (!gainsCsv.isNullOrBlank()) {
                    val parsed = gainsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
                    if (parsed.size == GraphicEQBands.BAND_COUNT) parsed else BuiltInEQPresets.FLAT.gainsDb
                } else {
                    BuiltInEQPresets.findById(presetId)?.gainsDb ?: BuiltInEQPresets.FLAT.gainsDb
                }
                val preamp = if (enabled) prefs.getFloat(KEY_GRAPHIC_EQ_PREAMP, 0f).toDouble() else 0.0

                val hasNonZeroBand = loadedGains.any { kotlin.math.abs(it) > 0.05f }
                if (!hasNonZeroBand && kotlin.math.abs(preamp) <= 0.05 && bassDb <= 0.05) {
                    return null
                }

                ParametricEQ(
                    preamp = preamp,
                    bands = if (hasNonZeroBand) GraphicEQBands.buildParametricBands(loadedGains) else emptyList(),
                    bassBoostDb = bassDb
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Returns a short badge label describing the active Equalizer & Bass Boost state
         * (e.g., "EQ Rock • Bass 80%"), or null if neither is active.
         */
        fun getActiveEqSummaryLabel(context: Context): String? {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean(KEY_GRAPHIC_EQ_ENABLED, false)
                val bassPercent = prefs.getInt(KEY_GRAPHIC_EQ_BASS_BOOST, 0).coerceIn(0, 100)
                val activeCustomId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)

                val eqName: String? = when {
                    !activeCustomId.isNullOrBlank() -> "EQ Custom"
                    enabled -> {
                        val presetId = prefs.getString(KEY_GRAPHIC_EQ_PRESET_ID, BuiltInEQPresets.FLAT.id)
                            ?: BuiltInEQPresets.FLAT.id
                        val presetName = BuiltInEQPresets.findById(presetId)?.name ?: "Custom"
                        "EQ $presetName"
                    }
                    else -> null
                }

                val bassLabel = if (bassPercent > 0) "Bass $bassPercent%" else null
                listOfNotNull(eqName, bassLabel).takeIf { it.isNotEmpty() }?.joinToString(" • ")
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Builds a compact cache key tag representing the active Equalizer and Bass Boost configuration.
         */
        fun buildActiveEqCacheTag(context: Context): String {
            val activeEq = loadActiveParametricEQ(context) ?: return "EQ_OFF"
            val bandsSignature = activeEq.bands.joinToString(",") {
                "${it.frequency.toInt()}:${"%.1f".format(java.util.Locale.US, it.gain)}"
            }
            val rawSig = "p=${"%.1f".format(java.util.Locale.US, activeEq.preamp)}_b=${"%.1f".format(java.util.Locale.US, activeEq.bassBoostDb)}_$bandsSignature"
            return "EQ_${kotlin.math.abs(rawSig.hashCode())}"
        }
    }

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        try {
            val profilesJson = prefs.getString(KEY_PROFILES, null)
            val loadedProfiles = if (profilesJson != null) {
                json.decodeFromString<List<SavedEQProfile>>(profilesJson)
            } else {
                emptyList()
            }
            _profiles.value = loadedProfiles

            val enabled = prefs.getBoolean(KEY_GRAPHIC_EQ_ENABLED, false)
            val presetId = prefs.getString(KEY_GRAPHIC_EQ_PRESET_ID, BuiltInEQPresets.FLAT.id)
                ?: BuiltInEQPresets.FLAT.id
            val gainsCsv = prefs.getString(KEY_GRAPHIC_EQ_GAINS, null)
            val loadedGains = if (!gainsCsv.isNullOrBlank()) {
                val parsed = gainsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
                if (parsed.size == GraphicEQBands.BAND_COUNT) parsed else BuiltInEQPresets.FLAT.gainsDb
            } else {
                BuiltInEQPresets.findById(presetId)?.gainsDb ?: BuiltInEQPresets.FLAT.gainsDb
            }
            val preamp = prefs.getFloat(KEY_GRAPHIC_EQ_PREAMP, 0f)
            val bassPercent = prefs.getInt(KEY_GRAPHIC_EQ_BASS_BOOST, 0).coerceIn(0, 100)

            _isEqEnabled.value = enabled
            _selectedPresetId.value = presetId
            _bandGains.value = loadedGains
            _preampDb.value = preamp
            _bassBoostPercent.value = bassPercent

            val activeCustomId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
            val customActive = loadedProfiles.find { it.id == activeCustomId }
            if (customActive != null) {
                _isEqEnabled.value = true
                _activeProfile.value = customActive.copy(
                    bassBoostDb = GraphicEQBands.bassBoostPercentToDb(bassPercent).toDouble()
                )
            } else if (enabled) {
                _activeProfile.value = buildGraphicEQProfile(presetId, loadedGains, preamp, bassPercent)
            } else {
                _activeProfile.value = null
            }
        } catch (e: Exception) {
            _profiles.value = emptyList()
            _activeProfile.value = null
        }
    }

    private fun buildGraphicEQProfile(
        presetId: String,
        gainsDb: List<Float>,
        preampDb: Float,
        bassBoostPercent: Int
    ): SavedEQProfile {
        val presetName = BuiltInEQPresets.findById(presetId)?.name ?: "Custom"
        val bassDb = GraphicEQBands.bassBoostPercentToDb(bassBoostPercent).toDouble()
        return SavedEQProfile(
            id = GRAPHIC_ACTIVE_PROFILE_ID,
            name = "$presetName (10-Band EQ)",
            deviceModel = "$presetName (10-Band EQ)",
            bands = GraphicEQBands.buildParametricBands(gainsDb),
            preamp = preampDb.toDouble(),
            bassBoostDb = bassDb,
            isCustom = false,
            isActive = true
        )
    }

    private fun persistGraphicEqState(
        enabled: Boolean,
        presetId: String,
        gainsDb: List<Float>,
        preampDb: Float,
        bassBoostPercent: Int
    ) {
        prefs.edit {
            putBoolean(KEY_GRAPHIC_EQ_ENABLED, enabled)
            putString(KEY_GRAPHIC_EQ_PRESET_ID, presetId)
            putString(KEY_GRAPHIC_EQ_GAINS, gainsDb.joinToString(","))
            putFloat(KEY_GRAPHIC_EQ_PREAMP, preampDb)
            putInt(KEY_GRAPHIC_EQ_BASS_BOOST, bassBoostPercent)
            remove(KEY_ACTIVE_PROFILE_ID)
        }
    }

    /**
     * Enable or disable the 10-band graphic equalizer.
     */
    fun setGraphicEqEnabled(enabled: Boolean) {
        _isEqEnabled.value = enabled
        persistGraphicEqState(
            enabled = enabled,
            presetId = _selectedPresetId.value,
            gainsDb = _bandGains.value,
            preampDb = _preampDb.value,
            bassBoostPercent = _bassBoostPercent.value
        )
        _activeProfile.value = if (enabled) {
            buildGraphicEQProfile(
                _selectedPresetId.value,
                _bandGains.value,
                _preampDb.value,
                _bassBoostPercent.value
            )
        } else {
            null
        }
    }

    /**
     * Apply a built-in 10-band preset (Rock, Jazz, Pop, Classical, etc.)
     */
    fun applyPreset(preset: GraphicEQPreset, resetBassBoostIfFlat: Boolean = false) {
        val newBassPercent = when {
            resetBassBoostIfFlat && preset.id == BuiltInEQPresets.FLAT.id -> 0
            preset.id == BuiltInEQPresets.BASS_BOOST.id && _bassBoostPercent.value == 0 -> 50
            else -> _bassBoostPercent.value
        }
        _isEqEnabled.value = true
        _selectedPresetId.value = preset.id
        _bandGains.value = preset.gainsDb
        _preampDb.value = preset.preampDb
        _bassBoostPercent.value = newBassPercent

        persistGraphicEqState(
            enabled = true,
            presetId = preset.id,
            gainsDb = preset.gainsDb,
            preampDb = preset.preampDb,
            bassBoostPercent = newBassPercent
        )
        _activeProfile.value = buildGraphicEQProfile(
            preset.id,
            preset.gainsDb,
            preset.preampDb,
            newBassPercent
        )
    }

    /**
     * Update a single band's gain in dB (-12..+12 dB) in real time.
     */
    fun updateBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until GraphicEQBands.BAND_COUNT) return
        val clamped = gainDb.coerceIn(GraphicEQBands.MIN_GAIN_DB, GraphicEQBands.MAX_GAIN_DB)
        val updated = _bandGains.value.toMutableList().apply {
            this[bandIndex] = clamped
        }
        _isEqEnabled.value = true
        _selectedPresetId.value = BuiltInEQPresets.CUSTOM_PRESET_ID
        _bandGains.value = updated

        persistGraphicEqState(
            enabled = true,
            presetId = BuiltInEQPresets.CUSTOM_PRESET_ID,
            gainsDb = updated,
            preampDb = _preampDb.value,
            bassBoostPercent = _bassBoostPercent.value
        )
        _activeProfile.value = buildGraphicEQProfile(
            BuiltInEQPresets.CUSTOM_PRESET_ID,
            updated,
            _preampDb.value,
            _bassBoostPercent.value
        )
    }

    /**
     * Update the dedicated Bass Boost strength (0%..100%, corresponding to 0..+12 dB low-frequency boost) in real time.
     */
    fun updateBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _bassBoostPercent.value = clamped
        if (clamped > 0 && !_isEqEnabled.value) {
            _isEqEnabled.value = true
        }
        val enabled = _isEqEnabled.value

        persistGraphicEqState(
            enabled = enabled,
            presetId = _selectedPresetId.value,
            gainsDb = _bandGains.value,
            preampDb = _preampDb.value,
            bassBoostPercent = clamped
        )
        _activeProfile.value = if (enabled) {
            buildGraphicEQProfile(
                _selectedPresetId.value,
                _bandGains.value,
                _preampDb.value,
                clamped
            )
        } else {
            null
        }
    }

    /**
     * Update the preamp gain in dB (-12..+12 dB) in real time.
     */
    fun updatePreamp(preampDb: Float) {
        val clamped = preampDb.coerceIn(GraphicEQBands.MIN_GAIN_DB, GraphicEQBands.MAX_GAIN_DB)
        _isEqEnabled.value = true
        _preampDb.value = clamped

        persistGraphicEqState(
            enabled = true,
            presetId = _selectedPresetId.value,
            gainsDb = _bandGains.value,
            preampDb = clamped,
            bassBoostPercent = _bassBoostPercent.value
        )
        _activeProfile.value = buildGraphicEQProfile(
            _selectedPresetId.value,
            _bandGains.value,
            clamped,
            _bassBoostPercent.value
        )
    }

    /**
     * Save current 10-band graphic EQ curve + Bass Boost as a named custom profile.
     */
    suspend fun saveCurrentGraphicAsProfile(name: String) = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifEmpty { "Custom 10-Band" }
        val id = "custom_${System.currentTimeMillis()}_${cleanName.hashCode()}"
        val bassDb = GraphicEQBands.bassBoostPercentToDb(_bassBoostPercent.value).toDouble()
        val profile = SavedEQProfile(
            id = id,
            name = cleanName,
            deviceModel = cleanName,
            bands = GraphicEQBands.buildParametricBands(_bandGains.value),
            preamp = _preampDb.value.toDouble(),
            bassBoostDb = bassDb,
            isCustom = true,
            isActive = true
        )
        saveProfile(profile)
        setActiveProfile(profile.id)
    }

    /**
     * Save a new EQ profile
     */
    suspend fun saveProfile(profile: SavedEQProfile) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }

        if (existingIndex >= 0) {
            currentProfiles[existingIndex] = profile
        } else {
            currentProfiles.add(profile)
        }

        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        _profiles.value = currentProfiles
    }

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(profileId: String) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value.toMutableList()
        currentProfiles.removeAll { it.id == profileId }

        val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
        prefs.edit { putString(KEY_PROFILES, profilesJson) }

        if (_activeProfile.value?.id == profileId) {
            _activeProfile.value = null
            _isEqEnabled.value = false
            prefs.edit {
                remove(KEY_ACTIVE_PROFILE_ID)
                putBoolean(KEY_GRAPHIC_EQ_ENABLED, false)
            }
        }

        _profiles.value = currentProfiles
    }

    /**
     * Set a custom profile as active (pass null to deactivate all profiles)
     */
    suspend fun setActiveProfile(profileId: String?) = withContext(Dispatchers.IO) {
        val currentProfiles = _profiles.value

        if (profileId == null) {
            _activeProfile.value = null
            _isEqEnabled.value = false
            prefs.edit {
                remove(KEY_ACTIVE_PROFILE_ID)
                putBoolean(KEY_GRAPHIC_EQ_ENABLED, false)
            }
        } else {
            val profile = currentProfiles.find { it.id == profileId }
            if (profile != null) {
                _isEqEnabled.value = true
                _selectedPresetId.value = profile.id
                _preampDb.value = profile.preamp.toFloat().coerceIn(
                    GraphicEQBands.MIN_GAIN_DB,
                    GraphicEQBands.MAX_GAIN_DB
                )
                val profileBassPercent = ((profile.bassBoostDb.toFloat() / GraphicEQBands.MAX_BASS_BOOST_DB) * 100f)
                    .toInt()
                    .coerceIn(0, 100)
                if (profileBassPercent > 0) {
                    _bassBoostPercent.value = profileBassPercent
                }
                // Map 10-band frequencies from profile bands if available
                val mappedGains = GraphicEQBands.FREQUENCIES_HZ.mapIndexed { idx, freq ->
                    val matchingBand = profile.bands.getOrNull(idx)
                        ?: profile.bands.minByOrNull { kotlin.math.abs(it.frequency - freq) }
                    (matchingBand?.gain?.toFloat() ?: 0f).coerceIn(
                        GraphicEQBands.MIN_GAIN_DB,
                        GraphicEQBands.MAX_GAIN_DB
                    )
                }
                _bandGains.value = mappedGains
                val effectiveBassDb = GraphicEQBands.bassBoostPercentToDb(_bassBoostPercent.value).toDouble()
                _activeProfile.value = profile.copy(bassBoostDb = effectiveBassDb)
                prefs.edit {
                    putString(KEY_ACTIVE_PROFILE_ID, profileId)
                    putBoolean(KEY_GRAPHIC_EQ_ENABLED, true)
                    putInt(KEY_GRAPHIC_EQ_BASS_BOOST, _bassBoostPercent.value)
                }
            }
        }
    }

    fun getAllProfiles(): List<SavedEQProfile> = _profiles.value

    fun getActiveProfile(): SavedEQProfile? = _activeProfile.value

    /**
     * Import a custom EQ profile from ParametricEQ data
     */
    suspend fun importCustomProfile(
        name: String,
        parametricEQ: ParametricEQ
    ) = withContext(Dispatchers.IO) {
        val id = "custom_${System.currentTimeMillis()}_${name.hashCode()}"

        val customProfile = SavedEQProfile(
            id = id,
            name = name,
            deviceModel = name,
            bands = parametricEQ.bands,
            preamp = parametricEQ.preamp,
            bassBoostDb = parametricEQ.bassBoostDb,
            isActive = false,
            isCustom = true
        )

        saveProfile(customProfile)
    }

    fun getSortedProfiles(): List<SavedEQProfile> {
        return _profiles.value
            .filter { it.isCustom }
            .sortedByDescending { it.addedTimestamp }
    }
}
