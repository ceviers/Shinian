package com.cevier.shinian.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AccessManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hasLocalUsage(hasNotes: Boolean): Boolean =
        preferences.getBoolean(KEY_INITIALIZED, false) ||
            hasNotes

    val hasPassphrase: Boolean
        get() = preferences.contains(KEY_PASSPHRASE_SALT) && preferences.contains(KEY_PASSPHRASE_HASH)

    val biometricEnabled: Boolean
        get() = preferences.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    val hideRecentPreview: Boolean
        get() = preferences.getBoolean(KEY_HIDE_RECENT_PREVIEW, true)

    fun completeSetup(passphrase: String, biometricEnabled: Boolean) {
        val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
        val hash = derive(passphrase, salt)
        preferences.edit {
            putString(KEY_PASSPHRASE_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(KEY_PASSPHRASE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
            putBoolean(KEY_INITIALIZED, true)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun setHideRecentPreview(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_HIDE_RECENT_PREVIEW, enabled) }
    }

    fun verifyPassphrase(passphrase: String): Boolean {
        val saltText = preferences.getString(KEY_PASSPHRASE_SALT, null) ?: return false
        val hashText = preferences.getString(KEY_PASSPHRASE_HASH, null) ?: return false
        return runCatching {
            val salt = Base64.decode(saltText, Base64.NO_WRAP)
            val expected = Base64.decode(hashText, Base64.NO_WRAP)
            MessageDigest.isEqual(expected, derive(passphrase, salt))
        }.getOrDefault(false)
    }

    private fun derive(passphrase: String, salt: ByteArray): ByteArray {
        val specification = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
        }
    }

    private companion object {
        const val PREFERENCES = "shinian"
        const val KEY_INITIALIZED = "initialized"
        const val KEY_PASSPHRASE_SALT = "passphrase_salt"
        const val KEY_PASSPHRASE_HASH = "passphrase_hash"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_HIDE_RECENT_PREVIEW = "hide_recent_preview"
        const val SALT_SIZE = 16
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
    }
}
