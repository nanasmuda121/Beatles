@file:OptIn(ExperimentalSerializationApi::class)

package com.nanzbeatles.innertube.models

import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_LIBRARY_ARTIST
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_NON_MUSIC_AUDIO_TRACK_PAGE
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_USER_CHANNEL
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Typical list item
 * Used in [MusicCarouselShelfRenderer], [MusicShelfRenderer]
 * Appears in quick picks, search results, table items, etc.
 */
@Serializable
data class MusicResponsiveListItemRenderer(
    val badges: List<Badges>? = null,
    val fixedColumns: List<FlexColumn>? = null,
    val flexColumns: List<FlexColumn> = emptyList(),
    val thumbnail: ThumbnailRenderer? = null,
    val menu: Menu? = null,
    val playlistItemData: PlaylistItemData? = null,
    val overlay: Overlay? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
) {
    val isSong: Boolean
        get() = (navigationEndpoint == null || navigationEndpoint.watchEndpoint?.videoId != null || playlistItemData?.videoId?.isNotBlank() == true) && !isPlaylist && !isAlbum && !isArtist
    val isPlaylist: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_PLAYLIST ||
                navigationEndpoint?.browseEndpoint?.isPlaylistEndpoint == true ||
                (navigationEndpoint?.watchPlaylistEndpoint?.playlistId != null && navigationEndpoint.watchEndpoint?.videoId == null && playlistItemData?.videoId.isNullOrBlank()) ||
                (navigationEndpoint?.watchEndpoint?.playlistId != null && navigationEndpoint.watchEndpoint.videoId == null && playlistItemData?.videoId.isNullOrBlank())
    val isAlbum: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_AUDIOBOOK ||
                navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true ||
                navigationEndpoint?.browseEndpoint?.browseId?.startsWith("FEmusic_library_privately_owned_release_detail") == true
    val isArtist: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_ARTIST
                || navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_LIBRARY_ARTIST
    val isPodcast: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE
    val isUserChannel: Boolean
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_USER_CHANNEL
    val isEpisode: Boolean
        get() {
            if (navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType == MUSIC_PAGE_TYPE_NON_MUSIC_AUDIO_TRACK_PAGE) {
                return true
            }
            val firstSubtitleText = flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text?.runs?.firstOrNull()?.text
            if (firstSubtitleText == "Episode") {
                return true
            }
            val hasPodcastLink = flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text?.runs?.any { run ->
                    run.navigationEndpoint?.browseEndpoint
                        ?.browseEndpointContextSupportedConfigs
                        ?.browseEndpointContextMusicConfig
                        ?.pageType == MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE
                } == true
            val hasVideoId = playlistItemData?.videoId != null ||
                navigationEndpoint?.watchEndpoint?.videoId != null
            return hasPodcastLink && hasVideoId
        }

    val musicVideoType: String?
        get() =
            overlay
                ?.musicItemThumbnailOverlayRenderer
                ?.content
                ?.musicPlayButtonRenderer
                ?.playNavigationEndpoint
                ?.musicVideoType
                ?: navigationEndpoint?.musicVideoType

    @Serializable
    data class FlexColumn(
        @JsonNames("musicResponsiveListItemFixedColumnRenderer")
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer = MusicResponsiveListItemFlexColumnRenderer(null),
    ) {
        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer(
            val text: Runs? = null,
        )
    }

    @Serializable
    data class PlaylistItemData(
        val playlistSetVideoId: String? = null,
        val videoId: String = "",
    )

    @Serializable
    data class Overlay(
        val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer = MusicItemThumbnailOverlayRenderer(),
    ) {
        @Serializable
        data class MusicItemThumbnailOverlayRenderer(
            val content: Content = Content(),
        ) {
            @Serializable
            data class Content(
                val musicPlayButtonRenderer: MusicPlayButtonRenderer? = null,
            ) {
                @Serializable
                data class MusicPlayButtonRenderer(
                    val playNavigationEndpoint: NavigationEndpoint? = null,
                )
            }
        }
    }
}
