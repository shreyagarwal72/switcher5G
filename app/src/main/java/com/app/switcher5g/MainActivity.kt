package com.app.switcher5g

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.screens.MainScreen
import com.app.switcher5g.ui.theme.Switcher5GTheme
import com.app.switcher5g.util.AppLogger
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i("MainActivity", "Activity onCreate triggered")

        val prefs = AppPreferences(applicationContext)

        // Handle deep link intent:
        // adb shell am start -a android.intent.action.VIEW -d "switcher5g://switch?mode=NR_ONLY"
        intent?.data?.let { uri ->
            uri.getQueryParameter("mode")?.let { modeParam ->
                val mode = runCatching { NetworkMode.valueOf(modeParam.uppercase()) }.getOrNull()
                if (mode != null) {
                    AppLogger.i("MainActivity", "Handling deep link mode switch: $mode")
                    val manager = NetworkModeManager(applicationContext)
                    CoroutineScope(Dispatchers.Main).launch {
                        manager.switchTo(mode)
                        manager.unbind()
                    }
                }
            }
        }

        setContent {
            val appPrefs = remember { AppPreferences(applicationContext) }
            Switcher5GTheme(prefs = appPrefs) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(prefs = appPrefs)
                }
            }
        }
    }
}
