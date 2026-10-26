package com.kimoxcallsecurity

import android.content.Context

object KimoxCallHistory {
    private const val MAX_HISTORY = 200

    fun record(context: Context, number: String, action: String, reason: String) {
        val preferences = SecurePreferences(context)
        val current = preferences.getStringSet(KimoxSecurityModule.CALL_HISTORY).toMutableSet()
        current.add("${System.currentTimeMillis()}|$number|$action|$reason")
        val retentionDays = preferences.getInt(KimoxSecurityModule.HISTORY_RETENTION_DAYS, 30)
        val cutoff = if (retentionDays == 0) Long.MIN_VALUE else System.currentTimeMillis() - retentionDays * 86_400_000L
        val limited = current.filter { (it.substringBefore('|').toLongOrNull() ?: 0L) >= cutoff }
            .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }
            .take(MAX_HISTORY)
            .toSet()
        preferences.putStringSet(KimoxSecurityModule.CALL_HISTORY, limited)
    }
}
