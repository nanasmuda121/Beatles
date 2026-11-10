package com.nanzbeatles.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Menu(
    val menuRenderer: MenuRenderer = MenuRenderer(),
) {
    @Serializable
    data class MenuRenderer(
        val items: List<Item>? = null,
        val topLevelButtons: List<TopLevelButton>? = null,
    ) {
        @Serializable
        data class Item(
            val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
            val menuServiceItemRenderer: MenuServiceItemRenderer? = null,
            val toggleMenuServiceItemRenderer: ToggleMenuServiceRenderer? = null,
        ) {
            @Serializable
            data class MenuNavigationItemRenderer(
                val text: Runs = Runs(),
                val icon: Icon = Icon(),
                val navigationEndpoint: NavigationEndpoint = NavigationEndpoint(),
            )

            @Serializable
            data class MenuServiceItemRenderer(
                val text: Runs = Runs(),
                val icon: Icon = Icon(),
                val serviceEndpoint: NavigationEndpoint = NavigationEndpoint(),
            )
            @Serializable
            data class ToggleMenuServiceRenderer(
                val defaultIcon: Icon = Icon(),
                val defaultServiceEndpoint: DefaultServiceEndpoint = DefaultServiceEndpoint(null, null),
                val toggledServiceEndpoint: ToggledServiceEndpoint? = null,
            )
        }

        @Serializable
        data class TopLevelButton(
            val buttonRenderer: ButtonRenderer? = null,
        ) {
            @Serializable
            data class ButtonRenderer(
                val icon: Icon = Icon(),
                val navigationEndpoint: NavigationEndpoint = NavigationEndpoint(),
            )
        }
    }
}
