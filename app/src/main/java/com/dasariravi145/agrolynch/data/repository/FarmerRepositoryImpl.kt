package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.FarmerDao
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.domain.repository.FarmerRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class FarmerRepositoryImpl @Inject constructor(
    private val farmerDao: FarmerDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FarmerRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getFarmers(): Flow<List<FarmerEntity>> = farmerDao.getAllFarmers()

    override suspend fun getFarmerById(id: String): FarmerEntity? = farmerDao.getFarmerById(id)

    override suspend fun addFarmer(farmer: FarmerEntity): Resource<Unit> {
        return try {
            farmerDao.insertFarmer(farmer)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val firestoreData = mapOf(
                            "farmerId" to farmer.id,
                            "ownerUserId" to uid,
                            "name" to farmer.name,
                            "phone" to farmer.mobileNumber,
                            "village" to farmer.village,
                            "totalArrivals" to farmer.totalArrivals,
                            "totalPayments" to farmer.totalPayments,
                            "pendingAmount" to farmer.pendingAmount,
                            "advanceAmount" to farmer.advanceAmount,
                            "lastUpdated" to farmer.lastUpdated,
                            "isDeleted" to farmer.isDeleted
                        )
                        firestore.collection("users").document(uid).collection("farmers").document(farmer.id).set(firestoreData).await()
                        farmerDao.markAsSynced(farmer.id)
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

    override suspend fun updateFarmer(farmer: FarmerEntity): Resource<Unit> {
        return try {
            val updatedFarmer = farmer.copy(isSynced = false, lastUpdated = System.currentTimeMillis())
            farmerDao.updateFarmer(updatedFarmer)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val firestoreData = updatedFarmer.javaClass.declaredFields.associate { field ->
                            field.isAccessible = true
                            field.name to field.get(updatedFarmer)
                        }.toMutableMap()
                        firestoreData["ownerUserId"] = uid
                        firestore.collection("users").document(uid).collection("farmers").document(farmer.id).set(firestoreData).await()
                        farmerDao.markAsSynced(farmer.id)
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

    override suspend fun deleteFarmer(id: String): Resource<Unit> {
        return try {
            farmerDao.softDeleteFarmer(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("farmers").document(id).update("isDeleted", true).await()
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

    override suspend fun syncFarmers(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsyncedFarmers = farmerDao.getUnsyncedFarmers()
            for (farmer in unsyncedFarmers) {
                val firestoreData = farmer.javaClass.declaredFields.associate { field ->
                    field.isAccessible = true
                    field.name to field.get(farmer)
                }.toMutableMap()
                firestoreData["ownerUserId"] = uid
                firestore.collection("users").document(uid).collection("farmers").document(farmer.id).set(firestoreData).await()
                farmerDao.markAsSynced(farmer.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateFarmerBalance(farmerId: String, amountChange: Double, isArrival: Boolean): Resource<Unit> {
        return try {
            val farmer = farmerDao.getFarmerById(farmerId) ?: return Resource.Error("Farmer not found")
            
            val updatedFarmer = if (isArrival) {
                // Arrival increases the amount we OWE the farmer (Pending)
                val newTotalArrivals = farmer.totalArrivals + amountChange
                val newPending = farmer.pendingAmount + amountChange
                
                // Advance adjustment if any
                var finalPending = newPending
                var finalAdvance = farmer.advanceAmount
                
                if (finalAdvance > 0) {
                    if (finalAdvance >= finalPending) {
                        finalAdvance -= finalPending
                        finalPending = 0.0
                    } else {
                        finalPending -= finalAdvance
                        finalAdvance = 0.0
                    }
                }
                
                farmer.copy(
                    totalArrivals = newTotalArrivals,
                    pendingAmount = finalPending,
                    advanceAmount = finalAdvance,
                    lastUpdated = System.currentTimeMillis(),
                    isSynced = false
                )
            } else {
                // Payment decreases the amount we OWE (Pending) or increases Advance
                val newTotalPayments = farmer.totalPayments + amountChange
                var newPending = farmer.pendingAmount - amountChange
                var newAdvance = farmer.advanceAmount
                
                if (newPending < 0) {
                    newAdvance += (-newPending)
                    newPending = 0.0
                }
                
                farmer.copy(
                    totalPayments = newTotalPayments,
                    pendingAmount = newPending,
                    advanceAmount = newAdvance,
                    lastUpdated = System.currentTimeMillis(),
                    isSynced = false
                )
            }
            
            updateFarmer(updatedFarmer)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Balance update failed")
        }
    }
}
