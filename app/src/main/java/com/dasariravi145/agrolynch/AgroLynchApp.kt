package com.dasariravi145.agrolynch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dasariravi145.agrolynch.ads.AdMobManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.dasariravi145.agrolynch.BuildConfig
import javax.inject.Inject

@HiltAndroidApp
class AgroLynchApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("AgroLynchApp: onCreate - App started")
        adMobManager.initialize()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
