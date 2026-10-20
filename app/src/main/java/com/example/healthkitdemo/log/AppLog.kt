package com.example.healthkitdemo.log

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

enum class LogCategory { APP, AUTH, API, PUSH }

data class LogEntry(
    val id: Long,
    val timeMillis: Long,
    val level: LogLevel,
    val category: LogCategory,
    val message: String,
    val detail: String? = null,
) {
    val time: String get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timeMillis))

    fun asText(): String = buildString {
        append("$time ${level.name.padEnd(5)} ${category.name.padEnd(4)} $message")
        detail?.let { append("\n").append(it.prependIndent("    ")) }
    }
}

object AppLog {

    private const val MAX_ENTRIES = 500
    private val ids = AtomicLong()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun debug(category: LogCategory, message: String, detail: String? = null) = add(LogLevel.DEBUG, category, message, detail)

    fun info(category: LogCategory, message: String, detail: String? = null) = add(LogLevel.INFO, category, message, detail)

    fun warn(category: LogCategory, message: String, detail: String? = null) = add(LogLevel.WARN, category, message, detail)

    fun error(category: LogCategory, message: String, detail: String? = null) = add(LogLevel.ERROR, category, message, detail)

    fun clear() {
        _entries.value = emptyList()
    }

    fun export(): String = _entries.value.joinToString("\n") { it.asText() }

    private fun add(level: LogLevel, category: LogCategory, message: String, detail: String?) {
        val entry = LogEntry(ids.incrementAndGet(), System.currentTimeMillis(), level, category, message, detail)
        _entries.update { (it + entry).takeLast(MAX_ENTRIES) }

        val tag = "HealthKitDemo/${category.name}"
        val text = if (detail == null) message else "$message\n$detail"
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, text)
            LogLevel.INFO -> Log.i(tag, text)
            LogLevel.WARN -> Log.w(tag, text)
            LogLevel.ERROR -> Log.e(tag, text)
        }
    }
}
