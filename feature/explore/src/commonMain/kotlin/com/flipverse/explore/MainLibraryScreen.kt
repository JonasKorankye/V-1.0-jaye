//package com.flipverse.explore
//
//import androidx.compose.runtime.Composable
//import dev.scottpierce.calf.filepicker.FilePickerResult
//import dev.scottpierce.calf.filepicker.rememberFilePicker
//import feature.explore.src.commonMain.kotlin.com.flipverse.explore.PickedBookFile
//
//@Composable
//fun MainLibraryScreen(
//    paddingValues: androidx.compose.foundation.layout.PaddingValues,
//    onNavigateToBookDetails: (String) -> Unit
//) {
//    val filePicker = rememberFilePicker(
//        mimeTypes = listOf(
//            "application/pdf",
//            "application/epub+zip",
//            "application/octet-stream",
//            "*/*"
//        )
//    )
//    LibraryScreen(
//        paddingValues = paddingValues,
//        onNavigateToBookDetails = onNavigateToBookDetails,
//        pickBookFile = {
//            val result: FilePickerResult? = filePicker.pick()
//            if (result != null) {
//                val fileBytes: ByteArray = result.readBytes()
//                val fileName: String = result.name ?: "Imported Book"
//                PickedBookFile(name = fileName, bytes = fileBytes)
//            } else {
//                null
//            }
//        }
//    )
//}
