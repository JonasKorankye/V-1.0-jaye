package com.flipverse.shared.domain

sealed class MediaItem {
    abstract val id: String
    abstract val fileName: String
    abstract val size: Long
    abstract val data: ByteArray
}

data class ImageItem(
    override val id: String,
    override val fileName: String,
    override val size: Long,
    override val data: ByteArray
) : MediaItem()

data class VideoItem(
    override val id: String,
    override val fileName: String,
    override val size: Long,
    override val data: ByteArray,
    val duration: Long? = null
) : MediaItem()

