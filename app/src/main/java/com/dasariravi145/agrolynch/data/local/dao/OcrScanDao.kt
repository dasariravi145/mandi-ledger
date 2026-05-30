package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.OcrScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: OcrScanEntity)

    @Query("SELECT * FROM ocr_scans ORDER BY createdAt DESC")
    fun getAllScans(): Flow<List<OcrScanEntity>>

    @Query("SELECT * FROM ocr_scans WHERE scanId = :id")
    suspend fun getScanById(id: String): OcrScanEntity?

    @Update
    suspend fun updateScan(scan: OcrScanEntity)

    @Query("DELETE FROM ocr_scans WHERE scanId = :id")
    suspend fun deleteScan(id: String)
}
