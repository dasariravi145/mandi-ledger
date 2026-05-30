package com.dasariravi145.agrolynch.data.repository

import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.ArrivalDao
import com.dasariravi145.agrolynch.data.local.dao.BuyerDao
import com.dasariravi145.agrolynch.data.local.dao.PaymentDao
import com.dasariravi145.agrolynch.data.local.dao.SaleDao
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity
import com.dasariravi145.agrolynch.domain.repository.SaleRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SaleRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SaleRepository {

    private val saleDao = database.saleDao()
    private val arrivalDao = database.arrivalDao()
    private val buyerDao = database.buyerDao()
    private val paymentDao = database.paymentDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()

    override suspend fun createSale(sale: SaleEntity, items: List<SaleItemEntity>): Resource<Unit> {
        return try {
            val updatedBuyer = database.withTransaction {
                // 1. Get Buyer
                val buyer = buyerDao.getBuyerById(sale.buyerId) ?: throw Exception("Buyer not found")

                // 2. Save Sale and SaleItems to Local DB
                saleDao.insertSale(sale)
                saleDao.insertSaleItems(items)

                // 3. Update Stocks
                for (item in items) {
                    arrivalDao.reduceStock(item.arrivalId, item.quantitySold)
                }

                // 4. Update Buyer Khata
                val updatedBuyer = buyer.copy(
                    totalPurchase = buyer.totalPurchase + sale.totalAmount,
                    pendingAmount = buyer.pendingAmount + sale.pendingAmount,
                    lastUpdated = System.currentTimeMillis(),
                    isSynced = false
                )
                buyerDao.updateBuyer(updatedBuyer)
                updatedBuyer
            }

            // 5. Sync to Firebase
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        val saleRef = firestore.collection("users").document(uid).collection("sales").document(sale.id)
                        batch.set(saleRef, sale)

                        batch.set(firestore.collection("users").document(uid).collection("buyers").document(updatedBuyer.id), updatedBuyer)

                        for (item in items) {
                            val itemRef = firestore.collection("users").document(uid).collection("sales").document(sale.id)
                                .collection("items").document(item.id)
                            batch.set(itemRef, item)
                            
                            val arrival = arrivalDao.getArrivalById(item.arrivalId)
                            if (arrival != null) {
                                val arrivalRef = firestore.collection("users").document(uid).collection("arrivals").document(arrival.id)
                                batch.set(arrivalRef, arrival)
                            }
                        }
                        batch.commit().await()
                        saleDao.markAsSynced(sale.id)
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateSale(sale: SaleEntity): Resource<Unit> {
        return try {
            saleDao.updateSale(sale.copy(isSynced = false))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun deleteSale(id: String): Resource<Unit> {
        return try {
            saleDao.softDeleteSale(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    override suspend fun addPaymentToSale(saleId: String, payment: PaymentEntity): Resource<Unit> {
        return try {
            val (updatedSale, updatedBuyer) = database.withTransaction {
                val sale = saleDao.getSaleById(saleId) ?: throw Exception("Sale not found")
                val buyer = buyerDao.getBuyerById(sale.buyerId) ?: throw Exception("Buyer not found")

                // 1. Update Sale
                val updatedSale = sale.copy(
                    paidAmount = sale.paidAmount + payment.amount,
                    pendingAmount = sale.pendingAmount - payment.amount,
                    isSynced = false
                )
                saleDao.updateSale(updatedSale)

                // 2. Update Buyer
                val updatedBuyer = buyer.copy(
                    pendingAmount = buyer.pendingAmount - payment.amount,
                    lastUpdated = System.currentTimeMillis(),
                    isSynced = false
                )
                buyerDao.updateBuyer(updatedBuyer)

                // 3. Save Payment
                paymentDao.insertPayment(payment.copy(remainingBalance = updatedBuyer.pendingAmount))
                
                updatedSale to updatedBuyer
            }

            // 4. Sync to Firebase
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
                    } catch (e: Exception) {
                        // Will be synced later
                    }
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
                    for (item in items) {
                        val itemRef = saleRef.collection("items").document(item.id)
                        transaction.set(itemRef, item)
                    }
                }.await()
                saleDao.markAsSynced(sale.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sync failed")
        }
    }
}
