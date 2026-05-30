package com.dasariravi145.agrolynch.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumStateManager: PremiumStateManager
) {
    fun initialize() {
        // Always initialize MobileAds SDK once at app start
        MobileAds.initialize(context) { status ->
            // SDK Initialized
        }
    }

    fun shouldShowAds(): Boolean {
        return !premiumStateManager.getCachedPremiumStatus()
    }
}
