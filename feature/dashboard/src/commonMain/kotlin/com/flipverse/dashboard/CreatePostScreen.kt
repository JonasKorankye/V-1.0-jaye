package com.flipverse.dashboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flipverse.shared.BlackLight
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.Resources
import com.flipverse.shared.domain.CreatePostRequest
import com.flipverse.shared.domain.CreatePostState
import com.flipverse.shared.domain.FeedPopulationStatus
import com.flipverse.shared.domain.PostType
import com.flipverse.shared.domain.PrivacyLevel
import com.flipverse.shared.Strings
import org.jetbrains.compose.resources.vectorResource
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostCreated: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {

    val viewModel = koinViewModel<DashboardViewModel>()
    val uiState by viewModel.uiCreatePostState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var selectedPostType by remember { mutableStateOf(PostType.RECOMMENDATION) }
    var selectedPrivacy by remember { mutableStateOf(PrivacyLevel.PUBLIC) }

    // Handle post creation success
    LaunchedEffect(uiState.createdPost) {
        if (uiState.createdPost != null && !uiState.isCreating) {
            onPostCreated()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(Strings.create_post_title) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(vectorResource(Resources.Icon.Close), contentDescription = Strings.close)
                }
            },
            actions = {
                // Post button with elegant theme styling
                val isDarkTheme = isSystemInDarkTheme()
                val buttonTextColor = if (isDarkTheme) BlackLight else MaterialTheme.colorScheme.onPrimary
                
                Button(
                    onClick = {
                        val request = CreatePostRequest(
                            whatsNew = title,
                            source = content,
                            postType = selectedPostType,
                            category = category,
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            privacy = selectedPrivacy
                        )
                        viewModel.createPost(
                            request, {  },
                            onError = {  }
                        )
                    },
                    enabled = content.isNotBlank() && !uiState.isCreating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = buttonTextColor,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        disabledContentColor = buttonTextColor.copy(alpha = 0.5f)
                    )
                ) {
                    if (uiState.isCreating) {
                        AdaptiveCircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = buttonTextColor
                        )
                    } else {
                        Text(Strings.post_button)
                    }
                }
            }
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(Strings.title_optional) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isCreating
            )

            // Content Input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(Strings.whats_on_your_mind) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                minLines = 4,
                enabled = !uiState.isCreating
            )

            // Post Type Selection
            PostTypeSelector(
                selectedType = selectedPostType,
                onTypeSelected = { selectedPostType = it },
                enabled = !uiState.isCreating
            )

            // Category Input
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(Strings.category_optional) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isCreating
            )

            // Tags Input
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(Strings.tags_comma_separated) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(Strings.sample_tags_placeholder) },
                enabled = !uiState.isCreating
            )

            // Privacy Selection
            PrivacySelector(
                selectedPrivacy = selectedPrivacy,
                onPrivacySelected = { selectedPrivacy = it },
                enabled = !uiState.isCreating
            )

            // Media Upload Section
            MediaUploadSection(
                uiState = uiState,
                enabled = !uiState.isCreating
            )

            // Upload Progress
            if (uiState.isUploadingMedia) {
                LinearProgressIndicator(
                    progress = { uiState.uploadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = Strings.uploading_media,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Feed Population Status
            FeedPopulationStatusIndicator(status = uiState.feedPopulationStatus)

            // Error Display
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PostTypeSelector(
    selectedType: PostType,
    onTypeSelected: (PostType) -> Unit,
    enabled: Boolean
) {
    Column {
        Text(
            text = "Post Type",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PostType.values().take(4).forEach { type ->
                FilterChip(
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    selected = selectedType == type,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun PrivacySelector(
    selectedPrivacy: PrivacyLevel,
    onPrivacySelected: (PrivacyLevel) -> Unit,
    enabled: Boolean
) {
    Column {
        Text(
            text = "Privacy",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrivacyLevel.values().forEach { privacy ->
                FilterChip(
                    onClick = { onPrivacySelected(privacy) },
                    label = { Text(privacy.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    selected = selectedPrivacy == privacy,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun MediaUploadSection(
    uiState: CreatePostState,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = Strings.add_media,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Handle image selection */ },
                    enabled = enabled
                ) {
                    Icon(
                        vectorResource(Resources.Icon.Recommendation),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.photos)
                }

                OutlinedButton(
                    onClick = { /* Handle video selection */ },
                    enabled = enabled
                ) {
                    Icon(
                        vectorResource(Resources.Icon.Review),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.video)
                }
            }
        }
    }
}

@Composable
private fun FeedPopulationStatusIndicator(status: FeedPopulationStatus) {
    when (status) {
        FeedPopulationStatus.NotStarted -> { /* No indicator */
        }

        FeedPopulationStatus.InProgress -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdaptiveCircularProgressIndicator(modifier = Modifier.size(16.dp))
                Text(
                    text = Strings.notifying_followers,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FeedPopulationStatus.Completed -> {
            Text(
                text = Strings.post_shared_to_followers,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        FeedPopulationStatus.Failed -> {
            Text(
                text = Strings.post_created_but_failed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}