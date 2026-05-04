//package com.flipverse.shared.presentation.component
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.unit.dp
//import coil3.compose.rememberAsyncImagePainter
//import coil3.request.ImageRequest
//import com.flipverse.shared.Black
//import com.flipverse.shared.BlackLight
//import com.flipverse.shared.Resources
//import com.mohamedrejeb.calf.core.LocalPlatformContext
//import com.mohamedrejeb.calf.io.readByteArray
//import com.mohamedrejeb.calf.picker.FilePickerFileType
//import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
//import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
//import kotlinx.coroutines.launch
//import org.jetbrains.compose.resources.painterResource
//import org.jetbrains.compose.resources.vectorResource
//
///**
// * A composable for picking and displaying a profile picture using Calf's file picker.
// *
// * @param onImagePicked A callback that provides the ByteArray of the picked image.
// * This ByteArray can then be uploaded to a server or saved locally.
// * @param  currentProfileImageUrl: String? = null An optional Painter to display the currently set profile image.
// * If null, a placeholder icon is shown.
// * @param isLoading A boolean indicating if an image upload/processing is in progress.
// */
//@Composable
//fun FlipProfilePicturePicker(
//    onImagePicked: (ByteArray) -> Unit,
//    currentProfileImageUrl: String? = null,
//    isLoading: Boolean = false
//) {
//    // State to hold the picked image's byte array for immediate display (optional)
//    var pickedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
//    val context = LocalPlatformContext.current
//    val scope = rememberCoroutineScope()
//
//    // Initialize the Calf file picker launcher
//    val pickerLauncher = rememberFilePickerLauncher(
//        type = FilePickerFileType.Image, // Specify that we want to pick image files
//        selectionMode = FilePickerSelectionMode.Single, // Allow only a single image to be picked
//        onResult = { files ->
//            scope.launch {
//                // This lambda is invoked when the user selects files
//                files.firstOrNull()?.let { file ->
//                    // Read the file's content as a ByteArray
//                    file.readByteArray(context).let { bytes ->
//                        pickedImageBytes = bytes // Update local state for display
//                        onImagePicked(bytes) // Pass the ByteArray to the callback for external handling
//                    }
//                }
//            }
//
//        }
//    )
//
//    Box(
//        modifier = Modifier.size(96.dp)
//            .clickable(enabled = !isLoading) { pickerLauncher.launch() },
//        contentAlignment = Alignment.BottomEnd
//    ) {
//        val imageModel = pickedImageBytes ?: currentProfileImageUrl
//
//        val painter = rememberAsyncImagePainter(
//            model = ImageRequest.Builder(LocalPlatformContext)
//                .data(imageModel)
//                .build()
//        )
//
//        if(imageModel != null) {
//            Image(
//                painter = painterResource(Resources.Icon.Person), // Placeholder user icon
//                contentDescription = "Profile Picture",
//                modifier = Modifier
//                    .fillMaxSize()
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.surface) // Dark gray background for placeholder
//                    .border(
//                        1.dp,
//                        MaterialTheme.colorScheme.primaryContainer,
//                        CircleShape
//                    ) // Thin white border
//            )
//        } else {
//            Image(
//                painter = painterResource(Resources.Icon.Person), // Placeholder user icon
//                contentDescription = "Profile Picture",
//                modifier = Modifier
//                    .fillMaxSize()
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.surface) // Dark gray background for placeholder
//                    .border(
//                        1.dp,
//                        MaterialTheme.colorScheme.primaryContainer,
//                        CircleShape
//                    ) // Thin white border
//            )
//        }
//        Icon(
//            imageVector = vectorResource(Resources.Icon.Add),
//            contentDescription = "Add Profile Picture",
//            tint = BlackLight,
//            modifier = Modifier
//                .size(32.dp)
//                .clip(CircleShape)
//                .background(MaterialTheme.colorScheme.onPrimaryContainer)
//                .border(
//                    1.dp,
//                    Black,
//                    CircleShape
//                ) // Black border to separate from user icon
//                .padding(6.dp) // Inner padding for the icon
//        )
//    }
//
//
//    Box(
//        modifier = Modifier
//            .size(120.dp)
//            .clip(CircleShape) // Clip to a circle for profile picture style
//            .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder background
//            .clickable(enabled = !isLoading) { pickerLauncher.launch() }, // Launch picker on click
//        contentAlignment = Alignment.Center
//    ) {
//        val painterToDisplay =
//            pickedImageBytes?.let { rememberByteArrayPainter(it) } ?: currentProfileImagePainter
//
//        if (painterToDisplay != null) {
//            Image(
//                painter = painterToDisplay,
//                contentDescription = "Profile Picture",
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop
//            )
//        } else {
//            // Placeholder icon if no image is picked or provided
//            Icon(
//                imageVector = Icons.Default.AddAPhoto,
//                contentDescription = "Pick Photo",
//                modifier = Modifier.size(60.dp),
//                tint = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//
//        if (isLoading) {
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center)
//            )
//        }
//    }
//
//
//}
