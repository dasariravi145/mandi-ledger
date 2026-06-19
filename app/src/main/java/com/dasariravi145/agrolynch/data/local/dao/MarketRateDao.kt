package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.MarketRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketRateDao {
    @Query("SELECT * FROM market_rates WHERE date = :date ORDER BY productName ASC")
    fun getRatesByDate(date: Long): Flow<List<MarketRateEntity>>

    @Query("SELECT * FROM market_rates WHERE productId = :productId ORDER BY date DESC")
    fun getHistoricalRates(productId: String): Flow<List<MarketRateEntity>>

    @Query("SELECT * FROM market_rates")
    suspend fun getMarketRatesList(): List<MarketRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketRate(rate: MarketRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: MarketRateEntity)

    @Update
    suspend fun updateRate(rate: MarketRateEntity)

    @Query("UPDATE market_rates SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteRate(id: String)

    @Query("SELECT * FROM market_rates WHERE isSynced = 0")
    suspend fun getUnsyncedRates(): List<MarketRateEntity>

    @Query("UPDATE market_rates SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
