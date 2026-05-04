package com.flipverse.shared.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import dev.gitlive.firebase.storage.File
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToURL

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PhotoPicker {
    private var openPhotoPicker = mutableStateOf(false)

    actual fun open() {
        openPhotoPicker.value = true
    }

    @OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    @Composable
    actual fun InitializePhotoPicker(
        onImageSelect: (file: File?, previewBytes: ByteArray?) -> Unit
    ) {
        val context = LocalPlatformContext.current
        val scope = rememberCoroutineScope()

        val pickerLauncher = rememberFilePickerLauncher(
            type = FilePickerFileType.Image,
            selectionMode = FilePickerSelectionMode.Single,
            onResult = { files ->
                scope.launch {
                    val kmpFile = files.firstOrNull()
                    if (kmpFile != null) {
                        try {
                            // Read image bytes for local preview (uses tempUrl internally)
                            val bytes = kmpFile.readByteArray(context)

                            // Write bytes to a temp file that Firebase can reliably access
                            // (the original security-scoped URL may expire)
                            val tempFileName = NSUUID().UUIDString() + ".jpg"
                            val tempFileUrl = NSURL.fileURLWithPath(
                                NSTemporaryDirectory() + tempFileName
                            )
                            val nsData = bytes.usePinned { pinned ->
                                NSData.create(
                                    bytes = pinned.addressOf(0),
                                    length = bytes.size.toULong()
                                )
                            }
                            nsData.writeToURL(tempFileUrl, atomically = true)

                            val firebaseFile = File(tempFileUrl)
                            onImageSelect(firebaseFile, bytes)
                        } catch (e: Exception) {
                            println("Error reading picked file: ${e.message}")
                            onImageSelect(null, null)
                        }
                    } else {
                        onImageSelect(null, null)
                    }
                    openPhotoPicker.value = false
                }
            }
        )

        val openPhotoPickerState by remember { openPhotoPicker }

        LaunchedEffect(openPhotoPickerState) {
            if (openPhotoPickerState) {
                pickerLauncher.launch()
            }
        }
    }
}
