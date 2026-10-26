package com.kimoxcallsecurity

import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

/** Compatibility bridge for Android 7 and 8 call blocking. */
object KimoxLegacyCallTerminator {
    data class Result(val ended: Boolean, val mechanism: String, val error: String? = null)

    fun endIncomingCall(context: Context): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return runCatching {
                Result(context.getSystemService(TelecomManager::class.java).endCall(), "telecom_end_call")
            }.getOrElse { Result(false, "telecom_end_call", it.javaClass.simpleName) }
        }
        return runCatching {
            val manager = context.getSystemService(TelephonyManager::class.java)
            val getService = manager.javaClass.getDeclaredMethod("getITelephony")
            getService.isAccessible = true
            val service = getService.invoke(manager)
                ?: return Result(false, "legacy_telephony", "ServiceUnavailable")
            val result = service.javaClass.getMethod("endCall").invoke(service)
            Result(result as? Boolean ?: true, "legacy_telephony")
        }.getOrElse { Result(false, "legacy_telephony", it.javaClass.simpleName) }
    }
}
