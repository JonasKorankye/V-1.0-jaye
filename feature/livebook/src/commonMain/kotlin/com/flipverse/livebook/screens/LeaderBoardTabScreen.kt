package com.flipverse.livebook.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.normalizeUsername
import com.flipverse.livebook.LeaderboardMetrics
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.livebook.UserStats
import com.flipverse.shared.Alpha
import com.flipverse.shared.Black
import com.flipverse.shared.Gold
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.round

@Composable
fun LeaderBoardTabScreen(refreshKey: Any = Unit, onImageClick: (String) -> Unit) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(refreshKey) {
        viewModel.fetchLeaderboardAnalytics()
    }

    when {
        uiState.isLoadingLeaderboard -> {
            LoadingState()
        }

        uiState.leaderboardMetrics.overallLeaders.isEmpty() -> {
            EmptyState()
        }

        else -> {
            LeaderboardContent(
                metrics = uiState.leaderboardMetrics,
                onImageClick = { id ->
                    viewModel.getUserEmailById(
                        userId = id,
                        onResult = { email -> onImageClick(email.toString()) }
                    )
                }
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdaptiveCircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = Strings.calculating_leaderboard,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painterResource(Resources.Icon.TrophyBadge),
                contentDescription = Strings.no_rankings,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = Strings.no_rankings_available,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Strings.start_participating_livebooks,
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LeaderboardContent(
    metrics: LeaderboardMetrics,
    onImageClick: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(0) }
    var showTooltip by remember { mutableStateOf(false) }

    val categories = listOf(
        Triple("Overall", metrics.overallLeaders, Resources.Icon.TrophyBadge),
        Triple("Story Master", metrics.topAuthors, Resources.Icon.StoryMaster),
        Triple("Indie Author", metrics.topContributors, Resources.Icon.IndieAuthor),
        Triple("Crowd Favourite", metrics.mostLiked, Resources.Icon.CrowdFavorite),
        Triple("Fast Fingers", metrics.fastestWriters, Resources.Icon.FastFingers),
        Triple("Genre Jumper", metrics.genreJumpers, Resources.Icon.GenreJumper),
        Triple("Pen Slinger", metrics.penSlingers, Resources.Icon.PenSlinger)
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Category Filter Chips with Tooltip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(categories[selectedCategory].third),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = categories[selectedCategory].first,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = { showTooltip = !showTooltip },
                modifier = Modifier.size(32.dp)
            ) {
                Image(
                    painter = painterResource(Resources.Icon.ToolTip),
                    contentDescription = Strings.info,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Tooltip content
        if (showTooltip) {
            CategoryTooltip(
                category = categories[selectedCategory].first,
                onDismiss = { showTooltip = false }
            )
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.size) { index ->
                val (title, _, iconResource) = categories[index]
                FilterChip(
                    selected = selectedCategory == index,
                    onClick = {
                        selectedCategory = index
                        showTooltip = false // Hide tooltip when switching categories
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = painterResource(iconResource),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title,
                                fontWeight = if (selectedCategory == index) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (selectedCategory == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                }
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        selectedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leaderboard List
        val currentList = categories[selectedCategory].second
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(currentList) { userStats ->
                LeaderboardItem(
                    userStats = userStats,
                    category = categories[selectedCategory].first,
                    onImageClick = { id -> onImageClick(id) }
                )
            }
        }
    }
}

@Composable
private fun LeaderboardItem(
    userStats: UserStats,
    onImageClick: (String) -> Unit,
    category: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (userStats.rank) {
                1 -> Gold.copy(alpha = 0.1f)
                2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f) // Silver
                3 -> Color(0xFFCD7F32).copy(alpha = 0.1f) // Bronze
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        when (userStats.rank) {
                            1 -> Gold
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Color.Gray
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userStats.rank.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Avatar
            if (userStats.thumbnail.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(userStats.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = userStats.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onImageClick(userStats.userId) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onImageClick(userStats.userId) }
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                        )
                        .border(2.dp, Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userStats.name.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userStats.name,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = normalizeUsername(userStats.username),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Category-specific stats
                Text(
                    text = getCategorySpecificStats(userStats, category),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trophy/Badge Icon
            Icon(
                imageVector = when (userStats.rank) {
                    1 -> vectorResource(Resources.Icon.TrophyBadge)
                    2, 3 -> vectorResource(Resources.Icon.RibbonBadge)
                    else -> vectorResource(Resources.Icon.RibbonBadge)
                },
                contentDescription = "Rank ${userStats.rank}",
                tint = when (userStats.rank) {
                    1 -> Gold
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Gray
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CategoryTooltip(
    category: String,
    onDismiss: () -> Unit
) {
    val explanation = getCategoryExplanation(category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(Resources.Icon.ToolTip),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Black.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Close),
                    contentDescription = Strings.close,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getCategoryExplanation(category: String): String {
    return when (category.lowercase()) {
        "overall" -> "Top contributors based on overall engagement score, combining stories authored, contributions, likes, and completion rate."
        "story master" -> "Users who have authored the most original LiveBook stories and completed them successfully."
        "indie author" -> "Active contributors who participate in many stories by adding meaningful paragraphs to collaborative narratives."
        "crowd favourite" -> "Most popular contributors whose paragraphs and stories receive the highest number of likes from the community."
        "fast fingers" -> "Speedy writers who contribute quality content in the shortest amount of time."
        "genre jumper" -> "Versatile writers who explore and contribute to the widest variety of story genres."
        "pen slinger" -> "Dedicated storytellers who participate in and complete the most LiveBook challenges."
        else -> "Top performers in this category based on their contributions and engagement."
    }
}

private fun getCategorySpecificStats(userStats: UserStats, category: String): String {
    return when (category.lowercase()) {
        "overall" -> "Score: ${roundToTwoDecimals(userStats.engagementScore)} • ${userStats.storiesAuthored} stories • ${userStats.totalLikes} likes"
        "story master" -> "${userStats.storiesAuthored} stories authored • ${userStats.completedStories} completed"
        "indie author" -> "${userStats.totalContributions} contributions • ${userStats.storiesContributed} stories"
        "crowd favourite" -> "${userStats.totalLikes} total likes • ${userStats.totalContributions} contributions"
        "fast fingers" -> "Avg: ${formatWritingTime(userStats.averageWritingTimeMs)} • ${userStats.totalContributions} contributions"
        "genre jumper" -> "${userStats.uniqueGenres.size} genres • ${userStats.storiesAuthored + userStats.storiesContributed} participations"
        "pen slinger" -> "${userStats.completedLiveBooks} completed stories • ${userStats.totalContributions} contributions"
        else -> "${userStats.storiesAuthored} stories • ${userStats.totalLikes} likes"
    }
}

private fun roundToTwoDecimals(value: Double): String {
    val rounded = round(value * 100) / 100
    return rounded.toString()
}

private fun formatWritingTime(timeMs: Long): String {
    val minutes = timeMs / (1000 * 60)
    val seconds = (timeMs % (1000 * 60)) / 1000
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
