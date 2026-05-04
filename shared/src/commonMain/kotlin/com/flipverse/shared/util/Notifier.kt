package com.flipverse.shared.util

interface Notifier {
    fun showNotification(title: String, message: String)
    fun scheduleNotification(title: String, message: String, delayMillis: Long)
}