import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildkonfig)
    // Removed CocoaPods plugin to avoid Ruby dependency issues
    // kotlin("native.cocoapods")
}

kotlin {
    androidTarget {
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
            baseName = "data"
            isStatic = true
            binaryOption("bundleId", "com.flipverse.data")

            // Pass CocoaPods framework search paths to the Kotlin/Native linker
            linkerOpts(cocoapodsLinkerOpts())
        }
    }

    // CocoaPods configuration removed to avoid Ruby dependency issues
    // Firebase Crashlytics for iOS will need to be added directly in Xcode if needed
    // cocoapods {
    //     version = "1.0.0"
    //     summary = "FlipVerse Data Module"
    //     homepage = "https://flipverse.com"
    //     ios.deploymentTarget = "14.0"
    //
    //     pod("FirebaseCrashlytics") {
    //         version = "10.19.0"
    //     }
    // }

//    jvm("desktop")

    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        androidMain.dependencies {
            // Firebase Crashlytics for Android
            implementation(project.dependencies.platform(libs.google.firebase.bom))
            implementation(libs.google.firebase.crashlytics)
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

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.firebase.firestore)
            implementation(libs.auth.firebase.kmp)
            implementation(project(path = ":shared"))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.core)

            implementation(libs.bundles.ktor)
            implementation(libs.bundles.coil)

            // Datetime
            implementation(libs.kotlinx.datetime)

            // Crypto for KMP
            implementation(libs.krypto)

            // UUID generation
            implementation(libs.uuid)

            implementation(libs.firebase.functions)
            implementation(libs.firebase.app)
            implementation(libs.firebase.common)
            implementation(libs.firebase.storage)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.flipverse.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Read API keys from local.properties (gitignored — safe for secrets)
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

buildkonfig {
    packageName = "com.flipverse.data"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "GOOGLE_BOOKS_API_KEY", localProperties.getProperty("GOOGLE_BOOKS_API_KEY", ""))
    }
}
