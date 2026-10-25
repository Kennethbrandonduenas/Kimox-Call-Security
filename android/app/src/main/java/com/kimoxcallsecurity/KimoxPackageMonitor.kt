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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class KimoxPackageMonitor : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return
        val preferences = SecurePreferences(context)
        if (!preferences.getBoolean(KimoxSecurityModule.MALWARE_CONSENT, false)) return
        val event = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) "updated" else "installed"
            Intent.ACTION_PACKAGE_REPLACED -> "updated"
            Intent.ACTION_PACKAGE_REMOVED -> if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return else "removed"
            else -> return
        }
        val changes = preferences.getStringSet(PACKAGE_CHANGES).toMutableSet().apply {
            add("${System.currentTimeMillis()}|$event|$packageName")
            if (size > MAX_EVENTS) sortedBy { it.substringBefore('|').toLongOrNull() ?: 0L }.take(size - MAX_EVENTS).forEach(::remove)
        }
        preferences.putStringSet(PACKAGE_CHANGES, changes)
        KimoxDiagnostics.record(context, "INFO", "PACKAGE_${event.uppercase()}", "package=$packageName")
        notifyReview(context, packageName, event)
    }

    private fun notifyReview(context: Context, packageName: String, event: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val pending = PendingIntent.getActivity(context, 8301, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val verb = if (event == "installed") "instaló" else if (event == "updated") "actualizó" else "eliminó"
        manager.notify(packageName.hashCode(), NotificationCompat.Builder(context, KimoxBackgroundManager.APP_CHANGES_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aplicación $verb")
            .setContentText("Abre Kimox para revisar $packageName")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build())
    }

    companion object {
        const val PACKAGE_CHANGES = "malware_package_changes"
        private const val MAX_EVENTS = 30
    }
}
