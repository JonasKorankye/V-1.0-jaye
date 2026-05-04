package com.flipverse.shared.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer as AndroidPdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

actual class PdfRenderer actual constructor() {
    private var pdfRenderer: AndroidPdfRenderer? = null
    private var pdDocument: PDDocument? = null
    private var tempFile: File? = null
    private var pageCount: Int = 0
    private var isInitialized = false

    actual suspend fun initialize(fileBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            // Initialize PDFBox for Android
            // Note: Call PDFBoxResourceLoader.init(context) in Application.onCreate()

            // Create a temporary file to store PDF bytes
            tempFile = File.createTempFile("pdf_temp_", ".pdf").apply {
                deleteOnExit()
            }

            FileOutputStream(tempFile).use { output ->
                output.write(fileBytes)
            }

            // Load PDF with PDFBox for text extraction
            pdDocument = PDDocument.load(tempFile)
            pageCount = pdDocument?.numberOfPages ?: 0

            // Also initialize Android PdfRenderer for potential image rendering
            val fileDescriptor = ParcelFileDescriptor.open(
                tempFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            pdfRenderer = AndroidPdfRenderer(fileDescriptor)

            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Try to clean up partial initialization
            pdDocument?.close()
            pdfRenderer?.close()
            tempFile?.delete()
            false
        }
    }

    actual fun getPageCount(): Int = pageCount

    actual suspend fun getPageText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || pdDocument == null || pageIndex < 0 || pageIndex >= pageCount) {
                return@withContext "Invalid page index or PDF not initialized"
            }

            // Extract text using PDFBox
            val stripper = PDFTextStripper().apply {
                // PDFBox uses 1-based page numbers
                startPage = pageIndex + 1
                endPage = pageIndex + 1
            }

            val pageText = stripper.getText(pdDocument).trim()

            if (pageText.isEmpty()) {
                // Get page dimensions for info
                val page = pdfRenderer?.openPage(pageIndex)
                val width = page?.width ?: 0
                val height = page?.height ?: 0
                page?.close()

                """
                    [PDF Page ${pageIndex + 1}]
                    
                    This page appears to be empty or contains only images/graphics.
                    
                    Page dimensions: ${width}x${height}
                    
                    No text content could be extracted from this page.
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
            if (!isInitialized || pdfRenderer == null || pageCount == 0) {
                return@withContext null
            }
            val page = pdfRenderer!!.openPage(0)
            // Render at 2x scale for good quality cover
            val scale = 2
            val bitmap = Bitmap.createBitmap(
                page.width * scale,
                page.height * scale,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, AndroidPdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun close() {
        try {
            pdDocument?.close()
            pdfRenderer?.close()
            tempFile?.delete()
            isInitialized = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
