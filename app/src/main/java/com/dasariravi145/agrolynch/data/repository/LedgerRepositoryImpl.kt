package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerRepositoryImpl @Inject constructor(
    private val arrivalDao: ArrivalDao,
    private val saleDao: SaleDao,
    private val paymentDao: PaymentDao,
    private val farmerDao: FarmerDao,
    private val buyerDao: BuyerDao,
    private val productDao: ProductDao,
) : LedgerRepository {

    override fun getFarmerLedger(farmerId: String): Flow<LedgerSummary> {
        return combine(
            arrivalDao.getAllArrivals(),
            paymentDao.getAllPayments(),
            farmerDao.getAllFarmers()
        ) { arrivals, payments, farmers ->
            val farmer = farmers.find { it.id == farmerId } ?: return@combine LedgerSummary("", "Unknown", 0.0, 0.0, 0.0)
            calculateFarmerSummary(farmer, arrivals, payments)
        }
    }

    private fun calculateFarmerSummary(
        farmer: FarmerEntity,
        allArrivals: List<ArrivalEntity>,
        allPayments: List<PaymentEntity>
    ): LedgerSummary {
        val farmerArrivals = allArrivals.filter { it.farmerId == farmer.id && !it.isDeleted }
        val farmerPayments = allPayments.filter { it.partyId == farmer.id && it.partyType == "FARMER" && !it.isDeleted }

        val entries = (farmerArrivals.map { arrival ->
            val details = LedgerEntryDetails(
                billNumber = arrival.id.takeLast(6).uppercase(),
                productName = arrival.productName,
                category = arrival.productCategory,
                grade = arrival.grade,
                quantity = arrival.quantity,
                unit = arrival.unit,
                rate = arrival.purchaseRate,
                grossAmount = arrival.grossAmount,
                commissionPercent = arrival.commissionPercent,
                commissionAmount = arrival.commissionAmount,
                netAmount = arrival.netAmount,
                laborCharges = 0.0, // Farmer side charges not in current arrival entity
                transportCharges = 0.0
            )
            LedgerEntry(
                id = arrival.id,
                title = "Stock Arrival: ${arrival.productName}",
                amount = arrival.netAmount,
                type = LedgerType.DEBIT,
                transactionType = TransactionType.ARRIVAL,
                date = arrival.date,
                status = if (arrival.netAmount == 0.0) LedgerStatus.PAID else LedgerStatus.PENDING,
                details = details
            )
        }
+ farmerPayments.map { payment ->
            val details = LedgerEntryDetails(
                paymentMade = payment.amount
            )
            LedgerEntry(
                id = payment.id,
                title = "Payment: ${payment.paymentMode}",
                amount = payment.amount,
                type = LedgerType.CREDIT,
                transactionType = TransactionType.PAYMENT,
                date = payment.date,
                status = LedgerStatus.PAID,
                reference = payment.referenceNumber,
                details = details
            )
        }).sortedBy { it.date }

        var currentBalance = 0.0
        val entriesWithBalance = entries.map {
            currentBalance += if (it.type == LedgerType.DEBIT) it.amount else -it.amount
            it.copy(balance = currentBalance)
        }

        return LedgerSummary(
            partyId = farmer.id,
            partyName = farmer.name,
            totalDebit = farmerArrivals.sumOf { it.netAmount },
            totalCredit = farmerPayments.sumOf { it.amount },
            balance = farmer.pendingAmount,
            advanceAmount = farmer.advanceAmount,
            totalTransactions = entries.size,
            lastTransactionDate = entries.lastOrNull()?.date ?: 0L,
            entries = entriesWithBalance.reversed()
        )
    }

    override fun getBuyerLedger(buyerId: String): Flow<LedgerSummary> {
        return combine(
            saleDao.getAllSales(),
            paymentDao.getAllPayments(),
            buyerDao.getAllBuyers(),
            productDao.getAllProducts()
        ) { sales, payments, buyers, products ->
            val buyer = buyers.find { it.id == buyerId } ?: return@combine LedgerSummary("", "Unknown", 0.0, 0.0, 0.0)
            calculateBuyerSummary(buyer, sales, payments, products)
        }
    }

    private fun calculateBuyerSummary(
        buyer: BuyerEntity,
        allSales: List<SaleEntity>,
        allPayments: List<PaymentEntity>,
        allProducts: List<ProductEntity> = emptyList()
    ): LedgerSummary {
        val buyerSales = allSales.filter { it.buyerId == buyer.id && !it.isDeleted }
        val buyerPayments = allPayments.filter { it.partyId == buyer.id && it.partyType == "BUYER" && !it.isDeleted }

        val entries = (buyerSales.map { sale ->
            val product = allProducts.find { it.id == sale.productId }
            val details = LedgerEntryDetails(
                billNumber = sale.id.takeLast(6).uppercase(),
                productName = sale.productName,
                category = product?.category ?: "General",
                grade = sale.grade,
                quantity = sale.totalQuantity,
                unit = "KG", 
                rate = if (sale.totalQuantity > 0) sale.totalAmount / sale.totalQuantity else 0.0,
                grossAmount = sale.totalAmount,
                commissionAmount = sale.totalMargin, // Margin is treated as commission in buyer side display
                transportCharges = sale.transportCharges,
                laborCharges = sale.otherCharges,
                netAmount = sale.totalAmount + sale.transportCharges + sale.otherCharges
            )
            LedgerEntry(
                id = sale.id,
                title = "Purchase: ${sale.productName}",
                amount = details.netAmount,
                type = LedgerType.DEBIT,
                transactionType = TransactionType.SALE,
                date = sale.date,
                status = if (sale.pendingAmount == 0.0) LedgerStatus.PAID else if (sale.paidAmount > 0) LedgerStatus.PARTIAL else LedgerStatus.PENDING,
                details = details
            )
        }
+ buyerPayments.map { payment ->
            val details = LedgerEntryDetails(
                paymentMade = payment.amount
            )
            LedgerEntry(
                id = payment.id,
                title = "Receipt: ${payment.paymentMode}",
                amount = payment.amount,
                type = LedgerType.CREDIT,
                transactionType = TransactionType.PAYMENT,
                date = payment.date,
                status = LedgerStatus.PAID,
                reference = payment.referenceNumber,
                details = details
            )
        }).sortedBy { it.date }

        var currentBalance = 0.0
        val entriesWithBalance = entries.map {
            currentBalance += if (it.type == LedgerType.DEBIT) it.amount else -it.amount
            it.copy(balance = currentBalance)
        }

        return LedgerSummary(
            partyId = buyer.id,
            partyName = buyer.name,
            totalDebit = buyerSales.sumOf { it.totalAmount + it.transportCharges + it.otherCharges },
            totalCredit = buyerPayments.sumOf { it.amount },
            balance = buyer.pendingAmount,
            advanceAmount = 0.0,
            totalTransactions = entries.size,
            lastTransactionDate = entries.lastOrNull()?.date ?: 0L,
            entries = entriesWithBalance.reversed()
        )
    }

    override fun getAllFarmerSummaries(): Flow<List<LedgerSummary>> {
        return combine(
            arrivalDao.getAllArrivals(),
            paymentDao.getAllPayments(),
            farmerDao.getAllFarmers()
        ) { arrivals, payments, farmers ->
            farmers.map { farmer ->
                calculateFarmerSummary(farmer, arrivals, payments)
            }
        }
    }

    override fun getAllBuyerSummaries(): Flow<List<LedgerSummary>> {
        return combine(
            saleDao.getAllSales(),
            paymentDao.getAllPayments(),
            buyerDao.getAllBuyers(),
            productDao.getAllProducts()
        ) { sales, payments, buyers, products ->
            buyers.map { buyer ->
                calculateBuyerSummary(buyer, sales, payments, products)
            }
        }
    }
}
