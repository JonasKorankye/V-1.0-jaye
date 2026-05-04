package com.flipverse.livebook.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flipverse.data.util.calculateLiveBookProgress
import com.flipverse.data.util.calculateTimeRemaining
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.livebook.component.FeatureBanner
import com.flipverse.shared.Alpha
import com.flipverse.shared.Ash
import com.flipverse.shared.LightGrayBackground
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.LiveBook
import com.flipverse.shared.domain.TaggedUser
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OpenTabScreen(
    onViewParticipants: (String) -> Unit,
    refreshKey: Any = Unit
) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(refreshKey) {
        viewModel.fetchActiveLiveBooks()
    }

    Column {
        Spacer(modifier = Modifier.height(24.dp))
        FeatureBanner()
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoadingLiveBooks -> {
                LoadingState()
            }

            uiState.activeLiveBooks.isEmpty() -> {
                EmptyState()
            }

            else -> {
                LiveBookList(
                    liveBooks = uiState.activeLiveBooks.sortedByDescending { it.createdAt },
                    onViewParticipants = { onViewParticipants(it) }
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
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
                text = Strings.loading_active_stories,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painterResource(Resources.Icon.FlipLiveBook),
                contentDescription = Strings.no_stories,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = Strings.no_active_stories,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Strings.see_stories_participating_here,
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LiveBookList(
    liveBooks: List<LiveBook>,
    onViewParticipants: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Padding to avoid FAB overlap
    ) {
        items(liveBooks) { liveBook ->
            LiveBookItem(
                liveBook = liveBook,
                onViewParticipants = { id -> onViewParticipants(id)}
            )
        }
    }
}

@Composable
fun LiveBookItem(
    liveBook: LiveBook,
    onViewParticipants: (String) -> Unit
) {
    val progress = calculateLiveBookProgress(liveBook)
    val timeRemaining = calculateTimeRemaining(liveBook.timestamp)
    val isExpired = timeRemaining == "Expired"

    Card(
        modifier = Modifier
            .then(
                if (!isExpired) {
                    Modifier.clickable { onViewParticipants(liveBook.id) }
                } else {
                    Modifier

                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isExpired) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = Alpha.DISABLED)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isExpired) Color.Gray.copy(alpha = 0.3f) else Ash
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        if (isExpired) {
                            LightGrayBackground.copy(alpha = 0.5f)
                        } else {
                            LightGrayBackground
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(getGenreIcon(liveBook.genre)),
                    contentDescription = liveBook.genre,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${liveBook.totalLikes}" + Strings.likes_label_suffix + " • $timeRemaining",
                    color = if (isExpired) Color.Red.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp
                )

                Text(
                    text = liveBook.title.uppercase(),
                    color = if (isExpired) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$progress%" + Strings.completed_suffix,
                        color = if (isExpired) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = Strings.bullet,
                        color = if (isExpired) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = liveBook.genre,
                        color = if (isExpired) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
//            Icon(
//                vectorResource(Resources.Icon.Like),
//                contentDescription = "Favorite",
//                tint = if (isExpired) Color.Gray.copy(alpha = 0.3f) else Color.Gray
//            )
        }
    }
}

private fun getGenreIcon(genre: String) = when (genre.lowercase().trim()) {
    "romance" -> Resources.Icon.Romance
    "science fiction", "sci-fi" -> Resources.Icon.ScienceFiction
    "fantasy", "fantasy / supernatural" -> Resources.Icon.FantasySupernatural
    "thriller", "thriller / suspense" -> Resources.Icon.ThrillerSuspense
    "literary fiction" -> Resources.Icon.LiteraryFiction
    "historical fiction" -> Resources.Icon.HistoricalFiction
    "contemporary" -> Resources.Icon.Contemporary
    "young adult" -> Resources.Icon.YoungAdult
    "humor" -> Resources.Icon.Humor
    "creative non-fiction" -> Resources.Icon.CreativeNonFiction
    "folklore", "folklore / mythic fiction" -> Resources.Icon.FolkloreMythicFiction
    else -> Resources.Icon.FlipLiveBook // Default fallback
}
