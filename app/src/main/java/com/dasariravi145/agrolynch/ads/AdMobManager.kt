package com.dasariravi145.agrolynch.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumStateManager: PremiumStateManager
) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    companion object {
        private const val TAG = "AdMobManager"
        
        // Use Test IDs for Debug builds, Real IDs for Release
        private val INTERSTITIAL_AD_ID = if (com.dasariravi145.agrolynch.BuildConfig.DEBUG) 
            "ca-app-pub-3940256099942544/1033173712" 
            else "ca-app-pub-8131384105009406/3313028292"
            
        private val REWARDED_AD_ID = if (com.dasariravi145.agrolynch.BuildConfig.DEBUG) 
            "ca-app-pub-3940256099942544/5224354917" 
            else "ca-app-pub-8131384105009406/1865910419"
    }

    fun initialize() {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob Initialized: $status")
            loadInterstitialAd()
            loadRewardedAd()
        }
    }

    fun shouldShowAds(): Boolean {
        val isPremium = premiumStateManager.getCachedPremiumStatus()
        Log.d(TAG, "shouldShowAds: isPremium=$isPremium")
        return !isPremium
    }

    fun loadInterstitialAd() {
        if (!shouldShowAds()) return

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Interstitial Ad Failed: Code=${adError.code}, Message=${adError.message}")
                interstitialAd = null
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial Ad Loaded")
                interstitialAd = ad
            }
        })
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        if (interstitialAd != null && shouldShowAds()) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad Dismissed")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                    interstitialAd = null
                    onAdClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad not ready or user is premium")
            onAdClosed()
        }
    }

    fun loadRewardedAd() {
        if (!shouldShowAds()) return

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Rewarded Ad Failed: Code=${adError.code}, Message=${adError.message}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded Ad Loaded")
                rewardedAd = ad
            }
        })
    }

    fun showRewardedAd(activity: Activity, onUserEarnedReward: (Int) -> Unit) {
        if (rewardedAd != null && shouldShowAds()) {
            rewardedAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad Dismissed")
                    rewardedAd = null
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Rewarded Ad Failed to Show: ${adError.message}")
                    rewardedAd = null
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem.amount)
            }
        } else {
            Log.d(TAG, "Rewarded Ad not ready or user is premium")
        }
    }
}
