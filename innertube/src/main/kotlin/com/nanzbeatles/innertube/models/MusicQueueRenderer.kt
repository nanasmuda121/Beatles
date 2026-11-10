package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicQueueRenderer(
    val content: Content? = null,
    val header: Header? = null,
) {
    @Serializable
    data class Content(
        val playlistPanelRenderer: PlaylistPanelRenderer? = null,
    )

    @Serializable
    data class Header(
        val musicQueueHeaderRenderer: MusicQueueHeaderRenderer? = null,
    ) {
        @Serializable
        data class MusicQueueHeaderRenderer(
            val title: Runs? = null,
            val subtitle: Runs? = null,
        )
    }
}
