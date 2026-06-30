package com.dasariravi145.agrolynch.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.ArrivalDao
import com.dasariravi145.agrolynch.data.local.dao.FarmerDao
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.ArrivalRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class ArrivalRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val exportService: com.dasariravi145.agrolynch.util.LedgerExportService,
    @ApplicationContext private val context: Context
) : ArrivalRepository {

    private val arrivalDao = database.arrivalDao()
    private val farmerDao = database.farmerDao()
    private val profileDao = database.companyProfileDao()
    private val boxWeightDao = database.boxWeightDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getArrivals(): Flow<List<ArrivalEntity>> = arrivalDao.getAllArrivals()

    override fun getAvailableStockByProduct(productId: String): Flow<List<ArrivalEntity>> = 
        arrivalDao.getAvailableStockByProduct(productId)

    override fun getAvailableStockByProductAndGrade(productId: String, grade: String): Flow<List<ArrivalEntity>> =
        arrivalDao.getAvailableStockByProductAndGrade(productId, grade)

    override fun getFarmersWithStock(): Flow<List<com.dasariravi145.agrolynch.data.local.dao.FarmerStockInfo>> =
        arrivalDao.getFarmersWithStock()

    override fun getAvailableStockByFarmer(farmerId: String): Flow<List<ArrivalEntity>> =
        arrivalDao.getAvailableStockByFarmer(farmerId)

    override suspend fun getArrivalById(id: String): ArrivalEntity? = arrivalDao.getArrivalById(id)

    override suspend fun addArrival(arrival: ArrivalEntity): Resource<Unit> {
        return addArrivalBatch(listOf(arrival))
    }

    override suspend fun addArrivalBatch(
        arrivals: List<ArrivalEntity>,
        boxWeights: List<com.dasariravi145.agrolynch.data.local.entity.BoxWeightItemEntity>
    ): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (arrivals.isEmpty()) return@withContext Resource.Error("No data to save")
                
                val startTime = System.currentTimeMillis()
                val (updatedFarmer, profile) = database.withTransaction {
                    val farmerId = arrivals.first().farmerId
                    val farmer = farmerDao.getFarmerById(farmerId) ?: throw Exception("Farmer not found")
                    val profile = profileDao.getProfile().first() ?: CompanyProfileEntity()
                    
                    var totalNetToPay = 0.0
                    arrivals.forEach {
                        arrivalDao.insertArrival(it)
                        totalNetToPay += it.netAmount
                    }

                    if (boxWeights.isNotEmpty()) {
                        boxWeightDao.insertBoxWeights(boxWeights)
                    }
                    
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
                    
                    val updated = farmer.copy(
                        totalArrivals = farmer.totalArrivals + arrivals.sumOf { it.grossAmount },
                        pendingAmount = newPending,
                        advanceAmount = newAdvance,
                        lastUpdated = System.currentTimeMillis(),
                        isSynced = false
                    )
                    farmerDao.updateFarmer(updated)
                    profileDao.incrementBillNumber()
                    timber.log.Timber.tag("BillRef").d("Saved ledger transaction id=${arrivals.first().id} billNumber=${arrivals.first().billNumber} billId=${arrivals.first().billNumber} referenceId=${arrivals.first().id}")
                    updated to profile
                }
                
                val dbTime = System.currentTimeMillis() - startTime
                timber.log.Timber.i("Arrival Save: DB transaction took %dms", dbTime)

                // Async Background Tasks
                repositoryScope.launch {
                    val backgroundStart = System.currentTimeMillis()
                    
                    // Generate PDF with Branding in background
                    try {
                        val firstArrival = arrivals.first()
                        val farmer = farmerDao.getFarmerById(firstArrival.farmerId)
                        exportService.exportArrivalToPdf(
                            context = context,
                            profile = profile,
                            arrivals = arrivals,
                            deductions = emptyList(),
                            farmerMobile = farmer?.mobileNumber ?: ""
                        )
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error generating PDF in background")
                    }
                    
                    // Background sync
                    userId?.let { uid ->
                        try {
                            val batch = firestore.batch()
                            val arrivalsRef = firestore.collection("users").document(uid).collection("arrivals")
                            val farmersRef = firestore.collection("users").document(uid).collection("farmers")

                            arrivals.forEach { arrival ->
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
                                batch.set(arrivalsRef.document(arrival.id), arrivalMap)
                            }
                            
                            // Map updatedFarmer explicitly
                            val farmerMap = mapOf(
                                "id" to updatedFarmer.id,
                                "name" to updatedFarmer.name,
                                "mobileNumber" to updatedFarmer.mobileNumber,
                                "pendingAmount" to updatedFarmer.pendingAmount,
                                "advanceAmount" to updatedFarmer.advanceAmount,
                                "totalArrivals" to updatedFarmer.totalArrivals,
                                "lastUpdated" to updatedFarmer.lastUpdated,
                                "ownerUserId" to uid
                            )
                            batch.set(farmersRef.document(updatedFarmer.id), farmerMap)

                            batch.commit().await()
                            
                            arrivals.forEach { arrivalDao.markAsSynced(it.id) }
                            farmerDao.markAsSynced(updatedFarmer.id)
                        } catch (e: Exception) { 
                            timber.log.Timber.e(e, "Firebase sync failed")
                        }
                    }
                    
                    val bgTime = System.currentTimeMillis() - backgroundStart
                    timber.log.Timber.i("Arrival Save: Background tasks took %dms", bgTime)
                }
                
                Resource.Success(Unit)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Add Arrival Batch Failed")
                Resource.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    override suspend fun updateArrival(arrival: ArrivalEntity): Resource<Unit> {
        return try {
            arrivalDao.updateArrival(arrival)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        val firestoreData = arrival.javaClass.declaredFields.associate { field ->
                            field.isAccessible = true
                            field.name to field.get(arrival)
                        }.toMutableMap()
                        firestoreData["ownerUserId"] = uid
                        firestore.collection("users").document(uid).collection("arrivals").document(arrival.id).set(firestoreData).await()
                        arrivalDao.markAsSynced(arrival.id)
                    } catch (e: Exception) { }
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
            
            val amountToReverse = arrival.netAmount
            var newPending = farmer.pendingAmount - amountToReverse
            var newAdvance = farmer.advanceAmount
            
            if (newPending < 0) {
                newAdvance += (-newPending)
                newPending = 0.0
            }
            
            val updatedFarmer = farmer.copy(
                totalArrivals = farmer.totalArrivals - arrival.grossAmount,
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
                        
                        val farmerData = updatedFarmer.javaClass.declaredFields.associate { field ->
                            field.isAccessible = true
                            field.name to field.get(updatedFarmer)
                        }.toMutableMap()
                        farmerData["ownerUserId"] = uid
                        batch.set(firestore.collection("users").document(uid).collection("farmers").document(farmer.id), farmerData)
                        batch.commit().await()
                    } catch (e: Exception) { }
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
                val firestoreData = arrival.javaClass.declaredFields.associate { field ->
                    field.isAccessible = true
                    field.name to field.get(arrival)
                }.toMutableMap()
                firestoreData["ownerUserId"] = uid
                firestore.collection("users").document(uid).collection("arrivals").document(arrival.id).set(firestoreData).await()
                arrivalDao.markAsSynced(arrival.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
