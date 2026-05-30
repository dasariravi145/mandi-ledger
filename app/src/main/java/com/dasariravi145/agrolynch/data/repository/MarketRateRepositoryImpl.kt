package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.MarketRateDao
import com.dasariravi145.agrolynch.data.local.entity.MarketRateEntity
import com.dasariravi145.agrolynch.domain.repository.MarketRateRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MarketRateRepositoryImpl @Inject constructor(
    private val marketRateDao: MarketRateDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : MarketRateRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getRatesByDate(date: Long): Flow<List<MarketRateEntity>> = marketRateDao.getRatesByDate(date)

    override fun getHistoricalRates(productId: String): Flow<List<MarketRateEntity>> = marketRateDao.getHistoricalRates(productId)

    override suspend fun saveRate(rate: MarketRateEntity): Resource<Unit> {
        return try {
            marketRateDao.insertRate(rate)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("market_rates").document(rate.id).set(rate).await()
                        marketRateDao.markAsSynced(rate.id)
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

    override suspend fun updateRate(rate: MarketRateEntity): Resource<Unit> {
        return try {
            val updated = rate.copy(isSynced = false, lastUpdated = System.currentTimeMillis())
            marketRateDao.updateRate(updated)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("market_rates").document(rate.id).set(updated).await()
                        marketRateDao.markAsSynced(rate.id)
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

    override suspend fun deleteRate(id: String): Resource<Unit> {
        return try {
            marketRateDao.softDeleteRate(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("market_rates").document(id).update("isDeleted", true).await()
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

    override suspend fun syncRates(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = marketRateDao.getUnsyncedRates()
            for (rate in unsynced) {
                firestore.collection("users").document(uid).collection("market_rates").document(rate.id).set(rate).await()
                marketRateDao.markAsSynced(rate.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
