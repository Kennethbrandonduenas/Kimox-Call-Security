package com.kimoxcallsecurity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object KimoxLegacyCallHandler {
    fun handleRinging(context: Context, rawNumber: String?) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return
        if (!hasPermissions(context)) {
            KimoxDiagnostics.record(context, "WARN", "LEGACY_CALL_SKIPPED", "missingPhonePermissions=true")
            return
        }
        val preferences = SecurePreferences(context)
        preferences.putString(KimoxSecurityModule.LAST_CALL_FILTER_AT, System.currentTimeMillis().toString())
        KimoxDiagnostics.record(context, "INFO", "LEGACY_CALL_EVALUATING", "hasNumber=${!rawNumber.isNullOrBlank()}")
        if (!preferences.getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)) return
        val number = rawNumber?.filter(Char::isDigit)?.takeIf { it.isNotEmpty() }
        val whitelist = preferences.getStringSet(KimoxSecurityModule.WHITELIST_NUMBERS)
            .map { it.filter(Char::isDigit) }.toSet()
        if (number != null && number in whitelist) return
        val isKnownContact = number?.let { isContact(context, it) }
        val countryCode = preferences.getString(
            KimoxSecurityModule.LOCAL_COUNTRY_CODE,
            DeviceCountryDetector.detect(context).dialingCode.ifEmpty { "51" },
        )?.filter(Char::isDigit).orEmpty()
        val trimmed = rawNumber?.trim().orEmpty()
        val international = trimmed.startsWith("+") || trimmed.startsWith("00")
        val internationalDigits = if (trimmed.startsWith("00")) number?.removePrefix("00") else number
        val reason = CallRuleEvaluator.evaluate(CallRuleInput(
            number = number,
            isPrivate = number == null,
            isKnownContact = isKnownContact,
            whitelist = whitelist,
            blockedNumbers = preferences.getStringSet(KimoxSecurityModule.BLOCKED_NUMBERS)
                .map { it.filter(Char::isDigit) }.toSet(),
            blockedPrefixes = preferences.getStringSet(KimoxSecurityModule.BLOCKED_PREFIXES)
                .map { it.filter(Char::isDigit) }.filter { it.isNotEmpty() },
            blockedCountries = preferences.getStringSet(KimoxSecurityModule.BLOCKED_COUNTRIES)
                .map { it.filter(Char::isDigit) }.filter { it.isNotEmpty() },
            blockPrivate = preferences.getBoolean(KimoxSecurityModule.BLOCK_PRIVATE, true),
            blockUnknown = preferences.getBoolean(KimoxSecurityModule.BLOCK_UNKNOWN, false),
            blockInternational = preferences.getBoolean(KimoxSecurityModule.BLOCK_INTERNATIONAL, true),
            allowInternationalContacts = preferences.getBoolean(KimoxSecurityModule.ALLOW_INTERNATIONAL_CONTACTS, true),
            isForeignInternational = international && countryCode.isNotEmpty() &&
                internationalDigits?.startsWith(countryCode) == false,
        ))
        if (reason == null) {
            preferences.putString(KimoxSecurityModule.LAST_CALL_OUTCOME, "Permitida: no coincide con una regla")
            KimoxCallHistory.record(context, rawNumber ?: "Número privado", "allowed", "Permitida: no coincide con una regla")
            if (number != null && isKnownContact == false &&
                preferences.getBoolean(KimoxSecurityModule.POST_CALL_PROMPT_ENABLED, true)) {
                KimoxPostCallPrompt.rememberCandidate(context, rawNumber ?: number)
            }
            return
        }
        val action = preferences.getString(KimoxSecurityModule.SCREENING_ACTION, "reject") ?: "reject"
        if (action == "silence") {
            KimoxCallHistory.record(context, rawNumber ?: "Número privado", action, reason)
            preferences.putString(KimoxSecurityModule.LAST_CALL_OUTCOME, "Silenciar no es compatible con el modo Android 9")
            KimoxDiagnostics.record(context, "WARN", "LEGACY_SILENCE_UNSUPPORTED", "reason=$reason")
            return
        }
        val result = KimoxLegacyCallTerminator.endIncomingCall(context)
        val ended = result.ended
        KimoxCallHistory.record(context, rawNumber ?: "Número privado", action, reason)
        KimoxDiagnostics.record(
            context,
            if (ended) "INFO" else "ERROR",
            "LEGACY_CALL_FILTERED",
            "reason=$reason; action=$action; ended=$ended; mechanism=${result.mechanism}; error=${result.error.orEmpty()}",
        )
        preferences.putString(KimoxSecurityModule.LAST_CALL_OUTCOME, if (ended) "Bloqueo confirmado: $reason" else "Android no confirmó el bloqueo: $reason")
    }

    private fun hasPermissions(context: Context): Boolean {
        val required = mutableListOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) required += Manifest.permission.ANSWER_PHONE_CALLS
        return required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun isContact(context: Context, number: String): Boolean? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number),
        )
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use {
            return it.moveToFirst()
        }
        return false
    }
}
