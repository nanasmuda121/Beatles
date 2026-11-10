package com.nanzbeatles.innertube.pages

import com.nanzbeatles.innertube.models.Album
import com.nanzbeatles.innertube.models.AlbumItem
import com.nanzbeatles.innertube.models.Artist
import com.nanzbeatles.innertube.models.MusicResponsiveHeaderRenderer
import com.nanzbeatles.innertube.models.MusicResponsiveListItemRenderer
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.innertube.models.getItems
import com.nanzbeatles.innertube.models.oddElements
import com.nanzbeatles.innertube.models.response.BrowseResponse
import com.nanzbeatles.innertube.models.splitBySeparator
import com.nanzbeatles.innertube.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
) {
    companion object {
        fun getPlaylistId(response: BrowseResponse): String? {
            val urlCanonical = response.microformat?.microformatDataRenderer?.urlCanonical
            var playlistId = urlCanonical?.substringAfter("list=", "")?.substringBefore("&")?.takeIf { it.isNotBlank() }
                ?: urlCanonical?.takeIf { it.contains("=") }?.substringAfterLast('=')?.substringBefore("&")
            if (playlistId.isNullOrEmpty()) {
                playlistId = getHeader(response)?.buttons?.firstNotNullOfOrNull {
                    it.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint?.playlistId
                        ?: it.menuRenderer?.items?.firstNotNullOfOrNull { item ->
                            item.menuNavigationItemRenderer?.navigationEndpoint?.anyWatchEndpoint?.playlistId
                                ?: item.menuServiceItemRenderer?.serviceEndpoint?.anyWatchEndpoint?.playlistId
                        }
                }
            }
            if (playlistId.isNullOrEmpty()) {
                playlistId = response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()
                    ?.buttonRenderer?.navigationEndpoint?.anyWatchEndpoint?.playlistId
            }
            if (playlistId.isNullOrEmpty()) {
                playlistId = getItemRenderers(response).firstNotNullOfOrNull {
                    PageHelper.watchEndpoint(it)?.playlistId
                }
            }
            return playlistId
        }

        fun getTitle(response: BrowseResponse): String? {
            val title = getHeader(response)?.title?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicDetailHeaderRenderer?.title?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer?.title?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicHeaderRenderer?.title?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicImmersiveHeaderRenderer?.title?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicVisualHeaderRenderer?.title?.takeIf { !it.runs.isNullOrEmpty() }
            return title?.runs?.firstOrNull()?.text?.takeIf { it.isNotBlank() }
        }

        fun getYear(response: BrowseResponse): Int? {
            val subtitle = getHeader(response)?.subtitle?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicDetailHeaderRenderer?.subtitle?.takeIf { !it.runs.isNullOrEmpty() }
                ?: response.header?.musicHeaderRenderer?.subtitle?.takeIf { !it.runs.isNullOrEmpty() }
            return subtitle?.runs?.lastOrNull()?.text?.toIntOrNull()
                ?: subtitle?.runs?.firstNotNullOfOrNull { it.text.toIntOrNull() }
        }

        fun getThumbnail(response: BrowseResponse): String? {
            return getHeader(response)?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: getHeader(response)?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                ?: response.background?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicHeaderRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
        }

        fun getArtists(response: BrowseResponse): List<Artist> {
            val header = getHeader(response)
            val straplineArtists = header?.straplineTextOne?.runs?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            }?.takeIf { it.isNotEmpty() }

            if (straplineArtists != null) return straplineArtists

            val headerSubtitleArtists = header?.subtitle?.runs
                ?.filter { it.navigationEndpoint?.browseEndpoint != null }
                ?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }?.takeIf { it.isNotEmpty() }

            if (headerSubtitleArtists != null) return headerSubtitleArtists

            val detailSubtitleArtists = response.header?.musicDetailHeaderRenderer?.subtitle?.runs
                ?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }?.takeIf { it.isNotEmpty() }
                ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs
                    ?.filter { it.navigationEndpoint?.browseEndpoint != null }
                    ?.map {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    }

            return detailSubtitleArtists ?: emptyList()
        }

        fun getHeader(response: BrowseResponse): MusicResponsiveHeaderRenderer? {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val tabContents = tabs?.flatMap { it?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty() }
            val directContents = response.contents?.sectionListRenderer?.contents
            val secondaryContents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents
            val allSections = tabContents.orEmpty() + directContents.orEmpty() + secondaryContents.orEmpty()
            return response.header?.musicResponsiveHeaderRenderer
                ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
                ?: allSections.firstNotNullOfOrNull { section ->
                    section.musicResponsiveHeaderRenderer
                        ?: section.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
                }
        }

        private fun getItemRenderers(response: BrowseResponse): List<MusicResponsiveListItemRenderer> {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val tabSectionContents = tabs?.flatMap { it?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty() }
            val secondarySectionContents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents
            val directSectionContents = response.contents?.sectionListRenderer?.contents

            val shelfItems = secondarySectionContents?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer?.contents?.getItems()?.takeIf { list -> list.isNotEmpty() } }
                ?: secondarySectionContents?.firstNotNullOfOrNull { it.musicShelfRenderer?.contents?.getItems()?.takeIf { list -> list.isNotEmpty() } }
                ?: tabSectionContents?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer?.contents?.getItems()?.takeIf { list -> list.isNotEmpty() } }
                ?: tabSectionContents?.firstNotNullOfOrNull { it.musicShelfRenderer?.contents?.getItems()?.takeIf { list -> list.isNotEmpty() } }
                ?: directSectionContents?.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer?.contents?.getItems()?.takeIf { list -> list.isNotEmpty() } }
                ?: directSectionContents?.firstNotNullOfOrNull { it.musicShelfRenderer?.contents?.getItems()?.takeIf { list -> list.isNotEmpty() } }

            return shelfItems ?: emptyList()
        }

        fun getSongs(response: BrowseResponse, album: AlbumItem): List<SongItem> {
            return getItemRenderers(response).mapNotNull {
                getSong(it, album)
            }
        }

        fun getSong(renderer: MusicResponsiveListItemRenderer, album: AlbumItem? = null): SongItem? {
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
            val watchEndpoint = PageHelper.watchEndpoint(renderer)
            val videoId = PageHelper.videoId(renderer)?.takeIf { it.isNotBlank() }
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

            val title = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text?.takeIf { it.isNotBlank() }
                ?: renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.takeIf { it.isNotBlank() }
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
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }.orEmpty()
            }.ifEmpty {
                album?.artists ?: emptyList()
            }

            val resolvedAlbum = album?.let {
                Album(it.title, it.browseId)
            } ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let { run ->
                val albumBrowseId = run.navigationEndpoint?.browseEndpoint?.browseId
                if (albumBrowseId != null) {
                    Album(name = run.text, id = albumBrowseId)
                } else null
            } ?: Album(name = album?.title.orEmpty(), id = album?.browseId.orEmpty())

            val duration = renderer.fixedColumns?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                ?.text?.parseTime()
                ?: renderer.flexColumns.lastOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.lastOrNull()
                    ?.text?.parseTime()
                ?: 0

            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: album?.thumbnail
                ?: ""

            return SongItem(
                id = videoId,
                title = title,
                artists = artists,
                album = resolvedAlbum,
                duration = duration,
                musicVideoType = renderer.musicVideoType,
                thumbnail = thumbnail,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = watchEndpoint,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }
    }
}

