package com.app.switcher5g.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.ui.theme.ColorStyle

enum class ActivationMethod { AUTO, SHIZUKU, ROOT, DIRECT_ADB, RADIO_INFO }
enum class AppThemeMode { SYSTEM, DARK, LIGHT }
enum class AppFont { SYSTEM, NUNITO, INTER, OUTFIT, LEXEND, MANROPE, GROTESK }

/**
 * Reactive SharedPreferences wrapper backed by Compose mutableStateOf properties.
 * Setting any preference instantly triggers an automatic app-wide re-render / theme refresh.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("switcher5g_settings", Context.MODE_PRIVATE)

    var activationMethod: ActivationMethod
        get() = activationMethodState
        set(value) {
            activationMethodState = value
            prefs.edit().putString(KEY_ACTIVATION_METHOD, value.name).apply()
        }

    var defaultNetworkMode: NetworkMode
        get() = defaultNetworkModeState
        set(value) {
            defaultNetworkModeState = value
            prefs.edit().putString(KEY_DEFAULT_MODE, value.name).apply()
        }

    var themeMode: AppThemeMode
        get() = themeModeState
        set(value) {
            themeModeState = value
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var amoled: Boolean
        get() = amoledState
        set(value) {
            amoledState = value
            prefs.edit().putBoolean(KEY_AMOLED, value).apply()
        }

    var paletteId: String
        get() = paletteIdState
        set(value) {
            paletteIdState = value
            prefs.edit().putString(KEY_PALETTE_ID, value).apply()
        }

    var colorStyle: ColorStyle
        get() = colorStyleState
        set(value) {
            colorStyleState = value
            prefs.edit().putString(KEY_COLOR_STYLE, value.name).apply()
        }

    var appFont: AppFont
        get() = appFontState
        set(value) {
            appFontState = value
            prefs.edit().putString(KEY_APP_FONT, value.name).apply()
        }

    var autoScanSims: Boolean
        get() = autoScanSimsState
        set(value) {
            autoScanSimsState = value
            prefs.edit().putBoolean(KEY_AUTO_SCAN_SIMS, value).apply()
        }

    var hasDismissedSetupCard: Boolean
        get() = hasDismissedSetupCardState
        set(value) {
            hasDismissedSetupCardState = value
            prefs.edit().putBoolean(KEY_HAS_DISMISSED_SETUP, value).apply()
        }

    var autoCheckUpdates: Boolean
        get() = autoCheckUpdatesState
        set(value) {
            autoCheckUpdatesState = value
            prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, value).apply()
        }

    var useDynamicTheme: Boolean
        get() = useDynamicThemeState
        set(value) {
            useDynamicThemeState = value
            prefs.edit().putBoolean(KEY_USE_DYNAMIC_THEME, value).apply()
        }

    var enableAnimations: Boolean
        get() = enableAnimationsState
        set(value) {
            enableAnimationsState = value
            prefs.edit().putBoolean(KEY_ENABLE_ANIMATIONS, value).apply()
        }

    var hasSeenManual5gDialog: Boolean
        get() = hasSeenManual5gDialogState
        set(value) {
            hasSeenManual5gDialogState = value
            prefs.edit().putBoolean(KEY_HAS_SEEN_MANUAL_5G_DIALOG, value).apply()
        }

    // Reactive Compose States
    var activationMethodState by mutableStateOf(readActivationMethod())
        private set

    var defaultNetworkModeState by mutableStateOf(readDefaultNetworkMode())
        private set

    var themeModeState by mutableStateOf(readThemeMode())
        private set

    var amoledState by mutableStateOf(readAmoled())
        private set

    var paletteIdState by mutableStateOf(readPaletteId())
        private set

    var colorStyleState by mutableStateOf(readColorStyle())
        private set

    var appFontState by mutableStateOf(readAppFont())
        private set

    var autoScanSimsState by mutableStateOf(readAutoScanSims())
        private set

    var hasDismissedSetupCardState by mutableStateOf(readHasDismissedSetupCard())
        private set

    var autoCheckUpdatesState by mutableStateOf(readAutoCheckUpdates())
        private set

    var useDynamicThemeState by mutableStateOf(readUseDynamicTheme())
        private set

    var enableAnimationsState by mutableStateOf(readEnableAnimations())
        private set

    var hasSeenManual5gDialogState by mutableStateOf(readHasSeenManual5gDialog())
        private set

    private fun readActivationMethod(): ActivationMethod {
        val name = prefs.getString(KEY_ACTIVATION_METHOD, ActivationMethod.AUTO.name)
        return runCatching { ActivationMethod.valueOf(name ?: ActivationMethod.AUTO.name) }.getOrDefault(ActivationMethod.AUTO)
    }

    private fun readDefaultNetworkMode(): NetworkMode {
        val name = prefs.getString(KEY_DEFAULT_MODE, NetworkMode.NR_LTE.name)
        return runCatching { NetworkMode.valueOf(name ?: NetworkMode.NR_LTE.name) }.getOrDefault(NetworkMode.NR_LTE)
    }

    private fun readThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name) }.getOrDefault(AppThemeMode.SYSTEM)
    }

    private fun readAmoled(): Boolean = prefs.getBoolean(KEY_AMOLED, false)

    private fun readPaletteId(): String = prefs.getString(KEY_PALETTE_ID, "tide") ?: "tide"

    private fun readColorStyle(): ColorStyle {
        val name = prefs.getString(KEY_COLOR_STYLE, ColorStyle.TONAL_SPOT.name)
        return runCatching { ColorStyle.valueOf(name ?: ColorStyle.TONAL_SPOT.name) }.getOrDefault(ColorStyle.TONAL_SPOT)
    }

    private fun readAppFont(): AppFont {
        val name = prefs.getString(KEY_APP_FONT, AppFont.SYSTEM.name)
        return runCatching { AppFont.valueOf(name ?: AppFont.SYSTEM.name) }.getOrDefault(AppFont.SYSTEM)
    }

    private fun readAutoScanSims(): Boolean = prefs.getBoolean(KEY_AUTO_SCAN_SIMS, true)

    private fun readHasDismissedSetupCard(): Boolean = prefs.getBoolean(KEY_HAS_DISMISSED_SETUP, false)

    private fun readAutoCheckUpdates(): Boolean = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true)

    private fun readUseDynamicTheme(): Boolean = prefs.getBoolean(KEY_USE_DYNAMIC_THEME, true)

    private fun readEnableAnimations(): Boolean = prefs.getBoolean(KEY_ENABLE_ANIMATIONS, true)

    private fun readHasSeenManual5gDialog(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_MANUAL_5G_DIALOG, false)

    var toggleMode1: NetworkMode
        get() = toggleMode1State
        set(value) {
            toggleMode1State = value
            prefs.edit().putString(KEY_TOGGLE_MODE_1, value.name).apply()
            notifyTileUpdate()
        }

    var toggleMode2: NetworkMode
        get() = toggleMode2State
        set(value) {
            toggleMode2State = value
            prefs.edit().putString(KEY_TOGGLE_MODE_2, value.name).apply()
            notifyTileUpdate()
        }

    var currentActiveMode: NetworkMode
        get() = currentActiveModeState
        set(value) {
            currentActiveModeState = value
            prefs.edit().putString(KEY_CURRENT_ACTIVE_MODE, value.name).apply()
            notifyTileUpdate()
        }

    var toggleMode1State by mutableStateOf(readToggleMode1())
        private set

    var toggleMode2State by mutableStateOf(readToggleMode2())
        private set

    var currentActiveModeState by mutableStateOf(readCurrentActiveMode())
        private set

    private fun readToggleMode1(): NetworkMode {
        val name = prefs.getString(KEY_TOGGLE_MODE_1, NetworkMode.NR_LTE.name)
        return runCatching { NetworkMode.valueOf(name ?: NetworkMode.NR_LTE.name) }.getOrDefault(NetworkMode.NR_LTE)
    }

    private fun readToggleMode2(): NetworkMode {
        val name = prefs.getString(KEY_TOGGLE_MODE_2, NetworkMode.LTE_ONLY.name)
        return runCatching { NetworkMode.valueOf(name ?: NetworkMode.LTE_ONLY.name) }.getOrDefault(NetworkMode.LTE_ONLY)
    }

    private fun readCurrentActiveMode(): NetworkMode {
        val name = prefs.getString(KEY_CURRENT_ACTIVE_MODE, defaultNetworkMode.name)
        return runCatching { NetworkMode.valueOf(name ?: defaultNetworkMode.name) }.getOrDefault(defaultNetworkMode)
    }

    fun notifyTileUpdate() {
        try {
            val intent = android.content.Intent(ACTION_NETWORK_MODE_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}
    }

    fun exportToJsonString(): String {
        val json = org.json.JSONObject()
        json.put("activationMethod", activationMethod.name)
        json.put("defaultNetworkMode", defaultNetworkMode.name)
        json.put("toggleMode1", toggleMode1.name)
        json.put("toggleMode2", toggleMode2.name)
        json.put("currentActiveMode", currentActiveMode.name)
        json.put("themeMode", themeMode.name)
        json.put("amoled", amoled)
        json.put("paletteId", paletteId)
        json.put("colorStyle", colorStyle.name)
        json.put("appFont", appFont.name)
        json.put("autoScanSims", autoScanSims)
        json.put("hasDismissedSetupCard", hasDismissedSetupCard)
        json.put("autoCheckUpdates", autoCheckUpdates)
        json.put("useDynamicTheme", useDynamicTheme)
        json.put("enableAnimations", enableAnimations)
        json.put("hasSeenManual5gDialog", hasSeenManual5gDialog)
        return json.toString(2)
    }

    fun importFromJsonString(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)
            if (json.has("activationMethod")) {
                runCatching { activationMethod = ActivationMethod.valueOf(json.getString("activationMethod")) }
            }
            if (json.has("defaultNetworkMode")) {
                runCatching { defaultNetworkMode = NetworkMode.valueOf(json.getString("defaultNetworkMode")) }
            }
            if (json.has("toggleMode1")) {
                runCatching { toggleMode1 = NetworkMode.valueOf(json.getString("toggleMode1")) }
            }
            if (json.has("toggleMode2")) {
                runCatching { toggleMode2 = NetworkMode.valueOf(json.getString("toggleMode2")) }
            }
            if (json.has("currentActiveMode")) {
                runCatching { currentActiveMode = NetworkMode.valueOf(json.getString("currentActiveMode")) }
            }
            if (json.has("themeMode")) {
                runCatching { themeMode = AppThemeMode.valueOf(json.getString("themeMode")) }
            }
            if (json.has("amoled")) {
                amoled = json.getBoolean("amoled")
            }
            if (json.has("paletteId")) {
                paletteId = json.getString("paletteId")
            }
            if (json.has("colorStyle")) {
                runCatching { colorStyle = ColorStyle.valueOf(json.getString("colorStyle")) }
            }
            if (json.has("appFont")) {
                runCatching { appFont = AppFont.valueOf(json.getString("appFont")) }
            }
            if (json.has("autoScanSims")) {
                autoScanSims = json.getBoolean("autoScanSims")
            }
            if (json.has("hasDismissedSetupCard")) {
                hasDismissedSetupCard = json.getBoolean("hasDismissedSetupCard")
            }
            if (json.has("autoCheckUpdates")) {
                autoCheckUpdates = json.getBoolean("autoCheckUpdates")
            }
            if (json.has("useDynamicTheme")) {
                useDynamicTheme = json.getBoolean("useDynamicTheme")
            }
            if (json.has("enableAnimations")) {
                enableAnimations = json.getBoolean("enableAnimations")
            }
            if (json.has("hasSeenManual5gDialog")) {
                hasSeenManual5gDialog = json.getBoolean("hasSeenManual5gDialog")
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        const val ACTION_NETWORK_MODE_CHANGED = "com.app.switcher5g.ACTION_NETWORK_MODE_CHANGED"
        private const val KEY_ACTIVATION_METHOD = "activation_method"
        private const val KEY_DEFAULT_MODE = "default_network_mode"
        private const val KEY_TOGGLE_MODE_1 = "toggle_mode_1"
        private const val KEY_TOGGLE_MODE_2 = "toggle_mode_2"
        private const val KEY_CURRENT_ACTIVE_MODE = "current_active_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AMOLED = "amoled"
        private const val KEY_PALETTE_ID = "palette_id"
        private const val KEY_COLOR_STYLE = "color_style"
        private const val KEY_APP_FONT = "app_font"
        private const val KEY_AUTO_SCAN_SIMS = "auto_scan_sims"
        private const val KEY_HAS_DISMISSED_SETUP = "has_dismissed_setup"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_USE_DYNAMIC_THEME = "use_dynamic_theme"
        private const val KEY_ENABLE_ANIMATIONS = "enable_animations"
        private const val KEY_HAS_SEEN_MANUAL_5G_DIALOG = "has_seen_manual_5g_dialog"
    }
}
