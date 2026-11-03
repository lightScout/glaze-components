# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-dontwarn org.jetbrains.compose.ui.tooling.preview.**

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# AndroidX Navigation
-keep class androidx.navigation.** { *; }

# AndroidX Credentials / Google Sign-In
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Firebase Auth (gitlive wrapper + underlying Google SDK)
-keep class dev.gitlive.firebase.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Multiplatform Settings
-keep class com.russhwolf.settings.** { *; }

# AndroidX Startup
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer { *; }
-keepnames class * implements androidx.startup.Initializer

# Suppress common third-party warnings
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Preserve stack traces for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
