package com.flipverse.livebook

sealed interface LiveBookEvent {
    data object Success : LiveBookEvent
    data class Error(val error: String) : LiveBookEvent

    sealed interface Navigate {
        data object OpenLiveBook : LiveBookEvent
    }
}