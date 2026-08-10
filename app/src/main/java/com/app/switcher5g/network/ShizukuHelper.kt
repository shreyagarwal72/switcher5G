package com.app.switcher5g.network

import rikka.shizuku.Shizuku

/**
 * Thin wrapper around Shizuku's static permission API.
 *
 * Shizuku must be installed and running (either via the Shizuku app + ADB pairing,
 * or via root) before any of this is usable. We deliberately don't auto-install or
 * auto-launch Shizuku — that's the user's call, surfaced in the UI as a setup step.
 */
object ShizukuHelper {

    const val REQUEST_CODE = 5271

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
}
