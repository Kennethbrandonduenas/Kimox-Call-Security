package com.kimoxcallsecurity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

object KimoxBackgroundManager {
    const val CALL_CHANNEL = "kimox_calls"
    const val POST_CALL_CHANNEL = "kimox_post_call"
    const val APP_CHANGES_CHANNEL = "kimox_app_changes"
    const val SECURITY_AUDIT_CHANNEL = "kimox_security_audits"
    const val PERIODIC_AUDIT_ENABLED = "periodic_audit_enabled"

    fun initialize(context: Context) {
        createNotificationChannels(context)
        val preferences = SecurePreferences(context)
        val auditEnabled = preferences.getBoolean(PERIODIC_AUDIT_ENABLED, false) &&
            preferences.getBoolean(KimoxSecurityModule.MALWARE_CONSENT, false)
        KimoxSecurityAuditWorker.configure(context, auditEnabled)
        KimoxDiagnostics.record(
            context,
            "INFO",
            "BACKGROUND_PROTECTION_READY",
            "callProtection=${preferences.getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)}; periodicAudit=$auditEnabled",
        )
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(CALL_CHANNEL, "Protección de llamadas", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(POST_CALL_CHANNEL, "Decisiones después de llamadas", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(APP_CHANGES_CHANNEL, "Cambios de aplicaciones", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(SECURITY_AUDIT_CHANNEL, "Auditorías de seguridad", NotificationManager.IMPORTANCE_DEFAULT),
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
