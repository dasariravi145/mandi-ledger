package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface ArrivalRepository {
    fun getArrivals(): Flow<List<ArrivalEntity>>
    suspend fun getArrivalById(id: String): ArrivalEntity?
    suspend fun addArrival(arrival: ArrivalEntity): Resource<Unit>
    suspend fun addArrivalBatch(arrivals: List<ArrivalEntity>): Resource<Unit>
    fun getAvailableStockByProduct(productId: String): Flow<List<ArrivalEntity>>
    fun getAvailableStockByProductAndGrade(productId: String, grade: String): Flow<List<ArrivalEntity>>
    fun getFarmersWithStock(): Flow<List<com.dasariravi145.agrolynch.data.local.dao.FarmerStockInfo>>
    fun getAvailableStockByFarmer(farmerId: String): Flow<List<ArrivalEntity>>
    suspend fun updateArrival(arrival: ArrivalEntity): Resource<Unit>
    suspend fun deleteArrival(id: String): Resource<Unit>
    suspend fun syncArrivals(): Resource<Unit>
}
