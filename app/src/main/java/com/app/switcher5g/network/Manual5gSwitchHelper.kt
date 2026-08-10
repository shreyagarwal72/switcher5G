package com.app.switcher5g.network

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Native RadioInfo & Hidden Testing Menu launcher based on OpenAppsLabs/5G.
 * Dynamically resolves manufacturer-specific testing activities across:
 * - AOSP Stock RadioInfo & TestingSettings
 * - Qualcomm Engineer / MobileNetworkSettings
 * - MediaTek EngineerMode
 * - Samsung Knox ServiceModeApp
 * - Stock BandMode
 */
object Manual5gSwitchHelper {
    private val COMPONENTS = arrayOf(
        Pair("com.android.settings", "com.android.settings.RadioInfo"),
        Pair("com.android.settings", "com.android.settings.Settings\$RadioInfoActivity"),
        Pair("com.android.settings", "com.android.settings.TestingSettings"),
        Pair("com.android.settings", "com.android.settings.Settings\$TestingSettingsActivity"),
        Pair("com.android.phone", "com.android.phone.settings.RadioInfo"),
        Pair("com.android.phone", "com.android.phone.RadioInfo"),
        Pair("com.qualcomm.qti.networksetting", "com.qualcomm.qti.networksetting.MobileNetworkSettings"),
        Pair("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode"),
        Pair("com.mediatek.engineermode", "com.mediatek.engineermode.modemtest.ModemTestActivity"),
        Pair("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.ServiceModeApp"),
        Pair("com.android.phone", "com.android.phone.MobileNetworkSettings"),
        Pair("com.android.settings", "com.android.settings.BandMode"),
    )

    private var cachedComponent: ComponentName? = null

    fun preResolve(context: Context): ComponentName? {
        if (cachedComponent != null) return cachedComponent

        for (comp in COMPONENTS) {
            val pkg = comp.first
            val cls = comp.second
            val componentName = ComponentName(pkg, cls)
            if (isActivityExists(context, componentName)) {
                cachedComponent = componentName
                return componentName
            }
        }
        return null
    }

    fun openRadioInfo(context: Context): Boolean {
        val component = cachedComponent ?: preResolve(context)

        if (component == null) {
            Toast.makeText(context, "Radio Info / 5G menu not supported on this ROM", Toast.LENGTH_SHORT).show()
            return false
        }

        val intent = Intent().apply {
            this.component = component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (context is TileService) {
            startActivityFromTile(context, intent)
        } else {
            tryStartActivity(context, intent)
        }
    }

    private fun isActivityExists(context: Context, componentName: ComponentName): Boolean {
        return try {
            val info = context.packageManager.getActivityInfo(componentName, 0)
            info.exported
        } catch (_: Exception) {
            false
        }
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startActivityFromTile(tileService: TileService, intent: Intent): Boolean {
        return try {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(
                tileService,
                0,
                intent,
                flags,
            )
            tileService.startActivityAndCollapse(pendingIntent)
            true
        } catch (_: Exception) {
            cachedComponent = null
            false
        }
    }
}
