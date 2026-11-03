import com.russhwolf.settings.Settings

expect fun createSettings(): Settings

object AppSettings {
    private val settings: Settings by lazy { createSettings() }

    var hasSeenWelcome: Boolean
        get() = settings.getBoolean("has_seen_welcome", false)
        set(value) = settings.putBoolean("has_seen_welcome", value)
}
