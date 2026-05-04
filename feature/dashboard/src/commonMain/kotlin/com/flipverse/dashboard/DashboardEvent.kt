package com.flipverse.dashboard

sealed interface DashboardEvent {
    data object
    Success : DashboardEvent
    data class Error(val error: String) : DashboardEvent

    sealed interface Navigate {
        data object Dashboard : DashboardEvent
    }
}