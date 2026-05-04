package com.flipverse.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.NomenclatureRepository
import com.flipverse.data.domain.UserRepository
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFirstName
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getLastName
import com.flipverse.shared.PreferencesRepository.getUsername
import com.flipverse.shared.PreferencesRepository.loadFlipAccounts
import com.flipverse.shared.PreferencesRepository.loadFlipInterests
import com.flipverse.shared.PreferencesRepository.saveEmail
import com.flipverse.shared.PreferencesRepository.saveFlipGenres
import com.flipverse.shared.PreferencesRepository.saveFlipInterests
import com.flipverse.shared.PreferencesRepository.savePassword
import com.flipverse.shared.PreferencesRepository.saveUsername
import com.flipverse.shared.PreferencesRepository.saveThumbnail
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.FlipNomenclatures
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AuthViewModel(
    private val userRepository: UserRepository,
    private val nomenclatureRepository: NomenclatureRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Global exception handler to prevent Kotlin/Native crashes from unhandled coroutine exceptions
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("AuthViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val event = eventChannel.receiveAsFlow()

    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
    var authState by mutableStateOf(AuthState())
        private set

    private val _suggestedFlipAccounts =
        MutableStateFlow<List<FlipNomenclatures.SuggestedFlipAccounts>>(emptyList())
    val suggestedFlipAccounts = _suggestedFlipAccounts.asStateFlow()


    private val _interests =
        MutableStateFlow<List<FlipNomenclatures.FlipInterests>>(emptyList())
    val interests = _interests.asStateFlow()

    private val _genres =
        MutableStateFlow<List<FlipNomenclatures.FlipGenres>>(emptyList())
    val genres = _genres.asStateFlow()


    var thumbnailUploaderState: RequestState<Unit> by mutableStateOf(RequestState.Idle)
        private set

    init {
        fetchInterests()
        fetchSuggestedAccounts()
        fetchLiveBookGenres()
    }

     private fun fetchLiveBookGenres() {
         viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
             try {
                 nomenclatureRepository.readGenresFlow().collectLatest { data ->
                     if (data.isSuccess()) {
                         val fetchedFlipGenres = data.getSuccessData()
                         _genres.value = fetchedFlipGenres
                         authState = AuthState(selectedGenres = fetchedFlipGenres)
                         saveFlipGenres(fetchedFlipGenres.flatMap { it.genres })

                     } else if (data.isError()) {
                         screenReady = RequestState.Error(data.getErrorMessage())
                     }
                 }
             } catch (e: Exception) {
                 println("Error fetching genres: ${e.message}")
                 screenReady = RequestState.Error("Failed to load genres: ${e.message}")
             }
         }
    }


    fun createUser(
        fvUser: FirebaseUser?,
        platformType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                userRepository.createUser(
                    user = fvUser,
                    onSuccess = onSuccess,
                    onError = onError,
                    loginType = platformType
                )
            } catch (e: Exception) {
                println("Error in createUser: ${e.message}")
                onError("Error creating user: ${e.message}")
            }
        }
    }


    /**This applies at the end of the onboarding process, just before dashboard**/
    fun updateUser(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            onLoading(true)
            try {
                userRepository.updateUserProfile(
                    email = getEmail(),
                    username = normalizeUsername(getUsername()),
                    firstName = getFirstName(),
                    lastName = getLastName(),
                    selectedInterests = loadFlipInterests(),
                    selectedSuggestions = loadFlipAccounts(),
                    onSuccess = onSuccess,
                    onError = onError,
                    fullname = getFullName()
                )
            } catch (e: Exception) {
                println("Error updating user: ${e.message}")
                onError("Failed to update profile: ${e.message}")
            } finally {
                onLoading(false)
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.OnForgotPasswordClick -> forgotPassword()

            is AuthAction.OnLoginClick -> login(
                onSuccessUserLogin = {
                    viewModelScope.launch(exceptionHandler) {
                        eventChannel.send(AuthEvent.LoginSuccess)
                    }
                },
                onError = { message ->
                    viewModelScope.launch(exceptionHandler) {
                        eventChannel.send(AuthEvent.Error(message))
                    }
                }
            )

            is AuthAction.OnSignUpContinue -> viewModelScope.launch(exceptionHandler) {
                eventChannel.send(AuthEvent.Navigate.VerifyEmail)
            }

            is AuthAction.OnSetUpPasswordClick -> signUp(
                onSuccess = {
                    viewModelScope.launch(exceptionHandler) {
                        savePassword(authState.password)
                        eventChannel.send(AuthEvent.Navigate.CreateProfile)
                    }
                },
                onError = { message ->
                    viewModelScope.launch(exceptionHandler) {
                        eventChannel.send(AuthEvent.Error(message))
                    }
                }
            )

            is AuthAction.OnTogglePasswordVisibilityClick -> authState =
                authState.copy(isVisible = action.isVisible)

            is AuthAction.OnToggleConfirmPasswordVisibilityClick -> authState =
                authState.copy(isConfirmVisible = action.isConfirmVisible)

            is AuthAction.OnPasswordChange -> authState =
                authState.copy(password = action.newPassword)

            is AuthAction.OnConfirmPasswordChange -> authState =
                authState.copy(confirmPassword = action.confirmPassword)

            is AuthAction.OnEmailIdChange -> authState =
                authState.copy(emailId = action.newEmail)

            is AuthAction.OnUsernameChange -> authState =
                authState.copy(usernameId = action.newUsername)

            is AuthAction.OnFullNameChange -> authState =
                authState.copy(fullName = action.newFullname)

            is AuthAction.OnSendOTPMailClick -> sendOTPVerificationEmail(
                onSuccess = {
                    viewModelScope.launch(exceptionHandler) {
                        saveEmail(authState.emailId)
                        eventChannel.send(AuthEvent.Navigate.VerifyOtp)
                    }
                },
                onError = { message ->
                    viewModelScope.launch(exceptionHandler) {
                        eventChannel.send(AuthEvent.Error(message))
                    }
                },
            )

            is AuthAction.OnboardingCompleted -> updateUser(
                onSuccess = {
                    viewModelScope.launch(exceptionHandler) {
                        eventChannel.send(AuthEvent.Navigate.Dashboard)
                    }
                },
                onError = { message ->
                    viewModelScope.launch(exceptionHandler) {
                        eventChannel.send(AuthEvent.Error(message))
                    }
                },
            )
        }
    }

    fun validateUsernameAndContinue(
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                val normalizedUsername = normalizeUsername(
                    if (authState.usernameId.startsWith("@")) authState.usernameId else "@${authState.usernameId}"
                )

                val isAvailable = userRepository.isUsernameAvailable(normalizedUsername)
                if (!isAvailable) {
                    eventChannel.send(AuthEvent.Error("This username is already taken. Please choose another one."))
                    return@launch
                }

                saveUsername(normalizedUsername)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                eventChannel.send(AuthEvent.Error("Could not verify username: ${e.message}"))
            }
        }
    }


    fun login(
        onSuccessUserLogin: () -> Unit,
        onError: (String) -> Unit
    ) {
        onLoading(true)

        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                userRepository.login(
                    email = authState.emailId,
                    password = authState.password,
                    onSuccess = onSuccessUserLogin,
                    onError = onError,
                )
            } catch (e: Exception) {
                println("Error in login: ${e.message}")
                onError("Login failed: ${e.message}")
            } finally {
                onLoading(false)
            }
        }

    }

    fun sendOTPVerificationEmail(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            onLoading(true)
            try {
                userRepository.sendOTPVerificationEmail(
                    email = authState.emailId,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            } catch (e: Exception) {
                println("Error sending OTP: ${e.message}")
                onError("Failed to send verification email: ${e.message}")
            } finally {
                onLoading(false)
            }
        }

    }

    fun verifyOTP(
        otp: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {

        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            onLoading(true)
            try {
                userRepository.verifyOTP(
                    email = authState.emailId,
                    onSuccess = onSuccess,
                    onError = onError,
                    otp = otp,
                    purpose = "SignUp"
                )
            } catch (e: Exception) {
                println("Error verifying OTP: ${e.message}")
                onError("OTP verification failed: ${e.message}")
            } finally {
                onLoading(false)
            }
        }

    }


    /**This applies just after password and password confirm**/
    private fun signUp(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            onLoading(true)
            try {
                userRepository.signUp(
                    email = getEmail(),
                    password = authState.password,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            } catch (e: Exception) {
                onError(e.message ?: "Sign up failed")
            } finally {
                onLoading(false)
            }
        }
    }

    private fun forgotPassword() {
        viewModelScope.launch(exceptionHandler) {
            eventChannel.send(AuthEvent.Navigate.ForgotPassword)
        }
    }

    private fun onLoading(newValue: Boolean) {
        authState = authState.copy(isLoading = newValue)
    }

    private fun fetchInterests() {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                nomenclatureRepository.readInterestsFlow().collectLatest { data ->
                    if (data.isSuccess()) {
                        val fetchedFlipInterests = data.getSuccessData()
                        _interests.value = fetchedFlipInterests
                        authState = AuthState(selectedInterests = fetchedFlipInterests)
                        screenReady = RequestState.Success(Unit)

                    } else if (data.isError()) {
                        screenReady = RequestState.Error(data.getErrorMessage())
                    }
                }
            } catch (e: Exception) {
                println("Error fetching interests: ${e.message}")
                screenReady = RequestState.Error("Failed to load interests: ${e.message}")
            }
        }
    }

    fun updateThumbnailUploaderState(value: RequestState<Unit>) {
        thumbnailUploaderState = value
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
            try {
                userRepository.deleteImageFromStorage(
                    downloadUrl = authState.thumbnail,
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
            } catch (e: Exception) {
                println("Error deleting thumbnail: ${e.message}")
                onError("Failed to delete thumbnail: ${e.message}")
            }
        }
    }



    private fun fetchSuggestedAccounts() {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                nomenclatureRepository.readSuggestedFlipAccountsFlow().collectLatest { data ->
                    if (data.isSuccess()) {
                        val fetchedFlipAccounts = data.getSuccessData()
                        authState = AuthState(selectedSuggestions = fetchedFlipAccounts)
                        _suggestedFlipAccounts.value = fetchedFlipAccounts
                        screenReady = RequestState.Success(Unit)

                    } else if (data.isError()) {
                        screenReady = RequestState.Error(data.getErrorMessage())
                    }
                }
            } catch (e: Exception) {
                println("Error fetching suggested accounts: ${e.message}")
                screenReady = RequestState.Error("Failed to load suggestions: ${e.message}")
            }
        }
    }

    fun toggleAccountSelection(selectedName: String) {
        _suggestedFlipAccounts.update { currentList ->
            currentList.map { accounts ->
                if (accounts.name == selectedName) {
//                    updatedFlipAccount = accounts.copy(isSelected = !accounts.isSelected)
                    accounts.copy(isSelected = !accounts.isSelected)

                } else {
                    accounts
                }
            }
        }

    }

    fun getSelectedFlipAccounts(): List<FlipNomenclatures.SuggestedFlipAccounts> {
        return _suggestedFlipAccounts.value.filter { it.isSelected }
    }

    fun toggleInterestSelection(interestName: String) {
        _interests.update { currentList ->
            currentList.map { interest ->
                if (interest.name.toString() == interestName) {
                    interest.copy(isSelected = !interest.isSelected)
                } else {
                    interest
                }
            }
        }
    }

    fun getSelectedInterests(): List<FlipNomenclatures.FlipInterests> {
        return _interests.value.filter { it.isSelected }
    }


    fun updateThumbnail(value: String) {
        authState = authState.copy(thumbnail = value)
        saveThumbnail(value)
    }
}
