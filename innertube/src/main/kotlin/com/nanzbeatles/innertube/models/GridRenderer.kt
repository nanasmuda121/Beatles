package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class GridRenderer(
    val header: Header? = null,
    val items: List<Item> = emptyList(),
    val continuations: List<Continuation>? = null,
) {
    @Serializable
    data class Header(
        val gridHeaderRenderer: GridHeaderRenderer? = null,
    ) {
        @Serializable
        data class GridHeaderRenderer(
            val title: Runs? = null,
        )
    }

    @Serializable
    data class Item(
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer? = null,
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
    )
}
