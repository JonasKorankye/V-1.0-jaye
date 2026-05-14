import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.room)
    alias(libs.plugins.crashlytics)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "com.flipverse.composeApp")
            export(project(":shared"))

            // Pass CocoaPods framework search paths to the Kotlin/Native linker
            linkerOpts(cocoapodsLinkerOpts())
        }
    }

//    jvm("desktop")

    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.ui)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.splash.screen)
            implementation(libs.koin.android)
            implementation(libs.androidx.lifecycle.process)

            implementation(libs.firebase.common.ktx)
            implementation(libs.firebase.auth.ktx)
            api(libs.google.firebase.common)
            implementation(libs.kotlinx.coroutines.play.services)

            implementation(libs.play.services.base)
            implementation(libs.pdfbox.android)
            
            // KMPNotifier only on Android (iOS uses native implementation)
            implementation(libs.kmp.notifier)

            implementation(project.dependencies.platform(libs.google.firebase.bom))
            implementation(libs.google.firebase.functions)
            implementation(libs.google.firebase.crashlytics)
            // Firebase Messaging for Android only (not the GitLive KMP version)
            implementation(libs.firebase.messaging)

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.core)
            implementation(libs.auth.kmp)
            implementation(libs.firebase.app)
            implementation(libs.firebase.common)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.crashlytics)
            implementation(libs.auth.firebase.kmp)

            // KMPNotifier moved to androidMain only

            implementation(project(path = ":navigation"))
            api(project(":shared"))  // Changed to api so it can be exported to iOS
            implementation(project(path = ":di"))
            implementation(project(path = ":data"))

            implementation(libs.bundles.ktor)
            implementation(libs.bundles.coil)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.golda.flipverse"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.golda.flipverse"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 5
        versionName = "2.4"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("../flipverse-release.jks")
            storePassword = "flipverse123"
            keyAlias = "flipverse"
            keyPassword = "flipverse123"
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
