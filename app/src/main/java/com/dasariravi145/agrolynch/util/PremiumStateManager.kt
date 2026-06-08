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
    // For development testing only: unlock all premium features in debug builds
    // Set to false temporarily to verify AdMob integration in debug builds
    private val isPremiumTestingEnabled = false // was BuildConfig.DEBUG

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
        _isPremium.value = if (isPremiumTestingEnabled) true else isPremium
    }

    fun getCachedPremiumStatus(): Boolean {
        if (isPremiumTestingEnabled) return true
        return sharedPreferences.getBoolean("is_premium", false)
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
