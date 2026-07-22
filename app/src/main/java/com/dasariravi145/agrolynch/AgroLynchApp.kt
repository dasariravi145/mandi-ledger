package com.dasariravi145.agrolynch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dasariravi145.agrolynch.ads.AdMobManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import javax.inject.Inject

@HiltAndroidApp
class AgroLynchApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var gson: Gson

    override fun onCreate() {
        super.onCreate()
        
        Timber.plant(Timber.DebugTree())
        Timber.tag("FirebaseInit").d("App onCreate - Initializing Firebase")

        // Install security provider
        try {
            ProviderInstaller.installIfNeeded(this)
            Timber.tag("GMSInit").d("ProviderInstaller successful")
        } catch (e: Exception) {
            Timber.tag("GMSInit").e(e, "ProviderInstaller failed")
        }

        // Check Google Play Services availability
        checkGooglePlayServices()
        
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
            if (BuildConfig.DEBUG) {
                try {
                    // Use a more robust way to load the debug provider to avoid crashes if it's missing
                    val debugProviderClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstanceMethod = debugProviderClass.getMethod("getInstance")
                    val factory = getInstanceMethod.invoke(null) as AppCheckProviderFactory
                    firebaseAppCheck.installAppCheckProviderFactory(factory)
                    Timber.tag("FirebaseInit").d("Firebase App Check installed with Debug provider")
                } catch (e: Exception) {
                    Timber.tag("FirebaseInit").e(e, "Failed to load DebugAppCheckProviderFactory")
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Timber.tag("FirebaseInit").d("Firebase App Check installed with Play Integrity provider")
            }
        } catch (e: Exception) {
            Timber.tag("FirebaseInit").e(e, "Firebase App Check installation failed")
        }

        adMobManager.initialize()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun checkGooglePlayServices() {
        try {
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)
            if (resultCode != ConnectionResult.SUCCESS) {
                Timber.tag("GMSInit").w("Google Play Services not available: ${availability.getErrorString(resultCode)}")
            } else {
                Timber.tag("GMSInit").d("Google Play Services is available")
            }
        } catch (e: Exception) {
            Timber.tag("GMSInit").e(e, "Failed to check Google Play Services")
        }
    }
}
