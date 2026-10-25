package com.kimoxcallsecurity

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

object KimoxNativeLogCollector {
    private val monitoring = AtomicBoolean(false)

    fun monitorNextLaunch(context: Context) {
        if (!monitoring.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        Thread({ monitor(appContext) }, "kimox-native-log-monitor").apply {
            isDaemon = true
            start()
        }
    }

    private fun monitor(context: Context) {
        try {
            KimoxDiagnostics.record(context, "INFO", "NATIVE_MONITOR_WAITING")
            val deadline = SystemClock.elapsedRealtime() + 15_000
            var pid: Int? = null
            while (SystemClock.elapsedRealtime() < deadline && pid == null) {
                pid = findMainProcessPid(context)
                if (pid == null) SystemClock.sleep(100)
            }
            if (pid == null) {
                KimoxDiagnostics.record(context, "WARN", "NATIVE_MONITOR_PROCESS_NOT_FOUND")
                return
            }

            KimoxDiagnostics.record(context, "INFO", "NATIVE_MONITOR_ATTACHED", "pid=$pid")
            KimoxDiagnostics.recordNative(context, "===== Monitoring main process pid=$pid =====")
            val process = ProcessBuilder(
                "logcat",
                "--pid=$pid",
                "-v",
                "threadtime",
                "*:V",
            ).redirectErrorStream(true).start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { KimoxDiagnostics.recordNative(context, it) }
            }
            val exitCode = process.waitFor()
            KimoxDiagnostics.record(
                context,
                "WARN",
                "MAIN_PROCESS_LOG_ENDED",
                "pid=$pid; logcatExit=$exitCode; processAlive=${isPidRunning(context, pid)}",
            )
            KimoxDiagnostics.recordNative(context, "===== Main process/log stream ended; exit=$exitCode =====")
        } catch (error: Throwable) {
            KimoxDiagnostics.record(
                context,
                "ERROR",
                "NATIVE_MONITOR_FAILED",
                "${error.javaClass.simpleName}: ${error.message}",
            )
        } finally {
            monitoring.set(false)
        }
    }

    private fun findMainProcessPid(context: Context): Int? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.runningAppProcesses
            ?.firstOrNull { it.processName == context.packageName }
            ?.pid
    }

    private fun isPidRunning(context: Context, pid: Int): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.runningAppProcesses?.any { it.pid == pid } == true
    }
}
