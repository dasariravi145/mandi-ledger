package com.dasariravi145.agrolynch.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dasariravi145.agrolynch.domain.repository.BackupRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportType = inputData.getString("report_type") ?: "WEEKLY"
        
        return when (val result = backupRepository.createLocalBackup(reportType)) {
            is Resource.Success -> {
                // If it's a premium user, we could automatically upload to cloud here
                // For now, just return success
                Result.success()
            }
            is Resource.Error -> {
                Result.retry()
            }
            else -> Result.failure()
        }
    }
}
