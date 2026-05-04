package com.flipverse.shared.util

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
        ?: throw IllegalArgumentException("Failed to decode byte array to bitmap")
    return bitmap.asImageBitmap()
}