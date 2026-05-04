package com.flipverse.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.FeedRepository
import com.flipverse.shared.domain.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

data class ExploreUiState(
    val followingUsers: Set<String> = emptySet(),
    val suggestedUsers: List<User> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

class ExploreViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("ExploreViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadFollowingUsers()
        loadSuggestedUsers()
        loadAllUsers() // Load all users on initialization
    }

    private fun loadFollowingUsers() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentUserId = com.flipverse.shared.PreferencesRepository.getEmail()
                val result = feedRepository.getUserFollowing(currentUserId)

                result.onSuccess { followingIds ->
                    _uiState.value = _uiState.value.copy(
                        followingUsers = followingIds.toSet(),
                        error = null
                    )
                    println("Successfully loaded ${followingIds.size} following IDs for current user")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        followingUsers = emptySet(),
                        error = error.message
                    )
                    println("Error loading current user following: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load following users"
                )
            }
        }
    }

    fun loadSuggestedUsers() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val currentUserId = com.flipverse.shared.PreferencesRepository.getEmail()

                // Load suggested users from the feed repository
                val criteria = com.flipverse.shared.domain.SuggestionCriteria(
                    minFollowers = 30,
                    maxFollowers = 50,
                    minPostCount = 10,
                    minQualityScore = 0.4,
                    minEngagementRate = 0.02,
                    categoryMatchWeight = 0.4,
                    mutualConnectionsWeight = 0.3,
                    activityWeight = 0.3
                )

                feedRepository.getSuggestedUsers(currentUserId, 20, criteria)
                    .onSuccess { users ->
                        _uiState.value = _uiState.value.copy(
                            suggestedUsers = users,
                            isLoading = false
                        )
                        println("✅ Loaded ${users.size} suggested users")
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load suggested users"
                        )
                        println("❌ Failed to load suggested users: ${error.message}")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load suggested users"
                )
                println("❌ Exception loading suggested users: ${e.message}")
            }
        }
    }

    fun loadAllUsers(limit: Int = 100, lastUserId: String? = null) {
        viewModelScope.launch(exceptionHandler) {
            try {
                if (lastUserId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
                }

                val currentUserId = com.flipverse.shared.PreferencesRepository.getEmail()

                feedRepository.getAllUsers(limit, lastUserId)
                    .onSuccess { rawUsers ->
                        // hasMorePages is based on raw doc count so pagination is never cut short
                        val filteredUsers = rawUsers.filter { it.id != currentUserId && !it.firstTimeLogin }

                        val currentUsers =
                            if (lastUserId == null) emptyList() else _uiState.value.allUsers
                        val updatedUsers = currentUsers + filteredUsers

                        _uiState.value = _uiState.value.copy(
                            allUsers = updatedUsers,
                            isLoading = false,
                            isLoadingMore = false,
                            hasMorePages = rawUsers.size == limit,
                            error = null
                        )
                        println("Successfully loaded ${rawUsers.size} raw docs (${filteredUsers.size} valid). Total: ${updatedUsers.size}")
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message ?: "Failed to load users"
                        )
                        println("Error loading all users: ${error.message}")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadMoreUsers(limit: Int = 100) {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMorePages || currentState.allUsers.isEmpty()) {
            return
        }

        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
                val lastCreatedAt = currentState.allUsers.lastOrNull()?.createdAt
                val currentUserId = com.flipverse.shared.PreferencesRepository.getEmail()

                feedRepository.getAllUsers(limit, lastCreatedAt)
                    .onSuccess { rawUsers ->
                        val filteredUsers = rawUsers.filter { it.id != currentUserId && !it.firstTimeLogin }
                        val updatedUsers = currentState.allUsers + filteredUsers

                        _uiState.value = _uiState.value.copy(
                            allUsers = updatedUsers,
                            isLoadingMore = false,
                            hasMorePages = rawUsers.size == limit,
                            error = null
                        )
                        println("Successfully loaded ${rawUsers.size} raw docs (${filteredUsers.size} valid). Total: ${updatedUsers.size}")
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            error = error.message ?: "Failed to load more users"
                        )
                        println("Error loading more users: ${error.message}")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun onFollowClick(userId: String) {
        val currentFollowing = _uiState.value.followingUsers
        val isNowFollowing = !currentFollowing.contains(userId)

        val newFollowing = if (currentFollowing.contains(userId)) {
            currentFollowing - userId
        } else {
            currentFollowing + userId
        }

        // Optimistically update local state
        _uiState.value = _uiState.value.copy(followingUsers = newFollowing)
        println("New Following status updated: $newFollowing")
        println("Following status updated: $isNowFollowing")

        // Update following status in Firebase and sync counts
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = feedRepository.updateFollowingStatus(
                    currentUserId = com.flipverse.shared.PreferencesRepository.getEmail(),
                    targetUserId = userId,
                    isFollowing = isNowFollowing
                )
                println("Following status updated: $result")
                if (result.isFailure) {
                    println("Failure here!!!!")
                    // Revert if backend fails
                    _uiState.value = _uiState.value.copy(
                        followingUsers = currentFollowing,
                        error = result.exceptionOrNull()?.message
                            ?: "Could not update following status"
                    )
                } else {
                    println("Success here!!!!")
                }
            } catch (e: Exception) {
                // Revert on error
                _uiState.value = _uiState.value.copy(
                    followingUsers = currentFollowing,
                    error = e.message ?: "Could not update following status"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
