package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.DashboardSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboard_summary WHERE id = 1")
    fun getSummary(): Flow<DashboardSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSummary(summary: DashboardSummaryEntity)

    // Reactive aggregation queries for live dashboard updates
    @Query("SELECT SUM(totalAmount) FROM sales WHERE date >= :todayStart AND isDeleted = 0")
    fun getTodaySalesFlow(todayStart: Long): Flow<Double?>

    @Query("SELECT SUM(commissionAmount) FROM arrivals WHERE date >= :todayStart AND isDeleted = 0")
    fun getTodayArrivalsCommissionFlow(todayStart: Long): Flow<Double?>

    @Query("SELECT SUM(totalMargin) FROM sales WHERE date >= :todayStart AND isDeleted = 0")
    fun getTodaySalesMarginFlow(todayStart: Long): Flow<Double?>

    @Query("SELECT SUM(commissionAmount) FROM arrivals WHERE isDeleted = 0")
    fun getTotalArrivalsCommissionFlow(): Flow<Double?>

    @Query("SELECT SUM(totalMargin) FROM sales WHERE isDeleted = 0")
    fun getTotalSalesMarginFlow(): Flow<Double?>

    // Pending Calculation Components
    @Query("SELECT SUM(totalAmount + transportCharges + otherCharges) FROM sales WHERE isDeleted = 0")
    fun getTotalSalesNetFlow(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE partyType = 'BUYER' AND isDeleted = 0")
    fun getTotalBuyerPaymentsFlow(): Flow<Double?>

    @Query("SELECT SUM(netAmount) FROM arrivals WHERE isDeleted = 0")
    fun getTotalArrivalsNetFlow(): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE isDeleted = 0")
    fun getTotalTransactionsAmountFlow(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE partyType = 'FARMER' AND isDeleted = 0")
    fun getTotalFarmerPaymentsFlow(): Flow<Double?>

    // Raw aggregation queries for background worker to refresh the summary table
    @Query("SELECT SUM(totalAmount) FROM sales WHERE date >= :todayStart AND isDeleted = 0")
    suspend fun calculateTodaySales(todayStart: Long): Double?

    @Query("SELECT SUM(grossAmount) FROM arrivals WHERE date >= :todayStart AND isDeleted = 0")
    suspend fun calculateTodayArrivals(todayStart: Long): Double?

    @Query("SELECT SUM(totalMargin) FROM sales WHERE date >= :todayStart AND isDeleted = 0")
    suspend fun calculateTodaySalesMargin(todayStart: Long): Double?

    @Query("SELECT SUM(commissionAmount) FROM arrivals WHERE date >= :todayStart AND isDeleted = 0")
    suspend fun calculateTodayArrivalsCommission(todayStart: Long): Double?

    @Query("SELECT SUM(totalMargin) FROM sales WHERE isDeleted = 0")
    suspend fun calculateTotalSalesMargin(): Double?

    @Query("SELECT SUM(commissionAmount) FROM arrivals WHERE isDeleted = 0")
    suspend fun calculateTotalArrivalsCommission(): Double?

    @Query("SELECT SUM(pendingAmount) FROM buyers WHERE isDeleted = 0")
    suspend fun calculateBuyerPending(): Double?

    @Query("SELECT SUM(pendingAmount) FROM farmers WHERE isDeleted = 0")
    suspend fun calculateFarmerPending(): Double?
}
