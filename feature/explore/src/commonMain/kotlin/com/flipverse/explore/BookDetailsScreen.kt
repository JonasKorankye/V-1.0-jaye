package com.flipverse.explore

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Gray
import com.flipverse.shared.RequestState
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.GoogleBookItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import com.flipverse.shared.Resources
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import openWebBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    val viewModel: BookStoreViewModel = koinViewModel()
    val bookState by viewModel.selectedBook.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.loadBookDetails(bookId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(Strings.book_details) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when (val state = bookState) {
            is RequestState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    AdaptiveCircularProgressIndicator()
                }
            }

            is RequestState.Success -> {
                BookDetailsContent(
                    book = state.data,
                    modifier = Modifier.padding(paddingValues),
                    onAddToCart = {
                        viewModel.addToCart(state.data)
                        onNavigateToCart()
                    }
                )
            }

            is RequestState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Strings.error,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun BookDetailsContent(
    book: GoogleBookItem,
    modifier: Modifier = Modifier,
    onAddToCart: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(color = MaterialTheme.colorScheme.primary)
    ) {
        // Book Cover with placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = book.volumeInfo.imageLinks?.large
                    ?: book.volumeInfo.imageLinks?.medium
                    ?: book.volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")

                if (imageUrl.isNullOrEmpty()) {
                    // Show placeholder icon when image URL is empty
                    Icon(
                        painter = painterResource(Resources.Icon.BookClosed),
                        contentDescription = "No image",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val painter = rememberAsyncImagePainter(imageUrl)
                    val painterState by painter.state.collectAsState()

                    Image(
                        painter = painter,
                        contentDescription = book.volumeInfo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Show placeholder icon when image fails to load
                    if (painterState is coil3.compose.AsyncImagePainter.State.Error) {
                        Icon(
                            painter = painterResource(Resources.Icon.BookClosed),
                            contentDescription = "Image failed to load",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Title
            Text(
                book.volumeInfo.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            // Subtitle (if available)
            book.volumeInfo.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    subtitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Authors
            book.volumeInfo.authors?.let { authors ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Strings.by} ",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        authors.joinToString(", "),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Publisher and Published Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                book.volumeInfo.publisher?.let { publisher ->
                    Text(
                        publisher,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                book.volumeInfo.publishedDate?.let { date ->
                    Text(
                        "${Strings.published}: $date",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating and Page Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                book.volumeInfo.averageRating?.let { rating ->
                    Column {
                        Text(
                            Strings.rating,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            "$rating / 5.0",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        book.volumeInfo.ratingsCount?.let { count ->
                            Text(
                                "($count ${Strings.ratings})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                book.volumeInfo.pageCount?.let { pageCount ->
                    Column {
                        Text(
                            Strings.pages,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            pageCount.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Price and Buy Section
            val saleInfo = book.saleInfo
            val retailPrice = saleInfo?.retailPrice
            val priceAmount = retailPrice?.amount
            val buyLink = saleInfo?.buyLink

            if (priceAmount != null || buyLink != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            Strings.price,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val priceText = when {
                            priceAmount != null -> "${retailPrice?.currencyCode ?: "USD"} $priceAmount"
                            saleInfo?.saleability == "FREE" -> "Free"
                            else -> "View on Google Books"
                        }

                        Text(
                            priceText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (priceText == "Free")
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Buy Now Button (if buy link available)
            if (buyLink != null) {
                Button(
                    onClick = {
                        // Note: You'll need to pass a callback to open the URL
                        openWebBrowser(buyLink)

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = BlackLight
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (priceAmount != null && priceAmount > 0) "Buy Now - $${priceAmount}"
                            else if (saleInfo?.saleability == "FREE") "Get Free Book"
                            else "View on Google Books",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            } else {
                // Fallback to Google Books search if no buy link
                val searchUrl = "https://www.google.com/search?tbm=bks&q=${
                    book.volumeInfo.title.replace(
                        " ",
                        "+"
                    )
                }"
//                            onOpenAffiliateLink(searchUrl)
                openWebBrowser(searchUrl)
            }

            // Add to Cart Button
//            Button(
//                onClick = onAddToCart,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp),
//                shape = RoundedCornerShape(8.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
//                    contentColor = BlackLight
//                )
//            ) {
//                Text(
//                    Strings.add_to_cart,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(
                Strings.book_info,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                book.volumeInfo.description ?: Strings.no_description,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Justify
            )

            // Categories
            book.volumeInfo.categories?.let { categories ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    categories.joinToString(" • "),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.padding(64.dp))
        }
    }
}
