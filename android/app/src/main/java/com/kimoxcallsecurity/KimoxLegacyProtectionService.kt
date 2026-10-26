package com.kimoxcallsecurity

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Keeps legacy Android 7-9 call protection eligible to receive phone events. */
class KimoxLegacyProtectionService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val enabled = SecurePreferences(this).getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || !enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        KimoxDiagnostics.record(this, "INFO", "LEGACY_BACKGROUND_SERVICE_ACTIVE")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification = NotificationCompat.Builder(this, KimoxBackgroundManager.CALL_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentTitle("Kimox protege tus llamadas")
        .setContentText("Protección en segundo plano activa")
        .setOngoing(true)
        .setShowWhen(false)
        .build()

    companion object {
        private const val NOTIFICATION_ID = 5106

        fun update(context: Context) {
            val intent = Intent(context, KimoxLegacyProtectionService::class.java)
            val enabled = SecurePreferences(context).getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)
            if (Build.VERSION.SDK_INT !in Build.VERSION_CODES.N..Build.VERSION_CODES.P || !enabled) {
                context.stopService(intent)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, intent)
            else context.startService(intent)
        }
    }
}
