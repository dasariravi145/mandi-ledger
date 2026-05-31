package com.dasariravi145.agrolynch.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: Starting data sync...")
        return when (val result = syncRepository.syncAllData()) {
            is Resource.Success -> {
                Timber.i("SyncWorker: Data sync completed successfully")
                Result.success()
            }
            is Resource.Error -> {
                Timber.e("SyncWorker: Data sync failed: ${result.message}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
            else -> Result.failure()
        }
    }
}
