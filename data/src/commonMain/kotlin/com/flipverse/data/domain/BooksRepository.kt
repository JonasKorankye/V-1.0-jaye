package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.CartItem
import com.flipverse.shared.domain.GoogleBookItem
import com.flipverse.shared.domain.GoogleBooksResponse
import kotlinx.coroutines.flow.Flow

interface BooksRepository {
    suspend fun searchBooks(
        query: String,
        startIndex: Int = 0,
        maxResults: Int = 40
    ): RequestState<GoogleBooksResponse>

    suspend fun getBooksByCategory(
        category: String,
        startIndex: Int = 0,
        maxResults: Int = 40,
        orderBy: String = "relevance"
    ): RequestState<GoogleBooksResponse>

    suspend fun getBestSellers(
        startIndex: Int = 0,
        maxResults: Int = 40,
        orderBy: String = "relevance"
    ): RequestState<GoogleBooksResponse>

    suspend fun getBookDetails(bookId: String): RequestState<GoogleBookItem>

    // Cart operations
    fun getCartItems(): Flow<List<CartItem>>
    suspend fun addToCart(item: CartItem): RequestState<Unit>
    suspend fun removeFromCart(bookId: String): RequestState<Unit>
    suspend fun updateCartItemQuantity(bookId: String, quantity: Int): RequestState<Unit>
    suspend fun clearCart(): RequestState<Unit>
    fun getCartTotal(): Flow<Double>
}
