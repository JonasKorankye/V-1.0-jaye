package com.flipverse.shared.util

import androidx.compose.runtime.Composable
import dev.gitlive.firebase.storage.File

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PhotoPicker {
    /**
     * Initializes the photo picker.
     * @param onImageSelect Called with the Firebase [File] for upload and a [ByteArray] of the
     *   image bytes for immediate local preview. Either or both may be null if picking was cancelled.
     */
    @Composable
    fun InitializePhotoPicker(onImageSelect: (file: File?, previewBytes: ByteArray?) -> Unit)
    fun open()
}