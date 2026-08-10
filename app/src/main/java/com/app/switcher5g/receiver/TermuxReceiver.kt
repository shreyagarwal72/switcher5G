package com.app.switcher5g.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.SwitchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for:
 *   adb shell am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "NR_ONLY"
 *
 * Security model: the receiver is guarded by a signature|privileged permission,
 * which a normal third-party app cannot hold. But `adb shell` (and therefore
 * Termux, which is itself just a shell) runs as the `shell` UID, and shell is
 * allow-listed to send broadcasts even to permission-protected receivers it
 * doesn't hold the permission for — same mechanism that lets `am broadcast`
 * work for other privileged receivers on stock Android. That's what makes this
 * usable from Termux without the permission tag actually being a hole for
 * random installed apps.
 */
class TermuxReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val modeArg = intent.getStringExtra("mode")?.uppercase()
        val mode = modeArg?.let { runCatching { NetworkMode.valueOf(it) }.getOrNull() }

        if (mode == null) {
            Log.w(TAG, "Ignoring broadcast with invalid/missing mode extra: $modeArg")
            return
        }

        val pending = goAsync()
        val manager = NetworkModeManager(context.applicationContext)
        CoroutineScope(Dispatchers.Main).launch {
            val result = manager.switchTo(mode)
            when (result) {
                is SwitchResult.Success -> Log.i(TAG, "Switched to $mode: ${result.message}")
                is SwitchResult.Failure -> Log.e(TAG, "Failed to switch to $mode: ${result.reason}")
            }
            manager.unbind()
            pending.finish()
        }
    }

    companion object {
        private const val TAG = "TermuxReceiver"
    }
}
