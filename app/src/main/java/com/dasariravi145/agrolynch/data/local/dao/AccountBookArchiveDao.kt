package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.AccountBookArchiveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountBookArchiveDao {
    @Query("SELECT * FROM account_book_archives ORDER BY archivedAt DESC")
    fun getAllArchives(): Flow<List<AccountBookArchiveEntity>>

    @Query("SELECT * FROM account_book_archives WHERE archiveId = :id")
    suspend fun getArchiveById(id: String): AccountBookArchiveEntity?

    @Query("SELECT * FROM account_book_archives")
    suspend fun getAllArchivesList(): List<AccountBookArchiveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: AccountBookArchiveEntity)

    @Update
    suspend fun updateArchive(archive: AccountBookArchiveEntity)

    @Query("DELETE FROM account_book_archives WHERE archiveId = :id")
    suspend fun deleteArchiveById(id: String)
}
