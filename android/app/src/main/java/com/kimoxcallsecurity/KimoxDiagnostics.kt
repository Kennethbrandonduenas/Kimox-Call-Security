package com.kimoxcallsecurity

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KimoxDiagnostics {
    private const val DIRECTORY = "diagnostics"
    private const val CURRENT_LOG = "kimox-diagnostics.txt"
    private const val NATIVE_LOG = "kimox-native-log.txt"
    private const val REPORT_LOG = "kimox-complete-report.txt"
    private const val MAX_BYTES = 256 * 1024L
    private const val MAX_ARCHIVES = 4
    private val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val longNumber = Regex("(?<!\\d)\\+?\\d[\\d ()-]{5,}\\d")

    @Synchronized
    fun record(context: Context, level: String, event: String, details: String? = null) {
        runCatching {
            val directory = directory(context)
            val log = File(directory, CURRENT_LOG)
            if (log.exists() && log.length() >= MAX_BYTES) rotate(directory, log)
            val cleanDetails = details?.let(::sanitize)?.take(16_000)
            val line = buildString {
                append(timestamp.format(Date())).append(" | ")
                append(level.take(8)).append(" | ").append(event.take(80))
                if (!cleanDetails.isNullOrBlank()) append(" | ").append(cleanDetails)
                append('\n')
            }
            log.appendText(line, Charsets.UTF_8)
        }
    }

    fun recordEnvironment(context: Context) {
        record(
            context,
            "INFO",
            "DEVICE_ENVIRONMENT",
            "manufacturer=${Build.MANUFACTURER}; model=${Build.MODEL}; android=${Build.VERSION.RELEASE}; api=${Build.VERSION.SDK_INT}; app=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}); hermes=${BuildConfig.IS_HERMES_ENABLED}",
        )
    }

    fun recordCrash(context: Context, thread: Thread, error: Throwable) {
        record(context, "FATAL", "UNCAUGHT_EXCEPTION", "thread=${thread.name}; ${error.stackTraceToString()}")
    }

    fun currentLog(context: Context): File = File(directory(context), CURRENT_LOG).also {
        if (!it.exists()) it.writeText("Kimox diagnostics: no events recorded yet.\n")
    }

    fun nativeLog(context: Context): File = File(directory(context), NATIVE_LOG).also {
        if (!it.exists()) it.writeText("Kimox native log: waiting for a monitored launch.\n")
    }

    @Synchronized
    fun recordNative(context: Context, value: String) {
        runCatching {
            val file = nativeLog(context)
            if (file.length() >= MAX_BYTES) file.writeText("Kimox native log rotated.\n")
            file.appendText(sanitize(value).take(16_000) + '\n', Charsets.UTF_8)
        }
    }

    fun completeReport(context: Context): File {
        val report = File(directory(context), REPORT_LOG)
        report.writeText(
            buildString {
                appendLine("KIMOX CALL SECURITY - COMPLETE DIAGNOSTIC REPORT")
                appendLine("Generated: ${timestamp.format(Date())}")
                appendLine()
                appendLine("===== APPLICATION EVENTS =====")
                append(latestText(context, 80_000))
                appendLine()
                appendLine("===== NATIVE LOGCAT =====")
                append(latestNativeText(context, 80_000))
            },
            Charsets.UTF_8,
        )
        return report
    }

    fun latestText(context: Context, maxChars: Int = 20_000): String {
        val file = currentLog(context)
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("Unable to read diagnostics.")
        return if (text.length <= maxChars) text else text.takeLast(maxChars)
    }

    fun latestNativeText(context: Context, maxChars: Int = 20_000): String {
        val text = runCatching { nativeLog(context).readText(Charsets.UTF_8) }
            .getOrDefault("Unable to read native diagnostics.")
        return if (text.length <= maxChars) text else text.takeLast(maxChars)
    }

    fun clear(context: Context) {
        directory(context).listFiles()?.forEach { runCatching { it.delete() } }
        record(context, "INFO", "DIAGNOSTICS_CLEARED")
    }

    private fun directory(context: Context) = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    private fun rotate(directory: File, current: File) {
        File(directory, "kimox-diagnostics-$MAX_ARCHIVES.txt").delete()
        for (index in MAX_ARCHIVES - 1 downTo 1) {
            val source = File(directory, "kimox-diagnostics-$index.txt")
            if (source.exists()) source.renameTo(File(directory, "kimox-diagnostics-${index + 1}.txt"))
        }
        current.renameTo(File(directory, "kimox-diagnostics-1.txt"))
    }

    private fun sanitize(value: String) = value
        .replace(longNumber) { match -> maskNumber(match.value) }
        .replace(Regex("(?i)(password|contraseña|secret|token)\\s*[=:]\\s*[^;\\s]+"), "$1=[REDACTED]")

    private fun maskNumber(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length <= 4) "[REDACTED]" else "********${digits.takeLast(4)}"
    }
}
