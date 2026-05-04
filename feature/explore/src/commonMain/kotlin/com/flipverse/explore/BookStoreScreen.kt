package com.flipverse.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.GoogleBookItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Determines if a book should be displayed based on its pricing information.
 * Filters out books with 0.0 price or "Not for sale" status.
 */
private fun shouldDisplayBook(book: GoogleBookItem): Boolean {
    val saleInfo = book.saleInfo
    val retailPrice = saleInfo?.retailPrice
    val priceAmount = retailPrice?.amount

    // Check if price is 0.0
    if (priceAmount != null && priceAmount == 0.0) {
        return false
    }

    // Check if book is "Not for sale" (no retail price, not free, and no buy link)
    val isNotForSale = priceAmount == null &&
            saleInfo?.saleability != "FREE" &&
            saleInfo?.buyLink == null

    return !isNotForSale
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookStoreScreen(
    paddingValues: PaddingValues,
    onNavigateToBookDetails: (String) -> Unit,
    onNavigateToCart: () -> Unit
) {
    val viewModel: BookStoreViewModel = koinViewModel()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val bestSellers by viewModel.bestSellers.collectAsState()
    val featuredDeals by viewModel.featuredDeals.collectAsState()
    val mostWishedFor by viewModel.mostWishedFor.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val currentFeaturedCategory by viewModel.currentFeaturedCategory.collectAsState()
    val currentMostWishedCategory by viewModel.currentMostWishedCategory.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Pull to refresh handler
    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.refreshAllSections()
        isRefreshing = false
    }


    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Search Bar with Cart Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
//            TextField(
//                value = searchQuery,
//                onValueChange = { viewModel.updateSearchQuery(it) },
//                placeholder = {
//                    Text(
//                        Strings.search_books,
//                        color = MaterialTheme.colorScheme.onSecondary
//                    )
//                },
//                leadingIcon = {
//                    Icon(
//                        imageVector = vectorResource(Resources.Icon.Search),
//                        contentDescription = Strings.search_icon_cd,
//                        tint = MaterialTheme.colorScheme.onSecondary
//                    )
//                },
//                modifier = Modifier
//                    .weight(1f)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(MaterialTheme.colorScheme.surface),
//                colors = TextFieldDefaults.colors(
//                    focusedContainerColor = MaterialTheme.colorScheme.surface,
//                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                    disabledContainerColor = MaterialTheme.colorScheme.surface,
//                    focusedIndicatorColor = Color.Transparent,
//                    unfocusedIndicatorColor = Color.Transparent,
//                    disabledIndicatorColor = Color.Transparent,
//                    cursorColor = MaterialTheme.colorScheme.onPrimary,
//                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
//                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
//                ),
//                singleLine = true
//            )

            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onPrimary),
                singleLine = true,
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = searchQuery,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                        interactionSource = interactionSource,
                        placeholder = {
                            Text(
                                text = Strings.search_books,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Resources.Icon.Search),
                                contentDescription = Strings.search,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            )

//            Spacer(modifier = Modifier.width(8.dp))

            // Cart Icon with Badge
//            IconButton(onClick = onNavigateToCart) {
//                BadgedBox(
//                    badge = {
//                        if (cartItems.isNotEmpty()) {
//                            Badge(
//                                containerColor = MaterialTheme.colorScheme.error,
//                                modifier = Modifier.size(16.dp)
//                            ) {
//                                Text(
//                                    text = cartItems.size.toString(),
//                                    fontSize = 10.sp,
//                                    color = Color.White
//                                )
//                            }
//                        }
//                    }
//                ) {
//                    Icon(
//                        imageVector = vectorResource(Resources.Icon.ShoppingCart),
//                        contentDescription = Strings.cart,
//                        tint = MaterialTheme.colorScheme.onPrimary,
//                        modifier = Modifier.size(28.dp)
//                    )
//                }
//            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Content with Pull to Refresh
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Show search results if searching
                if (searchQuery.isNotEmpty()) {
                item {
                    when (val state = searchResults) {
                        is RequestState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is RequestState.Success -> {
                            val items = state.data.items
                            if (items.isNullOrEmpty()) {
                                // Show loading spinner - will trigger new search or retry
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                Column {
                                    Text(
                                        Strings.search_results,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        is RequestState.Error -> {
                            ErrorState(state.message)
                        }

                        else -> {}
                    }
                }

                if (searchResults is RequestState.Success) {
                    val successState = searchResults as RequestState.Success
                    val books = successState.data.items
                    if (books != null) {
                        items(books.filter { shouldDisplayBook(it) }) { book ->
                            BookListItem(
                                book = book,
                                onClick = { onNavigateToBookDetails(book.id) },
                                onAddToCart = { viewModel.addToCart(book) }
                            )
                        }
                    }
                }
            } else {
                // Show categories when not searching
                item {
                    BookSection(
                        title = Strings.best_sellers,
                        state = bestSellers,
                        onBookClick = onNavigateToBookDetails,
                        onAddToCart = { viewModel.addToCart(it) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    BookSection(
                        title = currentFeaturedCategory,
                        state = featuredDeals,
                        onBookClick = onNavigateToBookDetails,
                        onAddToCart = { viewModel.addToCart(it) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    BookSection(
                        title = currentMostWishedCategory,
                        state = mostWishedFor,
                        onBookClick = onNavigateToBookDetails,
                        onAddToCart = { viewModel.addToCart(it) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    Spacer(modifier = Modifier.padding(64.dp))
                }
            }
        }
        }
    }
}

@Composable
private fun BookSection(
    title: String,
    state: RequestState<com.flipverse.shared.domain.GoogleBooksResponse>,
    onBookClick: (String) -> Unit,
    onAddToCart: (GoogleBookItem) -> Unit
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
                var items = state.data.items?.filter { shouldDisplayBook(it) }
                // If filtering removes all books, show all items anyway (don't filter)
                if (items.isNullOrEmpty() && !state.data.items.isNullOrEmpty()) {
                    items = state.data.items
                }
                if (items.isNullOrEmpty()) {
                    // Show loading spinner instead of empty state - will retry automatically
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(items) { book ->
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book.id) },
                                onAddToCart = { onAddToCart(book) }
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
private fun BookCard(
    book: GoogleBookItem,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
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
                val imageUrl = book.volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")

                if (imageUrl.isNullOrEmpty()) {
                    // Show placeholder icon when image URL is empty
                    Icon(
                        painter = painterResource(Resources.Icon.BookClosed),
                        contentDescription = "No image",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val painter = rememberAsyncImagePainter(imageUrl)
                    val painterState by painter.state.collectAsState()

                    Image(
                        painter = painter,
                        contentDescription = book.volumeInfo.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Show placeholder icon when image fails to load
                    if (painterState is AsyncImagePainter.State.Error) {
                        Icon(
                            painter = painterResource(Resources.Icon.BookClosed),
                            contentDescription = "Image failed to load",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            book.volumeInfo.title,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        book.volumeInfo.authors?.firstOrNull()?.let { author ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                author,
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        // Price or Free/View text
        val saleInfo = book.saleInfo
        val retailPrice = saleInfo?.retailPrice
        val priceAmount = retailPrice?.amount

        val priceText = when {
            priceAmount != null -> {
                "${retailPrice?.currencyCode ?: "USD"} $priceAmount"
            }
            saleInfo?.saleability == "FREE" -> "Free"
            saleInfo?.buyLink != null -> "View on Google"
            else -> "Not for sale"
        }

        Text(
            priceText,
            color = if (priceText == "Free") MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onPrimary,
            fontSize = 13.sp,
            fontWeight = if (priceAmount != null) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun BookListItem(
    book: GoogleBookItem,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = book.volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")

                if (imageUrl.isNullOrEmpty()) {
                    // Show placeholder icon when image URL is empty
                    Icon(
                        painter = painterResource(Resources.Icon.BookClosed),
                        contentDescription = "No image",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val painter = rememberAsyncImagePainter(imageUrl)
                    val painterState by painter.state.collectAsState()

                    Image(
                        painter = painter,
                        contentDescription = book.volumeInfo.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Show placeholder icon when image fails to load
                    if (painterState is AsyncImagePainter.State.Error) {
                        Icon(
                            painter = painterResource(Resources.Icon.BookClosed),
                            contentDescription = "Image failed to load",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        book.volumeInfo.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    book.volumeInfo.authors?.let { authors ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            authors.joinToString(", "),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                book.volumeInfo.authors?.let { authors ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        authors.joinToString(", "),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Price or Free/View text
                val saleInfo = book.saleInfo
                val retailPrice = saleInfo?.retailPrice
                val priceAmount = retailPrice?.amount

                val priceText = when {
                    priceAmount != null -> {
                        "${retailPrice?.currencyCode ?: "USD"} $priceAmount"
                    }

                    saleInfo?.saleability == "FREE" -> "Free"
                    saleInfo?.buyLink != null -> "View on Google"
                    else -> "Not for sale"
                }

                Text(
                    priceText,
                    fontSize = 14.sp,
                    fontWeight = if (priceAmount != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (priceText == "Free") MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
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
