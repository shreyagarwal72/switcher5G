package com.app.switcher5g.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.Manual5gSwitchHelper

/**
 * First-Time User Dialog for Manual 5G Switching via OpenAppsLabs/5G system activity resolution.
 */
@Composable
fun Manual5gFirstTimeDialog(
    onDismissRequest: () -> Unit,
    onDontShowAgain: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var dontShowAgainChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Rounded.NetworkCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = "Manual 5G System Switcher",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "This mode directly opens your device's hidden System 5G / RadioInfo menu (powered by OpenAppsLabs/5G engine). Works natively on 100% of Android phones without root or Shizuku!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "How to switch network mode manually:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "1. Tap 'Launch System 5G Menu' below.\n" +
                                "2. Scroll down to 'Set Preferred Network Type'.\n" +
                                "3. Choose 'NR only' for 5G Standalone, 'NR/LTE' for 5G NSA, or 'LTE only' for 4G.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = dontShowAgainChecked,
                        onCheckedChange = {
                            dontShowAgainChecked = it
                            onDontShowAgain(it)
                        },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Don't show this guide again",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDontShowAgain(dontShowAgainChecked)
                    onDismissRequest()
                    Manual5gSwitchHelper.openRadioInfo(context)
                },
                modifier = Modifier.bouncyClickable {},
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Launch System 5G Menu")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDontShowAgain(dontShowAgainChecked)
                    onDismissRequest()
                },
            ) {
                Text("Close")
            }
        },
    )
}
