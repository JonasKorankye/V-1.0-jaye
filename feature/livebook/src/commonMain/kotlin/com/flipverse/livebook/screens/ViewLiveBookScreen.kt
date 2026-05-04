package com.flipverse.livebook.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.formatTimestamp
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Gold
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.LiveBook
import com.flipverse.shared.domain.TaggedUser
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewLiveBookScreen(
    liveBookId: String,
    navigateToViewProfile: (String) -> Unit,
    navigateToContinueLiveBook: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // State for bottom sheet
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedParagraphNumber by remember { mutableStateOf<Int?>(null) }
    var selectedParticipant by remember { mutableStateOf<ParticipantData?>(null) }

    LaunchedEffect(liveBookId) {
        viewModel.fetchLiveBookById(liveBookId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(Strings.view_livebook_title, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back
                        )
                    }
                },
                actions = {
//                    IconButton(onClick = navigateToContinueLiveBook) {
//                        Icon(
//                            painter = painterResource(Resources.Icon.Next),
//                            modifier = Modifier.size(32.dp),
//                            tint = MaterialTheme.colorScheme.onPrimary,
//                            contentDescription = Strings.continue_story_cd
//                        )
//                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoadingCurrentBook -> {
                    LoadingState()
                }

                uiState.currentLiveBook == null -> {
                    ErrorState(
                        message = uiState.error ?: Strings.story_not_found,
                        onRetry = { viewModel.fetchLiveBookById(liveBookId) }
                    )
                }

                else -> {
                    val currentLiveBook = uiState.currentLiveBook!!

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Story Title
                        Text(
                            text = currentLiveBook.title.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = Strings.genre_prefix + currentLiveBook.genre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

//                        Spacer(modifier = Modifier.height(24.dp))
//
//                        Text(
//                            text = "Story Narration",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onPrimary
//                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (currentLiveBook.taggedUsers.isEmpty()) {
                            EmptyParticipantsState()
                        } else {
                            ParticipantsList(
                                author = ParticipantData(
                                    id = currentLiveBook.authorId,
                                    name = currentLiveBook.authorName,
                                    username = currentLiveBook.authorUsername,
                                    thumbnail = currentLiveBook.authorThumbnail,
                                    role = Strings.author_role,
                                    // authorId is stored as email — populate email so onImageClick
                                    // always receives an email regardless of participant type.
                                    email = currentLiveBook.authorId
                                ),
                                taggedUsers = currentLiveBook.taggedUsers,
                                currentLiveBook = currentLiveBook,
                                onLikeToggle = { participantId, isLiked ->
                                    viewModel.updateParagraphLikes(
                                        liveBookId = liveBookId,
                                        participantId = participantId,
                                        isLiked = isLiked
                                    )
                                },
                                onCommentClick = { participant, paragraphNumber ->
                                    selectedParticipant = participant
                                    selectedParagraphNumber = paragraphNumber
                                    showCommentSheet = true
                                },
                                onImageClick = { id -> navigateToViewProfile(id) },
                                navigateToContinueLiveBook = navigateToContinueLiveBook
                            )
                        }
                    }
                }
            }
        }

        // Comment Bottom Sheet
        if (showCommentSheet && selectedParticipant != null && selectedParagraphNumber != null) {
            CommentBottomSheet(
                participant = selectedParticipant!!,
                paragraphNumber = selectedParagraphNumber!!,
                liveBookId = liveBookId,
                onDismiss = {
                    showCommentSheet = false
                    selectedParticipant = null
                    selectedParagraphNumber = null
                }
            )
        }
    }
}

data class ParticipantData(
    val id: String,
    val name: String,
    val username: String,
    val thumbnail: String,
    val role: String,
    val email: String = ""
)

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdaptiveCircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = Strings.loading_completed_stories.replace("completed", "story details"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = vectorResource(Resources.Icon.Warning),
                contentDescription = Strings.error,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = Strings.retry,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptyParticipantsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = Strings.no_participants_tagged,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ParticipantsList(
    author: ParticipantData,
    taggedUsers: List<TaggedUser>,
    onImageClick: (String) -> Unit,
    currentLiveBook: LiveBook,
    onLikeToggle: (String, Boolean) -> Unit,
    onCommentClick: (ParticipantData, Int) -> Unit,
    navigateToContinueLiveBook: () -> Unit
) {
    // Helper function to check if a participant has content
    fun hasContent(participant: ParticipantData): Boolean {
        return when {
            participant.role == Strings.author_role -> currentLiveBook.initialParagraph.isNotEmpty()
            currentLiveBook.paragraph1ContributorId == participant.id -> currentLiveBook.paragraph1.isNotEmpty()
            currentLiveBook.paragraph2ContributorId == participant.id -> currentLiveBook.paragraph2.isNotEmpty()
            currentLiveBook.paragraph3ContributorId == participant.id -> currentLiveBook.paragraph3.isNotEmpty()
            currentLiveBook.paragraph4ContributorId == participant.id -> currentLiveBook.paragraph4.isNotEmpty()
            currentLiveBook.paragraph5ContributorId == participant.id -> currentLiveBook.paragraph5.isNotEmpty()
            currentLiveBook.paragraph6ContributorId == participant.id -> currentLiveBook.paragraph6.isNotEmpty()
            else -> false
        }
    }

    // Convert tagged users to participant data (excluding author)
    val contributors = taggedUsers.filter { it.id != author.id }.map { taggedUser ->
        ParticipantData(
            id = taggedUser.id,
            name = taggedUser.fullname.ifEmpty { Strings.unknown },
            username = taggedUser.username,
            thumbnail = taggedUser.thumbnail,
            role = Strings.contributor_role,
            email = taggedUser.email
        )
    }

    // Separate contributors with content and without content
    val contributorsWithContent = contributors.filter { hasContent(it) }
    val contributorsWithoutContent = contributors.filter { !hasContent(it) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show author first (always has initial paragraph)
        item {
            ParticipantItem(
                participant = author,
                currentLiveBook = currentLiveBook,
                onLikeToggle = onLikeToggle,
                onCommentClick = { paragraphNumber -> onCommentClick(author, paragraphNumber) },
                onImageClick = { userId ->
                    onImageClick(userId)
                },
                navigateToContinueLiveBook = navigateToContinueLiveBook
            )
        }

        // Show contributors with filled paragraphs
        items(contributorsWithContent) { participant ->
            ParticipantItem(
                participant = participant,
                currentLiveBook = currentLiveBook,
                onLikeToggle = onLikeToggle,
                onCommentClick = { paragraphNumber ->
                    onCommentClick(
                        participant,
                        paragraphNumber
                    )
                },
                onImageClick = { userId ->
                    onImageClick(userId)
                },
                navigateToContinueLiveBook = navigateToContinueLiveBook
            )
        }

        // Show contributors with empty paragraphs
        items(contributorsWithoutContent) { participant ->
            ParticipantItem(
                participant = participant,
                currentLiveBook = currentLiveBook,
                onLikeToggle = onLikeToggle,
                onCommentClick = { paragraphNumber ->
                    onCommentClick(
                        participant,
                        paragraphNumber
                    )
                },
                onImageClick = { userId ->
                    onImageClick(userId)
                },
                navigateToContinueLiveBook = navigateToContinueLiveBook
            )
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun ParticipantItem(
    participant: ParticipantData,
    onImageClick: (String) -> Unit,
    currentLiveBook: LiveBook,
    onLikeToggle: (String, Boolean) -> Unit,
    onCommentClick: (Int) -> Unit,
    navigateToContinueLiveBook: () -> Unit
) {
    // Helper function to get participant's paragraph likedBy list
    fun getParticipantLikedBy(): List<String> {
        return when {
            participant.role == Strings.author_role -> currentLiveBook.initialParagraphLikedBy
            currentLiveBook.paragraph1ContributorId == participant.id -> currentLiveBook.paragraph1LikedBy
            currentLiveBook.paragraph2ContributorId == participant.id -> currentLiveBook.paragraph2LikedBy
            currentLiveBook.paragraph3ContributorId == participant.id -> currentLiveBook.paragraph3LikedBy
            currentLiveBook.paragraph4ContributorId == participant.id -> currentLiveBook.paragraph4LikedBy
            currentLiveBook.paragraph5ContributorId == participant.id -> currentLiveBook.paragraph5LikedBy
            currentLiveBook.paragraph6ContributorId == participant.id -> currentLiveBook.paragraph6LikedBy
            else -> emptyList()
        }
    }

    // Helper function to get participant's paragraph comment count
    fun getParticipantCommentCount(): Int {
        return when {
            participant.role == Strings.author_role -> currentLiveBook.initialParagraphCommentsCount
            currentLiveBook.paragraph1ContributorId == participant.id -> currentLiveBook.paragraph1CommentsCount
            currentLiveBook.paragraph2ContributorId == participant.id -> currentLiveBook.paragraph2CommentsCount
            currentLiveBook.paragraph3ContributorId == participant.id -> currentLiveBook.paragraph3CommentsCount
            currentLiveBook.paragraph4ContributorId == participant.id -> currentLiveBook.paragraph4CommentsCount
            currentLiveBook.paragraph5ContributorId == participant.id -> currentLiveBook.paragraph5CommentsCount
            currentLiveBook.paragraph6ContributorId == participant.id -> currentLiveBook.paragraph6CommentsCount
            else -> 0
        }
    }

    // Get current user ID and email for matching
    val currentUserId = com.flipverse.shared.PreferencesRepository.getId()
    val currentUserEmail = com.flipverse.shared.PreferencesRepository.getEmail()

    // Check if current user has liked this paragraph
    val likedBy = getParticipantLikedBy()
    var isFavorite by remember { mutableStateOf(currentUserId in likedBy) }

    // Update isFavorite when likedBy changes (e.g., after refresh)
    LaunchedEffect(likedBy) {
        isFavorite = currentUserId in likedBy
    }

    // Get comment count
    val commentCount = getParticipantCommentCount()

    // Helper function to get participant's paragraph
    fun getParticipantParagraph(): String {
        return when {
            participant.role == Strings.author_role -> currentLiveBook.initialParagraph
            currentLiveBook.paragraph1ContributorId == participant.id -> currentLiveBook.paragraph1
            currentLiveBook.paragraph2ContributorId == participant.id -> currentLiveBook.paragraph2
            currentLiveBook.paragraph3ContributorId == participant.id -> currentLiveBook.paragraph3
            currentLiveBook.paragraph4ContributorId == participant.id -> currentLiveBook.paragraph4
            currentLiveBook.paragraph5ContributorId == participant.id -> currentLiveBook.paragraph5
            currentLiveBook.paragraph6ContributorId == participant.id -> currentLiveBook.paragraph6
            else -> ""
        }
    }

    val participantParagraph = getParticipantParagraph()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (participant.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(participant.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = participant.name,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onImageClick(participant.email.ifEmpty { participant.id }) }
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .clickable { onImageClick(participant.email.ifEmpty { participant.id }) }
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = Alpha.HALF
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = participant.name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LexendMediumFont()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = participant.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Text(
                            text = Strings.bullet,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = participant.role,
                            fontSize = 10.sp,
                            color = if (participant.role == Strings.author_role) {
                                Gold
                            } else {
                                Color.Gray
                            },
                            fontWeight = if (participant.role == Strings.author_role) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            }
                        )
//                        if (participant.username.isNotEmpty()) {
//                            Text(
//                                text = "•",
//                                fontSize = 14.sp,
//                                color = Color.Gray
//                            )
//                            Text(
//                                text = participant.username,
//                                fontSize = 12.sp,
//                                color = Color.Gray
//                            )
//                        }
                    }
                }

                // Like and Comment buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Comment Button with count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                when {
                                    participant.role == Strings.author_role -> onCommentClick(0)
                                    currentLiveBook.paragraph1ContributorId == participant.id -> onCommentClick(
                                        1
                                    )

                                    currentLiveBook.paragraph2ContributorId == participant.id -> onCommentClick(
                                        2
                                    )

                                    currentLiveBook.paragraph3ContributorId == participant.id -> onCommentClick(
                                        3
                                    )

                                    currentLiveBook.paragraph4ContributorId == participant.id -> onCommentClick(
                                        4
                                    )

                                    currentLiveBook.paragraph5ContributorId == participant.id -> onCommentClick(
                                        5
                                    )

                                    currentLiveBook.paragraph6ContributorId == participant.id -> onCommentClick(
                                        6
                                    )

                                    else -> {}
                                }
                            }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.Comment),
                            contentDescription = Strings.comments_cd,
                            tint = if (commentCount > 0) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        if (commentCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = commentCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Like Button
                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                            onLikeToggle(participant.id, isFavorite)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = vectorResource(
                                if (isFavorite) Resources.Icon.LikeSelected else Resources.Icon.Like
                            ),
                            contentDescription = if (isFavorite) Strings.remove_from_likes else Strings.add_to_likes,
                            tint = if (isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

            }

            if (participantParagraph.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = participantParagraph,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 20.sp
                    )
                }
            } else if (participant.role == Strings.contributor_role) {
                Spacer(modifier = Modifier.height(12.dp))

                // Check if this participant is the current user (match by ID or email)
                val isCurrentUser = (participant.id.isNotEmpty() && participant.id == currentUserId) ||
                        (participant.email.isNotEmpty() && participant.email == currentUserEmail)

                if (isCurrentUser) {
                    // Show clickable row with icon for current user
                    Button(
                        onClick = { navigateToContinueLiveBook() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Continue",
                                color = BlackLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.width(8.dp))

//                            Icon(
//                                painter = painterResource(Resources.Icon.Next),
//                                contentDescription = Strings.continue_story_cd,
//                                modifier = Modifier.size(20.dp)
//                            )
                        }
                    }
                } else {
                    // Show only text for other users (not clickable)
                    Text(
                        text = Strings.no_contribution_yet,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    participant: ParticipantData,
    paragraphNumber: Int,
    liveBookId: String,
    onDismiss: () -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load comments when sheet opens
    LaunchedEffect(liveBookId, paragraphNumber) {
        viewModel.loadParagraphComments(liveBookId, paragraphNumber)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Strings.comments_cd,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Close),
                        contentDescription = Strings.close,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = "${participant.name}'s paragraph",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Comments List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isLoadingComments) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AdaptiveCircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else if (uiState.paragraphComments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Be the first to comment!",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(uiState.paragraphComments) { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = {
                        Text(
                            text = "Add a comment...",
                            color = Color.Gray
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !uiState.isAddingComment
                )

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            viewModel.addParagraphComment(
                                liveBookId = liveBookId,
                                paragraphNumber = paragraphNumber,
                                commentText = newCommentText.trim(),
                                onSuccess = {
                                    newCommentText = ""
                                },
                                onError = { error ->
                                    println("Error adding comment: $error")
                                }
                            )
                        }
                    },
                    enabled = newCommentText.isNotBlank() && !uiState.isAddingComment,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (newCommentText.isNotBlank() && !uiState.isAddingComment)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Gray.copy(alpha = 0.3f)
                        )
                ) {
                    if (uiState.isAddingComment) {
                        AdaptiveCircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.SendMessageFlat),
                            contentDescription = Strings.send,
                            tint = if (newCommentText.isNotBlank())
                                MaterialTheme.colorScheme.onPrimary
                            else
                                Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: com.flipverse.shared.domain.ParagraphComment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User Avatar
        if (comment.userThumbnail.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(comment.userThumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = comment.userName,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userName.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Comment Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.userName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    text = Strings.bullet,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Text(
                    text = if (comment.timestamp.isNotEmpty()) {
                        formatTimestamp(comment.timestamp)
                    } else {
                        "Just now"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = comment.commentText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                lineHeight = 20.sp
            )
        }
    }
}