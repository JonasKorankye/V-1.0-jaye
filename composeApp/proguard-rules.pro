# Conservative R8 rules for Android release builds.
# Goal: enable shrinking/obfuscation without affecting Compose UI,
# navigation, serialization, DI, Firebase, or background components.

# Keep shrinking enabled, but avoid aggressive transformations that are more
# likely to affect runtime behavior or dramatically increase memory pressure.
-dontoptimize
-dontobfuscate

# Keep source/line metadata useful for crash reports and debugging.
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,EnclosingMethod,Signature

# Preserve Kotlin metadata used by reflection/serialization tooling.
-keep class kotlin.Metadata { *; }

# Compose / generated classes
-dontwarn androidx.compose.**
-dontwarn org.jetbrains.compose.**

# Navigation routes and Kotlin serialization models
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn kotlinx.serialization.**

# Keep app navigation models explicitly since routes are serialized.
-keep class com.flipverse.shared.navigation.** { *; }

# Koin dependency injection / reflection-sensitive construction
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Coroutines / atomicfu internals
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.atomicfu.**

# Firebase / Google Play Services
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn dev.gitlive.firebase.**

# Room / SQLite
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.**

# Keep Android components that may be instantiated by the framework.
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep classes referenced from the manifest.
-keep class com.golda.flipverse.** { *; }

# pdfbox-android: JP2 decoder is an optional native codec not present at runtime.
-dontwarn com.gemalto.jp2.JP2Decoder

# Coil3 + Ktor2: NIO channel write helper is absent on Android's JVM subset.
-dontwarn io.ktor.utils.io.jvm.nio.WritingKt
