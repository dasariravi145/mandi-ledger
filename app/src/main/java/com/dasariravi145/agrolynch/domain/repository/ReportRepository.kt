package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getStockReport(): Flow<List<StockReportModel>>
    fun getBuyerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedSaleReportModel>>
    fun getFarmerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedArrivalReportModel>>
    fun getProductPerformanceReport(): Flow<List<ProductPerformanceModel>>
    fun getOutstandingAgingReport(): Flow<List<OutstandingAgingModel>>
    fun getPaymentReport(startDate: Long, endDate: Long): Flow<List<PaymentReportModel>>
    fun getCommissionReport(startDate: Long, endDate: Long): Flow<List<CommissionReportModel>>
    fun getSalesTrend(sinceDate: Long): Flow<List<ChartDataModel>>
    
    fun getTotalSales(start: Long, end: Long): Flow<Double?>
    fun getTotalPurchases(start: Long, end: Long): Flow<Double?>
    fun getTotalCommission(start: Long, end: Long): Flow<Double?>
    fun getBuyerPendingTotal(): Flow<Double?>
    fun getFarmerPendingTotal(): Flow<Double?>
    suspend fun recalculateCommissions()
}
