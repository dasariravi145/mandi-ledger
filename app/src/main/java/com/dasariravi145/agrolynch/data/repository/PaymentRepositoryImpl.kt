package com.dasariravi145.agrolynch.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.BuyerDao
import com.dasariravi145.agrolynch.data.local.dao.FarmerDao
import com.dasariravi145.agrolynch.data.local.dao.PaymentDao
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.domain.repository.PaymentRepository
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val pdfService: TemplateInvoicePdfService,
    @ApplicationContext private val context: Context
) : PaymentRepository {

    private val paymentDao = database.paymentDao()
    private val farmerDao = database.farmerDao()
    private val buyerDao = database.buyerDao()
    private val profileDao = database.companyProfileDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    override suspend fun addPayment(payment: PaymentEntity): Resource<Unit> {
        return try {
            val (finalPayment, profile) = if (payment.partyType == "FARMER") {
                val (updatedFarmer, updatedPayment, profile) = database.withTransaction {
                    val farmer = farmerDao.getFarmerById(payment.partyId) ?: throw Exception("Farmer not found")
                    val profile = profileDao.getProfile().first() ?: CompanyProfileEntity()
                    
                    var newPending = farmer.pendingAmount - payment.amount
                    var newAdvance = farmer.advanceAmount
                    
                    if (newPending < 0) {
                        newAdvance += (-newPending)
                        newPending = 0.0
                    }
                    
                    val updatedFarmer = farmer.copy(
                        totalPayments = farmer.totalPayments + payment.amount,
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
                    profileDao.incrementReceiptNumber()
                    timber.log.Timber.tag("BillRef").d("Saved ledger transaction id=${updatedPayment.id} billNumber=${updatedPayment.billNumber} billId=${updatedPayment.billNumber} referenceId=${updatedPayment.id}")
                    Triple(updatedFarmer, updatedPayment, profile)
                }
                
                userId?.let { uid ->
                    repositoryScope.launch {
                        try {
                            val batch = firestore.batch()
                            
                            val paymentMap = mapOf(
                                "id" to updatedPayment.id,
                                "partyId" to updatedPayment.partyId,
                                "partyName" to updatedPayment.partyName,
                                "partyType" to updatedPayment.partyType,
                                "amount" to updatedPayment.amount,
                                "paymentMode" to updatedPayment.paymentMode,
                                "billNumber" to updatedPayment.billNumber,
                                "remainingBalance" to updatedPayment.remainingBalance,
                                "advanceAmount" to updatedPayment.advanceAmount,
                                "date" to updatedPayment.date,
                                "ownerUserId" to uid
                            )
                            
                            val farmerMap = mapOf(
                                "id" to updatedFarmer.id,
                                "name" to updatedFarmer.name,
                                "mobileNumber" to updatedFarmer.mobileNumber,
                                "pendingAmount" to updatedFarmer.pendingAmount,
                                "advanceAmount" to updatedFarmer.advanceAmount,
                                "totalPayments" to updatedFarmer.totalPayments,
                                "lastUpdated" to updatedFarmer.lastUpdated,
                                "ownerUserId" to uid
                            )

                            batch.set(firestore.collection("users").document(uid).collection("payments").document(updatedPayment.id), paymentMap)
                            batch.set(firestore.collection("users").document(uid).collection("farmers").document(updatedFarmer.id), farmerMap)
                            batch.commit().await()
                            paymentDao.markAsSynced(updatedPayment.id)
                            farmerDao.markAsSynced(updatedFarmer.id)
                        } catch (e: Exception) { 
                            timber.log.Timber.e(e, "Firebase sync failed for Payment")
                        }
                    }
                }
                updatedPayment to profile
            } else {
                val (updatedBuyer, updatedPayment, profile) = database.withTransaction {
                    val buyer = buyerDao.getBuyerById(payment.partyId) ?: throw Exception("Buyer not found")
                    val profile = profileDao.getProfile().first() ?: CompanyProfileEntity()
                    val updatedBuyer = buyer.copy(
                        totalPaid = buyer.totalPaid + payment.amount,
                        pendingAmount = buyer.pendingAmount - payment.amount,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    val updatedPayment = payment.copy(remainingBalance = updatedBuyer.pendingAmount)
                    paymentDao.insertPayment(updatedPayment)
                    buyerDao.updateBuyer(updatedBuyer)
                    profileDao.incrementReceiptNumber()
                    timber.log.Timber.tag("BillRef").d("Saved ledger transaction id=${updatedPayment.id} billNumber=${updatedPayment.billNumber} billId=${updatedPayment.billNumber} referenceId=${updatedPayment.id}")
                    Triple(updatedBuyer, updatedPayment, profile)
                }

                userId?.let { uid ->
                    repositoryScope.launch {
                        try {
                            val batch = firestore.batch()
                            
                            val paymentMap = mapOf(
                                "id" to updatedPayment.id,
                                "partyId" to updatedPayment.partyId,
                                "partyName" to updatedPayment.partyName,
                                "partyType" to updatedPayment.partyType,
                                "amount" to updatedPayment.amount,
                                "paymentMode" to updatedPayment.paymentMode,
                                "billNumber" to updatedPayment.billNumber,
                                "remainingBalance" to updatedPayment.remainingBalance,
                                "date" to updatedPayment.date,
                                "ownerUserId" to uid
                            )
                            
                            val buyerMap = mapOf(
                                "id" to updatedBuyer.id,
                                "name" to updatedBuyer.name,
                                "mobileNumber" to updatedBuyer.mobileNumber,
                                "pendingAmount" to updatedBuyer.pendingAmount,
                                "totalPaid" to updatedBuyer.totalPaid,
                                "lastUpdated" to updatedBuyer.lastUpdated,
                                "ownerUserId" to uid
                            )

                            batch.set(firestore.collection("users").document(uid).collection("payments").document(updatedPayment.id), paymentMap)
                            batch.set(firestore.collection("users").document(uid).collection("buyers").document(updatedBuyer.id), buyerMap)
                            batch.commit().await()
                            paymentDao.markAsSynced(updatedPayment.id)
                            buyerDao.markAsSynced(updatedBuyer.id)
                        } catch (e: Exception) {}
                    }
                }
                updatedPayment to profile
            }

            // Generate Payment PDF with Branding
            pdfService.generatePaymentReceiptPdf(context, profile, finalPayment, finalPayment.partyType == "Farmer")

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
                        val firestoreData = updated.javaClass.declaredFields.associate { field ->
                            field.isAccessible = true
                            field.name to field.get(updated)
                        }.toMutableMap()
                        firestoreData["ownerUserId"] = uid
                        firestore.collection("users").document(uid).collection("payments").document(payment.id).set(firestoreData).await()
                        paymentDao.markAsSynced(payment.id)
                    } catch (e: Exception) { }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Update failed")
        }
    }

    override suspend fun deletePayment(id: String): Resource<Unit> {
        return try {
            paymentDao.softDeletePayment(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("payments").document(id).update("isDeleted", true).await()
                    } catch (e: Exception) { }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Delete failed")
        }
    }

    override suspend fun syncPayments(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = paymentDao.getUnsyncedPayments()
            for (payment in unsynced) {
                val firestoreData = payment.javaClass.declaredFields.associate { field ->
                    field.isAccessible = true
                    field.name to field.get(payment)
                }.toMutableMap()
                firestoreData["ownerUserId"] = uid
                firestore.collection("users").document(uid).collection("payments").document(payment.id).set(firestoreData).await()
                paymentDao.markAsSynced(payment.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
