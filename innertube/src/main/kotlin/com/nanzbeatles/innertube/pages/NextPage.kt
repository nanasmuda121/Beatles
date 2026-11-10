package com.nanzbeatles.innertube.pages

import com.nanzbeatles.innertube.models.Album
import com.nanzbeatles.innertube.models.Artist
import com.nanzbeatles.innertube.models.BrowseEndpoint
import com.nanzbeatles.innertube.models.PlaylistPanelVideoRenderer
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.innertube.models.WatchEndpoint
import com.nanzbeatles.innertube.models.oddElements
import com.nanzbeatles.innertube.models.splitBySeparator
import com.nanzbeatles.innertube.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = (renderer.longBylineText ?: renderer.shortBylineText)?.runs?.splitBySeparator()
        val videoId = renderer.videoId?.takeIf { it.isNotBlank() }
            ?: renderer.navigationEndpoint.anyWatchEndpoint?.videoId?.takeIf { it.isNotBlank() }
            ?: return null

        // Extract library tokens using the new method that properly handles multiple toggle items
        val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

        return SongItem(
            id = videoId,
            title =
                renderer.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists =
                longByLineRuns?.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }.orEmpty(),
            album =
                longByLineRuns
                    ?.getOrNull(1)
                    ?.firstOrNull()
                    ?.takeIf {
                        it.navigationEndpoint?.browseEndpoint != null
                    }?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                        )
                    },
            duration =
                renderer.lengthText
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                    ?.parseTime() ?: 0,
            musicVideoType = renderer.navigationEndpoint.musicVideoType,
            thumbnail =
                renderer.thumbnail.thumbnails
                    .lastOrNull()
                    ?.url.orEmpty(),
            explicit =
                renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
            endpoint = renderer.navigationEndpoint.anyWatchEndpoint ?: WatchEndpoint(videoId = videoId),
            setVideoId = renderer.playlistSetVideoId,
            libraryAddToken = libraryTokens.addToken,
            libraryRemoveToken = libraryTokens.removeToken
        )
    }
}
