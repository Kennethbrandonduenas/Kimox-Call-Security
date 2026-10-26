package com.kimoxcallsecurity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object KimoxBackgroundManager {
    const val CALL_CHANNEL = "kimox_calls"
    const val POST_CALL_CHANNEL = "kimox_post_call"
    const val APP_CHANGES_CHANNEL = "kimox_app_changes"
    const val SECURITY_AUDIT_CHANNEL = "kimox_security_audits"
    const val HEALTH_CHANNEL = "kimox_protection_health"
    const val PERIODIC_AUDIT_ENABLED = "periodic_audit_enabled"

    fun initialize(context: Context) {
        createNotificationChannels(context)
        val preferences = SecurePreferences(context)
        val auditEnabled = preferences.getBoolean(PERIODIC_AUDIT_ENABLED, false) &&
            preferences.getBoolean(KimoxSecurityModule.MALWARE_CONSENT, false)
        KimoxSecurityAuditWorker.configure(context, auditEnabled)
        KimoxLegacyProtectionService.update(context)
        reportProtectionHealth(context)
        KimoxDiagnostics.record(
            context,
            "INFO",
            "BACKGROUND_PROTECTION_READY",
            "callProtection=${preferences.getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)}; periodicAudit=$auditEnabled",
        )
    }

    private fun reportProtectionHealth(context: Context) {
        val state = KimoxProtectionReadiness.check(context)
        val unrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        val issue = state.issue ?: if (!unrestricted) "Permite actividad sin restricción de batería" else null
        KimoxDiagnostics.record(context, if (issue == null) "INFO" else "WARN", "PROTECTION_HEALTH_CHECK", "ready=${state.ready}; batteryUnrestricted=$unrestricted; issue=${issue.orEmpty()}")
        if (issue == null || !canPostNotifications(context)) return
        val openApp = PendingIntent.getActivity(
            context,
            5105,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, HEALTH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Kimox requiere atención")
            .setContentText(issue)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$issue. Abre Kimox y revisa Ajustes y diagnóstico."))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(5105, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(CALL_CHANNEL, "Protección de llamadas", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(POST_CALL_CHANNEL, "Decisiones después de llamadas", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(APP_CHANGES_CHANNEL, "Cambios de aplicaciones", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(SECURITY_AUDIT_CHANNEL, "Auditorías de seguridad", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(HEALTH_CHANNEL, "Estado de protección", NotificationManager.IMPORTANCE_HIGH),
        ))
    }
}

class KimoxBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        KimoxBackgroundManager.initialize(context.applicationContext)
        KimoxDiagnostics.record(context, "INFO", "BACKGROUND_PROTECTION_RESTORED", "action=${intent.action}")
    }
}
