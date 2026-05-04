package com.flipverse.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.BooksRepository
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.CartItem
import com.flipverse.shared.domain.GoogleBookItem
import com.flipverse.shared.domain.GoogleBooksResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class BookStoreViewModel(
    private val booksRepository: BooksRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("BookStoreViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    // Genre lists for variety
    private val fictionGenres = listOf(
        "fiction", "mystery", "romance", "thriller", "fantasy",
        "science fiction", "horror", "adventure", "historical fiction", "crime"
    )
    private val scienceGenres = listOf(
        "science", "physics", "biology", "chemistry", "astronomy",
        "mathematics", "technology", "medicine", "psychology", "nature"
    )
    private val businessGenres = listOf(
        "business", "finance", "economics", "marketing", "leadership",
        "entrepreneurship", "management", "investing", "self-help", "productivity"
    )

    // Track last used categories to ensure variety
    private var lastFictionIndex = 0
    private var lastScienceIndex = 0

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults =
        MutableStateFlow<RequestState<GoogleBooksResponse>>(RequestState.Idle)
    val searchResults: StateFlow<RequestState<GoogleBooksResponse>> = _searchResults.asStateFlow()

    private val _bestSellers =
        MutableStateFlow<RequestState<GoogleBooksResponse>>(RequestState.Idle)
    val bestSellers: StateFlow<RequestState<GoogleBooksResponse>> = _bestSellers.asStateFlow()

    private val _featuredDeals =
        MutableStateFlow<RequestState<GoogleBooksResponse>>(RequestState.Idle)
    val featuredDeals: StateFlow<RequestState<GoogleBooksResponse>> = _featuredDeals.asStateFlow()

    private val _mostWishedFor =
        MutableStateFlow<RequestState<GoogleBooksResponse>>(RequestState.Idle)
    val mostWishedFor: StateFlow<RequestState<GoogleBooksResponse>> = _mostWishedFor.asStateFlow()

    // Track current categories for display
    private val _currentFeaturedCategory = MutableStateFlow("Fiction")
    val currentFeaturedCategory: StateFlow<String> = _currentFeaturedCategory.asStateFlow()

    private val _currentMostWishedCategory = MutableStateFlow("Science")
    val currentMostWishedCategory: StateFlow<String> = _currentMostWishedCategory.asStateFlow()

    private val _selectedBook = MutableStateFlow<RequestState<GoogleBookItem>>(RequestState.Idle)
    val selectedBook: StateFlow<RequestState<GoogleBookItem>> = _selectedBook.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = booksRepository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = booksRepository.getCartTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadBestSellers()
        loadFeaturedDeals()
        loadMostWishedFor()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            searchBooks(query)
        } else {
            _searchResults.value = RequestState.Idle
        }
    }

    private fun searchBooks(query: String) {
        viewModelScope.launch(exceptionHandler) {
            _searchResults.value = RequestState.Loading
            
            var result = booksRepository.searchBooks(query)
            
            // If search returns empty, try with just the first word
            if (result is RequestState.Success && result.data.items.isNullOrEmpty()) {
                val broaderQuery = query.split(" ").firstOrNull()
                if (broaderQuery != null && broaderQuery != query) {
                    result = booksRepository.searchBooks(broaderQuery)
                }
            }
            
            _searchResults.value = result
        }
    }

    private fun loadBestSellers() {
        viewModelScope.launch(exceptionHandler) {
            _bestSellers.value = RequestState.Loading

            // Try up to 5 times. On error, fall back to a business/productivity category
            // so the section stays populated even if the primary endpoint is unavailable.
            var attempt = 0
            var result: RequestState<GoogleBooksResponse>

            do {
                result = if (attempt == 0) {
                    // First attempt: the dedicated bestsellers endpoint
                    val randomOffset = (0..4).random() * 40
                    val orderBy = if (kotlin.random.Random.nextBoolean()) "newest" else "relevance"
                    booksRepository.getBestSellers(startIndex = randomOffset, orderBy = orderBy)
                } else {
                    // Fallback attempts: rotate through business genres
                    val fallbackCategory = businessGenres[(attempt - 1) % businessGenres.size]
                    val randomOffset = (0..3).random() * 40
                    booksRepository.getBooksByCategory(
                        category = fallbackCategory,
                        startIndex = randomOffset,
                        orderBy = "relevance"
                    )
                }
                attempt++
            } while (shouldRetry(result) && attempt < 5)

            _bestSellers.value = sanitiseError(result)
        }
    }

    private fun loadFeaturedDeals() {
        viewModelScope.launch(exceptionHandler) {
            _featuredDeals.value = RequestState.Loading

            // Rotate through fiction genres, retrying on both empty results AND errors.
            var attempt = 0
            var result: RequestState<GoogleBooksResponse>

            do {
                lastFictionIndex = (lastFictionIndex + 1) % fictionGenres.size
                val category = fictionGenres[lastFictionIndex]
                _currentFeaturedCategory.value = category.replaceFirstChar { it.uppercase() }
                val randomOffset = (0..3).random() * 40
                result = booksRepository.getBooksByCategory(
                    category = category,
                    startIndex = randomOffset,
                    orderBy = "newest"
                )
                attempt++
            } while (shouldRetry(result) && attempt < 5)

            _featuredDeals.value = sanitiseError(result)
        }
    }

    private fun loadMostWishedFor() {
        viewModelScope.launch(exceptionHandler) {
            _mostWishedFor.value = RequestState.Loading

            // Rotate through science genres, retrying on both empty results AND errors.
            var attempt = 0
            var result: RequestState<GoogleBooksResponse>

            do {
                lastScienceIndex = (lastScienceIndex + 1) % scienceGenres.size
                val category = scienceGenres[lastScienceIndex]
                _currentMostWishedCategory.value = category.replaceFirstChar { it.uppercase() }
                val randomOffset = (0..3).random() * 40
                result = booksRepository.getBooksByCategory(
                    category = category,
                    startIndex = randomOffset,
                    orderBy = "relevance"
                )
                attempt++
            } while (shouldRetry(result) && attempt < 5)

            _mostWishedFor.value = sanitiseError(result)
        }
    }

    /**
     * Returns true when the loop should try another category:
     * — result is an error (e.g. 503 Service Unavailable), OR
     * — result succeeded but came back with no books.
     */
    private fun shouldRetry(result: RequestState<GoogleBooksResponse>): Boolean =
        result is RequestState.Error ||
        (result is RequestState.Success && result.data.items.isNullOrEmpty())

    /**
     * If all retries were exhausted and the last result is still an error,
     * replace the raw technical message (which may contain JSON / HTTP details)
     * with a short, user-friendly string.
     */
    private fun sanitiseError(result: RequestState<GoogleBooksResponse>): RequestState<GoogleBooksResponse> =
        if (result is RequestState.Error) {
            RequestState.Error("Books are temporarily unavailable. Pull down to refresh.")
        } else {
            result
        }

    /**
     * Refresh all sections with new random books
     */
    fun refreshAllSections() {
        loadBestSellers()
        loadFeaturedDeals()
        loadMostWishedFor()
    }

    fun loadBookDetails(bookId: String) {
        viewModelScope.launch(exceptionHandler) {
            _selectedBook.value = RequestState.Loading
            _selectedBook.value = booksRepository.getBookDetails(bookId)
        }
    }

    fun addToCart(book: GoogleBookItem) {
        viewModelScope.launch(exceptionHandler) {
            val price = book.saleInfo?.retailPrice?.amount ?: 0.0
            val currencyCode = book.saleInfo?.retailPrice?.currencyCode ?: "USD"
            val authors = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author"
            val imageUrl = book.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://")
            val buyLink = book.saleInfo?.buyLink

            val cartItem = CartItem(
                bookId = book.id,
                title = book.volumeInfo.title,
                authors = authors,
                price = price,
                currencyCode = currencyCode,
                imageUrl = imageUrl,
                buyLink = buyLink,
                quantity = 1
            )

            booksRepository.addToCart(cartItem)
        }
    }

    fun removeFromCart(bookId: String) {
        viewModelScope.launch(exceptionHandler) {
            booksRepository.removeFromCart(bookId)
        }
    }

    fun updateCartItemQuantity(bookId: String, quantity: Int) {
        viewModelScope.launch(exceptionHandler) {
            booksRepository.updateCartItemQuantity(bookId, quantity)
        }
    }

    fun clearCart() {
        viewModelScope.launch(exceptionHandler) {
            booksRepository.clearCart()
        }
    }
}
