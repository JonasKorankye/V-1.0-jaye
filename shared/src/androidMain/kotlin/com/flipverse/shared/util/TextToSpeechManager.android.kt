package com.flipverse.shared.util

actual fun getTTSProvider(): TTSProvider {
    return AndroidTTSProvider()
}
