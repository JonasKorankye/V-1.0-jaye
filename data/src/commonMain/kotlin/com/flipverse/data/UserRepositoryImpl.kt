package com.flipverse.data

import com.flipverse.data.domain.UserRepository
import com.flipverse.data.util.generateOtp
import com.flipverse.data.util.generateSalt
import com.flipverse.data.util.generateUserId
import com.flipverse.data.util.hashPassword
import com.flipverse.data.util.normalizeUsername
import com.flipverse.data.util.verifyPassword
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.saveAvatar
import com.flipverse.shared.PreferencesRepository.saveBio
import com.flipverse.shared.PreferencesRepository.saveEmail
import com.flipverse.shared.PreferencesRepository.saveFirstName
import com.flipverse.shared.PreferencesRepository.saveFirstTimeLoginStatus
import com.flipverse.shared.PreferencesRepository.saveFullName
import com.flipverse.shared.PreferencesRepository.saveLastName
import com.flipverse.shared.PreferencesRepository.saveThumbnail
import com.flipverse.shared.PreferencesRepository.saveUsername
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.OTPRequest
import com.flipverse.shared.domain.OtpResponse
import com.flipverse.shared.domain.OtpVerification
import com.flipverse.shared.domain.PhoneNumber
import com.flipverse.shared.domain.User
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime

class UserRepositoryImpl : UserRepository {
    private val functions = Firebase.functions

    private val dateString =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()

    override fun getCurrentUserId(): String? {
        return if (Firebase.auth.currentUser?.uid.isNullOrEmpty() && getEmail().isEmpty()) {
            null  //show login screen
        } else if (!Firebase.auth.currentUser?.uid.isNullOrEmpty()) {
            //show dashboard/home screen
            Firebase.auth.currentUser?.uid
        } else {
            getEmail()
        }
    }

    override suspend fun fetchUserDetails(userId: String?): String {
        val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")

        val userQuery = fvUserCollection
            .where { "id" equalTo userId }
            .get()

        val userDoc = userQuery.documents.first()
        val user = userDoc.data<User>()
        println("avatar: ${user.avatar}")
        saveEmail(user.email)
        saveUsername(user.username)
        saveFullName(user.fullname)
        saveThumbnail(user.thumbnail)
        saveFirstName(user.firstName)
        saveLastName(user.lastName)
        return user.email
    }

    override suspend fun createUser(
        user: FirebaseUser?,
        loginType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (user != null) {
                val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")
                val uuid = generateUserId()

                val fvUser = User(
                    id = user.uid,
                    firstName = user.displayName?.split(" ")?.firstOrNull() ?: com.flipverse.shared.Strings.unknown,
                    lastName = user.displayName?.split(" ")?.lastOrNull() ?: com.flipverse.shared.Strings.unknown,
                    email = user.email?.trim() ?: com.flipverse.shared.Strings.unknown,
                    username = "",
                    bio = "",
                    imageUrl = "",
                    followersCount = 0,
                    followingCount = 0,
                    isFollowing = false,
                    avatar = "",
                    isOwnProfile = true,
                    passCode = "",
                    createdAt = dateString,
                    phoneNumber = PhoneNumber(233, ""),
                    hashedPasscode = "",
                    selectedInterests = emptyList(),
                    selectedSuggestions = emptyList(),
                    salt = "",
                    lastLogin = dateString,
                    loginType = loginType,
                    fullname = user.displayName ?: com.flipverse.shared.Strings.unknown,
                    firstTimeLogin = true,
                    thumbnail = "",
                    postsCount = 0,
                    isVerified = false,
                )

                val fvUserExists = fvUserCollection.document(user.uid).get().exists
                if (fvUserExists) {
                    //todo: update first time login as false as well as store email in shared preference
                    saveEmail(user.email)
                    updateFirstTimeLogin(user.uid, false)
                    
                    // Load user data to initialize preferences
                    val existingUserDoc = fvUserCollection.document(user.uid).get()
                    val existingUser = existingUserDoc.data<User>()
                    com.flipverse.shared.PreferencesRepository.saveFlipInterests(existingUser.interests)
                    com.flipverse.shared.PreferencesRepository.saveFlipAccounts(existingUser.selectedAccounts)
                    
                    onSuccess()
                } else {
                    fvUserCollection.document(user.uid).set(fvUser)
                    //todo: store email in shared preference
                    saveEmail(user.email)
                    saveFirstTimeLoginStatus(true)
                    
                    // Initialize empty preferences for new user
                    com.flipverse.shared.PreferencesRepository.saveFlipInterests(emptyList())
                    com.flipverse.shared.PreferencesRepository.saveFlipAccounts(emptyList())
                    
                    onSuccess()

                }

            } else {
                onError("FlipVerse User is not available")
            }

        } catch (e: Exception) {
            onError("Error while creating flipVerse User: ${e.message}")
        }

    }

    override suspend fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {

            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")

            // First, find user by username
            val userQuery = fvUserCollection
                .where { "email" equalTo email.trim() }
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("FlipVerse User Not Found!")
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            // Verify password
            if (verifyPassword(password.trim(), user.hashedPasscode!!, user.salt!!)) {
                updateLastLogin(user.id)
                updateFirstTimeLogin(user.email, false)
                saveEmail(user.email)
                saveThumbnail(user.thumbnail)
                saveUsername(user.username)
                saveFullName(user.fullname)
                
                // Initialize user preferences from Firestore
                com.flipverse.shared.PreferencesRepository.saveFlipInterests(user.interests)
                com.flipverse.shared.PreferencesRepository.saveFlipAccounts(user.selectedAccounts)
                
                // Set user ID in Crashlytics after successful login
                CrashlyticsLogger.setUserId(user.id)
                
                onSuccess()
            } else {
                return onError("Invalid username or password")
            }
        } catch (e: Exception) {
            // Log authentication error to Crashlytics
            CrashlyticsLogger.logAuthError(
                userId = null,
                operation = "login",
                error = e
            )
            return onError("Error while manually Signing in flipVerse User: ${e.message}")
        }

    }


    override suspend fun signUp(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")
            
            val sanitizedEmail = email.trim().lowercase()
            val sanitizedPassword = password.trim()
            
            // First, check if user already exists
            val userQuery = fvUserCollection
                .where { "email" equalTo sanitizedEmail }
                .get()

            if (userQuery.documents.isNotEmpty()) {
                onError("An account with this email already exists.")
                return
            }

            val uuid = generateUserId()
            val salt = generateSalt()
            val hashedPassword = hashPassword(sanitizedPassword, salt)
            
            val fvUser = User(
                id = uuid,
                firstName = com.flipverse.shared.Strings.unknown,
                lastName = com.flipverse.shared.Strings.unknown,
                passCode = sanitizedPassword,
                email = sanitizedEmail,
                username = "",
                bio = "",
                imageUrl = "",
                followersCount = 0,
                followingCount = 0,
                isFollowing = false,
                avatar = "",
                isOwnProfile = true,
                createdAt = dateString,
                phoneNumber = PhoneNumber(233, ""),
                hashedPasscode = hashedPassword,
                salt = salt,
                lastLogin = dateString,
                selectedInterests = emptyList(),
                selectedSuggestions = emptyList(),
                loginType = "Traditional",
                fullname = "",
                thumbnail = "",
                postsCount = 0,
                isVerified = false,
            )

            fvUserCollection.document(uuid).set(fvUser)
            
            // Initialize empty preferences for new user
            com.flipverse.shared.PreferencesRepository.saveFlipInterests(emptyList())
            com.flipverse.shared.PreferencesRepository.saveFlipAccounts(emptyList())
            
            // Set user ID in Crashlytics after successful signup
            CrashlyticsLogger.setUserId(uuid)
            
            onSuccess()

        } catch (e: Exception) {
            // Log authentication error to Crashlytics
            CrashlyticsLogger.logAuthError(
                userId = null,
                operation = "signUp",
                error = e
            )
            onError("Error while manually creating flipVerse User: ${e.message}")
        }

    }

    override suspend fun signOut(): RequestState<Unit> {
        return try {
            // Clear authentication
            Firebase.auth.signOut()

            // Clear all caches on logout
            try {
                ChatRepositoryImpl.clearAllCaches()
                println("✅ All caches cleared on logout")
            } catch (e: Exception) {
                println("⚠️ Error clearing caches on logout: ${e.message}")
            }
            
            // Clear user ID from Crashlytics on logout
            CrashlyticsLogger.clearUserId()

            RequestState.Success(data = Unit)
        } catch (e: Exception) {
            CrashlyticsLogger.logAuthError(
                userId = null,
                operation = "signOut",
                error = e
            )
            RequestState.Error("Error while Signing Out: ${e.message}")
        }
    }


    override fun readUserProfileFlow(): Flow<RequestState<User>> = channelFlow {
        try {
            val database = Firebase.firestore
            val userCollection = database.collection(collectionPath = "user")


            val userQuery = userCollection
                .where { "email" equalTo getEmail().lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                println("Empty user query")
                send(RequestState.Error("Queried FlipVerse user profile not found."))
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            if (userDoc.exists) {
//                userCollection
//                    .document(user.id)
//                    .collectLatest { document ->
//                        if (document.exists) {
                val privateDataDocument =
                    database.collection(collectionPath = "user")
                        .document(user.id)
                        .collection(collectionPath = "privateData")
                        .document("role")
                        .get()
                val selectedInterests =
                    userDoc.get("selectedInterests") as? List<String> ?: emptyList()
                val selectedSuggestions =
                    userDoc.get("selectedSuggestions") as? List<String> ?: emptyList()
                println("selectedInterests: $selectedInterests")
                println("selectedSuggestions: $selectedSuggestions")

                val userDetails = User(
                    id = user.id,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    username = user.username,
                    bio = user.bio,
                    imageUrl = user.imageUrl,
                    phoneNumber = user.phoneNumber,
                    isFollowing = user.isFollowing,
                    avatar = user.avatar,
                    createdAt = user.createdAt,
                    followersCount = user.followersCount,
                    followingCount = user.followingCount,
                    hashedPasscode = user.hashedPasscode,
                    interests = selectedInterests,
                    selectedAccounts = selectedSuggestions,
                    salt = user.salt,
                    lastLogin = user.lastLogin,
                    isOwnProfile = user.isOwnProfile,
                    passCode = user.passCode,
                    loginType = user.loginType,
                    fullname = user.fullname,
                    thumbnail = user.thumbnail,
                    postsCount = user.postsCount,
                    firstTimeLogin = user.firstTimeLogin,
                    isVerified = user.isVerified,
                )
                send(RequestState.Success(data = userDetails))
                println("Success user query$userDetails")

            } else {
                send(RequestState.Error("User is not available."))
                println("User is not available.")
            }
        } catch (e: Exception) {
            send(RequestState.Error("Error while reading User information: ${e.message}"))
            println("Error while reading User information: ${e.message}")
        }

    }


    override suspend fun updateUserProfile(
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        fullname: String,
        selectedInterests: List<String>,
        selectedSuggestions: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        return try {
            val firestore = Firebase.firestore
            val userCollection = firestore.collection(collectionPath = "user")


            // First, find user by username
            val userQuery = userCollection
                .where { "email" equalTo email.lowercase() }
                .get()

            if (userQuery.documents.isEmpty()) {
                onError("FlipVerse User Not Found!")
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()


            if (userDoc.exists) {
                val normalizedUsername = normalizeUsername(
                    if (username.startsWith("@")) username else "@$username"
                ).trim()
                if (!isUsernameAvailable(normalizedUsername, user.id)) {
                    onError("This username is already taken. Please choose another one.")
                    return
                }

                userCollection
                    .document(user.id)
                    .update(
                        "selectedInterests" to selectedInterests,
                        "selectedSuggestions" to selectedSuggestions,
                        "username" to normalizeUsername(buildString {
                            append("@")
                            append(username)
                        }),
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "fullname" to fullname,
                        // Mark onboarding as complete so the user is visible to others
                        "firstTimeLogin" to false,
                    )
                saveBio(user.bio)
                saveFullName(fullname)
                saveUsername(normalizeUsername(buildString {
                    append("@")
                    append(username)
                }))
                saveFirstTimeLoginStatus(false)
                onSuccess()
            } else {
                onError("User not found.")
            }

        } catch (e: Exception) {
            onError("Error while updating FlipVerse User information: ${e.message}")
        }
    }

    override suspend fun updateUserProfileThumbnail(
        downloadUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val email = getEmail().trim()

            val database = Firebase.firestore
            val userCollection = database.collection(collectionPath = "user")

            val userQuery = userCollection
                .where { "email" equalTo email.lowercase() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("Queried FlipVerse user profile not found.")
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            if (userDoc.exists) {
                userCollection
                    .document(user.id)
                    .update(
                        "thumbnail" to downloadUrl,
                    )
                onSuccess()
            } else {
                onError("User not found.")
            }
        } catch (e: Exception) {
            onError("Error while updating a thumbnail image: ${e.message}")
        }

    }

    override suspend fun updateUserProfileDetails(
        fullname: String,
        bio: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val email = getEmail().trim()

            val database = Firebase.firestore
            val userCollection = database.collection(collectionPath = "user")

            val userQuery = userCollection
                .where { "email" equalTo email.lowercase() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("Queried FlipVerse user profile not found.")
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            if (userDoc.exists) {
                val normalizedUsername = normalizeUsername(
                    if (username.startsWith("@")) username else "@$username"
                ).trim()

                userCollection
                    .document(user.id)
                    .update(
                        "fullname" to fullname,
                        "bio" to bio,
                        "username" to normalizedUsername,
                    )
                saveBio(bio)
                saveFullName(fullname)
                saveUsername(normalizedUsername)
                onSuccess()
            } else {
                onError("User not found.")
            }
        } catch (e: Exception) {
            onError("Error while updating user profile details: ${e.message}")
        }
    }

    override suspend fun isUsernameAvailable(
        username: String,
        currentUserId: String?
    ): Boolean {
        val normalizedUsername = normalizeUsername(
            if (username.startsWith("@")) username else "@$username"
        ).trim().lowercase()

        if (normalizedUsername.isBlank() || normalizedUsername == "@") return false

        return try {
            val userCollection = Firebase.firestore.collection(collectionPath = "user")
            val userQuery = userCollection.get()

            userQuery.documents.none { document ->
                val user = document.data<User>()
                val existingUsername = normalizeUsername(user.username).trim().lowercase()
                val isCurrentUser = !currentUserId.isNullOrBlank() && user.id == currentUserId

                !isCurrentUser && existingUsername == normalizedUsername
            }
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun updateLastLogin(userId: String) {
        try {
            val firestore = Firebase.firestore
            val userCollection = firestore.collection(collectionPath = "user")

            userCollection.document(userId).update(
                mapOf("lastLogin" to dateString)
            )
        } catch (e: Exception) {
            println("Update not successful: ${e.message}")
        }
    }

    private suspend fun updateFirstTimeLogin(userId: String, status: Boolean) {
        try {
            val firestore = Firebase.firestore
            val userCollection = firestore.collection(collectionPath = "user")

            userCollection.document(userId).update(
                mapOf("firstTimeLogin" to status)
            )
            saveFirstTimeLoginStatus(status)
        } catch (e: Exception) {
            println("Update not successful: ${e.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun sendOTPVerificationEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        return try {
            // First check if the email entered already exists
            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")
            val userQuery = fvUserCollection
                .where { "email" equalTo email.trim().lowercase() }
                .get()

            if (userQuery.documents.isNotEmpty()) {
                return onError("An account with this email already exists. Please login instead.")
            }
            
            // Generate a random 6-digit OTP
            val otp = generateOtp()

            // Create OTP request
            val otpRequest = OTPRequest(
                email = email.trim(),
                otp = otp.trim()
            )

            val callable = functions.httpsCallable("postOtpEmail")
            val result = callable(otpRequest)

            // Parse the response
            val responseData = result.data(OtpResponse.serializer())

            // Create OTP record
            val otpRecord = OtpVerification(
                id = responseData.email,
                email = responseData.email,
                otp = responseData.otp,
                purpose = "SignUp",
                userId = ""
            )

            // Save OTP to Firestore
            Firebase.firestore.collection(collectionPath = "otp_verifications")
                .document(otpRecord.id).set(otpRecord)

            onSuccess(responseData.message)

        } catch (e: Exception) {
            onError(e.message.toString())
        }
    }


 @OptIn(ExperimentalTime::class)
    override suspend fun sendOTPVerificationEmailForForgottenPassword(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        return try {
            // First check if the email entered already exists
            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")
            val userQuery = fvUserCollection
                .where { "email" equalTo email.trim().lowercase() }
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("An account with this email does not exist. Please retry.")
            }

            // Generate a random 6-digit OTP
            val otp = generateOtp()

            // Create OTP request
            val otpRequest = OTPRequest(
                email = email.trim(),
                otp = otp.trim()
            )

            val callable = functions.httpsCallable("postOtpEmail")
            val result = callable(otpRequest)

            // Parse the response
            val responseData = result.data(OtpResponse.serializer())

            // Create OTP record
            val otpRecord = OtpVerification(
                id = responseData.email,
                email = responseData.email,
                otp = responseData.otp,
                purpose = "PasswordReset",
                userId = ""
            )

            // Save OTP to Firestore
            Firebase.firestore.collection(collectionPath = "otp_verifications")
                .document(otpRecord.id).set(otpRecord)

            onSuccess(responseData.message)

        } catch (e: Exception) {
            onError(e.message.toString())
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun verifyOTP(
        email: String,
        otp: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        purpose: String
    ) {
        try {
            // Find valid OTP
            val otpCollection = Firebase.firestore.collection(collectionPath = "otp_verifications")

            val otpQuery = otpCollection
//                .where { "email" equalTo email }
                .where { "otp" equalTo otp }
                .where { "purpose" equalTo purpose }
                .where { "isUsed" equalTo false }
                .get()

            if (otpQuery.documents.isEmpty()) {
                onError("Invalid or expired OTP")
                return
            }

            val otpDoc = otpQuery.documents.first()
            val otpRecord = otpDoc.data<OtpVerification>()

            // Check if OTP is expired
            val currentTime = now().toEpochMilliseconds()
            if (currentTime > otpRecord.expiresAt) {
                onError("OTP has expired")
                return
            }

            // Check attempts
            if (otpRecord.attempts >= otpRecord.maxAttempts) {
                onError("Maximum attempts exceeded")
                return
            }

            // Mark OTP as used
            otpCollection.document(otpRecord.id).update(
                mapOf(
                    "isUsed" to true,
                    "attempts" to otpRecord.attempts + 1
                )
            )

            onSuccess()

        } catch (e: Exception) {
            onError("OTP Verification failed: ${e.message}")
        }
    }


    override suspend fun uploadImageToStorage(file: File): String? {
        return if (getCurrentUserId() != null) {
            val storage = Firebase.storage.reference
            val imagePath = storage.child(path = "images/users/${generateUserId()}")
            try {
                withTimeout(timeMillis = 20000L) {
                    imagePath.putFile(file)
                    imagePath.getDownloadUrl()
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    private fun extractFirebaseStoragePath(downloadUrl: String): String? {
        val startIndex = downloadUrl.indexOf("/o/") + 3
        if (startIndex < 3) return null

        val endIndex = downloadUrl.indexOf("?", startIndex)
        val encodedPath = if (endIndex != -1) {
            downloadUrl.substring(startIndex, endIndex)
        } else {
            downloadUrl.substring(startIndex)
        }

        return decodeFirebasePath(encodedPath)
    }

    private fun decodeFirebasePath(encodedPath: String): String {
        return encodedPath
            .replace("%2F", "/")
            .replace("%20", " ")
    }


    override suspend fun deleteImageFromStorage(
        downloadUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val storagePath = extractFirebaseStoragePath(downloadUrl)
            if (storagePath != null) {
                Firebase.storage.reference(storagePath).delete()
                onSuccess()
            } else {
                onError("Storage Path is null.")
            }
        } catch (e: Exception) {
            onError("Error while deleting a thumbnail: ${e.message}")
        }
    }

    override suspend fun updateMessageToken(
        userId: String,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val firestore = Firebase.firestore
            val userCollection = firestore.collection("user")

            // Find the user by userId (which could be email or Firebase UID)
            val userQuery = if (userId.contains("@")) {
                // userId is email, query by email
                userCollection.where { "email" equalTo userId.lowercase().trim() }
            } else {
                // userId is Firebase UID, query by id
                userCollection.where { "id" equalTo userId }
            }

            val queryResult = userQuery.get()

            if (queryResult.documents.isEmpty()) {
                onError("User not found")
                return
            }

            val userDoc = queryResult.documents.first()
            val user = userDoc.data<User>()

            // Update the user document with the message token
            userCollection
                .document(user.id)
                .update(
                    mapOf(
                        "senderMessageToken" to token,
                        "tokenUpdatedAt" to dateString
                    )
                )

            println(" Message token updated successfully for user: ${user.email} (${user.id})")
            onSuccess()

        } catch (e: Exception) {
            println(" Error updating message token: ${e.message}")
            onError("Error updating message token: ${e.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun sendPasswordResetEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        return try {
            // First verify the user exists
            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")
            val userQuery = fvUserCollection
                .where { "email" equalTo email.trim().lowercase() }
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("No account found with this email address")
            }

            // Generate a random 6-digit OTP
            val otp = generateOtp()

            // Create OTP request
            val otpRequest = OTPRequest(
                email = email.trim(),
                otp = otp.trim()
            )

            val callable = functions.httpsCallable("postOtpEmail")
            val result = callable(otpRequest)

            // Parse the response
            val responseData = result.data(OtpResponse.serializer())

            // Create OTP record for password reset
            val otpRecord = OtpVerification(
                id = responseData.email,
                email = responseData.email,
                otp = responseData.otp,
                purpose = "PasswordReset",
                userId = ""
            )

            // Save OTP to Firestore
            Firebase.firestore.collection(collectionPath = "otp_verifications")
                .document(otpRecord.id).set(otpRecord)

            onSuccess("Password reset code has been sent to your email")

        } catch (e: Exception) {
            onError("Failed to send password reset email: ${e.message}")
        }
    }

    override suspend fun changePassword(
        email: String,
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")

            // Find user by email
            val userQuery = fvUserCollection
                .where { "email" equalTo email.trim().lowercase() }
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("User not found")
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            // Verify current password
            val hashedPasscode = user.hashedPasscode
            val salt = user.salt

            if (hashedPasscode.isNullOrEmpty() || salt.isNullOrEmpty()) {
                return onError("Account was created with social login. Please use 'Forgot Password' instead.")
            }

            if (!verifyPassword(currentPassword.trim(), hashedPasscode, salt)) {
                return onError("Current password is incorrect")
            }

            // Generate new salt and hash for new password
            val newSalt = generateSalt()
            val newHashedPassword = hashPassword(newPassword.trim(), newSalt)

            // Update password in Firestore
            fvUserCollection
                .document(user.id)
                .update(
                    mapOf(
                        "hashedPasscode" to newHashedPassword,
                        "salt" to newSalt,
                        "lastLogin" to dateString
                    )
                )

            onSuccess()

        } catch (e: Exception) {
            onError("Error while changing password: ${e.message}")
        }
    }

    override suspend fun resetPassword(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val fvUserCollection = Firebase.firestore.collection(collectionPath = "user")

            val userQuery = fvUserCollection
                .where { "email" equalTo email.trim().lowercase() }
                .get()

            if (userQuery.documents.isEmpty()) {
                return onError("User not found")
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            val newSalt = generateSalt()
            val newHashedPassword = hashPassword(newPassword.trim(), newSalt)

            fvUserCollection
                .document(user.id)
                .update(
                    mapOf(
                        "hashedPasscode" to newHashedPassword,
                        "salt" to newSalt,
                        "passCode" to newPassword,
                        "lastLogin" to dateString
                    )
                )

            onSuccess()
        } catch (e: Exception) {
            onError("Error while resetting password: ${e.message}")
        }
    }
}
