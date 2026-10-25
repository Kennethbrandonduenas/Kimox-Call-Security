package com.kimoxcallsecurity

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(KimoxSecurityModule.PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getStringSet(key: String, fallback: Set<String> = emptySet()): Set<String> {
        secureValue(key)?.let { value ->
            return runCatching {
                val array = JSONArray(value)
                buildSet { for (index in 0 until array.length()) add(array.getString(index)) }
            }.getOrDefault(fallback)
        }
        val legacy = prefs.getStringSet(key, null)?.toSet() ?: return fallback
        putStringSet(key, legacy)
        prefs.edit().remove(key).apply()
        return legacy
    }

    fun putStringSet(key: String, value: Set<String>) = putSecure(key, JSONArray(value.toList()).toString())
    fun getBoolean(key: String, fallback: Boolean) = secureValue(key)?.toBooleanStrictOrNull() ?: migrateBoolean(key, fallback)
    fun putBoolean(key: String, value: Boolean) = putSecure(key, value.toString())
    fun getInt(key: String, fallback: Int) = secureValue(key)?.toIntOrNull() ?: migrateInt(key, fallback)
    fun putInt(key: String, value: Int) = putSecure(key, value.toString())
    fun getLong(key: String, fallback: Long) = secureValue(key)?.toLongOrNull() ?: fallback
    fun putLong(key: String, value: Long) = putSecure(key, value.toString())
    fun getString(key: String, fallback: String): String = secureValue(key) ?: migrateString(key, fallback)
    fun putString(key: String, value: String) = putSecure(key, value)
    fun remove(key: String) { prefs.edit().remove(secureKey(key)).remove(key).apply() }
    fun clear() { prefs.edit().clear().apply() }

    private fun migrateBoolean(key: String, fallback: Boolean): Boolean {
        if (!prefs.contains(key)) return fallback
        val value = prefs.getBoolean(key, fallback)
        putBoolean(key, value); prefs.edit().remove(key).apply(); return value
    }

    private fun migrateInt(key: String, fallback: Int): Int {
        if (!prefs.contains(key)) return fallback
        val value = prefs.getInt(key, fallback)
        putInt(key, value); prefs.edit().remove(key).apply(); return value
    }

    private fun migrateString(key: String, fallback: String): String {
        val value = prefs.getString(key, null) ?: return fallback
        putString(key, value); prefs.edit().remove(key).apply(); return value
    }

    private fun secureValue(key: String): String? = prefs.getString(secureKey(key), null)?.let {
        runCatching { decrypt(it) }.getOrNull()
    }
    private fun putSecure(key: String, value: String) { prefs.edit().putString(secureKey(key), encrypt(value)).apply() }
    private fun secureKey(key: String) = "secure_$key"

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload.copyOfRange(0, IV_SIZE)))
        return String(cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size)), StandardCharsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "kimox_local_data_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12

        fun resetEncryptionKey() {
            runCatching {
                KeyStore.getInstance(KEYSTORE).apply {
                    load(null)
                    if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
                }
            }
        }
    }
}
