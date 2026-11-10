/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.utils

import android.text.Html
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.db.entities.PlaylistEntity
import com.nanzbeatles.nanas.db.entities.PlaylistSongMap
import com.nanzbeatles.nanas.models.toMediaMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

object SpotifyPlaylistImporter {

    private const val TAG = "SpotifyPlaylistImporter"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    enum class SpotifyResourceType(val pathSegment: String, val label: String) {
        PLAYLIST("playlist", "Playlist"),
        ALBUM("album", "Album"),
        TRACK("track", "Lagu")
    }

    data class SpotifyParsedUrl(
        val type: SpotifyResourceType,
        val id: String,
        val canonicalUrl: String
    )

    data class SpotifyTrackInfo(
        val id: String,
        val title: String,
        val artist: String,
        val durationMs: Long = 0L,
        val thumbnailUrl: String? = null
    ) {
        val formattedDuration: String
            get() {
                if (durationMs <= 0L) return ""
                val totalSec = durationMs / 1000L
                val min = totalSec / 60L
                val sec = totalSec % 60L
                return "%d:%02d".format(min, sec)
            }
    }

    data class SpotifyPlaylistData(
        val id: String,
        val type: SpotifyResourceType,
        val name: String,
        val subtitle: String,
        val coverArtUrl: String?,
        val tracks: List<SpotifyTrackInfo>
    )

    data class ImportProgress(
        val processedCount: Int,
        val totalCount: Int,
        val matchedCount: Int,
        val failedCount: Int,
        val currentTrackTitle: String
    ) {
        val progressFraction: Float
            get() = if (totalCount > 0) (processedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
    }

    data class ImportSummary(
        val playlistId: String,
        val playlistName: String,
        val totalRequested: Int,
        val matchedCount: Int,
        val failedTracks: List<SpotifyTrackInfo>
    )

    /**
     * Extracts and resolves a Spotify URL or URI from raw user input (including share text or short links).
     */
    suspend fun parseAndResolveSpotifyUrl(rawInput: String): Result<SpotifyParsedUrl> = withContext(Dispatchers.IO) {
        try {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Masukkan URL Spotify terlebih dahulu"))
            }

            // 1. Check direct spotify:playlist:ID URI
            val uriRegex = Regex("""spotify:(playlist|album|track):([a-zA-Z0-9]{22})""", RegexOption.IGNORE_CASE)
            uriRegex.find(trimmed)?.let { match ->
                val typeStr = match.groupValues[1].lowercase()
                val id = match.groupValues[2]
                val type = parseType(typeStr)
                return@withContext Result.success(
                    SpotifyParsedUrl(
                        type = type,
                        id = id,
                        canonicalUrl = "https://open.spotify.com/${type.pathSegment}/$id"
                    )
                )
            }

            // 2. Extract URL if user pasted share text with surrounding words
            val urlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
            var candidateUrl = urlRegex.find(trimmed)?.value ?: trimmed
            if (!candidateUrl.startsWith("http://", ignoreCase = true) &&
                !candidateUrl.startsWith("https://", ignoreCase = true)
            ) {
                candidateUrl = "https://$candidateUrl"
            }

            // 3. If it's a shortened link (spotify.link / spoti.fi), resolve redirect first
            if (candidateUrl.contains("spotify.link", ignoreCase = true) ||
                candidateUrl.contains("spoti.fi", ignoreCase = true) ||
                candidateUrl.contains("link.tospotify.com", ignoreCase = true)
            ) {
                val resolved = resolveShortLink(candidateUrl)
                if (resolved != null) {
                    candidateUrl = resolved
                }
            }

            // 4. Match open.spotify.com/(intl-xx/)?(embed/)?(user/xxx/)?(playlist|album|track)/{id}
            val webRegex = Regex(
                """spotify\.com/(?:intl-[a-zA-Z-]+/)?(?:embed/)?(?:user/[^/]+/)?(playlist|album|track)/([a-zA-Z0-9]{15,30})""",
                RegexOption.IGNORE_CASE
            )
            val webMatch = webRegex.find(candidateUrl)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Format URL Spotify tidak valid. Gunakan link Playlist, Album, atau Lagu dari Spotify.")
                )

            val type = parseType(webMatch.groupValues[1].lowercase())
            val id = webMatch.groupValues[2]
            Result.success(
                SpotifyParsedUrl(
                    type = type,
                    id = id,
                    canonicalUrl = "https://open.spotify.com/${type.pathSegment}/$id"
                )
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse Spotify URL: $rawInput")
            Result.failure(e)
        }
    }

    private fun parseType(typeStr: String): SpotifyResourceType = when (typeStr) {
        "album" -> SpotifyResourceType.ALBUM
        "track" -> SpotifyResourceType.TRACK
        else -> SpotifyResourceType.PLAYLIST
    }

    private fun resolveShortLink(shortUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(shortUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.request.url.toString()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to resolve short link: $shortUrl")
            null
        }
    }

    /**
     * Fetches playlist/album/track metadata and tracklist from Spotify without requiring user login.
     */
    suspend fun fetchSpotifyPlaylist(rawUrl: String): Result<SpotifyPlaylistData> = withContext(Dispatchers.IO) {
        val parsed = parseAndResolveSpotifyUrl(rawUrl).getOrElse { return@withContext Result.failure(it) }

        try {
            // 1. Fetch oEmbed metadata in parallel/first for guaranteed clean title and high-res cover image
            val oEmbedMeta = fetchOEmbedMetadata(parsed.canonicalUrl)

            // 2. Try Spotify Embed __NEXT_DATA__ (fast, structured JSON, zero auth required)
            val embedResult = fetchFromEmbedPage(parsed)

            // 3. Try Web API via anonymous web-player access token if embed returned no tracks or if playlist might have >100 tracks
            val apiResult = if (embedResult == null || embedResult.tracks.isEmpty() || embedResult.tracks.size >= 100) {
                fetchFromWebApiWithAnonymousToken(parsed)
            } else null

            // Pick whichever source returned more tracks
            val primaryData = when {
                apiResult != null && (embedResult == null || apiResult.tracks.size >= embedResult.tracks.size) -> apiResult
                embedResult != null && embedResult.tracks.isNotEmpty() -> embedResult
                else -> fetchFromPublicHtmlFallback(parsed, oEmbedMeta)
            }

            if (primaryData == null || primaryData.tracks.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Tidak dapat mengambil daftar lagu dari Spotify. Pastikan playlist bersifat Publik (Public)."
                    )
                )
            }

            val finalName = primaryData.name.ifBlank { oEmbedMeta?.first ?: "Spotify Playlist" }
            val finalCover = primaryData.coverArtUrl?.takeIf { it.isNotBlank() } ?: oEmbedMeta?.second

            Result.success(
                primaryData.copy(
                    name = decodeHtmlEntities(finalName),
                    coverArtUrl = finalCover
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching Spotify playlist data")
            Result.failure(Exception("Gagal memuat playlist dari Spotify: ${e.localizedMessage ?: "Periksa koneksi internet"}"))
        }
    }

    private fun fetchOEmbedMetadata(canonicalUrl: String): Pair<String?, String?>? {
        return try {
            val oembedUrl = "https://open.spotify.com/oembed?url=$canonicalUrl"
            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val thumb = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
                Pair(title, thumb)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses `https://open.spotify.com/embed/{type}/{id}` which embeds full structured JSON in `__NEXT_DATA__`.
     */
    private fun fetchFromEmbedPage(parsed: SpotifyParsedUrl): SpotifyPlaylistData? {
        return try {
            val embedUrl = "https://open.spotify.com/embed/${parsed.type.pathSegment}/${parsed.id}"
            val req = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            val html = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            } ?: return null

            val nextDataRegex = Regex(
                """<script[^>]*id=["']__NEXT_DATA__["'][^>]*>(.*?)</script>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val jsonStr = nextDataRegex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: return null
            val root = JSONObject(jsonStr)

            val entity = root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity")
                ?: findEntityObjectRecursively(root)
                ?: return null

            val title = entity.optString("title").ifBlank { entity.optString("name") }
            val subtitle = entity.optString("subtitle").ifBlank { entity.optString("description") }

            var coverUrl: String? = null
            val coverSources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
                ?: entity.optJSONObject("visualIdentity")?.optJSONArray("image")
            if (coverSources != null && coverSources.length() > 0) {
                coverUrl = coverSources.optJSONObject(0)?.optString("url")
            }

            val tracks = mutableListOf<SpotifyTrackInfo>()

            if (parsed.type == SpotifyResourceType.TRACK) {
                // Single track embed
                val trackTitle = title.ifBlank { "Spotify Track" }
                val trackArtist = cleanArtistString(subtitle)
                val duration = entity.optLong("duration", 0L)
                tracks.add(
                    SpotifyTrackInfo(
                        id = parsed.id,
                        title = decodeHtmlEntities(trackTitle),
                        artist = decodeHtmlEntities(trackArtist),
                        durationMs = duration,
                        thumbnailUrl = coverUrl
                    )
                )
            } else {
                val trackListArray = entity.optJSONArray("trackList")
                if (trackListArray != null) {
                    for (i in 0 until trackListArray.length()) {
                        val item = trackListArray.optJSONObject(i) ?: continue
                        val trackTitle = item.optString("title").ifBlank { item.optString("name") }.trim()
                        if (trackTitle.isEmpty()) continue
                        val rawSubtitle = item.optString("subtitle").trim()
                        val artist = cleanArtistString(rawSubtitle)
                        val duration = item.optLong("duration", 0L)
                        val uri = item.optString("uri", "")
                        val trackId = uri.substringAfterLast(":").ifBlank { "${parsed.id}_$i" }

                        tracks.add(
                            SpotifyTrackInfo(
                                id = trackId,
                                title = decodeHtmlEntities(trackTitle),
                                artist = decodeHtmlEntities(artist),
                                durationMs = duration,
                                thumbnailUrl = null
                            )
                        )
                    }
                }
            }

            if (tracks.isEmpty()) return null

            SpotifyPlaylistData(
                id = parsed.id,
                type = parsed.type,
                name = decodeHtmlEntities(title.ifBlank { "Spotify ${parsed.type.label}" }),
                subtitle = decodeHtmlEntities(cleanArtistString(subtitle)),
                coverArtUrl = coverUrl,
                tracks = tracks
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Embed page extraction failed for ${parsed.canonicalUrl}")
            null
        }
    }

    private fun findEntityObjectRecursively(obj: JSONObject): JSONObject? {
        if (obj.has("trackList") && obj.opt("trackList") is JSONArray) {
            return obj
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val child = obj.optJSONObject(k)
            if (child != null) {
                val found = findEntityObjectRecursively(child)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * Attempts to obtain an anonymous web player token from Spotify and query `api.spotify.com/v1/...`
     * with full track pagination (up to 500 tracks).
     */
    private fun fetchFromWebApiWithAnonymousToken(parsed: SpotifyParsedUrl): SpotifyPlaylistData? {
        return try {
            val token = fetchAnonymousSpotifyToken(parsed.canonicalUrl) ?: return null

            when (parsed.type) {
                SpotifyResourceType.PLAYLIST -> {
                    val url = "https://api.spotify.com/v1/playlists/${parsed.id}"
                    val json = getJsonWithBearer(url, token) ?: return null
                    val name = json.optString("name", "Spotify Playlist")
                    val owner = json.optJSONObject("owner")?.optString("display_name") ?: ""
                    val coverUrl = json.optJSONArray("images")?.optJSONObject(0)?.optString("url")

                    val tracks = mutableListOf<SpotifyTrackInfo>()
                    var tracksObj = json.optJSONObject("tracks")
                    var pagesFetched = 0

                    while (tracksObj != null && pagesFetched < 5) {
                        val items = tracksObj.optJSONArray("items")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val wrapper = items.optJSONObject(i) ?: continue
                                val track = wrapper.optJSONObject("track") ?: wrapper
                                parseWebApiTrackObject(track, fallbackCover = coverUrl)?.let { tracks.add(it) }
                            }
                        }
                        val nextUrl = tracksObj.optString("next").takeIf { it.startsWith("http") }
                        tracksObj = if (nextUrl != null) getJsonWithBearer(nextUrl, token) else null
                        pagesFetched++
                    }

                    if (tracks.isEmpty()) return null
                    SpotifyPlaylistData(
                        id = parsed.id,
                        type = parsed.type,
                        name = name,
                        subtitle = owner,
                        coverArtUrl = coverUrl,
                        tracks = tracks
                    )
                }

                SpotifyResourceType.ALBUM -> {
                    val url = "https://api.spotify.com/v1/albums/${parsed.id}"
                    val json = getJsonWithBearer(url, token) ?: return null
                    val name = json.optString("name", "Spotify Album")
                    val albumArtist = parseArtistsArray(json.optJSONArray("artists"))
                    val coverUrl = json.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                    val items = json.optJSONObject("tracks")?.optJSONArray("items") ?: return null

                    val tracks = mutableListOf<SpotifyTrackInfo>()
                    for (i in 0 until items.length()) {
                        val track = items.optJSONObject(i) ?: continue
                        parseWebApiTrackObject(track, defaultArtist = albumArtist, fallbackCover = coverUrl)?.let {
                            tracks.add(it)
                        }
                    }
                    if (tracks.isEmpty()) return null
                    SpotifyPlaylistData(
                        id = parsed.id,
                        type = parsed.type,
                        name = name,
                        subtitle = albumArtist,
                        coverArtUrl = coverUrl,
                        tracks = tracks
                    )
                }

                SpotifyResourceType.TRACK -> {
                    val url = "https://api.spotify.com/v1/tracks/${parsed.id}"
                    val json = getJsonWithBearer(url, token) ?: return null
                    val trackInfo = parseWebApiTrackObject(json) ?: return null
                    SpotifyPlaylistData(
                        id = parsed.id,
                        type = parsed.type,
                        name = trackInfo.title,
                        subtitle = trackInfo.artist,
                        coverArtUrl = trackInfo.thumbnailUrl,
                        tracks = listOf(trackInfo)
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Web API anonymous fetch failed")
            null
        }
    }

    private fun fetchAnonymousSpotifyToken(pageUrl: String): String? {
        // 1. Try open.spotify.com/get_access_token
        try {
            val req = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val token = json.optString("accessToken")
                        if (token.isNotBlank()) return token
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try extracting session accessToken from main page HTML
        try {
            val req = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val html = resp.body?.string() ?: return null
                    val tokenMatch = Regex(""""accessToken"\s*:\s*"([^"]+)"""").find(html)
                    val token = tokenMatch?.groupValues?.getOrNull(1)
                    if (!token.isNullOrBlank()) return token
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun getJsonWithBearer(url: String, token: String): JSONObject? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            return JSONObject(body)
        }
    }

    private fun parseWebApiTrackObject(
        track: JSONObject,
        defaultArtist: String = "",
        fallbackCover: String? = null
    ): SpotifyTrackInfo? {
        val title = track.optString("name").trim()
        if (title.isEmpty()) return null
        val id = track.optString("id").ifBlank { title.hashCode().toString() }
        val artist = parseArtistsArray(track.optJSONArray("artists")).ifBlank { defaultArtist }
        val durationMs = track.optLong("duration_ms", 0L)
        val thumb = track.optJSONObject("album")
            ?.optJSONArray("images")
            ?.optJSONObject(0)
            ?.optString("url")
            ?: fallbackCover

        return SpotifyTrackInfo(
            id = id,
            title = decodeHtmlEntities(title),
            artist = decodeHtmlEntities(artist),
            durationMs = durationMs,
            thumbnailUrl = thumb
        )
    }

    private fun parseArtistsArray(artistsArr: JSONArray?): String {
        if (artistsArr == null || artistsArr.length() == 0) return ""
        val names = mutableListOf<String>()
        for (i in 0 until artistsArr.length()) {
            val a = artistsArr.optJSONObject(i)
            val name = a?.optString("name")?.trim()
            if (!name.isNullOrEmpty()) names.add(name)
        }
        return names.joinToString(", ")
    }

    /**
     * Fallback HTML scraper for `https://open.spotify.com/{type}/{id}` if both Embed and API fail.
     */
    private fun fetchFromPublicHtmlFallback(
        parsed: SpotifyParsedUrl,
        oEmbedMeta: Pair<String?, String?>?
    ): SpotifyPlaylistData? {
        return try {
            val req = Request.Builder()
                .url(parsed.canonicalUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val html = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            } ?: return null

            val ogTitle = Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)
                ?: oEmbedMeta?.first
                ?: "Spotify Playlist"

            val ogImage = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)
                ?: oEmbedMeta?.second

            // Extract track URLs from <meta name="music:song" content="https://open.spotify.com/track/...">
            val trackUrlRegex = Regex("""https://open\.spotify\.com/track/([a-zA-Z0-9]{22})""")
            val trackIds = trackUrlRegex.findAll(html).map { it.groupValues[1] }.distinct().take(60).toList()

            val tracks = mutableListOf<SpotifyTrackInfo>()
            for (tid in trackIds) {
                val trackEmbed = fetchFromEmbedPage(
                    SpotifyParsedUrl(
                        type = SpotifyResourceType.TRACK,
                        id = tid,
                        canonicalUrl = "https://open.spotify.com/track/$tid"
                    )
                )
                trackEmbed?.tracks?.firstOrNull()?.let { tracks.add(it) }
            }

            if (tracks.isEmpty()) return null

            SpotifyPlaylistData(
                id = parsed.id,
                type = parsed.type,
                name = decodeHtmlEntities(ogTitle),
                subtitle = "Spotify",
                coverArtUrl = ogImage,
                tracks = tracks
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Public HTML fallback failed")
            null
        }
    }

    private fun cleanArtistString(raw: String): String {
        return raw
            .replace('\u00A0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun decodeHtmlEntities(text: String): String {
        if (!text.contains('&')) return text
        return try {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (_: Exception) {
            text
        }
    }

    /**
     * Imports the selected Spotify tracks into NanzBeatles MusicDatabase by matching each song
     * on YouTube Music (`YouTube.search`) and creating/updating the target playlist.
     */
    suspend fun importTracksToDatabase(
        database: MusicDatabase,
        playlistName: String,
        coverArtUrl: String?,
        tracks: List<SpotifyTrackInfo>,
        existingPlaylistId: String? = null,
        onProgress: (ImportProgress) -> Unit
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            if (tracks.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Tidak ada lagu yang dipilih untuk diimport"))
            }

            val finalPlaylistName = playlistName.trim().ifEmpty { "Spotify Playlist" }
            val targetPlaylistId = existingPlaylistId ?: PlaylistEntity.generatePlaylistId()

            if (existingPlaylistId == null) {
                val newPlaylist = PlaylistEntity(
                    id = targetPlaylistId,
                    name = finalPlaylistName,
                    bookmarkedAt = LocalDateTime.now(),
                    thumbnailUrl = coverArtUrl,
                    isEditable = true
                )
                database.query {
                    insert(newPlaylist)
                }
            }

            val total = tracks.size
            val processedAtomic = AtomicInteger(0)
            val matchedAtomic = AtomicInteger(0)
            val failedAtomic = AtomicInteger(0)
            val semaphore = Semaphore(4) // 4 concurrent YouTube Music search workers for high speed

            data class IndexedMatchResult(
                val index: Int,
                val track: SpotifyTrackInfo,
                val matchedSong: SongItem?
            )

            val matchResults = coroutineScope {
                tracks.mapIndexed { index, track ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val matched = searchBestYouTubeMatch(track)
                            val done = processedAtomic.incrementAndGet()
                            val okCount = if (matched != null) {
                                matchedAtomic.incrementAndGet()
                            } else {
                                failedAtomic.incrementAndGet()
                                matchedAtomic.get()
                            }

                            if (matched != null) {
                                try {
                                    database.insert(matched.toMediaMetadata())
                                } catch (e: Exception) {
                                    Timber.tag(TAG).w(e, "Error inserting matched song ${matched.title}")
                                }
                            }

                            onProgress(
                                ImportProgress(
                                    processedCount = done,
                                    totalCount = total,
                                    matchedCount = okCount,
                                    failedCount = failedAtomic.get(),
                                    currentTrackTitle = "${track.title} - ${track.artist}"
                                )
                            )

                            IndexedMatchResult(index, track, matched)
                        }
                    }
                }.awaitAll()
            }

            // Preserve the exact Spotify playlist order when inserting into playlist_song_map
            val orderedResults = matchResults.sortedBy { it.index }
            val matchedSongIds = orderedResults.mapNotNull { it.matchedSong?.id }
            val failedTracks = orderedResults.filter { it.matchedSong == null }.map { it.track }

            if (matchedSongIds.isNotEmpty()) {
                database.transaction {
                    matchedSongIds.forEachIndexed { pos, songId ->
                        insert(
                            PlaylistSongMap(
                                songId = songId,
                                playlistId = targetPlaylistId,
                                position = pos
                            )
                        )
                    }
                }
            }

            Result.success(
                ImportSummary(
                    playlistId = targetPlaylistId,
                    playlistName = finalPlaylistName,
                    totalRequested = total,
                    matchedCount = matchedSongIds.size,
                    failedTracks = failedTracks
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed during Spotify playlist import")
            Result.failure(e)
        }
    }

    /**
     * Searches YouTube Music for the best matching SongItem given a SpotifyTrackInfo.
     */
    private suspend fun searchBestYouTubeMatch(track: SpotifyTrackInfo): SongItem? {
        val cleanTitle = track.title.replace(Regex("""\s*\((?:feat|ft)\..*?\)""", RegexOption.IGNORE_CASE), "").trim()
        val primaryArtist = track.artist.split(",").firstOrNull()?.trim() ?: track.artist
        val query = if (track.artist.isNotBlank()) {
            "${track.title} ${track.artist}"
        } else {
            track.title
        }

        // First attempt: full title + artist
        val candidates = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            .getOrNull()
            ?.items
            ?.filterIsInstance<SongItem>()
            .orEmpty()

        if (candidates.isNotEmpty()) {
            return pickBestSongCandidate(candidates, track)
        }

        // Second attempt fallback: simplified title + primary artist
        val fallbackQuery = "$cleanTitle $primaryArtist".trim()
        if (fallbackQuery != query && fallbackQuery.isNotBlank()) {
            val fallbackCandidates = YouTube.search(fallbackQuery, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                .orEmpty()
            if (fallbackCandidates.isNotEmpty()) {
                return pickBestSongCandidate(fallbackCandidates, track)
            }
        }

        return null
    }

    private fun pickBestSongCandidate(candidates: List<SongItem>, target: SpotifyTrackInfo): SongItem {
        if (candidates.size == 1 || target.durationMs <= 0L) {
            return candidates.first()
        }
        val targetDurationSec = (target.durationMs / 1000L).toInt()
        val targetTitleLower = target.title.lowercase()
        val primaryArtistLower = target.artist.split(",").firstOrNull()?.trim()?.lowercase().orEmpty()

        return candidates.take(5).minByOrNull { song ->
            var penalty = 0
            val songDur = song.duration
            if (songDur != null && songDur > 0 && targetDurationSec > 0) {
                val diffSec = abs(songDur - targetDurationSec)
                penalty += diffSec * 2
            }
            val songTitleLower = song.title.lowercase()
            if (!songTitleLower.contains(targetTitleLower) && !targetTitleLower.contains(songTitleLower)) {
                penalty += 25
            }
            if (primaryArtistLower.isNotEmpty()) {
                val artistMatched = song.artists.any {
                    it.name.lowercase().contains(primaryArtistLower) || primaryArtistLower.contains(it.name.lowercase())
                }
                if (!artistMatched) penalty += 20
            }
            penalty
        } ?: candidates.first()
    }
}
