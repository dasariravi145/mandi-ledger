package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.DashboardSummaryEntity
import com.dasariravi145.agrolynch.domain.model.DashboardSummary
import com.dasariravi145.agrolynch.domain.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val dashboardDao: DashboardDao,
    private val transactionDao: TransactionDao
) : DashboardRepository {

    override fun getDashboardSummary(): Flow<DashboardSummary> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        return combine(
            dashboardDao.getTodaySalesFlow(todayStart),
            dashboardDao.getTodaySalesMarginFlow(todayStart),
            dashboardDao.getTodayArrivalsCommissionFlow(todayStart),
            dashboardDao.getTotalSalesMarginFlow(),
            dashboardDao.getTotalArrivalsCommissionFlow(),
            dashboardDao.getTotalSalesNetFlow(),
            dashboardDao.getTotalBuyerPaymentsFlow(),
            dashboardDao.getTotalArrivalsNetFlow(),
            dashboardDao.getTotalFarmerPaymentsFlow(),
            dashboardDao.getTotalTransactionsAmountFlow(),
            transactionDao.getRecentTransactions(10)
        ) { flows ->
            val todaySales = (flows[0] as? Double) ?: 0.0
            val todaySalesMargin = (flows[1] as? Double) ?: 0.0
            val todayArrivalsComm = (flows[2] as? Double) ?: 0.0
            
            val totalSalesMargin = (flows[3] as? Double) ?: 0.0
            val totalArrivalsComm = (flows[4] as? Double) ?: 0.0
            
            val totalSalesNet = (flows[5] as? Double) ?: 0.0
            val totalBuyerPayments = (flows[6] as? Double) ?: 0.0
            
            val totalArrivalsNet = (flows[7] as? Double) ?: 0.0
            val totalFarmerPayments = (flows[8] as? Double) ?: 0.0
            val totalLegacyTrans = (flows[9] as? Double) ?: 0.0
            
            @Suppress("UNCHECKED_CAST")
            val recent = (flows[10] as? List<com.dasariravi145.agrolynch.data.local.entity.TransactionEntity>) ?: emptyList()

            val buyerPending = totalSalesNet - totalBuyerPayments
            val farmerPending = (totalArrivalsNet + totalLegacyTrans) - totalFarmerPayments
            val todayCommission = todaySalesMargin + todayArrivalsComm
            val totalCommission = totalSalesMargin + totalArrivalsComm

            timber.log.Timber.d("Dashboard Recalculated: TodaySales=%f, TodayComm=%f, TotalComm=%f, BuyerPending=%f, FarmerPending=%f", 
                todaySales, todayCommission, totalCommission, buyerPending, farmerPending)

            DashboardSummary(
                todaySales = todaySales,
                todayCommission = todayCommission,
                commissionEarned = totalCommission,
                buyerPending = buyerPending,
                farmerPending = farmerPending,
                netBalance = buyerPending - farmerPending,
                recentTransactions = recent
            )
        }
    }

    override suspend fun refreshSummary() {
        // No-op: Dashboard is now fully reactive and doesn't rely on the summary table
    }
}
