package com.app.switcher5g.network

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.Toast
import com.app.switcher5g.util.AppLogger

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

        val intent = Intent().apply {
            if (component != null) {
                this.component = component
            } else {
                action = Intent.ACTION_MAIN
                setClassName("com.android.settings", "com.android.settings.RadioInfo")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // 1. Android 14+ (API 34+) require PendingIntent for TileService.startActivityAndCollapse
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val pendingIntent = PendingIntent.getActivity(
                    tileService,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val method = TileService::class.java.getMethod("startActivityAndCollapse", PendingIntent::class.java)
                method.invoke(tileService, pendingIntent)
                AppLogger.i("Manual5gSwitchHelper", "Launched activity from Tile via PendingIntent (Android 14+)")
                return true
            } catch (t: Throwable) {
                AppLogger.w("Manual5gSwitchHelper", "Android 14 startActivityAndCollapse(PendingIntent) failed", t)
            }
        }

        // 2. Android 7.0 - 13 (API 24 - 33) use Intent version of TileService.startActivityAndCollapse
        try {
            val method = TileService::class.java.getMethod("startActivityAndCollapse", Intent::class.java)
            method.invoke(tileService, intent)
            AppLogger.i("Manual5gSwitchHelper", "Launched activity from Tile via Intent (Android 7-13)")
            return true
        } catch (t: Throwable) {
            AppLogger.w("Manual5gSwitchHelper", "startActivityAndCollapse(Intent) failed", t)
        }

        // 3. Fallback: launch activity directly via context
        return tryStartActivity(tileService.applicationContext, intent)
    }
}
