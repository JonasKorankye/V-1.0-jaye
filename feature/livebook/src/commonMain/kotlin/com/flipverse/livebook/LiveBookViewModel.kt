package com.flipverse.livebook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.FeedRepository
import com.flipverse.data.domain.LiveBookRepository
import com.flipverse.data.domain.NomenclatureRepository
import com.flipverse.data.util.vetStoryContinuation
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getId
import com.flipverse.shared.domain.User
import com.flipverse.shared.domain.LiveBook
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LiveBookState(
    val allUsers: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val selectedUsers: List<User> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String = "",
    val challengeTitle: String = "",
    val initialParagraph: String = "",
    val isLoading: Boolean = false,
    val isLoadingGenres: Boolean = false,
    val isPublishing: Boolean = false,
    val hasMorePages: Boolean = true,
    val activeLiveBooks: List<LiveBook> = emptyList(),
    val isLoadingLiveBooks: Boolean = false,
    val completedLiveBooks: List<LiveBook> = emptyList(),
    val isLoadingCompletedBooks: Boolean = false,
    val archivedLiveBooks: List<LiveBook> = emptyList(),
    val isLoadingArchivedBooks: Boolean = false,
    val currentLiveBook: LiveBook? = null,
    val isLoadingCurrentBook: Boolean = false,
    val isLiked: Boolean = false,
    val userParticipation: com.flipverse.data.domain.UserParticipationStatus? = null,
    val isLoadingParticipation: Boolean = false,
    val isSubmittingContribution: Boolean = false,
    val leaderboardMetrics: LeaderboardMetrics = LeaderboardMetrics(),
    val isLoadingLeaderboard: Boolean = false,
    val previousLiveBook: LiveBook? = null,
    val canRevertToPrevious: Boolean = false,
    val userLiveBooks: List<LiveBook> = emptyList(),
    val isLoadingUserLiveBooks: Boolean = false,
    val paragraphComments: List<com.flipverse.shared.domain.ParagraphComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isAddingComment: Boolean = false,
    val error: String? = null
)

data class UserStats(
    val userId: String,
    val name: String,
    val username: String,
    val thumbnail: String,
    val storiesAuthored: Int = 0,
    val storiesContributed: Int = 0,
    val totalContributions: Int = 0,
    val totalLikes: Int = 0,
    val totalWritingTimeMs: Long = 0,
    val completedStories: Int = 0,
    val averageWritingTimeMs: Long = 0,
    val engagementScore: Double = 0.0,
    val uniqueGenres: Set<String> = emptySet(),
    val completedLiveBooks: Int = 0,
    val rank: Int = 0
)

data class LeaderboardMetrics(
    val topAuthors: List<UserStats> = emptyList(),
    val topContributors: List<UserStats> = emptyList(),
    val fastestWriters: List<UserStats> = emptyList(),
    val mostLiked: List<UserStats> = emptyList(),
    val mostEngaged: List<UserStats> = emptyList(),
    val overallLeaders: List<UserStats> = emptyList(),
    val genreJumpers: List<UserStats> = emptyList(),
    val penSlingers: List<UserStats> = emptyList()
)

class LiveBookViewModel(
    private val feedRepository: FeedRepository,
    private val liveBookRepository: LiveBookRepository,
    private val nomenclatureRepository: NomenclatureRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("LiveBookViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _uiState = MutableStateFlow(LiveBookState())
    val uiState: StateFlow<LiveBookState> = _uiState.asStateFlow()

    private val eventChannel = Channel<LiveBookEvent>()
    val event = eventChannel.receiveAsFlow()

    // Flag to prevent concurrent analytics calls
    private var isAnalyticsRunning = false

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadAllUsers()
        loadGenres()
        fetchActiveLiveBooks()
        fetchCompletedLiveBooks()
        fetchArchivedLiveBooks()
    }

    fun fetchLeaderboardAnalytics() {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                if (isAnalyticsRunning) {
                    return@launch
                }
                isAnalyticsRunning = true

                _uiState.value = _uiState.value.copy(isLoadingLeaderboard = true, error = null)

                // Fetch smaller datasets to prevent memory issues
                val liveBooksResult = liveBookRepository.fetchAllLiveBooksForAnalytics()
                val usersResult = feedRepository.getAllUsers(limit = 200) // Reduced from 1000

                if (liveBooksResult.isSuccess() && usersResult.isSuccess) {
                    val allLiveBooks = liveBooksResult.getSuccessData()
                    val allUsers = usersResult.getOrNull() ?: emptyList()

                    // Early exit if no data to prevent crashes
                    if (allLiveBooks.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            leaderboardMetrics = LeaderboardMetrics(),
                            isLoadingLeaderboard = false,
                            error = null
                        )
                        isAnalyticsRunning = false
                        return@launch
                    }

                    // Process data in smaller batches to prevent memory issues
                    val userStatsMap = mutableMapOf<String, UserStats>()

                    // Initialize stats only for users who actually participated
                    val participatingUserIds = mutableSetOf<String>()
                    allLiveBooks.forEach { liveBook ->
                        if (liveBook.authorId.isNotBlank()) {
                            participatingUserIds.add(liveBook.authorId)
                        }
                        listOf(
                            liveBook.paragraph1ContributorId,
                            liveBook.paragraph2ContributorId,
                            liveBook.paragraph3ContributorId,
                            liveBook.paragraph4ContributorId,
                            liveBook.paragraph5ContributorId,
                            liveBook.paragraph6ContributorId
                        ).forEach { contributorId ->
                            if (contributorId.isNotBlank()) {
                                participatingUserIds.add(contributorId)
                            }
                        }
                    }

                    // Only initialize stats for participating users
                    allUsers.forEach { user ->
                        if (participatingUserIds.contains(user.id)) {
                            userStatsMap[user.id] = UserStats(
                                userId = user.id,
                                name = user.fullname.takeIf { it.isNotBlank() } ?: com.flipverse.shared.Strings.unknown,
                                username = user.username.takeIf { it.isNotBlank() } ?: com.flipverse.shared.Strings.unknown_lower,
                                thumbnail = user.thumbnail
                            )
                        }
                    }

                    // Process LiveBooks with null safety
                    allLiveBooks.forEach { liveBook ->
                        try {
                            // Track author statistics with null safety
                            if (liveBook.authorId.isNotBlank()) {
                                val authorStats = userStatsMap[liveBook.authorId]
                                if (authorStats != null) {
                                    userStatsMap[liveBook.authorId] = authorStats.copy(
                                        storiesAuthored = authorStats.storiesAuthored + 1,
                                        totalContributions = authorStats.totalContributions + 1, // Count initial paragraph as contribution
                                        totalWritingTimeMs = authorStats.totalWritingTimeMs + maxOf(
                                            0L,
                                            liveBook.initialParagraphWritingTime
                                        ),
                                        completedStories = if (liveBook.status == "completed") authorStats.completedStories + 1 else authorStats.completedStories,
                                        uniqueGenres = authorStats.uniqueGenres + liveBook.genre
                                    )
                                }
                            }

                            // Track contributor statistics with better null safety
                            val contributorData = listOf(
                                Triple(
                                    liveBook.paragraph1ContributorId,
                                    liveBook.paragraph1WritingTime,
                                    liveBook.paragraph1Likes
                                ),
                                Triple(
                                    liveBook.paragraph2ContributorId,
                                    liveBook.paragraph2WritingTime,
                                    liveBook.paragraph2Likes
                                ),
                                Triple(
                                    liveBook.paragraph3ContributorId,
                                    liveBook.paragraph3WritingTime,
                                    liveBook.paragraph3Likes
                                ),
                                Triple(
                                    liveBook.paragraph4ContributorId,
                                    liveBook.paragraph4WritingTime,
                                    liveBook.paragraph4Likes
                                ),
                                Triple(
                                    liveBook.paragraph5ContributorId,
                                    liveBook.paragraph5WritingTime,
                                    liveBook.paragraph5Likes
                                ),
                                Triple(
                                    liveBook.paragraph6ContributorId,
                                    liveBook.paragraph6WritingTime,
                                    liveBook.paragraph6Likes
                                )
                            )

                            contributorData.forEach { (contributorId, writingTime, likes) ->
                                if (contributorId.isNotBlank()) {
                                    val contributorStats = userStatsMap[contributorId]
                                    if (contributorStats != null) {
                                        userStatsMap[contributorId] = contributorStats.copy(
                                            storiesContributed = contributorStats.storiesContributed + 1,
                                            totalContributions = contributorStats.totalContributions + 1,
                                            totalWritingTimeMs = contributorStats.totalWritingTimeMs + maxOf(
                                                0L,
                                                writingTime
                                            ),
                                            totalLikes = contributorStats.totalLikes + maxOf(
                                                0,
                                                likes
                                            ),
                                            completedStories = if (liveBook.status == "completed") contributorStats.completedStories + 1 else contributorStats.completedStories,
                                            uniqueGenres = contributorStats.uniqueGenres + liveBook.genre
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("Error processing LiveBook ${liveBook.id}: ${e.message}")
                            // Continue processing other books
                        }
                    }

                    // Calculate derived metrics with null safety
                    val finalUserStats = userStatsMap.values.mapNotNull { stats ->
                        try {
                            val avgWritingTime = if (stats.totalContributions > 0) {
                                stats.totalWritingTimeMs / stats.totalContributions
                            } else 0L

                            // Prevent division by zero and handle edge cases
                            val engagementScore = try {
                                calculateEngagementScore(
                                    storiesAuthored = stats.storiesAuthored,
                                    storiesContributed = stats.storiesContributed,
                                    totalLikes = stats.totalLikes,
                                    completedStories = stats.completedStories,
                                    avgWritingTime = avgWritingTime,
                                    uniqueGenres = stats.uniqueGenres
                                )
                            } catch (e: Exception) {
                                0.0 // Default score if calculation fails
                            }

                            stats.copy(
                                averageWritingTimeMs = avgWritingTime,
                                engagementScore = engagementScore.takeIf { it.isFinite() } ?: 0.0,
                                completedLiveBooks = stats.completedStories
                            )
                        } catch (e: Exception) {
                            println("Error calculating stats for user ${stats.userId}: ${e.message}")
                            null // Skip this user if calculation fails
                        }
                    }.filter {
                        // Only include users who have actually participated
                        it.storiesAuthored > 0 || it.storiesContributed > 0 
                    }

                    // Create leaderboard rankings with reduced sizes
                    val leaderboardMetrics = LeaderboardMetrics(
                        topAuthors = finalUserStats.sortedByDescending { it.storiesAuthored }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        
                        topContributors = finalUserStats.sortedByDescending { it.totalContributions }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        
                        fastestWriters = finalUserStats.filter { it.averageWritingTimeMs > 0 }
                            .sortedBy { it.averageWritingTimeMs }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        
                        mostLiked = finalUserStats.sortedByDescending { it.totalLikes }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        
                        mostEngaged = finalUserStats.sortedByDescending { it.engagementScore }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        
                        overallLeaders = finalUserStats.sortedByDescending { it.engagementScore }
                            .take(10).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        genreJumpers = finalUserStats.sortedByDescending { it.uniqueGenres.size }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) },
                        penSlingers = finalUserStats.sortedByDescending { it.storiesAuthored + it.storiesContributed }
                            .take(5).mapIndexed { index, stats -> stats.copy(rank = index + 1) }
                    )

                    _uiState.value = _uiState.value.copy(
                        leaderboardMetrics = leaderboardMetrics,
                        isLoadingLeaderboard = false,
                        error = null
                    )

                    println("Leaderboard analytics calculated successfully with ${finalUserStats.size} participating users")
                    isAnalyticsRunning = false

                } else {
                    val errorMsg = when {
                        !liveBooksResult.isSuccess() -> liveBooksResult.getErrorMessage()
                        !usersResult.isSuccess -> usersResult.exceptionOrNull()?.message
                            ?: "Failed to fetch users"
                        else -> "Failed to fetch data for leaderboard"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingLeaderboard = false,
                        error = errorMsg
                    )
                    println("Error fetching leaderboard data: $errorMsg")
                    isAnalyticsRunning = false
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLeaderboard = false,
                    error = "Failed to calculate rankings. Please try again."
                )
                println("Exception calculating leaderboard: ${e.message}")
                e.printStackTrace()
                isAnalyticsRunning = false
            }
        }
    }

    private fun calculateEngagementScore(
        storiesAuthored: Int,
        storiesContributed: Int,
        totalLikes: Int,
        completedStories: Int,
        avgWritingTime: Long,
        uniqueGenres: Set<String>
    ): Double {
        // Weighted engagement score algorithm
        val authorWeight = 3.0 // Creating stories is weighted higher
        val contributionWeight = 2.0
        val likesWeight = 1.5
        val completionWeight = 2.5
        val speedBonusWeight = 0.5 // Bonus for faster writing (inverted)
        val genreDiversityWeight = 1.0 // Bonus for writing in multiple genres

        val authorScore = storiesAuthored * authorWeight
        val contributionScore = storiesContributed * contributionWeight
        val likesScore = totalLikes * likesWeight
        val completionScore = completedStories * completionWeight
        
        // Speed bonus (faster writers get bonus points)
        val speedBonus = if (avgWritingTime > 0) {
            // Convert milliseconds to minutes, then give bonus for writing under 10 minutes
            val avgMinutes = avgWritingTime / (1000.0 * 60.0)
            val speedBonus = if (avgMinutes < 10) (10 - avgMinutes) * speedBonusWeight else 0.0
            maxOf(0.0, speedBonus)
        } else 0.0

        // Genre diversity bonus (more genres = higher engagement)
        val genreDiversityBonus = uniqueGenres.size * genreDiversityWeight

        return authorScore + contributionScore + likesScore + completionScore + speedBonus + genreDiversityBonus
    }

    

    fun fetchActiveLiveBooks() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingLiveBooks = true, error = null)
                val currentUserId = getEmail()
                val result = liveBookRepository.fetchActiveLiveBooks(currentUserId)
                if (result.isSuccess()) {
                    val books = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        activeLiveBooks = books,
                        isLoadingLiveBooks = false,
                        error = null
                    )
                    println("Fetched ${books.size} active live books")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLiveBooks = false,
                        error = result.getErrorMessage()
                    )
                    println("Error fetching active live books: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLiveBooks = false,
                    error = e.message ?: "Failed to fetch active live books"
                )
                println("Exception fetching active live books: ${e.message}")
            }
        }
    }

    fun fetchCompletedLiveBooks() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingCompletedBooks = true, error = null)
                val currentUserId = getEmail()
                val result = liveBookRepository.fetchCompletedLiveBooks(currentUserId)
                if (result.isSuccess()) {
                    val books = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        completedLiveBooks = books,
                        isLoadingCompletedBooks = false,
                        error = null
                    )
                    println("Fetched ${books.size} completed live books")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingCompletedBooks = false,
                        error = result.getErrorMessage()
                    )
                    println("Error fetching completed live books: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingCompletedBooks = false,
                    error = e.message ?: "Failed to fetch completed live books"
                )
                println("Exception fetching completed live books: ${e.message}")
            }
        }
    }

    fun fetchArchivedLiveBooks() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingArchivedBooks = true, error = null)
                val currentUserId = getEmail()
                val result = liveBookRepository.fetchArchivedLiveBooks(currentUserId)
                if (result.isSuccess()) {
                    val books = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        archivedLiveBooks = books,
                        isLoadingArchivedBooks = false,
                        error = null
                    )
                    println("Fetched ${books.size} archived live books")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingArchivedBooks = false,
                        error = result.getErrorMessage()
                    )
                    println("Error fetching archived live books: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingArchivedBooks = false,
                    error = e.message ?: "Failed to fetch archived live books"
                )
                println("Exception fetching archived live books: ${e.message}")
            }
        }
    }

    fun getUserLiveBooks(userId: String? = null) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Set loading state before fetching
                _uiState.value = _uiState.value.copy(isLoadingUserLiveBooks = true, error = null)

                val targetUserId = userId ?: getEmail()
                val result = liveBookRepository.getUserLiveBooks(targetUserId)

                if (result.isSuccess()) {
                    val books = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        userLiveBooks = books,
                        isLoadingUserLiveBooks = false,
                        error = null
                    )
                    println("Fetched ${books.size} user live books for userId: $targetUserId")
                } else {
                    _uiState.value = _uiState.value.copy(
                        userLiveBooks = emptyList(),
                        isLoadingUserLiveBooks = false,
                        error = result.getErrorMessage()
                    )
                    println("Error fetching user live books: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    userLiveBooks = emptyList(),
                    isLoadingUserLiveBooks = false,
                    error = e.message ?: "Failed to fetch user live books"
                )
                println("Exception fetching user live books: ${e.message}")
            }
        }
    }

    fun clearUserLiveBooks() {
        _uiState.value = _uiState.value.copy(
            userLiveBooks = emptyList(),
            isLoadingUserLiveBooks = false
        )
    }

    fun loadAllUsers(limit: Int = 200, lastUserId: String? = null) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                var allFetchedUsers: List<User> = if (lastUserId == null) emptyList() else _uiState.value.allUsers
                var currentLastUserId: String? = lastUserId
                var hasMore = true

                // Auto-paginate to fetch ALL users
                while (hasMore) {
                    val result = feedRepository.getAllUsers(limit, currentLastUserId)
                    result
                        .onSuccess { users: List<User> ->
                            allFetchedUsers = allFetchedUsers + users
                            hasMore = users.size == limit
                            currentLastUserId = users.lastOrNull()?.id
                            println("Loaded ${users.size} users. Total so far: ${allFetchedUsers.size}")
                        }
                        .onFailure { error: Throwable ->
                            hasMore = false
                            println("Error loading users page: ${error.message}")
                        }
                }

                _uiState.value = _uiState.value.copy(
                    allUsers = allFetchedUsers,
                    isLoading = false,
                    hasMorePages = false,
                    error = null
                )
                println("Successfully loaded all ${allFetchedUsers.size} users.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadGenres() {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingGenres = true)

                nomenclatureRepository.readGenresFlow().collectLatest { data ->
                    if (data.isSuccess()) {
                        val fetchedGenres = data.getSuccessData()
                        val allGenres = fetchedGenres.flatMap { it.genres }.distinct().sorted()

                        _uiState.value = _uiState.value.copy(
                            genres = allGenres,
                            isLoadingGenres = false,
                            error = null
                        )
                        println("Successfully loaded ${allGenres.size} genres: $allGenres")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingGenres = false,
                            error = data.getErrorMessage()
                        )
                        println("Error loading genres: ${data.getErrorMessage()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingGenres = false,
                    error = e.message ?: "Failed to load genres"
                )
            }
        }
    }

    fun selectGenre(genre: String) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
    }

    fun updateChallengeTitle(title: String) {
        _uiState.value = _uiState.value.copy(challengeTitle = title)
    }

    fun updateInitialParagraph(paragraph: String) {
        _uiState.value = _uiState.value.copy(initialParagraph = paragraph)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        filterUsers(query)
    }

    private fun filterUsers(query: String) {
        val currentUserId = getEmail() // Get current user ID
        val filtered = if (query.isEmpty()) {
            emptyList()
        } else {
            _uiState.value.allUsers.filter { user ->
                // Exclude current user
                user.email != currentUserId && user.id != currentUserId &&
                        (user.fullname.contains(query, ignoreCase = true) ||
                        user.username.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true))
            }.take(10) // Limit to 10 results for performance
        }

        _uiState.value = _uiState.value.copy(filteredUsers = filtered)
    }

    fun selectUser(user: User) {
        val currentSelected = _uiState.value.selectedUsers
        val maxTaggedParticipants = 7 // Max 7 tagged users + 1 initiator = 8 total
        
        if (!currentSelected.contains(user)) {
            if (currentSelected.size >= maxTaggedParticipants) {
                // Don't add the user, limit reached
                return
            }
            _uiState.value = _uiState.value.copy(
                selectedUsers = currentSelected + user,
                filteredUsers = emptyList()
            )
            searchQuery = ""
        }
    }

    fun removeUser(user: User) {
        val currentSelected = _uiState.value.selectedUsers
        _uiState.value = _uiState.value.copy(
            selectedUsers = currentSelected - user
        )
    }

    fun clearSearch() {
        searchQuery = ""
        _uiState.value = _uiState.value.copy(filteredUsers = emptyList())
    }

    fun toggleLike() {
        _uiState.value = _uiState.value.copy(isLiked = !_uiState.value.isLiked)
    }

    fun fetchLiveBookById(liveBookId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingCurrentBook = true, error = null)
                val result = liveBookRepository.fetchLiveBookById(liveBookId)
                if (result.isSuccess()) {
                    val liveBook = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        currentLiveBook = liveBook,
                        isLoadingCurrentBook = false,
                        error = null
                    )
                    println("Fetched LiveBook with ID: $liveBookId")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingCurrentBook = false,
                        currentLiveBook = null,
                        error = result.getErrorMessage()
                    )
                    println("Error fetching LiveBook by ID: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingCurrentBook = false,
                    currentLiveBook = null,
                    error = e.message ?: "Failed to fetch LiveBook by ID"
                )
                println("Exception fetching LiveBook by ID: ${e.message}")
            }
        }
    }

    fun updateParagraphLikes(liveBookId: String, participantId: String, isLiked: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentLiveBook = _uiState.value.currentLiveBook ?: return@launch

                // Determine which paragraph this participant contributed
                val paragraphNumber = when (participantId) {
                    currentLiveBook.authorId -> 0 // Initial paragraph
                    currentLiveBook.paragraph1ContributorId -> 1
                    currentLiveBook.paragraph2ContributorId -> 2
                    currentLiveBook.paragraph3ContributorId -> 3
                    currentLiveBook.paragraph4ContributorId -> 4
                    currentLiveBook.paragraph5ContributorId -> 5
                    currentLiveBook.paragraph6ContributorId -> 6
                    else -> {
                        println("No paragraph found for participant: $participantId")
                        return@launch
                    }
                }

                val currentUserId = getId()
                val result = liveBookRepository.updateParagraphLikes(
                    liveBookId = liveBookId,
                    paragraphNumber = paragraphNumber,
                    userId = currentUserId,
                    isLiked = isLiked
                )

                if (result.isSuccess()) {
                    // Refresh the LiveBook data to get the updated likes
                    fetchLiveBookById(liveBookId)
                    println("Updated paragraph $paragraphNumber like status to $isLiked")
                } else {
                    println("Error updating paragraph likes: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                println("Exception updating paragraph likes: ${e.message}")
            }
        }
    }

    fun publishNewStory(writingTimeSeconds: Long = 0) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isPublishing = true, error = null)

                val currentState = _uiState.value

                // Get current user ID from preferences or auth
                val currentUserId = getEmail()

                val result = liveBookRepository.publishNewStory(
                    currentUserId = currentUserId,
                    title = currentState.challengeTitle,
                    genre = currentState.selectedGenre,
                    initialParagraph = currentState.initialParagraph,
                    taggedUsers = currentState.selectedUsers,
                    isLiked = currentState.isLiked,
                    initialParagraphWritingTimeSeconds = writingTimeSeconds
                )

                if (result.isSuccess()) {
                    println("Story published successfully with writing time: ${writingTimeSeconds}s")

                    // Update the initial paragraph likes (paragraph 0) if the user liked it
                    if (currentState.isLiked) {
                        val liveBookId = result.getSuccessData()
                        val userId = getId()

                        // Like the initial paragraph (paragraph 0) instead of just favoriting the LiveBook
                        val paragraphLikesResult = liveBookRepository.updateParagraphLikes(
                            liveBookId = liveBookId,
                            paragraphNumber = 0, // Initial paragraph
                            userId = userId,
                            isLiked = true
                        )

                        if (paragraphLikesResult.isSuccess()) {
                            println("Successfully updated initial paragraph likes for new LiveBook: $liveBookId")
                        } else {
                            println("Failed to update initial paragraph likes: ${paragraphLikesResult.getErrorMessage()}")
                        }
                    }
                    
                    // Reset form after successful publish
                    _uiState.value = _uiState.value.copy(
                        challengeTitle = "",
                        initialParagraph = "",
                        selectedGenre = "",
                        selectedUsers = emptyList(),
                        isLiked = false,
                        isPublishing = false,
                        error = null
                    )
                    eventChannel.send(LiveBookEvent.Success)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPublishing = false,
                        error = result.getErrorMessage()
                    )
                    println("Error publishing story: ${result.getErrorMessage()}")
                    eventChannel.send(LiveBookEvent.Error("Error publishing story: ${result.getErrorMessage()}"))

                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPublishing = false,
                    error = e.message ?: "Failed to publish story"
                )
                println("Exception publishing story: ${e.message}")
            }
        }
    }

    fun checkUserParticipation() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingParticipation = true, error = null)
                val currentUserId = getEmail()
                val result = liveBookRepository.checkUserParticipation(currentUserId)

                if (result.isSuccess()) {
                    val participationStatus = result.getSuccessData()
                    val currentBook = _uiState.value.userParticipation?.currentWritingTurn
                    val updatedParticipation = if (currentBook != null) {
                        participationStatus.copy(currentWritingTurn = currentBook)
                    } else {
                        participationStatus
                    }
                    _uiState.value = _uiState.value.copy(
                        userParticipation = updatedParticipation,
                        isLoadingParticipation = false,
                        error = null
                    )
                    println("User participation status: ${participationStatus.isParticipant}")
                    println("Authored books: ${participationStatus.authoredBooks.size}")
                    println("Contributed books: ${participationStatus.contributedBooks.size}")
                    updatedParticipation.currentWritingTurn?.let {
                        println("Current writing turn: ${it.title}")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingParticipation = false,
                        error = result.getErrorMessage()
                    )
                    println("Error checking user participation: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingParticipation = false,
                    error = e.message ?: "Failed to check user participation"
                )
                println("Exception checking user participation: ${e.message}")
            }
        }
    }

    fun submitContribution(
        liveBookId: String,
        paragraphContent: String,
        writingTimeSeconds: Long = 0
    ) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isSubmittingContribution = true, error = null)
                val currentUserId = getId()

                val result = liveBookRepository.submitContribution(
                    liveBookId = liveBookId,
                    contributorId = currentUserId,
                    paragraphContent = paragraphContent.trim(),
                    writingTimeSeconds = writingTimeSeconds
                )

                if (result.isSuccess()) {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingContribution = false,
                        error = null
                    )

                    // Refresh the LiveBook data to get the updated content
                    fetchLiveBookById(liveBookId)

                    // Refresh user participation to update the writing turn status
                    checkUserParticipation()

                    // Refresh active live books
                    fetchActiveLiveBooks()

                    println("Contribution submitted successfully for LiveBook: $liveBookId with writing time: ${writingTimeSeconds}s")
                    eventChannel.send(LiveBookEvent.Success)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingContribution = false,
                        error = result.getErrorMessage()
                    )
                    println("Error submitting contribution: ${result.getErrorMessage()}")
                    eventChannel.send(LiveBookEvent.Error("Error submitting contribution: ${result.getErrorMessage()}"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingContribution = false,
                    error = e.message ?: "Failed to submit contribution"
                )
                println("Exception submitting contribution: ${e.message}")
                eventChannel.send(LiveBookEvent.Error("Exception submitting contribution: ${e.message}"))
            }
        }
    }

    fun switchToLiveBook(newLiveBook: LiveBook) {
        val currentBook = _uiState.value.userParticipation?.currentWritingTurn

        // Store the current book as previous if it's different from the new one
        val previousBook = if (currentBook != null && currentBook.id != newLiveBook.id) {
            currentBook
        } else {
            _uiState.value.previousLiveBook
        }

        // Update participation status with the new book as current writing turn
        val updatedParticipation = _uiState.value.userParticipation?.copy(
            currentWritingTurn = newLiveBook
        )

        _uiState.value = _uiState.value.copy(
            userParticipation = updatedParticipation,
            previousLiveBook = previousBook,
            canRevertToPrevious = previousBook != null && previousBook.id != newLiveBook.id
        )

        println("Switched to LiveBook: ${newLiveBook.title}")
        if (previousBook != null) {
            println("Can revert to previous book: ${previousBook.title}")
        }
    }

    fun revertToPreviousLiveBook() {
        val previousBook = _uiState.value.previousLiveBook
        val currentBook = _uiState.value.userParticipation?.currentWritingTurn

        if (previousBook != null) {
            // Switch back to the previous book
            val updatedParticipation = _uiState.value.userParticipation?.copy(
                currentWritingTurn = previousBook
            )

            _uiState.value = _uiState.value.copy(
                userParticipation = updatedParticipation,
                previousLiveBook = currentBook, // Current becomes the new previous
                canRevertToPrevious = false // Reset revert capability after one revert
            )

            println("Reverted to previous LiveBook: ${previousBook.title}")
        } else {
            println("No previous LiveBook to revert to")
        }
    }

    fun clearPreviousLiveBook() {
        _uiState.value = _uiState.value.copy(
            previousLiveBook = null,
            canRevertToPrevious = false
        )
        println("Cleared previous LiveBook reference")
    }

    fun updateLiveBookFavoriteStatus(liveBookId: String, isLiked: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentUserId = getId()
                val result = liveBookRepository.updateLiveBookFavoriteStatus(
                    liveBookId = liveBookId,
                    userId = currentUserId,
                    isLiked = isLiked
                )

                if (result.isSuccess()) {
                    println("Successfully updated favorite status for LiveBook $liveBookId: $isLiked")
                    // Optionally refresh the current book or update local state
                } else {
                    println("Failed to update favorite status: ${result.getErrorMessage()}")
                    _uiState.value = _uiState.value.copy(
                        error = result.getErrorMessage()
                    )
                }
            } catch (e: Exception) {
                println("Exception updating favorite status: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update favorite status"
                )
            }
        }
    }

    fun loadLiveBookFavoriteStatus(liveBookId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentUserId = getId()
                val result = liveBookRepository.getUserFavoriteStatus(
                    liveBookId = liveBookId,
                    userId = currentUserId
                )

                if (result.isSuccess()) {
                    val isFavorited = result.getSuccessData()
                    println("Loaded favorite status for LiveBook $liveBookId: $isFavorited")
                    onResult(isFavorited)
                } else {
                    println("Failed to load favorite status: ${result.getErrorMessage()}")
                    onResult(false) // Default to not favorited on error
                }
            } catch (e: Exception) {
                println("Exception loading favorite status: ${e.message}")
                onResult(false) // Default to not favorited on error
            }
        }
    }

    fun getUserEmailById(userId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Use FeedRepository's readProfileFlow to get user by ID/username and extract email
                feedRepository.readProfileFlow(userId).collectLatest { result ->
                    when (result) {
                        is com.flipverse.shared.RequestState.Success -> {
                            val user = result.getSuccessData()
                            onResult(user.email)
                            println("Successfully fetched email for userId $userId: ${user.email}")
                        }

                        is com.flipverse.shared.RequestState.Error -> {
                            onResult(null)
                            println("Error fetching user email for userId $userId: ${result.getErrorMessage()}")
                        }

                        else -> {
                            // Loading or Idle state, no action needed
                        }
                    }
                }
            } catch (e: Exception) {
                onResult(null)
                println("Exception fetching user email for userId $userId: ${e.message}")
            }
        }
    }

    fun loadParagraphComments(liveBookId: String, paragraphNumber: Int) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingComments = true, error = null)

                val result = liveBookRepository.getParagraphComments(liveBookId, paragraphNumber)

                if (result.isSuccess()) {
                    val comments = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        paragraphComments = comments,
                        isLoadingComments = false,
                        error = null
                    )
                    println("Loaded ${comments.size} comments for paragraph $paragraphNumber")
                } else {
                    _uiState.value = _uiState.value.copy(
                        paragraphComments = emptyList(),
                        isLoadingComments = false,
                        error = result.getErrorMessage()
                    )
                    println("Error loading comments: ${result.getErrorMessage()}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    paragraphComments = emptyList(),
                    isLoadingComments = false,
                    error = e.message ?: "Failed to load comments"
                )
                println("Exception loading comments: ${e.message}")
            }
        }
    }

    fun addParagraphComment(
        liveBookId: String,
        paragraphNumber: Int,
        commentText: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isAddingComment = true, error = null)

                val currentUserId = getId()
                val currentUserName = com.flipverse.shared.PreferencesRepository.getFullName()
                val currentUserThumbnail = com.flipverse.shared.PreferencesRepository.getThumbnail()

                val result = liveBookRepository.addParagraphComment(
                    liveBookId = liveBookId,
                    paragraphNumber = paragraphNumber,
                    userId = currentUserId,
                    userName = currentUserName,
                    userThumbnail = currentUserThumbnail,
                    commentText = commentText
                )

                if (result.isSuccess()) {
                    _uiState.value = _uiState.value.copy(isAddingComment = false, error = null)

                    // Refresh comments after adding
                    loadParagraphComments(liveBookId, paragraphNumber)

                    // Refresh the LiveBook to update comment count
                    fetchLiveBookById(liveBookId)

                    println("Comment added successfully")
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAddingComment = false,
                        error = result.getErrorMessage()
                    )
                    println("Error adding comment: ${result.getErrorMessage()}")
                    onError(result.getErrorMessage())
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingComment = false,
                    error = e.message ?: "Failed to add comment"
                )
                println("Exception adding comment: ${e.message}")
                onError(e.message ?: "Failed to add comment")
            }
        }
    }
}