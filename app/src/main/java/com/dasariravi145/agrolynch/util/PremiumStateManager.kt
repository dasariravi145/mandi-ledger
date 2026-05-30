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
    private val isPremiumTestingEnabled = BuildConfig.DEBUG

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
}
