package com.app.switcher5g.tile

import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.app.switcher5g.network.Manual5gSwitchHelper
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppLogger
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dedicated Quick Settings Tile: Direct Root (`su`) 5G Network Mode Switcher.
 * Exclusively executes Root-based switching across 5G SA (NR_ONLY), 5G NSA (NR_LTE), and 4G LTE (LTE_ONLY).
 */
class RootTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var tilePrefs: SharedPreferences
    private lateinit var appPrefs: AppPreferences
    private lateinit var manager: NetworkModeManager

    override fun onCreate() {
        super.onCreate()
        tilePrefs = getSharedPreferences("switcher5g_root_tile", MODE_PRIVATE)
        appPrefs = AppPreferences(applicationContext)
        manager = NetworkModeManager(applicationContext)
        AppLogger.i("RootTileService", "Dedicated Root tile service created")
    }

    override fun onStartListening() {
        super.onStartListening()
        renderTile(currentMode())
    }

    override fun onClick() {
        super.onClick()
        val current = currentMode()
        val next = nextMode(current)
        AppLogger.i("RootTileService", "Root QS tile clicked: $current -> $next")

        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            subtitle = "Root Executing..."
            updateTile()
        }

        scope.launch {
            val rootAvailable = RootHelper.isRootAvailable()
            if (!rootAvailable) {
                CoroutineScope(Dispatchers.Main).launch {
                    renderTile(current)
                    Toast.makeText(applicationContext, "⚠️ Root (su) not detected on this device", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val rootGranted = RootHelper.requestRootAccess()
            if (!rootGranted) {
                CoroutineScope(Dispatchers.Main).launch {
                    renderTile(current)
                    Toast.makeText(applicationContext, "⚠️ Root access denied by Superuser manager", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val subId = 1
            val result = manager.switchTo(next, overrideSubId = subId, method = ActivationMethod.ROOT)

            CoroutineScope(Dispatchers.Main).launch {
                when (result) {
                    is SwitchResult.Success -> {
                        AppLogger.i("RootTileService", "Root tile switch success: ${result.message}")
                        tilePrefs.edit().putString(KEY_MODE, next.name).apply()
                        renderTile(next)
                        Toast.makeText(applicationContext, "⚡ [Root] ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    is SwitchResult.Failure -> {
                        AppLogger.e("RootTileService", "Root tile switch failed: ${result.reason}")
                        renderTile(current)
                        Toast.makeText(applicationContext, "⚠️ [Root] ${result.reason}", Toast.LENGTH_LONG).show()
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
            label = "Root 5G Switcher"
            subtitle = when (mode) {
                NetworkMode.NR_ONLY -> "⚡ Root: 5G SA"
                NetworkMode.NR_LTE -> "⚡ Root: 5G NSA"
                NetworkMode.LTE_ONLY -> "⚡ Root: 4G LTE"
            }
            updateTile()
        }
    }

    companion object {
        private const val KEY_MODE = "current_root_mode"
    }
}
