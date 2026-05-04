package com.flipverse.explore


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.flipverse.data.util.generateUserId
import com.flipverse.shared.Black
import com.flipverse.shared.BlackLight
import com.flipverse.shared.BlackLighter
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.ImageItem
import com.flipverse.shared.domain.LibraryBook
import com.flipverse.shared.domain.ReadingStatus
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.permissions.ExperimentalPermissionsApi
import com.mohamedrejeb.calf.permissions.Permission
import com.mohamedrejeb.calf.permissions.isGranted
import com.mohamedrejeb.calf.permissions.rememberPermissionState
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel


data class FabItem(val text: String, val icon: Painter, var onClick: () -> Unit)

data class PickedBookFile(val name: String, val bytes: ByteArray?)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    paddingValues: PaddingValues,
    onNavigateToBookDetails: (String) -> Unit = {},
    onNavigateToBookReader: (String) -> Unit = {},
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val currentlyReading by viewModel.currentlyReading.collectAsState()
    val wantToRead by viewModel.wantToRead.collectAsState()
    val alreadyRead by viewModel.alreadyRead.collectAsState()

    val context = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val storagePermission = rememberPermissionState(Permission.ReadStorage)
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showTooltip by remember { mutableStateOf(true) }

    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.All,
        selectionMode = FilePickerSelectionMode.Multiple,
        onResult = { files ->
            coroutineScope.launch {
                files.forEach { file ->
                    val rawName = file.getName(context).toString()
                    // Decode URL-encoded characters (e.g. %20 → space)
                    val decodedName = decodeUrlEncodedName(rawName)
                    val bytes = file.readByteArray(context)
                    viewModel.importBookFromFile(decodedName, bytes)
                }
            }
        }
    )

    val fabMenuItems = listOf(
        FabItem(
            Strings.import_book,
            painterResource(Resources.Icon.Recommendation)
        ) {
            isFabMenuExpanded = false
            coroutineScope.launch {
                println("I am clicked!")
                if (storagePermission.status.isGranted) {
                    filePickerLauncher.launch()
                } else {
                    pendingAction = { filePickerLauncher.launch() }
                    showPermissionDialog = true
                    storagePermission.launchPermissionRequest()
                }
//                val picked = pickBookFile()
//                if (picked != null) {
//                    viewModel.importBookFromFile(picked.name, picked.bytes)
//                    isFabMenuExpanded = false
//                }
            }
        },
    )

    Scaffold(
        floatingActionButton = {
            // Floating Action Button with expand/collapse functionality
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(
                    bottom = 96.dp,
                    end = 16.dp,
                ) // Position above bottom bar

            ) {
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        fabMenuItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.text,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { item.onClick() }
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = item.onClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.text,
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(
                        imageVector = if (isFabMenuExpanded) vectorResource(Resources.Icon.Close) else vectorResource(
                            Resources.Icon.Add
                        ),
                        contentDescription = if (isFabMenuExpanded) Strings.close else "Expand",
                        tint = Color.Black
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { innerPadding ->
        // Check if all categories are empty
        val allEmpty = currentlyReading is RequestState.Success &&
                wantToRead is RequestState.Success &&
                alreadyRead is RequestState.Success &&
                (currentlyReading as? RequestState.Success)?.data?.isEmpty() == true &&
                (wantToRead as? RequestState.Success)?.data?.isEmpty() == true &&
                (alreadyRead as? RequestState.Success)?.data?.isEmpty() == true

        if (allEmpty) {
            // Show empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .blur(if (isFabMenuExpanded) 12.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(Resources.Icon.BookClosed),
                    contentDescription = "Library Icon",
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.add_books_to_library,
                    textAlign = TextAlign.Center,
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        } else {
            // Show library categories
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .blur(if (isFabMenuExpanded) 12.dp else 0.dp)
            ) {
                // Tooltip item
                item {
                    if (showTooltip) {
                        CategoryTooltip(
                            onDismiss = { showTooltip = false }
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                }

                // Currently Reading Section
                item {
                    LibrarySection(
                        title = Strings.currently_reading,
                        state = currentlyReading,
                        onBookClick = onNavigateToBookReader, // Open reader for currently reading books
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Want to Read Section
                item {
                    LibrarySection(
                        title = Strings.want_to_read,
                        state = wantToRead,
                        onBookClick = onNavigateToBookDetails, // Show details for want to read books
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Already Read Section
                item {
                    LibrarySection(
                        title = Strings.already_read,
                        state = alreadyRead,
                        onBookClick = onNavigateToBookDetails, // Show details for already read books
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Extra bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.padding(64.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryTooltip(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(Resources.Icon.ToolTip),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Reading Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Books can only be read when they're in the \"Currently Reading\" category. Move books to this category to start reading them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Black.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Close),
                    contentDescription = Strings.close,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LibrarySection(
    title: String,
    state: RequestState<List<LibraryBook>>,
    onBookClick: (String) -> Unit,
    viewModel: LibraryViewModel
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state) {
            is RequestState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RequestState.Success -> {
                val books = state.data
                if (books.isEmpty()) {
                    EmptyLibraryCategory()
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(books) { book ->
                            LibraryBookCard(
                                book = book,
                                onClick = {
                                    // Only allow click if currently reading
                                    if (book.readingStatus == "CURRENTLY_READING") {
                                        onBookClick(book.id)
                                    }
                                },
                                onMoveStatus = { newStatus ->
                                    val enum = ReadingStatus.valueOf(newStatus)
                                    viewModel.moveBookToStatus(book.id, enum)
                                },
                                onDelete = {
                                    val status = ReadingStatus.valueOf(book.readingStatus)
                                    viewModel.removeBook(book.id, status)
                                }
                            )
                        }
                    }
                }
            }

            is RequestState.Error -> {
                ErrorState(state.message)
            }

            else -> {}
        }
    }
}

@Composable
private fun LibraryBookCard(
    book: LibraryBook,
    onClick: () -> Unit,
    onMoveStatus: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    // Only make card clickable if it's currently reading
    val isClickable = book.readingStatus == "CURRENTLY_READING"

    // Load local cover if available
    var localCoverBytes by remember { mutableStateOf<ByteArray?>(null) }
    if (book.hasLocalCover) {
        val libraryRepository: com.flipverse.data.domain.LibraryRepository = org.koin.compose.koinInject()
        androidx.compose.runtime.LaunchedEffect(book.id) {
            val result = libraryRepository.getCoverImage(book.id)
            if (result is RequestState.Success) {
                localCoverBytes = result.data
            }
        }
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .then(
                if (isClickable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .alpha(if (isClickable) 1f else 0.7f), // Slightly fade non-clickable books
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .width(120.dp)
                .height(180.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (localCoverBytes != null) {
                    // Display locally extracted cover image
                    Image(
                        painter = rememberAsyncImagePainter(localCoverBytes),
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (book.imageUrl.isNullOrEmpty()) {
                    Icon(
                        painter = painterResource(Resources.Icon.BookClosed),
                        contentDescription = "No image",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val painter = rememberAsyncImagePainter(book.imageUrl)
                    val painterState by painter.state.collectAsState()

                    Image(
                        painter = painter,
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (painterState is AsyncImagePainter.State.Error) {
                        Icon(
                            painter = painterResource(Resources.Icon.BookClosed),
                            contentDescription = "Image failed to load",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Progress indicator for currently reading books
                if ((book.currentPage ?: 0) > 0 && (book.pageCount ?: 0) > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val percent = ((book.currentPage ?: 0).toFloat() / (book.pageCount
                            ?: 1) * 100).toInt()
                        Text(
                            text = "$percent%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            book.title,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier.height(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val authors = book.authors
            if (!authors.isNullOrEmpty()) {
                authors.firstOrNull()?.let { author ->
                    Text(
                        author,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Show rating if available
        book.rating?.let { rating ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Resources.Icon.Review),
                    contentDescription = "Rating",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${rating}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action buttons row
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Status change button
            when (book.readingStatus) {
                "WANT_TO_READ" -> {
                    ActionButton(
                        label = "Start Reading",
                        modifier = Modifier.weight(1f)
                    ) { onMoveStatus("CURRENTLY_READING") }
                }

                "CURRENTLY_READING" -> {
                    ActionButton(
                        label = "Mark as Read",
                        modifier = Modifier.weight(1f)
                    ) { onMoveStatus("ALREADY_READ") }
                }

                "ALREADY_READ" -> {
                    ActionButton(
                        label = "Read Again",
                        modifier = Modifier.weight(1f)
                    ) { onMoveStatus("WANT_TO_READ") }
                }
            }

            // Delete button
            DeleteButton(onClick = onDelete)
        }
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = BlackLight,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            softWrap = false
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vectorResource(Resources.Icon.Delete),
            contentDescription = "Delete",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun EmptyLibraryCategory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Resources.Icon.BookClosed),
                contentDescription = "Empty category",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.no_books_in_category,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Decodes URL-encoded characters in a file name (e.g. %20 → space, %28 → '(').
 */
private fun decodeUrlEncodedName(name: String): String {
    return name.replace(Regex("%[0-9A-Fa-f]{2}")) { match ->
        val charCode = match.value.substring(1).toInt(16)
        charCode.toChar().toString()
    }
}
