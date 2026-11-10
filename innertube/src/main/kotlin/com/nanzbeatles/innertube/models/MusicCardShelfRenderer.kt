package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicCardShelfRenderer(
    val title: Runs = Runs(),
    val subtitle: Runs = Runs(),
    val thumbnail: ThumbnailRenderer = ThumbnailRenderer(null, null, null),
    val header: Header? = null,
    val contents: List<Content>? = null,
    val buttons: List<Button> = emptyList(),
    val onTap: NavigationEndpoint = NavigationEndpoint(),
    val subtitleBadges: List<Badges>? = null,
) {
    @Serializable
    data class Header(
        val musicCardShelfHeaderBasicRenderer: MusicCardShelfHeaderBasicRenderer = MusicCardShelfHeaderBasicRenderer(),
    ) {
        @Serializable
        data class MusicCardShelfHeaderBasicRenderer(
            val title: Runs = Runs(),
        )
    }

    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
    )
}
