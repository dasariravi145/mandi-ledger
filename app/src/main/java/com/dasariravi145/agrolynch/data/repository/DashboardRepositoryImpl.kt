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
        return combine(
            dashboardDao.getSummary(),
            transactionDao.getRecentTransactions(5)
        ) { summaryEntity, recent ->
            val summary = summaryEntity ?: DashboardSummaryEntity(id = 1)
            DashboardSummary(
                todaySales = summary.todaySales,
                todayCommission = summary.todayCommission,
                commissionEarned = summary.totalCommission,
                buyerPending = summary.buyerPending,
                farmerPending = summary.farmerPending,
                netBalance = summary.netBalance,
                recentTransactions = recent
            )
        }
    }

    override suspend fun refreshSummary() = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val todaySales = (dashboardDao.calculateTodaySales(todayStart) ?: 0.0) + 
                         (dashboardDao.calculateTodayArrivals(todayStart) ?: 0.0)
        
        val todayCommission = (dashboardDao.calculateTodaySalesMargin(todayStart) ?: 0.0) + 
                              (dashboardDao.calculateTodayArrivalsCommission(todayStart) ?: 0.0)
        
        val totalCommission = (dashboardDao.calculateTotalSalesMargin() ?: 0.0) + 
                              (dashboardDao.calculateTotalArrivalsCommission() ?: 0.0)
        
        val buyerPending = dashboardDao.calculateBuyerPending() ?: 0.0
        val farmerPending = dashboardDao.calculateFarmerPending() ?: 0.0

        val newSummary = DashboardSummaryEntity(
            id = 1,
            todaySales = todaySales,
            todayCommission = todayCommission,
            totalCommission = totalCommission,
            buyerPending = buyerPending,
            farmerPending = farmerPending,
            netBalance = buyerPending - farmerPending,
            updatedAt = System.currentTimeMillis()
        )
        dashboardDao.updateSummary(newSummary)
    }
}
