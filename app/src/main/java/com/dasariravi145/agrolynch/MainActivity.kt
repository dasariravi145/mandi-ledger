package com.dasariravi145.agrolynch

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.dasariravi145.agrolynch.util.LanguageManager
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dasariravi145.agrolynch.data.worker.SyncWorker
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.ui.components.NotificationPermissionHandler
import com.dasariravi145.agrolynch.ui.navigation.SetupNavGraph
import com.dasariravi145.agrolynch.ui.screens.notification.NotificationViewModel
import com.dasariravi145.agrolynch.ui.screens.security.SecurityViewModel
import com.dasariravi145.agrolynch.ui.theme.MandiLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val securityViewModel: SecurityViewModel by viewModels()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LanguageManager.getLanguageCodeSync(newBase)
        super.attachBaseContext(LanguageManager.applyLocale(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            timber.log.Timber.e(throwable, "CRITICAL: Uncaught exception in thread ${thread.name}")
        }

        enableEdgeToEdge()
        
        timber.log.Timber.d("MainActivity: onCreate started")
        
        notificationViewModel.updateFCMToken()
        notificationViewModel.subscribeToMarketUpdates()
        
        scheduleSync()
        scheduleAutoBackup()

        setContent {
            val context = LocalContext.current
            val languageCode by settingsRepository.languageCode.collectAsState(initial = "en")
            val isDarkMode by settingsRepository.isDarkMode.collectAsState(initial = false)
            
            // Re-apply locale if it changes during runtime
            val localizedContext = LanguageManager.applyLocale(context, languageCode)

            MandiLedgerTheme(darkTheme = isDarkMode) {
                NotificationPermissionHandler()
                val navController = rememberNavController()
                
                DisposableEffect(Unit) {
                    onDispose {
                        securityViewModel.logout() // Clear session on close
                    }
                }

                SetupNavGraph(navController = navController)
            }
        }
        timber.log.Timber.d("MainActivity: onCreate finished in ${System.currentTimeMillis() - startTime}ms")
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_work",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun scheduleAutoBackup() {
        val backupRequest = PeriodicWorkRequestBuilder<com.dasariravi145.agrolynch.data.worker.BackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_backup",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    override fun onResume() {
        super.onResume()
        securityViewModel.checkSession()
    }
}
