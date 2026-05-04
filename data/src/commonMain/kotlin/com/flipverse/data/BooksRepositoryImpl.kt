package com.flipverse.data

import com.flipverse.data.domain.BooksRepository
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.CartItem
import com.flipverse.shared.domain.GoogleBookItem
import com.flipverse.shared.domain.GoogleBooksResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class BooksRepositoryImpl(
    private val httpClient: HttpClient
) : BooksRepository {

    private val baseUrl = "https://www.googleapis.com/books/v1/volumes"

    /** Maps an HTTP status code to a short, user-readable message (no raw JSON exposed). */
    private fun httpErrorMessage(statusCode: Int): String = when (statusCode) {
        503 -> "Service temporarily unavailable"
        429 -> "Too many requests — please try again later"
        401, 403 -> "Access denied"
        404 -> "Content not found"
        else -> "Something went wrong (error $statusCode)"
    }

    private val apiKey: String? = BuildKonfig.GOOGLE_BOOKS_API_KEY.ifEmpty { null }
    private val cartItems = MutableStateFlow<List<CartItem>>(emptyList())

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        // Load cart from preferences
        val savedCart = PreferencesRepository.getString("cart_items", "")
        if (savedCart.isNotEmpty()) {
            try {
                cartItems.value = json.decodeFromString(savedCart)
            } catch (e: Exception) {
                println("Error loading cart: ${e.message}")
            }
        }
    }

    override suspend fun searchBooks(
        query: String,
        startIndex: Int,
        maxResults: Int
    ): RequestState<GoogleBooksResponse> {
        return try {
            val httpResponse = httpClient.get(baseUrl) {
                parameter("q", query)
                parameter("startIndex", startIndex)
                parameter("maxResults", maxResults)
                parameter("printType", "books")
                parameter("country", "US")
                apiKey?.let { parameter("key", it) }
            }

            if (httpResponse.status.value !in 200..299) {
                val errorBody = httpResponse.bodyAsText()
                println("searchBooks HTTP ${httpResponse.status.value}: $errorBody")
                return RequestState.Error(httpErrorMessage(httpResponse.status.value))
            }

            val response = httpResponse.body<GoogleBooksResponse>()
            RequestState.Success(response)
        } catch (e: Exception) {
            println("Error searching books: ${e.message}")
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "searchBooks (Explore)",
                additionalInfo = mapOf(
                    "query" to query,
                    "start_index" to startIndex.toString(),
                    "max_results" to maxResults.toString()
                )
            )
            RequestState.Error(e.message ?: "Failed to search books")
        }
    }

    override suspend fun getBooksByCategory(
        category: String,
        startIndex: Int,
        maxResults: Int,
        orderBy: String
    ): RequestState<GoogleBooksResponse> {
        return try {
            val httpResponse = httpClient.get(baseUrl) {
                parameter("q", "subject:$category")
                parameter("startIndex", startIndex)
                parameter("maxResults", maxResults)
                parameter("printType", "books")
                parameter("orderBy", orderBy)
                parameter("country", "US")
                apiKey?.let { parameter("key", it) }
            }

            if (httpResponse.status.value !in 200..299) {
                val errorBody = httpResponse.bodyAsText()
                println("getBooksByCategory HTTP ${httpResponse.status.value}: $errorBody")
                return RequestState.Error(httpErrorMessage(httpResponse.status.value))
            }

            val response = httpResponse.body<GoogleBooksResponse>()
            RequestState.Success(response)
        } catch (e: Exception) {
            println("Error getting books by category: ${e.message}")
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "getBooksByCategory (Explore)",
                additionalInfo = mapOf(
                    "category" to category,
                    "start_index" to startIndex.toString()
                )
            )
            RequestState.Error(e.message ?: "Failed to get books by category")
        }
    }

    override suspend fun getBestSellers(
        startIndex: Int,
        maxResults: Int,
        orderBy: String
    ): RequestState<GoogleBooksResponse> {
        return try {
            val httpResponse = httpClient.get(baseUrl) {
                parameter("q", "bestseller")
                parameter("startIndex", startIndex)
                parameter("maxResults", maxResults)
                parameter("printType", "books")
                parameter("orderBy", orderBy)
                parameter("country", "US")
                apiKey?.let { parameter("key", it) }
            }

            if (httpResponse.status.value !in 200..299) {
                val errorBody = httpResponse.bodyAsText()
                println("getBestSellers HTTP ${httpResponse.status.value}: $errorBody")
                return RequestState.Error(httpErrorMessage(httpResponse.status.value))
            }

            val response = httpResponse.body<GoogleBooksResponse>()
            RequestState.Success(response)
        } catch (e: Exception) {
            println("Error getting best sellers: ${e.message}")
            RequestState.Error(e.message ?: "Failed to get best sellers")
        }
    }

    override suspend fun getBookDetails(bookId: String): RequestState<GoogleBookItem> {
        return try {
            val httpResponse = httpClient.get("$baseUrl/$bookId") {
                parameter("country", "US")
                apiKey?.let { parameter("key", it) }
            }

            if (httpResponse.status.value !in 200..299) {
                val errorBody = httpResponse.bodyAsText()
                println("getBookDetails HTTP ${httpResponse.status.value}: $errorBody")
                return RequestState.Error(httpErrorMessage(httpResponse.status.value))
            }

            val response = httpResponse.body<GoogleBookItem>()
            RequestState.Success(response)
        } catch (e: Exception) {
            println("Error getting book details: ${e.message}")
            RequestState.Error(e.message ?: "Failed to get book details")
        }
    }

    override fun getCartItems(): Flow<List<CartItem>> = cartItems

    override suspend fun addToCart(item: CartItem): RequestState<Unit> {
        return try {
            val currentItems = cartItems.value.toMutableList()
            val existingItemIndex = currentItems.indexOfFirst { it.bookId == item.bookId }

            if (existingItemIndex != -1) {
                // Update quantity if item already exists
                currentItems[existingItemIndex] = currentItems[existingItemIndex].copy(
                    quantity = currentItems[existingItemIndex].quantity + item.quantity
                )
            } else {
                // Add new item
                currentItems.add(item)
            }

            cartItems.value = currentItems
            saveCart()
            RequestState.Success(Unit)
        } catch (e: Exception) {
            println("Error adding to cart: ${e.message}")
            RequestState.Error(e.message ?: "Failed to add to cart")
        }
    }

    override suspend fun removeFromCart(bookId: String): RequestState<Unit> {
        return try {
            cartItems.value = cartItems.value.filter { it.bookId != bookId }
            saveCart()
            RequestState.Success(Unit)
        } catch (e: Exception) {
            println("Error removing from cart: ${e.message}")
            RequestState.Error(e.message ?: "Failed to remove from cart")
        }
    }

    override suspend fun updateCartItemQuantity(bookId: String, quantity: Int): RequestState<Unit> {
        return try {
            if (quantity <= 0) {
                return removeFromCart(bookId)
            }

            val currentItems = cartItems.value.toMutableList()
            val itemIndex = currentItems.indexOfFirst { it.bookId == bookId }

            if (itemIndex != -1) {
                currentItems[itemIndex] = currentItems[itemIndex].copy(quantity = quantity)
                cartItems.value = currentItems
                saveCart()
            }

            RequestState.Success(Unit)
        } catch (e: Exception) {
            println("Error updating cart item quantity: ${e.message}")
            RequestState.Error(e.message ?: "Failed to update quantity")
        }
    }

    override suspend fun clearCart(): RequestState<Unit> {
        return try {
            cartItems.value = emptyList()
            saveCart()
            RequestState.Success(Unit)
        } catch (e: Exception) {
            println("Error clearing cart: ${e.message}")
            RequestState.Error(e.message ?: "Failed to clear cart")
        }
    }

    override fun getCartTotal(): Flow<Double> = cartItems.map { items ->
        items.sumOf { it.price * it.quantity }
    }

    private fun saveCart() {
        try {
            val cartJson = json.encodeToString(cartItems.value)
            PreferencesRepository.putString("cart_items", cartJson)
        } catch (e: Exception) {
            println("Error saving cart: ${e.message}")
        }
    }
}
