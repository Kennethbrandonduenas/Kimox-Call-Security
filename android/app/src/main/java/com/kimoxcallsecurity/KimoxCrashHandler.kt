package com.kimoxcallsecurity

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build

class KimoxCrashHandler(
    context: Context,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    private val appContext = context.applicationContext

    override fun uncaughtException(thread: Thread, error: Throwable) {
        KimoxDiagnostics.recordCrash(appContext, thread, error)
        if (!isRecoveryProcess()) {
            runCatching {
                appContext.startActivity(Intent(appContext, KimoxRecoveryActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("opened_after_crash", true)
                })
                Thread.sleep(250)
            }
        }
        previous?.uncaughtException(thread, error) ?: run {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun isRecoveryProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }?.processName.orEmpty()
        }
        return processName.endsWith(":recovery")
    }

    companion object {
        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is KimoxCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(KimoxCrashHandler(context, current))
            }
        }
    }
}
