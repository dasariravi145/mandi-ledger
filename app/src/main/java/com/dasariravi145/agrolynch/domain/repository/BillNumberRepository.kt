package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity
import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import kotlinx.coroutines.flow.Flow

interface BillNumberRepository {
    fun getAllSeries(): Flow<List<BillNumberSeriesEntity>>
    suspend fun getSeriesByType(type: String): BillNumberSeriesEntity?
    suspend fun updateSeries(series: BillNumberSeriesEntity)
    suspend fun generateNextBillNumber(type: String): String
    suspend fun incrementBillNumber(type: String)
    
    fun getDeductionsByEntryId(entryId: String): Flow<List<EntryDeductionEntity>>
    suspend fun getDeductionsByEntryIdSync(entryId: String): List<EntryDeductionEntity>
    suspend fun saveDeductions(deductions: List<EntryDeductionEntity>)
    suspend fun deleteDeductionsByEntryId(entryId: String)
}
