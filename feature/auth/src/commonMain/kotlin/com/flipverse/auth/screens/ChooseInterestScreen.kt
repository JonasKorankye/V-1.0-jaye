package com.flipverse.auth.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.auth.AuthViewModel
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.DisplayResult
import com.flipverse.shared.PreferencesRepository.saveFlipInterests
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.WorkSansFont
import com.flipverse.shared.presentation.component.InfoCard
import com.flipverse.shared.presentation.component.LoadingCard
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseInterestsScreen(
    onBackClicked: () -> Unit,
    navigateToSuggestions: () -> Unit
) {

    val interests = listOf(
        "Mystery", "Thriller", "Fantasy",
        "Science", "Adventure", "Romance",
        "Horror", "Literature", "Suspense",
        "Comedy", "Drama",
        "Western", "Survival", "Psychological",
        "Detective", "Memoir", "Satire",
        "Supernatural", "Medical",
        "Fiction", "Non-fiction", "Tragedy",
        "Poetry", "History", "Short story", "Academic"
    )

    val viewModel = koinViewModel<AuthViewModel>()
    val authState = viewModel.authState
    val screenReady = viewModel.screenReady
    val onAction = viewModel::onAction

    val selectedInterests = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        contentAlignment = Alignment.Center
//
//                    ) {
//                        Image(
//                            painter = painterResource(Bookmark),
//                            contentDescription = "App Logo",
//                            modifier = Modifier
//                                .wrapContentSize()
//                                .height(36.dp),
//
//                        )
//                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClicked() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary// Screen background color
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
            onSuccess = { message ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .background(MaterialTheme.colorScheme.primary),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose your interests",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 30.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Select topics to personalize your experience",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Interests Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Make grid take available space
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.interests.value[0].name.toList()) { interest ->
                            val isSelected = selectedInterests.contains(interest)
                            InterestChip(
                                text = interest,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedInterests.remove(interest)
                                    } else {
                                        selectedInterests.add(interest)
                                        viewModel.toggleInterestSelection(interest)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bottom Button
                    val buttonText = when {
                        selectedInterests.size == 0 -> "Select 3 more to continue"
                        selectedInterests.size == 1 -> "Select 2 more to continue"
                        selectedInterests.size == 2 -> "Select 1 more to continue"
                        else -> "Continue"
                    }
                    val isButtonEnabled = selectedInterests.size >= 3


                    Button(
                        onClick = {
                            val selectedFlipInterests = viewModel.getSelectedInterests()
                            println("selected-Interests::" + selectedInterests.toList())
                            println("selectedFlipInterests::" + selectedFlipInterests.toList())
                            saveFlipInterests(selectedInterests.toList())
                            navigateToSuggestions()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(size = 99.dp)
                            ),
                        enabled = isButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.onSecondary.copy(
                                alpha = Alpha.HALF
                            )
                        )
                    ) {
                        Text(
                            text = buttonText,
                            fontFamily = FontFamily.SansSerif,
                            color = if (isButtonEnabled) BlackLight else MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                }
            },
        )

    }
}

@Composable
fun InterestChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface // Orange if selected, dark gray otherwise
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary // Text color is always white

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp)) // Rounded corners for chips
            .background(backgroundColor)
            .border(
                1.dp,
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp), // Padding inside the chip
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontFamily = WorkSansFont(),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
