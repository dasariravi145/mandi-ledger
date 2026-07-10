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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import javax.inject.Inject

@HiltAndroidApp
class AgroLynchApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()
        
        Timber.plant(Timber.DebugTree())
        Timber.tag("FirebaseInit").d("App onCreate - Initializing Firebase")
        
        try {
            FirebaseApp.initializeApp(this)
            Timber.tag("FirebaseInit").d("FirebaseApp initialized successfully")
        } catch (e: Exception) {
            Timber.tag("FirebaseInit").e(e, "FirebaseApp initialization failed")
        }
        
        // Configure Firestore with production settings
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            
            // Ensure network is enabled (fix for "client is offline" issues)
            firestore.enableNetwork().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.tag("Firestore").d("Firestore network enabled")
                } else {
                    Timber.tag("Firestore").e(task.exception, "Failed to enable Firestore network")
                }
            }
            Timber.tag("Firestore").d("Firestore configured with persistence")
        } catch (e: Exception) {
            Timber.tag("Firestore").e(e, "Firestore configuration failed")
        }

        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Timber.tag("FirebaseInit").d("Firebase App Check installed")
        } catch (e: Exception) {
            Timber.tag("FirebaseInit").e(e, "Firebase App Check installation failed")
        }

        adMobManager.initialize()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
