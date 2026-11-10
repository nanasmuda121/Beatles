package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicDescriptionShelfRenderer(
    val header: Runs? = null,
    val subheader: Runs? = null,
    val description: Runs = Runs(),
    val footer: Runs? = null,
)
