package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.MarketOutlookCacheEntity

@Dao
interface MarketOutlookCacheDao {
    @Query("SELECT * FROM market_outlook_cache WHERE id = :id")
    suspend fun getCachedForecast(id: String): MarketOutlookCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: MarketOutlookCacheEntity)

    @Query("DELETE FROM market_outlook_cache WHERE timestamp < :cutoffTimestamp")
    suspend fun clearOldCache(cutoffTimestamp: Long)
}
