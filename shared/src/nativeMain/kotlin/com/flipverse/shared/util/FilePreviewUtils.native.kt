package com.flipverse.shared.util

import dev.gitlive.firebase.storage.File

actual fun File.toCoilModel(): Any? = this.url.absoluteString
