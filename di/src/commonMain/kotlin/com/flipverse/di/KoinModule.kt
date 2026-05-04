package com.flipverse.di

import com.flipverse.auth.AuthViewModel
import com.flipverse.dashboard.DashboardViewModel
import com.flipverse.data.BooksRepositoryImpl
import com.flipverse.data.ChatRepositoryImpl
import com.flipverse.data.FeedRepositoryImpl
import com.flipverse.data.LibraryRepositoryImpl
import com.flipverse.data.LiveBookRepositoryImpl
import com.flipverse.data.NomenclatureRepositoryImpl
import com.flipverse.data.PostRepositoryImpl
import com.flipverse.data.UserRepositoryImpl
import com.flipverse.data.domain.BooksRepository
import com.flipverse.data.domain.ChatRepository
import com.flipverse.data.domain.FeedRepository
import com.flipverse.data.domain.LibraryRepository
import com.flipverse.data.domain.LiveBookRepository
import com.flipverse.data.domain.NomenclatureRepository
import com.flipverse.data.domain.PostRepository
import com.flipverse.data.domain.UserRepository
import com.flipverse.explore.ExploreViewModel
import com.flipverse.explore.LibraryViewModel
import com.flipverse.explore.BookStoreViewModel
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.userprofile.UserProfileViewModel
import com.flipverse.userprofile.ChangePasswordViewModel
import com.flipverse.chat.ChatViewModel
import com.flipverse.chat.avatar.AvatarRepository
import com.flipverse.chat.avatar.AvatarRepositoryImpl
import com.flipverse.chat.avatar.AvatarSelectionViewModel
import com.flipverse.data.NotificationRepositoryImpl
import com.flipverse.data.domain.NotificationRepository
import com.flipverse.notification.NotificationViewModel
import com.flipverse.shared.navigation.NotificationNavigationBridge
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val sharedModule = module {
    // HttpClient for network requests
    single<HttpClient> {
        HttpClient {
            expectSuccess = false // Handle HTTP errors manually for better diagnostics
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    isLenient = true
                })
            }
        }
    }

    single<UserRepository> { UserRepositoryImpl() }
    single<PostRepository> { PostRepositoryImpl() }
    single<FeedRepository> { FeedRepositoryImpl() }
    single<NomenclatureRepository> { NomenclatureRepositoryImpl() }
    single<LiveBookRepository> { LiveBookRepositoryImpl() }
    single<ChatRepository> { ChatRepositoryImpl() }
    single<AvatarRepository> { AvatarRepositoryImpl() }
    single<NotificationRepository> { NotificationRepositoryImpl() }
    single<BooksRepository> { BooksRepositoryImpl(get()) }
    single<LibraryRepository> { LibraryRepositoryImpl() }
    viewModelOf(::AuthViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::UserProfileViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::LiveBookViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::AvatarSelectionViewModel)
    viewModelOf(::NotificationViewModel)
    viewModelOf(::LibraryViewModel)
    viewModel { ExploreViewModel(get())}
    viewModel { BookStoreViewModel(get()) }
}

expect val targetModule: Module

// Guards against double-initialization. On iOS, ComposeUIViewController's configure
// block can be invoked more than once (e.g. SwiftUI view recreation), which would
// throw KoinAlreadyStartedException and crash the app at launch.
private var koinInitialized = false

/**
 * Platform-specific notification initialization
 * Android: Sets up KMPNotifier listeners
 * iOS: Uses native APNs (handled in Swift)
 */
expect fun KoinApplication.setupPlatformNotifications()

private var notificationListenerRegistered = false

fun initializeKoin(
    config: (KoinApplication.() -> Unit)? = null
) {
    if (koinInitialized) {
        println("⚠️ Koin already initialized — skipping duplicate startKoin call")
        return
    }
    koinInitialized = true
    startKoin {
        config?.invoke(this)
        modules(
            sharedModule, targetModule
        )
        onApplicationStart()
    }
}

private fun KoinApplication.onApplicationStart() {
    println("KoinApplication started")
    
    // Platform-specific notification setup (Android: KMPNotifier, iOS: Native)
    setupPlatformNotifications()
}