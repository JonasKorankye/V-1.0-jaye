package com.flipverse.auth

import com.flipverse.shared.domain.FlipNomenclatures


data class AuthState(
    val emailId: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    var otp: String = "",
    val usernameId: String = "",
    val fullName: String = "",
    val thumbnail: String = "thumbnail image",
    var selectedInterests: List<FlipNomenclatures.FlipInterests> = emptyList(),
    var selectedGenres: List<FlipNomenclatures.FlipGenres> = emptyList(),
    var selectedSuggestions: List<FlipNomenclatures.SuggestedFlipAccounts> = emptyList(),
    val isVisible: Boolean = false,
    val isConfirmVisible: Boolean = false,
    var isLoading: Boolean = false,
) {
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasSpecialCharacter = SpecialCharacterValidator(password).containsSpecialCharacters()
    val hasEmailCharacters = SpecialCharacterValidator(emailId).containsSpecialCharacters()
    val hasNumber = password.any { it.isDigit() }
    val hasMinLength = password.length in 6..25
    val isValid = hasUpperCase && hasSpecialCharacter && hasNumber && hasMinLength
}

