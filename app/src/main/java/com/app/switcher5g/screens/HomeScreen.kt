package com.app.switcher5g.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.ui.components.FancyLiquidProgressBar
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { NetworkModeManager(context.applicationContext) }

    var selectedMode by remember { mutableStateOf(NetworkMode.NR_LTE) }
    var statusText by remember { mutableStateOf("Idle") }
    var isSwitching by remember { mutableStateOf(false) }
    var shizukuReady by remember { mutableStateOf(ShizukuHelper.hasPermission()) }

    DisposableEffect(Unit) {
        onDispose { manager.unbind() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("5G Switcher", style = MaterialTheme.typography.headlineMedium)

        if (!shizukuReady) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shizuku permission required", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Install/start Shizuku (ADB pairing or root), then grant permission here.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        ShizukuHelper.requestPermission()
                        shizukuReady = ShizukuHelper.hasPermission()
                    }) { Text("Grant Shizuku permission") }
                }
            }
        }

        NetworkMode.entries.forEach { mode ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(mode.label(), style = MaterialTheme.typography.titleMedium)
                        Text(mode.description(), style = MaterialTheme.typography.bodySmall)
                    }
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                    )
                }
            }
        }

        if (isSwitching) {
            FancyLiquidProgressBar(progress = 0.6f, modifier = Modifier.fillMaxWidth())
        }

        Button(
            enabled = !isSwitching,
            onClick = {
                isSwitching = true
                statusText = "Switching…"
                scope.launch {
                    val result = manager.switchTo(selectedMode)
                    statusText = when (result) {
                        is SwitchResult.Success -> "✅ ${result.message}"
                        is SwitchResult.Failure -> "⚠️ ${result.reason}"
                    }
                    isSwitching = false
                }
            },
        ) {
            Text("Apply mode")
        }

        Text(statusText, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun NetworkMode.label(): String = when (this) {
    NetworkMode.NR_ONLY -> "NR Only (5G SA)"
    NetworkMode.NR_LTE -> "NR / LTE (5G NSA)"
    NetworkMode.LTE_ONLY -> "LTE Only (4G)"
}

private fun NetworkMode.description(): String = when (this) {
    NetworkMode.NR_ONLY -> "Pure 5G standalone. Falls back poorly where SA coverage is thin."
    NetworkMode.NR_LTE -> "5G with LTE anchor — most compatible, closest to default."
    NetworkMode.LTE_ONLY -> "Locks to 4G. Useful for battery testing or poor 5G areas."
}
