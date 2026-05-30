package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.MarketRateEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface MarketRateRepository {
    fun getRatesByDate(date: Long): Flow<List<MarketRateEntity>>
    fun getHistoricalRates(productId: String): Flow<List<MarketRateEntity>>
    suspend fun saveRate(rate: MarketRateEntity): Resource<Unit>
    suspend fun updateRate(rate: MarketRateEntity): Resource<Unit>
    suspend fun deleteRate(id: String): Resource<Unit>
    suspend fun syncRates(): Resource<Unit>
}
