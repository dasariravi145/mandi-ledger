package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.DashboardSummaryEntity
import com.dasariravi145.agrolynch.domain.model.DashboardSummary
import com.dasariravi145.agrolynch.domain.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val dashboardDao: DashboardDao,
    private val transactionDao: TransactionDao
) : DashboardRepository {

    private val _dashboardCache = MutableStateFlow<DashboardSummary?>(null)

    override fun getDashboardSummary(): Flow<DashboardSummary> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val todayEnd = calendar.timeInMillis

        timber.log.Timber.d("Dashboard Today Commission Query Started: Range $todayStart to $todayEnd")

        return combine(
            dashboardDao.getTodaySalesFlow(todayStart, todayEnd).distinctUntilChanged(),
            dashboardDao.getTodayArrivalsCommissionFlow(todayStart, todayEnd).distinctUntilChanged(),
            dashboardDao.getTotalArrivalsCommissionFlow().distinctUntilChanged(),
            dashboardDao.getTotalSalesNetFlow().distinctUntilChanged(),
            dashboardDao.getTotalBuyerPaymentsFlow().distinctUntilChanged(),
            dashboardDao.getTotalArrivalsNetFlow().distinctUntilChanged(),
            dashboardDao.getTotalFarmerPaymentsFlow().distinctUntilChanged(),
            dashboardDao.getTotalTransactionsAmountFlow().distinctUntilChanged(),
            transactionDao.getRecentTransactions(10).distinctUntilChanged()
        ) { flows ->
            val todaySales = (flows[0] as? Double) ?: 0.0
            val todayArrivalsComm = (flows[1] as? Double) ?: 0.0
            val totalArrivalsComm = (flows[2] as? Double) ?: 0.0
            
            val totalSalesNet = (flows[3] as? Double) ?: 0.0
            val totalBuyerPayments = (flows[4] as? Double) ?: 0.0
            
            val totalArrivalsNet = (flows[5] as? Double) ?: 0.0
            val totalFarmerPayments = (flows[6] as? Double) ?: 0.0
            val totalLegacyTrans = (flows[7] as? Double) ?: 0.0
            
            @Suppress("UNCHECKED_CAST")
            val recent = (flows[8] as? List<com.dasariravi145.agrolynch.data.local.entity.TransactionEntity>) ?: emptyList()

            val buyerPending = totalSalesNet - totalBuyerPayments
            val farmerPendingRaw = (totalArrivalsNet + totalLegacyTrans) - totalFarmerPayments
            val farmerPending = kotlin.math.abs(farmerPendingRaw)
            
            val summary = DashboardSummary(
                todaySales = todaySales,
                todayCommission = todayArrivalsComm,
                commissionEarned = totalArrivalsComm,
                buyerPending = buyerPending,
                farmerPending = farmerPending,
                netBalance = buyerPending - farmerPendingRaw,
                recentTransactions = recent
            )
            _dashboardCache.value = summary
            summary
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun refreshSummary() {
        // No-op: Dashboard is now fully reactive and doesn't rely on the summary table
    }
}
