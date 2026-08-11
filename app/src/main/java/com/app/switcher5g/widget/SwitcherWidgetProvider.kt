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
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.util.AppLogger
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home Screen App Widget for Switcher 5G.
 * Allows 1-tap mode switching directly from Home Screen.
 */
class SwitcherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, "Ready")
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
            AppLogger.i("SwitcherWidgetProvider", "Widget button tapped: switching to $targetMode")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = AppPreferences(context.applicationContext)
                    val manager = NetworkModeManager(context.applicationContext)
                    val result = manager.switchTo(targetMode, method = prefs.activationMethod)

                    val statusMsg = when (result) {
                        is SwitchResult.Success -> "Switched to ${targetMode.name}"
                        is SwitchResult.Failure -> "Failed: ${result.reason}"
                    }

                    // Update all widget instances
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, SwitcherWidgetProvider::class.java)
                    val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

                    for (widgetId in widgetIds) {
                        updateWidgetView(context, appWidgetManager, widgetId, statusMsg, targetMode)
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                    }
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

            // SA Intent
            val saIntent = Intent(context, SwitcherWidgetProvider::class.java).apply { action = ACTION_SWITCH_SA }
            val saPendingIntent = PendingIntent.getBroadcast(
                context, 1, saIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_sa, saPendingIntent)

            // NSA Intent
            val nsaIntent = Intent(context, SwitcherWidgetProvider::class.java).apply { action = ACTION_SWITCH_NSA }
            val nsaPendingIntent = PendingIntent.getBroadcast(
                context, 2, nsaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_nsa, nsaPendingIntent)

            // LTE Intent
            val lteIntent = Intent(context, SwitcherWidgetProvider::class.java).apply { action = ACTION_SWITCH_LTE }
            val ltePendingIntent = PendingIntent.getBroadcast(
                context, 3, lteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_lte, ltePendingIntent)

            // App Open Intent
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 4, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
