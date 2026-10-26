package com.kimoxcallsecurity

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Small, process-safe readiness check used after boot and package updates. */
object KimoxProtectionReadiness {
    data class State(val ready: Boolean, val issue: String?)

    fun check(context: Context): State {
        if (!SecurePreferences(context).getBoolean(KimoxSecurityModule.PROTECTION_ENABLED, true)) {
            return State(false, "Protección desactivada por el usuario")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            val ready = roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            return State(ready, if (ready) null else "Activa a Kimox como filtro de llamadas")
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return State(false, "Esta versión de Android no es compatible")
        val required = mutableListOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) required += Manifest.permission.ANSWER_PHONE_CALLS
        val ready = required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        return State(ready, if (ready) null else "Concede los permisos de teléfono a Kimox")
    }
}
