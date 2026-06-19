package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuyerDao {
    @Query("SELECT * FROM buyers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllBuyers(): Flow<List<BuyerEntity>>

    @Query("SELECT * FROM buyers WHERE id = :id")
    suspend fun getBuyerById(id: String): BuyerEntity?

    @Query("SELECT * FROM buyers")
    suspend fun getBuyersList(): List<BuyerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyer(buyer: BuyerEntity)

    @Update
    suspend fun updateBuyer(buyer: BuyerEntity)

    @Query("UPDATE buyers SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteBuyer(id: String)

    @Query("SELECT * FROM buyers WHERE isSynced = 0")
    suspend fun getUnsyncedBuyers(): List<BuyerEntity>

    @Query("UPDATE buyers SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
