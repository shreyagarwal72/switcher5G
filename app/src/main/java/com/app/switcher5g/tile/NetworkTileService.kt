package com.app.switcher5g.tile

import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.SwitchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Cycles NR_ONLY -> NR_LTE -> LTE_ONLY -> NR_ONLY... on each tap.
 * Does the switch in-process (no activity launch, so no BAL restrictions apply)
 * by calling NetworkModeManager directly from the tile's own process.
 */
class NetworkTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var prefs: SharedPreferences
    private lateinit var manager: NetworkModeManager

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("switcher5g_tile", MODE_PRIVATE)
        manager = NetworkModeManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        renderTile(currentMode())
    }

    override fun onClick() {
        super.onClick()
        val next = nextMode(currentMode())
        scope.launch {
            when (val result = manager.switchTo(next)) {
                is SwitchResult.Success -> {
                    prefs.edit().putString(KEY_MODE, next.name).apply()
                    renderTile(next)
                }
                is SwitchResult.Failure -> {
                    // Tile has no room for error text; state reflects failure so the
                    // user knows to open the app for details.
                    qsTile?.state = Tile.STATE_UNAVAILABLE
                    qsTile?.subtitle = result.reason.take(40)
                    qsTile?.updateTile()
                }
            }
        }
    }

    private fun currentMode(): NetworkMode =
        prefs.getString(KEY_MODE, null)?.let {
            runCatching { NetworkMode.valueOf(it) }.getOrNull()
        } ?: NetworkMode.NR_LTE

    private fun nextMode(mode: NetworkMode): NetworkMode = when (mode) {
        NetworkMode.NR_ONLY -> NetworkMode.NR_LTE
        NetworkMode.NR_LTE -> NetworkMode.LTE_ONLY
        NetworkMode.LTE_ONLY -> NetworkMode.NR_ONLY
    }

    private fun renderTile(mode: NetworkMode) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "5G Switcher"
            subtitle = when (mode) {
                NetworkMode.NR_ONLY -> "5G SA"
                NetworkMode.NR_LTE -> "5G NSA"
                NetworkMode.LTE_ONLY -> "LTE"
            }
            updateTile()
        }
    }

    companion object {
        private const val KEY_MODE = "current_mode"
    }
}
