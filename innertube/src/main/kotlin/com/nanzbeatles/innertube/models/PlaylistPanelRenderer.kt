package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelRenderer(
    val title: String? = null,
    val titleText: Runs? = null,
    val shortBylineText: Runs? = null,
    val contents: List<Content> = emptyList(),
    val isInfinite: Boolean? = null,
    val numItemsToShow: Int? = null,
    val playlistId: String? = null,
    val continuations: List<Continuation>? = null,
) {
    @Serializable
    data class Content(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
        val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer? = null,
    )
}
