package com.dasariravi145.agrolynch.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_APP_PIN = "app_pin"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAST_ACTIVITY = "last_activity_time"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LOCATION = "user_location"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_PENDING_PROFILE_SYNC = "pending_profile_sync"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_PIN_CREATED = "pin_created"
        private const val KEY_PROFILE_CREATED = "profile_created"
        private const val SESSION_TIMEOUT = 5 * 60 * 1000 // 5 minutes
    }

    fun saveSession(uid: String, phone: String, name: String, location: String, pin: String) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, uid)
            .putString(KEY_PHONE_NUMBER, phone)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_LOCATION, location)
            .putString(KEY_APP_PIN, pin)
            .putBoolean(KEY_PIN_CREATED, true)
            .putBoolean(KEY_PROFILE_CREATED, true)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()
        timber.log.Timber.d("SESSION_SAVE_SUCCESS")
        timber.log.Timber.d("PROFILE_CREATED_LOCAL_TRUE")
    }

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    fun isProfileCreated(): Boolean = sharedPreferences.getBoolean(KEY_PROFILE_CREATED, false)
    fun isPinCreated(): Boolean = sharedPreferences.getBoolean(KEY_PIN_CREATED, false)

    fun setPendingProfileSync(pending: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PENDING_PROFILE_SYNC, pending).apply()
    }

    fun hasPendingProfileSync(): Boolean = sharedPreferences.getBoolean(KEY_PENDING_PROFILE_SYNC, false)

    fun saveRegistrationInfo(name: String, location: String, pin: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_LOCATION, location)
            .putString(KEY_APP_PIN, pin)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()
    }

    fun isRegistered(): Boolean = sharedPreferences.getBoolean(KEY_IS_REGISTERED, false)

    fun getUserName(): String = sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
    fun getUserLocation(): String = sharedPreferences.getString(KEY_USER_LOCATION, "") ?: ""

    fun savePin(pin: String) {
        val hashedPin = hashPin(pin)
        sharedPreferences.edit().putString(KEY_APP_PIN, hashedPin).apply()
    }

    private fun hashPin(pin: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin // Fallback to plaintext if hashing fails (should not happen)
        }
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedPin = getPin() ?: return false
        val hashedInput = hashPin(inputPin)
        return storedPin == hashedInput || storedPin == inputPin // Support legacy plaintext
    }

    fun getPin(): String? {
        return sharedPreferences.getString(KEY_APP_PIN, null)
    }

    fun isPinSet(): Boolean = getPin() != null

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun updateLastActivityTime() {
        sharedPreferences.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    fun isSessionExpired(): Boolean {
        val lastActivity = sharedPreferences.getLong(KEY_LAST_ACTIVITY, 0L)
        if (lastActivity == 0L) return false
        return (System.currentTimeMillis() - lastActivity) > SESSION_TIMEOUT
    }

    fun clearSession() {
        sharedPreferences.edit().remove(KEY_LAST_ACTIVITY).apply()
    }
}
