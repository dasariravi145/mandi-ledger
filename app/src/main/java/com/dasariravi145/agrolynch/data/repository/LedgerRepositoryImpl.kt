package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
import com.dasariravi145.agrolynch.domain.repository.BillNumberRepository
import kotlinx.coroutines.Dispatchers
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
    private val billNumberRepository: BillNumberRepository
) : LedgerRepository {

    override fun getFarmerLedger(farmerId: String): Flow<LedgerSummary> {
        return combine(
            arrivalDao.getAllArrivals().distinctUntilChanged(),
            paymentDao.getAllPayments().distinctUntilChanged(),
            farmerDao.getAllFarmers().distinctUntilChanged()
        ) { arrivals, payments, farmers ->
            val farmer = farmers.find { it.id == farmerId } ?: return@combine null
            farmer to (arrivals.filter { it.farmerId == farmerId && !it.isDeleted } to 
                      payments.filter { it.partyId == farmerId && it.partyType == "FARMER" && !it.isDeleted })
        }.map { data ->
            if (data == null) return@map LedgerSummary("", "Unknown", 0.0, 0.0, 0.0)
            val (farmer, pair) = data
            val (arrivals, payments) = pair
            
            val arrivalsByBill = arrivals.groupBy { it.billNumber }
            
            val entries = (arrivalsByBill.map { (billNo, billArrivals) ->
                val firstArrival = billArrivals.first()
                val totalGross = billArrivals.sumOf { it.grossAmount }
                val totalNetPayable = billArrivals.sumOf { it.netAmount }
                val totalCommission = billArrivals.sumOf { it.commissionAmount }
                val totalLabor = billArrivals.sumOf { it.laborCharges }
                val totalTransport = billArrivals.sumOf { it.transportCharges }
                val totalPacking = billArrivals.sumOf { it.packingCharges }
                val totalOtherDeductions = billArrivals.sumOf { it.otherDeductions }
                
                val allDeductions = billArrivals.flatMap { billNumberRepository.getDeductionsByEntryIdSync(it.id) }.distinctBy { it.id }

                val details = LedgerEntryDetails(
                    billNumber = if (billNo.isBlank() || billNo == "N/A") "Legacy-${firstArrival.id.take(8).uppercase()}" else billNo,
                    productName = firstArrival.productName,
                    category = firstArrival.productCategory,
                    grade = if (billArrivals.size > 1) "Multiple" else firstArrival.grade,
                    quantity = billArrivals.sumOf { it.quantity },
                    damageQuantity = billArrivals.sumOf { it.spoilageQuantity },
                    netQuantity = billArrivals.sumOf { it.netQuantity },
                    unit = firstArrival.unit,
                    rate = if (billArrivals.size == 1) firstArrival.purchaseRate else 0.0,
                    purchaseRate = if (billArrivals.size == 1) firstArrival.purchaseRate else 0.0,
                    ratePerKg = if (billArrivals.size == 1) firstArrival.ratePerKg else 0.0,
                    grossAmount = totalGross,
                    commissionPercent = firstArrival.commissionPercent,
                    commissionAmount = totalCommission,
                    netAmount = totalNetPayable,
                    totalNetWeightKg = billArrivals.sumOf { it.finalNetWeightKg },
                    totalWeightTon = billArrivals.sumOf { it.totalWeightTon },
                    emptyBoxWeightPerBox = firstArrival.emptyBoxWeightPerBox,
                    totalGrossKg = billArrivals.sumOf { it.grossWeightKg },
                    lessWeightKg = billArrivals.sumOf { it.totalEmptyBoxWeightKg },
                    spoilagePercentage = firstArrival.spoilagePercentage,
                    spoilageKg = billArrivals.sumOf { it.spoilageKg },
                    laborCharges = totalLabor,
                    transportCharges = totalTransport,
                    packingCharges = totalPacking,
                    otherDeductions = totalOtherDeductions,
                    deductions = allDeductions,
                    arrivalItems = billArrivals
                )
                LedgerEntry(
                    id = firstArrival.id,
                    title = "Stock Arrival: ${if(billArrivals.distinctBy { it.productId }.size > 1) "Multiple Products" else firstArrival.productName}",
                    amount = totalNetPayable,
                    type = LedgerType.DEBIT,
                    transactionType = TransactionType.ARRIVAL,
                    date = firstArrival.date,
                    status = if (totalNetPayable == 0.0) LedgerStatus.PAID else LedgerStatus.PENDING,
                    details = details
                )
            } + payments.map { payment ->
                LedgerEntry(
                    id = payment.id,
                    title = "Payment: ${payment.paymentMode}",
                    amount = payment.amount,
                    type = LedgerType.CREDIT,
                    transactionType = TransactionType.PAYMENT,
                    date = payment.date,
                    status = LedgerStatus.PAID,
                    reference = payment.referenceNumber,
                    details = LedgerEntryDetails(
                        paymentMade = payment.amount, 
                        billNumber = if (payment.billNumber.isBlank() || payment.billNumber == "N/A") "Legacy-${payment.id.take(8).uppercase()}" else payment.billNumber
                    )
                )
            }).sortedBy { it.date }

            var currentBalance = 0.0
            val entriesWithBalance = entries.map {
                currentBalance += if (it.type == LedgerType.DEBIT) it.amount else -it.amount
                it.copy(balance = currentBalance)
            }

            LedgerSummary(
                partyId = farmer.id,
                partyName = farmer.name,
                totalDebit = arrivals.sumOf { it.netAmount },
                totalCredit = payments.sumOf { it.amount },
                balance = farmer.pendingAmount,
                advanceAmount = farmer.advanceAmount,
                totalTransactions = entries.size,
                lastTransactionDate = entries.lastOrNull()?.date ?: 0L,
                entries = entriesWithBalance.reversed()
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getBuyerLedger(buyerId: String): Flow<LedgerSummary> {
        return combine(
            saleDao.getAllSales().distinctUntilChanged(),
            saleDao.getAllSaleItems().distinctUntilChanged(),
            paymentDao.getAllPayments().distinctUntilChanged(),
            buyerDao.getAllBuyers().distinctUntilChanged()
        ) { sales, items, payments, buyers ->
            val buyer = buyers.find { it.id == buyerId } ?: return@combine null
            buyer to (Triple(sales.filter { it.buyerId == buyerId && !it.isDeleted }, items, payments.filter { it.partyId == buyerId && it.partyType == "BUYER" && !it.isDeleted }))
        }.map { data ->
            if (data == null) return@map LedgerSummary("", "Unknown", 0.0, 0.0, 0.0)
            val (buyer, triple) = data
            val (sales, allItems, payments) = triple
            
            val entries = (sales.map { sale ->
                val saleItems = allItems.filter { it.saleId == sale.id }
                val deductions = billNumberRepository.getDeductionsByEntryIdSync(sale.id)
                val originalQty = if (saleItems.isNotEmpty()) saleItems.sumOf { it.inputQuantity } else sale.totalQuantity
                val details = LedgerEntryDetails(
                    billNumber = if (sale.billNumber.isBlank() || sale.billNumber == "N/A") "Legacy-${sale.id.take(8).uppercase()}" else sale.billNumber,
                    farmerName = sale.farmerName,
                    productName = sale.productName,
                    category = "General",
                    grade = sale.grade,
                    quantity = originalQty,
                    unit = if (saleItems.isNotEmpty()) saleItems.first().unit else "KG",
                    rate = if (originalQty > 0) sale.totalAmount / originalQty else 0.0,
                    purchaseRate = if (originalQty > 0) sale.totalPurchaseAmount / originalQty else 0.0,
                    ratePerKg = if (saleItems.isNotEmpty()) saleItems.first().saleRate else 0.0,
                    grossAmount = sale.totalAmount,
                    commissionAmount = sale.totalCommission, 
                    transportCharges = sale.transportCharges,
                    laborCharges = sale.laborCharges,
                    packingCharges = sale.packingCharges,
                    otherDeductions = sale.otherCharges,
                    netAmount = sale.totalNetAmount,
                    totalNetWeightKg = saleItems.sumOf { it.quantitySold },
                    paymentMade = sale.paidAmount,
                    pendingAmount = sale.pendingAmount,
                    deductions = deductions,
                    saleItems = saleItems
                )
                LedgerEntry(
                    id = sale.id,
                    title = "Purchase: ${sale.productName}",
                    amount = sale.totalNetAmount,
                    type = LedgerType.DEBIT,
                    transactionType = TransactionType.SALE,
                    date = sale.date,
                    status = if (sale.pendingAmount == 0.0) LedgerStatus.PAID else if (sale.paidAmount > 0) LedgerStatus.PARTIAL else LedgerStatus.PENDING,
                    details = details
                )
            } + payments.map { payment ->
                LedgerEntry(
                    id = payment.id,
                    title = "Receipt: ${payment.paymentMode}",
                    amount = payment.amount,
                    type = LedgerType.CREDIT,
                    transactionType = TransactionType.PAYMENT,
                    date = payment.date,
                    status = LedgerStatus.PAID,
                    reference = payment.referenceNumber,
                    details = LedgerEntryDetails(
                        paymentMade = payment.amount, 
                        billNumber = if (payment.billNumber.isBlank() || payment.billNumber == "N/A") "Legacy-${payment.id.take(8).uppercase()}" else payment.billNumber
                    )
                )
            }).sortedBy { it.date }

            var currentBalance = 0.0
            val entriesWithBalance = entries.map {
                currentBalance += if (it.type == LedgerType.DEBIT) it.amount else -it.amount
                it.copy(balance = currentBalance)
            }

            LedgerSummary(
                partyId = buyer.id,
                partyName = buyer.name,
                totalDebit = sales.sumOf { it.totalNetAmount },
                totalCredit = payments.sumOf { it.amount },
                balance = buyer.pendingAmount,
                advanceAmount = 0.0,
                totalTransactions = entries.size,
                lastTransactionDate = entries.lastOrNull()?.date ?: 0L,
                entries = entriesWithBalance.reversed()
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllFarmerSummaries(): Flow<List<LedgerSummary>> {
        return combine(
            arrivalDao.getAllArrivals().distinctUntilChanged(),
            paymentDao.getAllPayments().distinctUntilChanged(),
            farmerDao.getAllFarmers().distinctUntilChanged()
        ) { arrivals, payments, farmers ->
            val arrivalMap = arrivals.filter { !it.isDeleted }.groupBy { it.farmerId }
            val paymentMap = payments.filter { it.partyType == "FARMER" && !it.isDeleted }.groupBy { it.partyId }
            
            farmers.filter { !it.isDeleted }.map { farmer ->
                val farmerArrivals = arrivalMap[farmer.id] ?: emptyList()
                val farmerPayments = paymentMap[farmer.id] ?: emptyList()
                
                // Use dynamic calculation for balance to ensure consistency (Fixes M2)
                val totalDebit = farmerArrivals.sumOf { it.netAmount }
                val totalCredit = farmerPayments.sumOf { it.amount }

                LedgerSummary(
                    partyId = farmer.id,
                    partyName = farmer.name,
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    balance = totalDebit - totalCredit,
                    advanceAmount = farmer.advanceAmount,
                    totalTransactions = farmerArrivals.size + farmerPayments.size,
                    lastTransactionDate = maxOf(
                        farmerArrivals.maxOfOrNull { it.date } ?: 0L,
                        farmerPayments.maxOfOrNull { it.date } ?: 0L
                    ),
                    entries = emptyList() // Summaries usually don't need the full entry list
                )
            }.sortedByDescending { it.balance }
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllBuyerSummaries(): Flow<List<LedgerSummary>> {
        return combine(
            saleDao.getAllSales().distinctUntilChanged(),
            paymentDao.getAllPayments().distinctUntilChanged(),
            buyerDao.getAllBuyers().distinctUntilChanged()
        ) { sales, payments, buyers ->
            val saleMap = sales.filter { !it.isDeleted }.groupBy { it.buyerId }
            val paymentMap = payments.filter { it.partyType == "BUYER" && !it.isDeleted }.groupBy { it.partyId }

            buyers.filter { !it.isDeleted }.map { buyer ->
                val buyerSales = saleMap[buyer.id] ?: emptyList()
                val buyerPayments = paymentMap[buyer.id] ?: emptyList()
                
                val totalDebit = buyerSales.sumOf { it.totalNetAmount }
                val totalCredit = buyerPayments.sumOf { it.amount }

                LedgerSummary(
                    partyId = buyer.id,
                    partyName = buyer.name,
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    balance = totalDebit - totalCredit,
                    advanceAmount = 0.0,
                    totalTransactions = buyerSales.size + buyerPayments.size,
                    lastTransactionDate = maxOf(
                        buyerSales.maxOfOrNull { it.date } ?: 0L,
                        buyerPayments.maxOfOrNull { it.date } ?: 0L
                    ),
                    entries = emptyList()
                )
            }.sortedByDescending { it.balance }
        }.flowOn(Dispatchers.IO)
    }
}
