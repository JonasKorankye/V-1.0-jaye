package com.flipverse.data

import com.flipverse.data.domain.LibraryRepository
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.Bookmark
import com.flipverse.shared.domain.Highlight
import com.flipverse.shared.domain.LibraryBook
import com.flipverse.shared.domain.Note
import com.flipverse.shared.domain.ReadingStatus
import com.flipverse.shared.domain.ReaderPreferences
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class LibraryRepositoryImpl : LibraryRepository {

    private val database = Firebase.firestore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Book management
    override suspend fun addBook(book: LibraryBook): RequestState<Unit> {
        return try {
            val userId = PreferencesRepository.getId()
            if (userId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            database.collection("library")
                .document(userId)
                .collection("books")
                .document(book.id)
                .set(book)

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to add book")
        }
    }

    override suspend fun updateBook(book: LibraryBook): RequestState<Unit> {
        return try {
            val userId = PreferencesRepository.getId()
            if (userId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            database.collection("library")
                .document(userId)
                .collection("books")
                .document(book.id)
                .set(book)

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update book")
        }
    }

    override suspend fun deleteBook(bookId: String): RequestState<Unit> {
        return try {
            val userId = PreferencesRepository.getId()
            if (userId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            database.collection("library")
                .document(userId)
                .collection("books")
                .document(bookId)
                .delete()

            // Also delete stored file if exists
            PreferencesRepository.remove("book_file_$bookId")

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to delete book")
        }
    }

    override suspend fun getBook(bookId: String): RequestState<LibraryBook> {
        return try {
            val userId = PreferencesRepository.getId()
            if (userId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            val document = database.collection("library")
                .document(userId)
                .collection("books")
                .document(bookId)
                .get()

            val book = document.data<LibraryBook>()
            RequestState.Success(book)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get book")
        }
    }

    override fun getBooksByStatus(
        userId: String,
        status: ReadingStatus
    ): Flow<RequestState<List<LibraryBook>>> = callbackFlow {
        try {
            val actualUserId = userId.ifEmpty { PreferencesRepository.getId() }
            if (actualUserId.isEmpty()) {
                send(RequestState.Error("User not logged in"))
                close()
                return@callbackFlow
            }

            val subscription = database.collection("library")
                .document(actualUserId)
                .collection("books")
                .where { "readingStatus" equalTo status.name }
                .snapshots
                .collect { snapshot ->
                    val books = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.data<LibraryBook>()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    send(RequestState.Success(books))
                }

            awaitClose { }
        } catch (e: Exception) {
            send(RequestState.Error(e.message ?: "Failed to get books"))
            close()
        }
    }.catch { e ->
        emit(RequestState.Error(e.message ?: "Failed to get books"))
    }

    override fun getAllBooks(userId: String): Flow<RequestState<List<LibraryBook>>> =
        callbackFlow {
            try {
                val actualUserId = userId.ifEmpty { PreferencesRepository.getId() }
                if (actualUserId.isEmpty()) {
                    send(RequestState.Error("User not logged in"))
                    close()
                    return@callbackFlow
                }

                database.collection("library")
                    .document(actualUserId)
                    .collection("books")
                    .snapshots
                    .collect { snapshot ->
                        val books = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<LibraryBook>()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        send(RequestState.Success(books))
                    }

                awaitClose { }
            } catch (e: Exception) {
                send(RequestState.Error(e.message ?: "Failed to get books"))
                close()
            }
        }.catch { e ->
            emit(RequestState.Error(e.message ?: "Failed to get books"))
        }

    // Reading progress
    override suspend fun updateReadingProgress(
        bookId: String,
        currentPage: Int
    ): RequestState<Unit> {
        return try {
            val userId = PreferencesRepository.getId()
            if (userId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val updatedBook = bookResult.data.copy(
                    currentPage = currentPage,
                    lastReadDate = Clock.System.now().toEpochMilliseconds().toString()
                )
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update reading progress")
        }
    }

    override suspend fun updateReadingStatus(
        bookId: String,
        status: ReadingStatus
    ): RequestState<Unit> {
        return try {
            val userId = PreferencesRepository.getId()
            if (userId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val updatedBook = bookResult.data.copy(
                    readingStatus = status.name,
                    currentPage = if (status == ReadingStatus.CURRENTLY_READING) 
                        bookResult.data.currentPage ?: 1 else bookResult.data.currentPage
                )
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update reading status")
        }
    }

    // Bookmarks
    override suspend fun addBookmark(bookId: String, bookmark: Bookmark): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val currentBookmarks = bookResult.data.bookmarks.orEmpty().toMutableList()
                currentBookmarks.add(bookmark)
                val updatedBook = bookResult.data.copy(bookmarks = currentBookmarks)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to add bookmark")
        }
    }

    override suspend fun removeBookmark(bookId: String, bookmarkId: String): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val updatedBookmarks =
                    bookResult.data.bookmarks.orEmpty().filter { it.id != bookmarkId }
                val updatedBook = bookResult.data.copy(bookmarks = updatedBookmarks)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to remove bookmark")
        }
    }

    override suspend fun getBookmarks(bookId: String): RequestState<List<Bookmark>> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                RequestState.Success(bookResult.data.bookmarks.orEmpty())
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get bookmarks")
        }
    }

    // Highlights
    override suspend fun addHighlight(bookId: String, highlight: Highlight): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val currentHighlights = bookResult.data.highlights.orEmpty().toMutableList()
                currentHighlights.add(highlight)
                val updatedBook = bookResult.data.copy(highlights = currentHighlights)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to add highlight")
        }
    }

    override suspend fun removeHighlight(bookId: String, highlightId: String): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val updatedHighlights =
                    bookResult.data.highlights.orEmpty().filter { it.id != highlightId }
                val updatedBook = bookResult.data.copy(highlights = updatedHighlights)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to remove highlight")
        }
    }

    override suspend fun getHighlights(bookId: String): RequestState<List<Highlight>> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                RequestState.Success(bookResult.data.highlights.orEmpty())
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get highlights")
        }
    }

    // Notes
    override suspend fun addNote(bookId: String, note: Note): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val currentNotes = bookResult.data.notes.orEmpty().toMutableList()
                currentNotes.add(note)
                val updatedBook = bookResult.data.copy(notes = currentNotes)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to add note")
        }
    }

    override suspend fun updateNote(bookId: String, note: Note): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val updatedNotes = bookResult.data.notes.orEmpty().map { existingNote ->
                    if (existingNote.id == note.id) {
                        note.copy(dateModified = Clock.System.now().toEpochMilliseconds().toString())
                    } else {
                        existingNote
                    }
                }
                val updatedBook = bookResult.data.copy(notes = updatedNotes)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update note")
        }
    }

    override suspend fun removeNote(bookId: String, noteId: String): RequestState<Unit> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                val updatedNotes = bookResult.data.notes.orEmpty().filter { it.id != noteId }
                val updatedBook = bookResult.data.copy(notes = updatedNotes)
                updateBook(updatedBook)
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to remove note")
        }
    }

    override suspend fun getNotes(bookId: String): RequestState<List<Note>> {
        return try {
            val bookResult = getBook(bookId)
            if (bookResult is RequestState.Success) {
                RequestState.Success(bookResult.data.notes.orEmpty())
            } else {
                RequestState.Error("Book not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get notes")
        }
    }

    // Reader preferences
    override suspend fun getReaderPreferences(userId: String): RequestState<ReaderPreferences> {
        return try {
            val actualUserId = userId.ifEmpty { PreferencesRepository.getId() }
            if (actualUserId.isEmpty()) {
                return RequestState.Error("User not logged in")
            }

            val prefsJson = PreferencesRepository.getString("reader_prefs_$actualUserId", "")
            if (prefsJson.isEmpty()) {
                // Return default preferences
                val defaultPrefs = ReaderPreferences(userId = actualUserId)
                RequestState.Success(defaultPrefs)
            } else {
                val prefs = json.decodeFromString<ReaderPreferences>(prefsJson)
                RequestState.Success(prefs)
            }
        } catch (e: Exception) {
            // Return default on error
            val actualUserId = userId.ifEmpty { PreferencesRepository.getId() }
            RequestState.Success(ReaderPreferences(userId = actualUserId))
        }
    }

    override suspend fun updateReaderPreferences(preferences: ReaderPreferences): RequestState<Unit> {
        return try {
            val prefsJson = json.encodeToString(preferences)
            PreferencesRepository.putString("reader_prefs_${preferences.userId}", prefsJson)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update reader preferences")
        }
    }

    // File storage (using local preferences for now - in production, use Cloud Storage)
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun saveBookFile(bookId: String, fileBytes: ByteArray): RequestState<String> {
        return try {
            // For now, store as base64 in preferences (not recommended for large files)
            // In production, upload to Firebase Cloud Storage or similar
            val base64String = Base64.encode(fileBytes)
            PreferencesRepository.putString("book_file_$bookId", base64String)
            RequestState.Success("local_storage_$bookId")
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to save book file")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getBookFile(bookId: String): RequestState<ByteArray> {
        return try {
            val base64String = PreferencesRepository.getString("book_file_$bookId", "")
            if (base64String.isEmpty()) {
                return RequestState.Error("Book file not found")
            }
            val fileBytes = Base64.decode(base64String)
            RequestState.Success(fileBytes)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get book file")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun saveCoverImage(bookId: String, imageBytes: ByteArray): RequestState<String> {
        return try {
            val base64String = Base64.encode(imageBytes)
            PreferencesRepository.putString("book_cover_$bookId", base64String)
            RequestState.Success("cover_$bookId")
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to save cover image")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getCoverImage(bookId: String): RequestState<ByteArray> {
        return try {
            val base64String = PreferencesRepository.getString("book_cover_$bookId", "")
            if (base64String.isEmpty()) {
                return RequestState.Error("Cover image not found")
            }
            val imageBytes = Base64.decode(base64String)
            RequestState.Success(imageBytes)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get cover image")
        }
    }
}
