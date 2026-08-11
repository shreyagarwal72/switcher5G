package com.app.switcher5g.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.app.switcher5g.MainActivity
import com.app.switcher5g.R
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home Screen App Widget for Switcher 5G.
 * Uses Shizuku IPC or Root (su) power user service directly on widget background threads.
 * Does NOT open any settings screens or activities when buttons are pressed.
 */
class SwitcherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, "Tap mode to switch")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val targetMode = when (action) {
            ACTION_SWITCH_SA -> NetworkMode.NR_ONLY
            ACTION_SWITCH_NSA -> NetworkMode.NR_LTE
            ACTION_SWITCH_LTE -> NetworkMode.LTE_ONLY
            else -> null
        }

        if (targetMode != null) {
            AppLogger.i("SwitcherWidgetProvider", "Widget button tapped: switching to $targetMode via Shizuku/Root only")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = NetworkModeManager(context.applicationContext)
                    
                    // Check if Shizuku or Root is available for 1-tap background switch
                    val shizukuReady = ShizukuHelper.hasPermission()
                    val rootReady = RootHelper.isRootAvailable() && RootHelper.isRootGranted()

                    val result: SwitchResult = when {
                        shizukuReady -> manager.switchTo(targetMode, method = ActivationMethod.SHIZUKU)
                        rootReady -> manager.switchTo(targetMode, method = ActivationMethod.ROOT)
                        else -> manager.switchTo(targetMode, method = ActivationMethod.AUTO).let { autoRes ->
                            if (autoRes is SwitchResult.Success && autoRes.message.contains("RadioInfo")) {
                                SwitchResult.Failure("Shizuku or Root access required for 1-tap widget switch.")
                            } else autoRes
                        }
                    }

                    val statusMsg = when (result) {
                        is SwitchResult.Success -> "✅ Switched to ${targetMode.name}"
                        is SwitchResult.Failure -> "⚠️ ${result.reason}"
                    }

                    // Update all widget instances
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, SwitcherWidgetProvider::class.java)
                    val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

                    for (widgetId in widgetIds) {
                        updateWidgetView(context, appWidgetManager, widgetId, statusMsg, if (result is SwitchResult.Success) targetMode else null)
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                    }
                } catch (t: Throwable) {
                    AppLogger.e("SwitcherWidgetProvider", "Widget switch error", t)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_SWITCH_SA = "com.app.switcher5g.widget.ACTION_SWITCH_SA"
        const val ACTION_SWITCH_NSA = "com.app.switcher5g.widget.ACTION_SWITCH_NSA"
        const val ACTION_SWITCH_LTE = "com.app.switcher5g.widget.ACTION_SWITCH_LTE"

        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            statusText: String = "Ready",
            activeMode: NetworkMode? = null,
        ) {
            val views = RemoteViews(context.packageName, R.layout.switcher_widget_layout)

            views.setTextViewText(R.id.widget_title, "Switcher 5G")
            val modeLabel = when (activeMode) {
                NetworkMode.NR_ONLY -> "5G SA Active"
                NetworkMode.NR_LTE -> "5G NSA Active"
                NetworkMode.LTE_ONLY -> "4G LTE Active"
                null -> statusText
            }
            views.setTextViewText(R.id.widget_status, modeLabel)

            // SA Intent (Direct background broadcast)
            val saIntent = Intent(context, SwitcherWidgetProvider::class.java).apply { action = ACTION_SWITCH_SA }
            val saPendingIntent = PendingIntent.getBroadcast(
                context, 1, saIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_sa, saPendingIntent)

            // NSA Intent (Direct background broadcast)
            val nsaIntent = Intent(context, SwitcherWidgetProvider::class.java).apply { action = ACTION_SWITCH_NSA }
            val nsaPendingIntent = PendingIntent.getBroadcast(
                context, 2, nsaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_nsa, nsaPendingIntent)

            // LTE Intent (Direct background broadcast)
            val lteIntent = Intent(context, SwitcherWidgetProvider::class.java).apply { action = ACTION_SWITCH_LTE }
            val ltePendingIntent = PendingIntent.getBroadcast(
                context, 3, lteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_lte, ltePendingIntent)

            // Header tap opens Main App
            val appIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 4, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
