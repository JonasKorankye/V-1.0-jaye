package com.flipverse.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Constants
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.saveSuggestedUsers
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.domain.User
import com.flipverse.shared.util.createShareManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSeeAllScreen(
    onNavigateBack: () -> Unit,
    navigateToViewProfile: (String) -> Unit = {}
) {
    val shareManager = remember { createShareManager() }
    val uriHandler = LocalUriHandler.current
    val viewModel = koinViewModel<DashboardViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isLoadingSuggestions by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.people_to_follow,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            // Invite friends Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary) // Card-like background
                    .padding(12.dp)
                    .clickable { uriHandler.openUri(Constants.INVITE_LANDING_URL) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF74C0FC)
                            ) //  color for the circle
                            .border(2.dp, Color.Transparent, CircleShape), //  border color
                    ) {
                        Icon(
                            modifier = Modifier.padding(10.dp),
                            imageVector = vectorResource(Resources.Icon.Invite), // Replace with appropriate icon if available
                            contentDescription = "Invite Friends Icon",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        Strings.invite_friends,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp
                    )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

//                    Text(
//                        text = Constants.INVITE_LANDING_URL,
//                        color = MaterialTheme.colorScheme.primaryContainer,
//                        fontSize = 12.sp,
//                        textDecoration = TextDecoration.Underline,
//                        modifier = Modifier.clickable {
//                            uriHandler.openUri(Constants.INVITE_LANDING_URL)
//                        }
//                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        shareManager.shareInviteLink(
                            inviteLink = Constants.INVITE_LANDING_URL,
                            title = Strings.join_me_invite_title
                        )
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        vectorResource(Resources.Icon.Arrow),
                        Strings.chevron_right_cd,
                        tint = BlackLight.copy(alpha = Alpha.HALF),
                        modifier = Modifier.size(16.dp)
                    )
                }

            }

            HorizontalDivider(
                modifier = Modifier.padding(4.dp),
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = Alpha.DISABLED)
            )


            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.suggested_people,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color.Gray
                )

                FilledTonalIconButton(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = {
                        viewModel.pageSize = 30
                        scope.launch {
                            isLoadingSuggestions = true
                            try {
                                viewModel.loadSuggestedUsers(getEmail())
                                    .collectLatest { suggestedUsersData ->
                                        println("Suggested Users Data: $suggestedUsersData")
                                        if (suggestedUsersData.isNotEmpty()) {
                                            saveSuggestedUsers(
                                                suggestedUsersData
                                            )
                                        }
                                        isLoadingSuggestions = false
                                    }
                            } catch (e: Exception) {
                                isLoadingSuggestions = false
                            }
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent
                    ),
                    enabled = !isLoadingSuggestions
                ) {
                    if (isLoadingSuggestions) {
                        AdaptiveCircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(Alpha.HALF)
                        )
                    } else {
                        Icon(
                            vectorResource(Resources.Icon.Refresh),
                            Strings.refresh_suggested_people,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(Alpha.HALF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            // Suggestions List
            LazyColumn(
                userScrollEnabled = true
            ) {
                items(PreferencesRepository.loadSuggestedUsers()) { suggestion ->
                    SuggestionItem(
                        suggestion = suggestion,
                        onFollowClick = { userId ->
                            viewModel.onFollowClick(userId)
                        },
                        isFollowing = uiState.followingUsers.contains(suggestion.email),
                        onProfileClick = { userId ->
                            navigateToViewProfile(userId)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }

    }

}


@Composable
fun SuggestionItem(
    suggestion: User,
    isFollowing: Boolean,
    onFollowClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (suggestion.thumbnail.isNotEmpty()) {
                AsyncImage(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(suggestion.email) },
                    model = ImageRequest.Builder(
                        LocalPlatformContext.current
                    ).data(suggestion.thumbnail)
                        .crossfade(enable = true)
                        .build(),
                    contentDescription = "Author Profile URL",
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(suggestion.email) }
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                        .border(2.dp, Color.Transparent, CircleShape), //  border color
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = suggestion.fullname.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )

                }
            }

            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    suggestion.fullname,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
                Text(normalizeUsername(suggestion.username), color = Color.Gray, fontSize = 14.sp)
            }
        }
        Button(
            onClick = { onFollowClick(suggestion.email) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer), // Orange color
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (isFollowing) Strings.following else Strings.follow,
                fontSize = 12.sp,
                color = if (isFollowing)
                    CoffeeDark
                else
                    BlackLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class Suggestion(val name: String, val handle: String)
