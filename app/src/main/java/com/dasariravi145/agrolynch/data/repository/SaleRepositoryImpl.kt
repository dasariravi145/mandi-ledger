package com.dasariravi145.agrolynch.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.ArrivalDao
import com.dasariravi145.agrolynch.data.local.dao.BuyerDao
import com.dasariravi145.agrolynch.data.local.dao.PaymentDao
import com.dasariravi145.agrolynch.data.local.dao.SaleDao
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity
import com.dasariravi145.agrolynch.domain.repository.SaleRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class SaleRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val exportService: com.dasariravi145.agrolynch.util.LedgerExportService,
    @ApplicationContext private val context: Context
) : SaleRepository {

    private val saleDao = database.saleDao()
    private val arrivalDao = database.arrivalDao()
    private val buyerDao = database.buyerDao()
    private val farmerDao = database.farmerDao()
    private val paymentDao = database.paymentDao()
    private val profileDao = database.companyProfileDao()
    private val productDao = database.productDao()
    private val productTypeDao = database.productTypeDao()
    private val entryDeductionDao = database.entryDeductionDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()

    override suspend fun createSale(sale: SaleEntity, items: List<SaleItemEntity>): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val (updatedBuyer, profile) = database.withTransaction {
                    val buyer = buyerDao.getBuyerById(sale.buyerId) ?: throw Exception("Buyer not found")
                    val profile = profileDao.getProfile().first() ?: CompanyProfileEntity()

                    saleDao.insertSale(sale)
                    
                    val allArrivalIds = items.map { it.arrivalId }
                    val arrivalMap = arrivalDao.getArrivalsByIds(allArrivalIds).associateBy { it.id }

                    val updatedItems = items.map { item ->
                        val arrival = arrivalMap[item.arrivalId] ?: throw Exception("Stock item ${item.productName} not found")
                        var reductionAmount = if (arrival.unit == "Ton" || arrival.unit == "Boxes") {
                            item.quantitySold / 1000.0
                        } else {
                            item.quantitySold
                        }
                        
                        var finalQuantitySold = item.quantitySold
                        if (Math.abs(arrival.remainingQuantity - reductionAmount) < 0.001) {
                            reductionAmount = arrival.remainingQuantity
                            finalQuantitySold = if (arrival.unit == "Ton" || arrival.unit == "Boxes") {
                                arrival.remainingQuantity * 1000.0
                            } else {
                                arrival.remainingQuantity
                            }
                        }

                        arrivalDao.reduceStock(item.arrivalId, reductionAmount)
                        item.copy(quantitySold = finalQuantitySold)
                    }
                    saleDao.insertSaleItems(updatedItems)

                    val totalNetReceivable = sale.totalNetAmount
                    val updatedBuyer = buyer.copy(
                        totalPurchase = buyer.totalPurchase + totalNetReceivable,
                        pendingAmount = buyer.pendingAmount + totalNetReceivable,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    buyerDao.updateBuyer(updatedBuyer)
                    profileDao.incrementInvoiceNumber()
                    timber.log.Timber.tag("BillRef").d("Saved ledger transaction id=${sale.id} billNumber=${sale.billNumber} billId=${sale.billNumber} referenceId=${sale.id}")
                    updatedBuyer to profile
                }
                
                val dbTime = System.currentTimeMillis() - startTime
                timber.log.Timber.i("Sale Save: DB transaction took %dms", dbTime)

                // Async Background Tasks
                repositoryScope.launch {
                    val backgroundStart = System.currentTimeMillis()
                    
                    // Generate Invoice PDF with Branding in background
                    try {
                        val buyer = buyerDao.getBuyerById(sale.buyerId)
                        exportService.exportSaleToPdf(
                            context = context,
                            profile = profile,
                            sale = sale,
                            items = items,
                            deductions = emptyList(),
                            buyerMobile = buyer?.mobileNumber ?: ""
                        )
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error generating Sale PDF in background")
                    }

                    // Background sync
                    userId?.let { uid ->
                        try {
                            val userSalesRef = firestore.collection("users").document(uid).collection("sales")
                            val userBuyersRef = firestore.collection("users").document(uid).collection("buyers")
                            
                            val batch = firestore.batch()
                            
                            // Map Sale explicitly to avoid reflection/obfuscation issues
                            val saleMap = mapOf(
                                "id" to sale.id,
                                "buyerId" to sale.buyerId,
                                "buyerName" to sale.buyerName,
                                "farmerName" to sale.farmerName,
                                "totalAmount" to sale.totalAmount,
                                "totalNetAmount" to sale.totalNetAmount,
                                "paidAmount" to sale.paidAmount,
                                "pendingAmount" to sale.pendingAmount,
                                "billNumber" to sale.billNumber,
                                "date" to sale.date,
                                "ownerUserId" to uid
                            )
                            batch.set(userSalesRef.document(sale.id), saleMap)
                            
                            // Map Buyer explicitly
                            val buyerMap = mapOf(
                                "id" to updatedBuyer.id,
                                "name" to updatedBuyer.name,
                                "mobileNumber" to updatedBuyer.mobileNumber,
                                "pendingAmount" to updatedBuyer.pendingAmount,
                                "totalPurchase" to updatedBuyer.totalPurchase,
                                "lastUpdated" to updatedBuyer.lastUpdated,
                                "ownerUserId" to uid
                            )
                            batch.set(userBuyersRef.document(updatedBuyer.id), buyerMap)
                            
                            for (item in items) {
                                val itemMap = mapOf(
                                    "id" to item.id,
                                    "saleId" to item.saleId,
                                    "productName" to item.productName,
                                    "grade" to item.grade,
                                    "quantitySold" to item.quantitySold,
                                    "saleRate" to item.saleRate,
                                    "saleAmount" to item.saleAmount,
                                    "ownerUserId" to uid
                                )
                                batch.set(userSalesRef.document(sale.id).collection("items").document(item.id), itemMap)
                            }
                            batch.commit().await()
                            saleDao.markAsSynced(sale.id)
                        } catch (e: Exception) { 
                            timber.log.Timber.e(e, "Firebase sync failed for Sale")
                        }
                    }
                    
                    val bgTime = System.currentTimeMillis() - backgroundStart
                    timber.log.Timber.i("Sale Save: Background tasks took %dms", bgTime)
                }

                Resource.Success(Unit)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Create Sale Failed")
                Resource.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    override suspend fun createBuyerSaleWithFarmerArrivals(
        sale: SaleEntity,
        farmerArrivals: List<Triple<com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity, SaleItemEntity, List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>>>,
        buyerDeductions: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>
    ): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val (updatedBuyer, profile, savedArrivals, savedFarmers) = database.withTransaction {
                    // 1. Buyer
                    val buyer = buyerDao.getBuyerById(sale.buyerId) ?: throw Exception("Buyer not found")
                    val profile = profileDao.getProfile().first() ?: CompanyProfileEntity()

                    val savedArrivalsList = mutableListOf<com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity>()
                    val savedFarmersMap = mutableMapOf<String, com.dasariravi145.agrolynch.data.local.entity.FarmerEntity>()
                    val saleItemsToInsert = mutableListOf<SaleItemEntity>()

                    // 2. Process each Farmer Arrival & Sale Item
                    for ((arrival, saleItem, farmerDeductions) in farmerArrivals) {
                        // Farmer check/update
                        var farmer = savedFarmersMap[arrival.farmerId] ?: farmerDao.getFarmerById(arrival.farmerId)
                        if (farmer == null) {
                            val allFarmers = farmerDao.getFarmersList()
                            farmer = allFarmers.find { it.name.equals(arrival.farmerName, ignoreCase = true) }
                            if (farmer == null) {
                                farmer = com.dasariravi145.agrolynch.data.local.entity.FarmerEntity(
                                    id = arrival.farmerId,
                                    name = arrival.farmerName
                                )
                                farmerDao.insertFarmer(farmer)
                            }
                        }

                        val totalNetToPay = arrival.netAmount
                        var newPending = farmer.pendingAmount + totalNetToPay
                        var newAdvance = farmer.advanceAmount

                        if (newAdvance > 0) {
                            if (newAdvance >= newPending) {
                                newAdvance -= newPending
                                newPending = 0.0
                            } else {
                                newPending -= newAdvance
                                newAdvance = 0.0
                            }
                        }

                        val updatedFarmer = farmer.copy(
                            totalArrivals = farmer.totalArrivals + arrival.grossAmount,
                            pendingAmount = newPending,
                            advanceAmount = newAdvance,
                            lastUpdated = System.currentTimeMillis(),
                            isSynced = false
                        )
                        farmerDao.updateFarmer(updatedFarmer)
                        savedFarmersMap[updatedFarmer.id] = updatedFarmer

                        // Product check/update
                        var product = productDao.getProductById(arrival.productId)
                        if (product == null) {
                            product = productDao.getProductByName(arrival.productName)
                            if (product == null) {
                                product = com.dasariravi145.agrolynch.data.local.entity.ProductEntity(
                                    id = arrival.productId,
                                    name = arrival.productName,
                                    category = arrival.productCategory,
                                    availableGrades = listOf(arrival.grade)
                                )
                                productDao.insertProduct(product)
                            }
                        }

                        if (arrival.productType.isNotBlank()) {
                            val existingType = productTypeDao.getProductTypeByName(product.id, arrival.productType)
                            if (existingType == null) {
                                productTypeDao.insertProductType(
                                    com.dasariravi145.agrolynch.data.local.entity.ProductTypeEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        productId = product.id,
                                        productName = arrival.productName,
                                        productTypeName = arrival.productType
                                    )
                                )
                            }
                        }

                        // Save Arrival (remainingQuantity is set to 0.0 as it is consumed in this Buyer Sale)
                        val arrivalToSave = arrival.copy(remainingQuantity = 0.0)
                        arrivalDao.insertArrival(arrivalToSave)
                        savedArrivalsList.add(arrivalToSave)

                        // Save Farmer Deductions
                        if (farmerDeductions.isNotEmpty()) {
                            entryDeductionDao.insertAll(farmerDeductions)
                        }

                        saleItemsToInsert.add(saleItem)
                    }

                    // 3. Save Sale & Sale Items
                    saleDao.insertSale(sale)
                    saleDao.insertSaleItems(saleItemsToInsert)

                    // 4. Save Buyer Deductions
                    if (buyerDeductions.isNotEmpty()) {
                        entryDeductionDao.insertAll(buyerDeductions)
                    }

                    // 5. Update Buyer
                    val totalNetReceivable = sale.totalNetAmount
                    val updatedBuyer = buyer.copy(
                        totalPurchase = buyer.totalPurchase + totalNetReceivable,
                        pendingAmount = buyer.pendingAmount + totalNetReceivable,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    buyerDao.updateBuyer(updatedBuyer)
                    profileDao.incrementInvoiceNumber()

                    timber.log.Timber.tag("BillRef").d("Saved Buyer Sale + Farmer Arrivals transaction id=${sale.id} billNumber=${sale.billNumber}")

                    Quadruple(updatedBuyer, profile, savedArrivalsList, savedFarmersMap.values.toList())
                }

                val dbTime = System.currentTimeMillis() - startTime
                timber.log.Timber.i("Buyer Sale + Farmer Arrivals Save: DB transaction took %dms", dbTime)

                // Async Background Tasks
                repositoryScope.launch {
                    try {
                        val buyer = buyerDao.getBuyerById(sale.buyerId)
                        exportService.exportSaleToPdf(
                            context = context,
                            profile = profile,
                            sale = sale,
                            items = farmerArrivals.map { it.second },
                            deductions = buyerDeductions,
                            buyerMobile = buyer?.mobileNumber ?: ""
                        )
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error generating Sale PDF in background")
                    }

                    userId?.let { uid ->
                        try {
                            val userSalesRef = firestore.collection("users").document(uid).collection("sales")
                            val userBuyersRef = firestore.collection("users").document(uid).collection("buyers")
                            val userArrivalsRef = firestore.collection("users").document(uid).collection("arrivals")
                            val userFarmersRef = firestore.collection("users").document(uid).collection("farmers")

                            val batch = firestore.batch()

                            val saleMap = mapOf(
                                "id" to sale.id,
                                "buyerId" to sale.buyerId,
                                "buyerName" to sale.buyerName,
                                "farmerName" to sale.farmerName,
                                "totalAmount" to sale.totalAmount,
                                "totalNetAmount" to sale.totalNetAmount,
                                "paidAmount" to sale.paidAmount,
                                "pendingAmount" to sale.pendingAmount,
                                "billNumber" to sale.billNumber,
                                "date" to sale.date,
                                "ownerUserId" to uid
                            )
                            batch.set(userSalesRef.document(sale.id), saleMap)

                            val buyerMap = mapOf(
                                "id" to updatedBuyer.id,
                                "name" to updatedBuyer.name,
                                "mobileNumber" to updatedBuyer.mobileNumber,
                                "pendingAmount" to updatedBuyer.pendingAmount,
                                "totalPurchase" to updatedBuyer.totalPurchase,
                                "lastUpdated" to updatedBuyer.lastUpdated,
                                "ownerUserId" to uid
                            )
                            batch.set(userBuyersRef.document(updatedBuyer.id), buyerMap)

                            for ((_, item, _) in farmerArrivals) {
                                val itemMap = mapOf(
                                    "id" to item.id,
                                    "saleId" to item.saleId,
                                    "productName" to item.productName,
                                    "grade" to item.grade,
                                    "quantitySold" to item.quantitySold,
                                    "saleRate" to item.saleRate,
                                    "saleAmount" to item.saleAmount,
                                    "ownerUserId" to uid
                                )
                                batch.set(userSalesRef.document(sale.id).collection("items").document(item.id), itemMap)
                            }

                            for (arrival in savedArrivals) {
                                val arrivalMap = mapOf(
                                    "id" to arrival.id,
                                    "farmerId" to arrival.farmerId,
                                    "farmerName" to arrival.farmerName,
                                    "productId" to arrival.productId,
                                    "productName" to arrival.productName,
                                    "grade" to arrival.grade,
                                    "quantity" to arrival.quantity,
                                    "unit" to arrival.unit,
                                    "netAmount" to arrival.netAmount,
                                    "grossAmount" to arrival.grossAmount,
                                    "billNumber" to arrival.billNumber,
                                    "date" to arrival.date,
                                    "ownerUserId" to uid
                                )
                                batch.set(userArrivalsRef.document(arrival.id), arrivalMap)
                            }

                            for (f in savedFarmers) {
                                val farmerMap = mapOf(
                                    "id" to f.id,
                                    "name" to f.name,
                                    "mobileNumber" to f.mobileNumber,
                                    "pendingAmount" to f.pendingAmount,
                                    "advanceAmount" to f.advanceAmount,
                                    "totalArrivals" to f.totalArrivals,
                                    "lastUpdated" to f.lastUpdated,
                                    "ownerUserId" to uid
                                )
                                batch.set(userFarmersRef.document(f.id), farmerMap)
                            }

                            batch.commit().await()

                            saleDao.markAsSynced(sale.id)
                            savedArrivals.forEach { arrivalDao.markAsSynced(it.id) }
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Firebase sync failed for Buyer Sale + Farmer Arrivals")
                        }
                    }
                }

                Resource.Success(Unit)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "createBuyerSaleWithFarmerArrivals Failed")
                Resource.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    override suspend fun updateSale(sale: SaleEntity): Resource<Unit> {
        return try {
            saleDao.updateSale(sale.copy(isSynced = false))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Update failed")
        }
    }

    override suspend fun deleteSale(id: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    val sale = saleDao.getSaleById(id) ?: throw Exception("Sale not found")
                    val items = saleDao.getItemsBySaleId(id)

                    // Restore stock for each item
                    for (item in items) {
                        val arrival = arrivalDao.getArrivalById(item.arrivalId)
                        if (arrival != null) {
                            val restoreAmount = if (arrival.unit == "Ton" || arrival.unit == "Boxes") {
                                item.quantitySold / 1000.0
                            } else {
                                item.quantitySold
                            }
                            arrivalDao.updateArrival(arrival.copy(
                                remainingQuantity = arrival.remainingQuantity + restoreAmount
                            ))
                        }
                    }

                    // Adjust Buyer pending amount
                    val buyer = buyerDao.getBuyerById(sale.buyerId)
                    if (buyer != null) {
                        val amountToDeduct = sale.totalNetAmount
                        buyerDao.updateBuyer(buyer.copy(
                            totalPurchase = buyer.totalPurchase - amountToDeduct,
                            pendingAmount = buyer.pendingAmount - (sale.totalNetAmount - sale.paidAmount),
                            lastUpdated = System.currentTimeMillis(),
                            isSynced = false
                        ))
                    }

                    saleDao.softDeleteSale(id)
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Delete Sale Failed")
                Resource.Error(e.message ?: "Delete failed")
            }
        }
    }

    override suspend fun addPaymentToSale(saleId: String, payment: PaymentEntity): Resource<Unit> {
        return try {
            val (updatedSale, updatedBuyer) = database.withTransaction {
                val sale = saleDao.getSaleById(saleId) ?: throw Exception("Sale not found")
                val buyer = buyerDao.getBuyerById(sale.buyerId) ?: throw Exception("Buyer not found")

                val updatedSale = sale.copy(
                    paidAmount = sale.paidAmount + payment.amount,
                    pendingAmount = sale.pendingAmount - payment.amount,
                    isSynced = false
                )
                saleDao.updateSale(updatedSale)

                val updatedBuyer = buyer.copy(
                    pendingAmount = buyer.pendingAmount - payment.amount,
                    lastUpdated = System.currentTimeMillis(),
                    isSynced = false
                )
                buyerDao.updateBuyer(updatedBuyer)
                paymentDao.insertPayment(payment.copy(remainingBalance = updatedBuyer.pendingAmount))
                
                updatedSale to updatedBuyer
            }

            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        batch.set(firestore.collection("users").document(uid).collection("sales").document(updatedSale.id), updatedSale)
                        batch.set(firestore.collection("users").document(uid).collection("buyers").document(updatedBuyer.id), updatedBuyer)
                        batch.set(firestore.collection("users").document(uid).collection("payments").document(payment.id), payment)
                        batch.commit().await()
                        
                        saleDao.markAsSynced(updatedSale.id)
                        buyerDao.markAsSynced(updatedBuyer.id)
                        paymentDao.markAsSynced(payment.id)
                    } catch (e: Exception) { }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add payment to sale")
        }
    }

    override suspend fun syncSales(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = saleDao.getUnsyncedSales()
            for (sale in unsynced) {
                val items = saleDao.getItemsBySaleId(sale.id)
                firestore.runTransaction { transaction ->
                    val saleRef = firestore.collection("users").document(uid).collection("sales").document(sale.id)
                    transaction.set(saleRef, sale)
                    for (item in items) { transaction.set(saleRef.collection("items").document(item.id), item) }
                }.await()
                saleDao.markAsSynced(sale.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sync failed")
        }
    }
}
