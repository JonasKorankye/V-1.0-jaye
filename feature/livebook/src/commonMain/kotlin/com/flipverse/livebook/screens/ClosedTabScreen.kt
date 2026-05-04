package com.flipverse.livebook.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flipverse.data.util.calculateLiveBookProgress
import com.flipverse.data.util.formatRelativeTime
import com.flipverse.data.util.parseAnyTimestampToEpochMillis
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.LightGrayBackground
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.LiveBook
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClosedTabScreen(
    onViewStory: (String) -> Unit,
    refreshKey: Any = Unit
) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(refreshKey) {
        try {
            viewModel.fetchArchivedLiveBooks()
            viewModel.fetchCompletedLiveBooks()
        } catch (e: Exception) {
            println("Error fetching books: ${e.message}")
        }
    }

    var searchQuery by remember { mutableStateOf("") }

    // Combine completed and archived books with null safety
    val allClosedBooks =
        remember(uiState.completedLiveBooks, uiState.archivedLiveBooks, searchQuery) {
            try {
                val completed = uiState.completedLiveBooks.takeIf { it.isNotEmpty() } ?: emptyList()
                val archived = uiState.archivedLiveBooks.takeIf { it.isNotEmpty() } ?: emptyList()
                val combined = completed + archived

                val filtered = if (searchQuery.isBlank()) {
                    combined
                } else {
                    combined.filter { book ->
                        book.title.contains(searchQuery, ignoreCase = true) ||
                                book.genre.contains(searchQuery, ignoreCase = true) ||
                                book.authorName.contains(searchQuery, ignoreCase = true)
                    }
                }

                filtered.sortedByDescending {
                    val dateStr = it.completedAt.takeIf { date -> date.isNotBlank() } ?: it.lastUpdatedAt
                    parseAnyTimestampToEpochMillis(dateStr)
                }
            } catch (e: Exception) {
                println("Error processing closed books: ${e.message}")
                emptyList()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoadingCompletedBooks || uiState.isLoadingArchivedBooks -> {
                LoadingState()
            }

            allClosedBooks.isEmpty() -> {
                EmptyState(hasSearchQuery = searchQuery.isNotBlank())
            }

            else -> {
                WriteUpList(
                    liveBooks = allClosedBooks,
                    onViewStory = { id-> onViewStory(id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange ,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onPrimary
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onPrimary),
        singleLine = true,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = searchQuery,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = Strings.search_flip_conversations_placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Search),
                        contentDescription = Strings.search,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    )

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
                text = Strings.loading_completed_stories,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyState(hasSearchQuery: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
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

            if (hasSearchQuery) {
                Text(
                    text = Strings.no_stories_found,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = Strings.try_searching_different_keywords,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = Strings.no_completed_stories,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = Strings.stories_completed_will_appear_here,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WriteUpList(
    liveBooks: List<LiveBook>,
    onViewStory: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(liveBooks) { liveBook ->
            WriteUpItem(
                liveBook = liveBook,
                onClick = { onViewStory(liveBook.id) }
            )
        }
    }
}

@Composable
private fun WriteUpItem(
    liveBook: LiveBook,
    onClick: () -> Unit
) {
    val progress = try {
        calculateLiveBookProgress(liveBook)
    } catch (e: Exception) {
        0
    }

    val completionDate = try {
        when {
            liveBook.completedAt.isNotEmpty() -> formatRelativeTime(liveBook.completedAt)
            liveBook.lastUpdatedAt.isNotEmpty() -> formatRelativeTime(liveBook.lastUpdatedAt)
            else -> Strings.unknown
        }
    } catch (e: Exception) {
        Strings.unknown
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    println("Error on click: ${e.message}")
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBackground),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(getGenreIcon(liveBook.genre)),
                contentDescription = liveBook.genre.takeIf { it.isNotBlank() } ?: Strings.unknown,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = liveBook.title.takeIf { it.isNotBlank() }
                    ?.split(" ")
                    ?.joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
                    ?: Strings.unknown,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$progress% completed",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = liveBook.genre.takeIf { it.isNotBlank() } ?: Strings.unknown,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (liveBook.status) {
                    "completed" -> "Completed $completionDate"
                    "archived" -> "Archived $completionDate"
                    else -> "Updated $completionDate"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

private fun getGenreIcon(genre: String) = when (genre.lowercase().trim()) {
    "romance" -> Resources.Icon.Romance
    "science fiction", "sci-fi" -> Resources.Icon.ScienceFiction
    "fantasy", "fantasy & supernatural" -> Resources.Icon.FantasySupernatural
    "thriller", "thriller & suspense" -> Resources.Icon.ThrillerSuspense
    "literary fiction" -> Resources.Icon.LiteraryFiction
    "historical fiction" -> Resources.Icon.HistoricalFiction
    "contemporary" -> Resources.Icon.Contemporary
    "young adult" -> Resources.Icon.YoungAdult
    "humor" -> Resources.Icon.Humor
    "creative non-fiction" -> Resources.Icon.CreativeNonFiction
    "folklore", "mythic fiction" -> Resources.Icon.FolkloreMythicFiction
    else -> Resources.Icon.FlipLiveBook // Default fallback
}
