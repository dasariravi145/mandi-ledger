package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao
) : ReportRepository {

    override fun getStockReport(): Flow<List<StockReportModel>> = reportDao.getStockReport()

    override fun getBuyerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedSaleReportModel>> =
        reportDao.getBuyerDetailedReport(startDate, endDate)

    override fun getFarmerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedArrivalReportModel>> =
        reportDao.getFarmerDetailedReport(startDate, endDate)

    override fun getProductPerformanceReport(): Flow<List<ProductPerformanceModel>> =
        reportDao.getProductPerformanceReport()

    override fun getOutstandingAgingReport(): Flow<List<OutstandingAgingModel>> =
        reportDao.getOutstandingAgingReport()

    override fun getPaymentReport(startDate: Long, endDate: Long): Flow<List<PaymentReportModel>> =
        reportDao.getPaymentReport(startDate, endDate)

    override fun getCommissionReport(startDate: Long, endDate: Long): Flow<List<CommissionReportModel>> =
        reportDao.getCommissionReport(startDate, endDate)

    override fun getSalesTrend(sinceDate: Long): Flow<List<ChartDataModel>> =
        reportDao.getSalesTrend(sinceDate)

    override fun getTotalSales(start: Long, end: Long): Flow<Double?> = reportDao.getTotalSales(start, end)

    override fun getTotalPurchases(start: Long, end: Long): Flow<Double?> = reportDao.getTotalPurchases(start, end)

    override fun getTotalCommission(start: Long, end: Long): Flow<Double?> = reportDao.getTotalCommission(start, end)

    override fun getBuyerPendingTotal(): Flow<Double?> = reportDao.getBuyerPendingTotal()

    override fun getFarmerPendingTotal(): Flow<Double?> = reportDao.getFarmerPendingTotal()

    override suspend fun recalculateCommissions() = reportDao.recalculateCommissions()
}
