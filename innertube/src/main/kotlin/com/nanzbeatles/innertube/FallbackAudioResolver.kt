package com.nanzbeatles.innertube

import com.nanzbeatles.innertube.models.ResponseContext
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.innertube.models.Thumbnail
import com.nanzbeatles.innertube.models.Thumbnails
import com.nanzbeatles.innertube.models.WatchEndpoint
import com.nanzbeatles.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_65_10
import com.nanzbeatles.innertube.models.YouTubeClient.Companion.VISIONOS
import com.nanzbeatles.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Resolves metadata and playable audio streams when YouTube's `/player` endpoint
 * rejects unauthenticated or datacenter requests with `LOGIN_REQUIRED`
 * ("Login untuk mengonfirmasi bahwa Anda bukan bot").
 */
object FallbackAudioResolver {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    @Volatile
    private var cachedSoundCloudClientId: String = "2yeWkbX88QhTzS3OtZp8cLGaJRrOAWwF"

    @Volatile
    private var clientIdRefreshed: Boolean = false

    private val videoDetailsCache: MutableMap<String, PlayerResponse.VideoDetails> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, PlayerResponse.VideoDetails>(64, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, PlayerResponse.VideoDetails>?
                ): Boolean = size > 128
            }
        )

    data class CachedStreamEntry(
        val file: File,
        val mimeType: String,
        val bitrate: Int,
        val durationMs: Long,
        val itag: Int,
    )

    private val streamFileCache = ConcurrentHashMap<String, CachedStreamEntry>()

    fun getCachedStreamFile(videoId: String): File? {
        return streamFileCache[videoId]?.file?.takeIf { it.exists() && it.length() > 1024L }
    }

    private val cacheDir: File by lazy {
        val baseTmp = System.getProperty("java.io.tmpdir") ?: "/data/local/tmp"
        File(baseTmp, "beatles_stream_cache").apply { mkdirs() }
    }

    @Volatile
    private var localServerPort: Int = 0
    private val serverLock = Any()
    private val serverExecutor = Executors.newCachedThreadPool()

    /**
     * Ensures the local loopback HTTP server on 127.0.0.1 is running and returns its port.
     */
    private fun ensureLocalServer(): Int {
        if (localServerPort > 0) return localServerPort
        synchronized(serverLock) {
            if (localServerPort > 0) return localServerPort
            val serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            localServerPort = serverSocket.localPort
            Thread({
                while (!serverSocket.isClosed) {
                    try {
                        val client = serverSocket.accept()
                        serverExecutor.execute { handleClientSocket(client) }
                    } catch (_: Throwable) {
                        if (serverSocket.isClosed) break
                    }
                }
            }, "BeatlesLocalStreamServer").apply {
                isDaemon = true
                start()
            }
            return localServerPort
        }
    }

    private fun handleClientSocket(socket: Socket) {
        socket.use { sock ->
            sock.soTimeout = 15_000
            val input = sock.getInputStream()
            val headerBytes = ByteArrayOutputStream()
            var prev3 = 0
            var prev2 = 0
            var prev1 = 0
            while (true) {
                val b = input.read()
                if (b == -1) break
                headerBytes.write(b)
                if (prev3 == '\r'.code && prev2 == '\n'.code && prev1 == '\r'.code && b == '\n'.code) {
                    break
                }
                prev3 = prev2
                prev2 = prev1
                prev1 = b
                if (headerBytes.size() > 16384) break
            }

            val requestText = headerBytes.toString(Charsets.ISO_8859_1.name())
            val lines = requestText.split("\r\n")
            val requestLine = lines.firstOrNull() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val fullPath = parts[1]

            val queryPart = fullPath.substringAfter("?", "")
            val queryParams = queryPart.split("&").mapNotNull { param ->
                val idx = param.indexOf('=')
                if (idx > 0) {
                    val k = URLDecoder.decode(param.substring(0, idx), "UTF-8")
                    val v = URLDecoder.decode(param.substring(idx + 1), "UTF-8")
                    k to v
                } else null
            }.toMap()

            val videoId = queryParams["id"]
            val entry = if (videoId != null) streamFileCache[videoId] else null
            val out = BufferedOutputStream(sock.getOutputStream())

            if (entry == null || !entry.file.exists()) {
                val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                out.write(notFound.toByteArray(Charsets.US_ASCII))
                out.flush()
                return
            }

            val totalLength = entry.file.length()
            val contentType = entry.mimeType.substringBefore(";").trim()

            var rangeHeader: String? = null
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line.substringAfter(":").trim()
                    break
                }
            }

            var start = 0L
            var end = totalLength - 1
            var isPartial = false

            if (rangeHeader != null && rangeHeader.startsWith("bytes=", ignoreCase = true)) {
                val spec = rangeHeader.substring(6).trim().substringBefore(",")
                val startStr = spec.substringBefore("-")
                val endStr = spec.substringAfter("-", "")
                if (startStr.isNotEmpty()) {
                    start = startStr.toLongOrNull() ?: 0L
                    end = if (endStr.isNotEmpty()) {
                        (endStr.toLongOrNull() ?: (totalLength - 1)).coerceAtMost(totalLength - 1)
                    } else {
                        totalLength - 1
                    }
                    isPartial = true
                }
            } else if (queryParams["range"] != null) {
                val spec = queryParams["range"]!!
                val startStr = spec.substringBefore("-")
                val endStr = spec.substringAfter("-", "")
                if (startStr.isNotEmpty()) {
                    start = startStr.toLongOrNull() ?: 0L
                    end = if (endStr.isNotEmpty()) {
                        (endStr.toLongOrNull() ?: (totalLength - 1)).coerceAtMost(totalLength - 1)
                    } else {
                        totalLength - 1
                    }
                    isPartial = true
                }
            }

            if (start < 0L || start >= totalLength || start > end) {
                val badRange =
                    "HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$totalLength\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                out.write(badRange.toByteArray(Charsets.US_ASCII))
                out.flush()
                return
            }

            val contentLength = end - start + 1
            val headers = buildString {
                if (isPartial) {
                    append("HTTP/1.1 206 Partial Content\r\n")
                    append("Content-Range: bytes $start-$end/$totalLength\r\n")
                } else {
                    append("HTTP/1.1 200 OK\r\n")
                }
                append("Content-Type: $contentType\r\n")
                append("Content-Length: $contentLength\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Cache-Control: no-cache\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            out.write(headers.toByteArray(Charsets.US_ASCII))

            if (method != "HEAD") {
                RandomAccessFile(entry.file, "r").use { raf ->
                    raf.seek(start)
                    val buf = ByteArray(32 * 1024)
                    var remaining = contentLength
                    while (remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), remaining).toInt()
                        val read = raf.read(buf, 0, toRead)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        remaining -= read
                    }
                }
            }
            out.flush()
        }
    }

    /**
     * Resolves [PlayerResponse.VideoDetails] via `YouTube.next` when `/player` fails to return videoDetails.
     */
    suspend fun resolveVideoDetails(videoId: String): PlayerResponse.VideoDetails? {
        videoDetailsCache[videoId]?.let { return it }
        return withContext(Dispatchers.IO) {
            val fromNext = runCatching {
                val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull() ?: return@runCatching null
                val item = nextResult.items.firstOrNull { it.id == videoId }
                    ?: nextResult.items.firstOrNull()
                    ?: return@runCatching null
                if (item.title.isBlank()) return@runCatching null
                val artistsText = item.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
                val durationSec = item.duration ?: 0
                val thumbUrl = item.thumbnail.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }
                PlayerResponse.VideoDetails(
                    videoId = videoId,
                    title = item.title,
                    author = artistsText,
                    channelId = item.artists.firstOrNull()?.id ?: "",
                    lengthSeconds = durationSec.toString(),
                    musicVideoType = "MUSIC_VIDEO_TYPE_ATV",
                    viewCount = null,
                    thumbnail = Thumbnails(
                        thumbnails = listOf(
                            Thumbnail(url = thumbUrl, width = 544, height = 544)
                        )
                    )
                )
            }.getOrNull()

            if (fromNext != null) {
                videoDetailsCache[videoId] = fromNext
                return@withContext fromNext
            }

            // Fallback to public YouTube oEmbed endpoint when YouTube.next fails
            val fromOembed = runCatching {
                val oembedUrl = "https://www.youtube.com/oembed?url=${
                    URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
                }&format=json"
                httpClient.newCall(
                    Request.Builder().url(oembedUrl).header("User-Agent", BROWSER_UA).build()
                ).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val bodyStr = resp.body?.string() ?: return@use null
                    val obj = json.parseToJsonElement(bodyStr).jsonObject
                    val rawTitle = obj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@use null
                    val rawAuthor = obj["author_name"]?.jsonPrimitive?.content
                        ?.replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
                        ?.trim()
                        ?.ifBlank { "Unknown Artist" }
                        ?: "Unknown Artist"
                    val thumbUrl = obj["thumbnail_url"]?.jsonPrimitive?.content
                        ?.takeIf { it.isNotBlank() }
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    PlayerResponse.VideoDetails(
                        videoId = videoId,
                        title = rawTitle,
                        author = rawAuthor,
                        channelId = "",
                        lengthSeconds = "0",
                        musicVideoType = "MUSIC_VIDEO_TYPE_ATV",
                        viewCount = null,
                        thumbnail = Thumbnails(
                            thumbnails = listOf(
                                Thumbnail(url = thumbUrl, width = 544, height = 544)
                            )
                        )
                    )
                }
            }.getOrNull()

            if (fromOembed != null) {
                videoDetailsCache[videoId] = fromOembed
            }
            fromOembed
        }
    }

    /**
     * Synthesizes a metadata-only [PlayerResponse] when `YouTube.player` returns `LOGIN_REQUIRED`.
     */
    suspend fun buildMetadataPlayerResponse(videoId: String): PlayerResponse? {
        val details = resolveVideoDetails(videoId) ?: return null
        return PlayerResponse(
            responseContext = ResponseContext(visitorData = null, serviceTrackingParams = null),
            playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK", reason = null),
            playerConfig = null,
            streamingData = null,
            captions = null,
            videoDetails = details,
            playbackTracking = null,
        )
    }

    /**
     * Searches YouTube for an alternative video ID of the same song that is directly playable
     * on `VISIONOS` or `ANDROID_VR_1_65_10` without bot-check.
     */
    suspend fun findPlayableAlternativeVideoId(
        title: String,
        artist: String,
        originalVideoId: String,
        targetDurationSec: Int,
    ): Pair<String, PlayerResponse>? = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
            val cleanArtist = artist.split(",", "&").firstOrNull()?.trim() ?: artist
            val query = "$cleanArtist $cleanTitle".trim()
            if (query.isBlank()) return@runCatching null

            val searchPage = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                ?: return@runCatching null
            val candidates = searchPage.items
                .filterIsInstance<SongItem>()
                .filter { it.id != originalVideoId }
                .filter { item ->
                    val dur = item.duration ?: 0
                    targetDurationSec <= 0 || dur <= 0 || abs(dur - targetDurationSec) <= 45
                }
                .take(2)

            val probeClients = listOf(VISIONOS, ANDROID_VR_1_65_10)
            for (candidate in candidates) {
                for (client in probeClients) {
                    val resp = YouTube.player(candidate.id, null, client, setLogin = false).getOrNull()
                    if (resp?.playabilityStatus?.status == "OK" &&
                        resp.streamingData?.adaptiveFormats?.any { it.isAudio && !it.url.isNullOrBlank() } == true
                    ) {
                        return@runCatching candidate.id to resp
                    }
                }
            }
            null
        }.getOrNull()
    }

    /**
     * Resolves a full playable audio stream for [videoId] via SoundCloud's CDN, packages it into
     * a seekable fMP4 (with merged master `sidx`) or MP3 file, and serves it over the local
     * loopback HTTP server (`http://127.0.0.1:<port>/stream?id=<videoId>`).
     */
    suspend fun resolveFallbackFormatAndUrl(
        videoId: String,
        existingDetails: PlayerResponse.VideoDetails? = null,
    ): Triple<PlayerResponse.StreamingData.Format, String, PlayerResponse.VideoDetails?>? =
        withContext(Dispatchers.IO) {
            val resolvedDetails = existingDetails ?: resolveVideoDetails(videoId)

            streamFileCache[videoId]?.takeIf { it.file.exists() && it.file.length() > 1024L }?.let { cached ->
                val port = ensureLocalServer()
                val url = "http://127.0.0.1:$port/stream?id=${URLEncoder.encode(videoId, "UTF-8")}"
                val format = buildFormat(cached, url)
                return@withContext Triple(format, url, resolvedDetails)
            }

            val title = resolvedDetails?.title?.takeIf { it.isNotBlank() } ?: return@withContext null
            val author = resolvedDetails.author?.takeIf { it.isNotBlank() && it != "Unknown Artist" } ?: ""
            val targetDurationMs = (resolvedDetails.lengthSeconds.toLongOrNull() ?: 0L) * 1000L

            val downloaded = downloadTrackFromSoundCloud(videoId, title, author, targetDurationMs)
                ?: return@withContext null

            streamFileCache[videoId] = downloaded
            pruneOldCacheFiles(keepVideoId = videoId)

            val port = ensureLocalServer()
            val url = "http://127.0.0.1:$port/stream?id=${URLEncoder.encode(videoId, "UTF-8")}"
            val format = buildFormat(downloaded, url)
            Triple(format, url, resolvedDetails)
        }

    private fun buildFormat(
        entry: CachedStreamEntry,
        url: String,
    ): PlayerResponse.StreamingData.Format = PlayerResponse.StreamingData.Format(
        itag = entry.itag,
        url = url,
        mimeType = entry.mimeType,
        bitrate = entry.bitrate,
        width = null,
        height = null,
        contentLength = entry.file.length(),
        quality = "tiny",
        fps = null,
        qualityLabel = null,
        averageBitrate = entry.bitrate,
        audioQuality = "AUDIO_QUALITY_MEDIUM",
        approxDurationMs = entry.durationMs.toString(),
        audioSampleRate = 48000,
        audioChannels = 2,
        loudnessDb = null,
        lastModified = System.currentTimeMillis() * 1000L,
        signatureCipher = null,
        cipher = null,
        audioTrack = null,
    )

    private fun pruneOldCacheFiles(keepVideoId: String) {
        runCatching {
            val files = cacheDir.listFiles() ?: return
            if (files.size <= 24) return
            files.sortedBy { it.lastModified() }
                .take(files.size - 20)
                .forEach { file ->
                    if (!file.name.contains(keepVideoId)) {
                        file.delete()
                    }
                }
        }
    }

    private fun getOrRefreshSoundCloudClientId(forceRefresh: Boolean = false): String {
        if (!forceRefresh && cachedSoundCloudClientId.isNotBlank()) {
            return cachedSoundCloudClientId
        }
        runCatching {
            val homeReq = Request.Builder()
                .url("https://soundcloud.com/")
                .header("User-Agent", BROWSER_UA)
                .build()
            val html = httpClient.newCall(homeReq).execute().use { it.body?.string() ?: "" }
            val scriptUrls = Regex("https://a-v2\\.sndcdn\\.com/assets/[^\"']+\\.js")
                .findAll(html)
                .map { it.value }
                .toList()
                .takeLast(6)
                .reversed()
            val cidRegexes = listOf(
                Regex("client_id\\s*[:=]\\s*[\"']([a-zA-Z0-9]{32})[\"']"),
                Regex("client_id=([a-zA-Z0-9]{32})"),
            )
            for (jsUrl in scriptUrls) {
                val jsReq = Request.Builder()
                    .url(jsUrl)
                    .header("User-Agent", BROWSER_UA)
                    .build()
                val js = httpClient.newCall(jsReq).execute().use { it.body?.string() ?: "" }
                for (regex in cidRegexes) {
                    val match = regex.find(js)
                    if (match != null) {
                        cachedSoundCloudClientId = match.groupValues[1]
                        clientIdRefreshed = true
                        return cachedSoundCloudClientId
                    }
                }
            }
        }
        return cachedSoundCloudClientId
    }

    private suspend fun downloadTrackFromSoundCloud(
        videoId: String,
        title: String,
        author: String,
        targetDurationMs: Long,
    ): CachedStreamEntry? = withContext(Dispatchers.IO) {
        val cleanTitle = title
            .replace(Regex("\\(Official.*?\\)|\\[Official.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Lyric.*?\\)|\\[Lyric.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Audio.*?\\)|\\[Audio.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Music Video.*?\\)|\\[Music Video.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\|.*$"), "")
            .trim()
        val strippedTitle = cleanTitle
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .trim()
        val primaryArtist = author
            .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("VEVO$", RegexOption.IGNORE_CASE), "")
            .split(",", "&")
            .firstOrNull()
            ?.trim()
            ?: ""

        val queries = buildList {
            if (primaryArtist.isNotBlank() && cleanTitle.isNotBlank()) add("$primaryArtist $cleanTitle")
            if (cleanTitle.isNotBlank()) add(cleanTitle)
            if (primaryArtist.isNotBlank() && strippedTitle.isNotBlank() && strippedTitle != cleanTitle) {
                add("$primaryArtist $strippedTitle")
            }
            if (strippedTitle.isNotBlank() && strippedTitle != cleanTitle) add(strippedTitle)
            add("$author $title".trim())
        }.distinct().filter { it.isNotBlank() }

        var cid = getOrRefreshSoundCloudClientId(forceRefresh = false)

        for (query in queries) {
            val searchUrl =
                "https://api-v2.soundcloud.com/search/tracks?q=${URLEncoder.encode(query, "UTF-8")}&client_id=$cid&limit=15"
            var searchResp = runCatching {
                httpClient.newCall(
                    Request.Builder().url(searchUrl).header("User-Agent", BROWSER_UA).build()
                ).execute()
            }.getOrNull()

            if (searchResp?.code == 401 || searchResp?.code == 403) {
                searchResp.close()
                cid = getOrRefreshSoundCloudClientId(forceRefresh = true)
                val retryUrl =
                    "https://api-v2.soundcloud.com/search/tracks?q=${URLEncoder.encode(query, "UTF-8")}&client_id=$cid&limit=15"
                searchResp = runCatching {
                    httpClient.newCall(
                        Request.Builder().url(retryUrl).header("User-Agent", BROWSER_UA).build()
                    ).execute()
                }.getOrNull()
            }

            val bodyStr = searchResp?.use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: continue

            val collection = runCatching {
                json.parseToJsonElement(bodyStr).jsonObject["collection"]?.jsonArray
            }.getOrNull() ?: continue

            if (collection.isEmpty()) continue

            val scoredTracks = collection.mapNotNull { el ->
                val obj = el.jsonObject
                val trackDuration = obj["duration"]?.jsonPrimitive?.intOrNull?.toLong() ?: return@mapNotNull null
                if (trackDuration <= 15_000L) return@mapNotNull null
                val policy = obj["policy"]?.jsonPrimitive?.content ?: "ALLOW"
                val isSnip = policy.equals("SNIP", ignoreCase = true) ||
                    (trackDuration <= 32_000L && targetDurationMs > 45_000L)
                val trackTitle = obj["title"]?.jsonPrimitive?.content?.lowercase() ?: ""
                val trackUser = obj["user"]?.jsonObject?.get("username")?.jsonPrimitive?.content?.lowercase() ?: ""
                val transcodings = obj["media"]?.jsonObject?.get("transcodings")?.jsonArray ?: return@mapNotNull null
                if (transcodings.isEmpty()) return@mapNotNull null

                var score = 0
                if (isSnip) score -= 250
                if (cleanTitle.isNotBlank() && trackTitle.contains(cleanTitle.lowercase())) score += 50
                else if (strippedTitle.isNotBlank() && trackTitle.contains(strippedTitle.lowercase())) score += 35
                if (primaryArtist.isNotBlank() &&
                    (trackTitle.contains(primaryArtist.lowercase()) || trackUser.contains(primaryArtist.lowercase()))
                ) {
                    score += 40
                }
                if (targetDurationMs > 0) {
                    val diffSec = abs(trackDuration - targetDurationMs) / 1000L
                    when {
                        diffSec <= 3 -> score += 60
                        diffSec <= 10 -> score += 40
                        diffSec <= 25 -> score += 15
                        diffSec > 90 -> score -= 35
                    }
                }
                val penaltyWords = listOf("remix", "cover", "karaoke", "nightcore", "8d", "slowed", "sped up")
                for (pw in penaltyWords) {
                    if (trackTitle.contains(pw) && !title.lowercase().contains(pw)) {
                        score -= 20
                    }
                }
                Triple(obj, trackDuration, score)
            }.sortedByDescending { it.third }

            for (candidateTrack in scoredTracks.take(4)) {
                val trackObj = candidateTrack.first
                val trackDurationMs = candidateTrack.second
                val transcodings = trackObj["media"]?.jsonObject?.get("transcodings")?.jsonArray ?: continue

                // Priority 1: Progressive MP3 stream (single HTTP file)
                val progressiveMp3 = transcodings.firstOrNull { t ->
                    val obj = t.jsonObject
                    val proto = obj["format"]?.jsonObject?.get("protocol")?.jsonPrimitive?.content ?: ""
                    val mime = obj["format"]?.jsonObject?.get("mime_type")?.jsonPrimitive?.content ?: ""
                    proto == "progressive" && mime.contains("mpeg")
                }?.jsonObject

                if (progressiveMp3 != null) {
                    val apiUrl = progressiveMp3["url"]?.jsonPrimitive?.content
                    if (!apiUrl.isNullOrBlank()) {
                        val resolvedStreamUrl = resolveSoundCloudTranscodingUrl(apiUrl, cid)
                        if (!resolvedStreamUrl.isNullOrBlank()) {
                            val safeId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val outFile = File(cacheDir, "${safeId}.mp3")
                            val ok = downloadUrlToFile(resolvedStreamUrl, outFile)
                            if (ok && outFile.length() > 1024L) {
                                return@withContext CachedStreamEntry(
                                    file = outFile,
                                    mimeType = "audio/mpeg; codecs=\"mp3\"",
                                    bitrate = 128_000,
                                    durationMs = trackDurationMs,
                                    itag = 140,
                                )
                            }
                        }
                    }
                }

                // Priority 2: HLS fMP4 AAC (aac_160k or aac_96k) merged with a unified master sidx box
                val hlsMp4 = transcodings.filter { t ->
                    val obj = t.jsonObject
                    val proto = obj["format"]?.jsonObject?.get("protocol")?.jsonPrimitive?.content ?: ""
                    val mime = obj["format"]?.jsonObject?.get("mime_type")?.jsonPrimitive?.content ?: ""
                    proto == "hls" && mime.startsWith("audio/mp4")
                }.maxByOrNull { t ->
                    val preset = t.jsonObject["preset"]?.jsonPrimitive?.content ?: ""
                    when {
                        preset.contains("160") -> 160
                        preset.contains("96") -> 96
                        else -> 64
                    }
                }?.jsonObject

                if (hlsMp4 != null) {
                    val apiUrl = hlsMp4["url"]?.jsonPrimitive?.content
                    val preset = hlsMp4["preset"]?.jsonPrimitive?.content ?: ""
                    val bitrate = if (preset.contains("160")) 160_000 else 128_000
                    if (!apiUrl.isNullOrBlank()) {
                        val m3u8Url = resolveSoundCloudTranscodingUrl(apiUrl, cid)
                        if (!m3u8Url.isNullOrBlank()) {
                            val safeId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val outFile = File(cacheDir, "${safeId}.m4a")
                            val ok = downloadAndMergeFmp4Hls(m3u8Url, outFile)
                            if (ok && outFile.length() > 1024L) {
                                return@withContext CachedStreamEntry(
                                    file = outFile,
                                    mimeType = "audio/mp4; codecs=\"mp4a.40.2\"",
                                    bitrate = bitrate,
                                    durationMs = trackDurationMs,
                                    itag = 140,
                                )
                            }
                        }
                    }
                }

                // Priority 3: HLS MP3 segments concatenated into a single MP3 file
                val hlsMp3 = transcodings.firstOrNull { t ->
                    val obj = t.jsonObject
                    val proto = obj["format"]?.jsonObject?.get("protocol")?.jsonPrimitive?.content ?: ""
                    val mime = obj["format"]?.jsonObject?.get("mime_type")?.jsonPrimitive?.content ?: ""
                    proto == "hls" && mime.contains("mpeg")
                }?.jsonObject

                if (hlsMp3 != null) {
                    val apiUrl = hlsMp3["url"]?.jsonPrimitive?.content
                    if (!apiUrl.isNullOrBlank()) {
                        val m3u8Url = resolveSoundCloudTranscodingUrl(apiUrl, cid)
                        if (!m3u8Url.isNullOrBlank()) {
                            val safeId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val outFile = File(cacheDir, "${safeId}.mp3")
                            val ok = downloadAndMergeMp3Hls(m3u8Url, outFile)
                            if (ok && outFile.length() > 1024L) {
                                return@withContext CachedStreamEntry(
                                    file = outFile,
                                    mimeType = "audio/mpeg; codecs=\"mp3\"",
                                    bitrate = 128_000,
                                    durationMs = trackDurationMs,
                                    itag = 140,
                                )
                            }
                        }
                    }
                }
            }
        }
        null
    }

    private suspend fun downloadAndMergeMp3Hls(m3u8Url: String, targetFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val m3u8Text = httpClient.newCall(
                    Request.Builder().url(m3u8Url).header("User-Agent", BROWSER_UA).build()
                ).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching false
                    resp.body?.string() ?: return@runCatching false
                }

                val segmentUrls = m3u8Text.lines()
                    .map { it.trim() }
                    .filter { it.startsWith("http://") || it.startsWith("https://") }
                if (segmentUrls.isEmpty()) return@runCatching false

                val rawSegments = supervisorScope {
                    segmentUrls.chunked(16).flatMap { batch ->
                        batch.map { segUrl ->
                            async(Dispatchers.IO) { fetchBytes(segUrl) }
                        }.awaitAll()
                    }
                }

                val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                FileOutputStream(tmpFile).use { fos ->
                    BufferedOutputStream(fos, 64 * 1024).use { bos ->
                        for (seg in rawSegments) {
                            if (seg == null || seg.isEmpty()) return@runCatching false
                            bos.write(seg)
                        }
                        bos.flush()
                    }
                }

                if (tmpFile.exists() && tmpFile.length() > 1024L) {
                    tmpFile.renameTo(targetFile)
                    true
                } else {
                    tmpFile.delete()
                    false
                }
            }.getOrDefault(false)
        }

    private fun resolveSoundCloudTranscodingUrl(apiUrl: String, clientId: String): String? {
        val separator = if (apiUrl.contains("?")) "&" else "?"
        val fullUrl = "${apiUrl}${separator}client_id=$clientId"
        return runCatching {
            httpClient.newCall(
                Request.Builder().url(fullUrl).header("User-Agent", BROWSER_UA).build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                json.parseToJsonElement(body).jsonObject["url"]?.jsonPrimitive?.content
            }
        }.getOrNull()
    }

    private fun downloadUrlToFile(url: String, targetFile: File): Boolean {
        return runCatching {
            val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            httpClient.newCall(
                Request.Builder().url(url).header("User-Agent", BROWSER_UA).build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body ?: return false
                FileOutputStream(tmpFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }
            }
            if (tmpFile.exists() && tmpFile.length() > 1024L) {
                tmpFile.renameTo(targetFile)
                true
            } else {
                tmpFile.delete()
                false
            }
        }.getOrDefault(false)
    }

    private data class SegmentParsed(
        val referenceId: Int,
        val timescale: Int,
        val earliestPresentationTime: Long,
        val subsegmentDuration: Int,
        val sapFlags: Int,
        val moofMdatBytes: ByteArray,
    )

    /**
     * Downloads all segments referenced by an HLS fMP4 playlist (`#EXT-X-MAP:URI="..."` + `.m4s` segments)
     * in parallel, strips per-segment `styp` and `sidx` boxes, and builds a single spec-compliant
     * standalone fMP4 file `[ftyp, moov, master sidx, moof, mdat, ...]` so ExoPlayer's
     * `FragmentedMp4Extractor` sees the full track duration and supports instant seeking.
     */
    private suspend fun downloadAndMergeFmp4Hls(m3u8Url: String, targetFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val m3u8Text = httpClient.newCall(
                    Request.Builder().url(m3u8Url).header("User-Agent", BROWSER_UA).build()
                ).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching false
                    resp.body?.string() ?: return@runCatching false
                }

                var initUrl: String? = null
                val segmentUrls = mutableListOf<String>()
                for (rawLine in m3u8Text.lines()) {
                    val line = rawLine.trim()
                    if (line.startsWith("#EXT-X-MAP:")) {
                        val uriMatch = Regex("URI=\"([^\"]+)\"").find(line)
                        if (uriMatch != null) {
                            initUrl = uriMatch.groupValues[1]
                        }
                    } else if (line.startsWith("http://") || line.startsWith("https://")) {
                        segmentUrls.add(line)
                    }
                }

                val nonNullInitUrl = initUrl ?: return@runCatching false
                if (segmentUrls.isEmpty()) return@runCatching false

                val initBytes = fetchBytes(nonNullInitUrl) ?: return@runCatching false

                // Download segments in parallel batches of 16
                val rawSegments = supervisorScope {
                    segmentUrls.chunked(16).flatMap { batch ->
                        batch.map { segUrl ->
                            async(Dispatchers.IO) { fetchBytes(segUrl) }
                        }.awaitAll()
                    }
                }

                val parsedSegments = mutableListOf<SegmentParsed>()
                for (segBytes in rawSegments) {
                    if (segBytes == null || segBytes.size < 16) return@runCatching false
                    val parsed = parseFmp4Segment(segBytes) ?: return@runCatching false
                    parsedSegments.add(parsed)
                }

                if (parsedSegments.isEmpty()) return@runCatching false

                val first = parsedSegments.first()
                val n = parsedSegments.size
                // v1 sidx box: 8 (box header) + 4 (version=1, flags=0) + 4 (reference_ID) + 4 (timescale)
                // + 8 (earliest_presentation_time) + 8 (first_offset) + 2 (reserved) + 2 (reference_count)
                // + 12 * n
                val sidxSize = 40 + 12 * n
                val masterSidx = ByteBuffer.allocate(sidxSize)
                masterSidx.putInt(sidxSize)
                masterSidx.put("sidx".toByteArray(Charsets.US_ASCII))
                masterSidx.putInt(0x01000000) // version 1, flags 0
                masterSidx.putInt(first.referenceId)
                masterSidx.putInt(first.timescale)
                masterSidx.putLong(first.earliestPresentationTime)
                masterSidx.putLong(0L) // first_offset = 0 (immediately follows sidx)
                masterSidx.putShort(0) // reserved
                masterSidx.putShort(n.toShort())
                for (seg in parsedSegments) {
                    masterSidx.putInt(seg.moofMdatBytes.size and 0x7FFFFFFF)
                    masterSidx.putInt(seg.subsegmentDuration)
                    masterSidx.putInt(seg.sapFlags)
                }

                val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                FileOutputStream(tmpFile).use { fos ->
                    BufferedOutputStream(fos, 64 * 1024).use { bos ->
                        bos.write(initBytes)
                        bos.write(masterSidx.array())
                        for (seg in parsedSegments) {
                            bos.write(seg.moofMdatBytes)
                        }
                        bos.flush()
                    }
                }

                if (tmpFile.exists() && tmpFile.length() > 1024L) {
                    tmpFile.renameTo(targetFile)
                    true
                } else {
                    tmpFile.delete()
                    false
                }
            }.getOrDefault(false)
        }

    private fun fetchBytes(url: String): ByteArray? {
        for (attempt in 1..2) {
            try {
                httpClient.newCall(
                    Request.Builder().url(url).header("User-Agent", BROWSER_UA).build()
                ).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return resp.body?.bytes()
                    }
                }
            } catch (_: Throwable) {
                // Retry once on transient error
            }
        }
        return null
    }

    private fun parseFmp4Segment(segBytes: ByteArray): SegmentParsed? {
        val bb = ByteBuffer.wrap(segBytes)
        var pos = 0
        var refId = 1
        var timescale = 48000
        var ept = 0L
        var subDur = 0
        var sapFlags = 0x90000000.toInt()
        var moofOffset = -1

        while (pos + 8 <= segBytes.size) {
            val boxSize = bb.getInt(pos)
            if (boxSize < 8 || pos + boxSize > segBytes.size) break
            val type = String(segBytes, pos + 4, 4, Charsets.US_ASCII)
            if (type == "sidx") {
                val version = segBytes[pos + 8].toInt() and 0xFF
                refId = bb.getInt(pos + 12)
                timescale = bb.getInt(pos + 16)
                var p = pos + 20
                if (version == 0) {
                    ept = bb.getInt(p).toLong() and 0xFFFFFFFFL
                    p += 8
                } else {
                    ept = bb.getLong(p)
                    p += 16
                }
                p += 2 // reserved
                val count = bb.getShort(p).toInt() and 0xFFFF
                p += 2
                if (count >= 1 && p + 12 <= pos + boxSize) {
                    var totalDur = 0
                    for (i in 0 until count) {
                        if (p + 12 > pos + boxSize) break
                        totalDur += bb.getInt(p + 4)
                        if (i == 0) {
                            sapFlags = bb.getInt(p + 8)
                        }
                        p += 12
                    }
                    subDur = totalDur
                }
            } else if (type == "moof") {
                moofOffset = pos
                break
            }
            pos += boxSize
        }

        if (moofOffset < 0) return null
        val moofMdat = segBytes.copyOfRange(moofOffset, segBytes.size)
        return SegmentParsed(
            referenceId = refId,
            timescale = timescale,
            earliestPresentationTime = ept,
            subsegmentDuration = subDur,
            sapFlags = sapFlags,
            moofMdatBytes = moofMdat,
        )
    }
}
