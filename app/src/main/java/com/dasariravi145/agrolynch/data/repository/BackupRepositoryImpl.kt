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

    companion object {
        const val SLOT_LOCAL = "SLOT_LOCAL"
        const val SLOT_CLOUD = "SLOT_CLOUD"
        const val SLOT_SAFETY = "SLOT_SAFETY"
    }

    override fun getBackupHistory(): Flow<List<BackupEntity>> = backupDao.getBackupHistory()

    override suspend fun createLocalBackup(reportType: String): Resource<File> {
        val slotId = if (reportType == "PRE_RESTORE_SAFETY") SLOT_SAFETY else SLOT_LOCAL
        val fileName = if (reportType == "PRE_RESTORE_SAFETY") "latest_safety_backup.json" else "latest_local_backup.json"
        
        val result = backupManager.createLocalBackup(fileName)
        if (result is Resource.Success) {
            val file = result.data!!
            val user = userRepository.getUserProfile().first()
            val phoneNumber = auth.currentUser?.phoneNumber ?: user?.phoneNumber ?: ""
            val userName = user?.name ?: "UnknownUser"

            val backup = BackupEntity(
                id = slotId,
                fileName = fileName,
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
        val user = userRepository.getUserProfile().first()
        val phoneNumber = auth.currentUser?.phoneNumber ?: user?.phoneNumber ?: ""
        val userName = user?.name ?: "UnknownUser"

        // Initial record as UPLOADING
        val initialBackup = BackupEntity(
            id = SLOT_CLOUD,
            fileName = "latest_cloud_backup.json",
            filePath = "",
            storagePath = "",
            size = file.length(),
            type = "CLOUD",
            reportType = reportType,
            status = "UPLOADING",
            timestamp = System.currentTimeMillis(),
            phoneNumber = phoneNumber,
            userName = userName
        )
        backupDao.insertBackup(initialBackup)

        val uploadResult = backupManager.uploadBackupToFirebase(file)
        if (uploadResult is Resource.Success) {
            val resultData = uploadResult.data!!
            val downloadUrl = resultData.downloadUrl
            val storagePath = resultData.storagePath

            val finalBackup = initialBackup.copy(
                filePath = downloadUrl,
                storagePath = storagePath,
                status = "SUCCESS"
            )
            backupDao.insertBackup(finalBackup)

            // Update user profile last backup timestamp
            userRepository.getUserProfile().first()?.let { currentUser ->
                userRepository.saveProfile(currentUser.copy(lastUpdatedAt = System.currentTimeMillis()))
            }

            return Resource.Success(Unit)
        } else {
            backupDao.insertBackup(initialBackup.copy(status = "FAILED"))
            return Resource.Error(uploadResult.message ?: "Cloud upload failed")
        }
    }

    override suspend fun restoreFromCloud(backupId: String): Resource<Unit> {
        val backup = backupDao.getBackupByIdSync(backupId) ?: return Resource.Error("Backup record not found")
        return restoreFromStoragePath(backup.storagePath)
    }

    override suspend fun restoreLocalBackup(file: File): Resource<Unit> {
        return backupManager.restoreLocalBackup(file)
    }

    override suspend fun softDeleteBackup(backupId: String): Resource<Unit> {
        backupDao.softDeleteBackup(backupId)
        return Resource.Success(Unit)
    }

    override suspend fun recoverBackupFromTrash(backupId: String): Resource<Unit> {
        backupDao.restoreFromTrash(backupId)
        return Resource.Success(Unit)
    }

    override suspend fun deleteBackupPermanently(backupId: String): Resource<Unit> {
        val backup = backupDao.getBackupByIdSync(backupId)
        if (backup != null) {
            // Delete local file
            if (backup.type == "LOCAL") {
                val file = File(backup.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Delete from Firebase Storage if it's a cloud backup
            if (backup.type == "CLOUD" && backup.storagePath.isNotBlank()) {
                backupManager.deleteBackupFromFirebase(backup.storagePath)
            }
        }
        backupDao.deleteBackup(backupId)
        return Resource.Success(Unit)
    }

    override suspend fun deleteBackup(backupId: String): Resource<Unit> {
        return softDeleteBackup(backupId)
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

    override suspend fun downloadToTempFile(storagePath: String): Resource<File> {
        return backupManager.downloadBackupFromFirebase(storagePath)
    }

    override suspend fun getLastDataModificationTime(): Long {
        return backupDao.getLastDataModificationTime() ?: 0L
    }
}
