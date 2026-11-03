# Multiplatform Settings

Persist key-value preferences across Android (SharedPreferences) and iOS (NSUserDefaults) from shared KMP code. One expect/actual factory, one common wrapper — zero platform boilerplate in your ViewModels.

Tags: `settings`, `preferences`, `storage`, `kmp`, `android`, `ios`

## Files

| File | Platform |
|---|---|
| [common/AppSettings.kt](common/AppSettings.kt) | common |
| [android/AppSettings.android.kt](android/AppSettings.android.kt) | android |
| [ios/AppSettings.ios.kt](ios/AppSettings.ios.kt) | ios |

## Setup

### 1. Add multiplatform-settings to the version catalog

*KMP*

```
In gradle/libs.versions.toml:

  [versions]
  multiplatform-settings = "1.3.0"

  [libraries]
  multiplatform-settings = { module =
    "com.russhwolf:multiplatform-settings",
    version.ref = "multiplatform-settings" }
```

### 2. Add to commonMain dependencies

*KMP*

```
In shared/build.gradle.kts inside sourceSets { commonMain.dependencies }:

  implementation(libs.multiplatform.settings)

The library is fully multiplatform — one dependency covers
Android, iOS, JS, and WasmJS.
```

### 3. Declare expect fun createSettings()

*KMP*

```
In commonMain/settings/AppSettings.kt declare:

  expect fun createSettings(): Settings

Then wrap it in a singleton object:

  object AppSettings {
    private val settings by lazy { createSettings() }
    var myKey: Boolean
      get() = settings.getBoolean("my_key", false)
      set(v) = settings.putBoolean("my_key", v)
  }
```

### 4. Implement Android actual with SharedPreferences

*Android*

```
In androidMain/settings/AppSettings.android.kt:

  actual fun createSettings(): Settings =
    SharedPreferencesSettings(
      applicationContext.getSharedPreferences(
        "app_prefs", Context.MODE_PRIVATE
      )
    )

applicationContext must be injected before first use.
Use an androidx.startup.Initializer to capture it on app launch.
```

### 5. Implement iOS actual with NSUserDefaults

*iOS*

```
In iosMain/settings/AppSettings.ios.kt:

  actual fun createSettings(): Settings =
    NSUserDefaultsSettings(
      NSUserDefaults.standardUserDefaults
    )

No extra iOS setup needed — NSUserDefaults is available
from platform.Foundation out of the box.
```

---

Run it live and tweak the parameters in [Glaze](https://glaze.lightscout.co.uk) · [Google Play](https://play.google.com/store/apps/details?id=org.lightscout.candybox)
