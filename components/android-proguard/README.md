# Android ProGuard

R8/ProGuard setup for Android KMP projects: minification, resource shrinking, and keep rules for Compose, Kotlin Serialization, Firebase Auth, AndroidX Credentials, and Multiplatform Settings. Includes stack-trace preservation for crash reporting.

Tags: `android`, `proguard`, `r8`, `release`, `build`

## Files

| File | Platform |
|---|---|
| [android/proguard-rules.pro](android/proguard-rules.pro) | android |
| [android/build.gradle.kts](android/build.gradle.kts) | android |

## Setup

### 1. Enable minification in the release build type

*Android*

```
In androidApp/build.gradle.kts inside android { buildTypes }:

  getByName("release") {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
      getDefaultProguardFile("proguard-android-optimize.txt"),
      file("proguard-rules.pro")
    )
  }

proguard-android-optimize.txt enables extra R8 optimisations on top
of the defaults. isShrinkResources removes unused drawables/layouts.
```

### 2. Create androidApp/proguard-rules.pro

*Android*

```
Create the file next to androidApp/build.gradle.kts.
Copy the rules from the Code tab — they cover every dependency
in a standard KMP Compose project:

• Kotlin + Coroutines
• Kotlin Serialization (@Serializable classes)
• Compose Multiplatform
• AndroidX Lifecycle, Navigation, Startup
• AndroidX Credentials / Google Sign-In
• Firebase Auth (gitlive wrapper)
• Multiplatform Settings
```

### 3. Keep serializable and data classes used across the KMP boundary

*Android*

```
R8 can rename or remove classes that are only referenced via
reflection or serialization. Add explicit -keep rules for any
data classes you serialise with kotlinx.serialization:

  -keep @kotlinx.serialization.Serializable class * { *; }

This rule is already included in the provided proguard-rules.pro.
```

### 4. Preserve stack traces for crash reporting

*Android*

```
The provided rules include:

  -keepattributes SourceFile,LineNumberTable
  -renamesourcefileattribute SourceFile

This keeps line numbers in stack traces while still renaming
source file names to the literal string "SourceFile", hiding
your original file structure from reverse engineers.
```

### 5. Test the release build after enabling ProGuard

*Android*

```
Build a release APK/AAB and run it through your full app flow:

  ./gradlew :androidApp:assembleRelease

Common issues after enabling R8:
• Missing -keep rules for reflection-used classes
• Gson / kotlinx.serialization models stripped
• Firebase or Credential Manager flows silently broken

Check logcat and crash reports — stack traces are readable
because we kept SourceFile,LineNumberTable.
```

---

Run it live and tweak the parameters in [Glaze](https://glaze.lightscout.co.uk) · [Google Play](https://play.google.com/store/apps/details?id=org.lightscout.candybox)
