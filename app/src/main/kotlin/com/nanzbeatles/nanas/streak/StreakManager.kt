/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.streak

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import com.nanzbeatles.nanas.constants.StreakCountKey
import com.nanzbeatles.nanas.constants.StreakFreezeResetKey
import com.nanzbeatles.nanas.constants.StreakHistoryEpochDaysKey
import com.nanzbeatles.nanas.constants.StreakInitializedZeroV2Key
import com.nanzbeatles.nanas.constants.StreakLastListenEpochDayKey
import com.nanzbeatles.nanas.constants.StreakLongestKey
import com.nanzbeatles.nanas.extensions.tryOrNull
import com.nanzbeatles.nanas.extensions.zipInputStream
import com.nanzbeatles.nanas.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

enum class StreakTier(
    val minLevel: Int,
    val maxLevel: Int?,
    val title: String,
    val badgeLabel: String,
    val levelRangeText: String,
    val description: String,
    val outerColor: Color,
    val midColor: Color,
    val coreColor: Color,
    val glowColor: Color,
    val thicknessScale: Float = 1.0f,
    val isThickFlame: Boolean = false,
) {
    UNLIT(
        minLevel = 0,
        maxLevel = 0,
        title = "Percikan",
        badgeLabel = "Belum Menyala",
        levelRangeText = "0 Hari",
        description = "Putar musik sekali sehari sebelum jam 00:00 untuk menyalakan api streak.",
        outerColor = Color(0xFF525866),
        midColor = Color(0xFF7B8394),
        coreColor = Color(0xFFB8C0D0),
        glowColor = Color(0xFF64748B),
        thicknessScale = 0.90f,
        isThickFlame = false,
    ),
    YELLOW_SPARK(
        minLevel = 1,
        maxLevel = 10,
        title = "Percikan",
        badgeLabel = "Api Kuning Biasa",
        levelRangeText = "1–10 Hari",
        description = "Api kuning hangat dari kebiasaan mendengarkan musik setiap hari.",
        outerColor = Color(0xFFFFB300),
        midColor = Color(0xFFFFD54F),
        coreColor = Color(0xFFFFF9C4),
        glowColor = Color(0xFFFFC107),
        thicknessScale = 1.0f,
        isThickFlame = false,
    ),
    ORANGE_BLAZE(
        minLevel = 11,
        maxLevel = 30,
        title = "Kobaran Oren",
        badgeLabel = "Api Oren",
        levelRangeText = "11–30 Hari",
        description = "Nyala api oren terang yang semakin stabil dan bertenaga.",
        outerColor = Color(0xFFFF6D00),
        midColor = Color(0xFFFF9E22),
        coreColor = Color(0xFFFFECB3),
        glowColor = Color(0xFFFF7A00),
        thicknessScale = 1.08f,
        isThickFlame = false,
    ),
    DEEP_ORANGE_THICK(
        minLevel = 31,
        maxLevel = 70,
        title = "Bara Membara",
        badgeLabel = "Api Oren Tua Tebal",
        levelRangeText = "31–70 Hari",
        description = "Api oren tua berlapis tebal dengan panas membara yang intens.",
        outerColor = Color(0xFFD83300),
        midColor = Color(0xFFFF5722),
        coreColor = Color(0xFFFFCC80),
        glowColor = Color(0xFFE64A19),
        thicknessScale = 1.24f,
        isThickFlame = true,
    ),
    MYSTIC_PURPLE(
        minLevel = 71,
        maxLevel = 100,
        title = "Api Ungu Mistis",
        badgeLabel = "Api Ungu",
        levelRangeText = "71–100 Hari",
        description = "Api ungu berenergi tinggi milik pendengar musik sejati.",
        outerColor = Color(0xFF6D28D9),
        midColor = Color(0xFFA855F7),
        coreColor = Color(0xFFF3E8FF),
        glowColor = Color(0xFF9333EA),
        thicknessScale = 1.22f,
        isThickFlame = true,
    ),
    PLASMA_BLUE(
        minLevel = 101,
        maxLevel = 200,
        title = "Plasma Biru",
        badgeLabel = "Api Biru Plasma",
        levelRangeText = "101–200 Hari",
        description = "Api plasma biru elektrik di atas suhu maksimum biasa.",
        outerColor = Color(0xFF0284C7),
        midColor = Color(0xFF00E5FF),
        coreColor = Color(0xFFE0F2FE),
        glowColor = Color(0xFF38BDF8),
        thicknessScale = 1.26f,
        isThickFlame = true,
    ),
    COSMIC_ETERNAL(
        minLevel = 201,
        maxLevel = null,
        title = "Api Kosmik Abadi",
        badgeLabel = "Api Kosmik Abadi",
        levelRangeText = "201+ Hari",
        description = "Tingkat tertinggi yang menyatu dengan galaksi musik tanpa henti.",
        outerColor = Color(0xFFDB2777),
        midColor = Color(0xFF8B5CF6),
        coreColor = Color(0xFFFEF08A),
        glowColor = Color(0xFFF43F5E),
        thicknessScale = 1.32f,
        isThickFlame = true,
    );

    val selectionId: String
        get() = "TIER_$name"

    fun toVisual(): StreakFlameVisual = StreakFlameVisual(
        id = selectionId,
        title = title,
        badgeLabel = badgeLabel,
        levelRangeText = levelRangeText,
        description = description,
        minLevel = minLevel,
        outerColor = outerColor,
        midColor = midColor,
        coreColor = coreColor,
        glowColor = glowColor,
        thicknessScale = thicknessScale,
        isThickFlame = isThickFlame,
        isUnlit = this == UNLIT,
    )

    companion object {
        val evolutionTiers = listOf(
            YELLOW_SPARK,
            ORANGE_BLAZE,
            DEEP_ORANGE_THICK,
            MYSTIC_PURPLE,
            PLASMA_BLUE,
            COSMIC_ETERNAL,
        )

        fun fromStreak(streak: Int): StreakTier = when {
            streak <= 0 -> UNLIT
            streak in 1..10 -> YELLOW_SPARK
            streak in 11..30 -> ORANGE_BLAZE
            streak in 31..70 -> DEEP_ORANGE_THICK
            streak in 71..100 -> MYSTIC_PURPLE
            streak in 101..200 -> PLASMA_BLUE
            else -> COSMIC_ETERNAL
        }
    }
}

/**
 * Unified visual representation used by [StreakFlameIcon], [HomeStreakCard], and [StreakDetailModalDialog].
 */
data class StreakFlameVisual(
    val id: String,
    val title: String,
    val badgeLabel: String,
    val levelRangeText: String,
    val description: String,
    val minLevel: Int,
    val outerColor: Color,
    val midColor: Color,
    val coreColor: Color,
    val glowColor: Color,
    val thicknessScale: Float = 1.0f,
    val isThickFlame: Boolean = false,
    val isUnlit: Boolean = false,
)

data class StreakState(
    val currentStreak: Int = 0,
    val previousStreak: Int = 0,
    val lastListenEpochDay: Long = -1L,
    val longestStreak: Int = 0,
    val historyEpochDays: Set<Long> = emptySet(),
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val freezeReset: Boolean = false,
    val animationTriggerToken: Int = 0,
) {
    val listenedToday: Boolean
        get() = lastListenEpochDay == todayEpochDay && currentStreak > 0

    val tier: StreakTier
        get() = StreakTier.fromStreak(currentStreak)

    val previousTier: StreakTier
        get() = StreakTier.fromStreak(previousStreak)

    val nextTier: StreakTier?
        get() = StreakTier.evolutionTiers.firstOrNull { it.minLevel > currentStreak }

    /**
     * Progress (0f..1f) within the current tier toward the next tier evolution.
     */
    val tierProgress: Float
        get() {
            if (currentStreak <= 0) return 0f
            val currentMin = tier.minLevel
            val currentMax = tier.maxLevel ?: return 1f
            val span = (currentMax - currentMin + 1).coerceAtLeast(1)
            return ((currentStreak - currentMin + 1).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }

    /**
     * Flame visual is strictly determined by the current level's [StreakTier].
     */
    val activeFlame: StreakFlameVisual
        get() = tier.toVisual()
}

object StreakManager {
    const val STREAK_FILENAME = "streak_data.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialized = AtomicBoolean(false)

    private val _state = MutableStateFlow(StreakState())
    val state: StateFlow<StreakState> = _state.asStateFlow()

    fun ensureInitialized(context: Context) {
        val appContext = context.applicationContext
        if (initialized.compareAndSet(false, true)) {
            scope.launch {
                loadAndEvaluate(appContext)
                startMidnightWatcher(appContext)
            }
        } else {
            scope.launch {
                evaluateMidnightReset(appContext)
            }
        }
    }

    private fun startMidnightWatcher(appContext: Context) {
        scope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val millisUntilMidnight = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
                delay(minOf(millisUntilMidnight + 500L, 30_000L))
                evaluateMidnightReset(appContext)
            }
        }
    }

    suspend fun loadAndEvaluate(context: Context): StreakState = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = appContext.dataStore.data.first()
        val initializedZeroV2 = prefs[StreakInitializedZeroV2Key] ?: false
        var count = prefs[StreakCountKey] ?: 0
        var lastDay = prefs[StreakLastListenEpochDayKey] ?: -1L
        var longest = prefs[StreakLongestKey] ?: 0
        var history = parseHistory(prefs[StreakHistoryEpochDaysKey].orEmpty())
        var freezeReset = prefs[StreakFreezeResetKey] ?: false
        val today = LocalDate.now().toEpochDay()
        val file = File(appContext.filesDir, STREAK_FILENAME)

        if (!initializedZeroV2) {
            // Reset any previously seeded 32-day / 47-longest dummy streak back to 0
            if (count == 32 && longest == 47) {
                count = 0
                lastDay = -1L
                longest = 0
                history = emptySet()
                runCatching { if (file.exists()) file.delete() }
            }
        } else if (count == 0 && lastDay == -1L && file.exists()) {
            // If DataStore has no streak yet, check if streak_data.json exists (e.g. from file restore)
            runCatching {
                val parsed = parseJson(file.readText())
                if (parsed != null && !(parsed.currentStreak == 32 && parsed.longestStreak == 47)) {
                    count = parsed.currentStreak
                    lastDay = parsed.lastListenEpochDay
                    longest = maxOf(longest, parsed.longestStreak)
                    history = (history + parsed.historyEpochDays).toSortedSet()
                    freezeReset = freezeReset || parsed.freezeReset
                }
            }
        }

        // Reset at 00:00 if more than 1 day has elapsed since lastListenEpochDay (unless freezeReset is active)
        if (!freezeReset && lastDay >= 0L && today - lastDay > 1L && count > 0) {
            count = 0
        }

        longest = maxOf(longest, count)
        val newState = StreakState(
            currentStreak = count,
            previousStreak = count,
            lastListenEpochDay = lastDay,
            longestStreak = longest,
            historyEpochDays = history,
            todayEpochDay = today,
            freezeReset = freezeReset,
            animationTriggerToken = 0,
        )
        _state.value = newState
        persistState(appContext, newState)
        newState
    }

    suspend fun evaluateMidnightReset(context: Context): StreakState = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val current = _state.value
        val today = LocalDate.now().toEpochDay()
        val shouldReset = !current.freezeReset &&
            current.lastListenEpochDay >= 0L &&
            today - current.lastListenEpochDay > 1L &&
            current.currentStreak > 0

        if (shouldReset || current.todayEpochDay != today) {
            val nextCount = if (shouldReset) 0 else current.currentStreak
            val updated = current.copy(
                previousStreak = current.currentStreak,
                currentStreak = nextCount,
                todayEpochDay = today,
            )
            _state.value = updated
            persistState(appContext, updated)
            updated
        } else {
            current
        }
    }

    /**
     * Called when the user plays music. Increments the streak at most once per calendar day
     * (local time, resetting day boundary at 00:00).
     */
    fun onMusicPlayed(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val loaded = if (!initialized.get()) {
                initialized.set(true)
                startMidnightWatcher(appContext)
                loadAndEvaluate(appContext)
            } else {
                evaluateMidnightReset(appContext)
            }

            val today = LocalDate.now().toEpochDay()
            if (loaded.lastListenEpochDay == today && loaded.currentStreak > 0) {
                return@launch
            }

            val newStreak = when {
                loaded.lastListenEpochDay == today - 1L && loaded.currentStreak > 0 -> loaded.currentStreak + 1
                loaded.freezeReset && loaded.currentStreak > 0 -> loaded.currentStreak + 1
                else -> 1
            }
            val newLongest = maxOf(loaded.longestStreak, newStreak)
            val newHistory = (loaded.historyEpochDays + today)
                .filter { today - it <= 365L }
                .toSortedSet()

            val updated = loaded.copy(
                previousStreak = loaded.currentStreak,
                currentStreak = newStreak,
                lastListenEpochDay = today,
                longestStreak = newLongest,
                historyEpochDays = newHistory,
                todayEpochDay = today,
                animationTriggerToken = loaded.animationTriggerToken + 1,
            )
            _state.value = updated
            persistState(appContext, updated)
            Timber.tag("StreakManager").i("Music streak updated: day=$newStreak (flame=${updated.activeFlame.title})")
        }
    }

    /**
     * Allows the user to customize their streak level/day count specifically to see the
     * level-up and color-evolution animations across the fixed StreakTiers.
     */
    fun setCustomStreakCount(
        context: Context,
        days: Int,
        longestDays: Int? = null,
        markListenedToday: Boolean = true,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val current = _state.value
            val today = LocalDate.now().toEpochDay()
            val coercedDays = days.coerceIn(0, 999_999)
            val newLongest = (longestDays ?: maxOf(current.longestStreak, coercedDays)).coerceAtLeast(coercedDays)
            val newLastDay = when {
                coercedDays == 0 -> -1L
                markListenedToday -> today
                current.lastListenEpochDay >= 0L -> current.lastListenEpochDay
                else -> today
            }
            val newHistory = if (coercedDays > 0 && markListenedToday) {
                (current.historyEpochDays + today).toSortedSet()
            } else {
                current.historyEpochDays
            }
            val updated = current.copy(
                previousStreak = current.currentStreak,
                currentStreak = coercedDays,
                lastListenEpochDay = newLastDay,
                longestStreak = newLongest,
                historyEpochDays = newHistory,
                todayEpochDay = today,
                animationTriggerToken = current.animationTriggerToken + 1,
            )
            _state.value = updated
            persistState(appContext, updated)
        }
    }

    /**
     * Re-triggers the level-up / color-evolution burst animation on the current level.
     */
    fun replayLevelAnimation() {
        val current = _state.value
        val prevLevelForReplay = when {
            current.tier == StreakTier.UNLIT -> 0
            current.tier == StreakTier.YELLOW_SPARK -> 0
            else -> (current.tier.minLevel - 1).coerceAtLeast(1)
        }
        _state.value = current.copy(
            previousStreak = prevLevelForReplay,
            animationTriggerToken = current.animationTriggerToken + 1,
        )
    }

    fun setFreezeReset(context: Context, freeze: Boolean) {
        val appContext = context.applicationContext
        scope.launch {
            val updated = _state.value.copy(freezeReset = freeze)
            _state.value = updated
            persistState(appContext, updated)
        }
    }

    private suspend fun persistState(context: Context, state: StreakState) {
        val historyCsv = state.historyEpochDays.sorted().takeLast(365).joinToString(",")
        runCatching {
            context.dataStore.edit { prefs ->
                prefs[StreakInitializedZeroV2Key] = true
                prefs[StreakCountKey] = state.currentStreak
                prefs[StreakLastListenEpochDayKey] = state.lastListenEpochDay
                prefs[StreakLongestKey] = state.longestStreak
                prefs[StreakHistoryEpochDaysKey] = historyCsv
                prefs[StreakFreezeResetKey] = state.freezeReset
            }
        }
        runCatching {
            val file = File(context.filesDir, STREAK_FILENAME)
            file.writeText(toJsonString(state))
        }
    }

    fun writeBackupFileSync(context: Context): File {
        val file = File(context.filesDir, STREAK_FILENAME)
        runCatching {
            file.writeText(toJsonString(_state.value))
        }
        return file
    }

    fun restoreFromBackupJsonSync(
        context: Context,
        rawJson: String,
        ignoreElapsedReset: Boolean = false,
    ): Boolean {
        val parsed = parseJson(rawJson) ?: return false
        val today = LocalDate.now().toEpochDay()
        val effectiveStreak = if (
            !ignoreElapsedReset &&
            !parsed.freezeReset &&
            parsed.lastListenEpochDay >= 0L &&
            today - parsed.lastListenEpochDay > 1L
        ) {
            0
        } else {
            parsed.currentStreak
        }
        val current = _state.value
        val restored = parsed.copy(
            previousStreak = current.currentStreak,
            currentStreak = effectiveStreak,
            lastListenEpochDay = if (ignoreElapsedReset && effectiveStreak > 0) today else parsed.lastListenEpochDay,
            longestStreak = maxOf(parsed.longestStreak, effectiveStreak),
            todayEpochDay = today,
            animationTriggerToken = current.animationTriggerToken + 1,
        )
        _state.value = restored
        runCatching {
            File(context.filesDir, STREAK_FILENAME).writeText(toJsonString(restored))
        }
        scope.launch {
            persistState(context.applicationContext, restored)
        }
        return true
    }

    suspend fun exportStreakToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val current = evaluateMidnightReset(context)
            val json = toJsonString(current)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext false
            true
        }.getOrElse {
            Timber.tag("StreakManager").e(it, "Failed to export streak file")
            false
        }
    }

    suspend fun importStreakFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext false

            val rawText = rawBytes.toString(Charsets.UTF_8).trim()
            if (rawText.startsWith("{")) {
                return@withContext restoreFromBackupJsonSync(context, rawText, ignoreElapsedReset = true)
            }

            rawBytes.inputStream().zipInputStream().use { zipIn ->
                var entry = tryOrNull { zipIn.nextEntry }
                while (entry != null) {
                    if (entry.name == STREAK_FILENAME) {
                        val entryJson = zipIn.readBytes().toString(Charsets.UTF_8)
                        return@withContext restoreFromBackupJsonSync(context, entryJson, ignoreElapsedReset = true)
                    }
                    entry = tryOrNull { zipIn.nextEntry }
                }
            }
            false
        }.getOrElse {
            Timber.tag("StreakManager").e(it, "Failed to import streak file")
            false
        }
    }

    private fun toJsonString(state: StreakState): String {
        val obj = JSONObject().apply {
            put("version", 2)
            put("currentStreak", state.currentStreak)
            put("lastListenEpochDay", state.lastListenEpochDay)
            put("longestStreak", state.longestStreak)
            put("historyEpochDays", JSONArray(state.historyEpochDays.sorted()))
            put("freezeReset", state.freezeReset)
            put("updatedAt", System.currentTimeMillis())
        }
        return obj.toString(2)
    }

    private fun parseJson(raw: String): StreakState? = runCatching {
        val obj = JSONObject(raw)
        val count = obj.optInt("currentStreak", 0).coerceAtLeast(0)
        val lastDay = obj.optLong("lastListenEpochDay", -1L)
        val longest = obj.optInt("longestStreak", count).coerceAtLeast(count)
        val historySet = mutableSetOf<Long>()
        val arr = obj.optJSONArray("historyEpochDays")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                historySet.add(arr.optLong(i))
            }
        }
        if (lastDay >= 0L) historySet.add(lastDay)
        StreakState(
            currentStreak = count,
            previousStreak = count,
            lastListenEpochDay = lastDay,
            longestStreak = longest,
            historyEpochDays = historySet,
            todayEpochDay = LocalDate.now().toEpochDay(),
            freezeReset = obj.optBoolean("freezeReset", false),
        )
    }.getOrNull()

    private fun parseHistory(csv: String): Set<Long> =
        csv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
}
