package com.flipverse.dashboard.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flipverse.shared.BrandOrange
import com.flipverse.shared.domain.User

@Composable
fun PeopleToFollowSection(
    suggestedUsers: List<User>,
    followingUsers: Set<String>,
    onFollowClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onRemoveUser: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "People to Follow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandOrange,
                    modifier = Modifier.clickable(onClick = { onSeeAllClick() })
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(suggestedUsers) { user ->
                    println("followingUsers $followingUsers")
                    println("Users $user")
                    SuggestedUserCard(
                        user = user,
                        isFollowing = followingUsers.contains(user.email),
                        onFollowClick = { onFollowClick(user.email) },
                        onClose = { onRemoveUser(user.email) },
                        onThumbnailClick = { userId ->
                            println("Clicked on user: $userId")
                            onImageClick(userId)
                        }
                    )
                }
            }
        }
    }
}
