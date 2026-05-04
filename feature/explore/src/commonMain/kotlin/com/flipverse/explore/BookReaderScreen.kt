package com.flipverse.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.data.domain.LibraryRepository
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.Bookmark
import com.flipverse.shared.domain.LibraryBook
import com.flipverse.shared.domain.ReaderPreferences
import com.flipverse.shared.epub.EpubParser
import com.flipverse.shared.pdf.PdfRenderer
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    bookId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val libraryRepository: LibraryRepository = koinInject()
    val currentlyReading by viewModel.currentlyReading.collectAsState()
    val wantToRead by viewModel.wantToRead.collectAsState()
    val alreadyRead by viewModel.alreadyRead.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = com.mohamedrejeb.calf.core.LocalPlatformContext.current

    // Find the book from all categories
    val book = remember(currentlyReading, wantToRead, alreadyRead) {
        val allBooks = (currentlyReading as? RequestState.Success)?.data.orEmpty() +
                (wantToRead as? RequestState.Success)?.data.orEmpty() +
                (alreadyRead as? RequestState.Success)?.data.orEmpty()
        allBooks.firstOrNull { it.id == bookId }
    }

    var currentPage by remember { mutableStateOf(book?.currentPage ?: 1) }
    val totalPages = book?.pageCount ?: 100

    // Reader preferences
    var readerPreferences by remember { mutableStateOf<ReaderPreferences?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var showBookmarksMenu by remember { mutableStateOf(false) }

    // Book file data
    var bookFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var bookFileLoadError by remember { mutableStateOf<String?>(null) }
    var isLoadingFile by remember { mutableStateOf(false) }

    // EPUB-specific state
    var epubChapterCount by remember { mutableStateOf<Int?>(null) }
    var epubTableOfContents by remember {
        mutableStateOf<List<com.flipverse.shared.epub.TocItem>>(
            emptyList()
        )
    }

    // PDF-specific state
    var pdfPageCount by remember { mutableStateOf<Int?>(null) }

    // Determine if this is an EPUB book
    val isEpubBook = book?.fileType == "epub"
    val pageLabel = if (isEpubBook) "Chapter" else "Page"
    val actualTotalPages = epubChapterCount ?: pdfPageCount ?: totalPages

    // File picker for reimporting book
    val filePickerLauncher = com.mohamedrejeb.calf.picker.rememberFilePickerLauncher(
        type = com.mohamedrejeb.calf.picker.FilePickerFileType.All,
        selectionMode = com.mohamedrejeb.calf.picker.FilePickerSelectionMode.Single,
        onResult = { files ->
            files.firstOrNull()?.let { file ->
                coroutineScope.launch {
                    try {
                        isLoadingFile = true
                        bookFileLoadError = null
                        val fileName = file.getName(context)
                        val fileBytes = file.readByteArray(context)
                        // Save the new file
                        val saveResult = libraryRepository.saveBookFile(bookId, fileBytes)
                        if (saveResult is RequestState.Success) {
                            // Retrieve the latest book, update its fileStoragePath and update
                            val currentBook = libraryRepository.getBook(bookId)
                            if (currentBook is RequestState.Success) {
                                val updatedBook =
                                    currentBook.data.copy(fileStoragePath = saveResult.data)
                                libraryRepository.updateBook(updatedBook)
                                bookFileBytes = fileBytes
                                bookFileLoadError = null
                                println("Book file reimported successfully")
                            } else {
                                bookFileLoadError = "Failed to get book to update file path."
                            }
                        } else if (saveResult is RequestState.Error) {
                            bookFileLoadError = "Failed to save file: ${saveResult.message}"
                        }
                    } catch (e: Exception) {
                        bookFileLoadError = "Failed to import file: ${e.message}"
                    } finally {
                        isLoadingFile = false
                    }
                }
            }
        }
    )

    // Load reader preferences
    LaunchedEffect(Unit) {
        val userId = PreferencesRepository.getId()
        val prefsResult = libraryRepository.getReaderPreferences(userId)
        if (prefsResult is RequestState.Success) {
            readerPreferences = prefsResult.data
        }
    }

    // Load book file if it's an imported book
    LaunchedEffect(book?.id, book?.fileStoragePath) {
        if (book != null && book.fileStoragePath != null && book.fileType in listOf(
                "pdf",
                "epub",
                "imported"
            )
        ) {
            isLoadingFile = true
            val fileResult = libraryRepository.getBookFile(book.id)
            when (fileResult) {
                is RequestState.Success -> {
                    bookFileBytes = fileResult.data
                    bookFileLoadError = null
                }

                is RequestState.Error -> {
                    bookFileLoadError = fileResult.message
                    bookFileBytes = null
                }

                else -> {}
            }
            isLoadingFile = false
        }
    }

    // Load EPUB metadata if it's an EPUB file
    LaunchedEffect(bookFileBytes, book?.fileType) {
        if (bookFileBytes != null && book?.fileType == "epub") {
            try {
                val epubParser = EpubParser()
                val initialized = epubParser.initialize(bookFileBytes!!)
                if (initialized) {
                    epubChapterCount = epubParser.getChapterCount()
                    epubTableOfContents = epubParser.getTableOfContents()
                    epubParser.close()
                }
            } catch (e: Exception) {
                println("Error loading EPUB metadata: ${e.message}")
            }
        }
    }

    // Load PDF page count if it's a PDF file
    LaunchedEffect(bookFileBytes, book?.fileType) {
        if (bookFileBytes != null && book?.fileType == "pdf") {
            try {
                val pdfRenderer = PdfRenderer()
                val initialized = pdfRenderer.initialize(bookFileBytes!!)
                if (initialized) {
                    pdfPageCount = pdfRenderer.getPageCount()
                    pdfRenderer.close()
                }
            } catch (e: Exception) {
                println("Error loading PDF metadata: ${e.message}")
            }
        }
    }

    // Clamp currentPage when actual page count becomes known
    LaunchedEffect(actualTotalPages) {
        if (currentPage > actualTotalPages) {
            currentPage = actualTotalPages.coerceAtLeast(1)
        }
    }

    // Save progress when page changes
    LaunchedEffect(currentPage) {
        if (book != null && currentPage != book.currentPage) {
            libraryRepository.updateReadingProgress(bookId, currentPage)
        }
    }

    val backgroundColor = if (readerPreferences?.nightMode == true) {
        Color(0xFF1A1A1A)
    } else {
        MaterialTheme.colorScheme.background
    }

    val textColor = if (readerPreferences?.nightMode == true) {
        Color(0xFFE0E0E0)
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = book?.title ?: "Book Reader",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        book?.authors?.firstOrNull()?.let { author ->
                            Text(
                                text = author,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Bookmarks menu
                    Box {
                        IconButton(onClick = { showBookmarksMenu = !showBookmarksMenu }) {
                            Icon(
                                imageResource(Resources.Icon.Bookmark),
                                contentDescription = "Bookmarks",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showBookmarksMenu,
                            onDismissRequest = { showBookmarksMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Bookmark",color = MaterialTheme.colorScheme.onPrimary) },
                                onClick = {
                                    showBookmarkDialog = true
                                    showBookmarksMenu = false
                                }
                            )

                            // Show EPUB Table of Contents if available
                            if (isEpubBook && epubTableOfContents.isNotEmpty()) {
                                // Separator text
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Table of Contents",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    onClick = { },
                                    enabled = false
                                )

                                epubTableOfContents.forEach { tocItem ->
                                    DropdownMenuItem(
                                        text = { Text(tocItem.title, color = MaterialTheme.colorScheme.onPrimary) },
                                        onClick = {
                                            currentPage = tocItem.chapterIndex + 1
                                            showBookmarksMenu = false
                                        }
                                    )
                                }
                            }

                            // Show bookmarks if any
                            if (!book?.bookmarks.isNullOrEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Bookmarks",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                            }

                            book?.bookmarks?.forEach { bookmark ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${bookmark.title ?: "Page ${bookmark.page}"} (p.${bookmark.page})",
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    onClick = {
                                        currentPage = bookmark.page
                                        showBookmarksMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Settings button
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            vectorResource(Resources.Icon.Settings),
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // Reading progress bar and controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(bottom = 120.dp)
            ) {
                // Progress indicator
                val progress =
                    if (actualTotalPages > 0) currentPage.toFloat() / actualTotalPages else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$pageLabel $currentPage of $actualTotalPages",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page navigation controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPage > 1) {
                                currentPage--
                            }
                        },
                        enabled = currentPage > 1
                    ) {
                        Icon(
                            vectorResource(Resources.Icon.BackArrow),
                            contentDescription = "Previous Page",
                            tint = if (currentPage > 1) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                        )
                    }

                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { currentPage = it.toInt() },
                        valueRange = 1f..actualTotalPages.toFloat(),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (currentPage < actualTotalPages) {
                                currentPage++
                            }
                        },
                        enabled = currentPage < actualTotalPages
                    ) {
                        Icon(
                            vectorResource(Resources.Icon.ArrowRight),
                            contentDescription = "Next Page",
                            tint = if (currentPage < actualTotalPages) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading book...",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        } else {
            // Book content area
            BookContentView(
                book = book,
                currentPage = currentPage,
                textColor = MaterialTheme.colorScheme.onPrimary,
                fontSize = readerPreferences?.fontSize ?: 16,
                bookFileBytes = bookFileBytes,
                bookFileLoadError = bookFileLoadError,
                isLoadingFile = isLoadingFile,
                isEpubBook = isEpubBook,
                chapterTitle = epubTableOfContents.find { it.chapterIndex == currentPage - 1 }?.title,
                filePickerLauncher = filePickerLauncher,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }

        // Settings Dialog
        if (showSettingsDialog && readerPreferences != null) {
            ReaderSettingsDialog(
                preferences = readerPreferences!!,
                onDismiss = { showSettingsDialog = false },
                onSave = { newPrefs ->
                    coroutineScope.launch {
                        libraryRepository.updateReaderPreferences(newPrefs)
                        readerPreferences = newPrefs
                        showSettingsDialog = false
                    }
                }
            )
        }

        // Bookmark Dialog
        if (showBookmarkDialog) {
            AddBookmarkDialog(
                currentPage = currentPage,
                onDismiss = { showBookmarkDialog = false },
                onSave = { title ->
                    coroutineScope.launch {
                        val bookmark = Bookmark(
                            id = "${bookId}_bookmark_${Clock.System.now().toEpochMilliseconds()}",
                            page = currentPage,
                            title = title
                        )
                        libraryRepository.addBookmark(bookId, bookmark)
                        showBookmarkDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun BookContentView(
    book: LibraryBook,
    currentPage: Int,
    textColor: Color,
    fontSize: Int,
    bookFileBytes: ByteArray?,
    bookFileLoadError: String?,
    isLoadingFile: Boolean,
    isEpubBook: Boolean,
    chapterTitle: String?,
    filePickerLauncher: com.mohamedrejeb.calf.picker.FilePickerLauncher,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Extract content for current page
    var pageContent by remember { mutableStateOf<String?>(null) }
    var isExtractingContent by remember { mutableStateOf(false) }

    LaunchedEffect(bookFileBytes, currentPage, book.fileType) {
        if (bookFileBytes != null) {
            isExtractingContent = true
            pageContent =
                extractPageContent(bookFileBytes, currentPage, book.fileType, book.pageCount)
            isExtractingContent = false
        } else {
            pageContent = null
        }
    }

    // Scroll to top when page/chapter changes
    LaunchedEffect(currentPage) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = modifier
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (isEpubBook) {
                Text(
                    text = chapterTitle ?: "Chapter $currentPage",
                    fontSize = (fontSize + 8).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Text(
                    text = "Book Reader - Page $currentPage",
                    fontSize = (fontSize + 8).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isLoadingFile || isExtractingContent) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (isLoadingFile) {
                                when (book.fileType) {
                                    "epub" -> "Loading EPUB book..."
                                    "pdf" -> "Loading PDF document..."
                                    else -> "Loading book content..."
                                }
                            } else {
                                when (book.fileType) {
                                    "epub" -> "Extracting chapter..."
                                    "pdf" -> "Extracting page..."
                                    else -> "Extracting content..."
                                }
                            },
                            color = textColor
                        )
                    }
                }
            } else if (bookFileLoadError != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Warning),
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "Error loading book file:",
                        fontSize = (fontSize + 2).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = bookFileLoadError!!,
                        fontSize = fontSize.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The book file couldn't be loaded. This may be due to a corrupted file or storage issue.",
                        fontSize = (fontSize - 2).sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { filePickerLauncher.launch() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.Refresh),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reimport Book File",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Select the book file again to restore reading access",
                        fontSize = (fontSize - 2).sp,
                        color = textColor.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else if (pageContent != null) {
                // Separate header, body, and footer content
                val sections = separatePageSections(pageContent!!)

                // Header section
                if (sections.header.isNotEmpty()) {
                    sections.header.forEach { line ->
                        Text(
                            text = line,
                            fontSize = (fontSize - 2).sp,
                            lineHeight = (fontSize * 1.4f).sp,
                            color = textColor.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Body section
                val paragraphs = sections.body.split("\n\n").filter { it.isNotBlank() }
                paragraphs.forEachIndexed { index, paragraph ->
                    Text(
                        text = paragraph.trim(),
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.8f).sp,
                        color = textColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (index < paragraphs.lastIndex) {
                        Spacer(modifier = Modifier.height((fontSize * 0.6f).dp))
                    }
                }

                // Footer section
                if (sections.footer.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    sections.footer.forEach { line ->
                        Text(
                            text = line,
                            fontSize = (fontSize - 2).sp,
                            lineHeight = (fontSize * 1.4f).sp,
                            color = textColor.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (book.fileType == "google_books") {
                // For Google Books, show description or preview
                val description = book.description
                if (description != null) {
                    Column {
                        Text(
                            text = "Preview",
                            fontSize = (fontSize + 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = description,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.8f).sp,
                            color = textColor,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Note: This is a Google Books item. The full content is not available for offline reading. Visit the book details to purchase or view online.",
                            fontSize = (fontSize - 2).sp,
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "This book is from Google Books catalog. Full content is not available for offline reading.\n\nTo read this book, please visit Google Books or purchase it from the bookstore.",
                        fontSize = fontSize.sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = """
                        No book content available.
                        
                        This book may not have been imported with file data.
                        
                        Current book information:
                        Title: ${book.title}
                        Authors: ${book.authors?.joinToString(", ") ?: "Unknown"}
                        File Type: ${book.fileType ?: "Unknown"}
                        
                        To read this book:
                        1. Import the book file again
                        2. Or purchase from the bookstore
                    """.trimIndent(),
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5f).sp,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/**
 * Extract text content for a specific page from book file bytes.
 * Uses platform-specific PDF renderers for actual text extraction.
 */
private suspend fun extractPageContent(
    fileBytes: ByteArray,
    page: Int,
    fileType: String?,
    totalPages: Int?
): String {
    return try {
        when (fileType) {
            "pdf" -> {
                // Use platform-specific PDF renderer
                val pdfRenderer = PdfRenderer()
                try {
                    val initialized = pdfRenderer.initialize(fileBytes)
                    if (!initialized) {
                        return """
                            Failed to initialize PDF renderer.
                            
                            Please ensure:
                            1. The PDF file is not corrupted
                            2. The PDF is not password-protected
                            3. The file format is valid
                            
                            File size: ${fileBytes.size} bytes
                        """.trimIndent()
                    }

                    val actualPageCount = pdfRenderer.getPageCount()
                    if (page > actualPageCount) {
                        pdfRenderer.close()
                        return "Page $page is out of bounds. This PDF has $actualPageCount pages."
                    }

                    // Extract text from page (0-indexed)
                    val rawText = pdfRenderer.getPageText(page - 1)
                    pdfRenderer.close()

                    // Post-process PDF text for better readability
                    formatPdfText(rawText)
                } catch (e: Exception) {
                    pdfRenderer.close()
                    "Error rendering PDF page: ${e.message}"
                }
            }

            "epub" -> {
                // Use EPUB parser
                val epubParser = EpubParser()
                try {
                    val initialized = epubParser.initialize(fileBytes)
                    if (!initialized) {
                        return """
                            Failed to initialize EPUB parser.
                            
                            Please ensure:
                            1. The EPUB file is not corrupted
                            2. The EPUB structure is valid
                            3. The file format is correct
                            
                            File size: ${fileBytes.size} bytes
                        """.trimIndent()
                    }

                    val chapterCount = epubParser.getChapterCount()
                    if (page > chapterCount) {
                        epubParser.close()
                        return "Chapter $page is out of bounds. This EPUB has $chapterCount chapters."
                    }

                    // Extract text from chapter (0-indexed)
                    // Note: For EPUB, we treat each chapter as a "page"
                    val text = epubParser.getChapterText(page - 1)
                    epubParser.close()

                    text
                } catch (e: Exception) {
                    epubParser.close()
                    "Error rendering EPUB chapter: ${e.message}"
                }
            }

            "txt", "imported" -> {
                // For plain text files, decode and split into pages
                val fullText = fileBytes.decodeToString()

                // Simple pagination: split by paragraphs and group into pages
                val paragraphs = fullText.split("\n\n")
                val paragraphsPerPage = 10
                val startIdx = (page - 1) * paragraphsPerPage
                val endIdx = minOf(startIdx + paragraphsPerPage, paragraphs.size)

                if (startIdx < paragraphs.size) {
                    paragraphs.subList(startIdx, endIdx).joinToString("\n\n")
                } else {
                    "End of book."
                }
            }

            else -> {
                // Unknown file type
                """
                    Unknown file type: $fileType
                    
                    Cannot display content for this file type.
                    File size: ${fileBytes.size} bytes
                """.trimIndent()
            }
        }
    } catch (e: Exception) {
        "Error extracting content: ${e.message}"
    }
}

/**
 * Represents the three logical sections of a page: header, body, and footer.
 */
private data class PageSections(
    val header: List<String>,
    val body: String,
    val footer: List<String>
)

/**
 * Separate page content into header, body, and footer sections.
 *
 * Header lines: short lines at the very top that look like running headers
 * (book title, chapter name, etc.) — typically short and not sentence-like.
 *
 * Footer lines: short lines at the very bottom that look like page numbers,
 * footnotes markers, or running footers.
 *
 * Everything in between is body text.
 */
private fun separatePageSections(content: String): PageSections {
    val paragraphs = content.split("\n\n").filter { it.isNotBlank() }

    if (paragraphs.size <= 2) {
        // Too little content to separate — treat it all as body
        return PageSections(header = emptyList(), body = content.trim(), footer = emptyList())
    }

    val headerLines = mutableListOf<String>()
    val footerLines = mutableListOf<String>()

    // Detect header: check first 1-2 paragraphs
    // A header paragraph is typically short (< 80 chars), doesn't end with sentence punctuation,
    // and is not a full paragraph of body text
    for (i in 0 until minOf(2, paragraphs.size)) {
        val p = paragraphs[i].trim()
        if (isLikelyHeaderOrFooter(p)) {
            headerLines.add(p)
        } else {
            break
        }
    }

    // Detect footer: check last 1-2 paragraphs (only from those not already claimed by header)
    val remainingStart = headerLines.size
    val remainingParagraphs = paragraphs.subList(remainingStart, paragraphs.size)

    if (remainingParagraphs.size > 1) {
        // Check from the end
        for (i in remainingParagraphs.indices.reversed()) {
            val p = remainingParagraphs[i].trim()
            if (isLikelyHeaderOrFooter(p) || isLikelyPageNumber(p)) {
                footerLines.add(0, p)
            } else {
                break
            }
            if (footerLines.size >= 2) break
        }
    }

    // Body is everything between header and footer
    val bodyStart = headerLines.size
    val bodyEnd = paragraphs.size - footerLines.size
    val bodyParagraphs = if (bodyStart < bodyEnd) {
        paragraphs.subList(bodyStart, bodyEnd)
    } else {
        paragraphs
    }

    // If we accidentally claimed everything as header/footer, reset
    if (bodyParagraphs.isEmpty()) {
        return PageSections(header = emptyList(), body = content.trim(), footer = emptyList())
    }

    return PageSections(
        header = headerLines,
        body = bodyParagraphs.joinToString("\n\n"),
        footer = footerLines
    )
}

/**
 * Heuristic: a short line that doesn't look like a body paragraph.
 * Typical running headers/footers are short, don't end with sentence-ending punctuation,
 * and often contain title-like text or numbers.
 */
private fun isLikelyHeaderOrFooter(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.length > 80) return false  // Too long to be a header/footer
    if (trimmed.isEmpty()) return false
    // Single line, no paragraph structure
    if (trimmed.contains("\n")) return false
    // Doesn't end like a sentence (body text usually ends with . ! ? etc.)
    val lastChar = trimmed.last()
    if (lastChar in listOf('.', '!', '?', '"', '\u201D') && trimmed.length > 30) return false
    // Likely a header/footer
    return true
}

/**
 * Check if text looks like a standalone page number.
 */
private fun isLikelyPageNumber(text: String): Boolean {
    val trimmed = text.trim()
    // Pure number
    if (trimmed.all { it.isDigit() }) return true
    // Common patterns: "Page 5", "- 5 -", "5 of 100", roman numerals
    if (trimmed.matches(Regex("""^(Page\s*)?\d+(\s*of\s*\d+)?$""", RegexOption.IGNORE_CASE))) return true
    if (trimmed.matches(Regex("""^-?\s*\d+\s*-?$"""))) return true
    if (trimmed.matches(Regex("""^[ivxlcdm]+$""", RegexOption.IGNORE_CASE)) && trimmed.length <= 8) return true
    return false
}

/**
 * Post-process raw PDF text for better readability.
 * PDF extractors often produce text with inconsistent line breaks.
 */
private fun formatPdfText(rawText: String): String {
    val lines = rawText.lines()
    val result = StringBuilder()
    var previousLineEmpty = false

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            // Empty line = paragraph break
            if (!previousLineEmpty) {
                result.append("\n\n")
                previousLineEmpty = true
            }
        } else {
            // If the previous non-empty line ended without sentence-ending punctuation
            // and this line starts with a lowercase letter, it's likely a continuation
            if (result.isNotEmpty() && !previousLineEmpty) {
                val lastChar = result.lastOrNull { it != ' ' && it != '\n' }
                val startsWithLower = trimmed.firstOrNull()?.isLowerCase() == true
                if (startsWithLower && lastChar != null && lastChar !in listOf('.', '!', '?', ':', ';', '\n')) {
                    // Continuation of the same paragraph — join with space
                    result.append(" ")
                } else if (lastChar != null && lastChar == '-') {
                    // Hyphenated word break — remove hyphen and join
                    result.deleteAt(result.length - 1)
                } else {
                    result.append(" ")
                }
            }
            result.append(trimmed)
            previousLineEmpty = false
        }
    }

    return result.toString().trim()
}

@Composable
private fun ReaderSettingsDialog(
    preferences: ReaderPreferences,
    onDismiss: () -> Unit,
    onSave: (ReaderPreferences) -> Unit
) {
    var fontSize by remember { mutableStateOf(preferences.fontSize) }
    var nightMode by remember { mutableStateOf(preferences.nightMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reader Settings") },
        text = {
            Column {
                Text("Font Size: $fontSize", fontWeight = FontWeight.Medium)
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { fontSize = it.toInt() },
                    valueRange = 12f..24f,
                    steps = 11
                )

                Spacer(modifier = Modifier.height(16.dp))

//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text("Night Mode")
//                    Switch(
//                        checked = nightMode,
//                        onCheckedChange = { nightMode = it }
//                    )
//                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        preferences.copy(
                            fontSize = fontSize,
                            nightMode = nightMode
                        )
                    )
                }
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    )
}

@Composable
private fun AddBookmarkDialog(
    currentPage: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var bookmarkTitle by remember { mutableStateOf("Page $currentPage") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark") },
        text = {
            Column {
                Text("Page: $currentPage")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = bookmarkTitle,
                    onValueChange = { bookmarkTitle = it },
                    label = { Text("Bookmark Title") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(bookmarkTitle) }) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    )
}
