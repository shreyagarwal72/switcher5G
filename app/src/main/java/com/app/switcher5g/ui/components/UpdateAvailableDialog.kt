package com.app.switcher5g.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.update.UpdateInfo
import com.app.switcher5g.update.UpdateManager
import kotlinx.coroutines.launch

/**
 * Proper Update Available Dialog triggered on app start or manual check when a new release is available.
 */
@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismissRequest() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = "New Update Available! (v${updateInfo.latestVersion})",
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
                    text = "A new version of Switcher 5G is available. What's new:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = com.app.switcher5g.util.MarkdownUtils.parseMarkdown(updateInfo.releaseNotes),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Downloading update… ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isDownloading && updateInfo.apkUrl.isNotBlank(),
                onClick = {
                    isDownloading = true
                    scope.launch {
                        UpdateManager.downloadAndInstallApk(
                            context = context,
                            apkUrl = updateInfo.apkUrl,
                            onProgress = { progress -> downloadProgress = progress },
                        )
                        isDownloading = false
                        onDismissRequest()
                    }
                },
                modifier = Modifier.bouncyClickable {},
            ) {
                if (isDownloading) {
                    FancyCircularOrbLoader(size = 16.dp)
                } else {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDownloading) "Downloading…" else "Download & Install")
            }
        },
        dismissButton = {
            if (!isDownloading) {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Later")
                }
            }
        },
    )
}
