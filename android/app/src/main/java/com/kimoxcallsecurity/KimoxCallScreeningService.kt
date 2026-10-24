package com.kimoxcallsecurity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class KimoxCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val preferences = SecurePreferences(this)
        if (!preferences.getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart
        val number = rawNumber?.normalizePhoneNumber()
        val presentation = callDetails.handlePresentation
        val isPrivate = number.isNullOrEmpty() || presentation == TelecomManager.PRESENTATION_RESTRICTED
        val whitelist = preferences.getStringSet(KimoxSecurityModule.WHITELIST_NUMBERS).map { it.normalizePhoneNumber() }.toSet()

        if (number != null && number in whitelist) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val blockedNumbers = preferences.getStringSet(KimoxSecurityModule.BLOCKED_NUMBERS).map { it.normalizePhoneNumber() }.toSet()
        val blockedPrefixes = preferences.getStringSet(KimoxSecurityModule.BLOCKED_PREFIXES).map { it.normalizePhoneNumber() }.filter { it.isNotEmpty() }
        val blockedCountries = preferences.getStringSet(KimoxSecurityModule.BLOCKED_COUNTRIES).map { it.normalizePhoneNumber() }.filter { it.isNotEmpty() }
        val blockUnknown = preferences.getBoolean(KimoxSecurityModule.BLOCK_UNKNOWN, false)
        val blockPrivate = preferences.getBoolean(KimoxSecurityModule.BLOCK_PRIVATE, false)

        val reason = CallRuleEvaluator.evaluate(CallRuleInput(
            number = number,
            isPrivate = isPrivate,
            isKnownContact = number?.let(::isContact),
            whitelist = whitelist,
            blockedNumbers = blockedNumbers,
            blockedPrefixes = blockedPrefixes,
            blockedCountries = blockedCountries,
            blockPrivate = blockPrivate,
            blockUnknown = blockUnknown,
        ))

        if (reason == null) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val action = preferences.getString(KimoxSecurityModule.SCREENING_ACTION, "reject") ?: "reject"
        val response = when (action) {
            "silence" -> CallResponse.Builder().setSilenceCall(true).build()
            "block" -> CallResponse.Builder().setDisallowCall(true).setRejectCall(false).setSkipCallLog(false).setSkipNotification(false).build()
            else -> CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipCallLog(false).setSkipNotification(false).build()
        }
        val displayNumber = rawNumber ?: "Número privado"
        saveHistory(displayNumber, action, reason)
        if (preferences.getBoolean(KimoxSecurityModule.NOTIFICATIONS_ENABLED, true)) {
            val notificationNumber = if (preferences.getBoolean(KimoxSecurityModule.HIDE_NOTIFICATION_NUMBERS, true)) maskNumber(displayNumber) else displayNumber
            notifyScreened(notificationNumber, action)
        }
        respondToCall(callDetails, response)
    }

    private fun isContact(number: String): Boolean? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val uri = UriHelper.contactLookup(number)
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { return it.moveToFirst() }
        return false
    }

    private fun saveHistory(number: String, action: String, reason: String) {
        val preferences = SecurePreferences(this)
        val current = preferences.getStringSet(KimoxSecurityModule.CALL_HISTORY).toMutableSet()
        current.add("${System.currentTimeMillis()}|$number|$action|$reason")
        val retentionDays = preferences.getInt(KimoxSecurityModule.HISTORY_RETENTION_DAYS, 30)
        val cutoff = if (retentionDays == 0) Long.MIN_VALUE else System.currentTimeMillis() - retentionDays * 86_400_000L
        val limited = current.filter { (it.substringBefore('|').toLongOrNull() ?: 0L) >= cutoff }
            .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }.take(MAX_HISTORY).toSet()
        preferences.putStringSet(KimoxSecurityModule.CALL_HISTORY, limited)
    }

    private fun notifyScreened(number: String, action: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Protección de llamadas", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val verb = if (action == "silence") "silenciada" else "bloqueada"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Llamada $verb")
            .setContentText(number)
            .setAutoCancel(true)
            .build()
        manager.notify((number + System.currentTimeMillis()).hashCode(), notification)
    }

    private fun String.normalizePhoneNumber() = filter(Char::isDigit)

    private fun maskNumber(number: String): String {
        val digits = number.filter(Char::isDigit)
        return if (digits.length < 4) "Número oculto" else "•••• ${digits.takeLast(4)}"
    }

    private object UriHelper {
        fun contactLookup(number: String) = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number),
        )
    }

    companion object {
        private const val CHANNEL_ID = "kimox_calls"
        private const val MAX_HISTORY = 200
    }
}
