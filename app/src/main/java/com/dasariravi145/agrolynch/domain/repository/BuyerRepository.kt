package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface BuyerRepository {
    fun getBuyers(): Flow<List<BuyerEntity>>
    suspend fun getBuyerById(id: String): BuyerEntity?
    suspend fun addBuyer(buyer: BuyerEntity): Resource<Unit>
    suspend fun updateBuyer(buyer: BuyerEntity): Resource<Unit>
    suspend fun deleteBuyer(id: String): Resource<Unit>
    suspend fun syncBuyers(): Resource<Unit>
}
