package com.app.switcher5g.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.app.switcher5g.network.Manual5gSwitchHelper
import com.app.switcher5g.util.AppLogger

/**
 * Quick Settings Tile Mode 2: Opens System Manual 5G / RadioInfo Settings (*#*#4636#*#*).
 */
class ManualRadioInfoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Manual 5G Settings"
            subtitle = "Open RadioInfo"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        AppLogger.i("ManualRadioInfoTileService", "Opening manual RadioInfo settings from QS tile")
        try {
            Manual5gSwitchHelper.openRadioInfo(applicationContext)
        } catch (e: Exception) {
            AppLogger.e("ManualRadioInfoTileService", "Failed to launch RadioInfo", e)
        }
    }
}
