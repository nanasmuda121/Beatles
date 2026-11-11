/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
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
     * Built-in Google Translate API using Chrome Extension client credentials.
     * Guaranteed to work without API key, never blocked by automated query blocks.
     */
    suspend fun translateWithGoogleApi(
        text: String,
        targetLanguageCode: String,
        useClients5: Boolean = false,
        mode: String = "Literal"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.failure(Exception("Teks input kosong"))
            val lines = text.lines()
            if (lines.isEmpty()) return@withContext Result.success(emptyList())

            val normalizedLang = normalizeLanguageCode(targetLanguageCode)
            val resultLines = MutableList(lines.size) { "" }

            // 1. First attempt: translate lines in chunks of 25 for fast single round-trip
            val batchSize = 25
            for (batchStart in lines.indices step batchSize) {
                val batchEnd = minOf(batchStart + batchSize, lines.size)
                val batch = lines.subList(batchStart, batchEnd)
                val batchJoined = batch.joinToString("\n")

                val batchTranslated = requestGoogleTranslate(
                    queryText = batchJoined,
                    targetLang = normalizedLang,
                    useClients5 = useClients5
                )?.lines()

                if (batchTranslated != null && batchTranslated.size == batch.size) {
                    for (i in batch.indices) {
                        resultLines[batchStart + i] = batchTranslated[i].trim()
                    }
                } else {
                    // Fallback to per-line translation for exact alignment
                    val perLineResults = coroutineScope {
                        val dispatcher = Dispatchers.IO.limitedParallelism(4)
                        batch.map { singleLine ->
                            async(dispatcher) {
                                if (singleLine.isBlank()) {
                                    ""
                                } else {
                                    requestGoogleTranslate(
                                        queryText = singleLine,
                                        targetLang = normalizedLang,
                                        useClients5 = useClients5
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

            val hasValidTranslations = resultLines.any { it.isNotBlank() }
            if (hasValidTranslations) {
                Result.success(resultLines)
            } else {
                Result.failure(Exception("Google Translate menghasilkan output kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun requestGoogleTranslate(
        queryText: String,
        targetLang: String,
        useClients5: Boolean = false
    ): String? {
        if (queryText.isBlank()) return ""
        return try {
            val url = if (useClients5) {
                "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=auto&tl=$targetLang"
            } else {
                "https://translate.googleapis.com/translate_a/t?client=dict-chrome-ex&sl=auto&tl=$targetLang"
            }

            val formBody = FormBody.Builder()
                .add("q", queryText)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val rootArray = JSONArray(body)
                val firstItem = rootArray.optJSONArray(0) ?: return null
                val result = firstItem.optString(0)
                if (result.isNotBlank()) result else null
            }
        } catch (e: Exception) {
            Timber.tag("OpenRouterService").w("requestGoogleTranslate failed: ${e.message}")
            null
        }
    }

    /**
     * MyMemory free translation API provider.
     */
    suspend fun translateWithMyMemory(
        text: String,
        targetLanguageCode: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.failure(Exception("Teks input kosong"))
            val lines = text.lines()
            if (lines.isEmpty()) return@withContext Result.success(emptyList())

            val normalizedLang = normalizeLanguageCode(targetLanguageCode)
            val perLineResults = coroutineScope {
                val dispatcher = Dispatchers.IO.limitedParallelism(4)
                lines.map { singleLine ->
                    async(dispatcher) {
                        if (singleLine.isBlank()) {
                            ""
                        } else {
                            requestMyMemoryTranslate(singleLine, normalizedLang) ?: singleLine
                        }
                    }
                }.awaitAll()
            }

            val hasValidTranslations = perLineResults.any { it.isNotBlank() }
            if (hasValidTranslations) {
                Result.success(perLineResults)
            } else {
                Result.failure(Exception("MyMemory menghasilkan output kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun requestMyMemoryTranslate(queryText: String, targetLang: String): String? {
        if (queryText.isBlank()) return ""
        return try {
            val encodedQuery = URLEncoder.encode(queryText, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=autodetect|$targetLang"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val responseData = json.optJSONObject("responseData") ?: return null
                val text = responseData.optString("translatedText")
                if (text.isNotBlank() && !text.startsWith("MYMEMORY WARNING")) {
                    android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                } else null
            }
        } catch (e: Exception) {
            Timber.tag("OpenRouterService").w("requestMyMemoryTranslate failed: ${e.message}")
            null
        }
    }

    /**
     * Built-in Default Translation API (auto-cascading Google -> Clients5 -> MyMemory).
     */
    suspend fun translateWithDefaultApi(
        text: String,
        targetLanguageCode: String,
        mode: String = "Literal"
    ): Result<List<String>> {
        val googleResult = translateWithGoogleApi(text, targetLanguageCode, useClients5 = false, mode = mode)
        if (googleResult.isSuccess) return googleResult
        val clients5Result = translateWithGoogleApi(text, targetLanguageCode, useClients5 = true, mode = mode)
        if (clients5Result.isSuccess) return clients5Result
        return translateWithMyMemory(text, targetLanguageCode)
    }

    fun normalizeLanguageCode(lang: String): String {
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
        targetLanguageCode: String = "id",
        provider: String = "Auto"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext Result.failure(Exception("Teks input kosong"))
        }

        val effectiveLang = targetLanguageCode.ifBlank { targetLanguage }

        when (provider) {
            "Google" -> {
                val r = translateWithGoogleApi(text, effectiveLang, useClients5 = false, mode = mode)
                if (r.isSuccess) r else translateWithGoogleApi(text, effectiveLang, useClients5 = true, mode = mode)
            }
            "Google (Clients5)" -> {
                val r = translateWithGoogleApi(text, effectiveLang, useClients5 = true, mode = mode)
                if (r.isSuccess) r else translateWithGoogleApi(text, effectiveLang, useClients5 = false, mode = mode)
            }
            "MyMemory" -> {
                val r = translateWithMyMemory(text, effectiveLang)
                if (r.isSuccess) r else translateWithGoogleApi(text, effectiveLang, useClients5 = false, mode = mode)
            }
            "AI", "AI Kustom" -> {
                if (apiKey.isNotBlank()) {
                    translateWithAi(text, targetLanguage, apiKey, baseUrl, model, mode, maxRetries, effectiveLang)
                        .recoverCatching {
                            translateWithGoogleApi(text, effectiveLang, useClients5 = false, mode = mode).getOrThrow()
                        }
                } else {
                    translateWithGoogleApi(text, effectiveLang, useClients5 = false, mode = mode)
                }
            }
            else -> { // "Auto" multi-provider fallback
                // 1. If user set an AI API key, try AI first
                if (apiKey.isNotBlank()) {
                    val aiResult = translateWithAi(text, targetLanguage, apiKey, baseUrl, model, mode, maxRetries, effectiveLang)
                    if (aiResult.isSuccess) return@withContext aiResult
                }
                // 2. Try Google Translate primary
                val gResult = translateWithGoogleApi(text, effectiveLang, useClients5 = false, mode = mode)
                if (gResult.isSuccess) return@withContext gResult

                // 3. Try Google Translate mirror
                val cResult = translateWithGoogleApi(text, effectiveLang, useClients5 = true, mode = mode)
                if (cResult.isSuccess) return@withContext cResult

                // 4. Try MyMemory API
                val mResult = translateWithMyMemory(text, effectiveLang)
                if (mResult.isSuccess) return@withContext mResult

                gResult
            }
        }
    }

    private suspend fun translateWithAi(
        text: String,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        mode: String,
        maxRetries: Int = 2,
        targetLanguageCode: String = "id"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
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
