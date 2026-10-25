package com.kimoxcallsecurity

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.NotificationManager
import android.app.KeyguardManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import java.security.SecureRandom
import java.security.MessageDigest
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.io.File
import java.io.FileOutputStream

class KimoxSecurityModule(
    private val context: ReactApplicationContext,
) : ReactContextBaseJavaModule(context) {
    override fun getName() = "KimoxSecurity"

    private val preferences by lazy { SecurePreferences(context) }
    private var apkScanPromise: Promise? = null
    private val activityListener = object : BaseActivityEventListener() {
        override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode != APK_PICK_REQUEST) return
            val promise = apkScanPromise.also { apkScanPromise = null } ?: return
            val uri = data?.data
            if (resultCode != Activity.RESULT_OK || uri == null) {
                promise.reject("APK_SCAN_CANCELLED", "No se seleccionó ningún APK.")
                return
            }
            runCatching { scanSelectedApk(uri) }
                .onSuccess(promise::resolve)
                .onFailure { promise.reject("APK_SCAN_FAILED", it.message ?: "El archivo no es un APK válido.", it) }
        }
    }

    init {
        context.addActivityEventListener(activityListener)
    }

    @ReactMethod
    fun recordDiagnostic(level: String, event: String, details: String?) {
        KimoxDiagnostics.record(context, level, event, details)
    }

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
        preferences.putBoolean(BLOCK_PRIVATE, rules.booleanOrDefault("blockPrivate", true))
        preferences.putBoolean(BLOCK_UNKNOWN, rules.booleanOrDefault("blockUnknown", false))
        preferences.putBoolean(BLOCK_INTERNATIONAL, rules.booleanOrDefault("blockInternational", true))
        preferences.putBoolean(ALLOW_INTERNATIONAL_CONTACTS, rules.booleanOrDefault("allowInternationalContacts", true))
        preferences.putBoolean(POST_CALL_PROMPT_ENABLED, rules.booleanOrDefault("postCallPromptEnabled", true))
        preferences.putString(LOCAL_COUNTRY_CODE, rules.stringOrDefault("localCountryCode", "51").filter(Char::isDigit))
        preferences.putBoolean(NOTIFICATIONS_ENABLED, rules.booleanOrDefault("notificationsEnabled", true))
        preferences.putBoolean(HIDE_NOTIFICATION_NUMBERS, rules.booleanOrDefault("hideNumbersInNotifications", true))
        preferences.putInt(HISTORY_RETENTION_DAYS, rules.intOrDefault("historyRetentionDays", 30).takeIf { it in VALID_RETENTION_DAYS } ?: 30)
        preferences.putString(SCREENING_ACTION, rules.stringOrDefault("action", "reject"))
    }

    @ReactMethod
    fun getSecurityRules(promise: Promise) {
        val detectedCountry = DeviceCountryDetector.detect(context)
        val result = Arguments.createMap().apply {
            putArray("whitelist", preferences.getStringSet(WHITELIST_NUMBERS).toWritableArray())
            putArray("blockedPrefixes", preferences.getStringSet(BLOCKED_PREFIXES).toWritableArray())
            putArray("blockedCountries", preferences.getStringSet(BLOCKED_COUNTRIES).toWritableArray())
            putBoolean("blockPrivate", preferences.getBoolean(BLOCK_PRIVATE, true))
            putBoolean("blockUnknown", preferences.getBoolean(BLOCK_UNKNOWN, false))
            putBoolean("blockInternational", preferences.getBoolean(BLOCK_INTERNATIONAL, true))
            putBoolean("allowInternationalContacts", preferences.getBoolean(ALLOW_INTERNATIONAL_CONTACTS, true))
            putBoolean("postCallPromptEnabled", preferences.getBoolean(POST_CALL_PROMPT_ENABLED, true))
            putString("localCountryCode", preferences.getString(LOCAL_COUNTRY_CODE, detectedCountry.dialingCode.ifEmpty { "51" }))
            putBoolean("notificationsEnabled", preferences.getBoolean(NOTIFICATIONS_ENABLED, true))
            putBoolean("hideNumbersInNotifications", preferences.getBoolean(HIDE_NOTIFICATION_NUMBERS, true))
            putInt("historyRetentionDays", preferences.getInt(HISTORY_RETENTION_DAYS, 30))
            putString("action", preferences.getString(SCREENING_ACTION, "reject"))
        }
        promise.resolve(result)
    }

    @ReactMethod
    fun detectDeviceCountry(promise: Promise) {
        val country = DeviceCountryDetector.detect(context)
        promise.resolve(Arguments.createMap().apply {
            putString("iso", country.iso)
            putString("name", country.name)
            putString("dialingCode", country.dialingCode)
            putString("source", country.source)
        })
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
        KimoxBackgroundManager.initialize(context)
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
        } else legacyCallPermissionsGranted()
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
            putBoolean("backgroundReady", preferences.getBoolean(PROTECTION_ENABLED, true))
            putBoolean("periodicAudit", preferences.getBoolean(KimoxBackgroundManager.PERIODIC_AUDIT_ENABLED, false) &&
                preferences.getBoolean(MALWARE_CONSENT, false))
        })
    }

    @ReactMethod
    fun isCallScreeningEnabled(promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            promise.resolve(legacyCallPermissionsGranted())
            return
        }
        val manager = context.getSystemService(RoleManager::class.java)
        promise.resolve(manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }

    @ReactMethod
    fun requestCallScreeningRole() {
        val activity = context.currentActivity ?: return
        val permissions = mutableListOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.READ_CALL_LOG)
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
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
    fun openManufacturerBatterySettings() {
        val huaweiIntent = Intent().setComponent(ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        )).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(huaweiIntent) }.getOrElse { openBatterySettings() }
    }

    @ReactMethod
    fun openInstalledAppSettings(packageName: String) =
        openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    @ReactMethod
    fun requestUninstallApp(packageName: String) =
        openSettings(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))

    @ReactMethod
    fun setAppTrusted(packageName: String, trusted: Boolean) {
        val packages = preferences.getStringSet(TRUSTED_APPS).toMutableSet()
        if (trusted) packages += packageName else packages -= packageName
        preferences.putStringSet(TRUSTED_APPS, packages)
    }

    @ReactMethod
    fun getMalwareScanHistory(promise: Promise) {
        val history = preferences.getStringSet(MALWARE_SCAN_HISTORY)
            .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }
        promise.resolve(history.toWritableArray())
    }

    @ReactMethod
    fun getPackageEvents(promise: Promise) {
        promise.resolve(preferences.getStringSet(KimoxPackageMonitor.PACKAGE_CHANGES)
            .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }.toWritableArray())
    }

    @ReactMethod
    fun clearPackageEvents() = preferences.remove(KimoxPackageMonitor.PACKAGE_CHANGES)

    @ReactMethod
    fun getMalwareConsent(promise: Promise) = promise.resolve(preferences.getBoolean(MALWARE_CONSENT, false))

    @ReactMethod
    fun setMalwareConsent(accepted: Boolean) {
        preferences.putBoolean(MALWARE_CONSENT, accepted)
        if (!accepted) {
            preferences.putBoolean(KimoxBackgroundManager.PERIODIC_AUDIT_ENABLED, false)
            KimoxSecurityAuditWorker.configure(context, false)
        }
    }

    @ReactMethod
    fun clearMalwareData() {
        listOf(LAST_SCANNED_PACKAGES, LAST_MALWARE_SCAN, TRUSTED_APPS, MALWARE_SCAN_HISTORY, APP_AUDIT_SNAPSHOTS, KimoxPackageMonitor.PACKAGE_CHANGES).forEach(preferences::remove)
        preferences.putBoolean(MALWARE_CONSENT, false)
        preferences.putBoolean(KimoxBackgroundManager.PERIODIC_AUDIT_ENABLED, false)
        KimoxSecurityAuditWorker.configure(context, false)
    }

    @ReactMethod
    fun configurePeriodicAudit(enabled: Boolean) {
        preferences.putBoolean(KimoxBackgroundManager.PERIODIC_AUDIT_ENABLED, enabled)
        KimoxSecurityAuditWorker.configure(
            context,
            enabled && preferences.getBoolean(MALWARE_CONSENT, false),
        )
    }

    @ReactMethod
    fun selectAndScanApk(promise: Promise) {
        val activity = context.currentActivity ?: run {
            promise.reject("NO_ACTIVITY", "Kimox no está visible.")
            return
        }
        if (apkScanPromise != null) {
            promise.reject("APK_SCAN_BUSY", "Ya existe un análisis de APK en curso.")
            return
        }
        apkScanPromise = promise
        activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
        }, APK_PICK_REQUEST)
    }

    @Suppress("DEPRECATION")
    private fun scanSelectedApk(uri: Uri): com.facebook.react.bridge.WritableMap {
        val file = File(context.cacheDir, "kimox_apk_scan_${System.currentTimeMillis()}.apk")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "No se pudo leer el archivo seleccionado." }
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        require(total <= MAX_APK_SIZE) { "El APK supera el límite local de 500 MB." }
                        output.write(buffer, 0, read)
                    }
                }
            }
            val manager = context.packageManager
            val info = manager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or signatureFlag())
                ?: error("Android no reconoce el archivo como un APK válido.")
            val requested = info.requestedPermissions?.toList().orEmpty()
            val sensitive = requested.filter(SENSITIVE_PERMISSIONS::contains)
            val signature = signatureSha256(info)
            val accessibilityService = info.services.orEmpty().any { it.permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE }
            val reasons = mutableListOf<String>()
            var score = 0
            if (signature.isEmpty()) { score += 70; reasons += "No se pudo verificar un certificado de firma" }
            if (Manifest.permission.REQUEST_INSTALL_PACKAGES in sensitive) { score += 20; reasons += "Puede solicitar la instalación de otros APK" }
            if (Manifest.permission.SYSTEM_ALERT_WINDOW in sensitive) { score += 18; reasons += "Solicita mostrarse sobre otras aplicaciones" }
            if (accessibilityService) { score += 35; reasons += "Declara un servicio de accesibilidad" }
            if (Manifest.permission.READ_SMS in sensitive || Manifest.permission.RECEIVE_SMS in sensitive) { score += 15; reasons += "Solicita acceso a SMS" }
            score = score.coerceIn(0, 100)
            return Arguments.createMap().apply {
                putString("fileName", uri.lastPathSegment ?: file.name)
                putString("packageName", info.packageName ?: "desconocido")
                putString("versionName", info.versionName ?: "desconocida")
                putString("sha256", fileSha256(file))
                putString("signatureSha256", signature)
                putArray("permissions", sensitive.map(::permissionLabel).distinct().toWritableArray())
                putInt("score", score)
                putString("risk", when { score >= 60 -> "high"; score >= 20 -> "review"; else -> "low" })
                putArray("reasons", reasons.toWritableArray())
            }
        } finally {
            file.delete()
        }
    }

    private fun fileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @ReactMethod
    fun getDeviceSecurityAudit(promise: Promise) {
        runCatching {
            val checks = Arguments.createArray()
            val rooted = ROOT_INDICATORS.any { File(it).exists() } || Build.TAGS?.contains("test-keys") == true
            val developer = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
            val adb = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            val keyguard = context.getSystemService(KeyguardManager::class.java).isDeviceSecure
            val encryption = context.getSystemService(DevicePolicyManager::class.java).storageEncryptionStatus
            val encrypted = encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER)
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            val vpnActive = connectivity.activeNetwork?.let { network ->
                connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } == true
            val privateDns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Settings.Global.getString(context.contentResolver, "private_dns_mode").orEmpty() else "no disponible"
            val keyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty().substringBefore('/')
            val userCertificates = runCatching {
                val store = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
                store.aliases().toList().count { it.startsWith("user:", ignoreCase = true) }
            }.getOrDefault(0)

            checks.pushDeviceCheck("root", "Integridad del sistema", if (rooted) "Se encontraron indicadores compatibles con root." else "No se encontraron indicadores comunes de root.", if (rooted) "Evita operaciones bancarias y restaura el software oficial." else "Sin acciones necesarias.", if (rooted) "danger" else "safe")
            checks.pushDeviceCheck("lock", "Bloqueo de pantalla", if (keyguard) "El dispositivo tiene bloqueo seguro." else "No hay PIN, patrón o contraseña segura.", "Configura un bloqueo seguro en Android.", if (keyguard) "safe" else "danger")
            checks.pushDeviceCheck("encryption", "Cifrado del dispositivo", if (encrypted) "El almacenamiento está cifrado." else "No se pudo confirmar el cifrado completo.", "Activa el cifrado desde los ajustes de seguridad si está disponible.", if (encrypted) "safe" else "warning")
            checks.pushDeviceCheck("developer", "Opciones de desarrollador", if (developer) "Están activadas." else "Están desactivadas.", "Desactívalas cuando no las necesites.", if (developer) "warning" else "safe")
            checks.pushDeviceCheck("adb", "Depuración USB", if (adb) "Está activada." else "Está desactivada.", "Desactívala y revoca equipos autorizados.", if (adb) "danger" else "safe")
            checks.pushDeviceCheck("patch", "Parche de seguridad", Build.VERSION.SECURITY_PATCH.ifEmpty { "El fabricante no informó la fecha del parche." }, "Instala las actualizaciones disponibles del fabricante.", if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) "warning" else "info")
            checks.pushDeviceCheck("vpn", "Conexión VPN", if (vpnActive) "Hay una VPN activa." else "No se detecta una VPN activa.", "Confirma que reconoces y confías en la aplicación VPN.", if (vpnActive) "info" else "safe")
            checks.pushDeviceCheck("dns", "DNS privado", if (privateDns.isEmpty() || privateDns == "off") "No está activo." else "Modo configurado: $privateDns", "Considera un proveedor DNS privado confiable.", if (privateDns.isEmpty() || privateDns == "off") "info" else "safe")
            checks.pushDeviceCheck("keyboard", "Teclado predeterminado", keyboard.ifEmpty { "No identificado" }, "Los teclados pueden procesar texto sensible; conserva solo los que reconozcas.", "info")
            checks.pushDeviceCheck("certificates", "Certificados de usuario", if (userCertificates == 0) "No se detectaron certificados de usuario." else "$userCertificates certificado(s) instalado(s).", "Elimina certificados que no reconozcas desde los ajustes de credenciales.", if (userCertificates > 0) "warning" else "safe")
            checks
        }.onSuccess(promise::resolve).onFailure { promise.reject("DEVICE_AUDIT_FAILED", "No se pudo auditar el dispositivo.", it) }
    }

    @ReactMethod
    fun scanInstalledApps(promise: Promise) {
        runCatching { performAppAudit() }
            .onSuccess(promise::resolve)
            .onFailure { promise.reject("APP_AUDIT_FAILED", "No se pudo completar la auditoría local.", it) }
    }

    @Suppress("DEPRECATION")
    private fun performAppAudit() = Arguments.createArray().also { result ->
        val manager = context.packageManager
        val previousPackages = preferences.getStringSet(LAST_SCANNED_PACKAGES)
        val previousSnapshots = preferences.getStringSet(APP_AUDIT_SNAPSHOTS).associateBy { it.substringBefore('|') }
        val trustedApps = preferences.getStringSet(TRUSTED_APPS).toMutableSet()
        val hasPreviousScan = preferences.getLong(LAST_MALWARE_SCAN, 0L) > 0L
        val currentPackages = mutableSetOf<String>()
        val currentSnapshots = mutableSetOf<String>()
        val evaluatedScores = mutableListOf<Int>()
        var highCount = 0
        var reviewCount = 0
        val enabledAccessibility = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().lowercase()
        val enabledNotificationListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty().lowercase()
        val activeAdmins = context.getSystemService(DevicePolicyManager::class.java)
            .activeAdmins.orEmpty().map { it.packageName }.toSet()

        manager.getInstalledApplications(0)
            .sortedBy { manager.getApplicationLabel(it).toString().lowercase() }
            .forEach { app ->
                runCatching {
                    val packageInfo = manager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS or signatureFlag())
                    val requested = packageInfo.requestedPermissions?.toList().orEmpty()
                    val flags = packageInfo.requestedPermissionsFlags ?: IntArray(0)
                    val granted = requested.filterIndexed { index, permission ->
                        ((flags.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0 ||
                            manager.checkPermission(permission, app.packageName) == PackageManager.PERMISSION_GRANTED
                    }
                    val sensitive = requested.filter(SENSITIVE_PERMISSIONS::contains)
                    val grantedSensitive = granted.filter(SENSITIVE_PERMISSIONS::contains)
                    val system = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    val installer = runCatching { installerName(manager, app.packageName) }.getOrDefault("desconocido")
                    val accessibility = enabledAccessibility.contains(app.packageName.lowercase())
                    val notificationAccess = enabledNotificationListeners.contains(app.packageName.lowercase())
                    val deviceAdmin = app.packageName in activeAdmins
                    val signature = signatureSha256(packageInfo)
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
                    val permissionSnapshot = grantedSensitive.sorted().joinToString(",")
                    val snapshot = "${app.packageName}|$versionCode|$signature|$permissionSnapshot"
                    val previous = previousSnapshots[app.packageName]?.split('|').orEmpty()
                    val changes = buildList {
                        if (previous.isNotEmpty()) {
                            if (previous.getOrNull(1) != versionCode.toString()) add("La versión de la aplicación cambió")
                            if (previous.getOrNull(2).orEmpty().isNotEmpty() && previous.getOrNull(2) != signature) add("La firma digital cambió")
                            val oldPermissions = previous.getOrNull(3).orEmpty().split(',').filter(String::isNotEmpty).toSet()
                            (grantedSensitive.toSet() - oldPermissions).forEach { add("Nuevo permiso concedido: ${permissionLabel(it)}") }
                        }
                    }
                    val signatureChanged = changes.any { it == "La firma digital cambió" }
                    val score = calculateRiskScore(
                        system = system,
                        installer = installer,
                        granted = grantedSensitive.toSet(),
                        accessibility = accessibility,
                        notificationAccess = notificationAccess,
                        deviceAdmin = deviceAdmin,
                        signatureChanged = signatureChanged,
                    )
                    val reasons = riskReasons(
                        installer = installer,
                        granted = grantedSensitive.toSet(),
                        accessibility = accessibility,
                        notificationAccess = notificationAccess,
                        deviceAdmin = deviceAdmin,
                    ).toMutableList()
                    if (signatureChanged) reasons.add("La firma digital difiere del análisis anterior")
                    currentPackages += app.packageName
                    currentSnapshots += snapshot
                    if (signatureChanged) trustedApps -= app.packageName
                    val trusted = app.packageName in trustedApps
                    if (!trusted) {
                        evaluatedScores += score
                        if (score >= 60) highCount++ else if (score >= 20) reviewCount++
                    }
                    result.pushMap(Arguments.createMap().apply {
                        putString("packageName", app.packageName)
                        putString("name", manager.getApplicationLabel(app).toString())
                        putBoolean("isSystem", system)
                        putString("installer", installer)
                        putString("installerLabel", INSTALLER_LABELS[installer] ?: if (installer == "desconocido") "Instalación manual o desconocida" else installer)
                        putString("risk", when { score >= 60 -> "high"; score >= 20 -> "review"; else -> "low" })
                        putInt("score", score)
                        putArray("sensitivePermissions", sensitive.map(::permissionLabel).distinct().toWritableArray())
                        putArray("grantedSensitivePermissions", grantedSensitive.map(::permissionLabel).distinct().toWritableArray())
                        putArray("reasons", reasons.toWritableArray())
                        putDouble("firstInstallTime", packageInfo.firstInstallTime.toDouble())
                        putDouble("lastUpdateTime", packageInfo.lastUpdateTime.toDouble())
                        putBoolean("isNew", hasPreviousScan && app.packageName !in previousPackages)
                        putBoolean("enabled", app.enabled)
                        putBoolean("trusted", trusted)
                        putString("versionName", packageInfo.versionName ?: versionCode.toString())
                        putString("signatureSha256", signature)
                        putArray("changes", changes.toWritableArray())
                    })
                }.onFailure {
                    KimoxDiagnostics.record(context, "WARN", "APP_AUDIT_ITEM_FAILED", "package=${app.packageName}; error=${it.javaClass.simpleName}")
                }
            }
        preferences.putStringSet(LAST_SCANNED_PACKAGES, currentPackages)
        preferences.putStringSet(APP_AUDIT_SNAPSHOTS, currentSnapshots)
        preferences.putStringSet(TRUSTED_APPS, trustedApps)
        preferences.putLong(LAST_MALWARE_SCAN, System.currentTimeMillis())
        val relevantScores = evaluatedScores.sortedDescending().take(5)
        val securityScore = if (relevantScores.isEmpty()) 100 else 100 - relevantScores.average().toInt()
        val entry = listOf(System.currentTimeMillis(), securityScore, highCount, reviewCount, currentPackages.size).joinToString("|")
        val history = preferences.getStringSet(MALWARE_SCAN_HISTORY).toMutableSet().apply {
            add(entry)
            if (size > 10) sortedBy { it.substringBefore('|').toLongOrNull() ?: 0L }.take(size - 10).forEach(::remove)
        }
        preferences.putStringSet(MALWARE_SCAN_HISTORY, history)
    }

    private fun com.facebook.react.bridge.WritableArray.pushDeviceCheck(
        id: String,
        title: String,
        detail: String,
        recommendation: String,
        status: String,
    ) = pushMap(Arguments.createMap().apply {
        putString("id", id)
        putString("title", title)
        putString("detail", detail)
        putString("recommendation", recommendation)
        putString("status", status)
    })

    private fun calculateRiskScore(
        system: Boolean,
        installer: String,
        granted: Set<String>,
        accessibility: Boolean,
        notificationAccess: Boolean,
        deviceAdmin: Boolean,
        signatureChanged: Boolean,
    ): Int {
        var score = 0
        if (installer == "desconocido") score += 12
        if (Manifest.permission.REQUEST_INSTALL_PACKAGES in granted) score += 20
        if (Manifest.permission.SYSTEM_ALERT_WINDOW in granted) score += 18
        if (Manifest.permission.READ_SMS in granted || Manifest.permission.RECEIVE_SMS in granted) score += 15
        if (Manifest.permission.RECORD_AUDIO in granted) score += 8
        if (Manifest.permission.CAMERA in granted) score += 6
        if (accessibility) score += 35
        if (notificationAccess) score += 20
        if (deviceAdmin) score += 25
        if (signatureChanged) score += 55
        if (accessibility && Manifest.permission.SYSTEM_ALERT_WINDOW in granted) score += 25
        if (notificationAccess && (Manifest.permission.READ_SMS in granted || Manifest.permission.RECEIVE_SMS in granted)) score += 15
        if (system) score -= 35
        return score.coerceIn(0, 100)
    }

    private fun riskReasons(
        installer: String,
        granted: Set<String>,
        accessibility: Boolean,
        notificationAccess: Boolean,
        deviceAdmin: Boolean,
    ) = buildList {
        if (accessibility) add("Tiene un servicio de accesibilidad activado")
        if (notificationAccess) add("Puede leer las notificaciones del dispositivo")
        if (deviceAdmin) add("Es administrador activo del dispositivo")
        if (Manifest.permission.SYSTEM_ALERT_WINDOW in granted) add("Puede mostrarse sobre otras aplicaciones")
        if (Manifest.permission.REQUEST_INSTALL_PACKAGES in granted) add("Puede solicitar la instalación de APK")
        if (Manifest.permission.READ_SMS in granted || Manifest.permission.RECEIVE_SMS in granted) add("Tiene acceso a mensajes SMS")
        if (Manifest.permission.RECORD_AUDIO in granted) add("Tiene acceso al micrófono")
        if (Manifest.permission.CAMERA in granted) add("Tiene acceso a la cámara")
        if (installer == "desconocido") add("No se pudo verificar la tienda de instalación")
    }

    private fun permissionLabel(permission: String) = PERMISSION_LABELS[permission] ?: permission.substringAfterLast('.')

    @Suppress("DEPRECATION")
    private fun signatureFlag() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun signatureSha256(info: PackageInfo): String {
        val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return ""
            if (signing.hasPastSigningCertificates()) signing.signingCertificateHistory else signing.apkContentsSigners
        } else info.signatures
        val certificate = certificates?.firstOrNull() ?: return ""
        return MessageDigest.getInstance("SHA-256").digest(certificate.toByteArray()).joinToString(":") { "%02X".format(it) }
    }

    private fun openSettings(intent: Intent) {
        context.currentActivity?.startActivity(intent)
    }

    private fun legacyCallPermissionsGranted(): Boolean = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.ANSWER_PHONE_CALLS,
    ).all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

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
        const val BLOCK_INTERNATIONAL = "block_international"
        const val ALLOW_INTERNATIONAL_CONTACTS = "allow_international_contacts"
        const val LOCAL_COUNTRY_CODE = "local_country_code"
        const val POST_CALL_PROMPT_ENABLED = "post_call_prompt_enabled"
        const val SCREENING_ACTION = "screening_action"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val HIDE_NOTIFICATION_NUMBERS = "hide_notification_numbers"
        const val HISTORY_RETENTION_DAYS = "history_retention_days"
        const val PROTECTION_ENABLED = "protection_enabled"
        const val CALL_HISTORY = "call_history"
        const val LAST_SCANNED_PACKAGES = "last_scanned_packages"
        const val LAST_MALWARE_SCAN = "last_malware_scan"
        const val TRUSTED_APPS = "trusted_apps"
        const val MALWARE_SCAN_HISTORY = "malware_scan_history"
        const val APP_AUDIT_SNAPSHOTS = "app_audit_snapshots"
        const val MALWARE_CONSENT = "malware_consent"
        private const val PERMISSION_REQUEST_CODE = 7002
        private const val APK_PICK_REQUEST = 7010
        private const val MAX_APK_SIZE = 500L * 1024L * 1024L
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
        private val PERMISSION_LABELS = mapOf(
            Manifest.permission.READ_SMS to "Leer SMS",
            Manifest.permission.RECEIVE_SMS to "Recibir SMS",
            Manifest.permission.RECORD_AUDIO to "Micrófono",
            Manifest.permission.CAMERA to "Cámara",
            Manifest.permission.ACCESS_FINE_LOCATION to "Ubicación precisa",
            Manifest.permission.READ_CONTACTS to "Contactos",
            Manifest.permission.REQUEST_INSTALL_PACKAGES to "Instalar APK",
            Manifest.permission.SYSTEM_ALERT_WINDOW to "Superposición de pantalla",
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE to "Servicio de accesibilidad",
        )
        private val INSTALLER_LABELS = mapOf(
            "com.android.vending" to "Google Play Store",
            "com.huawei.appmarket" to "Huawei AppGallery",
            "com.amazon.venezia" to "Amazon Appstore",
            "com.sec.android.app.samsungapps" to "Samsung Galaxy Store",
            "com.xiaomi.mipicks" to "Xiaomi GetApps",
            "com.android.packageinstaller" to "Instalador del sistema",
            "com.google.android.packageinstaller" to "Instalador del sistema",
        )
        private val ROOT_INDICATORS = listOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
        )
    }
}
