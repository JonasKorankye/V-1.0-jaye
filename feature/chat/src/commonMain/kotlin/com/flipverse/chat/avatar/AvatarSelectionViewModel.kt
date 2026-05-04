package com.flipverse.chat.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class AvatarSelectionViewModel(
    private val avatarRepository: AvatarRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("AvatarSelectionViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _uiState = MutableStateFlow(AvatarSelectionState())
    val uiState: StateFlow<AvatarSelectionState> = _uiState.asStateFlow()

    init {
        loadAvatarStyles()
    }

    private fun loadAvatarStyles() {
        viewModelScope.launch(exceptionHandler) {
            when (val result = avatarRepository.getAvatarStyles()) {
                is RequestState.Success -> {
                    val styles = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        availableStyles = styles,
                        selectedStyle = styles.firstOrNull() ?: AvatarStyle.DEFAULT_STYLES.first()
                    )
                }

                is RequestState.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.getErrorMessage(),
                        availableStyles = AvatarStyle.DEFAULT_STYLES,
                        selectedStyle = AvatarStyle.DEFAULT_STYLES.first()
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        availableStyles = AvatarStyle.DEFAULT_STYLES,
                        selectedStyle = AvatarStyle.DEFAULT_STYLES.first()
                    )
                }
            }
        }
    }

    fun generateAvatars() {
        val currentStyle = _uiState.value.selectedStyle
        generateAvatarsForStyle(currentStyle)
    }

    fun regenerateAvatars() {
        val currentStyle = _uiState.value.selectedStyle
        generateAvatarsForStyle(currentStyle)
    }

    fun selectStyle(style: AvatarStyle) {
        _uiState.value = _uiState.value.copy(
            selectedStyle = style,
            selectedAvatarUrl = "", // Clear selection when changing style
            customizations = AvatarCustomizations() // Clear customizations when changing style
        )
        generateAvatarsForStyle(style)
    }

    fun selectAvatar(avatarUrl: String) {
        val selectedAvatar = _uiState.value.avatars.find { it.url == avatarUrl }
        val cleanCustomizations = selectedAvatar?.customizations?.let { customizations ->
            cleanCustomizationsForStyle(_uiState.value.selectedStyle, customizations)
        } ?: AvatarCustomizations()

        _uiState.value = _uiState.value.copy(
            selectedAvatarUrl = avatarUrl,
            selectedAvatar = selectedAvatar,
            customizations = cleanCustomizations
        )

        // Auto-save avatar when selected
        val currentUserId = PreferencesRepository.getId()
        if (currentUserId.isNotEmpty()) {
            PreferencesRepository.saveAvatar(avatarUrl)
            saveAvatar(currentUserId, avatarUrl)
        } else {
            println("Warning: No user ID found for auto-saving avatar")
        }
    }

    fun toggleCustomization() {
        _uiState.value = _uiState.value.copy(
            showCustomization = !_uiState.value.showCustomization
        )
    }

    fun hideCustomization() {
        _uiState.value = _uiState.value.copy(showCustomization = false)
    }

    fun updateCustomization(key: String, value: String?) {
        val currentCustomizations = _uiState.value.customizations

        // Validate color values - they should either be null, "transparent", or 6-character hex codes
        val validatedValue = if (value != null && isColorField(key)) {
            validateColorValue(value)
        } else {
            value
        }

        val updatedCustomizations = when (key) {
            "accessories" -> currentCustomizations.copy(accessories = validatedValue)
            "accessoriesColor" -> currentCustomizations.copy(accessoriesColor = validatedValue)
            "top" -> currentCustomizations.copy(top = validatedValue)
            "hair" -> currentCustomizations.copy(top = validatedValue) // For adventurer style
            "hairColor" -> currentCustomizations.copy(hairColor = validatedValue)
            "facialHair" -> currentCustomizations.copy(facialHair = validatedValue)
            "facialHairColor" -> currentCustomizations.copy(facialHairColor = validatedValue)
            "eyes" -> currentCustomizations.copy(eyes = validatedValue)
            "eyebrows" -> currentCustomizations.copy(eyebrows = validatedValue)
            "mouth" -> currentCustomizations.copy(mouth = validatedValue)
            "skinColor" -> currentCustomizations.copy(skinColor = validatedValue)
            "clothing" -> currentCustomizations.copy(clothing = validatedValue)
            "clothesColor" -> currentCustomizations.copy(clothesColor = validatedValue)
            "clothingGraphic" -> currentCustomizations.copy(clothingGraphic = validatedValue)
            "nose" -> currentCustomizations.copy(nose = validatedValue)
            "hatColor" -> currentCustomizations.copy(hatColor = validatedValue)
            else -> currentCustomizations
        }

        _uiState.value = _uiState.value.copy(customizations = updatedCustomizations)

        // Generate updated avatar with new customizations
        _uiState.value.selectedAvatar?.let { avatar ->
            generateCustomizedAvatar(avatar, updatedCustomizations)
        }
    }

    private fun isColorField(key: String): Boolean {
        return key.contains("Color", ignoreCase = true)
    }

    private fun validateColorValue(value: String): String? {
        return when {
            value.isEmpty() -> null
            value == "transparent" -> value
            value.matches(Regex("^[a-fA-F0-9]{6}$")) -> value
            value.matches(Regex("^#[a-fA-F0-9]{6}$")) -> value.substring(1) // Remove # if present
            else -> {
                // Log warning for invalid color value and return null
                println("Warning: Invalid color value '$value'. Expected 6-character hex code or 'transparent'.")
                null
            }
        }
    }

    private fun generateCustomizedAvatar(avatar: Avatar, customizations: AvatarCustomizations) {
        viewModelScope.launch(exceptionHandler) {
            when (val result = avatarRepository.generateCustomizedAvatar(avatar, customizations)) {
                is RequestState.Success -> {
                    val customizedAvatar = result.getSuccessData()
                    _uiState.value = _uiState.value.copy(
                        selectedAvatarUrl = customizedAvatar.url,
                        selectedAvatar = customizedAvatar
                    )
                }

                is RequestState.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.getErrorMessage()
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to customize avatar"
                    )
                }
            }
        }
    }

    private fun generateAvatarsForStyle(style: AvatarStyle) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = avatarRepository.generateAvatars(style, count = 12)) {
                is RequestState.Success -> {
                    _uiState.value = _uiState.value.copy(
                        avatars = result.getSuccessData(),
                        isLoading = false
                    )
                }

                is RequestState.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.getErrorMessage(),
                        isLoading = false
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun saveAvatar(userId: String, avatarUrl: String) {
        viewModelScope.launch(exceptionHandler) {
            when (val result = avatarRepository.saveUserAvatar(userId, avatarUrl)) {
                is RequestState.Success -> {
                    // Avatar saved successfully
                    println("Avatar saved successfully for user: $userId")
                    _uiState.value = _uiState.value.copy(error = null)
                }

                is RequestState.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.getErrorMessage()
                    )
                    println("Error saving avatar: ${result.getErrorMessage()}")
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to save avatar"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun cleanCustomizationsForStyle(
        style: AvatarStyle,
        customizations: AvatarCustomizations
    ): AvatarCustomizations {
        return AvatarCustomizations(
            accessories = if (hasValidCustomizationOption(
                    style,
                    "accessories",
                    customizations.accessories
                )
            )
                customizations.accessories else null,
            accessoriesColor = validateAndCleanColorValue(customizations.accessoriesColor),
            top = if (hasValidCustomizationOption(style, "top", customizations.top))
                customizations.top else null,
            hairColor = validateAndCleanColorValue(customizations.hairColor),
            facialHair = if (hasValidCustomizationOption(
                    style,
                    "facialHair",
                    customizations.facialHair
                )
            )
                customizations.facialHair else null,
            facialHairColor = validateAndCleanColorValue(customizations.facialHairColor),
            eyes = if (hasValidCustomizationOption(style, "eyes", customizations.eyes))
                customizations.eyes else null,
            eyebrows = if (hasValidCustomizationOption(style, "eyebrows", customizations.eyebrows))
                customizations.eyebrows else null,
            mouth = if (hasValidCustomizationOption(style, "mouth", customizations.mouth))
                customizations.mouth else null,
            skinColor = validateAndCleanColorValue(customizations.skinColor),
            clothing = if (hasValidCustomizationOption(style, "clothing", customizations.clothing))
                customizations.clothing else null,
            clothesColor = validateAndCleanColorValue(customizations.clothesColor),
            clothingGraphic = if (hasValidCustomizationOption(
                    style,
                    "clothingGraphic",
                    customizations.clothingGraphic
                )
            )
                customizations.clothingGraphic else null,
            nose = if (hasValidCustomizationOption(style, "nose", customizations.nose))
                customizations.nose else null,
            hatColor = validateAndCleanColorValue(customizations.hatColor)
        )
    }

    private fun hasValidCustomizationOption(
        style: AvatarStyle,
        optionKey: String,
        value: String?
    ): Boolean {
        if (value == null) return true

        // Handle key mapping between different styles
        val actualKey = when {
            optionKey == "hair" && style.name == "adventurer" -> "hair"
            optionKey == "top" && style.name == "adventurer" -> "hair" // Map top to hair for adventurer
            optionKey == "hair" && style.name == "avataaars" -> "top" // Map hair to top for avataaars
            else -> optionKey
        }

        val option = style.customizationOptions.find { it.key == actualKey }
        return option?.values?.any { it.value == value || it.valueCode == value } ?: false
    }

    private fun validateAndCleanColorValue(value: String?): String? {
        if (value == null) return null
        return validateColorValue(value)
    }
}