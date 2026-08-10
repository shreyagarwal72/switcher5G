package com.app.switcher5g.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.app.switcher5g.network.NetworkMode

/**
import android.content.Context
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
        private const val KEY_AUTO_SCAN_SIMS = "auto_scan_sims"
        private const val KEY_USE_WHEEL_PICKER = "use_wheel_picker"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_USE_DYNAMIC_THEME = "use_dynamic_theme"
        private const val KEY_ENABLE_ANIMATIONS = "enable_animations"
    }
}
