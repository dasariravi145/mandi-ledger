package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDeductionDao {
    @Query("SELECT * FROM entry_deductions WHERE entryId = :entryId")
    fun getDeductionsByEntryId(entryId: String): Flow<List<EntryDeductionEntity>>

    @Query("SELECT * FROM entry_deductions WHERE entryId = :entryId")
    suspend fun getDeductionsByEntryIdSync(entryId: String): List<EntryDeductionEntity>

    @Query("SELECT * FROM entry_deductions")
    suspend fun getAllDeductionsList(): List<EntryDeductionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeduction(deduction: EntryDeductionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deductions: List<EntryDeductionEntity>)

    @Query("DELETE FROM entry_deductions WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("SELECT * FROM entry_deductions WHERE entryType = :type")
    fun getDeductionsByType(type: String): Flow<List<EntryDeductionEntity>>
}
