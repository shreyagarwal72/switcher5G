package com.app.switcher5g.util

import android.content.Context

/**
 * Single source of truth for Switcher 5G versioning & package information.
 */
object AppInfo {
    const val VERSION_NAME = "1.0.1"
    const val VERSION_CODE = 2

    fun getAppVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: VERSION_NAME
        } catch (_: Throwable) {
            VERSION_NAME
        }
    }
}
