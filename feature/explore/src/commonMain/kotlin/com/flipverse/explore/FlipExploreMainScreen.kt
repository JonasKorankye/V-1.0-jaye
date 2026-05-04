package com.flipverse.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.WorkSansBoldFont
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipExploreScreen(
    navigateToProfile: () -> Unit,
    navigateToBookDetails: (String) -> Unit,
    navigateToCart: () -> Unit,
    navigateToViewProfile: (String) -> Unit = {},
    onNavigateToBookReader: (String) -> Unit = {}
) {
    val tabs = listOf("Discover", "Bookstore", "Library")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()


    Scaffold(
        modifier = Modifier.padding(6.dp),
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Strings.explore_title,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        Box(
//                            modifier = Modifier
//                                .padding(start = 2.dp),
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.primary, //TopAppBar background
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            height = 4.dp, // Optional: adjust height
                            color = MaterialTheme.colorScheme.onPrimary // Your desired color
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    when (index) {
                                        0 -> Strings.tab_discover
                                        1 -> Strings.tab_bookstore
                                        else -> Strings.tab_library
                                    },
                                    color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary // background for the entire screen
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    DiscoverScreen(
                        paddingValues = paddingValues,
                        navigateToViewProfile = navigateToViewProfile
                    )
                }

                1 -> {
                    BookStoreScreen(
                        paddingValues = paddingValues,
                        onNavigateToBookDetails = { id-> navigateToBookDetails(id) },
                        onNavigateToCart = { navigateToCart() }
                    )

                }

                2 -> {
                    LibraryScreen(
                        paddingValues = paddingValues,
                        onNavigateToBookDetails = { id -> navigateToBookDetails(id) },
                        onNavigateToBookReader = { id -> onNavigateToBookReader(id) }
                    )

                }

                else -> {
                    DiscoverScreen(
                        paddingValues = paddingValues,
                        navigateToViewProfile = navigateToViewProfile
                    )

                }
            }
        }
    }
}


@Composable
fun SuggestionItem(suggestion: Suggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = Alpha.DISABLED)) // Placeholder for profile image
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(suggestion.name, color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
                Text(suggestion.handle, color = Color.Gray, fontSize = 14.sp)
            }
        }
        Button(
            onClick = { /* Handle Follow */ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer), // Orange color
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(Strings.follow, color = BlackLight, fontSize = 14.sp)
        }
    }
}


data class Suggestion(val name: String, val handle: String)


fun getDummySuggestions(): List<Suggestion> {
    return listOf(
        Suggestion("LastCodeDaily", "@lastcodedaily"),
        Suggestion("System Design Daily", "@systemdesigndaily"),
        Suggestion("AWS", "@aws"),
        Suggestion("DevOps Engineering", "@devopsengineering"),
        Suggestion("OpenAI for Education", "@openalforeducation"),
        Suggestion("Steve Wozniak", "@stevewozniak"),
        Suggestion("AI Agents Simplified", "@aiagentssimplified"),
        Suggestion("Better Engineering", "@betterengineers"),
        Suggestion("Steve Wozniak", "@stevewozniak"),
        Suggestion("AI Agents Simplified", "@aiagentssimplified"),
        Suggestion("Better Engineering", "@betterengineers")
    )
}

