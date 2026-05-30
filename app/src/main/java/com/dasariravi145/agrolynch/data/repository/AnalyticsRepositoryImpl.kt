package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val arrivalDao: ArrivalDao,
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao
) : AnalyticsRepository {

    override fun getAnalyticsSummary(): Flow<AnalyticsSummary> {
        return combine(
            transactionDao.getAllTransactions(),
            arrivalDao.getAllArrivals(),
            saleDao.getAllSales(),
            expenseDao.getAllExpenses(),
            paymentDao.getAllPayments()
        ) { transactions, arrivals, sales, expenses, payments ->
            
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val now = System.currentTimeMillis()
            val startOfToday = getStartOfDay(now)
            val endOfToday = getEndOfDay(now)

            // Today's Sales
            val todaySales = sales.filter { it.date in startOfToday..endOfToday }.sumOf { it.totalAmount }
            
            // Commission Earned = Sale Margins + Arrival Commissions
            val totalCommission = sales.sumOf { it.totalMargin } + arrivals.sumOf { it.commissionAmount }

            // Buyer Pending = Total Sales - Payments from Buyers
            val totalSalesAmount = sales.sumOf { it.totalAmount }
            val buyerPayments = payments.filter { it.partyType == "BUYER" }.sumOf { it.amount }
            val buyerPending = totalSalesAmount - buyerPayments

            // Farmer Pending = Total Farmer Payable - Payments to Farmers
            val totalFarmerPayable = arrivals.sumOf { it.netAmount }
            val farmerPayments = payments.filter { it.partyType == "FARMER" }.sumOf { it.amount }
            val farmerPending = totalFarmerPayable - farmerPayments

            // Net Balance = Buyer Pending - Farmer Pending
            val netBalance = buyerPending - farmerPending

            // Cash Received (from Buyers)
            val cashReceived = buyerPayments

            // Combine legacy transactions and new arrivals for trends
            val allArrivalsTotal = arrivals + transactions.map { 
                ArrivalEntity(id = it.id, farmerName = it.farmerName, netAmount = it.totalAmount, quantity = it.quantity, date = it.date, productName = it.productName.ifEmpty { it.fruitType })
            }
            
            // 1. Sales Trend (Last 7 days)
            val salesTrend = (0..6).map { i ->
                val dateCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateStart = getStartOfDay(dateCal.timeInMillis)
                val dateEnd = getEndOfDay(dateCal.timeInMillis)
                
                val daySales = sales.filter { it.date in dateStart..dateEnd }.sumOf { it.totalAmount }
                ChartPoint(dateFormat.format(dateCal.time), daySales.toFloat())
            }.reversed()

            // 2. Top Farmers
            val topFarmers = allArrivalsTotal.groupBy { it.farmerName }
                .map { (name, list) ->
                    TopEntity(name, list.sumOf { it.netAmount }, list.size)
                }
                .sortedByDescending { it.totalValue }
                .take(5)

            // 3. Top Buyers
            val topBuyers = sales.groupBy { it.buyerName }
                .map { (name, list) ->
                    TopEntity(name, list.sumOf { it.totalAmount }, list.size)
                }
                .sortedByDescending { it.totalValue }
                .take(5)

            // 4. Product Distribution
            val colors = listOf(0xFF6200EE, 0xFF03DAC5, 0xFFBB86FC, 0xFF3700B3, 0xFF018786)
            val productDistribution = sales.groupBy { it.productName }
                .toList()
                .mapIndexed { index, pair ->
                    val name = pair.first
                    val list = pair.second
                    PieChartData(name, list.sumOf { it.totalQuantity }.toFloat(), colors[index % colors.size])
                }
                .sortedByDescending { it.value }
                .take(5)

            // 5. Profit Trend (Sales - Expenses - Farmer Payables)
            val profitTrend = (0..6).map { i ->
                val dateCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateStart = getStartOfDay(dateCal.timeInMillis)
                val dateEnd = getEndOfDay(dateCal.timeInMillis)
                
                val daySales = sales.filter { it.date in dateStart..dateEnd }.sumOf { it.totalAmount }
                val dayExpenses = expenses.filter { it.date in dateStart..dateEnd }.sumOf { it.amount }
                ChartPoint(dateFormat.format(dateCal.time), (daySales - dayExpenses).toFloat())
            }.reversed()

            AnalyticsSummary(
                todaySales = todaySales,
                commissionEarned = totalCommission,
                buyerPending = buyerPending,
                farmerPending = farmerPending,
                netBalance = netBalance,
                cashReceived = cashReceived,
                salesTrend = salesTrend,
                profitTrend = profitTrend,
                topFarmers = topFarmers,
                topBuyers = topBuyers,
                productDistribution = productDistribution
            )
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
