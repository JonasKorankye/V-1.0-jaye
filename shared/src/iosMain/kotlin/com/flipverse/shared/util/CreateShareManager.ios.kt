package com.flipverse.shared.util

actual fun createShareManager(): ShareManager {
    return IOSShareManager()
}