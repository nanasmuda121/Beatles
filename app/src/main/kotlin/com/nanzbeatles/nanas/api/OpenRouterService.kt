/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object OpenRouterService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Built-in Default Translation API (no API key required).
     * Translates lyric lines 1-to-1 using the public GTX translation endpoint with batch + per-line alignment guarantee.
     */
    suspend fun translateWithDefaultApi(
        text: String,
        targetLanguageCode: String,
        mode: String = "Literal"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Teks input kosong"))
            }
            val lines = text.lines()
            if (lines.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val normalizedLang = normalizeLanguageCode(targetLanguageCode)
            val resultLines = MutableList(lines.size) { "" }

            // Process in batches of 25 lines for fast single-roundtrip translation
            val batchSize = 25
            for (batchStart in lines.indices step batchSize) {
                val batchEnd = minOf(batchStart + batchSize, lines.size)
                val batch = lines.subList(batchStart, batchEnd)
                val batchJoined = batch.joinToString("\n")

                val batchTranslated = requestPublicTranslate(
                    queryText = batchJoined,
                    targetLang = normalizedLang,
                    mode = mode
                )?.lines()

                if (batchTranslated != null && batchTranslated.size == batch.size) {
                    for (i in batch.indices) {
                        resultLines[batchStart + i] = batchTranslated[i].trim()
                    }
                } else {
                    // Fallback to concurrent per-line translation for 100% exact line alignment
                    val perLineResults = coroutineScope {
                        batch.map { singleLine ->
                            async {
                                if (singleLine.isBlank()) {
                                    ""
                                } else {
                                    requestPublicTranslate(
                                        queryText = singleLine,
                                        targetLang = normalizedLang,
                                        mode = mode
                                    )?.lineSequence()?.firstOrNull()?.trim() ?: singleLine
                                }
                            }
                        }.awaitAll()
                    }
                    for (i in batch.indices) {
                        resultLines[batchStart + i] = perLineResults[i]
                    }
                }
            }

            Result.success(resultLines)
        } catch (e: Exception) {
            Result.failure(Exception("Penerjemahan API default gagal: ${e.message}"))
        }
    }

    private fun requestPublicTranslate(
        queryText: String,
        targetLang: String,
        mode: String
    ): String? {
        if (queryText.isBlank()) return ""
        return try {
            val encodedQuery = URLEncoder.encode(queryText, "UTF-8")
            val dtParams = if (mode == "Romanized") "dt=t&dt=rm" else "dt=t"
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&$dtParams&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val rootArray = JSONArray(body)
                val segments = rootArray.optJSONArray(0) ?: return null

                if (mode == "Romanized") {
                    // Extract romanization if available in the last segment, otherwise translated text
                    val romanizedBuilder = StringBuilder()
                    for (i in 0 until segments.length()) {
                        val seg = segments.optJSONArray(i) ?: continue
                        val rom = seg.optString(3).takeIf { it.isNotBlank() && it != "null" }
                            ?: seg.optString(2).takeIf { it.isNotBlank() && it != "null" }
                        if (rom != null) {
                            romanizedBuilder.append(rom)
                        }
                    }
                    if (romanizedBuilder.isNotBlank()) {
                        return romanizedBuilder.toString()
                    }
                }

                val translatedBuilder = StringBuilder()
                for (i in 0 until segments.length()) {
                    val seg = segments.optJSONArray(i) ?: continue
                    val part = seg.optString(0)
                    if (part != "null") {
                        translatedBuilder.append(part)
                    }
                }
                translatedBuilder.toString().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeLanguageCode(lang: String): String {
        val cleaned = lang.trim()
        return when (cleaned.lowercase()) {
            "indonesian", "bahasa indonesia", "in", "id", "id-id" -> "id"
            "english", "en", "en-us", "en-gb" -> "en"
            "japanese", "ja", "jp" -> "ja"
            "korean", "ko", "kr" -> "ko"
            "chinese", "chinese (simplified)", "zh", "zh-cn" -> "zh-CN"
            "chinese (traditional)", "zh-tw" -> "zh-TW"
            "spanish", "es" -> "es"
            "french", "fr" -> "fr"
            "german", "de" -> "de"
            "russian", "ru" -> "ru"
            "arabic", "ar" -> "ar"
            "portuguese", "pt" -> "pt"
            "hindi", "hi" -> "hi"
            "turkish", "tr" -> "tr"
            "vietnamese", "vi" -> "vi"
            "thai", "th" -> "th"
            "malay", "ms" -> "ms"
            else -> if (cleaned.length <= 5) cleaned.lowercase() else "id"
        }
    }

    suspend fun translate(
        text: String,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        mode: String,
        maxRetries: Int = 2,
        sourceLanguage: String? = null,
        targetLanguageCode: String = "id"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        // Validate input
        if (text.isBlank()) {
            return@withContext Result.failure(Exception("Teks input kosong"))
        }

        // Default mode: if the user did not configure a custom AI API Key, use the built-in Default Translation API!
        if (apiKey.isBlank()) {
            return@withContext translateWithDefaultApi(
                text = text,
                targetLanguageCode = targetLanguageCode.ifBlank { targetLanguage },
                mode = mode
            )
        }

        var currentAttempt = 0
        val lines = text.lines()
        val lineCount = lines.size

        while (currentAttempt < maxRetries) {
            try {
                // Enhanced prompt with strict formatting requirements
                val systemPrompt = """You are a precise lyrics translation assistant. Your output must ALWAYS be a valid JSON array of strings.

CRITICAL RULES:
1. Output ONLY a JSON array: ["line1", "line2", "line3"]
2. NO explanations, NO questions, NO additional text
3. Each input line maps to exactly one output line
4. Preserve empty lines as empty strings ""
5. Return EXACTLY $lineCount items in the array
6. If uncertain, provide best approximation but maintain line count"""

                val userPrompt = when (mode) {
                    "Romanized" -> """Romanize/transliterate the following $lineCount lines into simple Latin script using ONLY basic English letters (a-z, A-Z).

CRITICAL REQUIREMENTS:
- Use ONLY simple ASCII characters (a-z, A-Z, 0-9, basic punctuation)
- NO special characters like ā, ī, ū, ñ, ç, etc.
- NO diacritics or accent marks
- If text is already in Latin script, return it UNCHANGED
- For non-Latin scripts (Hindi, Chinese, Japanese, Korean, Cyrillic, etc.), provide simple romanization
- DO NOT translate meaning, only convert script to simple English letters
- Keep all punctuation and formatting
- Preserve line-by-line structure exactly

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings using ONLY simple ASCII characters."""

                    "Transcribed" -> """Transcribe/transliterate the following $lineCount lines phonetically into $targetLanguage script.

CRITICAL REQUIREMENTS:
- Convert the SOUND/PRONUNCIATION of the original text into $targetLanguage script
- DO NOT translate the meaning - only represent how the original words SOUND
- Use the native script of $targetLanguage (e.g., Devanagari for Hindi, Hangul for Korean, etc.)
- Preserve the original pronunciation as closely as possible in the target script
- Keep punctuation and formatting
- Preserve line-by-line structure exactly
- If text is already in $targetLanguage script, return it UNCHANGED

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings in $targetLanguage script."""

                    else -> """Translate the following $lineCount lines to $targetLanguage.

IMPORTANT:
- Provide natural, accurate translation
- Maintain poetic flow and meaning
- Keep punctuation appropriate for target language
- Preserve line-by-line structure exactly
- For song lyrics, prioritize singability

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings."""
                }

                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }

                val jsonBody = JSONObject().apply {
                    if (model.isNotBlank()) {
                        put("model", model)
                    }
                    put("messages", messages)
                    put("temperature", 0.3)
                    put("max_tokens", lineCount * 100)
                }

                val request = Request.Builder()
                    .url(baseUrl.ifBlank { "https://openrouter.ai/api/v1/chat/completions" })
                    .apply {
                        if (apiKey.isNotBlank()) {
                            addHeader("Authorization", "Bearer ${apiKey.trim()}")
                        }
                    }
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://github.com/nanasmuda121/NanzBeatles")
                    .addHeader("X-Title", "Beatles")
                    .post(jsonBody.toString().toRequestBody(JSON))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    if (response.code >= 500) {
                        currentAttempt++
                        kotlinx.coroutines.delay(1000L * currentAttempt)
                        continue
                    }
                    // Fallback to built-in default translation API if custom AI key/endpoint fails
                    return@withContext translateWithDefaultApi(
                        text = text,
                        targetLanguageCode = targetLanguageCode.ifBlank { targetLanguage },
                        mode = mode
                    )
                }

                if (responseBody == null) {
                    currentAttempt++
                    continue
                }

                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    var content = message?.optString("content")?.trim()

                    if (!content.isNullOrBlank()) {
                        var translatedLines: List<String>? = null

                        try {
                            val jsonArray = JSONArray(content)
                            translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                        } catch (e: Exception) {
                            content = content.replace("```json", "").replace("```", "").trim()

                            try {
                                val jsonArray = JSONArray(content)
                                translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                            } catch (e2: Exception) {
                                val startIdx = content.indexOf('[')
                                val endIdx = content.lastIndexOf(']')

                                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                                    val jsonString = content.substring(startIdx, endIdx + 1)
                                    try {
                                        val jsonArray = JSONArray(jsonString)
                                        translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                                    } catch (e3: Exception) {
                                        translatedLines = content.lines()
                                            .filter { it.trim().isNotEmpty() }
                                            .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                    }
                                }
                            }
                        }

                        if (translatedLines != null) {
                            if (translatedLines.size == lineCount) {
                                return@withContext Result.success(translatedLines)
                            } else if (translatedLines.size > lineCount) {
                                return@withContext Result.success(translatedLines.take(lineCount))
                            } else {
                                val paddedLines = translatedLines.toMutableList()
                                while (paddedLines.size < lineCount) {
                                    paddedLines.add("")
                                }
                                return@withContext Result.success(paddedLines)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (currentAttempt == maxRetries - 1) {
                    return@withContext translateWithDefaultApi(
                        text = text,
                        targetLanguageCode = targetLanguageCode.ifBlank { targetLanguage },
                        mode = mode
                    )
                }
            }
            currentAttempt++
            kotlinx.coroutines.delay(800L * currentAttempt)
        }
        return@withContext translateWithDefaultApi(
            text = text,
            targetLanguageCode = targetLanguageCode.ifBlank { targetLanguage },
            mode = mode
        )
    }
}
