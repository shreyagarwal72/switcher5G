package com.app.switcher5g.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import rikka.shizuku.Shizuku

/**
 * Wrapper around Shizuku's static permission API and setup helpers.
 */
object ShizukuHelper {

    const val REQUEST_CODE = 5271
    const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

    // Commands to run from PC terminal or LADB (outside adb shell)
    const val ADB_PC_SDCARD = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh"
    const val ADB_PC_USER = "adb shell sh /data/user/0/moe.shizuku.privileged.api/files/start.sh"

    // Commands to run inside an existing adb shell prompt ($)
    const val SHELL_SDCARD = "sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh"
    const val SHELL_USER = "sh /data/user/0/moe.shizuku.privileged.api/files/start.sh"

    fun isAvailable(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

    fun hasPermission(): Boolean =
        isAvailable() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun requestPermission() {
        if (isAvailable() && !hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    fun launchShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    const val ADB_ACCESSIBILITY_COMMAND = "adb shell settings put secure enabled_accessibility_services com.app.switcher5g/.service.AccessibilitySwitchService"

    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        return am?.isEnabled == true
    }

    fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PACKAGE_NAME")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE_NAME")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
