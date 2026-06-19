package com.dasariravi145.agrolynch.data.repository

import android.content.Context
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import com.dasariravi145.agrolynch.domain.repository.BackupRepository
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import com.dasariravi145.agrolynch.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupDao: BackupDao,
    private val backupManager: BackupManager,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val premiumStateManager: PremiumStateManager
) : BackupRepository {

    override fun getBackupHistory(): Flow<List<BackupEntity>> = backupDao.getBackupHistory()

    override suspend fun createLocalBackup(reportType: String): Resource<File> {
        val result = backupManager.createLocalBackup()
        if (result is Resource.Success) {
            val file = result.data!!
            val user = userRepository.getUserProfile().first()
            val phoneNumber = auth.currentUser?.phoneNumber ?: user?.phoneNumber ?: ""
            val userName = user?.name ?: "UnknownUser"

            val backup = BackupEntity(
                id = UUID.randomUUID().toString(),
                fileName = file.name,
                filePath = file.absolutePath,
                size = file.length(),
                type = "LOCAL",
                reportType = reportType,
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                phoneNumber = phoneNumber,
                userName = userName
            )
            backupDao.insertBackup(backup)
        }
        return result
    }

    override suspend fun uploadBackupToCloud(file: File, reportType: String, localBackupId: String?): Resource<Unit> {
        val uploadResult = backupManager.uploadBackupToFirebase(file)
        if (uploadResult is Resource.Success) {
            val resultData = uploadResult.data!!
            val downloadUrl = resultData.downloadUrl
            val storagePath = resultData.storagePath
            val user = userRepository.getUserProfile().first()
            val phoneNumber = auth.currentUser?.phoneNumber ?: user?.phoneNumber ?: ""
            val userName = user?.name ?: "UnknownUser"

            if (localBackupId != null) {
                val existing = backupDao.getBackupByIdSync(localBackupId)
                if (existing != null) {
                    backupDao.insertBackup(existing.copy(
                        filePath = downloadUrl,
                        storagePath = storagePath,
                        type = "CLOUD",
                        status = "SUCCESS"
                    ))
                    return Resource.Success(Unit)
                }
            }

            val backup = BackupEntity(
                id = UUID.randomUUID().toString(),
                fileName = file.name,
                filePath = downloadUrl,
                storagePath = storagePath,
                size = file.length(),
                type = "CLOUD",
                reportType = reportType,
                status = "SUCCESS",
                timestamp = System.currentTimeMillis(),
                phoneNumber = phoneNumber,
                userName = userName
            )
            backupDao.insertBackup(backup)
            return Resource.Success(Unit)
        }
        return Resource.Error(uploadResult.message ?: "Cloud upload failed")
    }

    override suspend fun restoreFromCloud(backupId: String): Resource<Unit> {
        android.util.Log.d("BACKUP", "RESTORE_CLICKED: $backupId")
        if (!premiumStateManager.getCachedPremiumStatus()) {
            android.util.Log.e("BACKUP", "RESTORE_BLOCKED_FREE_USER")
            return Resource.Error("Premium subscription required to restore cloud backup")
        }
        android.util.Log.d("BACKUP", "RESTORE_ALLOWED_PREMIUM_USER")

        Timber.d("RESTORE_START: $backupId")
        val backup = backupDao.getBackupByIdSync(backupId) ?: return Resource.Error("Backup record not found")
        if (backup.type != "CLOUD") return Resource.Error("Not a cloud backup")

        Timber.d("STORAGE_PATH: ${backup.storagePath}")
        val downloadResult = backupManager.downloadBackupFromFirebase(backup.storagePath)
        if (downloadResult is Resource.Success) {
            Timber.d("DOWNLOAD_SUCCESS")
            val file = downloadResult.data!!
            return backupManager.restoreLocalBackup(file)
        }
        Timber.e("DOWNLOAD_FAILED: ${downloadResult.message}")
        return Resource.Error(downloadResult.message ?: "Failed to download backup")
    }

    override suspend fun deleteBackup(backupId: String): Resource<Unit> {
        val backup = backupDao.getBackupByIdSync(backupId)
        if (backup != null && backup.type == "LOCAL") {
            val file = File(backup.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        backupDao.deleteBackup(backupId)
        return Resource.Success(Unit)
    }

    override suspend fun listCloudBackups(): Resource<List<String>> {
        return backupManager.listCloudBackupsForCurrentUser()
    }

    override suspend fun restoreFromStoragePath(storagePath: String): Resource<Unit> {
        android.util.Log.d("BACKUP", "RESTORE_CLICKED: $storagePath")
        if (!premiumStateManager.getCachedPremiumStatus()) {
            android.util.Log.e("BACKUP", "RESTORE_BLOCKED_FREE_USER")
            return Resource.Error("Premium subscription required to restore cloud backup")
        }
        android.util.Log.d("BACKUP", "RESTORE_ALLOWED_PREMIUM_USER")
        return backupManager.restoreSelectedCloudBackup(storagePath)
    }

    override suspend fun restoreLatestCloudBackup(): Resource<Unit> {
        android.util.Log.d("BACKUP", "RESTORE_CLICKED: LATEST")
        if (!premiumStateManager.getCachedPremiumStatus()) {
            android.util.Log.e("BACKUP", "RESTORE_BLOCKED_FREE_USER")
            return Resource.Error("Premium subscription required to restore cloud backup")
        }
        android.util.Log.d("BACKUP", "RESTORE_ALLOWED_PREMIUM_USER")
        return backupManager.restoreLatestCloudBackup()
    }
}
