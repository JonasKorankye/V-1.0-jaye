package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.Bookmark
import com.flipverse.shared.domain.Highlight
import com.flipverse.shared.domain.LibraryBook
import com.flipverse.shared.domain.Note
import com.flipverse.shared.domain.ReadingStatus
import com.flipverse.shared.domain.ReaderPreferences
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    // Book management
    suspend fun addBook(book: LibraryBook): RequestState<Unit>
    suspend fun updateBook(book: LibraryBook): RequestState<Unit>
    suspend fun deleteBook(bookId: String): RequestState<Unit>
    suspend fun getBook(bookId: String): RequestState<LibraryBook>
    fun getBooksByStatus(
        userId: String,
        status: ReadingStatus
    ): Flow<RequestState<List<LibraryBook>>>

    fun getAllBooks(userId: String): Flow<RequestState<List<LibraryBook>>>

    // Reading progress
    suspend fun updateReadingProgress(bookId: String, currentPage: Int): RequestState<Unit>
    suspend fun updateReadingStatus(bookId: String, status: ReadingStatus): RequestState<Unit>

    // Bookmarks
    suspend fun addBookmark(bookId: String, bookmark: Bookmark): RequestState<Unit>
    suspend fun removeBookmark(bookId: String, bookmarkId: String): RequestState<Unit>
    suspend fun getBookmarks(bookId: String): RequestState<List<Bookmark>>

    // Highlights
    suspend fun addHighlight(bookId: String, highlight: Highlight): RequestState<Unit>
    suspend fun removeHighlight(bookId: String, highlightId: String): RequestState<Unit>
    suspend fun getHighlights(bookId: String): RequestState<List<Highlight>>

    // Notes
    suspend fun addNote(bookId: String, note: Note): RequestState<Unit>
    suspend fun updateNote(bookId: String, note: Note): RequestState<Unit>
    suspend fun removeNote(bookId: String, noteId: String): RequestState<Unit>
    suspend fun getNotes(bookId: String): RequestState<List<Note>>

    // Reader preferences
    suspend fun getReaderPreferences(userId: String): RequestState<ReaderPreferences>
    suspend fun updateReaderPreferences(preferences: ReaderPreferences): RequestState<Unit>

    // File storage
    suspend fun saveBookFile(
        bookId: String,
        fileBytes: ByteArray
    ): RequestState<String> // Returns storage path

    suspend fun getBookFile(bookId: String): RequestState<ByteArray>

    // Cover image storage
    suspend fun saveCoverImage(bookId: String, imageBytes: ByteArray): RequestState<String>
    suspend fun getCoverImage(bookId: String): RequestState<ByteArray>
}
