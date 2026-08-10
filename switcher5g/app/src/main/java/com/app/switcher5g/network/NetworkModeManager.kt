package com.app.switcher5g.network

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.app.switcher5g.IUserService
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
 * into calls on [NetworkModeUserService]. One instance per app process is fine
 * — the underlying Shizuku process is reused across calls.
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
            service = binder?.let { IUserService.Stub.asInterface(it) }
            pendingConnect?.resume(service != null)
            pendingConnect = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private suspend fun ensureBound(): Boolean {
        if (service != null) return true
        if (!ShizukuHelper.hasPermission()) return false
        return suspendCancellableCoroutine { cont ->
            pendingConnect = cont
            Shizuku.bindUserService(userServiceArgs, connection)
        }
    }

    suspend fun switchTo(mode: NetworkMode): SwitchResult {
        if (!ShizukuHelper.isAvailable()) {
            return SwitchResult.Failure("Shizuku is not running. Start it from the Shizuku app first.")
        }
        if (!ShizukuHelper.hasPermission()) {
            return SwitchResult.Failure("Shizuku permission not granted yet.")
        }
        if (!ensureBound()) {
            return SwitchResult.Failure("Could not bind the privileged service.")
        }
        val svc = service ?: return SwitchResult.Failure("Service unavailable.")

        return try {
            val subId = svc.defaultDataSubId.takeIf { it != -1 }
                ?: return SwitchResult.Failure("Could not resolve the active data SIM.")
            val result = svc.setNetworkMode(subId, mode.name)
            if (result.startsWith("OK")) SwitchResult.Success(result) else SwitchResult.Failure(result)
        } catch (t: Throwable) {
            SwitchResult.Failure("${t.javaClass.simpleName}: ${t.message}")
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(userServiceArgs, connection, true)
        } catch (_: Throwable) {
        }
        service = null
    }
}
