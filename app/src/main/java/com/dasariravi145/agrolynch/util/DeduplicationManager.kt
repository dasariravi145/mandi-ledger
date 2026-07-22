package com.dasariravi145.agrolynch.util

import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeduplicationManager @Inject constructor(
    private val database: AgroLynchDatabase
) {
    private val arrivalDao = database.arrivalDao()
    private val saleDao = database.saleDao()
    private val paymentDao = database.paymentDao()
    private val transactionDao = database.transactionDao()
    private val farmerDao = database.farmerDao()
    private val buyerDao = database.buyerDao()

    suspend fun findDuplicateFarmerCandidates(): List<DuplicateCandidate<FarmerEntity>> {
        val farmers = farmerDao.getFarmersList()
        val groups = farmers.filter { it.mobileNumber.isNotBlank() }
            .groupBy { PhoneNumberUtils.normalize(it.mobileNumber) }
            .filter { it.value.size > 1 }

        return groups.map { (phone, list) ->
            DuplicateCandidate(
                canonicalId = list.first().id,
                duplicates = list.drop(1),
                normalizedPhone = phone,
                name = list.first().name
            )
        }
    }

    suspend fun findDuplicateBuyerCandidates(): List<DuplicateCandidate<BuyerEntity>> {
        val buyers = buyerDao.getBuyersList()
        val groups = buyers.filter { it.mobileNumber.isNotBlank() }
            .groupBy { PhoneNumberUtils.normalize(it.mobileNumber) }
            .filter { it.value.size > 1 }

        return groups.map { (phone, list) ->
            DuplicateCandidate(
                canonicalId = list.first().id,
                duplicates = list.drop(1),
                normalizedPhone = phone,
                name = list.first().name
            )
        }
    }

    suspend fun mergeDuplicates(): MergeReport {
        var farmerMerged = 0
        var buyerMerged = 0
        
        database.withTransaction {
            // 1. Merge Farmers
            val farmerDuplicates = findDuplicateFarmerCandidates()
            farmerDuplicates.forEach { candidate ->
                val canonicalId = candidate.canonicalId
                candidate.duplicates.forEach { duplicate ->
                    val duplicateId = duplicate.id
                    
                    arrivalDao.updateFarmerId(duplicateId, canonicalId)
                    saleDao.updateFarmerIdInItems(duplicateId, canonicalId)
                    paymentDao.updatePartyId(duplicateId, canonicalId)
                    transactionDao.updateFarmerId(duplicateId, canonicalId)
                    
                    farmerDao.deleteFarmerById(duplicateId)
                    farmerMerged++
                }
                recalculateFarmerBalances(canonicalId)
            }

            // 2. Merge Buyers
            val buyerDuplicates = findDuplicateBuyerCandidates()
            buyerDuplicates.forEach { candidate ->
                val canonicalId = candidate.canonicalId
                candidate.duplicates.forEach { duplicate ->
                    val duplicateId = duplicate.id
                    
                    saleDao.updateBuyerId(duplicateId, canonicalId)
                    paymentDao.updatePartyId(duplicateId, canonicalId)
                    
                    buyerDao.deleteBuyerById(duplicateId)
                    buyerMerged++
                }
                recalculateBuyerBalances(canonicalId)
            }
        }
        
        return MergeReport(farmerMerged, buyerMerged)
    }

    suspend fun recalculateFarmerBalances(farmerId: String) {
        val totalGrossArrivals = arrivalDao.getSumGrossAmountForFarmer(farmerId) ?: 0.0
        val totalNetArrivals = arrivalDao.getSumNetAmountForFarmer(farmerId) ?: 0.0
        val totalLegacyTrans = transactionDao.getSumTotalAmountForFarmer(farmerId) ?: 0.0
        val totalPayments = paymentDao.getSumAmountForParty(farmerId, "FARMER") ?: 0.0

        val totalReceivable = totalNetArrivals + totalLegacyTrans
        val balance = totalReceivable - totalPayments

        val newPending = if (balance > 0) balance else 0.0
        val newAdvance = if (balance < 0) -balance else 0.0

        val farmer = farmerDao.getFarmerById(farmerId)
        farmer?.let {
            farmerDao.updateFarmer(it.copy(
                totalArrivals = totalGrossArrivals,
                totalPayments = totalPayments,
                pendingAmount = newPending,
                advanceAmount = newAdvance,
                isSynced = false,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    suspend fun recalculateBuyerBalances(buyerId: String) {
        val totalPurchase = saleDao.getSumTotalNetAmountForBuyer(buyerId) ?: 0.0
        val totalPaidByPayments = paymentDao.getSumAmountForParty(buyerId, "BUYER") ?: 0.0
        val totalPaidBySales = saleDao.getSumPaidAmountForBuyer(buyerId) ?: 0.0
        
        val totalPaid = totalPaidByPayments + totalPaidBySales
        val balance = totalPurchase - totalPaid

        val buyer = buyerDao.getBuyerById(buyerId)
        buyer?.let {
            buyerDao.updateBuyer(it.copy(
                totalPurchase = totalPurchase,
                totalPaid = totalPaid,
                pendingAmount = balance,
                isSynced = false,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    suspend fun recalculateAllAffected(farmerIds: Set<String>, buyerIds: Set<String>) {
        farmerIds.forEach { recalculateFarmerBalances(it) }
        buyerIds.forEach { recalculateBuyerBalances(it) }
        reconcileStockBalances()
    }

    suspend fun reconcileStockBalances(): ReconciliationReport {
        val arrivals = arrivalDao.getAllArrivalsList().filter { !it.isDeleted }
        val allSales = saleDao.getAllSalesList()
        val activeSaleIds = allSales.filter { !it.isDeleted }.map { it.id }.toSet()
        
        val allItems = saleDao.getAllSaleItemsList().filter { it.saleId in activeSaleIds }
        val itemsByArrival = allItems.groupBy { it.arrivalId }

        var checked = 0
        var corrected = 0
        var orphans = 0

        arrivals.forEach { arrival ->
            checked++
            val items = itemsByArrival[arrival.id] ?: emptyList()
            val totalSoldKg = items.sumOf { it.quantitySold }
            
            // Convert KG sold to the arrival's unit (canonical storage unit is Ton for Ton/Boxes, KG for KG)
            val soldInUnit = when(arrival.unit) {
                "Ton", "Boxes" -> totalSoldKg / 1000.0
                else -> totalSoldKg
            }
            
            var newRemaining = (arrival.netQuantity - soldInUnit).coerceAtLeast(0.0)
            
            // Precision snap: if remaining is extremely small, set to zero.
            // Tolerance: 0.02 Tons (20 KG) for bulk units, 0.5 KG for KG units
            val snapThreshold = when(arrival.unit) {
                "Ton", "Boxes" -> 0.02 
                else -> 0.5
            }

            if (newRemaining < snapThreshold) {
                newRemaining = 0.0
            }

            if (Math.abs(arrival.remainingQuantity - newRemaining) > 1e-6) {
                arrivalDao.updateArrival(arrival.copy(remainingQuantity = newRemaining))
                corrected++
                Timber.d("Stock Reconciled: arrivalId=${arrival.id}, old=${arrival.remainingQuantity}, new=$newRemaining, soldKg=$totalSoldKg")
            }
        }

        // Identify orphan sale items
        val arrivalIds = arrivals.map { it.id }.toSet()
        allItems.forEach { item ->
            if (item.arrivalId.isNotBlank() && item.arrivalId !in arrivalIds) {
                orphans++
                Timber.w("Orphan Sale Item detected: itemId=${item.id}, arrivalId=${item.arrivalId}, product=${item.productName}, farmer=${item.farmerName}")
            }
        }

        return ReconciliationReport(checked, corrected, orphans)
    }
}

data class ReconciliationReport(
    val checkedCount: Int,
    val correctedCount: Int,
    val orphanCount: Int
)

data class DuplicateCandidate<T>(
    val canonicalId: String,
    val duplicates: List<T>,
    val normalizedPhone: String,
    val name: String
)

data class MergeReport(
    val farmerMergedCount: Int,
    val buyerMergedCount: Int
)
