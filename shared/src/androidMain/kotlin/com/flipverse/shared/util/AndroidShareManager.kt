package com.flipverse.shared.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent

class AndroidShareManager(private val context: Context) : ShareManager {
    override fun shareInviteLink(inviteLink: String, title: String) {
        // Build a properly styled HTML version for apps that support rich content (Gmail, etc.).
        //
        // Order matters:
        //   1. Escape HTML special chars in the user content (& < >)
        //   2. Wrap URLs in <a href> tags — BEFORE converting \n to <br>, so that \S+ stops
        //      cleanly at the real newline (whitespace) and doesn't accidentally capture <br> tags
        //   3. Convert remaining \n to <br> line-breaks
        val urlRegex = Regex("https?://\\S+")
        val htmlContent = inviteLink
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace(urlRegex) { match ->
                // URL was already clean (no < or > to escape); wrap it as a blue hyperlink
                "<a href=\"${match.value}\" style=\"color:#1565C0;\">${match.value}</a>"
            }
            .replace("\n", "<br>")   // convert newlines AFTER URL wrapping

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            // Plain text → WhatsApp, Telegram, SMS etc. auto-detect the URL as a tappable link
            putExtra(Intent.EXTRA_TEXT, inviteLink)
            // HTML text → Gmail / email clients render the <a href> as a blue underlined hyperlink
            putExtra(Intent.EXTRA_HTML_TEXT, htmlContent)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share invite link")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}

@SuppressLint("StaticFieldLeak")
object AndroidShareManagerContext {
    var context: Context? = null
        private set


    fun init(context: Context) {
        this.context = context.applicationContext // Use applicationContext to avoid memory leaks
    }
}


