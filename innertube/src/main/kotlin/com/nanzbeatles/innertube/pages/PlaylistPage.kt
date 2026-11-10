package com.nanzbeatles.innertube.pages

import com.nanzbeatles.innertube.models.Album
import com.nanzbeatles.innertube.models.Artist
import com.nanzbeatles.innertube.models.MusicResponsiveListItemRenderer
import com.nanzbeatles.innertube.models.PlaylistItem
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.innertube.models.oddElements
import com.nanzbeatles.innertube.utils.parseTime

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
            val watchEndpoint = PageHelper.watchEndpoint(renderer)

            val videoId = PageHelper.videoId(renderer)?.takeIf { it.isNotBlank() }
                ?: renderer.playlistItemData?.videoId?.takeIf { it.isNotBlank() }
                ?: renderer.navigationEndpoint?.anyWatchEndpoint?.videoId?.takeIf { it.isNotBlank() }
                ?: renderer.flexColumns.firstNotNullOfOrNull { col ->
                    col.musicResponsiveListItemFlexColumnRenderer.text?.runs?.firstNotNullOfOrNull { run ->
                        run.navigationEndpoint?.anyWatchEndpoint?.videoId?.takeIf { it.isNotBlank() }
                    }
                }
                ?: renderer.menu?.menuRenderer?.items?.firstNotNullOfOrNull { item ->
                    item.menuNavigationItemRenderer?.navigationEndpoint?.anyWatchEndpoint?.videoId?.takeIf { it.isNotBlank() }
                        ?: item.menuServiceItemRenderer?.serviceEndpoint?.queueAddEndpoint?.queueTarget?.videoId?.takeIf { it.isNotBlank() }
                        ?: item.menuServiceItemRenderer?.serviceEndpoint?.anyWatchEndpoint?.videoId?.takeIf { it.isNotBlank() }
                }
                ?: return null

            val title = renderer.flexColumns.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text
                ?.runs?.firstOrNull()?.text?.takeIf { it.isNotBlank() }
                ?: PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text?.takeIf { it.isNotBlank() }
                ?: renderer.flexColumns.firstNotNullOfOrNull { col ->
                    col.musicResponsiveListItemFlexColumnRenderer.text?.runs?.firstOrNull()?.text?.takeIf { it.isNotBlank() }
                }
                ?: return null

            val artists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST").map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            }.ifEmpty {
                renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }.orEmpty()
            }

            val album = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ALBUM").firstOrNull()?.let {
                val browseId = it.navigationEndpoint?.browseEndpoint?.browseId
                if (browseId != null) Album(name = it.text, id = browseId) else null
            } ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                Album(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                )
            }

            val duration = renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.parseTime()
                ?: renderer.flexColumns.lastOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.lastOrNull()?.text?.parseTime()

            return SongItem(
                id = videoId,
                title = title,
                artists = artists,
                album = album,
                duration = duration,
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: renderer.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                    ?: "",
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = watchEndpoint ?: com.nanzbeatles.innertube.models.WatchEndpoint(videoId = videoId),
                setVideoId = PageHelper.playlistSetVideoId(renderer),
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }
    }
}
