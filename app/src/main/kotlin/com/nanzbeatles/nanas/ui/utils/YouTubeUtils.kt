/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.utils

private val YT_VIDEO_ID_FROM_URL_REGEX = Regex("""(?:/vi(?:_webp)?/)([a-zA-Z0-9_-]{11})""")
private val YT_RAW_ID_REGEX = Regex("""^[a-zA-Z0-9_-]{11}$""")

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    val isGoogleCdn = this.contains("googleusercontent.com") || this.contains("ggpht.com")
    val isYtimg = this.contains("ytimg.com") || this.contains("img.youtube.com")

    if (isGoogleCdn) {
        val w = width ?: height!!
        val h = height ?: width!!

        // Replace existing wNNN-hNNN segments anywhere in the URL/path
        if (this.contains(Regex("w\\d+-h\\d+"))) {
            return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
        }

        // Trim any existing size parameters (=w..., =s..., =h...) so we can rebuild them
        val baseUrl = this.split("=w", "=s", "=h", limit = 2)[0]

        // Use =w-h-p-l90-rj for explicit dimensions / banners (smart cropping, high quality JPEG),
        // otherwise use =s for square-ish images.
        return if ((this.contains("=w") && this.contains("-h")) || (width != null && height != null)) {
            "$baseUrl=w$w-h$h-p-l90-rj"
        } else {
            "$baseUrl=s$w-p-l90-rj"
        }
    }

    if (isYtimg) {
        val target = width ?: height!!
        val cleanBase = this.substringBefore("?")
        val extractedId = YT_VIDEO_ID_FROM_URL_REGEX.find(this)?.groupValues?.getOrNull(1)

        // Repair previously broken maxresdefault URLs (which 404 when signed with ?sqp= or on non-HD videos)
        if (cleanBase.contains("maxresdefault")) {
            return if (extractedId != null) {
                "https://i.ytimg.com/vi/$extractedId/hqdefault.jpg"
            } else {
                cleanBase
                    .replace("/vi_webp/", "/vi/")
                    .replace("maxresdefault.webp", "hqdefault.jpg")
                    .replace("maxresdefault.jpg", "hqdefault.jpg")
            }
        }

        // When upgrading small default/mqdefault thumbnails to hqdefault, strip ?sqp= signature
        // because YouTube CDN returns HTTP 404 if the filename does not match the signed ?sqp= parameter.
        return when {
            target > 320 && (cleanBase.endsWith("/default.jpg") || cleanBase.endsWith("/mqdefault.jpg") ||
                cleanBase.endsWith("/default.webp") || cleanBase.endsWith("/mqdefault.webp")) -> {
                if (extractedId != null) {
                    "https://i.ytimg.com/vi/$extractedId/hqdefault.jpg"
                } else {
                    cleanBase
                        .replace("/vi_webp/", "/vi/")
                        .replace("mqdefault.webp", "hqdefault.jpg")
                        .replace("default.webp", "hqdefault.jpg")
                        .replace("mqdefault.jpg", "hqdefault.jpg")
                        .replace("default.jpg", "hqdefault.jpg")
                }
            }
            else -> this
        }
    }

    return this
}

/**
 * Returns a high quality version of the thumbnail URL.
 * Useful for large album art in player and album detail screens.
 */
fun String?.toHighQualityThumbnail(): String? {
    if (this.isNullOrBlank()) return null
    return this.resize(1200, 1200)
}

/**
 * Extracts a valid 11-character YouTube video ID from either [mediaId] or [artworkUri].
 */
fun extractYouTubeVideoId(mediaId: String?, artworkUri: String? = null): String? {
    val cleanId = mediaId?.trim()
    if (!cleanId.isNullOrEmpty() && YT_RAW_ID_REGEX.matches(cleanId)) {
        return cleanId
    }
    if (!artworkUri.isNullOrBlank()) {
        YT_VIDEO_ID_FROM_URL_REGEX.find(artworkUri)?.groupValues?.getOrNull(1)?.let { return it }
    }
    return null
}

/**
 * Builds a guaranteed-existing standard YouTube `hqdefault.jpg` fallback URL for a video/song ID.
 */
fun fallbackYtThumbnailUrl(mediaId: String?, artworkUri: String? = null): String? {
    val videoId = extractYouTubeVideoId(mediaId, artworkUri) ?: return null
    return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}

/**
 * Resolves a playable item's artwork URL, repairing broken `maxresdefault` URLs and falling back
 * to `https://i.ytimg.com/vi/<videoId>/hqdefault.jpg` when [artworkUri] is null or blank.
 */
fun resolveArtworkUrl(artworkUri: String?, mediaId: String? = null): String? {
    val trimmed = artworkUri?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    if (trimmed != null) {
        return trimmed.resize(1200, 1200)
    }
    return fallbackYtThumbnailUrl(mediaId, artworkUri)
}
