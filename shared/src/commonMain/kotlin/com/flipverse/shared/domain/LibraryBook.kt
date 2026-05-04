package com.flipverse.shared.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

enum class ReadingStatus {
    CURRENTLY_READING,
    WANT_TO_READ,
    ALREADY_READ
}

@Serializable
data class LibraryBook(
    val id: String,
    val title: String,
    val authors: List<String>? = null,
    val imageUrl: String? = null,
    val pageCount: Int? = null,
    val currentPage: Int? = null,
    val rating: Double? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val readingStatus: String, // CURRENTLY_READING, WANT_TO_READ, ALREADY_READ
    val dateAdded: String = Clock.System.now().toEpochMilliseconds().toString(),
    val lastReadDate: String? = null,
    val fileType: String? = null, // "pdf", "epub", "imported", "google_books"
    val fileStoragePath: String? = null, // Path to file in storage or base64 data for small files
    val hasLocalCover: Boolean = false,
    val bookmarks: List<Bookmark>? = null,
    val highlights: List<Highlight>? = null,
    val notes: List<Note>? = null
)

@Serializable
data class Bookmark(
    val id: String,
    val page: Int,
    val title: String? = null,
    val dateCreated: String = Clock.System.now().toEpochMilliseconds().toString()
)

@Serializable
data class Highlight(
    val id: String,
    val page: Int,
    val text: String,
    val color: String = "#FFFF00", // Yellow by default
    val dateCreated: String = Clock.System.now().toEpochMilliseconds().toString()
)

@Serializable
data class Note(
    val id: String,
    val page: Int,
    val content: String,
    val dateCreated: String = Clock.System.now().toEpochMilliseconds().toString(),
    val dateModified: String? = null
)

@Serializable
data class ReaderPreferences(
    val userId: String,
    val fontSize: Int = 16,
    val fontFamily: String = "default",
    val lineHeight: Float = 1.5f,
    val backgroundColor: String = "#FFFFFF",
    val textColor: String = "#000000",
    val brightness: Float = 1.0f,
    val nightMode: Boolean = false,
    val autoNightMode: Boolean = false
)

fun GoogleBookItem.toLibraryBook(readingStatus: ReadingStatus): LibraryBook {
    return LibraryBook(
        id = this.id,
        title = this.volumeInfo.title,
        authors = this.volumeInfo.authors,
        imageUrl = this.volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:"),
        pageCount = this.volumeInfo.pageCount,
        currentPage = if (readingStatus == ReadingStatus.CURRENTLY_READING) 0 else null,
        rating = this.volumeInfo.averageRating,
        publisher = this.volumeInfo.publisher,
        publishedDate = this.volumeInfo.publishedDate,
        description = this.volumeInfo.description,
        readingStatus = readingStatus.name,
        dateAdded = Clock.System.now().toEpochMilliseconds().toString(),
        fileType = "google_books"
    )
}
