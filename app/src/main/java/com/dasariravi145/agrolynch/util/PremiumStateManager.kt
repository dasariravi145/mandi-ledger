package com.dasariravi145.agrolynch.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.dasariravi145.agrolynch.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumStateManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "premium_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isPremium = MutableStateFlow(getCachedPremiumStatus())
    val isPremium = _isPremium.asStateFlow()

    fun updatePremiumStatus(isPremium: Boolean, expiryTime: Long = 0L) {
        sharedPreferences.edit()
            .putBoolean("is_premium", isPremium)
            .putLong("expiry_time", expiryTime)
            .putLong("last_verified", System.currentTimeMillis())
            .apply()
        _isPremium.value = getCachedPremiumStatus()
    }

    fun getCachedPremiumStatus(): Boolean {
        if (BuildConfig.DEBUG) {
            if (sharedPreferences.contains("premium_testing_enabled")) {
                return sharedPreferences.getBoolean("premium_testing_enabled", false)
            }
        }
        
        val isPremium = sharedPreferences.getBoolean("is_premium", false)
        val expiryTime = sharedPreferences.getLong("expiry_time", 0L)
        
        if (isPremium && expiryTime > 0 && System.currentTimeMillis() > expiryTime) {
            // Plan expired
            return false
        }
        
        return isPremium
    }

    // --- Developer / Testing Overrides ---

    fun setPremiumTestingOverride(enabled: Boolean?) {
        if (!BuildConfig.DEBUG) return
        
        if (enabled == null) {
            sharedPreferences.edit().remove("premium_testing_enabled").apply()
            timber.log.Timber.d("Premium Testing Override Reset")
        } else {
            sharedPreferences.edit().putBoolean("premium_testing_enabled", enabled).apply()
            timber.log.Timber.d("Premium Testing Override Set: $enabled")
        }
        _isPremium.value = getCachedPremiumStatus()
    }

    fun getPremiumTestingOverride(): Boolean? {
        if (!BuildConfig.DEBUG) return null
        if (!sharedPreferences.contains("premium_testing_enabled")) return null
        return sharedPreferences.getBoolean("premium_testing_enabled", false)
    }

    fun getExpiryTime(): Long {
        return sharedPreferences.getLong("expiry_time", 0L)
    }

    fun getLastVerifiedTime(): Long {
        return sharedPreferences.getLong("last_verified", 0L)
    }

    // --- Premium Popup Logic ---

    fun shouldShowPremiumPopup(userId: String?): Boolean {
        if (getCachedPremiumStatus()) return false
        
        if (userId != null && isPopupDisabledForUser(userId)) return false
        
        val lastSkipped = sharedPreferences.getLong("last_premium_popup_skipped_at", 0L)
        val currentTime = System.currentTimeMillis()
        
        // Show again after 24 hours
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L
        return currentTime - lastSkipped > twentyFourHoursInMillis
    }

    fun isPopupDisabledForUser(userId: String): Boolean {
        return sharedPreferences.getBoolean("premium_popup_disabled_user_$userId", false)
    }

    fun setPopupDisabledForUser(userId: String, disabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("premium_popup_disabled_user_$userId", disabled)
            .apply()
    }

    fun markPopupSkipped() {
        sharedPreferences.edit()
            .putLong("last_premium_popup_skipped_at", System.currentTimeMillis())
            .apply()
    }

    fun setHasSeenPopupAfterRegistration(seen: Boolean) {
        sharedPreferences.edit()
            .putBoolean("has_seen_premium_popup_after_registration", seen)
            .apply()
    }

    fun hasSeenPopupAfterRegistration(): Boolean {
        return sharedPreferences.getBoolean("has_seen_premium_popup_after_registration", false)
    }
}
