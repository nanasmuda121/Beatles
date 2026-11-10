package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail> = emptyList(),
)

@Serializable
data class Thumbnail(
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null,
)
