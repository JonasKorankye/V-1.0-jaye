package com.flipverse.shared.util

interface ShareManager {
    fun shareInviteLink(inviteLink: String, title: String = "Join me on the app!")
}