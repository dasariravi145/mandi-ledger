package com.dasariravi145.agrolynch.domain.repository

import android.graphics.Bitmap
import com.dasariravi145.agrolynch.data.local.entity.OcrScanEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface OcrRepository {
    fun getScanHistory(): Flow<List<OcrScanEntity>>
    suspend fun saveScan(scan: OcrScanEntity): Resource<Unit>
    suspend fun saveScanWithImage(scan: OcrScanEntity, bitmap: Bitmap?): Resource<Unit>
    suspend fun getScanById(id: String): OcrScanEntity?
    suspend fun syncScans(): Resource<Unit>
}
