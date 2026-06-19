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
    private val paymentDao = database.paymentDao()
    private val profileDao = database.companyProfileDao()

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
                    saleDao.insertSaleItems(items)

                    for (item in items) {
                        val arrival = arrivalDao.getArrivalById(item.arrivalId) ?: throw Exception("Stock item not found")
                        val reductionAmount = if (arrival.unit == "Ton" || arrival.unit == "Boxes") {
                            item.quantitySold / 1000.0 // Convert KG sold to Tons for database reduction
                        } else {
                            item.quantitySold
                        }
                        arrivalDao.reduceStock(item.arrivalId, reductionAmount)
                    }

                    val totalNetReceivable = sale.totalNetAmount
                    val updatedBuyer = buyer.copy(
                        totalPurchase = buyer.totalPurchase + totalNetReceivable,
                        pendingAmount = buyer.pendingAmount + totalNetReceivable,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    buyerDao.updateBuyer(updatedBuyer)
                    profileDao.incrementInvoiceNumber()
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
                            val batch = firestore.batch()
                            
                            // Convert sale to map and add ownerUserId
                            val saleMap = sale.javaClass.declaredFields.associate { field ->
                                field.isAccessible = true
                                field.name to field.get(sale)
                            }.toMutableMap()
                            saleMap["ownerUserId"] = uid
                            
                            batch.set(firestore.collection("users").document(uid).collection("sales").document(sale.id), saleMap)
                            
                            // Convert updatedBuyer to map and add ownerUserId
                            val buyerMap = updatedBuyer.javaClass.declaredFields.associate { field ->
                                field.isAccessible = true
                                field.name to field.get(updatedBuyer)
                            }.toMutableMap()
                            buyerMap["ownerUserId"] = uid

                            batch.set(firestore.collection("users").document(uid).collection("buyers").document(updatedBuyer.id), buyerMap)
                            
                            for (item in items) {
                                val itemMap = item.javaClass.declaredFields.associate { field ->
                                    field.isAccessible = true
                                    field.name to field.get(item)
                                }.toMutableMap()
                                itemMap["ownerUserId"] = uid
                                batch.set(firestore.collection("users").document(uid).collection("sales").document(sale.id).collection("items").document(item.id), itemMap)
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

    override suspend fun updateSale(sale: SaleEntity): Resource<Unit> {
        return try {
            saleDao.updateSale(sale.copy(isSynced = false))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Update failed")
        }
    }

    override suspend fun deleteSale(id: String): Resource<Unit> {
        return try {
            saleDao.softDeleteSale(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Delete failed")
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
