package com.flipverse.shared.domain

import com.flipverse.shared.Resources
import com.flipverse.shared.navigation.BottomBarScreens
import com.flipverse.shared.navigation.Screen
import org.jetbrains.compose.resources.DrawableResource

/**
 * Bottom bar navigation destinations for the main dashboard.
 * 
 * Note: FlipLiveBook features a continuous animated icon with pulsing/glowing effects
 * to draw user attention as the flagship feature.
 */
enum class BottomBarDestination(
    val icon: DrawableResource,
    val title: String,
    val screen: Screen
) {
    FlipHome(
        icon = Resources.Icon.FlipHome,
        title = "Flipverse",
        screen = BottomBarScreens.FlipHome
    ),

    FlipExplore(
        icon = Resources.Icon.FlipExplore,
        title = "Explore",
        screen = BottomBarScreens.FlipExplore
    ),

    FlipLiveBook(
        icon = Resources.Icon.FlipLiveBook,
        title = "LiveBook",
        screen = BottomBarScreens.FlipLiveBook
    ),

    FlipChat(
        icon = Resources.Icon.FlipChat,
        title = "Flip",
        screen = BottomBarScreens.FlipChat
    ),

    FlipNotify(
        icon = Resources.Icon.FlipNotify,
        title = "Alerts",
        screen = BottomBarScreens.FlipNotify
    )
}