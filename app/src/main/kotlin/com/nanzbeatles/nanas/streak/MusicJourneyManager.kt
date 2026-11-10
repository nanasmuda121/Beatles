/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.streak

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import com.nanzbeatles.nanas.constants.ProfileJourneyJsonV2Key
import com.nanzbeatles.nanas.constants.ProfileLocalBioV2Key
import com.nanzbeatles.nanas.constants.ProfileLocalHandleV2Key
import com.nanzbeatles.nanas.constants.ProfileLocalNameV2Key
import com.nanzbeatles.nanas.extensions.tryOrNull
import com.nanzbeatles.nanas.extensions.zipInputStream
import com.nanzbeatles.nanas.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class ListenerLevelSpec(
    val level: Int,
    val title: String,
    val xpThreshold: Int,
    val nextXpTarget: Int,
    val subtitle: String,
)

enum class SummaryPeriodFilter(val label: String) {
    ALL("Semua"),
    DAY("Hari"),
    WEEK("Minggu"),
    MONTH("Bulan"),
    YEAR("Tahun"),
}

data class TopSongStatItem(
    val songId: String,
    val title: String,
    val artist: String,
    val playCount: Int,
    val listeningSeconds: Long,
    val thumbnailUrl: String? = null,
)

data class MonthlyRecordEntry(
    val yearMonthKey: String, // e.g. "2026-11"
    val displayTitle: String, // e.g. "November 2026"
    val listeningSeconds: Long,
) {
    val hoursInt: Int
        get() = (listeningSeconds / 3600L).toInt()
}

data class DailyRecordEntry(
    val epochDay: Long,
    val displayDate: String,
    val listeningSeconds: Long,
)

data class MusicJourneyState(
    val totalListeningSeconds: Long = 0L,
    val totalSongsPlayed: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val dailyRecordSeconds: Long = 0L,
    val monthlyRecordSeconds: Long = 0L,
    val xp: Int = 0,
    val level: Int = 1,
    val topSongs: List<TopSongStatItem> = emptyList(),
    val monthlyHistory: List<MonthlyRecordEntry> = emptyList(),
    val dailyHistory: List<DailyRecordEntry> = emptyList(),
    // Sen (0) .. Min (6) listening seconds
    val weekdaySeconds: List<Long> = listOf(0L, 0L, 0L, 0L, 0L, 0L, 0L),
) {
    val levelSpec: ListenerLevelSpec
        get() = MusicJourneyManager.resolveLevelSpec(xp, null)

    val xpTargetForCurrentBanner: Int
        get() = levelSpec.nextXpTarget

    val xpProgressFraction: Float
        get() {
            if (xp <= 0) return 0f
            val target = xpTargetForCurrentBanner.coerceAtLeast(1)
            return (xp.toFloat() / target.toFloat()).coerceIn(0.03f, 1f)
        }

    val totalHours: Int
        get() = (totalListeningSeconds / 3600L).toInt()

    val totalMinutes: Int
        get() = ((totalListeningSeconds % 3600L) / 60L).toInt()

    /**
     * Compact format for the 3-column card (e.g., "0 Jam", "15 Menit", "8.7 Jam", or "9 Jam").
     */
    val dailyRecordCompactText: String
        get() {
            if (dailyRecordSeconds <= 0L) return "0 Jam"
            if (dailyRecordSeconds < 3600L) {
                val mins = (dailyRecordSeconds / 60L).coerceAtLeast(1L)
                return "$mins Menit"
            }
            val hours = dailyRecordSeconds / 3600.0
            val wholeHours = dailyRecordSeconds / 3600L
            val remMinutes = (dailyRecordSeconds % 3600L) / 60L
            return if (remMinutes == 0L) {
                "$wholeHours Jam"
            } else {
                String.format(Locale.US, "%.1f Jam", hours)
            }
        }

    /**
     * Full descriptive format (e.g., "0 Menit", "8 Jam 42 Menit", or "9 Jam").
     */
    val dailyRecordFullText: String
        get() {
            if (dailyRecordSeconds <= 0L) return "0 Menit"
            val hours = dailyRecordSeconds / 3600L
            val minutes = ((dailyRecordSeconds % 3600L) / 60L).let {
                if (hours == 0L && it == 0L) 1L else it
            }
            return when {
                hours > 0L && minutes > 0L -> "$hours Jam $minutes Menit"
                hours > 0L -> "$hours Jam"
                else -> "$minutes Menit"
            }
        }

    val monthlyRecordHoursText: String
        get() {
            if (monthlyRecordSeconds <= 0L) return "0 Jam"
            if (monthlyRecordSeconds < 3600L) {
                val mins = (monthlyRecordSeconds / 60L).coerceAtLeast(1L)
                return "$mins Menit"
            }
            return "${monthlyRecordSeconds / 3600L} Jam"
        }

    fun summaryForPeriod(period: SummaryPeriodFilter): Pair<Long, Int> {
        return when (period) {
            SummaryPeriodFilter.ALL -> totalListeningSeconds to totalSongsPlayed
            SummaryPeriodFilter.DAY -> {
                val todayEpoch = LocalDate.now().toEpochDay()
                val todaySec = dailyHistory.firstOrNull { it.epochDay == todayEpoch }?.listeningSeconds ?: 0L
                val songs = if (todaySec > 0L) (todaySec / 180L).toInt().coerceAtLeast(1) else 0
                todaySec to songs
            }
            SummaryPeriodFilter.WEEK -> {
                val weekSec = weekdaySeconds.sum()
                val songs = if (weekSec > 0L) (weekSec / 180L).toInt().coerceAtLeast(1) else 0
                weekSec to songs
            }
            SummaryPeriodFilter.MONTH -> {
                val ym = YearMonth.now()
                val ymKey = String.format(Locale.US, "%04d-%02d", ym.year, ym.monthValue)
                val monthSec = monthlyHistory.firstOrNull { it.yearMonthKey == ymKey }?.listeningSeconds ?: 0L
                val songs = if (monthSec > 0L) (monthSec / 180L).toInt().coerceAtLeast(1) else 0
                monthSec to songs
            }
            SummaryPeriodFilter.YEAR -> {
                totalListeningSeconds to totalSongsPlayed
            }
        }
    }
}

object MusicJourneyManager {
    const val JOURNEY_FILENAME = "profile_journey_data_v2.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialized = AtomicBoolean(false)

    private val _state = MutableStateFlow(MusicJourneyState())
    val state: StateFlow<MusicJourneyState> = _state.asStateFlow()

    /**
     * Exact 10-Level system specified by the user:
     * Level 1: Pendengar Pemula (0 XP)
     * Level 2: Penikmat Musik (500 XP)
     * Level 3: Pendengar Aktif (1.000 XP)
     * Level 4: Pecinta Musik (1.750 XP)
     * Level 5: Pendengar Setia (2.500 XP)
     * Level 6: Musik Enthusiast (3.500 XP)
     * Level 7: Pendengar Hebat (5.000 XP)
     * Level 8: Master Musik (7.500 XP)
     * Level 9: Legenda Musik (10.000 XP)
     * Level 10: Ikon Musik (15.000+ XP)
     */
    val allLevels: List<ListenerLevelSpec> = listOf(
        ListenerLevelSpec(1, "Pendengar Pemula", 0, 500, "Perjalanan musikmu baru dimulai!"),
        ListenerLevelSpec(2, "Penikmat Musik", 500, 1_000, "Mulai menemukan ritme harianmu!"),
        ListenerLevelSpec(3, "Pendengar Aktif", 1_000, 1_750, "Musik selalu menemani aktivitasmu!"),
        ListenerLevelSpec(4, "Pecinta Musik", 1_750, 2_500, "Playlist favoritmu tak pernah berhenti!"),
        ListenerLevelSpec(5, "Pendengar Setia", 2_500, 3_500, "Setia bersama alunan nada setiap hari!"),
        ListenerLevelSpec(6, "Musik Enthusiast", 3_500, 5_000, "Telingamu peka akan karya terbaik!"),
        ListenerLevelSpec(7, "Pendengar Hebat", 5_000, 7_500, "Musik adalah hidupmu!"),
        ListenerLevelSpec(8, "Master Musik", 7_500, 10_000, "Penguasa segala genre dan suasana!"),
        ListenerLevelSpec(9, "Legenda Musik", 10_000, 15_000, "Dedikasi mendengarkan yang legendaris!"),
        ListenerLevelSpec(10, "Ikon Musik", 15_000, 15_000, "Puncak tertinggi pencinta musik sejati!"),
    )

    fun resolveLevelSpec(xp: Int, explicitLevel: Int? = null): ListenerLevelSpec {
        if (explicitLevel != null && explicitLevel in 1..10) {
            return allLevels[explicitLevel - 1]
        }
        return when {
            xp < 500 -> allLevels[0]
            xp < 1_000 -> allLevels[1]
            xp < 1_750 -> allLevels[2]
            xp < 2_500 -> allLevels[3]
            xp < 3_500 -> allLevels[4]
            xp < 5_000 -> allLevels[5]
            xp < 7_500 -> allLevels[6]
            xp < 10_000 -> allLevels[7]
            xp < 15_000 -> allLevels[8]
            else -> allLevels[9].copy(nextXpTarget = maxOf(15_000, ((xp / 5000) + 1) * 5000))
        }
    }

    fun ensureInitialized(context: Context) {
        val appContext = context.applicationContext
        if (initialized.compareAndSet(false, true)) {
            scope.launch {
                loadState(appContext)
            }
        }
    }

    suspend fun loadState(context: Context): MusicJourneyState = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = appContext.dataStore.data.first()
        val rawJson = prefs[ProfileJourneyJsonV2Key]
        val file = File(appContext.filesDir, JOURNEY_FILENAME)

        val parsed = when {
            !rawJson.isNullOrBlank() -> parseJson(rawJson)
            file.exists() -> runCatching { parseJson(file.readText()) }.getOrNull()
            else -> null
        }

        val streakState = StreakManager.state.value
        val base = parsed ?: MusicJourneyState()
        val synced = base.copy(
            currentStreak = streakState.currentStreak,
            longestStreak = maxOf(streakState.longestStreak, base.longestStreak),
        )
        _state.value = synced
        persistState(appContext, synced)
        synced
    }

    /**
     * Called when a song is played or playback time is recorded in MusicService.
     * Updates totalListeningSeconds, totalSongsPlayed, dailyRecordSeconds, monthlyRecordSeconds,
     * weekday pattern, XP, Level, and per-song playCount.
     */
    fun onSongPlayed(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        listenedMs: Long,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            if (!initialized.get()) {
                initialized.set(true)
                loadState(appContext)
            }
            val current = _state.value
            val addSeconds = (listenedMs / 1000L).coerceAtLeast(30L)
            val newTotalSeconds = current.totalListeningSeconds + addSeconds
            val newTotalSongs = current.totalSongsPlayed + 1

            // Update today's listening duration and check if it beats dailyRecordSeconds
            val todayEpoch = LocalDate.now().toEpochDay()
            val updatedDailyList = current.dailyHistory.toMutableList()
            val todayIdx = updatedDailyList.indexOfFirst { it.epochDay == todayEpoch }
            val newTodaySeconds = if (todayIdx >= 0) {
                val updatedEntry = updatedDailyList[todayIdx].copy(
                    listeningSeconds = updatedDailyList[todayIdx].listeningSeconds + addSeconds
                )
                updatedDailyList[todayIdx] = updatedEntry
                updatedEntry.listeningSeconds
            } else {
                val entry = DailyRecordEntry(todayEpoch, "Hari Ini", addSeconds)
                updatedDailyList.add(0, entry)
                addSeconds
            }
            val newDailyRecord = maxOf(current.dailyRecordSeconds, newTodaySeconds)

            // Update current month's listening duration and check if it beats monthlyRecordSeconds
            val ym = YearMonth.now()
            val ymKey = String.format(Locale.US, "%04d-%02d", ym.year, ym.monthValue)
            val monthNamesId = arrayOf(
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
            )
            val ymTitle = "${monthNamesId[(ym.monthValue - 1).coerceIn(0, 11)]} ${ym.year}"
            val updatedMonthlyList = current.monthlyHistory.toMutableList()
            val monthIdx = updatedMonthlyList.indexOfFirst { it.yearMonthKey == ymKey }
            val newMonthSeconds = if (monthIdx >= 0) {
                val updatedMonth = updatedMonthlyList[monthIdx].copy(
                    listeningSeconds = updatedMonthlyList[monthIdx].listeningSeconds + addSeconds
                )
                updatedMonthlyList[monthIdx] = updatedMonth
                updatedMonth.listeningSeconds
            } else {
                val newEntry = MonthlyRecordEntry(ymKey, ymTitle, addSeconds)
                updatedMonthlyList.add(0, newEntry)
                addSeconds
            }
            val newMonthlyRecord = maxOf(current.monthlyRecordSeconds, newMonthSeconds)

            // Update weekday pattern (Monday = 0 .. Sunday = 6)
            val dowIndex = (LocalDate.now().dayOfWeek.value - DayOfWeek.MONDAY.value).coerceIn(0, 6)
            val updatedWeekdays = current.weekdaySeconds.toMutableList()
            if (dowIndex in updatedWeekdays.indices) {
                updatedWeekdays[dowIndex] = updatedWeekdays[dowIndex] + addSeconds
            }

            // Update song play count in topSongs
            val updatedSongs = current.topSongs.toMutableList()
            val existingSongIdx = updatedSongs.indexOfFirst {
                it.songId == songId || (it.title.equals(title, ignoreCase = true) && it.artist.equals(artist, ignoreCase = true))
            }
            if (existingSongIdx >= 0) {
                val prev = updatedSongs[existingSongIdx]
                updatedSongs[existingSongIdx] = prev.copy(
                    playCount = prev.playCount + 1,
                    listeningSeconds = prev.listeningSeconds + addSeconds,
                    thumbnailUrl = thumbnailUrl ?: prev.thumbnailUrl
                )
            } else if (title.isNotBlank()) {
                updatedSongs.add(
                    TopSongStatItem(
                        songId = songId,
                        title = title,
                        artist = artist.ifBlank { "Artis" },
                        playCount = 1,
                        listeningSeconds = addSeconds,
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }
            updatedSongs.sortByDescending { it.playCount }

            // Award XP: +10 XP per song played + 1 XP per minute listened
            val xpGain = 10 + (addSeconds / 60L).toInt().coerceAtLeast(1)
            val newXp = current.xp + xpGain
            val newSpec = resolveLevelSpec(newXp, null)
            val streakState = StreakManager.state.value

            val updated = current.copy(
                totalListeningSeconds = newTotalSeconds,
                totalSongsPlayed = newTotalSongs,
                currentStreak = streakState.currentStreak,
                longestStreak = maxOf(streakState.longestStreak, current.longestStreak),
                dailyRecordSeconds = newDailyRecord,
                monthlyRecordSeconds = newMonthlyRecord,
                xp = newXp,
                level = newSpec.level,
                topSongs = updatedSongs.take(15),
                monthlyHistory = updatedMonthlyList,
                dailyHistory = updatedDailyList.take(14),
                weekdaySeconds = updatedWeekdays,
            )
            _state.value = updated
            persistState(appContext, updated)
        }
    }

    fun updateCustomStats(
        context: Context,
        totalHours: Int,
        totalSongs: Int,
        dailyRecordMinutes: Int,
        monthlyRecordHours: Int,
        xp: Int,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val current = _state.value
            val coercedXp = xp.coerceAtLeast(0)
            val newSpec = resolveLevelSpec(coercedXp, null)
            val updated = current.copy(
                totalListeningSeconds = totalHours.coerceAtLeast(0) * 3600L,
                totalSongsPlayed = totalSongs.coerceAtLeast(0),
                dailyRecordSeconds = dailyRecordMinutes.coerceAtLeast(0) * 60L,
                monthlyRecordSeconds = monthlyRecordHours.coerceAtLeast(0) * 3600L,
                xp = coercedXp,
                level = newSpec.level,
            )
            _state.value = updated
            persistState(appContext, updated)
        }
    }

    fun resetAllToZero(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val zeroState = MusicJourneyState()
            _state.value = zeroState
            StreakManager.setCustomStreakCount(appContext, days = 0, longestDays = 0)
            persistState(appContext, zeroState)
        }
    }

    private suspend fun persistState(context: Context, state: MusicJourneyState) {
        val json = toJsonString(state)
        runCatching {
            context.dataStore.edit { prefs ->
                prefs[ProfileJourneyJsonV2Key] = json
            }
        }
        runCatching {
            File(context.filesDir, JOURNEY_FILENAME).writeText(json)
        }
    }

    fun writeBackupFileSync(context: Context): File {
        val file = File(context.filesDir, JOURNEY_FILENAME)
        runCatching {
            file.writeText(toJsonString(_state.value))
        }
        return file
    }

    fun restoreFromBackupJsonSync(context: Context, rawJson: String): Boolean {
        val parsed = parseJson(rawJson) ?: return false
        _state.value = parsed
        runCatching {
            File(context.filesDir, JOURNEY_FILENAME).writeText(toJsonString(parsed))
        }
        scope.launch {
            persistState(context.applicationContext, parsed)
        }
        return true
    }

    /**
     * Exports all statistics (MusicJourneyState + Streak + local Profile metadata) to a user-chosen file URI.
     */
    suspend fun exportAllStatsToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val prefs = appContext.dataStore.data.first()
            val streakState = StreakManager.evaluateMidnightReset(appContext)
            val journeyState = _state.value.copy(
                currentStreak = streakState.currentStreak,
                longestStreak = maxOf(streakState.longestStreak, _state.value.longestStreak),
            )
            val journeyJsonObj = JSONObject(toJsonString(journeyState))
            val streakJsonObj = JSONObject().apply {
                put("version", 2)
                put("currentStreak", streakState.currentStreak)
                put("lastListenEpochDay", streakState.lastListenEpochDay)
                put("longestStreak", streakState.longestStreak)
                put("historyEpochDays", JSONArray(streakState.historyEpochDays.sorted()))
                put("freezeReset", streakState.freezeReset)
            }
            val profileJsonObj = JSONObject().apply {
                put("name", prefs[ProfileLocalNameV2Key] ?: "User")
                put("handle", prefs[ProfileLocalHandleV2Key] ?: "@user")
                put("bio", prefs[ProfileLocalBioV2Key] ?: "Belum ada bio")
            }

            val rootBundle = JSONObject().apply {
                put("backupType", "nanas_music_statistics_v2")
                put("exportedAt", System.currentTimeMillis())
                put("journey", journeyJsonObj)
                put("streak", streakJsonObj)
                put("profile", profileJsonObj)
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(rootBundle.toString(2).toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext false
            true
        }.getOrElse {
            Timber.tag("MusicJourneyManager").e(it, "Failed to export all statistics")
            false
        }
    }

    /**
     * Restores all statistics (MusicJourneyState + Streak + local Profile metadata) from either:
     * 1) A JSON statistics backup file (.json)
     * 2) A full app backup ZIP archive (.backup / .zip)
     */
    suspend fun importAllStatsFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext false

            val rawText = rawBytes.toString(Charsets.UTF_8).trim()
            if (rawText.startsWith("{")) {
                val obj = JSONObject(rawText)
                var restoredAny = false

                // Case A: Full statistics bundle ("journey" + "streak" + "profile")
                val journeyObj = obj.optJSONObject("journey")
                if (journeyObj != null) {
                    restoredAny = restoreFromBackupJsonSync(appContext, journeyObj.toString()) || restoredAny
                }
                val streakObj = obj.optJSONObject("streak")
                if (streakObj != null) {
                    restoredAny = StreakManager.restoreFromBackupJsonSync(
                        appContext,
                        streakObj.toString(),
                        ignoreElapsedReset = true
                    ) || restoredAny
                }
                val profileObj = obj.optJSONObject("profile")
                if (profileObj != null) {
                    appContext.dataStore.edit { prefs ->
                        profileObj.optString("name").takeIf { it.isNotBlank() }?.let {
                            prefs[ProfileLocalNameV2Key] = it
                        }
                        profileObj.optString("handle").takeIf { it.isNotBlank() }?.let {
                            prefs[ProfileLocalHandleV2Key] = it
                        }
                        profileObj.optString("bio").takeIf { it.isNotBlank() }?.let {
                            prefs[ProfileLocalBioV2Key] = it
                        }
                    }
                    restoredAny = true
                }

                // Case B: Direct MusicJourneyState JSON
                if (!restoredAny && obj.has("totalListeningSeconds")) {
                    restoredAny = restoreFromBackupJsonSync(appContext, rawText)
                    if (obj.has("currentStreak")) {
                        StreakManager.setCustomStreakCount(
                            appContext,
                            days = obj.optInt("currentStreak", 0),
                            longestDays = obj.optInt("longestStreak", 0)
                        )
                    }
                }

                // Case C: Direct StreakState JSON
                if (!restoredAny && obj.has("currentStreak")) {
                    restoredAny = StreakManager.restoreFromBackupJsonSync(
                        appContext,
                        rawText,
                        ignoreElapsedReset = true
                    )
                }

                return@withContext restoredAny
            }

            // Otherwise check if it is a .backup / .zip archive
            var restoredFromZip = false
            rawBytes.inputStream().zipInputStream().use { zipIn ->
                var entry = tryOrNull { zipIn.nextEntry }
                while (entry != null) {
                    when (entry.name) {
                        JOURNEY_FILENAME, "profile_journey_data.json" -> {
                            val entryJson = zipIn.readBytes().toString(Charsets.UTF_8)
                            restoredFromZip = restoreFromBackupJsonSync(appContext, entryJson) || restoredFromZip
                        }
                        StreakManager.STREAK_FILENAME -> {
                            val entryJson = zipIn.readBytes().toString(Charsets.UTF_8)
                            restoredFromZip = StreakManager.restoreFromBackupJsonSync(
                                appContext,
                                entryJson,
                                ignoreElapsedReset = true
                            ) || restoredFromZip
                        }
                    }
                    entry = tryOrNull { zipIn.nextEntry }
                }
            }
            restoredFromZip
        }.getOrElse {
            Timber.tag("MusicJourneyManager").e(it, "Failed to import statistics file")
            false
        }
    }

    fun toJsonString(state: MusicJourneyState): String {
        val songsObj = JSONObject()
        state.topSongs.forEach { item ->
            songsObj.put(
                item.songId,
                JSONObject().apply {
                    put("title", item.title)
                    put("artist", item.artist)
                    put("playCount", item.playCount)
                    put("listeningSeconds", item.listeningSeconds)
                    put("thumbnailUrl", item.thumbnailUrl ?: "")
                }
            )
        }
        val monthlyArr = JSONArray()
        state.monthlyHistory.forEach { m ->
            monthlyArr.put(
                JSONObject().apply {
                    put("yearMonthKey", m.yearMonthKey)
                    put("displayTitle", m.displayTitle)
                    put("listeningSeconds", m.listeningSeconds)
                }
            )
        }
        val dailyArr = JSONArray()
        state.dailyHistory.forEach { d ->
            dailyArr.put(
                JSONObject().apply {
                    put("epochDay", d.epochDay)
                    put("displayDate", d.displayDate)
                    put("listeningSeconds", d.listeningSeconds)
                }
            )
        }
        val weekdayArr = JSONArray(state.weekdaySeconds)

        val root = JSONObject().apply {
            put("version", 2)
            put("totalListeningSeconds", state.totalListeningSeconds)
            put("totalSongsPlayed", state.totalSongsPlayed)
            put("currentStreak", state.currentStreak)
            put("longestStreak", state.longestStreak)
            put("dailyRecordSeconds", state.dailyRecordSeconds)
            put("monthlyRecordSeconds", state.monthlyRecordSeconds)
            put("xp", state.xp)
            put("level", state.level)
            put("songs", songsObj)
            put("monthlyHistory", monthlyArr)
            put("dailyHistory", dailyArr)
            put("weekdaySeconds", weekdayArr)
        }
        return root.toString(2)
    }

    private fun parseJson(raw: String): MusicJourneyState? = runCatching {
        val obj = JSONObject(raw)
        val totalSec = obj.optLong("totalListeningSeconds", 0L).coerceAtLeast(0L)
        val totalSongs = obj.optInt("totalSongsPlayed", 0).coerceAtLeast(0)
        val curStreak = obj.optInt("currentStreak", 0).coerceAtLeast(0)
        val longStreak = obj.optInt("longestStreak", curStreak).coerceAtLeast(curStreak)
        val dailyRec = obj.optLong("dailyRecordSeconds", 0L).coerceAtLeast(0L)
        val monthlyRec = obj.optLong("monthlyRecordSeconds", 0L).coerceAtLeast(0L)
        val xp = obj.optInt("xp", 0).coerceAtLeast(0)
        val level = resolveLevelSpec(xp, null).level

        val parsedSongs = mutableListOf<TopSongStatItem>()
        val songsObj = obj.optJSONObject("songs")
        if (songsObj != null) {
            val keys = songsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val s = songsObj.optJSONObject(key) ?: continue
                parsedSongs.add(
                    TopSongStatItem(
                        songId = key,
                        title = s.optString("title", "Unknown"),
                        artist = s.optString("artist", "Unknown"),
                        playCount = s.optInt("playCount", 1),
                        listeningSeconds = s.optLong("listeningSeconds", 180L),
                        thumbnailUrl = s.optString("thumbnailUrl", "").ifBlank { null },
                    )
                )
            }
            parsedSongs.sortByDescending { it.playCount }
        }

        val parsedMonthly = mutableListOf<MonthlyRecordEntry>()
        val monthlyArr = obj.optJSONArray("monthlyHistory")
        if (monthlyArr != null) {
            for (i in 0 until monthlyArr.length()) {
                val m = monthlyArr.optJSONObject(i) ?: continue
                parsedMonthly.add(
                    MonthlyRecordEntry(
                        yearMonthKey = m.optString("yearMonthKey", ""),
                        displayTitle = m.optString("displayTitle", ""),
                        listeningSeconds = m.optLong("listeningSeconds", 0L),
                    )
                )
            }
        }

        val parsedDaily = mutableListOf<DailyRecordEntry>()
        val dailyArr = obj.optJSONArray("dailyHistory")
        if (dailyArr != null) {
            for (i in 0 until dailyArr.length()) {
                val d = dailyArr.optJSONObject(i) ?: continue
                parsedDaily.add(
                    DailyRecordEntry(
                        epochDay = d.optLong("epochDay", LocalDate.now().toEpochDay()),
                        displayDate = d.optString("displayDate", "Hari Ini"),
                        listeningSeconds = d.optLong("listeningSeconds", 0L),
                    )
                )
            }
        }

        val parsedWeekdays = mutableListOf<Long>()
        val weekdayArr = obj.optJSONArray("weekdaySeconds")
        if (weekdayArr != null && weekdayArr.length() == 7) {
            for (i in 0 until 7) {
                parsedWeekdays.add(weekdayArr.optLong(i, 0L).coerceAtLeast(0L))
            }
        }

        MusicJourneyState(
            totalListeningSeconds = totalSec,
            totalSongsPlayed = totalSongs,
            currentStreak = curStreak,
            longestStreak = longStreak,
            dailyRecordSeconds = dailyRec,
            monthlyRecordSeconds = monthlyRec,
            xp = xp,
            level = level,
            topSongs = parsedSongs,
            monthlyHistory = parsedMonthly,
            dailyHistory = parsedDaily,
            weekdaySeconds = if (parsedWeekdays.size == 7) parsedWeekdays else List(7) { 0L },
        )
    }.getOrNull()
}
