package com.app.switcher5g.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for:
 *   adb shell am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode "NR_ONLY"
 */
class TermuxReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val modeArg = intent.getStringExtra("mode")?.uppercase()
        val subIdArg = intent.getIntExtra("subId", -1).takeIf { it != -1 }
        val mode = modeArg?.let { runCatching { NetworkMode.valueOf(it) }.getOrNull() }

        AppLogger.i(TAG, "Broadcast received: mode=$modeArg, subId=$subIdArg")

        if (mode == null) {
            AppLogger.w(TAG, "Ignoring broadcast with invalid/missing mode extra: $modeArg")
            return
        }

        val pending = goAsync()
        val manager = NetworkModeManager(context.applicationContext)
        CoroutineScope(Dispatchers.Main).launch {
            val result = manager.switchTo(mode, subIdArg)
            when (result) {
                is SwitchResult.Success -> AppLogger.i(TAG, "Switched to $mode: ${result.message}")
                is SwitchResult.Failure -> AppLogger.e(TAG, "Failed to switch to $mode: ${result.reason}")
            }
            manager.unbind()
            pending.finish()
        }
    }

    companion object {
        private const val TAG = "TermuxReceiver"
    }
}
