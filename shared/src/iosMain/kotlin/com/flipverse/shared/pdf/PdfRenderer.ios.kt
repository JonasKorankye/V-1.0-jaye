package com.flipverse.shared.pdf

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.UIKit.UIImagePNGRepresentation

@OptIn(ExperimentalForeignApi::class)
actual class PdfRenderer actual constructor() {
    private var pdfDocument: PDFDocument? = null
    private var pageCount: Int = 0

    actual suspend fun initialize(fileBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            // Convert ByteArray to NSData
            val nsData = fileBytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = fileBytes.size.toULong()
                )
            }

            pdfDocument = PDFDocument(nsData)
            pageCount = pdfDocument?.pageCount()?.toInt() ?: 0

            pdfDocument != null
        } catch (e: Exception) {
            println("Error initializing PDF: ${e.message}")
            false
        }
    }

    actual fun getPageCount(): Int = pageCount

    actual suspend fun getPageText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        try {
            val document = pdfDocument
            if (document == null || pageIndex < 0 || pageIndex >= pageCount) {
                return@withContext "Invalid page index or PDF not initialized"
            }

            val page = document.pageAtIndex(pageIndex.toULong())
            val pageText = page?.string() ?: ""

            if (pageText.isEmpty()) {
                """
                    [PDF Page ${pageIndex + 1}]
                    
                    This page appears to be empty or contains only images.
                    
                    Page loaded successfully using PDFKit!
                """.trimIndent()
            } else {
                pageText
            }
        } catch (e: Exception) {
            "Error extracting text from page ${pageIndex + 1}: ${e.message}"
        }
    }

    actual suspend fun getCoverImage(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val document = pdfDocument ?: return@withContext null
            if (pageCount == 0) return@withContext null

            val page = document.pageAtIndex(0u) ?: return@withContext null

            // Use PDFPage's thumbnailOfSize to render the page as an image
            val thumbnailSize = CGSizeMake(600.0, 800.0)
            val image = page.thumbnailOfSize(thumbnailSize, forBox = platform.PDFKit.kPDFDisplayBoxMediaBox)

            val pngData = UIImagePNGRepresentation(image) ?: return@withContext null

            val bytes = ByteArray(pngData.length.toInt())
            bytes.usePinned { pinned ->
                platform.posix.memcpy(
                    pinned.addressOf(0),
                    pngData.bytes,
                    pngData.length
                )
            }
            bytes
        } catch (e: Exception) {
            println("Error extracting PDF cover: ${e.message}")
            null
        }
    }

    actual fun close() {
        pdfDocument = null
    }
}
