package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.TransactionDao
import com.dasariravi145.agrolynch.data.local.entity.TransactionEntity
import com.dasariravi145.agrolynch.domain.repository.TransactionRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val farmerDao: com.dasariravi145.agrolynch.data.local.dao.FarmerDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getTransactions(): Flow<List<TransactionEntity>> {
        return dao.getAllTransactions()
    }

    override suspend fun addTransaction(transaction: TransactionEntity): Resource<Unit> {
        return try {
            // 1. Save Transaction
            dao.insertTransaction(transaction)

            timber.log.Timber.i("Manual/OCR Transaction Added: Farmer=%s, Product=%s, Amount=%f", 
                transaction.farmerName, transaction.productName, transaction.totalAmount)
            
            // 2. Update Farmer Balance if farmerId is present
            var updatedFarmer: com.dasariravi145.agrolynch.data.local.entity.FarmerEntity? = null
            if (transaction.farmerId.isNotEmpty()) {
                val farmer = farmerDao.getFarmerById(transaction.farmerId)
                if (farmer != null) {
                    updatedFarmer = farmer.copy(
                        totalArrivals = farmer.totalArrivals + transaction.totalAmount,
                        pendingAmount = farmer.pendingAmount + transaction.totalAmount,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    farmerDao.updateFarmer(updatedFarmer)
                }
            }

            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        val transRef = firestore.collection("users").document(uid).collection("transactions")
                            .document(transaction.id)
                        batch.set(transRef, transaction)
                        
                        if (updatedFarmer != null) {
                            val farmerRef = firestore.collection("users").document(uid).collection("farmers")
                                .document(updatedFarmer.id)
                            batch.set(farmerRef, updatedFarmer)
                        }
                        
                        batch.commit().await()
                        dao.markAsSynced(transaction.id)
                        if (updatedFarmer != null) {
                            farmerDao.markAsSynced(updatedFarmer.id)
                        }
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity): Resource<Unit> {
        return try {
            val oldTransaction = dao.getTransactionById(transaction.id)
            val updated = transaction.copy(isSynced = false, lastUpdated = System.currentTimeMillis())
            dao.updateTransaction(updated)
            
            // Adjust Farmer Balance
            var updatedFarmer: com.dasariravi145.agrolynch.data.local.entity.FarmerEntity? = null
            if (transaction.farmerId.isNotEmpty()) {
                val farmer = farmerDao.getFarmerById(transaction.farmerId)
                if (farmer != null && oldTransaction != null) {
                    val balanceDiff = transaction.totalAmount - oldTransaction.totalAmount
                    updatedFarmer = farmer.copy(
                        totalArrivals = farmer.totalArrivals + balanceDiff,
                        pendingAmount = farmer.pendingAmount + balanceDiff,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    farmerDao.updateFarmer(updatedFarmer)
                }
            }

            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        batch.set(firestore.collection("users").document(uid).collection("transactions").document(transaction.id), updated)
                        if (updatedFarmer != null) {
                            batch.set(firestore.collection("users").document(uid).collection("farmers").document(updatedFarmer.id), updatedFarmer)
                        }
                        batch.commit().await()
                        dao.markAsSynced(transaction.id)
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Update failed")
        }
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> {
        return try {
            val transaction = dao.getTransactionById(id)
                ?: return Resource.Error("Transaction not found")
            
            dao.softDeleteTransaction(id)
            
            // Reverse Farmer Balance
            var updatedFarmer: com.dasariravi145.agrolynch.data.local.entity.FarmerEntity? = null
            if (transaction.farmerId.isNotEmpty()) {
                val farmer = farmerDao.getFarmerById(transaction.farmerId)
                if (farmer != null) {
                    val amountToReverse = transaction.totalAmount
                    var newPending = farmer.pendingAmount - amountToReverse
                    var newAdvance = farmer.advanceAmount
                    
                    if (newPending < 0) {
                        newAdvance += (-newPending)
                        newPending = 0.0
                    }
                    
                    updatedFarmer = farmer.copy(
                        totalArrivals = farmer.totalArrivals - amountToReverse,
                        pendingAmount = newPending,
                        advanceAmount = newAdvance,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    farmerDao.updateFarmer(updatedFarmer)
                }
            }

            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        batch.update(firestore.collection("users").document(uid).collection("transactions").document(id), "isDeleted", true)
                        if (updatedFarmer != null) {
                            batch.set(firestore.collection("users").document(uid).collection("farmers").document(updatedFarmer.id), updatedFarmer)
                        }
                        batch.commit().await()
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Delete failed")
        }
    }

    override suspend fun syncTransactions(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = dao.getUnsyncedTransactions()
            unsynced.forEach { transaction ->
                firestore.collection("users").document(uid).collection("transactions")
                    .document(transaction.id)
                    .set(transaction)
                    .await()
                dao.markAsSynced(transaction.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sync failed")
        }
    }
}
