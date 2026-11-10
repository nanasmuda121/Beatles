package com.nanzbeatles.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer? = null,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer? = null,
) {
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?
        get() = musicThumbnailRenderer

    constructor(
        musicThumbnailRenderer: MusicThumbnailRenderer?,
        musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
        croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
    ) : this(
        musicThumbnailRenderer = musicThumbnailRenderer ?: croppedSquareThumbnailRenderer,
        musicAnimatedThumbnailRenderer = musicAnimatedThumbnailRenderer,
    )
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails = Thumbnails(),
        val thumbnailCrop: String? = null,
        val thumbnailScale: String? = null,
    ) {
        fun getThumbnailUrl() = thumbnail.thumbnails.lastOrNull()?.url
    }

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails = Thumbnails(),
        val backupRenderer: MusicThumbnailRenderer = MusicThumbnailRenderer(),
    )
}
