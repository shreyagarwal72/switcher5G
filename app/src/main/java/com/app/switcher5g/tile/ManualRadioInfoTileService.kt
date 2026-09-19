package com.app.switcher5g.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.app.switcher5g.network.Manual5gSwitchHelper
import com.app.switcher5g.util.AppLogger

/**
 * Quick Settings Tile Mode 2: Opens System Manual 5G / RadioInfo Settings (*#*#4636#*#*).
 * Immediately collapses notification shade and launches manual 5G testing settings.
 */
class ManualRadioInfoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "5G Settings & Bands"
            subtitle = "Tap: App | Double: Radio"
            updateTile()
        }
    }

    private var lastClickTime = 0L

    override fun onClick() {
        super.onClick()
        val currentTime = System.currentTimeMillis()
        val isDoubleTap = (currentTime - lastClickTime) < 500
        lastClickTime = currentTime

        AppLogger.i("ManualRadioInfoTileService", "QS tile clicked (isDoubleTap=$isDoubleTap)")

        try {
            if (isDoubleTap) {
                // Double tap directly launches the RadioInfo / Band Selection Testing Activity
                val launched = Manual5gSwitchHelper.openBandSelection(this)
                if (launched) {
                    qsTile?.apply {
                        state = Tile.STATE_ACTIVE
                        subtitle = "Opened Radio/Bands"
                        updateTile()
                    }
                }
            } else {
                // Single tap opens the Switcher 5G application directly (resolves Issue #3)
                val launched = Manual5gSwitchHelper.openMainActivity(this)
                if (launched) {
                    qsTile?.apply {
                        state = Tile.STATE_ACTIVE
                        subtitle = "Opened App"
                        updateTile()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ManualRadioInfoTileService", "Failed to launch from QS tile", e)
        }
    }
}
