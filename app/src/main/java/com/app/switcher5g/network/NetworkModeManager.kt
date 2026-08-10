package com.app.switcher5g.network

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.app.switcher5g.IUserService
import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

enum class NetworkMode { NR_ONLY, NR_LTE, LTE_ONLY }

sealed class SwitchResult {
    data class Success(val message: String) : SwitchResult()
    data class Failure(val reason: String) : SwitchResult()
}

/**
 * Owns the Shizuku user-service connection and turns [NetworkMode] requests
 * into calls on [NetworkModeUserService].
 */
class NetworkModeManager(private val context: Context) {

    private var service: IUserService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, NetworkModeUserService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("networkmode")
        .debuggable(false)
        .version(1)

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
        if (!ShizukuHelper.hasPermission()) {
            AppLogger.w("NetworkModeManager", "Cannot bind service: Shizuku permission missing")
            return false
        }
        return suspendCancellableCoroutine { cont ->
            pendingConnect = cont
            try {
                AppLogger.i("NetworkModeManager", "Binding Shizuku user service...")
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (t: Throwable) {
                AppLogger.e("NetworkModeManager", "Failed to bind Shizuku user service", t)
                cont.resume(false)
                pendingConnect = null
            }
        }
    }

    suspend fun getAvailableSubIds(): List<Int> {
        if (!ShizukuHelper.hasPermission()) return listOf(1)
        if (!ensureBound()) return listOf(1)
        val svc = service ?: return listOf(1)
        return try {
            val ids = svc.availableSubIds
            AppLogger.i("NetworkModeManager", "Retrieved active Sub IDs: ${ids.toList()}")
            if (ids.isNotEmpty()) ids.toList() else listOf(1)
        } catch (t: Throwable) {
            AppLogger.e("NetworkModeManager", "Failed to get available sub IDs", t)
            listOf(1)
        }
    }

    suspend fun switchTo(mode: NetworkMode, overrideSubId: Int? = null): SwitchResult {
        AppLogger.i("NetworkModeManager", "Initiating network mode switch to $mode (overrideSubId=$overrideSubId)")
        
        if (!ShizukuHelper.isAvailable()) {
            val err = "Shizuku is not running. Start it from the Shizuku app first."
            AppLogger.e("NetworkModeManager", err)
            return SwitchResult.Failure(err)
        }
        if (!ShizukuHelper.hasPermission()) {
            val err = "Shizuku permission not granted yet."
            AppLogger.e("NetworkModeManager", err)
            return SwitchResult.Failure(err)
        }
        if (!ensureBound()) {
            val err = "Could not bind the privileged Shizuku service."
            AppLogger.e("NetworkModeManager", err)
            return SwitchResult.Failure(err)
        }
        val svc = service ?: return SwitchResult.Failure("Service unavailable.")

        return try {
            val subId = overrideSubId ?: run {
                val detected = svc.defaultDataSubId
                if (detected != -1 && detected != 2147483647) {
                    detected
                } else {
                    val available = svc.availableSubIds
                    if (available.isNotEmpty()) available[0] else 1
                }
            }
            AppLogger.i("NetworkModeManager", "Using target subId: $subId for mode $mode")
            val result = svc.setNetworkMode(subId, mode.name)
            if (result.startsWith("OK")) {
                AppLogger.i("NetworkModeManager", "Switch successful: $result")
                SwitchResult.Success(result)
            } else {
                AppLogger.e("NetworkModeManager", "Switch failed: $result")
                SwitchResult.Failure(result)
            }
        } catch (t: Throwable) {
            val err = "${t.javaClass.simpleName}: ${t.message}"
            AppLogger.e("NetworkModeManager", "Exception during mode switch", t)
            SwitchResult.Failure(err)
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(userServiceArgs, connection, true)
            AppLogger.i("NetworkModeManager", "Unbound Shizuku user service")
        } catch (_: Throwable) {
        }
        service = null
    }
}
