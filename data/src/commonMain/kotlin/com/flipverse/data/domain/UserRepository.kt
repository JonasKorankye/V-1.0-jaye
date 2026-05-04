package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.User
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUserId(): String?

    suspend fun fetchUserDetails(userId: String?): String?

    suspend fun createUser(
        user: FirebaseUser?,
        loginType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun signUp(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun sendOTPVerificationEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    )
        suspend fun sendOTPVerificationEmailForForgottenPassword(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun deleteImageFromStorage(
        downloadUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun verifyOTP(
        email: String,
        otp: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        purpose: String = "SignUp"
    )

    suspend fun signOut(): RequestState<Unit>

    fun readUserProfileFlow(): Flow<RequestState<User>>

    suspend fun updateUserProfile(
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        fullname: String,
        selectedInterests: List<String>,
        selectedSuggestions: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    suspend fun updateUserProfileThumbnail(
        downloadUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun updateUserProfileDetails(
        fullname: String,
        bio: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun isUsernameAvailable(
        username: String,
        currentUserId: String? = null,
    ): Boolean

    suspend fun uploadImageToStorage(file: File): String?

    suspend fun updateMessageToken(
        userId: String,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    suspend fun sendPasswordResetEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun changePassword(
        email: String,
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun resetPassword(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )
}
