package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.BuyerDao
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.domain.repository.BuyerRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class BuyerRepositoryImpl @Inject constructor(
    private val buyerDao: BuyerDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BuyerRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getBuyers(): Flow<List<BuyerEntity>> = buyerDao.getAllBuyers()

    override suspend fun getBuyerById(id: String): BuyerEntity? = buyerDao.getBuyerById(id)

    override suspend fun addBuyer(buyer: BuyerEntity): Resource<Unit> {
        return try {
            buyerDao.insertBuyer(buyer)
            // Fire and forget sync to Firestore
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("buyers").document(buyer.id).set(buyer).await()
                        buyerDao.markAsSynced(buyer.id)
                    } catch (e: Exception) {
                        // Will be synced later by Worker
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateBuyer(buyer: BuyerEntity): Resource<Unit> {
        return try {
            val updatedBuyer = buyer.copy(isSynced = false, lastUpdated = System.currentTimeMillis())
            buyerDao.updateBuyer(updatedBuyer)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("buyers").document(buyer.id).set(updatedBuyer).await()
                        buyerDao.markAsSynced(buyer.id)
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

    override suspend fun deleteBuyer(id: String): Resource<Unit> {
        return try {
            buyerDao.softDeleteBuyer(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("buyers").document(id).update("isDeleted", true).await()
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

    override suspend fun syncBuyers(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsyncedBuyers = buyerDao.getUnsyncedBuyers()
            for (buyer in unsyncedBuyers) {
                firestore.collection("users").document(uid).collection("buyers").document(buyer.id).set(buyer).await()
                buyerDao.markAsSynced(buyer.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
