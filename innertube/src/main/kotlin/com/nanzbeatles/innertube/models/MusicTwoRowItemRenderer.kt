package com.nanzbeatles.innertube.models

import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.nanzbeatles.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import kotlinx.serialization.Serializable

/**
 * Two row: a big thumbnail, a title, and a subtitle
 * Used in [GridRenderer] and [MusicCarouselShelfRenderer]
 * Item type: song, video, album, playlist, artist
 */
@Serializable
data class MusicTwoRowItemRenderer(
    val title: Runs = Runs(),
    val subtitle: Runs? = null,
    val subtitleBadges: List<Badges>? = null,
    val menu: Menu? = null,
    val thumbnailRenderer: ThumbnailRenderer = ThumbnailRenderer(null, null, null),
    val navigationEndpoint: NavigationEndpoint = NavigationEndpoint(),
    val thumbnailOverlay: MusicResponsiveListItemRenderer.Overlay? = null,
) {
    val isSong: Boolean
        get() = navigationEndpoint.watchEndpoint?.videoId != null
    val isPlaylist: Boolean
        get() =
            navigationEndpoint.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_PLAYLIST ||
                navigationEndpoint.browseEndpoint?.isPlaylistEndpoint == true ||
                (navigationEndpoint.watchPlaylistEndpoint?.playlistId != null && navigationEndpoint.watchEndpoint?.videoId == null) ||
                (navigationEndpoint.watchEndpoint?.playlistId != null && navigationEndpoint.watchEndpoint.videoId == null)
    val isAlbum: Boolean
        get() =
            navigationEndpoint.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType ==
                MUSIC_PAGE_TYPE_AUDIOBOOK ||
                navigationEndpoint.browseEndpoint?.browseId?.startsWith("MPREb_") == true ||
                navigationEndpoint.browseEndpoint?.browseId?.startsWith("FEmusic_library_privately_owned_release_detail") == true
    val isArtist: Boolean
        get() =
            navigationEndpoint.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_ARTIST

    val musicVideoType: String?
        get() =
            thumbnailOverlay
                ?.musicItemThumbnailOverlayRenderer
                ?.content
                ?.musicPlayButtonRenderer
                ?.playNavigationEndpoint
                ?.musicVideoType
                ?: navigationEndpoint.musicVideoType
}
