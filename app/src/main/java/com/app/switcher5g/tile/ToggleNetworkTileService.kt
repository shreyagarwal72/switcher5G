package com.app.switcher5g.tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.app.switcher5g.network.Manual5gSwitchHelper
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppLogger
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 2-Mode Quick Settings Toggle Tile.
 * Toggles between user-configured toggleMode1 and toggleMode2.
 * Shows active mode label and icon state.
 * Handles missing/revoked Shizuku or Root permissions gracefully with UNAVAILABLE tile state.
 * Listens for app-wide mode changes to keep UI and Tile in sync.
 */
class ToggleNetworkTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var appPrefs: AppPreferences
    private lateinit var manager: NetworkModeManager

    private val modeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AppPreferences.ACTION_NETWORK_MODE_CHANGED) {
                AppLogger.i("ToggleNetworkTileService", "Received mode update broadcast")
                updateTileState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appPrefs = AppPreferences(applicationContext)
        manager = NetworkModeManager(applicationContext)
        AppLogger.i("ToggleNetworkTileService", "Toggle Tile Service created")
    }

    override fun onStartListening() {
        super.onStartListening()
        registerModeReceiver()
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterModeReceiver()
    }

    private fun registerModeReceiver() {
        try {
            val filter = IntentFilter(AppPreferences.ACTION_NETWORK_MODE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(modeChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(modeChangeReceiver, filter)
            }
        } catch (_: Throwable) {}
    }

    private fun unregisterModeReceiver() {
        try {
            unregisterReceiver(modeChangeReceiver)
        } catch (_: Throwable) {}
    }

    override fun onClick() {
        super.onClick()

        val shizukuPermission = ShizukuHelper.hasPermission()
        val rootAvailable = RootHelper.isRootAvailable()
        val rootGranted = if (rootAvailable) RootHelper.isRootGranted() else false
        val preferredMethod = appPrefs.activationMethod

        val isAuthorized = when (preferredMethod) {
            ActivationMethod.SHIZUKU -> shizukuPermission
            ActivationMethod.ROOT -> rootGranted
            ActivationMethod.RADIO_INFO -> true
            else -> shizukuPermission || rootGranted
        }

        if (!isAuthorized) {
            renderPermissionErrorState()
            val msg = when (preferredMethod) {
                ActivationMethod.SHIZUKU -> "⚠️ Shizuku permission required to switch network mode."
                ActivationMethod.ROOT -> "⚠️ Root (su) permission required to switch network mode."
                else -> "⚠️ Permission required. Grant Shizuku or Root access in Switcher5G."
            }
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            return
        }

        val mode1 = appPrefs.toggleMode1
        val mode2 = appPrefs.toggleMode2
        val currentActive = appPrefs.currentActiveMode

        // Determine target mode to switch to
        val targetMode = if (currentActive == mode1) mode2 else mode1
        AppLogger.i("ToggleNetworkTileService", "Toggle tile clicked: active=$currentActive -> target=$targetMode (mode1=$mode1, mode2=$mode2)")

        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            subtitle = "Switching to ${formatModeShort(targetMode)}..."
            updateTile()
        }

        scope.launch {
            val targetMethod = when {
                preferredMethod == ActivationMethod.SHIZUKU && shizukuPermission -> ActivationMethod.SHIZUKU
                preferredMethod == ActivationMethod.ROOT && rootGranted -> ActivationMethod.ROOT
                preferredMethod == ActivationMethod.RADIO_INFO -> ActivationMethod.RADIO_INFO
                shizukuPermission -> ActivationMethod.SHIZUKU
                rootGranted -> ActivationMethod.ROOT
                else -> ActivationMethod.AUTO
            }

            val result = manager.switchTo(targetMode, method = targetMethod)

            if (result is SwitchResult.Success && result.message.contains("Radio Info")) {
                Manual5gSwitchHelper.openRadioInfo(this@ToggleNetworkTileService)
            }

            CoroutineScope(Dispatchers.Main).launch {
                when (result) {
                    is SwitchResult.Success -> {
                        AppLogger.i("ToggleNetworkTileService", "Toggle tile switch success: ${result.message}")
                        appPrefs.currentActiveMode = targetMode
                        updateTileState()
                        Toast.makeText(applicationContext, "✅ ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    is SwitchResult.Failure -> {
                        AppLogger.e("ToggleNetworkTileService", "Toggle tile switch failed: ${result.reason}")
                        updateTileState()
                        Toast.makeText(applicationContext, "⚠️ ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateTileState() {
        val shizukuPermission = ShizukuHelper.hasPermission()
        val rootAvailable = RootHelper.isRootAvailable()
        val rootGranted = if (rootAvailable) RootHelper.isRootGranted() else false
        val preferredMethod = appPrefs.activationMethod

        val isAuthorized = when (preferredMethod) {
            ActivationMethod.SHIZUKU -> shizukuPermission
            ActivationMethod.ROOT -> rootGranted
            ActivationMethod.RADIO_INFO -> true
            else -> shizukuPermission || rootGranted
        }

        if (!isAuthorized) {
            renderPermissionErrorState()
            return
        }

        val mode1 = appPrefs.toggleMode1
        val mode2 = appPrefs.toggleMode2
        val currentActive = appPrefs.currentActiveMode

        val isMode1Active = currentActive == mode1

        qsTile?.apply {
            state = if (isMode1Active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = formatModeShort(currentActive)
            subtitle = "Tap to switch to ${formatModeShort(if (isMode1Active) mode2 else mode1)}"
            updateTile()
        }
    }

    private fun renderPermissionErrorState() {
        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            label = "5G Switcher"
            subtitle = "Permission Required"
            updateTile()
        }
    }

    private fun formatModeShort(mode: NetworkMode): String = when (mode) {
        NetworkMode.NR_ONLY -> "5G SA"
        NetworkMode.NR_LTE -> "5G NSA"
        NetworkMode.LTE_ONLY -> "4G LTE"
    }
}
