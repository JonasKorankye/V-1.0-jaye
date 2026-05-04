package com.flipverse.userprofile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.UserRepository
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.PreferencesRepository.getBio
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.PreferencesRepository.getUsername
import com.flipverse.shared.PreferencesRepository.saveThumbnail
import com.flipverse.shared.RequestState
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("UserProfileViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    var profileState by mutableStateOf(
        ProfileState(
            name = getFullName(),
            bio = getBio(),
            username = getUsername(),
            // Seed from preferences so the image is visible immediately on first compose,
            // even before the Firestore refresh below completes.
            thumbnail = getThumbnail(),
            initialName = getFullName(),
            initialBio = getBio(),
            initialUsername = normalizeUsername(getUsername()),
        )
    )
        private set


    var thumbnailUploaderState: RequestState<Unit> by mutableStateOf(RequestState.Idle)
        private set

    var saveProfileState: RequestState<Unit> by mutableStateOf(RequestState.Idle)
        private set

    init {
        // Observe the live Firestore user document so the profile picture is always
        // up-to-date, even if preferences were stale when the ViewModel was created.
        viewModelScope.launch(exceptionHandler) {
            userRepository.readUserProfileFlow().collectLatest { state ->
                if (state.isSuccess()) {
                    val freshThumbnail = state.getSuccessData().thumbnail
                    if (freshThumbnail != profileState.thumbnail) {
                        profileState = profileState.copy(thumbnail = freshThumbnail)
                        saveThumbnail(freshThumbnail)
                    }
                }
            }
        }
    }


    fun signOut(
        onSuccess: Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                userRepository.signOut()
            }
            if (result.isSuccess()) {
                // Note: Caches are cleared via ChatRepositoryImpl.clearAllCaches()
                // which is called from the repository level
                onSuccess
            } else if (result.isError()) {
                onError(result.getErrorMessage())
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnNameChange -> profileState =
                profileState.copy(name = action.newName)

            is ProfileAction.OnBioChange -> profileState =
                profileState.copy(bio = action.newBio)

            ProfileAction.OnSaveClick -> {
                saveProfile()
            }

            is ProfileAction.OnUserNameChange -> profileState =
                profileState.copy(username = action.newUsername)
        }
    }

    fun initializeProfileFromPreferences() {
        profileState = profileState.copy(
            name = getFullName(),
            bio = getBio(),
            username = getUsername(),
            thumbnail = getThumbnail(),
            initialName = getFullName(),
            initialBio = getBio(),
            initialUsername = getUsername(),
        )
    }

    //region Thumbnail
    fun updateThumbnail(value: String) {
        profileState = profileState.copy(thumbnail = value)
        saveThumbnail(value)
    }

    fun updateThumbnailUploaderState(value: RequestState<Unit>) {
        thumbnailUploaderState = value
    }

    fun updateSaveProfileState(value: RequestState<Unit>) {
        saveProfileState = value
    }

    fun uploadThumbnailToStorage(
        file: File?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (file == null) {
            updateThumbnailUploaderState(RequestState.Error("File is null. Error while selecting an image.\nTRY AGAIN!"))
            return onError("File is null. Error while selecting an image.\nTRY AGAIN!")
        }

        updateThumbnailUploaderState(RequestState.Loading)

        viewModelScope.launch(exceptionHandler) {
            try {
                val downloadUrl = userRepository.uploadImageToStorage(file)

                if (downloadUrl.isNullOrEmpty()) {
                    throw Exception("Failed to retrieve a download URL after the upload.")
                }
                userRepository.updateUserProfileThumbnail(
                    downloadUrl = downloadUrl,
                    onSuccess = {
                        onSuccess()
                        updateThumbnailUploaderState(RequestState.Success(Unit))
                        updateThumbnail(downloadUrl)
                    },
                    onError = { message ->
                        updateThumbnailUploaderState(RequestState.Error(message))
                    }
                )
            } catch (e: Exception) {
                updateThumbnailUploaderState(RequestState.Error("Error while uploading: ${e.message}"))
            }
        }
    }

    fun deleteThumbnailFromStorage(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch(exceptionHandler) {
            userRepository.deleteImageFromStorage(
                downloadUrl = getThumbnail(),
                onSuccess = {
                    viewModelScope.launch(exceptionHandler) {
                        userRepository.updateUserProfileThumbnail(
                            downloadUrl = "",
                            onSuccess = {
                                updateThumbnail(value = "")
                                updateThumbnailUploaderState(RequestState.Idle)
                                saveThumbnail("")
                                onSuccess()
                            },
                            onError = { message -> onError(message) }
                        )
                    }

                },
                onError = onError
            )
        }
    }
//endregion

    private fun saveProfile() {
        saveProfileState = RequestState.Loading
        viewModelScope.launch(exceptionHandler) {
            try {
                val normalizedUsername = normalizeUsername(
                    if (profileState.username.startsWith("@")) profileState.username else "@${profileState.username}"
                )
                val isUsernameAvailable = userRepository.isUsernameAvailable(
                    username = normalizedUsername,
                    currentUserId = userRepository.getCurrentUserId()
                )

                if (!isUsernameAvailable) {
                    saveProfileState = RequestState.Error("This username is already taken. Please choose another one.")
                    return@launch
                }

                userRepository.updateUserProfileDetails(
                    fullname = profileState.name,
                    bio = profileState.bio,
                    username = profileState.username,
                    onSuccess = {
                        // Update initial values to reflect the saved state
                        profileState = profileState.copy(
                            initialName = profileState.name,
                            initialBio = profileState.bio,
                            initialUsername = profileState.username
                        )
                        saveProfileState = RequestState.Success(Unit)
                    },
                    onError = { message ->
                        saveProfileState = RequestState.Error(message)
                    }
                )
            } catch (e: Exception) {
                saveProfileState = RequestState.Error("Failed to verify username: ${e.message}")
            }
        }
    }
}

data class ProfileState(
    val name: String = "",
    val bio: String = "",
    val username: String = "",
    val thumbnail: String = "thumbnail image",
    val initialName: String = name,
    val initialBio: String = bio,
    val initialUsername: String = username,
) {
    fun hasChanges(): Boolean {
        return name != initialName ||
                bio != initialBio ||
                username != initialUsername
    }
}

sealed interface ProfileAction {
    data class OnNameChange(val newName: String) : ProfileAction
    data class OnBioChange(val newBio: String) : ProfileAction
    data class OnUserNameChange(val newUsername: String) : ProfileAction
    data object OnSaveClick : ProfileAction
}
