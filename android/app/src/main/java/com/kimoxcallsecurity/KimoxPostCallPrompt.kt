package com.kimoxcallsecurity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat

object KimoxPostCallPrompt {
    private const val NOTIFICATION_ID = 5102
    private const val CANDIDATE_NUMBER = "post_call_candidate_number"
    private const val CANDIDATE_TIME = "post_call_candidate_time"
    private const val LAST_PROMPT_NUMBER = "post_call_last_prompt_number"
    private const val LAST_PROMPT_TIME = "post_call_last_prompt_time"
    private const val CANDIDATE_MAX_AGE_MS = 6 * 60 * 60 * 1000L
    private const val REPEAT_DELAY_MS = 24 * 60 * 60 * 1000L

    fun rememberCandidate(context: Context, number: String) {
        val preferences = SecurePreferences(context)
        preferences.putString(CANDIDATE_NUMBER, number)
        preferences.putString(CANDIDATE_TIME, System.currentTimeMillis().toString())
    }

    fun showIfPending(context: Context) {
        val preferences = SecurePreferences(context)
        if (!preferences.getBoolean(KimoxSecurityModule.POST_CALL_PROMPT_ENABLED, true)) return
        val number = preferences.getString(CANDIDATE_NUMBER, "")?.takeIf { it.isNotEmpty() } ?: return
        val candidateTime = preferences.getString(CANDIDATE_TIME, "0")?.toLongOrNull() ?: 0L
        preferences.remove(CANDIDATE_NUMBER)
        preferences.remove(CANDIDATE_TIME)
        val now = System.currentTimeMillis()
        if (now - candidateTime !in 0..CANDIDATE_MAX_AGE_MS) return
        val lastNumber = preferences.getString(LAST_PROMPT_NUMBER, "").orEmpty()
        val lastTime = preferences.getString(LAST_PROMPT_TIME, "0")?.toLongOrNull() ?: 0L
        if (lastNumber == number && now - lastTime < REPEAT_DELAY_MS) return
        preferences.putString(LAST_PROMPT_NUMBER, number)
        preferences.putString(LAST_PROMPT_TIME, now.toString())

        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, KimoxBackgroundManager.POST_CALL_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("¿Qué deseas hacer con este número?")
            .setContentText(number)
            .setAutoCancel(true)
            .addAction(0, "Bloquear", action(context, ACTION_BLOCK, number, 1))
            .addAction(0, "Permitir siempre", action(context, ACTION_ALLOW, number, 2))
            .addAction(0, "Descartar", action(context, ACTION_DISMISS, number, 3))
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun action(context: Context, action: String, number: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, KimoxPostCallActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_NUMBER, number)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    const val ACTION_BLOCK = "com.kimoxcallsecurity.POST_CALL_BLOCK"
    const val ACTION_ALLOW = "com.kimoxcallsecurity.POST_CALL_ALLOW"
    const val ACTION_DISMISS = "com.kimoxcallsecurity.POST_CALL_DISMISS"
    const val EXTRA_NUMBER = "number"
    const val NOTIFICATION = NOTIFICATION_ID
}

class KimoxPhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val appContext = context.applicationContext
        val preferences = SecurePreferences(appContext)
        preferences.putString(KimoxSecurityModule.LAST_PHONE_EVENT_AT, System.currentTimeMillis().toString())
        KimoxDiagnostics.record(
            appContext,
            "INFO",
            "PHONE_STATE_RECEIVED",
            "state=$state; hasNumber=${intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)}",
        )
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (!number.isNullOrBlank()) {
                    generation++
                    lastNumberAt = SystemClock.elapsedRealtime()
                    KimoxLegacyCallHandler.handleRinging(appContext, number)
                } else {
                    val token = ++generation
                    val pendingResult = goAsync()
                    handler.postDelayed({
                        try {
                            val recentlyReceivedNumber = SystemClock.elapsedRealtime() - lastNumberAt < 2_000
                            if (token == generation && !recentlyReceivedNumber) {
                                KimoxLegacyCallHandler.handleRinging(appContext, null)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }, 500)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                generation++
                KimoxPostCallPrompt.showIfPending(appContext)
            }
        }
    }

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var generation = 0
        private var lastNumberAt = 0L
    }
}

class KimoxPostCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra(KimoxPostCallPrompt.EXTRA_NUMBER)?.filter(Char::isDigit)
        val preferences = SecurePreferences(context)
        when (intent.action) {
            KimoxPostCallPrompt.ACTION_BLOCK -> if (!number.isNullOrEmpty()) {
                preferences.putStringSet(
                    KimoxSecurityModule.BLOCKED_NUMBERS,
                    preferences.getStringSet(KimoxSecurityModule.BLOCKED_NUMBERS) + number,
                )
            }
            KimoxPostCallPrompt.ACTION_ALLOW -> if (!number.isNullOrEmpty()) {
                preferences.putStringSet(
                    KimoxSecurityModule.WHITELIST_NUMBERS,
                    preferences.getStringSet(KimoxSecurityModule.WHITELIST_NUMBERS) + number,
                )
            }
        }
        context.getSystemService(NotificationManager::class.java).cancel(KimoxPostCallPrompt.NOTIFICATION)
    }
}
