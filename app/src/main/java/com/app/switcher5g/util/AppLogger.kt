package com.app.switcher5g.util

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableMessage: String? = null,
)

/**
 * Lightweight, memory-optimized logger facility.
 * Bounded queue prevents RAM bloat.
 */
object AppLogger {

    private var nextId = 1L
    private const val MAX_ENTRIES = 50

    val logs = mutableStateListOf<LogEntry>()

    private val timeFormat by lazy { SimpleDateFormat("HH:mm:ss", Locale.US) }

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val timeStr = timeFormat.format(Date())
        val throwableStr = throwable?.let { 
            val full = "${it.javaClass.simpleName}: ${it.message}"
            if (full.length > 250) full.take(250) + "…" else full
        }

        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }

        val entry = LogEntry(
            id = nextId++,
            timestamp = timeStr,
            level = level,
            tag = tag,
            message = if (message.length > 300) message.take(300) + "…" else message,
            throwableMessage = throwableStr,
        )

        while (logs.size >= MAX_ENTRIES) {
            logs.removeAt(0)
        }
        logs.add(entry)
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    fun clear() {
        logs.clear()
    }

    fun getAllLogsText(): String {
        return logs.joinToString("\n") { entry ->
            val errStr = entry.throwableMessage?.let { "\n$it" } ?: ""
            "[${entry.timestamp}] [${entry.level.name}] [${entry.tag}] ${entry.message}$errStr"
        }
    }
}
