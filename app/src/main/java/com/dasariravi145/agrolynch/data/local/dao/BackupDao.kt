package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_history ORDER BY timestamp DESC")
    fun getBackupHistory(): Flow<List<BackupEntity>>

    @Query("SELECT * FROM backup_history WHERE id = :id")
    suspend fun getBackupByIdSync(id: String): BackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity)

    @Query("DELETE FROM backup_history WHERE id = :id")
    suspend fun deleteBackup(id: String)
}
