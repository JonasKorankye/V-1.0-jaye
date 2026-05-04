package com.flipverse.livebook.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.domain.FabItem
import com.flipverse.shared.domain.TaggedUser
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipLiveBookScreen(
    navigateToProfile: () -> Unit,
    navigateToTaggedUserProfile: (String) -> Unit,
    navigateToNewLiveBookScreen: () -> Unit,
    navigateToViewParticipantScreen: (String) -> Unit,
    navigateToContinueLiveBookScreen: () -> Unit,
) {

    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val fabMenuItems = listOf(
        FabItem(
            "New Live Book",
            painterResource(Resources.Icon.FlipLiveBook)
        ) { navigateToNewLiveBookScreen() },
//        FabItem(
//            "Continue Live Book",
//            painterResource(Resources.Icon.FlipLiveBook)
//        ) { navigateToContinueLiveBookScreen() },
    )
    val tabs = listOf("Open", "Closed", "Board")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        modifier = Modifier.padding(6.dp),
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = Strings.livebook_title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.CenterStart

                    ) {
                        Image(
                            painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoDark else Resources.Image.AppLogoWhite),
                            contentDescription = Strings.app_logo,
                            modifier = Modifier
                                .wrapContentSize()
                                .height(40.dp),

                            )
                    }
                },
                actions = {
                    if (getThumbnail().isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { navigateToProfile() }
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = Alpha.HALF
                                    )
                                ) //  color for the circle
                                .border(2.dp, Color.Transparent, CircleShape), //  border color
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = getFullName().take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontFamily = WorkSansBoldFont()
                            )

                        }
                    } else {
                        AsyncImage(
                            modifier = Modifier.size(40.dp)
                                .clip(CircleShape)
                                .clickable { navigateToProfile() }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            model = ImageRequest.Builder(
                                LocalPlatformContext.current
                            ).data(getThumbnail())
                                .crossfade(enable = true)
                                .build(),
                            contentDescription = Strings.user_profile_thumbnail,
                            contentScale = ContentScale.Crop
                        )

                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            // Floating Action Button with expand/collapse functionality
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(
                    bottom = 100.dp,
                    end = 0.dp,
                ) // Position above bottom bar
            ) {
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        fabMenuItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = item.text,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { item.onClick() }
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FloatingActionButton(
                                    onClick = item.onClick,
                                    modifier = Modifier.size(48.dp),
                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentColor = Color.Black,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = item.icon,
                                        modifier = Modifier.size(24.dp),
                                        contentDescription = item.text,
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isFabMenuExpanded) vectorResource(Resources.Icon.Close) else vectorResource(
                            Resources.Icon.Add
                        ),
                        contentDescription = if (isFabMenuExpanded) Strings.close else "Expand",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
//                .padding(horizontal = 16.dp)
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                )
                .blur(if (isFabMenuExpanded) 12.dp else 0.dp),
//                .padding(paddingValues)
        ) {
            SwipeableTabs(tabs = tabs, pagerState = pagerState)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OpenTabScreen(
                        onViewParticipants = { id ->
                            navigateToViewParticipantScreen(id)
                        },
                        refreshKey = pagerState.currentPage
                    )

                    1 -> ClosedTabScreen(
                        refreshKey = pagerState.currentPage,
                        onViewStory = { id ->
                            navigateToViewParticipantScreen(id)
                        }
                    )

                    2 -> LeaderBoardTabScreen(
                        refreshKey = pagerState.currentPage,
                        onImageClick = { id ->
                            println("Image clicked with id: $id")
                            navigateToTaggedUserProfile(id)
                        }
                    )

                    else -> OpenTabScreen(
                        onViewParticipants = { navigateToViewParticipantScreen(it) },
                        refreshKey = pagerState.currentPage
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableTabs(
    tabs: List<String>,
    pagerState: PagerState
) {
    val coroutineScope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

        },
        divider = {} // No divider
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = pagerState.currentPage == index
            Tab(
                selected = isSelected,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                text = {
                    Text(
                        text = title,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
            )
        }
    }
}





