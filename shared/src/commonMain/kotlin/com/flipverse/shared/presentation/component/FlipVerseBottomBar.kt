package com.flipverse.shared.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.flipverse.shared.domain.BottomBarDestination
import com.flipverse.shared.navigation.BottomBarScreens
import com.flipverse.shared.navigation.MainDashboardRoutes
import org.jetbrains.compose.resources.painterResource


@Composable
fun FlipVerseBottomBar(
    navController: NavController,
    bottomBarNavController: NavHostController
) {
    var selectedItem by rememberSaveable {
        mutableIntStateOf(0)
    }


    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            BottomBarDestination.entries.forEachIndexed { index, destination ->
                val isSelected = selectedItem == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        selectedItem = index

                        val navigateToScreenDestination = when (index) {
                            0 -> MainDashboardRoutes.FlipHomePages
                            1 -> MainDashboardRoutes.FlipExplorePages
                            2 -> MainDashboardRoutes.FlipLiveBookPages
                            3 -> MainDashboardRoutes.FlipChatPages
                            4 -> MainDashboardRoutes.FlipNotifyPages
                            else -> {
                                MainDashboardRoutes.FlipHomePages
                            }
                        }

                        bottomBarNavController.navigate(navigateToScreenDestination) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (index == 4) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = if (index == 4) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else Color.Transparent,
                                        modifier = Modifier.size(6.dp)
                                    )
                                }
                            ) {
                                Icon(
                                    painterResource(destination.icon),
                                    contentDescription = destination.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else if (index == 2) { // FlipLiveBook with animation
                            AnimatedFlipLiveBookIcon(
                                icon = destination.icon,
                                contentDescription = destination.title,
                                size = 24.dp,
                                tint = if (isSelected) Color.White else Color.Gray
                            )
                        } else {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = destination.title,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { /* Optional: Text(destination.title) */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color.Black
                    )
                )
            }
        }
    }

}

