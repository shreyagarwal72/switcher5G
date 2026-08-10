package com.app.switcher5g.util

import android.content.Context
import android.content.SharedPreferences
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.ui.theme.ColorStyle

enum class AppThemeMode { SYSTEM, DARK, LIGHT }

/**
 * SharedPreferences wrapper for persisting user settings across app sessions.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("switcher5g_settings", Context.MODE_PRIVATE)

    var defaultNetworkMode: NetworkMode
        get() {
            val name = prefs.getString(KEY_DEFAULT_MODE, NetworkMode.NR_LTE.name)
            return runCatching { NetworkMode.valueOf(name ?: NetworkMode.NR_LTE.name) }
                .getOrDefault(NetworkMode.NR_LTE)
        }
        set(value) {
            prefs.edit().putString(KEY_DEFAULT_MODE, value.name).apply()
        }

    var themeMode: AppThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
            return runCatching { AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name) }
                .getOrDefault(AppThemeMode.SYSTEM)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var amoled: Boolean
        get() = prefs.getBoolean(KEY_AMOLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED, value).apply()

    var paletteId: String
        get() = prefs.getString(KEY_PALETTE_ID, "tide") ?: "tide"
        set(value) = prefs.edit().putString(KEY_PALETTE_ID, value).apply()

    var colorStyle: ColorStyle
        get() {
            val name = prefs.getString(KEY_COLOR_STYLE, ColorStyle.TONAL_SPOT.name)
            return runCatching { ColorStyle.valueOf(name ?: ColorStyle.TONAL_SPOT.name) }
                .getOrDefault(ColorStyle.TONAL_SPOT)
        }
        set(value) {
            prefs.edit().putString(KEY_COLOR_STYLE, value.name).apply()
        }

    var autoScanSims: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCAN_SIMS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SCAN_SIMS, value).apply()

    var useWheelPicker: Boolean
        get() = prefs.getBoolean(KEY_USE_WHEEL_PICKER, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_WHEEL_PICKER, value).apply()

    var autoCheckUpdates: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, value).apply()

    var useDynamicTheme: Boolean
        get() = prefs.getBoolean(KEY_USE_DYNAMIC_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_DYNAMIC_THEME, value).apply()

    var enableAnimations: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_ANIMATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_ANIMATIONS, value).apply()

    companion object {
        private const val KEY_DEFAULT_MODE = "default_network_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AMOLED = "amoled"
        private const val KEY_PALETTE_ID = "palette_id"
        private const val KEY_COLOR_STYLE = "color_style"
        private const val KEY_AUTO_SCAN_SIMS = "auto_scan_sims"
        private const val KEY_USE_WHEEL_PICKER = "use_wheel_picker"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_USE_DYNAMIC_THEME = "use_dynamic_theme"
        private const val KEY_ENABLE_ANIMATIONS = "enable_animations"
    }
}
