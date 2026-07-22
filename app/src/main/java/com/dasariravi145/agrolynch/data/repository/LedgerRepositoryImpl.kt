package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
import com.dasariravi145.agrolynch.domain.repository.BillNumberRepository
import com.dasariravi145.agrolynch.util.Formatter
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

    private data class BuyerLedgerDataSnapshot(
        val sales: List<SaleEntity>,
        val allSaleItems: List<SaleItemEntity>,
        val payments: List<PaymentEntity>,
        val buyers: List<BuyerEntity>,
        val arrivals: List<ArrivalEntity>
    )

    override fun getFarmerLedger(farmerId: String): Flow<LedgerSummary> {
        return combine(
            arrivalDao.getArrivalsByFarmerFlow(farmerId).distinctUntilChanged(),
            paymentDao.getPaymentsByPartyFlow(farmerId, "FARMER").distinctUntilChanged(),
            farmerDao.getFarmerByIdFlow(farmerId).distinctUntilChanged()
        ) { arrivals, payments, farmer ->
            if (farmer == null) return@combine null
            farmer to (arrivals to payments)
        }.map { data ->
            if (data == null) return@map LedgerSummary("", "Unknown", 0.0, 0.0, 0.0)
            val (farmer, pair) = data
            val (arrivals, payments) = pair
            
            val arrivalsByBill = arrivals.groupBy { it.billNumber }
            val allArrivalIds = arrivals.map { it.id }
            val allDeductionsMap = billNumberRepository.getDeductionsByEntryIds(allArrivalIds).groupBy { it.entryId }
            
            val entries = (arrivalsByBill.map { (billNo, billArrivals) ->
                val firstArrival = billArrivals.first()
                val totalGross = billArrivals.sumOf { it.grossAmount }
                val totalNetPayable = billArrivals.sumOf { it.netAmount }
                val totalCommission = billArrivals.sumOf { it.commissionAmount }
                val totalLabor = billArrivals.sumOf { it.laborCharges }
                val totalTransport = billArrivals.sumOf { it.transportCharges }
                val totalPacking = billArrivals.sumOf { it.packingCharges }
                val totalOtherDeductions = billArrivals.sumOf { it.otherDeductions }
                
                val billDeductions = billArrivals.flatMap { allDeductionsMap[it.id] ?: emptyList() }.distinctBy { it.id }

                val details = LedgerEntryDetails(
                    billNumber = if (billNo.isBlank() || billNo == "N/A") "Legacy-${firstArrival.id.take(8).uppercase()}" else billNo,
                    productName = firstArrival.productName,
                    productType = firstArrival.productType,
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
                    numberOfBoxes = billArrivals.sumOf { it.numberOfBoxes }.toDouble(),
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
                    deductions = billDeductions,
                    arrivalItems = billArrivals
                )
                LedgerEntry(
                    id = firstArrival.id,
                    title = "Stock Arrival: ${if(billArrivals.distinctBy { it.productId }.size > 1) "Multiple Products" else firstArrival.productName}",
                    amount = totalNetPayable,
                    type = LedgerType.DEBIT,
                    transactionType = TransactionType.ARRIVAL,
                    date = firstArrival.date,
                    status = if (Formatter.normalizeMoney(totalNetPayable) == 0.0) LedgerStatus.PAID else LedgerStatus.PENDING,
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

            val totalDebit = arrivals.sumOf { it.netAmount }
            val totalCredit = payments.sumOf { it.amount }
            val diff = totalDebit - totalCredit
            val balance = Formatter.normalizeMoney(diff).coerceAtLeast(0.0)
            val advance = Formatter.normalizeMoney(-diff).coerceAtLeast(0.0)

            LedgerSummary(
                partyId = farmer.id,
                partyName = farmer.name,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                balance = balance,
                advanceAmount = advance,
                totalTransactions = entries.size,
                lastTransactionDate = entries.lastOrNull()?.date ?: 0L,
                entries = entriesWithBalance.reversed()
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getBuyerLedger(buyerId: String): Flow<LedgerSummary> {
        return combine(
            saleDao.getSalesByBuyerFlow(buyerId).distinctUntilChanged(),
            saleDao.getSaleItemsByBuyerFlow(buyerId).distinctUntilChanged(),
            paymentDao.getPaymentsByPartyFlow(buyerId, "BUYER").distinctUntilChanged(),
            buyerDao.getBuyerByIdFlow(buyerId).distinctUntilChanged(),
            arrivalDao.getAllArrivals().distinctUntilChanged()
        ) { sales, items, payments, buyer, arrivals ->
            if (buyer == null) return@combine null
            BuyerLedgerDataSnapshot(sales, items, payments, listOf(buyer), arrivals)
        }.map { snapshot ->
            if (snapshot == null) return@map LedgerSummary("", "Unknown", 0.0, 0.0, 0.0)
            val buyer = snapshot.buyers.first()
            
            val sales = snapshot.sales.filter { it.buyerId == buyerId && !it.isDeleted }
            val payments = snapshot.payments.filter { it.partyId == buyerId && it.partyType == "BUYER" && !it.isDeleted }
            val arrivalMap = snapshot.arrivals.associateBy { it.id }
            
            val allSaleIds = sales.map { it.id }
            val allDeductionsMap = billNumberRepository.getDeductionsByEntryIds(allSaleIds).groupBy { it.entryId }

            val entries = (sales.map { sale ->
                val saleItemsOfSale = snapshot.allSaleItems.filter { it.saleId == sale.id }
                val deductions = allDeductionsMap[sale.id] ?: emptyList()
                
                var totalGross = 0.0
                var totalLess = 0.0
                var totalSpoilage = 0.0
                val saleArrivals = mutableListOf<ArrivalEntity>()
                
                saleItemsOfSale.forEach { item ->
                    val arrival = arrivalMap[item.arrivalId]
                    if (arrival != null) {
                        saleArrivals.add(arrival)
                        if (arrival.finalNetWeightKg > 0) {
                            val ratio = item.quantitySold / arrival.finalNetWeightKg
                            totalGross += ratio * arrival.grossWeightKg
                            totalLess += ratio * arrival.totalEmptyBoxWeightKg
                            totalSpoilage += ratio * arrival.spoilageKg
                        }
                    }
                }

                val originalQty = if (saleItemsOfSale.isNotEmpty()) saleItemsOfSale.sumOf { it.inputQuantity } else sale.totalQuantity
                val firstSaleArrival = saleArrivals.firstOrNull()

                val details = LedgerEntryDetails(
                    billNumber = if (sale.billNumber.isBlank() || sale.billNumber == "N/A") "Legacy-${sale.id.take(8).uppercase()}" else sale.billNumber,
                    farmerName = sale.farmerName,
                    productName = sale.productName,
                    productType = sale.productType,
                    category = "General",
                    grade = sale.grade,
                    quantity = originalQty,
                    unit = if (saleItemsOfSale.isNotEmpty()) saleItemsOfSale.first().unit else "KG",
                    rate = if (originalQty > 0) sale.totalAmount / originalQty else 0.0,
                    purchaseRate = if (originalQty > 0) sale.totalPurchaseAmount / originalQty else 0.0,
                    ratePerKg = if (saleItemsOfSale.isNotEmpty()) saleItemsOfSale.first().saleRate else 0.0,
                    grossAmount = sale.totalAmount,
                    laborPercentage = sale.laborPercentage,
                    commissionAmount = sale.totalCommission, 
                    transportCharges = sale.transportCharges,
                    laborCharges = sale.laborCharges,
                    packingCharges = sale.packingCharges,
                    otherDeductions = sale.otherCharges,
                    netAmount = sale.totalNetAmount,
                    totalNetWeightKg = saleItemsOfSale.sumOf { it.quantitySold },
                    numberOfBoxes = if (saleItemsOfSale.any { it.unit == "Boxes" }) originalQty else 0.0,
                    totalGrossKg = totalGross,
                    lessWeightKg = totalLess,
                    spoilageKg = totalSpoilage,
                    emptyBoxWeightPerBox = firstSaleArrival?.emptyBoxWeightPerBox ?: 0.0,
                    spoilagePercentage = firstSaleArrival?.spoilagePercentage ?: 0.0,
                    paymentMade = sale.paidAmount,
                    pendingAmount = sale.pendingAmount,
                    deductions = deductions,
                    saleItems = saleItemsOfSale,
                    arrivalItems = saleArrivals
                )
                LedgerEntry(
                    id = sale.id,
                    title = "Purchase: ${sale.productName}",
                    amount = sale.totalNetAmount,
                    type = LedgerType.DEBIT,
                    transactionType = TransactionType.SALE,
                    date = sale.date,
                    status = if (Formatter.isAccountSettled(sale.totalNetAmount, sale.paidAmount)) LedgerStatus.PAID else if (sale.paidAmount > 0) LedgerStatus.PARTIAL else LedgerStatus.PENDING,
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

            val totalDebit = sales.sumOf { it.totalNetAmount }
            val totalCredit = payments.sumOf { it.amount }
            val diff = totalDebit - totalCredit
            val balance = Formatter.normalizeMoney(diff).coerceAtLeast(0.0)
            val advance = Formatter.normalizeMoney(-diff).coerceAtLeast(0.0)

            LedgerSummary(
                partyId = buyer.id,
                partyName = buyer.name,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                balance = balance,
                advanceAmount = advance,
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
            val distinctFarmers = farmers.distinctBy { it.id }
            val arrivalMap = arrivals.filter { !it.isDeleted }.groupBy { it.farmerId }
            val paymentMap = payments.filter { it.partyType == "FARMER" && !it.isDeleted }.groupBy { it.partyId }
            
            distinctFarmers.filter { !it.isDeleted }.mapNotNull { farmer ->
                val farmerArrivals = arrivalMap[farmer.id] ?: emptyList()
                val farmerPayments = paymentMap[farmer.id] ?: emptyList()
                
                if (farmerArrivals.isEmpty() && farmerPayments.isEmpty()) return@mapNotNull null

                val totalDebit = farmerArrivals.sumOf { it.netAmount }
                val totalCredit = farmerPayments.sumOf { it.amount }
                val diff = totalDebit - totalCredit
                val balance = Formatter.normalizeMoney(diff).coerceAtLeast(0.0)
                val advance = Formatter.normalizeMoney(-diff).coerceAtLeast(0.0)

                LedgerSummary(
                    partyId = farmer.id,
                    partyName = farmer.name,
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    balance = balance,
                    advanceAmount = advance,
                    totalTransactions = farmerArrivals.size + farmerPayments.size,
                    lastTransactionDate = maxOf(
                        farmerArrivals.maxOfOrNull { it.date } ?: 0L,
                        farmerPayments.maxOfOrNull { it.date } ?: 0L
                    ),
                    entries = emptyList()
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
            val distinctBuyers = buyers.distinctBy { it.id }
            val saleMap = sales.filter { !it.isDeleted }.groupBy { it.buyerId }
            val paymentMap = payments.filter { it.partyType == "BUYER" && !it.isDeleted }.groupBy { it.partyId }

            distinctBuyers.filter { !it.isDeleted }.mapNotNull { buyer ->
                val buyerSales = saleMap[buyer.id] ?: emptyList()
                val buyerPayments = paymentMap[buyer.id] ?: emptyList()
                
                if (buyerSales.isEmpty() && buyerPayments.isEmpty()) return@mapNotNull null

                val totalDebit = buyerSales.sumOf { it.totalNetAmount }
                val totalCredit = buyerPayments.sumOf { it.amount }
                val diff = totalDebit - totalCredit
                val balance = Formatter.normalizeMoney(diff).coerceAtLeast(0.0)
                val advance = Formatter.normalizeMoney(-diff).coerceAtLeast(0.0)

                LedgerSummary(
                    partyId = buyer.id,
                    partyName = buyer.name,
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    balance = balance,
                    advanceAmount = advance,
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
