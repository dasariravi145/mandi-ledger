package com.dasariravi145.agrolynch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dasariravi145.agrolynch.ads.AdMobManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import javax.inject.Inject

@HiltAndroidApp
class AgroLynchApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()
        
        // Plant Timber for Release too, but with a limited tree if needed. 
        // For now, planting DebugTree to see logs in release via logcat for debugging.
        Timber.plant(Timber.DebugTree())
        
        Timber.tag("AgroLynch").d("App onCreate - Initializing services")
        
        FirebaseApp.initializeApp(this)
        Timber.tag("AgroLynch").d("Firebase initialized")
        
        // Configure Firestore once at app startup
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            Timber.d("FIRESTORE_CONFIGURED")
        } catch (e: Exception) {
            Timber.e(e, "FIRESTORE_CONFIG_FAILED")
        }

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        adMobManager.initialize()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
