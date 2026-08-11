package com.app.switcher5g.tile

import android.content.Intent
import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
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
 * Quick Settings Tile Mode 1: Power User Mode Switcher.
 * Cycles 5G SA (NR_ONLY) -> 5G NSA (NR_LTE) -> 4G LTE (LTE_ONLY) -> 5G SA... on each tap directly in quick settings.
 */
class NetworkTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var tilePrefs: SharedPreferences
    private lateinit var appPrefs: AppPreferences
    private lateinit var manager: NetworkModeManager

    override fun onCreate() {
        super.onCreate()
        tilePrefs = getSharedPreferences("switcher5g_tile", MODE_PRIVATE)
        appPrefs = AppPreferences(applicationContext)
        manager = NetworkModeManager(applicationContext)
        AppLogger.i("NetworkTileService", "Power user tile service created")
    }

    override fun onStartListening() {
        super.onStartListening()
        renderTile(currentMode())
    }

    override fun onClick() {
        super.onClick()
        val next = nextMode(currentMode())
        AppLogger.i("NetworkTileService", "Power tile clicked; switching to $next using method ${appPrefs.activationMethod}")

        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            subtitle = "Switching..."
            updateTile()
        }

        val shizukuReady = ShizukuHelper.hasPermission()

        scope.launch {
            val rootReady = RootHelper.isRootAvailable() && RootHelper.isRootGranted()
            val targetMethod = when {
                shizukuReady -> ActivationMethod.SHIZUKU
                rootReady -> ActivationMethod.ROOT
                appPrefs.activationMethod == ActivationMethod.RADIO_INFO -> ActivationMethod.RADIO_INFO
                else -> ActivationMethod.AUTO
            }

            val result = manager.switchTo(next, method = targetMethod)

            if (result is SwitchResult.Success && result.message.contains("Opened System Radio Info")) {
                // Launch RadioInfo using TileService.startActivityAndCollapse
                val radioIntent = Intent().apply {
                    action = Intent.ACTION_MAIN
                    setClassName("com.android.settings", "com.android.settings.RadioInfo")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                runCatching { startActivityAndCollapse(radioIntent) }
            }

            CoroutineScope(Dispatchers.Main).launch {
                when (result) {
                    is SwitchResult.Success -> {
                        AppLogger.i("NetworkTileService", "Power tile switch success: ${result.message}")
                        tilePrefs.edit().putString(KEY_MODE, next.name).apply()
                        renderTile(next)
                        Toast.makeText(applicationContext, "✅ ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    is SwitchResult.Failure -> {
                        AppLogger.e("NetworkTileService", "Power tile switch failed: ${result.reason}")
                        qsTile?.apply {
                            state = Tile.STATE_INACTIVE
                            subtitle = "Failed"
                            updateTile()
                        }
                        Toast.makeText(applicationContext, "⚠️ ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun currentMode(): NetworkMode =
        tilePrefs.getString(KEY_MODE, null)?.let {
            runCatching { NetworkMode.valueOf(it) }.getOrNull()
        } ?: appPrefs.defaultNetworkMode

    private fun nextMode(mode: NetworkMode): NetworkMode = when (mode) {
        NetworkMode.NR_ONLY -> NetworkMode.NR_LTE
        NetworkMode.NR_LTE -> NetworkMode.LTE_ONLY
        NetworkMode.LTE_ONLY -> NetworkMode.NR_ONLY
    }

    private fun renderTile(mode: NetworkMode) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "5G Power Switcher"
            subtitle = when (mode) {
                NetworkMode.NR_ONLY -> "5G SA (NR)"
                NetworkMode.NR_LTE -> "5G NSA (NR/LTE)"
                NetworkMode.LTE_ONLY -> "4G LTE"
            }
            updateTile()
        }
    }

    companion object {
        private const val KEY_MODE = "current_mode"
    }
}
