package com.flipverse.di

import com.flipverse.shared.util.PhotoPicker
import com.flipverse.shared.util.IOSShareManager
import org.koin.core.KoinApplication
import org.koin.dsl.module

actual val targetModule = module {
    factory<PhotoPicker> { PhotoPicker() }
    single { IOSShareManager() }
}

/**
 * iOS-specific notification setup
 * iOS uses native APNs notifications handled in Swift (iOSApp.swift)
 * No KMPNotifier needed on iOS
 */
actual fun KoinApplication.setupPlatformNotifications() {
    println("🍎 iOS: Notifications handled natively via Swift/APNs")
    // No-op: iOS notifications are handled natively in iOSApp.swift
    // using UserNotifications framework and APNs directly
}