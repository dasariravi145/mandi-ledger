package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class StockReportModel(
    val productId: String,
    val productName: String,
    val totalQuantity: Double,
    val unit: String
)

data class DetailedSaleReportModel(
    val id: String,
    val buyerName: String,
    val date: Long,
    val productName: String,
    val grade: String,
    val quantity: Double,
    val unit: String = "KG",
    val rate: Double,
    val saleAmount: Double,
    val laborCharges: Double,
    val transportCharges: Double,
    val otherCharges: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val pendingAmount: Double
)

data class DetailedArrivalReportModel(
    val id: String,
    val farmerName: String,
    val date: Long,
    val productName: String,
    val grade: String,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val grossAmount: Double,
    val commissionPercent: Double,
    val commissionAmount: Double,
    val netAmount: Double,
    val pendingAmount: Double
)

data class ProductPerformanceModel(
    val productName: String,
    val category: String = "General",
    val grade: String,
    val totalArrivals: Double,
    val totalSold: Double,
    val currentStock: Double,
    val avgPurchaseRate: Double,
    val avgSaleRate: Double,
    val totalProfit: Double
)

data class OutstandingAgingModel(
    val entityId: String,
    val name: String,
    val type: String, // BUYER or FARMER
    val pendingAmount: Double,
    val lastPaymentDate: Long?,
    val daysPending: Int
)

data class PaymentReportModel(
    val id: String,
    val date: Long,
    val partyName: String,
    val partyType: String,
    val amount: Double,
    val paymentMode: String,
    val remainingBalance: Double,
    val status: String = "SUCCESS"
)

data class ChartDataModel(
    val label: String,
    val value: Double
)

data class CommissionReportModel(
    val id: String,
    val buyerName: String,
    val farmerName: String,
    val productName: String,
    val grade: String,
    val quantity: Double,
    val saleAmount: Double,
    val commissionPercent: Double,
    val commissionAmount: Double,
    val marginAmount: Double,
    val date: Long
)

@Dao
interface ReportDao {
    @Query("""
        SELECT productId, productName, SUM(remainingQuantity) as totalQuantity, unit 
        FROM arrivals 
        WHERE remainingQuantity > 0 AND isDeleted = 0
        GROUP BY productId
    """)
    fun getStockReport(): Flow<List<StockReportModel>>

    @Query("""
        SELECT id, buyerName, date, productName, grade, totalQuantity as quantity, 
               'KG' as unit, (totalAmount / totalQuantity) as rate, totalAmount as saleAmount,
               transportCharges, otherCharges as laborCharges, 0.0 as otherCharges,
               (totalAmount + transportCharges + otherCharges) as totalAmount,
               paidAmount, pendingAmount
        FROM sales 
        WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getBuyerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedSaleReportModel>>

    @Query("""
        SELECT id, farmerName, date, productName, grade, quantity, unit,
               purchaseRate as rate, grossAmount, commissionPercent, commissionAmount,
               netAmount, farmerPendingAmount as pendingAmount
        FROM arrivals
        WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getFarmerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedArrivalReportModel>>

    @Query("""
        SELECT productName, 'General' as category, grade, 
               SUM(quantity) as totalArrivals,
               COALESCE((SELECT SUM(totalQuantity) FROM sales s WHERE s.productName = a.productName AND s.grade = a.grade AND s.isDeleted = 0), 0.0) as totalSold,
               SUM(remainingQuantity) as currentStock,
               AVG(purchaseRate) as avgPurchaseRate,
               COALESCE((SELECT AVG(totalAmount / totalQuantity) FROM sales s WHERE s.productName = a.productName AND s.grade = a.grade AND s.isDeleted = 0), 0.0) as avgSaleRate,
               0.0 as totalProfit
        FROM arrivals a
        WHERE isDeleted = 0
        GROUP BY productName, grade
    """)
    fun getProductPerformanceReport(): Flow<List<ProductPerformanceModel>>

    @Query("""
        SELECT id as entityId, name, 'BUYER' as type, pendingAmount,
               (SELECT MAX(date) FROM payments p WHERE p.partyId = b.id AND p.isDeleted = 0) as lastPaymentDate,
               COALESCE(CAST((JulianDay('now') - JulianDay(datetime((SELECT MAX(date) FROM payments p WHERE p.partyId = b.id AND p.isDeleted = 0)/1000, 'unixepoch'))) AS INTEGER), 0) as daysPending
        FROM buyers b
        WHERE pendingAmount > 0 AND isDeleted = 0
        UNION ALL
        SELECT id as entityId, name, 'FARMER' as type, pendingAmount,
               (SELECT MAX(date) FROM payments p WHERE p.partyId = f.id AND p.isDeleted = 0) as lastPaymentDate,
               COALESCE(CAST((JulianDay('now') - JulianDay(datetime((SELECT MAX(date) FROM payments p WHERE p.partyId = f.id AND p.isDeleted = 0)/1000, 'unixepoch'))) AS INTEGER), 0) as daysPending
        FROM farmers f
        WHERE pendingAmount > 0 AND isDeleted = 0
    """)
    fun getOutstandingAgingReport(): Flow<List<OutstandingAgingModel>>

    @Query("""
        SELECT id, date, partyName, partyType, amount, paymentMode, remainingBalance, 'SUCCESS' as status
        FROM payments
        WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getPaymentReport(startDate: Long, endDate: Long): Flow<List<PaymentReportModel>>

    @Query("""
        SELECT 
            si.id, 
            s.buyerName, 
            si.farmerName, 
            si.productName, 
            a.grade,
            si.quantitySold as quantity, 
            si.saleAmount, 
            a.commissionPercent,
            ((si.quantitySold * a.purchaseRate) * a.commissionPercent / 100) as commissionAmount,
            si.marginAmount,
            si.date
        FROM sale_items si
        JOIN sales s ON si.saleId = s.id
        JOIN arrivals a ON si.arrivalId = a.id
        WHERE si.date BETWEEN :startDate AND :endDate
        ORDER BY si.date DESC
    """)
    fun getCommissionReport(startDate: Long, endDate: Long): Flow<List<CommissionReportModel>>

    @Query("""
        SELECT strftime('%d/%m', datetime(date/1000, 'unixepoch')) as label, SUM(totalAmount) as value
        FROM sales
        WHERE date > :sinceDate AND isDeleted = 0
        GROUP BY label
        ORDER BY date ASC
    """)
    fun getSalesTrend(sinceDate: Long): Flow<List<ChartDataModel>>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE date BETWEEN :start AND :end AND isDeleted = 0")
    fun getTotalSales(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(grossAmount) FROM arrivals WHERE date BETWEEN :start AND :end AND isDeleted = 0")
    fun getTotalPurchases(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(commissionAmount) FROM arrivals WHERE date BETWEEN :start AND :end AND isDeleted = 0")
    fun getTotalCommission(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(pendingAmount) FROM buyers WHERE isDeleted = 0")
    fun getBuyerPendingTotal(): Flow<Double?>

    @Query("SELECT SUM(pendingAmount) FROM farmers WHERE isDeleted = 0")
    fun getFarmerPendingTotal(): Flow<Double?>
}
