package com.app.switcher5g.network

import android.telephony.TelephonyManager
import com.app.switcher5g.IUserService
import com.app.switcher5g.util.AppLogger
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Hosted by Shizuku in a separate `shell`-UID process.
 */
class NetworkModeUserService : IUserService.Stub() {

    init {
        try {
            HiddenApiBypass.addHiddenApiExemptions("L")
            AppLogger.i("UserService", "HiddenApiBypass initialized successfully")
        } catch (t: Throwable) {
            AppLogger.w("UserService", "HiddenApiBypass exemption failed", t)
        }
    }

    private val reasonUser = 0

    private fun bitmaskFor(networkType: Int): Long = 1L shl (networkType - 1)

    private val bitmaskNr = bitmaskFor(TelephonyManager.NETWORK_TYPE_NR)     // 20
    private val bitmaskLte = bitmaskFor(TelephonyManager.NETWORK_TYPE_LTE)   // 13

    override fun setNetworkMode(subId: Int, mode: String): String {
        AppLogger.i("UserService", "setNetworkMode requested: subId=$subId, mode=$mode")
        
        // If subId is invalid, resolve active subId dynamically
        val targetSubId = if (subId <= 0 || subId == 2147483647) {
            val resolved = getDefaultDataSubId()
            AppLogger.i("UserService", "Invalid subId $subId passed; auto-resolved to $resolved")
            resolved
        } else {
            subId
        }

        return try {
            val allowedMask: Long = when (mode) {
                "NR_ONLY" -> bitmaskNr
                "NR_LTE" -> bitmaskNr or bitmaskLte
                "LTE_ONLY" -> bitmaskLte
                else -> {
                    val err = "ERROR: unknown mode '$mode'"
                    AppLogger.e("UserService", err)
                    return err
                }
            }

            val tmClass = TelephonyManager::class.java
            val createForSubscription = tmClass.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
            val baseTm = android.telephony.TelephonyManager::class.java
                .getMethod("getDefault")
                .invoke(null) as TelephonyManager

            val tm = createForSubscription.invoke(baseTm, targetSubId) as TelephonyManager

            val method = tmClass.getMethod(
                "setAllowedNetworkTypesForReason",
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
            )
            method.invoke(tm, reasonUser, allowedMask)

            val successMsg = "OK: mode set to $mode (mask=$allowedMask) on subId=$targetSubId"
            AppLogger.i("UserService", successMsg)
            successMsg
        } catch (t: Throwable) {
            val errorMsg = "ERROR: ${t.javaClass.simpleName}: ${t.message}"
            AppLogger.e("UserService", errorMsg, t)
            errorMsg
        }
    }

    override fun getDefaultDataSubId(): Int {
        val activeIds = getAvailableSubIds()
        if (activeIds.isNotEmpty()) {
            AppLogger.i("UserService", "getDefaultDataSubId returning first active subId: ${activeIds[0]}")
            return activeIds[0]
        }

        // Multi-tier fallback resolution
        val strategies = listOf(
            "getDefaultDataSubscriptionId",
            "getDefaultSubscriptionId",
            "getDefaultSmsSubscriptionId"
        )

        for (strategy in strategies) {
            try {
                val cls = Class.forName("android.telephony.SubscriptionManager")
                val method = cls.getMethod(strategy)
                val res = method.invoke(null) as? Int ?: -1
                if (res > 0 && res != 2147483647) {
                    AppLogger.i("UserService", "Resolved subId via $strategy: $res")
                    return res
                }
            } catch (t: Throwable) {
                AppLogger.d("UserService", "$strategy check failed: ${t.message}")
            }
        }

        AppLogger.w("UserService", "Defaulting to fallback subId 1")
        return 1
    }

    override fun getAvailableSubIds(): IntArray {
        val subIds = mutableListOf<Int>()
        try {
            val cls = Class.forName("android.telephony.SubscriptionManager")
            
            // Method 1: getActiveSubscriptionInfoList
            try {
                val getActiveList = cls.methods.firstOrNull { it.name == "getActiveSubscriptionInfoList" && it.parameterCount == 0 }
                if (getActiveList != null) {
                    val list = getActiveList.invoke(null) as? List<*>
                    if (!list.isNullOrEmpty()) {
                        for (info in list) {
                            if (info != null) {
                                val getSubId = info.javaClass.getMethod("getSubscriptionId")
                                val id = getSubId.invoke(info) as? Int ?: -1
                                if (id > 0 && id != 2147483647 && !subIds.contains(id)) {
                                    subIds.add(id)
                                }
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                AppLogger.d("UserService", "getActiveSubscriptionInfoList failed: ${t.message}")
            }

            // Method 2: getSubId for slots 0 and 1
            if (subIds.isEmpty()) {
                for (slot in 0..1) {
                    try {
                        val getSubId = cls.getMethod("getSubId", Int::class.javaPrimitiveType)
                        val res = getSubId.invoke(null, slot)
                        if (res is IntArray && res.isNotEmpty()) {
                            for (id in res) {
                                if (id > 0 && id != 2147483647 && !subIds.contains(id)) {
                                    subIds.add(id)
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }
            }
        } catch (t: Throwable) {
            AppLogger.e("UserService", "Failed to retrieve available subscription IDs", t)
        }

        if (subIds.isEmpty()) {
            // Check default data sub ID as last resort
            try {
                val cls = Class.forName("android.telephony.SubscriptionManager")
                val method = cls.getMethod("getDefaultDataSubscriptionId")
                val id = method.invoke(null) as? Int ?: -1
                if (id > 0 && id != 2147483647) {
                    subIds.add(id)
                }
            } catch (_: Throwable) {}
        }

        // Fallback to SIM 1 default if empty
        if (subIds.isEmpty()) {
            subIds.add(1)
        }

        AppLogger.i("UserService", "Available Sub IDs resolved: $subIds")
        return subIds.toIntArray()
    }

    override fun destroy() {
        AppLogger.i("UserService", "UserService destroying process")
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
