package com.nanzbeatles.nanas.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanzbeatles.nanas.eq.EqualizerService
import com.nanzbeatles.nanas.eq.data.BuiltInEQPresets
import com.nanzbeatles.nanas.eq.data.EQProfileRepository
import com.nanzbeatles.nanas.eq.data.GraphicEQPreset
import com.nanzbeatles.nanas.eq.data.ParametricEQParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

/**
 * ViewModel for the 10-Band Graphic Equalizer, Bass Boost & Custom Parametric EQ Screen.
 * Drives [EqualizerService] and [EQProfileRepository] in real time via Media3's AudioProcessor API.
 */
@HiltViewModel
class EQViewModel @Inject constructor(
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService
) : ViewModel() {

    private val _state = MutableStateFlow(EQState())
    val state: StateFlow<EQState> = _state.asStateFlow()

    init {
        observeEqualizerState()
    }

    private fun observeEqualizerState() {
        viewModelScope.launch {
            combine(
                eqProfileRepository.isEqEnabled,
                eqProfileRepository.selectedPresetId,
                eqProfileRepository.bandGains,
                eqProfileRepository.preampDb,
                eqProfileRepository.bassBoostPercent
            ) { isEnabled, presetId, gains, preamp, bassBoost ->
                EQStateSnapshot(
                    isEnabled = isEnabled,
                    selectedPresetId = presetId,
                    bandGains = gains,
                    preampDb = preamp,
                    bassBoostPercent = bassBoost
                )
            }.collect { snapshot ->
                _state.update {
                    it.copy(
                        isEnabled = snapshot.isEnabled,
                        selectedPresetId = snapshot.selectedPresetId,
                        bandGains = snapshot.bandGains,
                        preampDb = snapshot.preampDb,
                        bassBoostPercent = snapshot.bassBoostPercent
                    )
                }
            }
        }

        viewModelScope.launch {
            eqProfileRepository.activeProfile.collect { activeProfile ->
                _state.update { state ->
                    state.copy(activeProfileId = activeProfile?.id)
                }
            }
        }

        viewModelScope.launch {
            eqProfileRepository.profiles.collect {
                val sortedProfiles = eqProfileRepository.getSortedProfiles()
                _state.update { state ->
                    state.copy(profiles = sortedProfiles)
                }
            }
        }
    }

    private data class EQStateSnapshot(
        val isEnabled: Boolean,
        val selectedPresetId: String,
        val bandGains: List<Float>,
        val preampDb: Float,
        val bassBoostPercent: Int
    )

    /**
     * Toggle the 10-band graphic equalizer on or off.
     */
    fun setEqualizerEnabled(enabled: Boolean) {
        eqProfileRepository.setGraphicEqEnabled(enabled)
        if (enabled) {
            eqProfileRepository.getActiveProfile()?.let { profile ->
                equalizerService.applyProfile(profile)
            }
        } else {
            equalizerService.disable()
        }
    }

    /**
     * Apply a built-in 10-band preset (Rock, Jazz, Pop, Classical, Hip-Hop, etc.)
     */
    fun selectPreset(preset: GraphicEQPreset) {
        eqProfileRepository.applyPreset(preset, resetBassBoostIfFlat = false)
        eqProfileRepository.getActiveProfile()?.let { profile ->
            equalizerService.applyProfile(profile)
        }
    }

    /**
     * Update a single band's gain (-12 dB to +12 dB) in real time.
     */
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        eqProfileRepository.updateBandGain(bandIndex, gainDb)
        eqProfileRepository.getActiveProfile()?.let { profile ->
            equalizerService.applyProfile(profile)
        }
    }

    /**
     * Update dedicated Bass Boost strength (0% to 100%, corresponding to 0.0 to +12.0 dB low-frequency boost).
     */
    fun setBassBoost(percent: Int) {
        eqProfileRepository.updateBassBoost(percent)
        eqProfileRepository.getActiveProfile()?.let { profile ->
            equalizerService.applyProfile(profile)
        }
    }

    /**
     * Update preamp gain (-12 dB to +12 dB) in real time.
     */
    fun setPreamp(preampDb: Float) {
        eqProfileRepository.updatePreamp(preampDb)
        eqProfileRepository.getActiveProfile()?.let { profile ->
            equalizerService.applyProfile(profile)
        }
    }

    /**
     * Reset all 10 bands, Bass Boost, and preamp to Flat (0 dB / 0%).
     */
    fun resetToFlat() {
        eqProfileRepository.applyPreset(BuiltInEQPresets.FLAT, resetBassBoostIfFlat = true)
        eqProfileRepository.getActiveProfile()?.let { profile ->
            equalizerService.applyProfile(profile)
        }
    }

    /**
     * Save current 10-band sliders + Bass Boost as a custom named profile.
     */
    fun saveCurrentAsCustomProfile(name: String) {
        viewModelScope.launch {
            eqProfileRepository.saveCurrentGraphicAsProfile(name)
            eqProfileRepository.getActiveProfile()?.let { profile ->
                equalizerService.applyProfile(profile)
            }
        }
    }

    /**
     * Select and apply a saved custom EQ profile, or pass null to disable EQ.
     */
    fun selectProfile(profileId: String?) {
        viewModelScope.launch {
            if (profileId == null) {
                equalizerService.disable()
                eqProfileRepository.setActiveProfile(null)
            } else {
                val profile = _state.value.profiles.find { it.id == profileId }
                if (profile != null) {
                    eqProfileRepository.setActiveProfile(profileId)
                    val active = eqProfileRepository.getActiveProfile() ?: profile
                    val result = equalizerService.applyProfile(active)
                    result.onFailure { e ->
                        _state.update { it.copy(error = e.message ?: "Kesalahan tidak diketahui") }
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            eqProfileRepository.deleteProfile(profileId)
        }
    }

    /**
     * Import a custom EQ profile from an APO ParametricEQ .txt file
     */
    fun importCustomProfile(
        fileName: String,
        inputStream: InputStream,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val content = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val parametricEQ = ParametricEQParser.parseText(content)
                val validationErrors = ParametricEQParser.validate(parametricEQ)
                if (validationErrors.isNotEmpty()) {
                    onError(Exception("File EQ tidak valid: ${validationErrors.first()}"))
                    return@launch
                }

                val profileName = fileName.removeSuffix(".txt")
                eqProfileRepository.importCustomProfile(profileName, parametricEQ)

                _state.update { it.copy(importStatus = "Berhasil mengimpor $profileName") }
                onSuccess()
            } catch (e: Exception) {
                onError(Exception("Gagal mengimpor profil EQ: ${e.message}"))
            }
        }
    }
}