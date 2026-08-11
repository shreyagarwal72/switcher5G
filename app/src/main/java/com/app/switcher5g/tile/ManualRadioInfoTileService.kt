package com.app.switcher5g.tile

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.app.switcher5g.MainActivity
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
        
        val radioInfoIntent = Intent().apply {
            action = Intent.ACTION_MAIN
            setClassName("com.android.settings", "com.android.settings.RadioInfo")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            startActivityAndCollapse(radioInfoIntent)
        } catch (t: Throwable) {
            AppLogger.w("ManualRadioInfoTileService", "Direct RadioInfo failed, attempting TestingSettings fallback", t)
            val testingIntent = Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName("com.android.settings", "com.android.settings.TestingSettings")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            try {
                startActivityAndCollapse(testingIntent)
            } catch (_: Throwable) {
                val appIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                runCatching { startActivityAndCollapse(appIntent) }
            }
        }
    }
}
