package com.app.switcher5g.network

import android.content.Context
import android.os.IBinder
import android.telephony.TelephonyManager
import com.app.switcher5g.IUserService
import com.app.switcher5g.util.AppLogger
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.InvocationTargetException

/**
 * Hosted by Shizuku in a separate `shell`-UID process.
 * Provides multi-strategy network mode switching for 5G SA (NR_ONLY), 5G NSA (NR_LTE), and 4G (LTE_ONLY).
 */
class NetworkModeUserService : IUserService.Stub() {

    init {
        try {
            HiddenApiBypass.addHiddenApiExemptions("L")
            AppLogger.i("UserService", "HiddenApiBypass initialized successfully")
        } catch (t: Throwable) {
            val cause = unwrapThrowable(t)
            AppLogger.w("UserService", "HiddenApiBypass exemption failed: ${cause.message}")
        }
    }

    private val REASON_USER = 0
    private val REASON_CARRIER = 2

    // Bitmasks for AOSP Telephony allowed network types
    private val BITMASK_NR = 1L shl (TelephonyManager.NETWORK_TYPE_NR - 1)       // 1L shl 19 = 524288L
    private val BITMASK_LTE = 1L shl (TelephonyManager.NETWORK_TYPE_LTE - 1)     // 1L shl 12 = 4096L
    private val BITMASK_UMTS = 1L shl (TelephonyManager.NETWORK_TYPE_UMTS - 1)   // 1L shl 2 = 4L
    private val BITMASK_GSM = 1L shl (TelephonyManager.NETWORK_TYPE_GSM - 1)     // 1L shl 16 or 1L shl 0

    // Mode Constants (RILConstants / TelephonyManager)
    private val NETWORK_MODE_NR_ONLY = 28                 // 5G SA Only
    private val NETWORK_MODE_NR_LTE_GSM_WCDMA = 26       // 5G NSA (NR + LTE + 3G + 2G)
    private val NETWORK_MODE_NR_LTE = 24                 // 5G NSA (NR + LTE)
    private val NETWORK_MODE_LTE_ONLY = 11               // 4G LTE Only
    private val NETWORK_MODE_LTE_GSM_WCDMA = 9           // 4G LTE + 3G + 2G

    private fun unwrapThrowable(t: Throwable): Throwable {
        var current: Throwable = t
        while (current is InvocationTargetException && current.targetException != null) {
            current = current.targetException
        }
        return current
    }

    override fun setNetworkMode(subId: Int, mode: String): String {
        AppLogger.i("UserService", "setNetworkMode requested: subId=$subId, mode=$mode")

        val targetSubId = if (subId <= 0 || subId == 2147483647) {
            val resolved = getDefaultDataSubId()
            AppLogger.i("UserService", "Auto-resolved target subId: $resolved")
            resolved
        } else {
            subId
        }

        val preferredModeInt: Int
        val allowedMask: Long

        when (mode.uppercase()) {
            "NR_ONLY" -> {
                preferredModeInt = NETWORK_MODE_NR_ONLY // 28
                allowedMask = BITMASK_NR // 524288L
            }
            "NR_LTE" -> {
                preferredModeInt = NETWORK_MODE_NR_LTE_GSM_WCDMA // 26
                allowedMask = BITMASK_NR or BITMASK_LTE or BITMASK_UMTS or BITMASK_GSM // 528391L
            }
            "LTE_ONLY" -> {
                preferredModeInt = NETWORK_MODE_LTE_ONLY // 11
                allowedMask = BITMASK_LTE // 4096L
            }
            else -> {
                val err = "ERROR: Unknown mode '$mode'"
                AppLogger.e("UserService", err)
                return err
            }
        }

        val errors = mutableListOf<String>()

        // ---------------------------------------------------------------------
        // Strategy 1: TelephonyManager reflective invocation
        // ---------------------------------------------------------------------
        try {
            val tm = getTelephonyManagerForSubId(targetSubId)
            if (tm != null) {
                // 1a: setAllowedNetworkTypesForReason(reason, mask)
                for (reason in intArrayOf(REASON_USER, REASON_CARRIER)) {
                    try {
                        val m = tm.javaClass.getMethod(
                            "setAllowedNetworkTypesForReason",
                            Int::class.javaPrimitiveType,
                            Long::class.javaPrimitiveType,
                        )
                        m.invoke(tm, reason, allowedMask)
                        AppLogger.i("UserService", "Successfully set allowed network types reason $reason = $allowedMask")
                    } catch (t: Throwable) {
                        errors.add("TM.setAllowedNetworkTypesForReason($reason): ${unwrapThrowable(t).message}")
                    }
                }

                // 1b: setPreferredNetworkType(subId, modeInt) / setPreferredNetworkType(modeInt)
                try {
                    val m = tm.javaClass.getMethod("setPreferredNetworkType", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    m.invoke(tm, targetSubId, preferredModeInt)
                    val msg = "OK: Mode set to $mode (modeInt=$preferredModeInt) on subId=$targetSubId via TelephonyManager.setPreferredNetworkType"
                    AppLogger.i("UserService", msg)
                    return msg
                } catch (_: Throwable) {
                    try {
                        val m = tm.javaClass.getMethod("setPreferredNetworkType", Int::class.javaPrimitiveType)
                        m.invoke(tm, preferredModeInt)
                        val msg = "OK: Mode set to $mode (modeInt=$preferredModeInt) via TelephonyManager.setPreferredNetworkType"
                        AppLogger.i("UserService", msg)
                        return msg
                    } catch (t: Throwable) {
                        errors.add("TM.setPreferredNetworkType: ${unwrapThrowable(t).message}")
                    }
                }
            }
        } catch (t: Throwable) {
            errors.add("TM init: ${unwrapThrowable(t).message}")
        }

        // ---------------------------------------------------------------------
        // Strategy 2: ITelephony ServiceManager Binder
        // ---------------------------------------------------------------------
        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, "phone") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val iTelephony = asInterface.invoke(null, binder)

                if (iTelephony != null) {
                    // Try setPreferredNetworkType(subId, modeInt)
                    val setPrefMethod = iTelephony.javaClass.methods.firstOrNull {
                        it.name == "setPreferredNetworkType" && it.parameterCount == 2
                    }
                    if (setPrefMethod != null) {
                        setPrefMethod.invoke(iTelephony, targetSubId, preferredModeInt)
                        val msg = "OK: Mode set to $mode (modeInt=$preferredModeInt) via ITelephony.setPreferredNetworkType"
                        AppLogger.i("UserService", msg)
                        return msg
                    }

                    // Try setAllowedNetworkTypesForReason(subId, reason, allowedMask)
                    val setAllowedMethod = iTelephony.javaClass.methods.firstOrNull {
                        it.name == "setAllowedNetworkTypesForReason" && it.parameterCount == 3
                    }
                    if (setAllowedMethod != null) {
                        setAllowedMethod.invoke(iTelephony, targetSubId, REASON_USER, allowedMask)
                        val msg = "OK: Mode set to $mode (mask=$allowedMask) via ITelephony.setAllowedNetworkTypesForReason"
                        AppLogger.i("UserService", msg)
                        return msg
                    }
                }
            }
        } catch (t: Throwable) {
            errors.add("ITelephony binder: ${unwrapThrowable(t).message}")
        }

        // ---------------------------------------------------------------------
        // Strategy 3: Shell `cmd phone` commands
        // ---------------------------------------------------------------------
        val shellCommands = listOf(
            arrayOf("cmd", "phone", "set-preferred-network-mode", "-s", targetSubId.toString(), preferredModeInt.toString()),
            arrayOf("cmd", "phone", "set-preferred-network-mode", preferredModeInt.toString()),
            arrayOf("cmd", "phone", "set-allowed-network-types", "-s", targetSubId.toString(), "-r", "0", allowedMask.toString()),
            arrayOf("cmd", "phone", "set-allowed-network-types", "-r", "0", allowedMask.toString()),
            arrayOf("settings", "put", "global", "preferred_network_mode$targetSubId", preferredModeInt.toString()),
            arrayOf("settings", "put", "global", "preferred_network_mode", preferredModeInt.toString()),
        )

        for (cmd in shellCommands) {
            try {
                val process = Runtime.getRuntime().exec(cmd)
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    val msg = "OK: Mode set to $mode via '${cmd.joinToString(" ")}'"
                    AppLogger.i("UserService", msg)
                    return msg
                } else {
                    val errOut = process.errorStream.bufferedReader().readText().trim()
                    errors.add("'${cmd.joinToString(" ")}' (exit $exitCode: $errOut)")
                }
            } catch (t: Throwable) {
                errors.add("'${cmd.joinToString(" ")}': ${unwrapThrowable(t).message}")
            }
        }

        val finalError = "ERROR: All switch attempts failed:\n" + errors.joinToString("\n")
        AppLogger.e("UserService", finalError)
        return finalError
    }

    private fun getTelephonyManagerForSubId(subId: Int): TelephonyManager? {
        // Try constructor: TelephonyManager(Context context, int subId)
        try {
            val ctor = TelephonyManager::class.java.getDeclaredConstructor(Context::class.java, Int::class.javaPrimitiveType)
            ctor.isAccessible = true
            return ctor.newInstance(null, subId)
        } catch (_: Throwable) {}

        // Try no-arg constructor + createForSubscriptionId
        try {
            val ctorDefault = TelephonyManager::class.java.getDeclaredConstructor()
            ctorDefault.isAccessible = true
            val base = ctorDefault.newInstance()
            val createSub = TelephonyManager::class.java.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
            return createSub.invoke(base, subId) as? TelephonyManager
        } catch (_: Throwable) {}

        return null
    }

    override fun getDefaultDataSubId(): Int {
        val activeIds = getAvailableSubIds()
        if (activeIds.isNotEmpty()) {
            AppLogger.i("UserService", "getDefaultDataSubId returning first active subId: ${activeIds[0]}")
            return activeIds[0]
        }

        val strategies = listOf(
            "getDefaultDataSubscriptionId",
            "getDefaultSubscriptionId",
            "getDefaultSmsSubscriptionId",
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
                val u = unwrapThrowable(t)
                AppLogger.d("UserService", "$strategy check failed: ${u.message}")
            }
        }

        AppLogger.w("UserService", "Defaulting to fallback subId 1")
        return 1
    }

    override fun getAvailableSubIds(): IntArray {
        val subIds = mutableListOf<Int>()
        try {
            val cls = Class.forName("android.telephony.SubscriptionManager")

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
                val u = unwrapThrowable(t)
                AppLogger.d("UserService", "getActiveSubscriptionInfoList failed: ${u.message}")
            }

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
            val u = unwrapThrowable(t)
            AppLogger.e("UserService", "Failed to retrieve available subscription IDs", u)
        }

        if (subIds.isEmpty()) {
            try {
                val cls = Class.forName("android.telephony.SubscriptionManager")
                val method = cls.getMethod("getDefaultDataSubscriptionId")
                val id = method.invoke(null) as? Int ?: -1
                if (id > 0 && id != 2147483647) {
                    subIds.add(id)
                }
            } catch (_: Throwable) {}
        }

        if (!subIds.contains(1)) subIds.add(1)
        if (!subIds.contains(2)) subIds.add(2)

        val sortedSubIds = subIds.sorted().toIntArray()
        AppLogger.i("UserService", "Available Sub IDs resolved: ${sortedSubIds.toList()}")
        return sortedSubIds
    }

    override fun destroy() {
        AppLogger.i("UserService", "UserService destroying process")
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
