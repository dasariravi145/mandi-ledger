package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BackupRepository {
    fun getBackupHistory(): Flow<List<BackupEntity>>
    suspend fun createLocalBackup(reportType: String): Resource<File>
    suspend fun uploadBackupToCloud(file: File): Resource<Unit>
    suspend fun restoreFromCloud(backupId: String): Resource<Unit>
    suspend fun deleteBackup(id: String): Resource<Unit>
}
