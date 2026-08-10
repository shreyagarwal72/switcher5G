package com.app.switcher5g.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.ui.components.bouncyClickable
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.util.AppLogger
import com.app.switcher5g.util.LogEntry
import com.app.switcher5g.util.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLogsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }

    val logsList = AppLogger.logs

    val filteredLogs = remember(logsList, searchQuery, selectedFilter) {
        logsList.filter { entry ->
            val matchesFilter = selectedFilter == null || entry.level == selectedFilter
            val matchesSearch = searchQuery.isBlank() ||
                    entry.tag.contains(searchQuery, ignoreCase = true) ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    (entry.throwableMessage?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesSearch
        }.reversed()
    }

    val errorCount = remember(logsList) { logsList.count { it.level == LogLevel.ERROR } }
    val warnCount = remember(logsList) { logsList.count { it.level == LogLevel.WARN } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(0),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Developer Diagnostics",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Total: ${logsList.size} • Errors: $errorCount • Warnings: $warnCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val text = AppLogger.getAllLogsText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Switcher5G Logs", text))
                        Toast.makeText(context, "Copied ${logsList.size} logs to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.bouncyClickable {},
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = {
                        AppLogger.clear()
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.bouncyClickable {},
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .entrance(1),
            placeholder = { Text("Search tag, message, or exception…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("ALL") },
            )
            FilterChip(
                selected = selectedFilter == LogLevel.ERROR,
                onClick = { selectedFilter = if (selectedFilter == LogLevel.ERROR) null else LogLevel.ERROR },
                label = { Text("ERROR ($errorCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            )
            FilterChip(
                selected = selectedFilter == LogLevel.WARN,
                onClick = { selectedFilter = if (selectedFilter == LogLevel.WARN) null else LogLevel.WARN },
                label = { Text("WARN ($warnCount)") },
            )
            FilterChip(
                selected = selectedFilter == LogLevel.INFO,
                onClick = { selectedFilter = if (selectedFilter == LogLevel.INFO) null else LogLevel.INFO },
                label = { Text("INFO") },
            )
        }

        // Log Console Output
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (logsList.isEmpty()) "No logs captured yet." else "No matching logs found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(filteredLogs, key = { _, item -> item.id }) { index, logEntry ->
                    LogCard(entry = logEntry, index = index)
                }
            }
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry, index: Int) {
    var expanded by remember { mutableStateOf(false) }

    val (badgeColor, textColor) = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFEF5350) to Color(0xFFFFEBEE)
        LogLevel.WARN -> Color(0xFFFFA726) to Color(0xFFFFF3E0)
        LogLevel.INFO -> Color(0xFF66BB6A) to Color(0xFFE8F5E9)
        LogLevel.DEBUG -> Color(0xFF42A5F5) to Color(0xFFE3F2FD)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .entrance(index + 3)
            .bouncyClickable(scaleDown = 0.98f) { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = entry.level.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = Color.Black,
                        )
                    }
                    Text(
                        text = entry.tag,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )

            AnimatedVisibility(visible = expanded && entry.throwableMessage != null) {
                entry.throwableMessage?.let { stack ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                    ) {
                        Text(
                            text = stack,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
