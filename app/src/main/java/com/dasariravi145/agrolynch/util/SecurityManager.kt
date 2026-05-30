package com.dasariravi145.agrolynch.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext context: Context
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
        private const val SESSION_TIMEOUT = 5 * 60 * 1000 // 5 minutes
    }

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
        sharedPreferences.edit().putString(KEY_APP_PIN, pin).apply()
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
