package com.dasariravi145.agrolynch.data.repository

import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.BuyerDao
import com.dasariravi145.agrolynch.data.local.dao.FarmerDao
import com.dasariravi145.agrolynch.data.local.dao.PaymentDao
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.domain.repository.PaymentRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PaymentRepository {

    private val paymentDao = database.paymentDao()
    private val farmerDao = database.farmerDao()
    private val buyerDao = database.buyerDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    override suspend fun addPayment(payment: PaymentEntity): Resource<Unit> {
        return try {
            if (payment.partyType == "FARMER") {
                val (updatedFarmer, updatedPayment) = database.withTransaction {
                    val farmer = farmerDao.getFarmerById(payment.partyId) ?: throw Exception("Farmer not found")
                    
                    val paymentAmount = payment.amount
                    var newPending = farmer.pendingAmount - paymentAmount
                    var newAdvance = farmer.advanceAmount
                    
                    if (newPending < 0) {
                        newAdvance += (-newPending)
                        newPending = 0.0
                    }
                    
                    val updatedFarmer = farmer.copy(
                        totalPayments = farmer.totalPayments + paymentAmount,
                        pendingAmount = newPending,
                        advanceAmount = newAdvance,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    
                    val updatedPayment = payment.copy(
                        remainingBalance = newPending,
                        advanceAmount = newAdvance
                    )
                    
                    paymentDao.insertPayment(updatedPayment)
                    farmerDao.updateFarmer(updatedFarmer)
                    updatedFarmer to updatedPayment
                }
                
                userId?.let { uid ->
                    repositoryScope.launch {
                        try {
                            val batch = firestore.batch()
                            batch.set(firestore.collection("users").document(uid).collection("payments").document(updatedPayment.id), updatedPayment)
                            batch.set(firestore.collection("users").document(uid).collection("farmers").document(updatedFarmer.id), updatedFarmer)
                            batch.commit().await()
                            
                            paymentDao.markAsSynced(updatedPayment.id)
                            farmerDao.markAsSynced(updatedFarmer.id)
                        } catch (e: Exception) {
                            // Synced later
                        }
                    }
                }
            } else {
                val updatedBuyer = database.withTransaction {
                    val buyer = buyerDao.getBuyerById(payment.partyId) ?: throw Exception("Buyer not found")
                    val updatedBuyer = buyer.copy(
                        totalPaid = buyer.totalPaid + payment.amount,
                        pendingAmount = buyer.pendingAmount - payment.amount,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    paymentDao.insertPayment(payment)
                    buyerDao.updateBuyer(updatedBuyer)
                    updatedBuyer
                }

                userId?.let { uid ->
                    repositoryScope.launch {
                        try {
                            val batch = firestore.batch()
                            batch.set(firestore.collection("users").document(uid).collection("payments").document(payment.id), payment)
                            batch.set(firestore.collection("users").document(uid).collection("buyers").document(updatedBuyer.id), updatedBuyer)
                            batch.commit().await()
                            paymentDao.markAsSynced(payment.id)
                            buyerDao.markAsSynced(updatedBuyer.id)
                        } catch (e: Exception) {}
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updatePayment(payment: PaymentEntity): Resource<Unit> {
        return try {
            val updated = payment.copy(isSynced = false, lastUpdated = System.currentTimeMillis())
            paymentDao.updatePayment(updated)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("payments").document(payment.id).set(updated).await()
                        paymentDao.markAsSynced(payment.id)
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun deletePayment(id: String): Resource<Unit> {
        return try {
            paymentDao.softDeletePayment(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("payments").document(id).update("isDeleted", true).await()
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    override suspend fun syncPayments(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = paymentDao.getUnsyncedPayments()
            for (payment in unsynced) {
                firestore.collection("users").document(uid).collection("payments").document(payment.id).set(payment).await()
                paymentDao.markAsSynced(payment.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
