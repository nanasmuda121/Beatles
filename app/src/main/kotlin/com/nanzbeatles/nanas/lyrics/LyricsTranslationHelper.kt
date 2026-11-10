/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyrics

import android.content.Context
import com.nanzbeatles.nanas.api.OpenRouterService
import com.nanzbeatles.nanas.constants.LanguageCodeToName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object LyricsTranslationHelper {
    private val _status = MutableStateFlow<TranslationStatus>(TranslationStatus.Idle)
    val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val _manualTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val manualTrigger: SharedFlow<Unit> = _manualTrigger.asSharedFlow()
    
    private var translationJob: Job? = null
    private var isCompositionActive = true
    
    // Cache for translations: key = hash of (lyrics content + mode + language), value = list of translations
    private val translationCache = mutableMapOf<String, List<String>>()
    
    private fun getCacheKey(lyricsText: String, mode: String, language: String): String {
        return "${lyricsText.hashCode()}_${mode}_$language"
    }
    
    fun getCachedTranslations(lyrics: List<LyricsEntry>, mode: String, language: String): List<String>? {
        val lyricsText = lyrics.filter { it.text.isNotBlank() }.joinToString("\n") { it.text }
        val key = getCacheKey(lyricsText, mode, language)
        return translationCache[key]
    }
    
    fun applyCachedTranslations(lyrics: List<LyricsEntry>, mode: String, language: String): Boolean {
        val cached = getCachedTranslations(lyrics, mode, language) ?: return false
        val nonEmptyEntries = lyrics.mapIndexedNotNull { index, entry ->
            if (entry.text.isNotBlank()) index to entry else null
        }

        if (cached.size >= nonEmptyEntries.size) {
            nonEmptyEntries.forEachIndexed { idx, (originalIndex, entry) ->
                val trans = cached[idx].trim()
                lyrics[originalIndex].translatedTextFlow.value =
                    if (trans.isNotBlank() && !trans.equals(entry.text.trim(), ignoreCase = true)) trans else null
            }
            return true
        }
        return false
    }

    fun clearTranslations(lyrics: List<LyricsEntry>) {
        lyrics.forEach { it.translatedTextFlow.value = null }
    }

    fun triggerManualTranslation() {
        _manualTrigger.tryEmit(Unit)
    }
    
    fun resetStatus() {
        _status.value = TranslationStatus.Idle
    }
    
    fun clearCache() {
        translationCache.clear()
    }
    
    fun setCompositionActive(active: Boolean) {
        isCompositionActive = active
    }
    
    fun cancelTranslation() {
        isCompositionActive = false
        translationJob?.cancel()
        translationJob = null
    }

    fun translateLyrics(
        lyrics: List<LyricsEntry>,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        mode: String,
        scope: CoroutineScope,
        context: Context
    ) {
        translationJob?.cancel()
        _status.value = TranslationStatus.Translating
        
        // Clear existing translations to indicate re-translation
        lyrics.forEach { it.translatedTextFlow.value = null }
        
        translationJob = scope.launch(Dispatchers.IO) {
            try {
                if (lyrics.isEmpty()) {
                    _status.value = TranslationStatus.Error(context.getString(com.nanzbeatles.nanas.R.string.ai_error_no_lyrics))
                    return@launch
                }
                
                // Filter out empty lines and keep track of their indices
                val nonEmptyEntries = lyrics.mapIndexedNotNull { index, entry ->
                    if (entry.text.isNotBlank()) index to entry else null
                }
                
                if (nonEmptyEntries.isEmpty()) {
                    _status.value = TranslationStatus.Error(context.getString(com.nanzbeatles.nanas.R.string.ai_error_lyrics_empty))
                    return@launch
                }
                
                // Create text from non-empty lines only
                val fullText = nonEmptyEntries.joinToString("\n") { it.second.text }

                val effectiveLangCode = targetLanguage.ifBlank { "id" }

                // Convert language code to full language name for AI understanding
                val fullLanguageName = LanguageCodeToName[effectiveLangCode] 
                    ?: try {
                        Locale.forLanguageTag(effectiveLangCode).displayLanguage.takeIf { it.isNotBlank() && it != effectiveLangCode }
                    } catch (e: Exception) { null }
                    ?: effectiveLangCode

                val result = OpenRouterService.translate(
                    text = fullText,
                    targetLanguage = fullLanguageName,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    mode = mode,
                    targetLanguageCode = effectiveLangCode
                )
                
                result.onSuccess { translatedLines ->
                    if (!isCompositionActive) {
                        return@onSuccess
                    }
                    
                    // Cache the translations
                    val cacheKey = getCacheKey(fullText, mode, effectiveLangCode)
                    translationCache[cacheKey] = translatedLines
                    
                    val expectedCount = nonEmptyEntries.size
                    
                    when {
                        translatedLines.size >= expectedCount -> {
                            nonEmptyEntries.forEachIndexed { idx, (originalIndex, entry) ->
                                val trans = translatedLines[idx].trim()
                                lyrics[originalIndex].translatedTextFlow.value =
                                    if (trans.isNotBlank() && !trans.equals(entry.text.trim(), ignoreCase = true)) trans else null
                            }
                            _status.value = TranslationStatus.Success
                        }
                        translatedLines.size < expectedCount -> {
                            translatedLines.forEachIndexed { idx, translation ->
                                if (idx < nonEmptyEntries.size) {
                                    val (originalIndex, entry) = nonEmptyEntries[idx]
                                    val trans = translation.trim()
                                    lyrics[originalIndex].translatedTextFlow.value =
                                        if (trans.isNotBlank() && !trans.equals(entry.text.trim(), ignoreCase = true)) trans else null
                                }
                            }
                            _status.value = TranslationStatus.Success
                        }
                        else -> {
                            _status.value = TranslationStatus.Error(context.getString(com.nanzbeatles.nanas.R.string.ai_error_unexpected))
                        }
                    }
                    
                    delay(3000)
                    if (_status.value is TranslationStatus.Success && isCompositionActive) {
                        _status.value = TranslationStatus.Idle
                    }
                }.onFailure { error ->
                    if (!isCompositionActive) {
                        return@onFailure
                    }
                    val errorMessage = error.message ?: context.getString(com.nanzbeatles.nanas.R.string.ai_error_unknown)
                    _status.value = TranslationStatus.Error(errorMessage)
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException && isCompositionActive) {
                    val errorMessage = e.message ?: context.getString(com.nanzbeatles.nanas.R.string.ai_error_translation_failed)
                    _status.value = TranslationStatus.Error(errorMessage)
                }
            }
        }
    }

    sealed class TranslationStatus {
        data object Idle : TranslationStatus()
        data object Translating : TranslationStatus()
        data object Success : TranslationStatus()
        data class Error(val message: String) : TranslationStatus()
    }
}
