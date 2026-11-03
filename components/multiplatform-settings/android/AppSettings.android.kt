import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

// applicationContext is provided by a startup Initializer
actual fun createSettings(): Settings =
    SharedPreferencesSettings(
        applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    )
