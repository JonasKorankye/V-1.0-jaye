package com.flipverse.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.BooksRepository
import com.flipverse.data.domain.LibraryRepository
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.LibraryBook
import com.flipverse.shared.domain.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LibraryViewModel(
    private val booksRepository: BooksRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("LibraryViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _currentlyReading =
        MutableStateFlow<RequestState<List<LibraryBook>>>(RequestState.Idle)
    val currentlyReading: StateFlow<RequestState<List<LibraryBook>>> =
        _currentlyReading.asStateFlow()

    private val _wantToRead = MutableStateFlow<RequestState<List<LibraryBook>>>(RequestState.Idle)
    val wantToRead: StateFlow<RequestState<List<LibraryBook>>> = _wantToRead.asStateFlow()

    private val _alreadyRead = MutableStateFlow<RequestState<List<LibraryBook>>>(RequestState.Idle)
    val alreadyRead: StateFlow<RequestState<List<LibraryBook>>> = _alreadyRead.asStateFlow()

    init {
        loadLibraryBooks()
    }

    private fun loadLibraryBooks() {
        val userId = PreferencesRepository.getId()
        if (userId.isEmpty()) {
            _currentlyReading.value = RequestState.Error("User not logged in")
            _wantToRead.value = RequestState.Error("User not logged in")
            _alreadyRead.value = RequestState.Error("User not logged in")
            return
        }

        viewModelScope.launch(exceptionHandler) {
            // Load books by status from repository
            launch {
                libraryRepository.getBooksByStatus(userId, ReadingStatus.CURRENTLY_READING)
                    .collect { state ->
                        _currentlyReading.value = state
                    }
            }

            launch {
                libraryRepository.getBooksByStatus(userId, ReadingStatus.WANT_TO_READ)
                    .collect { state ->
                        _wantToRead.value = state
                    }
            }

            launch {
                libraryRepository.getBooksByStatus(userId, ReadingStatus.ALREADY_READ)
                    .collect { state ->
                        _alreadyRead.value = state
                    }
            }
        }
    }

    fun addBook(book: LibraryBook) {
        viewModelScope.launch(exceptionHandler) {
            libraryRepository.addBook(book)
        }
    }

    fun removeBook(bookId: String, status: ReadingStatus) {
        viewModelScope.launch(exceptionHandler) {
            libraryRepository.deleteBook(bookId)
        }
    }

    fun updateReadingStatus(bookId: String, newStatus: ReadingStatus) {
        viewModelScope.launch(exceptionHandler) {
            libraryRepository.updateReadingStatus(bookId, newStatus)
        }
    }

    fun refreshLibrary() {
        loadLibraryBooks()
    }

    fun importBookFromFile(fileName: String, fileBytes: ByteArray?) {
        viewModelScope.launch(exceptionHandler) {
            // Always use the filename (without extension) exactly as it appears on the device
            val fileNameWithoutExtension = fileName.substringBeforeLast(".")
            val finalTitle = fileNameWithoutExtension

            // Try to extract author from file metadata
            var extractedAuthor: String? = null
            if (fileBytes != null) {
                when {
                    fileName.endsWith(".epub", ignoreCase = true) -> {
                        val (_, author) = extractEpubMetadata(fileBytes, fileNameWithoutExtension)
                        extractedAuthor = author
                    }
                    fileName.endsWith(".pdf", ignoreCase = true) -> {
                        val (_, author) = extractPdfMetadata(fileBytes, fileNameWithoutExtension)
                        extractedAuthor = author
                    }
                }
            }

            // Fetch supplementary metadata (cover image, authors, page count, etc.) from Google Books
            // but never override the title
            var finalAuthors: List<String>? = extractedAuthor?.let { listOf(it) }
            var imageUrl: String? = null
            var pageCount: Int? = null
            var description: String? = null
            var publisher: String? = null
            var publishedDate: String? = null
            var rating: Double? = null

            try {
                val searchResult = booksRepository.searchBooks(
                    query = fileNameWithoutExtension,
                    startIndex = 0,
                    maxResults = 1
                )

                if (searchResult is RequestState.Success) {
                    val firstBook = searchResult.data.items?.firstOrNull()
                    if (firstBook != null) {
                        // Only fetch supplementary data — title stays as filename
                        if (firstBook.volumeInfo.authors?.isNotEmpty() == true) {
                            finalAuthors = firstBook.volumeInfo.authors
                        }
                        imageUrl =
                            firstBook.volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")
                        pageCount = firstBook.volumeInfo.pageCount
                        description = firstBook.volumeInfo.description
                        publisher = firstBook.volumeInfo.publisher
                        publishedDate = firstBook.volumeInfo.publishedDate
                        rating = firstBook.volumeInfo.averageRating
                    }
                }
            } catch (e: Exception) {
                println("Failed to fetch book metadata from Google Books: ${e.message}")
            }

            // Generate unique book ID
            val bookId = "$fileName-${Clock.System.now().toEpochMilliseconds()}"

            // Save file data if available
            var fileStoragePath: String? = null
            if (fileBytes != null) {
                val saveResult = libraryRepository.saveBookFile(bookId, fileBytes)
                if (saveResult is RequestState.Success) {
                    fileStoragePath = saveResult.data
                }
            }

            // Determine file type
            val fileType = when {
                fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
                fileName.endsWith(".epub", ignoreCase = true) -> "epub"
                else -> "imported"
            }

            // Extract and save cover image from the file
            var hasLocalCover = false
            if (fileBytes != null) {
                try {
                    val coverBytes: ByteArray? = when (fileType) {
                        "pdf" -> {
                            val pdfRenderer = com.flipverse.shared.pdf.PdfRenderer()
                            val initialized = pdfRenderer.initialize(fileBytes)
                            if (initialized) {
                                val cover = pdfRenderer.getCoverImage()
                                pdfRenderer.close()
                                cover
                            } else null
                        }
                        "epub" -> {
                            val epubParser = com.flipverse.shared.epub.EpubParser()
                            val initialized = epubParser.initialize(fileBytes)
                            if (initialized) {
                                val cover = epubParser.getCoverImage()
                                epubParser.close()
                                cover
                            } else null
                        }
                        else -> null
                    }
                    if (coverBytes != null) {
                        val coverResult = libraryRepository.saveCoverImage(bookId, coverBytes)
                        hasLocalCover = coverResult is RequestState.Success
                    }
                } catch (e: Exception) {
                    println("Failed to extract cover image: ${e.message}")
                }
            }

            val newBook = LibraryBook(
                id = bookId,
                title = finalTitle,
                authors = finalAuthors,
                imageUrl = imageUrl,
                hasLocalCover = hasLocalCover,
                pageCount = pageCount,
                currentPage = null,
                rating = rating,
                publisher = publisher,
                publishedDate = publishedDate,
                description = description,
                readingStatus = ReadingStatus.WANT_TO_READ.name,
                dateAdded = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    .toString(),
                fileType = fileType,
                fileStoragePath = fileStoragePath
            )

            // Add book to repository
            libraryRepository.addBook(newBook)
        }
    }

    // PDF metadata extraction using PdfRenderer
    private suspend fun extractPdfMetadata(
        fileBytes: ByteArray,
        fallbackTitle: String
    ): Pair<String, String?> {
        return try {
            val pdfRenderer = com.flipverse.shared.pdf.PdfRenderer()
            val initialized = pdfRenderer.initialize(fileBytes)
            if (initialized) {
                // PdfRenderer doesn't expose title directly, use cleaned filename
                pdfRenderer.close()
            }
            cleanFileName(fallbackTitle) to null
        } catch (e: Exception) {
            println("Failed to extract PDF metadata: ${e.message}")
            cleanFileName(fallbackTitle) to null
        }
    }

    // EPUB metadata extraction using EpubParser
    private suspend fun extractEpubMetadata(
        fileBytes: ByteArray,
        fallbackTitle: String
    ): Pair<String, String?> {
        return try {
            val epubParser = com.flipverse.shared.epub.EpubParser()
            val initialized = epubParser.initialize(fileBytes)
            if (initialized) {
                val title = epubParser.getTitle()?.takeIf { it.isNotBlank() }
                val author = epubParser.getAuthors().firstOrNull()?.takeIf { it.isNotBlank() }
                epubParser.close()
                (title ?: cleanFileName(fallbackTitle)) to author
            } else {
                cleanFileName(fallbackTitle) to null
            }
        } catch (e: Exception) {
            println("Failed to extract EPUB metadata: ${e.message}")
            cleanFileName(fallbackTitle) to null
        }
    }

    /**
     * Clean a filename to produce a readable book title.
     * Removes common separators (underscores, hyphens, dots) and extra whitespace.
     */
    private fun cleanFileName(fileName: String): String {
        return fileName
            .replace("_", " ")
            .replace("-", " ")
            .replace(".", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { fileName }
    }

    fun moveBookToStatus(bookId: String, targetStatus: ReadingStatus) {
        viewModelScope.launch(exceptionHandler) {
            libraryRepository.updateReadingStatus(bookId, targetStatus)
        }
    }
}
