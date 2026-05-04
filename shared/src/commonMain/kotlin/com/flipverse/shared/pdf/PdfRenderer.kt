package com.flipverse.shared.pdf

/**
 * Platform-specific PDF renderer interface
 * Implementations:
 * - Android: Uses android.graphics.pdf.PdfRenderer + PdfBox-Android
 * - iOS: Uses PDFKit
 * - Desktop/JVM: Uses Apache PDFBox
 */
expect class PdfRenderer() {
    /**
     * Initialize the PDF renderer with file bytes
     */
    suspend fun initialize(fileBytes: ByteArray): Boolean

    /**
     * Get the total number of pages in the PDF
     */
    fun getPageCount(): Int

    /**
     * Extract text content from a specific page (0-indexed)
     */
    suspend fun getPageText(pageIndex: Int): String

    /**
     * Render the first page as a cover image (PNG bytes)
     */
    suspend fun getCoverImage(): ByteArray?

    /**
     * Release resources
     */
    fun close()
}

/**
 * Result class for PDF operations
 */
sealed class PdfResult<out T> {
    data class Success<T>(val data: T) : PdfResult<T>()
    data class Error(val message: String) : PdfResult<Nothing>()
}
