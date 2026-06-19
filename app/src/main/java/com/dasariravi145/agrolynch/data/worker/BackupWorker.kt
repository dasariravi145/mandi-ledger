package com.dasariravi145.agrolynch.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dasariravi145.agrolynch.domain.repository.BackupRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val userRepository: com.dasariravi145.agrolynch.domain.repository.UserRepository,
    private val premiumStateManager: com.dasariravi145.agrolynch.util.PremiumStateManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportType = inputData.getString("report_type") ?: "WEEKLY"
        val isPremium = premiumStateManager.getCachedPremiumStatus()
        val userProfile = userRepository.getUserProfile().firstOrNull()
        val isCloudEnabled = userProfile?.cloudBackupEnabled == true

        return when (val result = backupRepository.createLocalBackup(reportType)) {
            is Resource.Success -> {
                if (isPremium && isCloudEnabled) {
                    val pdfFile = result.data
                    if (pdfFile != null && pdfFile.exists()) {
                        backupRepository.uploadBackupToCloud(pdfFile, reportType)
                    }
                }
                Result.success()
            }
            is Resource.Error -> {
                Result.retry()
            }
            else -> Result.failure()
        }
    }
}
