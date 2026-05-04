package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.auth.AuthAction
import com.flipverse.auth.AuthEvent
import com.flipverse.auth.AuthViewModel
import com.flipverse.shared.Black
import com.flipverse.shared.BlackLight
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.DisplayResult
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.PreferencesRepository.saveFlipAccounts
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.WorkSansFont
import com.flipverse.shared.domain.FlipNomenclatures
import com.flipverse.shared.presentation.component.InfoCard
import com.flipverse.shared.presentation.component.LoadingCard
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState


data class SuggestedFlipAccounts(
    val id: String,
    val name: String,
    val author: String,
    val logoResId: DrawableResource,
    val category: String? = "",
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreen(
    navigateToDashboard: () -> Unit
) {
    val categories =
        listOf(
            "For You",
        )
    var selectedCategory by rememberSaveable { mutableStateOf("For You") }


    val suggestedFlipAccounts = remember {
        mutableStateListOf(
            SuggestedFlipAccounts(
                "1",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Image.AppLogoWhite,
                "For You"
            ),
            SuggestedFlipAccounts(
                "2",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Image.AppLogoWhite,
                "For You"
            ),
            SuggestedFlipAccounts(
                "3",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Image.Google,
                "For You"
            ),
            SuggestedFlipAccounts(
                "4",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Image.AppLogoWhite,
                "For You"
            ),
            SuggestedFlipAccounts(
                "5",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Icon.BookClosed,
                "For You"
            ),
            SuggestedFlipAccounts(
                "6",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Icon.EditDocument,
                "For You"
            ),
            SuggestedFlipAccounts(
                "7",
                "The FlipVerse Post",
                "by FlipVerse",
                Resources.Image.AppLogoWhite,
                "For You"
            ),

            )
    }

    // Filtered list of publications based on selected category
//    val filteredPublications = remember(selectedCategory) {
//        suggestedFlipAccounts.filter { it.category == selectedCategory }
//    }.toMutableStateList()

    val viewModel = koinViewModel<AuthViewModel>()
    val screenState = viewModel.authState
    val screenReady = viewModel.screenReady
    val onAction = viewModel::onAction
    val messageBarState = rememberMessageBarState()
    val selected by rememberSaveable { mutableStateOf(false) }

    val accountsList = viewModel.suggestedFlipAccounts.value.toMutableList()
    val selectedList = remember { mutableStateListOf<FlipNomenclatures.SuggestedFlipAccounts>() }

//    val selectedList: MutableList<FlipNomenclatures.SuggestedFlipAccounts> =
//        mutableListOf()


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center

                    ) {
                        Image(
                            painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoFullDark else Resources.Image.AppLogoFullWhite),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .wrapContentSize()
                                .height(48.dp),

                            )
                    }
                },
                navigationIcon = {
                    // IconButton(onClick = { /* Handle back button click */ }) {
                    //     Icon(
                    //         imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    //         contentDescription = "Back",
                    //         tint = Color.White
                    //     )
                    // }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary // Screen background color
    ) { paddingValues ->
        screenReady.DisplayResult(
            onLoading = { LoadingCard(modifier = Modifier.fillMaxSize()) },
            onError = { message ->
                InfoCard(
                    image = Resources.Image.AppLogoFullOutlineDark,
                    title = "Oops!",
                    subtitle = message
                )
            },
            onSuccess = {
                ContentWithMessageBar(
                    contentBackgroundColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    messageBarState = messageBarState,
                    fontFamily = FontFamily.SansSerif,
                    errorMaxLines = 2,
                    errorContainerColor = MaterialTheme.colorScheme.error,
                    errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    successContentColor = MaterialTheme.colorScheme.onPrimary,
                    showCopyButton = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
//                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Suggestions",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 30.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState(0))
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                CategoryChip(
                                    text = category,
                                    isSelected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Select all suggestions",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = WorkSansFont(),
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // List of Publications
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Makes the list fill available space
                        ) {
                            items(viewModel.suggestedFlipAccounts.value.toList()) { publication ->
                                val isSelected = selectedList.contains(publication)
                                PublicationItem(
                                    isSelected = isSelected,
                                    publication = publication,
                                    onClick = {
                                        if (isSelected) {
                                            selectedList.remove(publication)
                                        } else {
                                            selectedList.add(publication)
                                            viewModel.toggleAccountSelection(publication.name)
//
                                        }
                                    }
                                )
                            }
                        }


                        val isButtonEnabled = selectedList.size >= 1
                        // Done Button
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(
                                    horizontal = 24.dp,
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(size = 99.dp)
                                ), // Add padding for bottom and sides
                            onClick = {
                                val selected = viewModel.getSelectedFlipAccounts()
                                println("Selected Accounts: ${selected.map { it.name }}")
                                saveFlipAccounts(selected.map { it.name })
                                viewModel.updateUser(
                                    onSuccess = { navigateToDashboard() },
                                    onError = { message ->
                                        println("message-> $message")
                                        messageBarState.addError(message)
                                    },
                                )
                            },
                            enabled = isButtonEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {

                            AnimatedVisibility(
                                visible = !screenState.isLoading
                            ) {
                                Text(
                                    "Done",
                                    color = BlackLight,
                                    fontSize = 18.sp,
                                    fontFamily = WorkSansBoldFont()
                                )
                            }
                            AnimatedVisibility(
                                visible = screenState.isLoading
                            ) {
                                AdaptiveCircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = BlackLight
                                )

//                                CircularProgressIndicator(
//                                    modifier = Modifier.size(24.dp),
//                                    strokeWidth = 2.dp,
//                                    color = MaterialTheme.colorScheme.onSecondary
//                                )
                            }


                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        )

    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface // White if selected, dark gray otherwise
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(99.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (text == "For You") { // Special case for "For You" chip icon
            Icon(
                painter = painterResource(Resources.Icon.Person),
                contentDescription = null,
                tint = if (isSystemInDarkTheme()) Color(0xFF007FFF) else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = WorkSansFont()
        )
    }
}

@Composable
fun PublicationItem(
    publication: FlipNomenclatures.SuggestedFlipAccounts,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 24.dp), // Padding for each item
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Resources.Image.AppLogoWhite),//todo:work on images from firesbase as wwll
                contentDescription = null, // Content description for the  logo
                modifier = Modifier
                    .size(48.dp) // Size of the  logo
                    .clip(RoundedCornerShape(8.dp)) // Slightly rounded corners for logos
                    .border(
                        width = 1.dp,
                        color = Color.Unspecified,
                        shape = RoundedCornerShape(size = 8.dp)
                    )
                    .background(Color.Unspecified),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = publication.name,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = publication.author,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // Checkmark or Add icon
        Box(
            modifier = Modifier
                .size(28.dp) // Size of the check/add icon container
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) // Orange if selected, dark gray if not
                .border(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.surface, // Orange border if selected, gray if not
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) vectorResource(Resources.Icon.Checkmark) else vectorResource(
                    Resources.Icon.Add
                ),
                contentDescription = if (isSelected) "Deselect" else "Select",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp) // Size of the icon inside the circle
            )
        }
    }
}

