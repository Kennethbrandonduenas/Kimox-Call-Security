package com.kimoxcallsecurity

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class KimoxSecurityModule(
    private val context: ReactApplicationContext,
) : ReactContextBaseJavaModule(context) {
    override fun getName() = "KimoxSecurity"

    private val preferences by lazy { SecurePreferences(context) }

    @ReactMethod
    fun setBlockedNumbers(numbers: ReadableArray) {
        preferences.putStringSet(BLOCKED_NUMBERS, numbers.toStringSet())
    }

    @ReactMethod
    fun getBlockedNumbers(promise: Promise) {
        promise.resolve(preferences.getStringSet(BLOCKED_NUMBERS).toWritableArray())
    }

    @ReactMethod
    fun saveSecurityRules(rules: ReadableMap) {
        val whitelist = rules.getArray("whitelist")?.toStringSet() ?: emptySet()
        val prefixes = rules.getArray("blockedPrefixes")?.toStringSet() ?: emptySet()
        val countries = rules.getArray("blockedCountries")?.toStringSet() ?: emptySet()
        preferences.putStringSet(WHITELIST_NUMBERS, whitelist)
        preferences.putStringSet(BLOCKED_PREFIXES, prefixes)
        preferences.putStringSet(BLOCKED_COUNTRIES, countries)
        preferences.putBoolean(BLOCK_PRIVATE, rules.booleanOrDefault("blockPrivate", false))
        preferences.putBoolean(BLOCK_UNKNOWN, rules.booleanOrDefault("blockUnknown", false))
        preferences.putBoolean(NOTIFICATIONS_ENABLED, rules.booleanOrDefault("notificationsEnabled", true))
        preferences.putBoolean(HIDE_NOTIFICATION_NUMBERS, rules.booleanOrDefault("hideNumbersInNotifications", true))
        preferences.putInt(HISTORY_RETENTION_DAYS, rules.intOrDefault("historyRetentionDays", 30).takeIf { it in VALID_RETENTION_DAYS } ?: 30)
        preferences.putString(SCREENING_ACTION, rules.stringOrDefault("action", "reject"))
    }

    @ReactMethod
    fun getSecurityRules(promise: Promise) {
        val result = Arguments.createMap().apply {
            putArray("whitelist", preferences.getStringSet(WHITELIST_NUMBERS).toWritableArray())
            putArray("blockedPrefixes", preferences.getStringSet(BLOCKED_PREFIXES).toWritableArray())
            putArray("blockedCountries", preferences.getStringSet(BLOCKED_COUNTRIES).toWritableArray())
            putBoolean("blockPrivate", preferences.getBoolean(BLOCK_PRIVATE, false))
            putBoolean("blockUnknown", preferences.getBoolean(BLOCK_UNKNOWN, false))
            putBoolean("notificationsEnabled", preferences.getBoolean(NOTIFICATIONS_ENABLED, true))
            putBoolean("hideNumbersInNotifications", preferences.getBoolean(HIDE_NOTIFICATION_NUMBERS, true))
            putInt("historyRetentionDays", preferences.getInt(HISTORY_RETENTION_DAYS, 30))
            putString("action", preferences.getString(SCREENING_ACTION, "reject"))
        }
        promise.resolve(result)
    }

    @ReactMethod
    fun getHistory(promise: Promise) {
        val history = pruneHistory(preferences.getStringSet(CALL_HISTORY))
            .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }
        preferences.putStringSet(CALL_HISTORY, history.toSet())
        promise.resolve(history.toWritableArray())
    }

    @ReactMethod
    fun clearHistory() {
        preferences.remove(CALL_HISTORY)
    }

    @ReactMethod
    fun clearAllData() {
        preferences.clear()
    }

    @ReactMethod
    fun encryptBackup(content: String, password: String, promise: Promise) {
        runCatching {
            require(password.length >= 8) { "La contraseña debe tener al menos 8 caracteres." }
            val salt = ByteArray(16).also(SecureRandom()::nextBytes)
            val iv = ByteArray(12).also(SecureRandom()::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, passwordKey(password, salt), GCMParameterSpec(128, iv))
            listOf(BACKUP_PREFIX, salt.encode(), iv.encode(), cipher.doFinal(content.toByteArray()).encode()).joinToString(":")
        }.onSuccess(promise::resolve).onFailure { promise.reject("BACKUP_ENCRYPTION_FAILED", it.message, it) }
    }

    @ReactMethod
    fun decryptBackup(content: String, password: String, promise: Promise) {
        runCatching {
            val parts = content.trim().split(':')
            require(parts.size == 4 && parts[0] == BACKUP_PREFIX) { "El respaldo cifrado no es válido." }
            val salt = parts[1].decode()
            val iv = parts[2].decode()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, passwordKey(password, salt), GCMParameterSpec(128, iv))
            String(cipher.doFinal(parts[3].decode()), Charsets.UTF_8)
        }.onSuccess(promise::resolve).onFailure { promise.reject("BACKUP_DECRYPTION_FAILED", "Contraseña incorrecta o respaldo dañado.", it) }
    }

    @ReactMethod
    fun setProtectionEnabled(enabled: Boolean) {
        preferences.putBoolean(PROTECTION_ENABLED, enabled)
    }

    @ReactMethod
    fun getProtectionEnabled(promise: Promise) {
        promise.resolve(preferences.getBoolean(PROTECTION_ENABLED, true))
    }

    @ReactMethod
    fun getDiagnostics(promise: Promise) {
        val roleEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = context.getSystemService(RoleManager::class.java)
            manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else false
        val notificationEnabled = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        val contactsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        promise.resolve(Arguments.createMap().apply {
            putBoolean("callScreening", roleEnabled)
            putBoolean("notifications", notificationEnabled)
            putBoolean("contacts", contactsEnabled)
            putBoolean("overlay", Settings.canDrawOverlays(context))
            putBoolean("accessibility", accessibilityManager.isEnabled)
            putBoolean("batteryUnrestricted", powerManager.isIgnoringBatteryOptimizations(context.packageName))
        })
    }

    @ReactMethod
    fun isCallScreeningEnabled(promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            promise.resolve(false)
            return
        }
        val manager = context.getSystemService(RoleManager::class.java)
        promise.resolve(manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }

    @ReactMethod
    fun requestCallScreeningRole() {
        val activity = context.currentActivity ?: return
        val permissions = mutableListOf(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        activity.requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = context.getSystemService(RoleManager::class.java)
            if (manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                activity.startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), ROLE_REQUEST_CODE)
            }
        }
    }

    @ReactMethod
    fun openAppSettings() = openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))

    @ReactMethod
    fun openAccessibilitySettings() = openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    @ReactMethod
    fun openOverlaySettings() = openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))

    @ReactMethod
    fun openBatterySettings() = openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    @ReactMethod
    fun scanInstalledApps(promise: Promise) {
        val manager = context.packageManager
        val result = Arguments.createArray()
        manager.getInstalledApplications(0).forEach { app ->
            val packageInfo = manager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val sensitive = permissions.filter { it in SENSITIVE_PERMISSIONS }
            val system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val installer = try {
                installerName(manager, app.packageName)
            } catch (_: Exception) {
                "desconocido"
            }
            result.pushMap(Arguments.createMap().apply {
                putString("packageName", app.packageName)
                putString("name", manager.getApplicationLabel(app).toString())
                putBoolean("isSystem", system)
                putString("installer", installer)
                putString("risk", if (system) "low" else if (sensitive.size >= 3 || installer == "desconocido") "high" else if (sensitive.isNotEmpty()) "review" else "low")
                putArray("sensitivePermissions", sensitive.map { it.substringAfterLast('.') }.toSet().toWritableArray())
            })
        }
        promise.resolve(result)
    }

    private fun openSettings(intent: Intent) {
        context.currentActivity?.startActivity(intent)
    }

    @Suppress("DEPRECATION")
    private fun installerName(manager: PackageManager, packageName: String): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) manager.getInstallSourceInfo(packageName).installingPackageName ?: "desconocido"
        else manager.getInstallerPackageName(packageName) ?: "desconocido"

    private fun ReadableArray.toStringSet(): Set<String> = buildSet {
        for (index in 0 until size()) getString(index)?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
    }

    private fun Set<String>.toWritableArray() = Arguments.createArray().also { array -> forEach(array::pushString) }
    private fun List<String>.toWritableArray() = Arguments.createArray().also { array -> forEach(array::pushString) }
    private fun ReadableMap.booleanOrDefault(key: String, default: Boolean) = if (hasKey(key) && !isNull(key)) getBoolean(key) else default
    private fun ReadableMap.stringOrDefault(key: String, default: String) = if (hasKey(key) && !isNull(key)) getString(key) ?: default else default
    private fun ReadableMap.intOrDefault(key: String, default: Int) = if (hasKey(key) && !isNull(key)) getInt(key) else default

    private fun pruneHistory(history: Set<String>): List<String> {
        val days = preferences.getInt(HISTORY_RETENTION_DAYS, 30)
        if (days == 0) return history.toList()
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        return history.filter { (it.substringBefore('|').toLongOrNull() ?: 0L) >= cutoff }
    }

    private fun passwordKey(password: String, salt: ByteArray) = SecretKeySpec(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 210_000, 256)).encoded,
        "AES",
    )
    private fun ByteArray.encode() = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode() = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val PREFERENCES_NAME = "kimox_security"
        const val BLOCKED_NUMBERS = "blocked_numbers"
        const val WHITELIST_NUMBERS = "whitelist_numbers"
        const val BLOCKED_PREFIXES = "blocked_prefixes"
        const val BLOCKED_COUNTRIES = "blocked_countries"
        const val BLOCK_PRIVATE = "block_private"
        const val BLOCK_UNKNOWN = "block_unknown"
        const val SCREENING_ACTION = "screening_action"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val HIDE_NOTIFICATION_NUMBERS = "hide_notification_numbers"
        const val HISTORY_RETENTION_DAYS = "history_retention_days"
        const val PROTECTION_ENABLED = "protection_enabled"
        const val CALL_HISTORY = "call_history"
        private const val PERMISSION_REQUEST_CODE = 7002
        private const val ROLE_REQUEST_CODE = 7001
        private val VALID_RETENTION_DAYS = setOf(0, 7, 30, 90)
        private const val BACKUP_PREFIX = "kimoxenc1"
        private val SENSITIVE_PERMISSIONS = setOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.REQUEST_INSTALL_PACKAGES,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        )
    }
}
