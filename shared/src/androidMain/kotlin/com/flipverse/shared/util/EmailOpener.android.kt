package com.flipverse.shared.util

import android.content.Intent
import android.net.Uri

actual fun openEmailApp(emailAddress: String) {
    val context = AndroidShareManagerContext.context

    if (context != null) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$emailAddress")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
