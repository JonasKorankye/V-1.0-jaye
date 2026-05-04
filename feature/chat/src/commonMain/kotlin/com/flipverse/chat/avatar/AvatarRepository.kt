package com.flipverse.chat.avatar

import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.RequestState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow

interface AvatarRepository {
    suspend fun generateAvatars(style: AvatarStyle, count: Int = 12): RequestState<List<Avatar>>
    suspend fun generateCustomizedAvatar(
        avatar: Avatar,
        customizations: AvatarCustomizations
    ): RequestState<Avatar>
    suspend fun getAvatarStyles(): RequestState<List<AvatarStyle>>
    suspend fun saveUserAvatar(userId: String, avatarUrl: String): RequestState<Unit>
    fun getUserAvatarFlow(userId: String): Flow<RequestState<String?>>
}

class AvatarRepositoryImpl : AvatarRepository {

    companion object {
        private const val DICEBEAR_BASE_URL = "https://api.dicebear.com/8.x"
    }

    private val firestore = Firebase.firestore
    private val userCollection = firestore.collection("user")

    override suspend fun generateAvatars(
        style: AvatarStyle,
        count: Int
    ): RequestState<List<Avatar>> {
        return try {
            val avatars = mutableListOf<Avatar>()

            // Generate random seeds for variety
            val seeds = generateRandomSeeds(count)

            seeds.forEachIndexed { index, seed ->
                val url = "$DICEBEAR_BASE_URL/${style.name}/png?seed=$seed&size=200"
                avatars.add(
                    Avatar(
                        id = "${style.name}_${seed}_$index",
                        url = url,
                        style = style.name,
                        seed = seed
                    )
                )
            }
            println("Generated avatars: $avatars")
            RequestState.Success(avatars)
        } catch (e: Exception) {
            RequestState.Error("Failed to generate avatars: ${e.message}")
        }
    }

    override suspend fun generateCustomizedAvatar(
        avatar: Avatar,
        customizations: AvatarCustomizations
    ): RequestState<Avatar> {
        return try {
            val baseUrl = "$DICEBEAR_BASE_URL/${avatar.style}/png?seed=${avatar.seed}&size=200"

            // Find the avatar style to get customization options
            val avatarStyle = AvatarStyle.DEFAULT_STYLES.find { it.name == avatar.style }
                ?: AvatarStyle.DEFAULT_STYLES.first()

            val customizationParams = customizations.toQueryParams(avatarStyle)
            val customizedUrl = baseUrl + customizationParams

            val customizedAvatar = avatar.copy(
                url = customizedUrl,
                customizations = customizations
            )

            println("Generated customized avatar: $customizedAvatar")
            RequestState.Success(customizedAvatar)
        } catch (e: Exception) {
            RequestState.Error("Failed to generate customized avatar: ${e.message}")
        }
    }

    override suspend fun getAvatarStyles(): RequestState<List<AvatarStyle>> {
        return try {
            RequestState.Success(AvatarStyle.DEFAULT_STYLES)
        } catch (e: Exception) {
            RequestState.Error("Failed to get avatar styles: ${e.message}")
        }
    }

    override suspend fun saveUserAvatar(userId: String, avatarUrl: String): RequestState<Unit> {
        return try {
            val email = PreferencesRepository.getEmail().trim()

            if (email.isEmpty()) {
                return RequestState.Error("User email not found in preferences")
            }

            // Find user by email (similar to other user operations)
            val userQuery = userCollection
                .where { "email" equalTo email.lowercase() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return RequestState.Error("User not found in database")
            }

            val userDoc = userQuery.documents.first()

            if (userDoc.exists) {
                // Update avatar field in Firebase user document
                userCollection
                    .document(userDoc.id)
                    .update("avatar" to avatarUrl)

                // Also save to local preferences
                PreferencesRepository.saveAvatar(avatarUrl)

                println("Avatar saved successfully for user: $email -> $avatarUrl")
                RequestState.Success(Unit)
            } else {
                RequestState.Error("User document does not exist")
            }
        } catch (e: Exception) {
            RequestState.Error("Failed to save avatar: ${e.message}")
        }
    }

    override fun getUserAvatarFlow(userId: String): Flow<RequestState<String?>> {
        // This would typically return a Flow from your database
        // For now, return empty flow
        return kotlinx.coroutines.flow.flow {
            emit(RequestState.Success(null))
        }
    }

    private fun generateRandomSeeds(count: Int): List<String> {
        val seeds = mutableListOf<String>()
        val possibleSeeds = listOf(
            "Abby", "Alex", "Angel", "Annie", "Aneka", "Ashley", "Bailey", "Bear",
            "Bella", "Bob", "Boots", "Charlie", "Chester", "Cookie", "Cuddles",
            "Diego", "Dusty", "Felix", "Fluffy", "Garfield", "George", "Ginger",
            "Gracie", "Henry", "Jack", "Jasmine", "Jax", "Kiki", "Leo", "Lucky",
            "Lucy", "Luna", "Max", "Mia", "Milo", "Mittens", "Molly", "Oliver",
            "Oreo", "Oscar", "Patches", "Princess", "Rocky", "Romeo", "Rosie",
            "Ruby", "Rusty", "Sam", "Shadow", "Simba", "Smokey", "Snow", "Snickers",
            "Sophie", "Tiger", "Toby", "Trouble", "Tucker", "Whiskers", "Ziggy"
        )

        // Shuffle and take required count, with fallback to random generation
        val shuffledSeeds = possibleSeeds.shuffled()

        repeat(count) { index ->
            if (index < shuffledSeeds.size) {
                seeds.add(shuffledSeeds[index])
            } else {
                // Generate random alphanumeric seed
                seeds.add(generateRandomSeed())
            }
        }

        return seeds
    }

    private fun generateRandomSeed(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
}