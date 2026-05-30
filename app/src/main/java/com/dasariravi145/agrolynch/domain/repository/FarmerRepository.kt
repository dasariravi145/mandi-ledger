package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface FarmerRepository {
    fun getFarmers(): Flow<List<FarmerEntity>>
    suspend fun getFarmerById(id: String): FarmerEntity?
    suspend fun addFarmer(farmer: FarmerEntity): Resource<Unit>
    suspend fun updateFarmer(farmer: FarmerEntity): Resource<Unit>
    suspend fun deleteFarmer(id: String): Resource<Unit>
    suspend fun syncFarmers(): Resource<Unit>
    suspend fun updateFarmerBalance(farmerId: String, amountChange: Double, isArrival: Boolean): Resource<Unit>
}
