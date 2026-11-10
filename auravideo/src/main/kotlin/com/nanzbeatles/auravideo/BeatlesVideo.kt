package com.nanzbeatles.auravideo

import android.util.Log
import com.nanzbeatles.innertube.FallbackAudioResolver
import com.nanzbeatles.innertube.NewPipeExtractor
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_65_10
import com.nanzbeatles.innertube.models.YouTubeClient.Companion.IPADOS
import com.nanzbeatles.innertube.models.YouTubeClient.Companion.VISIONOS
import com.nanzbeatles.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.nanzbeatles.innertube.YouTube.SearchFilter
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object BeatlesVideo {
    private const val TAG = "BeatlesVideo"
    data class VideoStreamResult(
        val url: String,
        val mimeType: String
    )

    /**
     * A richer description of how to play a video. YouTube only offers muxed
     * (audio+video) streams up to 720p, so to reach 1080p or higher we must
     * combine a video-only stream with a matching audio-only stream.
     */
    sealed class VideoStreamSource {
        /** Single URL that contains both audio and video (muxed). */
        data class Single(
            val url: String,
            val mimeType: String,
        ) : VideoStreamSource()

        /** Separate video and audio streams that must be merged for playback. */
        data class Merged(
            val videoUrl: String,
            val videoMimeType: String,
            val audioUrl: String,
            val audioMimeType: String,
            val height: Int,
        ) : VideoStreamSource()
    }

    data class VideoSearchResult(
        val videoId: String,
        val title: String,
        val channelName: String,
        val thumbnailUrl: String
    )

    /**
     * Available video quality options
     */
    enum class VideoQuality(val height: Int, val label: String) {
        QUALITY_360P(360, "360p"),
        QUALITY_480P(480, "480p"),
        QUALITY_720P(720, "720p"),
        QUALITY_1080P(1080, "1080p")
    }

    // Default preferred quality - 720p
    var currentPreferredQuality: VideoQuality = VideoQuality.QUALITY_720P

    /**
     * Set preferred video quality
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        currentPreferredQuality = quality
    }

    /**
     * Get all available qualities from a list of video streams
     */
    private fun getAvailableQualities(streams: List<org.schabi.newpipe.extractor.stream.VideoStream>): List<VideoQuality> {
        val qualities = mutableSetOf<VideoQuality>()
        for (stream in streams) {
            val height = stream.height
            when {
                height >= 1080 -> qualities.add(VideoQuality.QUALITY_1080P)
                height >= 720 -> qualities.add(VideoQuality.QUALITY_720P)
                height >= 480 -> qualities.add(VideoQuality.QUALITY_480P)
                height >= 360 -> qualities.add(VideoQuality.QUALITY_360P)
            }
        }
        return qualities.sortedBy { it.height }
    }

    /**
     * Find the best stream matching the preferred quality
     * Falls back to lower quality if preferred not available
     */
    private fun findBestStream(
        streams: List<org.schabi.newpipe.extractor.stream.VideoStream>,
        requireAudio: Boolean
    ): org.schabi.newpipe.extractor.stream.VideoStream? {
        if (streams.isEmpty()) return null

        // Filter streams based on whether we need audio
        val filteredStreams = if (requireAudio) {
            streams.filter { !it.isVideoOnly() }
        } else {
            streams
        }

        // Quality preference order: try exact match first, then fall back to lower qualities only
        val qualityOrder = when (currentPreferredQuality) {
            VideoQuality.QUALITY_1080P -> listOf(VideoQuality.QUALITY_1080P, VideoQuality.QUALITY_720P, VideoQuality.QUALITY_480P, VideoQuality.QUALITY_360P)
            VideoQuality.QUALITY_720P -> listOf(VideoQuality.QUALITY_720P, VideoQuality.QUALITY_480P, VideoQuality.QUALITY_360P)
            VideoQuality.QUALITY_480P -> listOf(VideoQuality.QUALITY_480P, VideoQuality.QUALITY_360P)
            VideoQuality.QUALITY_360P -> listOf(VideoQuality.QUALITY_360P)
        }

        // First pass: try to find exact or closest match at or below preferred quality
        for (quality in qualityOrder) {
            // Find streams at exactly the preferred quality or lower (not higher)
            val matching = filteredStreams.filter { stream ->
                stream.height <= quality.height && stream.height > 0
            }
            if (matching.isNotEmpty()) {
                // Return the highest quality that doesn't exceed preferred
                return matching.maxByOrNull { it.height }
            }
        }

        // Fallback: if no quality at or below preferred found, find any quality
        return if (requireAudio && filteredStreams.isNotEmpty()) {
            filteredStreams.maxByOrNull { it.height }
        } else {
            streams.maxByOrNull { it.height }
        }
    }

    private fun tokenizeWords(text: String): List<String> {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return normalized.lowercase()
            .replace("'", "")
            .replace("’", "")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
    }

    private fun cleanSongTitleForMatch(rawTitle: String): String {
        return rawTitle
            .replace(Regex("\\(.*?\\)|\\[.*?\\]|\\{.*?\\}"), " ")
            .replace(Regex("(?i)\\b(feat\\.?|ft\\.?|featuring)\\b.*$"), " ")
            .replace(Regex("(?i)\\|.*$"), " ")
            .replace(
                Regex("(?i)\\b(official|music|video|lyric|lyrics|lyrical|audio|mv|hd|4k|hq|remastered|remaster|version|visualizer|explicit|clean|clip)\\b"),
                " "
            )
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractArtistCandidates(rawArtist: String): List<String> {
        val cleaned = rawArtist
            .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
            .replace(Regex("(?i)\\b(vevo|official|channel)\\b"), "")
            .trim()
        val parts = cleaned
            .split(Regex("(?i)\\s*(?:,|&|/|;|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b|\\bx\\b)\\s*"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts else listOfNotNull(cleaned.takeIf { it.isNotBlank() })
    }

    private fun hasUnwantedVideoVariantKeywords(
        candidateTitle: String,
        candidateArtist: String,
        targetSongTitle: String,
        includeLyricAndAudioExclusion: Boolean,
    ): Boolean {
        val targetTokens = tokenizeWords(targetSongTitle).toSet()
        val candidateTokens = (tokenizeWords(candidateTitle) + tokenizeWords(candidateArtist)).toSet()

        val variantKeywords = buildList {
            addAll(
                listOf(
                    "live", "concert", "tour", "performance",
                    "cover", "covered", "karaoke", "instrumental",
                    "reaction", "react", "reacts", "review",
                    "remix", "remixed", "bootleg", "mashup",
                    "sped", "slowed", "reverb", "nightcore", "8d",
                    "tutorial", "lesson", "chord", "chords",
                    "tribute", "parody", "teaser", "trailer",
                    "making", "behind", "interview", "shorts", "amv", "fanmade"
                )
            )
            if (includeLyricAndAudioExclusion) {
                addAll(listOf("lyrics", "lyric", "lyrical", "audio", "visualizer"))
            }
        }

        for (kw in variantKeywords) {
            if (kw in candidateTokens && kw !in targetTokens) {
                return true
            }
        }
        return false
    }

    /**
     * Strictly verifies that at least one of the song's credited artists matches the candidate's
     * artist/channel name (or, when [allowTitleArtistFallback] is true for record-label YouTube uploads,
     * appears explicitly as the artist in the video title).
     */
    private fun isStrictArtistMatch(
        targetArtistRaw: String,
        candidateArtistRaw: String,
        candidateTitleRaw: String = "",
        allowTitleArtistFallback: Boolean = false,
    ): Boolean {
        val artistCandidates = extractArtistCandidates(targetArtistRaw)
        if (artistCandidates.isEmpty()) return false

        val candArtistClean = candidateArtistRaw
            .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
            .replace(Regex("(?i)(vevo|official|channel|music)$"), "")
            .trim()
        val candArtistTokens = tokenizeWords(candArtistClean)
        val candArtistSpaceless = candArtistTokens.joinToString("")
        val candTitleTokens = tokenizeWords(candidateTitleRaw)

        // Check if any credited artist (especially primary artist) matches candidateArtistRaw directly
        for (artist in artistCandidates) {
            val targetTokens = tokenizeWords(artist).filter { it !in setOf("the", "and") }
            if (targetTokens.isEmpty()) continue
            val targetSpaceless = targetTokens.joinToString("")

            // 1. All tokens of this credited artist appear as whole words in candidate artist/channel
            if (targetTokens.all { it in candArtistTokens }) {
                return true
            }
            // 2. Spaceless channel handle match (e.g. "ColdplayVEVO" / "MahaliniOfficial" / "LastChild")
            if (targetSpaceless.length >= 4 && candArtistSpaceless.isNotEmpty() &&
                (candArtistSpaceless == targetSpaceless ||
                    candArtistSpaceless.contains(targetSpaceless) ||
                    targetSpaceless.contains(candArtistSpaceless))
            ) {
                return true
            }
        }

        // 3. For YouTube videos uploaded by record labels (e.g., "Sun Eater", "Hits Records", "88rising", "HYBE LABELS"),
        // the primary artist's full token sequence must appear in the video title (e.g., "Hindia - Evaluasi").
        if (allowTitleArtistFallback) {
            val primaryTokens = tokenizeWords(artistCandidates.first()).filter { it !in setOf("the", "and") }
            if (primaryTokens.isNotEmpty() && primaryTokens.all { it in candTitleTokens }) {
                return true
            }
        }

        return false
    }

    /**
     * Strictly evaluates whether [candidateTitleRaw] matches [targetTitleRaw] for [targetArtistRaw].
     * Returns `(isMatch, qualityScore)`.
     */
    private fun evaluateStrictTitleMatch(
        targetTitleRaw: String,
        candidateTitleRaw: String,
        targetArtistRaw: String,
    ): Pair<Boolean, Int> {
        val cleanTarget = cleanSongTitleForMatch(targetTitleRaw)
        val targetTokens = tokenizeWords(cleanTarget)
        if (targetTokens.isEmpty()) return false to 0

        val artistTokensSet = extractArtistCandidates(targetArtistRaw)
            .flatMap { tokenizeWords(it) }
            .toSet()

        val cleanedCandidate = cleanSongTitleForMatch(candidateTitleRaw)

        // Build candidate title segments:
        // Many YouTube music videos use "Artist - Song Title" or "Song Title - Artist".
        val dashSegments = cleanedCandidate
            .split(Regex("\\s+[-–—:]\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val candidateVariants = mutableListOf<List<String>>()
        candidateVariants.add(tokenizeWords(cleanedCandidate))

        if (dashSegments.size >= 2) {
            for (seg in dashSegments) {
                val segTokens = tokenizeWords(seg)
                // If a segment is not purely the artist name, treat it as a song title candidate
                if (segTokens.isNotEmpty() && !segTokens.all { it in artistTokensSet }) {
                    candidateVariants.add(segTokens)
                }
            }
        }

        // Also create a variant with leading/trailing artist tokens stripped
        val fullTokens = tokenizeWords(cleanedCandidate)
        val strippedArtistTokens = fullTokens.dropWhile { it in artistTokensSet }
            .dropLastWhile { it in artistTokensSet }
        if (strippedArtistTokens.isNotEmpty()) {
            candidateVariants.add(strippedArtistTokens)
        }

        var bestScore = 0
        for (variant in candidateVariants) {
            if (variant.isEmpty()) continue

            // Exact token list match
            if (variant == targetTokens) {
                bestScore = maxOf(bestScore, 100)
                continue
            }

            // For 1-word song titles (e.g., "Sial", "Evaluasi", "Blue", "Yellow"),
            // require exact match on at least one title variant so "Blue" never matches "Blue Jeans".
            if (targetTokens.size == 1) {
                continue
            }

            // For multi-word titles (2+ words), check if targetTokens appear contiguously in order
            // and the candidate variant has at most 1 extra word.
            if (variant.size <= targetTokens.size + 1) {
                val containsInOrder = (0..(variant.size - targetTokens.size)).any { startIdx ->
                    variant.subList(startIdx, startIdx + targetTokens.size) == targetTokens
                }
                if (containsInOrder) {
                    bestScore = maxOf(bestScore, 85)
                }
            }
        }

        return (bestScore > 0) to bestScore
    }

    /**
     * Search for an official music video for a song with strict Song Title + Artist verification.
     */
    suspend fun searchOfficialMusicVideo(songTitle: String, artistName: String): Result<VideoSearchResult> = runCatching {
        if (songTitle.isBlank() || artistName.isBlank()) {
            throw Exception("Song title and artist name are required")
        }

        val cleanTitle = cleanSongTitleForMatch(songTitle).ifEmpty { songTitle.trim() }
        val primaryArtist = extractArtistCandidates(artistName).firstOrNull() ?: artistName.trim()

        val searchQueries = listOf(
            "$cleanTitle $primaryArtist official music video",
            "$cleanTitle $primaryArtist music video",
            "$cleanTitle $primaryArtist"
        )

        var overallBest: Pair<SongItem, Int>? = null

        for (searchQuery in searchQueries) {
            Log.d(TAG, "searchOfficialMusicVideo: Searching with query: $searchQuery")
            val searchResult = runCatching { YouTube.search(searchQuery, SearchFilter.FILTER_VIDEO).getOrThrow() }

            if (searchResult.isFailure) {
                continue
            }

            val items = searchResult.getOrNull()?.items ?: continue
            if (items.isEmpty()) continue

            for (item in items) {
                if (item !is SongItem) continue

                val rawTitle = item.title
                val titleLower = rawTitle.lowercase()
                val itemArtistsJoined = item.artists.joinToString(", ") { it.name }
                val channelNameLower = itemArtistsJoined.lowercase()

                // Reject static Art Tracks (- Topic)
                if (item.artists.any { it.name.endsWith("- Topic", ignoreCase = true) }) {
                    continue
                }

                // Reject too-short (<60s shorts/teasers) or too-long (>12m compilations) videos
                val durationSec = item.duration
                if (durationSec != null && durationSec !in 60..720) {
                    continue
                }

                // Reject unwanted variants (lyrics video, live, cover, remix, karaoke, reaction, audio, etc.)
                if (hasUnwantedVideoVariantKeywords(
                        candidateTitle = rawTitle,
                        candidateArtist = itemArtistsJoined,
                        targetSongTitle = songTitle,
                        includeLyricAndAudioExclusion = true
                    )
                ) {
                    continue
                }

                // HARD REQUIREMENT 1: Artist MUST strictly match
                val artistMatched = isStrictArtistMatch(
                    targetArtistRaw = artistName,
                    candidateArtistRaw = itemArtistsJoined,
                    candidateTitleRaw = rawTitle,
                    allowTitleArtistFallback = true
                )
                if (!artistMatched) {
                    continue
                }

                // HARD REQUIREMENT 2: Song Title MUST strictly match
                val (titleMatched, titleQualityScore) = evaluateStrictTitleMatch(
                    targetTitleRaw = songTitle,
                    candidateTitleRaw = rawTitle,
                    targetArtistRaw = artistName
                )
                if (!titleMatched) {
                    continue
                }

                var score = titleQualityScore + 50 // Base score for verified Title + Artist match

                if (item.isVideoSong) score += 25
                if (isStrictArtistMatch(artistName, itemArtistsJoined, rawTitle, allowTitleArtistFallback = false)) {
                    // Direct channel/artist credit match (not just title fallback)
                    score += 25
                }
                if (channelNameLower.contains("vevo") || channelNameLower.endsWith("official")) score += 20
                if (titleLower.contains("official")) score += 10
                if (titleLower.contains("music video") || titleLower.contains(" mv ") || titleLower.endsWith(" mv")) score += 10

                Log.d(TAG, "searchOfficialMusicVideo: Verified match '${item.title}' by '$itemArtistsJoined' = $score")

                if (overallBest == null || score > overallBest.second) {
                    overallBest = item to score
                }
            }

            if (overallBest != null && overallBest.second >= 190) {
                break
            }
        }

        val selectedVideo = overallBest?.first
        if (selectedVideo != null) {
            Log.d(TAG, "searchOfficialMusicVideo: Selected verified video '${selectedVideo.title}' (score=${overallBest.second})")
            val channelName = selectedVideo.artists.firstOrNull()?.name ?: artistName
            return@runCatching VideoSearchResult(
                videoId = selectedVideo.id,
                title = selectedVideo.title,
                channelName = channelName,
                thumbnailUrl = selectedVideo.thumbnail
            )
        }

        throw Exception("No verified music video found for '$songTitle' by '$artistName'")
    }

    /**
     * Get video stream URL, with automatic search fallback
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String, isVideoSong: Boolean = true): Result<VideoSearchResult> = runCatching {
        if (isVideoSong) {
            val directSource = getVideoStreamSource(videoId).getOrNull()
            if (directSource != null) {
                return@runCatching VideoSearchResult(
                    videoId = videoId,
                    title = songTitle,
                    channelName = artistName,
                    thumbnailUrl = ""
                )
            }
        } else {
            Log.d(TAG, "Skipping direct lookup for regular (non-video) song, searching for music video instead")
        }

        // Try official YouTube music video search if playable
        val searchResult = searchOfficialMusicVideo(songTitle, artistName).getOrNull()
        if (searchResult != null && getVideoStreamSource(searchResult.videoId).isSuccess) {
            return@runCatching searchResult
        }

        searchResult ?: throw Exception("No video available for this song")
    }

    /**
     * Extract base MIME type without codec parameters.
     * YouTube returns MIME types like "video/webm; codecs=\"vp9\"" which can cause
     * ExoPlayer NAL parsing errors when set as top-level MIME type.
     */
    private fun sanitizeMimeType(mimeType: String?): String {
        if (mimeType == null) return "video/mp4"
        // Extract only the base type (e.g., "video/mp4" from "video/mp4; codecs=\"avc1.4d401f\"")
        return mimeType.split(";").first().trim()
    }

    /**
     * Enhanced MIME type sanitization to prevent NAL parsing errors
     * - Forces MP4 format for problematic streams
     * - Removes problematic codec parameters that cause parsing issues
     */
    private fun sanitizeMimeTypeForExoPlayer(mimeType: String?): String {
        // Keep the real container type. Lying about WebM/3GPP as MP4 can make
        // ExoPlayer select the wrong extractor and produce a black/no-show video.
        return sanitizeMimeType(mimeType)
    }

    /**
     * Check if a MIME type is MP4/H.264 which ExoPlayer handles most reliably
     */
    private fun isMp4Format(mimeType: String?): Boolean {
        val base = sanitizeMimeType(mimeType)
        return base == "video/mp4" || base == "video/3gpp"
    }

    /**
     * Pick the best audio-only stream — prefer the highest-bitrate MP4/M4A
     * (AAC) for ExoPlayer compatibility, falling back to anything else.
     */
    private fun pickBestAudioStream(
        audioStreams: List<org.schabi.newpipe.extractor.stream.AudioStream>
    ): org.schabi.newpipe.extractor.stream.AudioStream? {
        if (audioStreams.isEmpty()) return null
        val mp4Audios = audioStreams.filter {
            val mime = it.format?.mimeType?.split(";")?.first()?.trim()
            mime == "audio/mp4" || mime == "audio/m4a"
        }
        return mp4Audios.maxByOrNull { it.averageBitrate }
            ?: audioStreams.maxByOrNull { it.averageBitrate }
    }

    /**
     * Resolve a video as either a single muxed stream or a pair of video-only +
     * audio-only streams that the caller will merge at the media-source layer.
     *
     * YouTube serves muxed streams only up to 720p (often only 360p in
     * practice). For 1080p and above the only option is a video-only DASH
     * stream paired with a separate audio-only stream — that's why this
     * function returns a [VideoStreamSource.Merged] in those cases.
     */
    suspend fun getVideoStreamSource(videoId: String): Result<VideoStreamSource> = runCatching {
        if (videoId.startsWith("https://") || videoId.startsWith("http://")) {
            return@runCatching VideoStreamSource.Single(videoId, "video/mp4")
        }

        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)

        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams
            val videoOnlyStreams = streamInfo.videoOnlyStreams
            val audioStreams = streamInfo.audioStreams

            Log.d(
                TAG,
                "getVideoStreamSource: ${muxedVideoStreams.size} muxed, " +
                        "${videoOnlyStreams.size} video-only, ${audioStreams.size} audio-only " +
                        "(preferred=${currentPreferredQuality.label})"
            )

            fun mergedSource(): VideoStreamSource.Merged? {
                val videoOnly = findBestStream(videoOnlyStreams, requireAudio = false)
                val audioOnly = pickBestAudioStream(audioStreams)
                val videoUrl = videoOnly?.content ?: videoOnly?.url
                val audioUrl = audioOnly?.content ?: audioOnly?.url
                if (videoOnly != null && audioOnly != null && !videoUrl.isNullOrBlank() && !audioUrl.isNullOrBlank()) {
                    Log.d(
                        TAG,
                        "getVideoStreamSource: Using MERGED video-only ${videoOnly.resolution} " +
                                "+ audio ${audioOnly.averageBitrate}bps"
                    )
                    return VideoStreamSource.Merged(
                        videoUrl = videoUrl,
                        videoMimeType = sanitizeMimeTypeForExoPlayer(videoOnly.format?.mimeType),
                        audioUrl = audioUrl,
                        audioMimeType = sanitizeMimeType(audioOnly.format?.mimeType),
                        height = videoOnly.height,
                    )
                }
                return null
            }

            fun singleSource(): VideoStreamSource.Single? {
                val streamGroups = listOf(
                    muxedVideoStreams.filter { isMp4Format(it.format?.mimeType) },
                    muxedVideoStreams,
                )
                for (streams in streamGroups) {
                    val stream = findBestStream(streams, requireAudio = true)
                    val url = stream?.content ?: stream?.url
                    if (stream != null && !url.isNullOrBlank()) {
                        Log.d(TAG, "getVideoStreamSource: Using SINGLE muxed stream ${stream.resolution}")
                        return VideoStreamSource.Single(
                            url = url,
                            mimeType = sanitizeMimeTypeForExoPlayer(stream.format?.mimeType),
                        )
                    }
                }
                return null
            }

            // When the user asks for above 720p, prefer the video-only + audio-only
            // merged path because muxed streams cap out below that.
            if (currentPreferredQuality.height > 720) {
                mergedSource()?.let { return@runCatching it }
                Log.d(TAG, "getVideoStreamSource: Merged path unavailable, falling back to muxed/single")
            }

            singleSource()?.let { return@runCatching it }

            // Some videos only expose separate streams. Merge them even for
            // <=720p rather than handing ExoPlayer a silent video-only URL.
            mergedSource()?.let { return@runCatching it }
        }

        // Fall back to the older resolver, which includes the YouTube player API
        // fallback if NewPipe cannot extract stream info at all.
        val singleResult = runCatching { getVideoStreamUrl(videoId).getOrThrow() }
            .getOrNull()
            ?: throw Exception("No playable video stream found")

        VideoStreamSource.Single(singleResult.url, singleResult.mimeType)
    }

    /**
     * Get video stream URL using NewPipeExtractor
     * Prioritizes muxed MP4 streams (video+audio, H.264) for best compatibility
     */
    suspend fun getVideoStreamUrl(videoId: String): Result<VideoStreamResult> = runCatching {
        if (videoId.startsWith("https://") || videoId.startsWith("http://")) {
            return@runCatching VideoStreamResult(videoId, "video/mp4")
        }

        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
        
        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams // These have both video and audio
            val videoOnlyStreams = streamInfo.videoOnlyStreams // These are video only (no audio)
            
            Log.d(TAG,"BeatlesVideo: Found ${muxedVideoStreams.size} muxed and ${videoOnlyStreams.size} video-only streams")
            
            // Priority 1: Muxed MP4 streams (best compatibility with ExoPlayer)
            val mp4MuxedStreams = muxedVideoStreams.filter { isMp4Format(it.format?.mimeType) }
            val bestMp4Muxed = findBestStream(mp4MuxedStreams, requireAudio = true)
            if (bestMp4Muxed != null) {
                val url = bestMp4Muxed.content ?: bestMp4Muxed.url
                if (url != null) {
                    Log.d(TAG,"BeatlesVideo: Using muxed MP4 stream with resolution ${bestMp4Muxed.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMp4Muxed.format?.mimeType))
                }
            }

            // Priority 2: Any muxed stream matching preferred quality
            val bestMuxedStream = findBestStream(muxedVideoStreams, requireAudio = true)
            if (bestMuxedStream != null) {
                val url = bestMuxedStream.content ?: bestMuxedStream.url
                if (url != null) {
                    Log.d(TAG,"BeatlesVideo: Using muxed stream with resolution ${bestMuxedStream.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMuxedStream.format?.mimeType))
                }
            }
            
            // Priority 3: Video-only MP4 streams
            val mp4VideoOnlyStreams = videoOnlyStreams.filter { isMp4Format(it.format?.mimeType) }
            val bestMp4VideoOnly = findBestStream(mp4VideoOnlyStreams, requireAudio = false)
            if (bestMp4VideoOnly != null) {
                val url = bestMp4VideoOnly.content ?: bestMp4VideoOnly.url
                if (url != null) {
                    Log.d(TAG,"BeatlesVideo: Using video-only MP4 stream with resolution ${bestMp4VideoOnly.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMp4VideoOnly.format?.mimeType))
                }
            }

            // Priority 4: Any video-only stream
            val bestVideoOnlyStream = findBestStream(videoOnlyStreams, requireAudio = false)
            if (bestVideoOnlyStream != null) {
                val url = bestVideoOnlyStream.content ?: bestVideoOnlyStream.url
                if (url != null) {
                    Log.d(TAG,"BeatlesVideo: Using video-only stream with resolution ${bestVideoOnlyStream.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestVideoOnlyStream.format?.mimeType))
                }
            }
            
            // Last resort: highest quality muxed stream regardless of format
            if (muxedVideoStreams.isNotEmpty()) {
                val fallbackStream = muxedVideoStreams.maxByOrNull { it.bitrate }
                    if (fallbackStream != null) {
                        val url = fallbackStream.content ?: fallbackStream.url
                        if (url != null) {
                            Log.d(TAG,"BeatlesVideo: Using fallback muxed stream with resolution ${fallbackStream.resolution}")
                            return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(fallbackStream.format?.mimeType))
                        }
                    }
            }
            
            // Last resort: highest quality video-only stream
            if (videoOnlyStreams.isNotEmpty()) {
                val fallbackStream = videoOnlyStreams.maxByOrNull { it.bitrate }
                if (fallbackStream != null) {
                    val url = fallbackStream.content ?: fallbackStream.url
                    if (url != null) {
                        Log.d(TAG,"BeatlesVideo: Using fallback video-only stream with resolution ${fallbackStream.resolution}")
                        return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(fallbackStream.format?.mimeType))
                    }
                }
            }
        }

        // Fallback to YouTube player API if NewPipe fails
        Log.d(TAG,"BeatlesVideo: NewPipe failed, trying YouTube player API")
        val fallbackClients = listOf(VISIONOS, ANDROID_VR_1_65_10, IPADOS, WEB_REMIX)
        for (client in fallbackClients) {
            val playerResponse = YouTube.player(videoId, client = client, setLogin = client == WEB_REMIX).getOrNull() ?: continue
            if (playerResponse.playabilityStatus.status != "OK") continue
            val streamingData = playerResponse.streamingData ?: continue

            val needsDeobfuscation = streamingData.formats?.any { it.signatureCipher != null || it.cipher != null } == true ||
                    streamingData.adaptiveFormats.any { it.signatureCipher != null || it.cipher != null }

            val muxedFormats = streamingData.formats ?: emptyList()
            val mp4MuxedFormats = muxedFormats.filter { isMp4Format(it.mimeType) }

            for (formats in listOf(mp4MuxedFormats, muxedFormats)) {
                val bestFormat = findBestFormat(formats, currentPreferredQuality, requireAudio = true)
                var url = bestFormat?.url
                if (url == null && bestFormat != null && needsDeobfuscation) {
                    url = NewPipeExtractor.getStreamUrl(bestFormat, videoId)
                }
                if (url != null) {
                    Log.d(TAG,"BeatlesVideo: Using YouTube (${client.clientName}) muxed format with height ${bestFormat?.height}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeType(bestFormat?.mimeType))
                }
            }

            val adaptiveFormats = streamingData.adaptiveFormats
            val mp4AdaptiveFormats = adaptiveFormats.filter { isMp4Format(it.mimeType) }

            for (formats in listOf(mp4AdaptiveFormats, adaptiveFormats)) {
                val bestFormat = findBestFormat(formats, currentPreferredQuality, requireAudio = false)
                var url = bestFormat?.url
                if (url == null && bestFormat != null && needsDeobfuscation) {
                    url = NewPipeExtractor.getStreamUrl(bestFormat, videoId)
                }
                if (url != null) {
                    Log.d(TAG,"BeatlesVideo: Using YouTube (${client.clientName}) adaptive format with height ${bestFormat?.height}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeType(bestFormat?.mimeType))
                }
            }
        }

        throw Exception("No video URL available")
    }

    /**
     * Find best format based on preferred quality
     */
    private fun findBestFormat(
        formats: List<com.nanzbeatles.innertube.models.response.PlayerResponse.StreamingData.Format>,
        quality: VideoQuality,
        requireAudio: Boolean
    ): com.nanzbeatles.innertube.models.response.PlayerResponse.StreamingData.Format? {
        if (formats.isEmpty()) return null

        // Filter to video formats
        val videoFormats = formats.filter { it.isVideo && it.height != null }

        // Try to find exact quality match first
        val targetHeight = quality.height
        val exactMatch = videoFormats.find { it.height == targetHeight }
        if (exactMatch != null) {
            return exactMatch
        }

        // Quality preference order: try lower qualities first before higher ones
        val qualityOrder = when (quality) {
            VideoQuality.QUALITY_1080P -> listOf(1080, 720, 480, 360)
            VideoQuality.QUALITY_720P -> listOf(720, 480, 360, 1080)
            VideoQuality.QUALITY_480P -> listOf(480, 360, 720, 1080)
            VideoQuality.QUALITY_360P -> listOf(360, 480, 720, 1080)
        }

        for (height in qualityOrder) {
            val match = videoFormats.find { it.height == height }
            if (match != null) {
                return match
            }
        }

        // Last fallback: highest quality available
        return videoFormats.maxByOrNull { it.height ?: 0 }
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean {
        return try {
            // First try NewPipeExtractor
            val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
            if (streamInfo != null) {
                val hasVideo = streamInfo.videoStreams.isNotEmpty() || streamInfo.videoOnlyStreams.isNotEmpty()
                if (hasVideo) return true
            }

            // Fallback to YouTube player API
            val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrNull()
            val streamingData = playerResponse?.streamingData
            
            if (streamingData != null) {
                val hasMuxedVideo = streamingData.formats?.any { it.isVideo } == true
                val hasAdaptiveVideo = streamingData.adaptiveFormats.any { it.isVideo }
                return hasMuxedVideo || hasAdaptiveVideo
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = runCatching {
        val response = YouTube.player(videoId, client = WEB_REMIX).getOrNull()
        val videoDetails = response?.videoDetails
            ?: FallbackAudioResolver.resolveVideoDetails(videoId)
            ?: throw Exception("No video details")

        VideoItem(
            id = videoDetails.videoId,
            title = videoDetails.title ?: "",
            channelName = videoDetails.author ?: "",
            channelId = videoDetails.channelId,
            thumbnailUrl = videoDetails.thumbnail.thumbnails.lastOrNull()?.url ?: "",
            duration = videoDetails.lengthSeconds.toIntOrNull() ?: 0,
            viewCount = videoDetails.viewCount?.toLongOrNull() ?: 0L,
            uploadDate = null,
            description = null,
        )
    }
}
