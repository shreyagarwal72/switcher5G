package com.app.switcher5g.network

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import com.app.switcher5g.IUserService
import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

import com.app.switcher5g.util.ActivationMethod

enum class NetworkMode { NR_ONLY, NR_LTE, LTE_ONLY }

sealed class SwitchResult {
    data class Success(val message: String) : SwitchResult()
    data class Failure(val reason: String) : SwitchResult()
}

/**
 * Multi-strategy Network Mode Manager.
 * Operates standalone across:
 * 1. Shizuku IPC Service (if available)
 * 2. Root Shell (`su`)
 * 3. System Radio Testing Menu (`RadioInfo`) Fallback
 */
class NetworkModeManager(private val context: Context) {

    private var service: IUserService? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, NetworkModeUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("networkmode")
            .debuggable(false)
            .version(1)
    }

    private var pendingConnect: kotlin.coroutines.Continuation<Boolean>? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            AppLogger.i("NetworkModeManager", "Shizuku user service connected successfully")
            service = binder?.let { IUserService.Stub.asInterface(it) }
            pendingConnect?.resume(service != null)
            pendingConnect = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            AppLogger.w("NetworkModeManager", "Shizuku user service disconnected")
            service = null
        }
    }

    private suspend fun ensureBound(): Boolean {
        if (service != null) return true
        if (!ShizukuHelper.hasPermission()) return false
        return suspendCancellableCoroutine { cont ->
            pendingConnect = cont
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (t: Throwable) {
                AppLogger.e("NetworkModeManager", "Failed to bind Shizuku user service", t)
                cont.resume(false)
                pendingConnect = null
            }
        }
    }

    suspend fun getAvailableSubIds(): List<Int> = withContext(Dispatchers.IO) {
        val detectedSubIds = mutableListOf<Int>()
        if (ShizukuHelper.hasPermission() && ensureBound()) {
            service?.let { svc ->
                try {
                    val ids = svc.availableSubIds
                    if (ids.isNotEmpty()) {
                        ids.forEach { if (!detectedSubIds.contains(it)) detectedSubIds.add(it) }
                    }
                } catch (_: Throwable) {
                }
            }
        }
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
            if (sm != null) {
                val list = sm.activeSubscriptionInfoList
                if (!list.isNullOrEmpty()) {
                    list.forEach { info ->
                        val subId = info.subscriptionId
                        if (subId > 0 && subId != 2147483647 && !detectedSubIds.contains(subId)) {
                            detectedSubIds.add(subId)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // Always guarantee both SIM 1 (Sub 1) and SIM 2 (Sub 2) are present in the list for Dual SIM support
        if (!detectedSubIds.contains(1)) detectedSubIds.add(1)
        if (!detectedSubIds.contains(2)) detectedSubIds.add(2)

        return@withContext detectedSubIds.sorted()
    }

    suspend fun switchTo(
        mode: NetworkMode,
        overrideSubId: Int? = null,
        method: ActivationMethod = ActivationMethod.AUTO,
    ): SwitchResult = withContext(Dispatchers.IO) {
        AppLogger.i("NetworkModeManager", "Initiating network mode switch to $mode (subId=$overrideSubId, method=$method)")

        when (method) {
            ActivationMethod.ROOT -> {
                if (RootHelper.isRootAvailable()) {
                    val rootResult = RootHelper.switchNetworkMode(mode, overrideSubId ?: 1)
                    if (rootResult is SwitchResult.Success) return@withContext rootResult
                    return@withContext SwitchResult.Failure("Root switch failed. Grant root permission in Superuser/Magisk app.")
                }
                return@withContext SwitchResult.Failure("Root binary (su) not detected on this device.")
            }
            ActivationMethod.SHIZUKU -> {
                if (ShizukuHelper.hasPermission() && ensureBound()) {
                    val svc = service
                    if (svc != null) {
                        try {
                            val subId = overrideSubId ?: run {
                                val detected = svc.defaultDataSubId
                                if (detected != -1 && detected != 2147483647) detected
                                else {
                                    val available = svc.availableSubIds
                                    if (available.isNotEmpty()) available[0] else 1
                                }
                            }
                            val result = svc.setNetworkMode(subId, mode.name)
                            if (result.startsWith("OK")) {
                                return@withContext SwitchResult.Success("Switched to ${mode.name} via Shizuku ($result)")
                            }
                        } catch (t: Throwable) {
                            AppLogger.w("NetworkModeManager", "Shizuku switch failed", t)
                        }
                    }
                }
                return@withContext SwitchResult.Failure("Shizuku service not running or authorized.")
            }
            ActivationMethod.RADIO_INFO -> {
                val launched = launchRadioInfo(context)
                if (launched) {
                    return@withContext SwitchResult.Success("Opened System Radio Info. Select ${mode.name} in Network Type menu.")
                }
                return@withContext SwitchResult.Failure("Could not launch RadioInfo activity.")
            }
            else -> { // AUTO & DIRECT_ADB
                // Strategy 1: Shizuku IPC Service
                if (ShizukuHelper.hasPermission() && ensureBound()) {
                    val svc = service
                    if (svc != null) {
                        try {
                            val subId = overrideSubId ?: run {
                                val detected = svc.defaultDataSubId
                                if (detected != -1 && detected != 2147483647) detected
                                else {
                                    val available = svc.availableSubIds
                                    if (available.isNotEmpty()) available[0] else 1
                                }
                            }
                            val result = svc.setNetworkMode(subId, mode.name)
                            if (result.startsWith("OK")) {
                                return@withContext SwitchResult.Success("Switched to ${mode.name} via Shizuku ($result)")
                            }
                        } catch (t: Throwable) {
                            AppLogger.w("NetworkModeManager", "Shizuku switch failed, falling back to Root/RadioInfo", t)
                        }
                    }
                }

                // Strategy 2: Direct Root Shell (`su`) Execution
                if (RootHelper.isRootAvailable()) {
                    val rootResult = RootHelper.switchNetworkMode(mode, overrideSubId ?: 1)
                    if (rootResult is SwitchResult.Success) {
                        return@withContext rootResult
                    }
                }

                // Strategy 3: System Radio Testing Menu (`RadioInfo`) Fallback (Standalone unrooted device)
                val launched = launchRadioInfo(context)
                if (launched) {
                    SwitchResult.Success("Opened System Radio Info. Select ${mode.name} in Network Type menu.")
                } else {
                    SwitchResult.Failure("Copy ADB command, grant Shizuku permission, or use Root to switch.")
                }
            }
        }
    }

    fun launchRadioInfo(context: Context): Boolean {
        return Manual5gSwitchHelper.openRadioInfo(context)
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(userServiceArgs, connection, true)
        } catch (_: Throwable) {
        }
        service = null
    }
}
