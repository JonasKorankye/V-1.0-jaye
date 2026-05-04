package com.flipverse.shared.util

import dev.gitlive.firebase.storage.File

/**
 * Converts a [dev.gitlive.firebase.storage.File] to a platform-specific object
 * that Coil's AsyncImage can load for local image preview.
 *
 * - Android: returns the underlying [android.net.Uri]
 * - iOS: returns the NSURL absolute string (file:// URL)
 */
expect fun File.toCoilModel(): Any?
