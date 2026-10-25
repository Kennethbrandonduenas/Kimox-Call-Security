package com.kimoxcallsecurity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class KimoxSecurityAuditWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        KimoxDiagnostics.record(applicationContext, "INFO", "PERIODIC_SECURITY_REVIEW_DUE")
        notifyReview()
        return Result.success()
    }

    private fun notifyReview() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Auditorías de seguridad", NotificationManager.IMPORTANCE_DEFAULT))
        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName) ?: return
        val pending = PendingIntent.getActivity(applicationContext, 8401, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(8401, NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Revisión de seguridad pendiente")
            .setContentText("Abre Kimox para analizar cambios recientes del dispositivo.")
            .setContentIntent(pending).setAutoCancel(true).build())
    }

    companion object {
        private const val UNIQUE_WORK = "kimox_periodic_security_audit"
        private const val CHANNEL = "kimox_security_audits"

        fun configure(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(UNIQUE_WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<KimoxSecurityAuditWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()
            workManager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
