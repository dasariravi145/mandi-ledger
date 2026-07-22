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

    @Query("UPDATE backup_history SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteBackup(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE backup_history SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM backup_history WHERE id = :id")
    suspend fun deleteBackup(id: String)

    @Query("""
        SELECT MAX(latest) FROM (
            SELECT MAX(date) as latest FROM arrivals
            UNION ALL
            SELECT MAX(date) as latest FROM sales
            UNION ALL
            SELECT MAX(date) as latest FROM payments
        )
    """)
    suspend fun getLastDataModificationTime(): Long?
}
