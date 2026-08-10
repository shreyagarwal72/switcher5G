package com.app.switcher5g.network

import android.telephony.TelephonyManager
import com.app.switcher5g.IUserService
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Hosted by Shizuku in a separate `shell`-UID process (NOT our normal app
 * process). Because Shizuku launches this via `app_process` rather than
 * through Zygote/the framework, ART's hidden-API enforcement does not apply
 * here the way it would in a normal app process — that's what makes calling
 * TelephonyManager's SystemApi methods via reflection viable without root.
 *
 * ⚠️ THIS IS THE LEAST PORTABLE PART OF THE APP. Method names/signatures here
 * are the AOSP telephony API surface, which OEMs (OneUI, HyperOS, ColorOS,
 * MIUI) have been known to alter or gate behind extra permission checks.
 * Verify on real hardware per-OEM before shipping — do not assume this
 * behaves identically across skins from a single test device.
 */
class NetworkModeUserService : IUserService.Stub() {

    init {
        // Best-effort: reflectively unlocks non-SDK interfaces on API 28+.
        // No-op (and harmless) in the shell-UID process where this usually
        // isn't even required, but kept as a defensive fallback for OEMs
        // that partially enforce the restriction outside Zygote too.
        try {
            HiddenApiBypass.addHiddenApiExemptions("L")
        } catch (_: Throwable) {
            // non-fatal — fall through and let the reflective call itself fail loudly if needed
        }
    }

    // Reason constant from TelephonyManager (@SystemApi, hidden):
    // ALLOWED_NETWORK_TYPES_REASON_USER = 0
    private val reasonUser = 0

    // NETWORK_TYPE_* are public constants; the *_BITMASK_* hidden equivalents are
    // just `1L shl (networkType - 1)`, so we compute them instead of reflecting.
    private fun bitmaskFor(networkType: Int): Long = 1L shl (networkType - 1)

    private val bitmaskNr = bitmaskFor(TelephonyManager.NETWORK_TYPE_NR)     // 20
    private val bitmaskLte = bitmaskFor(TelephonyManager.NETWORK_TYPE_LTE)   // 13

    override fun setNetworkMode(subId: Int, mode: String): String {
        return try {
            val allowedMask: Long = when (mode) {
                "NR_ONLY" -> bitmaskNr
                "NR_LTE" -> bitmaskNr or bitmaskLte
                "LTE_ONLY" -> bitmaskLte
                else -> return "ERROR: unknown mode '$mode'"
            }

            val tmClass = TelephonyManager::class.java
            val createForSubscription = tmClass.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
            // NOTE: getSystemContext() is not real API — see README for how MainActivity
            // supplies a Context into this service via IUserService if createForSubscriptionId
            // needs an instance rather than a static call on some OS versions.
            val baseTm = android.telephony.TelephonyManager::class.java
                .getMethod("getDefault")
                .invoke(null) as TelephonyManager

            val tm = createForSubscription.invoke(baseTm, subId) as TelephonyManager

            val method = tmClass.getMethod(
                "setAllowedNetworkTypesForReason",
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
            )
            method.invoke(tm, reasonUser, allowedMask)
            "OK: mode set to $mode (mask=$allowedMask) on subId=$subId"
        } catch (t: Throwable) {
            "ERROR: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    override fun getDefaultDataSubId(): Int {
        return try {
            val cls = Class.forName("android.telephony.SubscriptionManager")
            val method = cls.getMethod("getDefaultDataSubscriptionId")
            method.invoke(null) as Int
        } catch (_: Throwable) {
            -1
        }
    }

    override fun destroy() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
