package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicEditablePlaylistDetailHeaderRenderer(
    val header: Header = Header(null, null),
    val editHeader: EditHeader = EditHeader(null)
) {
    @Serializable
    data class Header(
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer? = null,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer? = null
    )

    @Serializable
    data class EditHeader(
        val musicPlaylistEditHeaderRenderer: MusicPlaylistEditHeaderRenderer? = null
    )
}

@Serializable
data class MusicDetailHeaderRenderer(
    val title: Runs = Runs(),
    val subtitle: Runs = Runs(),
    val secondSubtitle: Runs = Runs(),
    val description: Runs? = null,
    val thumbnail: ThumbnailRenderer = ThumbnailRenderer(null, null, null),
    val menu: Menu = Menu(),
)

@Serializable
data class MusicPlaylistEditHeaderRenderer(
    val editTitle: Runs? = null
)
