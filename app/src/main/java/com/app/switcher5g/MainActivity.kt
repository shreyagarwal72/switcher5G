package com.app.switcher5g

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.screens.HomeScreen
import com.app.switcher5g.ui.theme.Switcher5GTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle: adb shell am start -a android.intent.action.VIEW \
        //         -d "switcher5g://switch?mode=NR_ONLY"
        intent?.data?.let { uri ->
            uri.getQueryParameter("mode")?.let { modeParam ->
                val mode = runCatching { NetworkMode.valueOf(modeParam.uppercase()) }.getOrNull()
                if (mode != null) {
                    val manager = NetworkModeManager(applicationContext)
                    CoroutineScope(Dispatchers.Main).launch {
                        manager.switchTo(mode)
                        manager.unbind()
                    }
                }
            }
        }

        setContent {
            Switcher5GTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }
    }
}
