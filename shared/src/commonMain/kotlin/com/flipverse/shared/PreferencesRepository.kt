package com.flipverse.shared


import com.flipverse.shared.domain.FlipNomenclatures
import com.flipverse.shared.domain.User
import com.flipverse.shared.util.DateFormatter
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSettingsApi::class)
object PreferencesRepository {
    private val settings: ObservableSettings = Settings().makeObservable()

    private const val IS_SUCCESS = "isSuccess_paypal"
    private const val ERROR = "error_paypal"
    private const val TOKEN = "token_paypal"

    private const val ID = "id"
    private const val EMAIL = "email"
    private const val CREATED_AT = "createdAt"
    private const val PASSWORD = "password"
    private const val FIRSTNAME = "first_name"
    private const val LASTNAME = "last_name"
    private const val FULLNAME = "full_name"
    private const val USERNAME = "username"
    private const val BIO = "bio"
    private const val THUMBNAIL = "thumbnail"
    private const val AVATAR = "avatar"
    private const val IS_FIRST_TIME = "is_first_time"
    private const val APP_THEME = "app_theme_preference"
    private const val FONT_SIZE_SCALE = "font_size_scale"

    private val json = Json { encodeDefaults = true }


    fun savePayPalData(
        isSuccess: Boolean?,
        error: String?,
        token: String?,
    ) {
        isSuccess?.let { settings.putBoolean(IS_SUCCESS, it) }
        error?.let { settings.putString(ERROR, it) }
        token?.let { settings.putString(TOKEN, it) }
    }


    fun saveId(email: String?) {
        email?.let { settings.putString(ID, it) }
    }


    fun getId(): String {
        return getString(ID, "")
    }

    fun saveCreatedAt(dateString: String?) {
        dateString?.let { settings.putString(CREATED_AT, it) }
    }

    fun getCreatedAt(): String {
        return getString(CREATED_AT, "")
    }

    fun getFormattedCreatedAt(): String {
        val createdAt = getString(CREATED_AT, "")
        return DateFormatter.formatToReadableDate(createdAt)
    }

    fun saveEmail(email: String?) {
        email?.let { settings.putString(EMAIL, it) }
    }

    fun getEmail(): String {
        return getString(EMAIL, "")
    }

    fun saveThumbnail(thumbnail: String?) {
        thumbnail?.let { settings.putString(THUMBNAIL, it) }
    }

    fun getThumbnail(): String {
        return getString(THUMBNAIL, "")
    }

    fun hasValidThumbnail(): Boolean {
        val url = getString(THUMBNAIL, "")
        return url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
    }

    fun saveAvatar(thumbnail: String?) {
        thumbnail?.let { settings.putString(AVATAR, it) }
    }

    fun getAvatar(): String {
        return getString(AVATAR, "")
    }

    fun saveFirstTimeLoginStatus(isFirstTimeLogin: Boolean?) {
        isFirstTimeLogin?.let { settings.putBoolean(IS_FIRST_TIME, it) }
    }

    fun getFirstTimeLoginStatus(): Boolean {
        return settings.getBoolean(IS_FIRST_TIME, true)
    }


    fun savePassword(password: String?) {
        password?.let { settings.putString(PASSWORD, it) }
    }

    fun getPassword(): String {
        return getString(PASSWORD, "")
    }

    fun saveFirstName(firstName: String?) {
        firstName?.let { settings.putString(FIRSTNAME, it) }

    }

    fun getFirstName(): String {
        return getString(FIRSTNAME, "")
    }

    fun saveLastName(lastName: String?) {
        lastName?.let { settings.putString(LASTNAME, it) }

    }

    fun getLastName(): String {
        return getString(LASTNAME, "")
    }

    fun saveFullName(lastName: String?) {
        lastName?.let { settings.putString(FULLNAME, it) }

    }

    fun getFullName(): String {
        return getString(FULLNAME, "")
    }

    fun saveUsername(username: String?) {
        username?.let { settings.putString(USERNAME, it) }
    }

    fun getUsername(): String {
        return getString(USERNAME, "")
    }

    fun saveBio(bio: String?) {
        bio?.let { settings.putString(BIO, it) }
    }

    fun getBio(): String {
        return getString(BIO, "")
    }

    fun saveSuggestedUsers(users: List<User>) {
        val jsonString = json.encodeToString(ListSerializer(User.serializer()), users)
        putString("suggested_users_key", jsonString)
    }

    fun loadSuggestedUsers(): List<User> {
        val jsonString = getString("suggested_users_key", "[]")
        return json.decodeFromString(ListSerializer(User.serializer()), jsonString)
    }

    fun saveFlipGenres(genres: List<String>) {
        val genresList = FlipNomenclatures.FlipGenres(genres)
        val jsonString =
            json.encodeToString(FlipNomenclatures.FlipGenres.serializer(), genresList)
        putString("genres_key", jsonString)
    }

    fun saveFlipInterests(interests: List<String>) {
        val interestsList = FlipNomenclatures.FlipInterests(interests)
        val jsonString =
            json.encodeToString(FlipNomenclatures.FlipInterests.serializer(), interestsList)
        putString("interests_key", jsonString)
    }

    fun saveFlipAccounts(accounts: List<String>) {
        val accountsList = FlipNomenclatures.SelectedFlipAccounts(accounts)
        val jsonString =
            json.encodeToString(FlipNomenclatures.SelectedFlipAccounts.serializer(), accountsList)
        putString("accounts_key", jsonString)
    }

    fun loadFlipAccounts(): List<String> {
        val jsonString = getString("accounts_key", "")
        if (jsonString.isBlank() || jsonString == "[]") return emptyList()
        return try {
            json.decodeFromString(
                FlipNomenclatures.SelectedFlipAccounts.serializer(),
                jsonString
            ).accounts
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadFlipInterests(): List<String> {
        val jsonString = getString("interests_key", "")
        if (jsonString.isBlank() || jsonString == "[]") return emptyList()
        return try {
            json.decodeFromString(
                FlipNomenclatures.FlipInterests.serializer(),
                jsonString
            ).name
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadFlipGenres(): List<String> {
        val jsonString = getString("genres_key", "")
        if (jsonString.isBlank() || jsonString == "[]") return emptyList()
        return try {
            json.decodeFromString(
                FlipNomenclatures.FlipGenres.serializer(),
                jsonString
            ).genres
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveThemePreference(themeKey: String) {
        putString(APP_THEME, themeKey)
    }

    fun getThemePreference(): String {
        return getString(APP_THEME, "system_default")
    }

    fun saveFontSizeScale(scale: Float) {
        settings.putFloat(FONT_SIZE_SCALE, scale)
    }

    fun getFontSizeScale(): Float {
        return settings.getFloat(FONT_SIZE_SCALE, 1.0f)
    }

    fun getString(key: String, defaultValue: String): String {
        return settings.getString(key, defaultValue)
    }

    fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    fun remove(key: String) {
        settings.remove(key)
    }

    fun reset() {
        settings.clear()
    }
}