package com.flipverse.shared.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IOSShareManager : ShareManager {
    @OptIn(ExperimentalForeignApi::class)
    override fun shareInviteLink(inviteLink: String, title: String) {
        // Include the full text message so the share sheet and target apps (iMessage, WhatsApp,
        // Mail, etc.) receive the complete content. Also include the extracted NSURL as a second
        // item so apps that handle URLs natively can render it as a tappable hyperlink.
        val urlRegex = Regex("(https?://\\S+)")
        val extractedUrl = urlRegex.find(inviteLink)?.value
        val nsUrl = extractedUrl?.let { platform.Foundation.NSURL.URLWithString(it) }

        // Always put the full plain-text message first so every app sees the content.
        // Adding the NSURL as a second item lets URL-aware apps (Safari, Mail, etc.) also
        // treat it as a native link without hiding the surrounding text.
        val activityItems: List<Any> = if (nsUrl != null) {
            listOf(inviteLink, nsUrl)
        } else {
            listOf(inviteLink)
        }

        val activityViewController = UIActivityViewController(
            activityItems = activityItems,
            applicationActivities = null
        )

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    }
}