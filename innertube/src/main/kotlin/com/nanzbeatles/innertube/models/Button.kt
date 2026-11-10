package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Button(
    val buttonRenderer: ButtonRenderer = ButtonRenderer(),
) {
    @Serializable
    data class ButtonRenderer(
        val text: Runs = Runs(),
        val navigationEndpoint: NavigationEndpoint? = null,
        val command: NavigationEndpoint? = null,
        val icon: Icon? = null,
    )
}
