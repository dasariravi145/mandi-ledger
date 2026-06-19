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
            dashboardDao.getTodaySalesMarginFlow(todayStart, todayEnd).distinctUntilChanged(),
            dashboardDao.getTodayArrivalsCommissionFlow(todayStart, todayEnd).distinctUntilChanged(),
            dashboardDao.getTotalSalesMarginFlow().distinctUntilChanged(),
            dashboardDao.getTotalArrivalsCommissionFlow().distinctUntilChanged(),
            dashboardDao.getTotalSalesNetFlow().distinctUntilChanged(),
            dashboardDao.getTotalBuyerPaymentsFlow().distinctUntilChanged(),
            dashboardDao.getTotalArrivalsNetFlow().distinctUntilChanged(),
            dashboardDao.getTotalFarmerPaymentsFlow().distinctUntilChanged(),
            dashboardDao.getTotalTransactionsAmountFlow().distinctUntilChanged(),
            transactionDao.getRecentTransactions(10).distinctUntilChanged(),
            dashboardDao.getTodaySalesListFlow(todayStart, todayEnd).distinctUntilChanged(),
            dashboardDao.getTodaySaleItemsListFlow(todayStart, todayEnd).distinctUntilChanged(),
            dashboardDao.getTodayPaymentsListFlow(todayStart, todayEnd).distinctUntilChanged()
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

            @Suppress("UNCHECKED_CAST")
            val todaySalesList = (flows[11] as? List<com.dasariravi145.agrolynch.data.local.entity.SaleEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val todayItemsList = (flows[12] as? List<com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val todayPaymentsList = (flows[13] as? List<com.dasariravi145.agrolynch.data.local.entity.PaymentEntity>) ?: emptyList()

            timber.log.Timber.d("Dashboard Today Sales Query Started")
            timber.log.Timber.d("Today Sale Master Count: ${todaySalesList.size}")
            timber.log.Timber.d("Today Sale Item Count: ${todayItemsList.size}")
            timber.log.Timber.d("Included Sale IDs: ${todaySalesList.joinToString { it.id }}")
            timber.log.Timber.d("Included Sale Amounts: ${todaySalesList.joinToString { it.totalAmount.toString() }}")
            
            val collectionPayments = todayPaymentsList.filter { it.partyType == "BUYER" }
            val farmerPayments = todayPaymentsList.filter { it.partyType == "FARMER" }
            
            timber.log.Timber.d("Excluded Payment Amounts (Farmer): ${farmerPayments.sumOf { it.amount }}")
            timber.log.Timber.d("Excluded Collection Amounts (Buyer): ${collectionPayments.sumOf { it.amount }}")
            timber.log.Timber.d("Calculated Today Sales: $todaySales")

            val buyerPending = totalSalesNet - totalBuyerPayments
            val farmerPending = (totalArrivalsNet + totalLegacyTrans) - totalFarmerPayments
            
            // FIXED: Today Commission must only include stock entry commission (arrivals)
            val todayCommission = todayArrivalsComm
            
            // FIXED: Total Commission must only include stock entry commission (arrivals)
            // DO NOT include totalSalesMargin here
            val totalCommission = totalArrivalsComm

            timber.log.Timber.d("Calculated Today Commission: $todayCommission (Stock Entry Only)")

            val summary = DashboardSummary(
                todaySales = todaySales,
                todayCommission = todayCommission,
                commissionEarned = totalCommission,
                buyerPending = buyerPending,
                farmerPending = farmerPending,
                netBalance = buyerPending - farmerPending,
                recentTransactions = recent
            )
            _dashboardCache.value = summary
            summary
        }.onEach { timber.log.Timber.d("Dashboard Today Commission Updated: ${it.todayCommission}") }
        .flowOn(Dispatchers.Default)
    }

    override suspend fun refreshSummary() {
        // No-op: Dashboard is now fully reactive and doesn't rely on the summary table
    }
}
