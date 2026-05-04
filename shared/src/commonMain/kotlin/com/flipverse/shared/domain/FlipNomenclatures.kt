package com.flipverse.shared.domain

import kotlinx.serialization.Serializable


class FlipNomenclatures {
    @Serializable
    data class SuggestedFlipAccounts(
        val id: String,
        val name: String,
        val author: String,
        val logoResId: String? = "",
        val category: String? = "",
        var isSelected: Boolean = false
    )

    @Serializable
    data class SelectedFlipAccounts(
        val accounts: List<String>
    )

    @Serializable
    data class FlipGenres(
        val genres: List<String> = emptyList()
    )

    @Serializable
    data class FlipInterests(
        val name: List<String> = emptyList(),
        var isSelected: Boolean = false
    )

}