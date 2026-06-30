package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.BillNumberSeriesDao
import com.dasariravi145.agrolynch.data.local.dao.EntryDeductionDao
import com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity
import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import com.dasariravi145.agrolynch.domain.repository.BillNumberRepository
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject

class BillNumberRepositoryImpl @Inject constructor(
    private val seriesDao: BillNumberSeriesDao,
    private val deductionDao: EntryDeductionDao
) : BillNumberRepository {

    override fun getAllSeries(): Flow<List<BillNumberSeriesEntity>> = seriesDao.getAllSeries()

    override suspend fun getSeriesByType(type: String): BillNumberSeriesEntity? = seriesDao.getSeriesByType(type)

    override suspend fun updateSeries(series: BillNumberSeriesEntity) = seriesDao.insertOrUpdate(series)

    override suspend fun generateNextBillNumber(type: String): String {
        var series = seriesDao.getSeriesByType(type)
        if (series == null) {
            // Initialize missing series
            series = BillNumberSeriesEntity(
                seriesType = type,
                prefix = when(type) {
                    "STOCK" -> "GK-Farmer"
                    "SALE" -> "GK-SALE"
                    "PAYMENT" -> "PAY"
                    else -> "GEN"
                },
                currentNumber = 1,
                startingNumber = 1,
                resetYearly = true,
                financialYearEnabled = true,
                updatedAt = System.currentTimeMillis()
            )
            seriesDao.insertSeries(series)
            timber.log.Timber.i("Initialized missing bill series for $type")
        }
        return formatBillNumber(series)
    }

    override suspend fun incrementBillNumber(type: String) {
        val series = seriesDao.getSeriesByType(type) ?: return
        seriesDao.update(series.copy(
            currentNumber = series.currentNumber + 1,
            updatedAt = System.currentTimeMillis()
        ))
    }

    override fun getDeductionsByEntryId(entryId: String): Flow<List<EntryDeductionEntity>> = 
        deductionDao.getDeductionsByEntryId(entryId)

    override suspend fun getDeductionsByEntryIdSync(entryId: String): List<EntryDeductionEntity> =
        deductionDao.getDeductionsByEntryIdSync(entryId)

    override suspend fun saveDeductions(deductions: List<EntryDeductionEntity>) = 
        deductionDao.insertAll(deductions)

    override suspend fun deleteDeductionsByEntryId(entryId: String) = 
        deductionDao.deleteByEntryId(entryId)

    private fun formatBillNumber(series: BillNumberSeriesEntity): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val shortYear = year % 100
        val month = calendar.get(Calendar.MONTH) + 1
        
        var formatted = series.prefix
        
        if (series.financialYearEnabled) {
            val fy = if (month >= 4) "$shortYear-${shortYear + 1}" else "${shortYear - 1}-$shortYear"
            formatted += "-$fy"
        }
        
        formatted += "-${String.format("%06d", series.currentNumber)}"
        return formatted
    }
}
