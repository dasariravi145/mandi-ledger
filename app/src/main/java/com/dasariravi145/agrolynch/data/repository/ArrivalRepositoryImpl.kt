package com.dasariravi145.agrolynch.data.repository

import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.ArrivalDao
import com.dasariravi145.agrolynch.data.local.dao.FarmerDao
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.domain.repository.ArrivalRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ArrivalRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ArrivalRepository {

    private val arrivalDao = database.arrivalDao()
    private val farmerDao = database.farmerDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getArrivals(): Flow<List<ArrivalEntity>> = arrivalDao.getAllArrivals()

    override fun getAvailableStockByProduct(productId: String): Flow<List<ArrivalEntity>> = 
        arrivalDao.getAvailableStockByProduct(productId)

    override fun getAvailableStockByProductAndGrade(productId: String, grade: String): Flow<List<ArrivalEntity>> =
        arrivalDao.getAvailableStockByProductAndGrade(productId, grade)

    override suspend fun getArrivalById(id: String): ArrivalEntity? = arrivalDao.getArrivalById(id)

    override suspend fun addArrival(arrival: ArrivalEntity): Resource<Unit> {
        return try {
            val (arrivalToInsert, updatedFarmer) = database.withTransaction {
                // 1. Get Farmer
                val farmer = farmerDao.getFarmerById(arrival.farmerId) ?: throw Exception("Farmer not found")
                
                // 2. Calculate Balance Update
                val amountChange = arrival.netAmount
                val newTotalArrivals = farmer.totalArrivals + amountChange
                var newPending = farmer.pendingAmount + amountChange
                var newAdvance = farmer.advanceAmount
                
                // Adjust advance if exists
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
                    totalArrivals = newTotalArrivals,
                    pendingAmount = newPending,
                    advanceAmount = newAdvance,
                    lastUpdated = System.currentTimeMillis(),
                    isSynced = false
                )

                // 3. Update both atomically
                val toInsert = arrival.copy(
                    remainingQuantity = arrival.quantity,
                    farmerPendingAmount = newPending
                )
                
                arrivalDao.insertArrival(toInsert)
                farmerDao.updateFarmer(updatedFarmer)
                toInsert to updatedFarmer
            }
            
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        batch.set(firestore.collection("users").document(uid).collection("arrivals").document(arrival.id), arrivalToInsert)
                        batch.set(firestore.collection("users").document(uid).collection("farmers").document(updatedFarmer.id), updatedFarmer)
                        batch.commit().await()
                        
                        arrivalDao.markAsSynced(arrival.id)
                        farmerDao.markAsSynced(updatedFarmer.id)
                    } catch (e: Exception) {
                        // Synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateArrival(arrival: ArrivalEntity): Resource<Unit> {
        return try {
            arrivalDao.updateArrival(arrival)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("arrivals").document(arrival.id).set(arrival).await()
                        arrivalDao.markAsSynced(arrival.id)
                    } catch (e: Exception) {
                        // Synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun deleteArrival(id: String): Resource<Unit> {
        return try {
            val arrival = arrivalDao.getArrivalById(id) ?: return Resource.Error("Arrival not found")
            val farmer = farmerDao.getFarmerById(arrival.farmerId) ?: return Resource.Error("Farmer not found")
            
            // Reverse balance
            val amountToReverse = arrival.netAmount
            var newPending = farmer.pendingAmount - amountToReverse
            var newAdvance = farmer.advanceAmount
            
            if (newPending < 0) {
                newAdvance += (-newPending)
                newPending = 0.0
            }
            
            val updatedFarmer = farmer.copy(
                totalArrivals = farmer.totalArrivals - amountToReverse,
                pendingAmount = newPending,
                advanceAmount = newAdvance,
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
            
            arrivalDao.softDeleteArrival(id)
            farmerDao.updateFarmer(updatedFarmer)
            
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val batch = firestore.batch()
                        batch.update(firestore.collection("users").document(uid).collection("arrivals").document(id), "isDeleted", true)
                        batch.set(firestore.collection("users").document(uid).collection("farmers").document(farmer.id), updatedFarmer)
                        batch.commit().await()
                    } catch (e: Exception) {
                        // Synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun syncArrivals(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = arrivalDao.getUnsyncedArrivals()
            for (arrival in unsynced) {
                firestore.collection("users").document(uid).collection("arrivals").document(arrival.id).set(arrival).await()
                arrivalDao.markAsSynced(arrival.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
