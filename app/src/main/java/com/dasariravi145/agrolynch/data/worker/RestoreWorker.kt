package com.dasariravi145.agrolynch.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class RestoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val userRepository: UserRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("RestoreWorker: Starting cloud restore...")
        
        // 1. Restore User Profile first
        return when (val profileResult = userRepository.fetchProfileFromCloud()) {
            is Resource.Success -> {
                val user = profileResult.data
                if (user != null && user.isPremium) {
                    // 2. Restore all other data
                    when (val restoreResult = syncRepository.restoreAllData()) {
                        is Resource.Success -> {
                            Timber.i("RestoreWorker: Cloud restore completed successfully")
                            Result.success()
                        }
                        else -> {
                            Timber.e("RestoreWorker: Data restore failed: ${restoreResult.message}")
                            Result.failure()
                        }
                    }
                } else {
                    Timber.w("RestoreWorker: User is not premium or profile not found. No data to restore.")
                    Result.success()
                }
            }
            else -> {
                Timber.e("RestoreWorker: Profile fetch failed: ${profileResult.message}")
                Result.failure()
            }
        }
    }
}
