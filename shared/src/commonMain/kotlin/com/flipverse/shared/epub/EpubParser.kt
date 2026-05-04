package com.flipverse.shared.epub

/**
 * EPUB parser for extracting content from EPUB files
 * EPUB is a ZIP archive containing XHTML documents
 */
expect class EpubParser() {
    /**
     * Initialize the parser with EPUB file bytes
     */
    suspend fun initialize(fileBytes: ByteArray): Boolean

    /**
     * Get the book title from metadata
     */
    fun getTitle(): String?

    /**
     * Get the list of authors
     */
    fun getAuthors(): List<String>

    /**
     * Get total number of chapters
     */
    fun getChapterCount(): Int

    /**
     * Get chapter title by index
     */
    fun getChapterTitle(chapterIndex: Int): String?

    /**
     * Extract text content from a specific chapter (0-indexed)
     */
    suspend fun getChapterText(chapterIndex: Int): String

    /**
     * Get table of contents
     */
    fun getTableOfContents(): List<TocItem>

    /**
     * Extract the cover image bytes (PNG/JPEG) from the EPUB
     */
    fun getCoverImage(): ByteArray?

    /**
     * Release resources
     */
    fun close()
}

/**
 * Table of contents item
 */
data class TocItem(
    val title: String,
    val chapterIndex: Int
)

/**
 * EPUB metadata
 */
data class EpubMetadata(
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val language: String? = null,
    val description: String? = null
)
