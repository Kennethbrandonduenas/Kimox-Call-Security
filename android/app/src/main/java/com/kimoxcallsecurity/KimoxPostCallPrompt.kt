package com.kimoxcallsecurity

import android.app.AlarmManager
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
    internal const val IGNORED_NUMBERS = "post_call_ignored_numbers"
    internal const val PRIVATE_CANDIDATE = "__private__"
    internal const val IGNORE_PRIVATE_CANDIDATE = "post_call_ignore_private"
    private const val CANDIDATE_MAX_AGE_MS = 6 * 60 * 60 * 1000L
    private const val REPEAT_DELAY_MS = 24 * 60 * 60 * 1000L
    private const val REMINDER_DELAY_MS = 45 * 1000L
    private const val REMINDER_REQUEST_CODE = 5103

    fun rememberCandidate(context: Context, number: String) {
        val preferences = SecurePreferences(context)
        val normalizedNumber = number.filter(Char::isDigit)
        if (normalizedNumber.length < 7) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_CANDIDATE_SKIPPED", "invalidNumber=true")
            return
        }
        if (normalizedNumber in preferences.getStringSet(IGNORED_NUMBERS) ||
            normalizedNumber in preferences.getStringSet(KimoxSecurityModule.BLOCKED_NUMBERS) ||
            normalizedNumber in preferences.getStringSet(KimoxSecurityModule.WHITELIST_NUMBERS)) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_CANDIDATE_SKIPPED", "alreadyDecided=true")
            return
        }
        preferences.putString(CANDIDATE_NUMBER, normalizedNumber)
        preferences.putString(CANDIDATE_TIME, System.currentTimeMillis().toString())
        KimoxDiagnostics.record(context, "INFO", "POST_CALL_CANDIDATE_SAVED", "numberPresent=${number.isNotBlank()}")
        scheduleFallback(context)
    }

    fun rememberPrivateCandidate(context: Context) {
        val preferences = SecurePreferences(context)
        if (preferences.getBoolean(KimoxSecurityModule.BLOCK_PRIVATE, true) ||
            preferences.getBoolean(IGNORE_PRIVATE_CANDIDATE, false)) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_PRIVATE_SKIPPED", "alreadyDecided=true")
            return
        }
        preferences.putString(CANDIDATE_NUMBER, PRIVATE_CANDIDATE)
        preferences.putString(CANDIDATE_TIME, System.currentTimeMillis().toString())
        KimoxDiagnostics.record(context, "INFO", "POST_CALL_PRIVATE_CANDIDATE_SAVED")
        scheduleFallback(context)
    }

    fun showIfPending(context: Context) {
        cancelFallback(context)
        val preferences = SecurePreferences(context)
        if (!preferences.getBoolean(KimoxSecurityModule.POST_CALL_PROMPT_ENABLED, true)) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_PROMPT_SKIPPED", "disabled=true")
            return
        }
        val number = preferences.getString(CANDIDATE_NUMBER, "")?.takeIf { it.isNotEmpty() } ?: run {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_PROMPT_SKIPPED", "candidateMissing=true")
            return
        }
        val candidateTime = preferences.getString(CANDIDATE_TIME, "0")?.toLongOrNull() ?: 0L
        preferences.remove(CANDIDATE_NUMBER)
        preferences.remove(CANDIDATE_TIME)
        val now = System.currentTimeMillis()
        if (now - candidateTime !in 0..CANDIDATE_MAX_AGE_MS) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_PROMPT_SKIPPED", "candidateExpired=true")
            return
        }
        val lastNumber = preferences.getString(LAST_PROMPT_NUMBER, "").orEmpty()
        val lastTime = preferences.getString(LAST_PROMPT_TIME, "0")?.toLongOrNull() ?: 0L
        if (lastNumber == number && now - lastTime < REPEAT_DELAY_MS) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_PROMPT_SKIPPED", "repeatSuppressed=true")
            return
        }
        preferences.putString(LAST_PROMPT_NUMBER, number)
        preferences.putString(LAST_PROMPT_TIME, now.toString())

        val manager = context.getSystemService(NotificationManager::class.java)
        val isPrivate = number == PRIVATE_CANDIDATE
        val notification = NotificationCompat.Builder(context, KimoxBackgroundManager.POST_CALL_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("¿Qué deseas hacer con este número?")
            .setContentTitle(if (isPrivate) "Bloquear llamadas privadas" else "¿Qué deseas hacer con este número?")
            .setContentText(if (isPrivate) "La llamada no mostró un número" else number)
            .setAutoCancel(true)
            .addAction(0, if (isPrivate) "Bloquear privados" else "Bloquear", action(context, ACTION_BLOCK, number, 1))
            .addAction(0, if (isPrivate) "Permitir privados" else "Permitir siempre", action(context, ACTION_ALLOW, number, 2))
            .addAction(0, "Descartar", action(context, ACTION_DISMISS, number, 3))
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        KimoxDiagnostics.record(context, "INFO", "POST_CALL_PROMPT_POSTED", "numberPresent=true")
    }

    private fun scheduleFallback(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + REMINDER_DELAY_MS,
                reminderIntent(context),
            )
        }.onSuccess {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_REMINDER_SCHEDULED", "delaySeconds=45")
        }.onFailure {
            KimoxDiagnostics.record(context, "WARN", "POST_CALL_REMINDER_FAILED", it.javaClass.simpleName)
        }
    }

    fun retryFallback(context: Context) = scheduleFallback(context)

    private fun cancelFallback(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(reminderIntent(context))
    }

    private fun reminderIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REMINDER_REQUEST_CODE,
        Intent(context, KimoxPostCallReminderReceiver::class.java).setAction(ACTION_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
    const val ACTION_REMINDER = "com.kimoxcallsecurity.POST_CALL_REMINDER"
    const val EXTRA_NUMBER = "number"
    const val NOTIFICATION = NOTIFICATION_ID
}

class KimoxPostCallReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != KimoxPostCallPrompt.ACTION_REMINDER) return
        val callState = context.getSystemService(TelephonyManager::class.java).callState
        if (callState != TelephonyManager.CALL_STATE_IDLE) {
            KimoxDiagnostics.record(context, "INFO", "POST_CALL_REMINDER_DEFERRED", "callState=$callState")
            KimoxPostCallPrompt.retryFallback(context.applicationContext)
            return
        }
        KimoxDiagnostics.record(context, "INFO", "POST_CALL_REMINDER_FIRED")
        KimoxPostCallPrompt.showIfPending(context.applicationContext)
    }
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
        val rawCandidate = intent.getStringExtra(KimoxPostCallPrompt.EXTRA_NUMBER)
        val isPrivateCandidate = rawCandidate == KimoxPostCallPrompt.PRIVATE_CANDIDATE
        val number = rawCandidate?.filter(Char::isDigit)
        val preferences = SecurePreferences(context)
        when (intent.action) {
            KimoxPostCallPrompt.ACTION_BLOCK -> if (isPrivateCandidate) {
                preferences.putBoolean(KimoxSecurityModule.BLOCK_PRIVATE, true)
            } else if (!number.isNullOrEmpty()) {
                preferences.putStringSet(
                    KimoxSecurityModule.BLOCKED_NUMBERS,
                    preferences.getStringSet(KimoxSecurityModule.BLOCKED_NUMBERS) + number,
                )
            }
            KimoxPostCallPrompt.ACTION_ALLOW -> if (isPrivateCandidate) {
                preferences.putBoolean(KimoxSecurityModule.BLOCK_PRIVATE, false)
            } else if (!number.isNullOrEmpty()) {
                preferences.putStringSet(
                    KimoxSecurityModule.WHITELIST_NUMBERS,
                    preferences.getStringSet(KimoxSecurityModule.WHITELIST_NUMBERS) + number,
                )
            }
            KimoxPostCallPrompt.ACTION_DISMISS -> if (isPrivateCandidate) {
                preferences.putBoolean(KimoxPostCallPrompt.IGNORE_PRIVATE_CANDIDATE, true)
                KimoxDiagnostics.record(context, "INFO", "POST_CALL_PRIVATE_IGNORED")
            } else if (!number.isNullOrEmpty()) {
                preferences.putStringSet(
                    KimoxPostCallPrompt.IGNORED_NUMBERS,
                    preferences.getStringSet(KimoxPostCallPrompt.IGNORED_NUMBERS) + number,
                )
                KimoxDiagnostics.record(context, "INFO", "POST_CALL_PROMPT_IGNORED", "numberPresent=true")
            }
        }
        context.getSystemService(NotificationManager::class.java).cancel(KimoxPostCallPrompt.NOTIFICATION)
    }
}
