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

    private val reasonUser = 0 // ALLOWED_NETWORK_TYPES_REASON_USER

    private fun bitmaskFor(networkType: Int): Long = 1L shl (networkType - 1)

    private val bitmaskNr = bitmaskFor(TelephonyManager.NETWORK_TYPE_NR)     // 20 -> 1L shl 19
    private val bitmaskLte = bitmaskFor(TelephonyManager.NETWORK_TYPE_LTE)   // 13 -> 1L shl 12

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

        val allowedMask: Long = when (mode) {
            "NR_ONLY" -> bitmaskNr
            "NR_LTE" -> bitmaskNr or bitmaskLte
            "LTE_ONLY" -> bitmaskLte
            else -> {
                val err = "ERROR: Unknown mode '$mode'"
                AppLogger.e("UserService", err)
                return err
            }
        }

        val errors = mutableListOf<String>()

        // Approach 1: TelephonyManager instance via Constructor reflection
        try {
            AppLogger.i("UserService", "Attempting Approach 1: TelephonyManager constructor reflection")
            val tm = getTelephonyManagerForSubId(targetSubId)
            if (tm != null) {
                // Try setAllowedNetworkTypesForReason(reason, mask)
                try {
                    val m = tm.javaClass.getMethod(
                        "setAllowedNetworkTypesForReason",
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType,
                    )
                    m.invoke(tm, reasonUser, allowedMask)
                    val msg = "OK: Mode set to $mode (mask=$allowedMask) on subId=$targetSubId via TelephonyManager"
                    AppLogger.i("UserService", msg)
                    return msg
                } catch (t: Throwable) {
                    val u = unwrapThrowable(t)
                    AppLogger.d("UserService", "Approach 1a failed: ${u.javaClass.simpleName}: ${u.message}")
                    errors.add("Approach 1a (${u.javaClass.simpleName}: ${u.message})")
                }

                // Try setAllowedNetworkTypesForReason(subId, reason, mask)
                try {
                    val m = tm.javaClass.getMethod(
                        "setAllowedNetworkTypesForReason",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType,
                    )
                    m.invoke(tm, targetSubId, reasonUser, allowedMask)
                    val msg = "OK: Mode set to $mode on subId=$targetSubId via TelephonyManager(subId)"
                    AppLogger.i("UserService", msg)
                    return msg
                } catch (t: Throwable) {
                    val u = unwrapThrowable(t)
                    AppLogger.d("UserService", "Approach 1b failed: ${u.javaClass.simpleName}: ${u.message}")
                    errors.add("Approach 1b (${u.javaClass.simpleName}: ${u.message})")
                }
            }
        } catch (t: Throwable) {
            val u = unwrapThrowable(t)
            AppLogger.d("UserService", "Approach 1 instantiation failed: ${u.javaClass.simpleName}: ${u.message}")
            errors.add("Approach 1 init (${u.javaClass.simpleName}: ${u.message})")
        }

        // Approach 2: ITelephony ServiceManager binder
        try {
            AppLogger.i("UserService", "Attempting Approach 2: ITelephony binder reflection")
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, "phone") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val iTelephony = asInterface.invoke(null, binder)

                if (iTelephony != null) {
                    val methods = iTelephony.javaClass.methods
                    val targetMethod = methods.firstOrNull {
                        it.name == "setAllowedNetworkTypesForReason" && it.parameterCount == 3
                    }
                    if (targetMethod != null) {
                        targetMethod.invoke(iTelephony, targetSubId, reasonUser, allowedMask)
                        val msg = "OK: Mode set to $mode via ITelephony binder"
                        AppLogger.i("UserService", msg)
                        return msg
                    }
                }
            }
        } catch (t: Throwable) {
            val u = unwrapThrowable(t)
            AppLogger.d("UserService", "Approach 2 failed: ${u.javaClass.simpleName}: ${u.message}")
            errors.add("Approach 2 (${u.javaClass.simpleName}: ${u.message})")
        }

        // Approach 3: Shell `cmd phone` command execution
        try {
            AppLogger.i("UserService", "Attempting Approach 3: cmd phone shell command")
            val cmd = arrayOf("cmd", "phone", "set-allowed-network-types", "-s", targetSubId.toString(), "-r", reasonUser.toString(), allowedMask.toString())
            val process = Runtime.getRuntime().exec(cmd)
            val output = process.inputStream.bufferedReader().readText().trim()
            val errOutput = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val msg = "OK: Mode set to $mode via cmd phone"
                AppLogger.i("UserService", msg)
                return msg
            } else {
                AppLogger.w("UserService", "cmd phone exit $exitCode: out='$output', err='$errOutput'")
                errors.add("Approach 3 (exit $exitCode: $errOutput)")
            }
        } catch (t: Throwable) {
            val u = unwrapThrowable(t)
            AppLogger.d("UserService", "Approach 3 failed: ${u.javaClass.simpleName}: ${u.message}")
            errors.add("Approach 3 (${u.javaClass.simpleName}: ${u.message})")
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
