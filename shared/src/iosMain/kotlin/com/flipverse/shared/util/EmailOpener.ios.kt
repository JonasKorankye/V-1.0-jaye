package com.flipverse.shared.util

import platform.UIKit.UIApplication
import platform.Foundation.NSURL

actual fun openEmailApp(emailAddress: String) {
    val mailtoUrl = "mailto:$emailAddress"
    val url = NSURL.URLWithString(mailtoUrl)
    
    if (url != null) {
        val application = UIApplication.sharedApplication
        if (application.canOpenURL(url)) {
            application.openURL(url)
        }
    }
}
