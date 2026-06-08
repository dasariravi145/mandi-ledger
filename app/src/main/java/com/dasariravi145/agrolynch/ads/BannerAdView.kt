package com.dasariravi145.agrolynch.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun BannerAdView(
    adUnitId: String = if (com.dasariravi145.agrolynch.BuildConfig.DEBUG) 
        "ca-app-pub-3940256099942544/6300978111" 
        else "ca-app-pub-8131384105009406/8365023448",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            setAdUnitId(adUnitId)
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Log.e("BannerAdView", "Ad failed to load: ErrorCode=${error.code}, Message=${error.message}")
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("BannerAdView", "Ad loaded successfully")
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    Log.d("BannerAdView", "Ad opened")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d("BannerAdView", "Ad clicked")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    Log.d("BannerAdView", "Ad closed")
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
        update = { 
            Log.d("BannerAdView", "Loading Banner Ad")
            it.loadAd(AdRequest.Builder().build())
        }
    )
}
