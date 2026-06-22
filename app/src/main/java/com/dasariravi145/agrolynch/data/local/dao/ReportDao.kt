package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class StockReportModel(
    val productId: String,
    val productName: String,
    val totalQuantity: Double,
    val unit: String,
    val numberOfBoxes: Int = 0,
    val totalNetWeightKg: Double = 0.0
)

data class DetailedSaleReportModel(
    val id: String,
    val saleId: String = "",
    val buyerName: String,
    val date: Long,
    val billNumber: String = "",
    val productName: String,
    val grade: String,
    val quantity: Double, // Always Net KG
    val inputQuantity: Double = 0.0, // Original Unit Quantity
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
    val billNumber: String = "",
    val productName: String,
    val grade: String,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val grossAmount: Double,
    val commissionPercent: Double,
    val commissionAmount: Double,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val packingCharges: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val netAmount: Double,
    val pendingAmount: Double,
    val numberOfBoxes: Int = 0,
    val finalNetWeightKg: Double = 0.0,
    val totalWeightTon: Double = 0.0,
    val emptyBoxWeightPerBox: Double = 0.0,
    val totalEmptyBoxWeightKg: Double = 0.0,
    val spoilagePercentage: Double = 0.0,
    val spoilageKg: Double = 0.0
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
    val farmerName: String,
    val productName: String,
    val category: String,
    val grade: String,
    val quantity: Double,
    val netQuantity: Double,
    val rate: Double,
    val grossAmount: Double,
    val commissionPercent: Double,
    val commissionAmount: Double,
    val date: Long
)

@Dao
interface ReportDao {
    @Query("""
        SELECT productId, productName, SUM(remainingQuantity) as totalQuantity, unit,
               SUM(numberOfBoxes) as numberOfBoxes, SUM(finalNetWeightKg) as totalNetWeightKg
        FROM arrivals 
        WHERE remainingQuantity > 0 AND isDeleted = 0
        GROUP BY productId
    """)
    fun getStockReport(): Flow<List<StockReportModel>>

    @Query("""
        SELECT si.id, si.saleId, s.buyerName, s.date, s.billNumber, si.productName, si.grade, si.quantitySold as quantity,
               si.inputQuantity, si.unit, si.saleRate as rate, si.saleAmount,
               si.transportCharges, si.laborCharges, si.otherCharges,
               si.netAmount as totalAmount,
               0.0 as paidAmount, 0.0 as pendingAmount
        FROM sale_items si
        JOIN sales s ON si.saleId = s.id
        WHERE s.date BETWEEN :startDate AND :endDate AND s.isDeleted = 0
        ORDER BY s.date DESC
    """)
    fun getBuyerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedSaleReportModel>>

    @Query("""
        SELECT id, farmerName, date, billNumber, productName, grade, quantity, unit,
               purchaseRate as rate, grossAmount, commissionPercent, commissionAmount,
               laborCharges, transportCharges, packingCharges, otherDeductions,
               (SELECT COALESCE(SUM(amount), 0.0) FROM entry_deductions WHERE entryId = id AND deductionType = 'Advance') as advanceAmount,
               netAmount, farmerPendingAmount as pendingAmount,
               numberOfBoxes, finalNetWeightKg, totalWeightTon, emptyBoxWeightPerBox, totalEmptyBoxWeightKg,
               spoilagePercentage, spoilageKg
        FROM arrivals
        WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getFarmerDetailedReport(startDate: Long, endDate: Long): Flow<List<DetailedArrivalReportModel>>

    @Query("""
        SELECT a.productName, a.productCategory as category, a.grade, 
               SUM(CASE WHEN a.unit = 'Ton' THEN a.quantity * 1000.0 ELSE a.quantity END) as totalArrivals,
               COALESCE((SELECT SUM(si.quantitySold) FROM sale_items si WHERE si.productName = a.productName AND si.grade = a.grade), 0.0) as totalSold,
               SUM(CASE WHEN a.unit = 'Ton' THEN a.remainingQuantity * 1000.0 ELSE a.remainingQuantity END) as currentStock,
               AVG(CASE WHEN a.ratePerKg > 0 THEN a.ratePerKg WHEN a.unit = 'Ton' THEN a.purchaseRate / 1000.0 ELSE a.purchaseRate END) as avgPurchaseRate,
               COALESCE((SELECT AVG(si.saleRate) FROM sale_items si WHERE si.productName = a.productName AND si.grade = a.grade), 0.0) as avgSaleRate,
               0.0 as totalProfit
        FROM arrivals a
        WHERE a.isDeleted = 0
        GROUP BY a.productName, a.grade
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
            id, 
            farmerName, 
            productName, 
            productCategory as category,
            grade,
            quantity, 
            netQuantity,
            CASE WHEN purchaseRate > 0 AND ratePerKg > 0 THEN ratePerKg WHEN unit = 'Ton' THEN purchaseRate / 1000.0 ELSE purchaseRate END as rate,
            grossAmount, 
            commissionPercent,
            commissionAmount,
            date
        FROM arrivals
        WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun getCommissionReport(startDate: Long, endDate: Long): Flow<List<CommissionReportModel>>

    @Query("""
        SELECT strftime('%d/%m', datetime(date/1000, 'unixepoch')) as label, SUM(totalNetAmount) as value
        FROM sales
        WHERE date > :sinceDate AND isDeleted = 0
        GROUP BY label
        ORDER BY date ASC
    """)
    fun getSalesTrend(sinceDate: Long): Flow<List<ChartDataModel>>

    @Query("""
        SELECT SUM(totalNetAmount) 
        FROM sales 
        WHERE date BETWEEN :start AND :end AND isDeleted = 0
    """)
    fun getTotalSales(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(grossAmount) FROM arrivals WHERE date BETWEEN :start AND :end AND isDeleted = 0")
    fun getTotalPurchases(start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT 
            (SELECT COALESCE(SUM(CASE WHEN commissionAmount > 0 THEN commissionAmount ELSE (grossAmount * commissionPercent / 100) END), 0.0) FROM arrivals WHERE date BETWEEN :start AND :end AND isDeleted = 0) +
            (SELECT COALESCE(SUM(totalCommission), 0.0) FROM sales WHERE date BETWEEN :start AND :end AND isDeleted = 0)
    """)
    fun getTotalCommission(start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT (SELECT COALESCE(SUM(totalNetAmount), 0.0) FROM sales WHERE isDeleted = 0) - 
               (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE partyType = 'BUYER' AND isDeleted = 0)
    """)
    fun getBuyerPendingTotal(): Flow<Double?>

    @Query("""
        SELECT (SELECT COALESCE(SUM(netAmount), 0.0) FROM arrivals WHERE isDeleted = 0) + 
               (SELECT COALESCE(SUM(totalAmount), 0.0) FROM transactions WHERE isDeleted = 0) - 
               (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE partyType = 'FARMER' AND isDeleted = 0)
    """)
    fun getFarmerPendingTotal(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM arrivals WHERE isDeleted = 0")
    fun getArrivalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sales WHERE isDeleted = 0")
    fun getSaleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM payments WHERE isDeleted = 0")
    fun getPaymentCount(): Flow<Int>

    @Query("UPDATE sale_items SET commissionAmount = (saleAmount * commissionPercent / 100) WHERE (commissionAmount = 0 OR commissionAmount IS NULL) AND saleAmount > 0 AND commissionPercent > 0")
    suspend fun recalculateCommissions()
}
