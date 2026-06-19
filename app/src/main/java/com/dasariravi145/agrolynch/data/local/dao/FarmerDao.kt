package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllFarmers(): Flow<List<FarmerEntity>>

    @Query("SELECT * FROM farmers WHERE isDeleted = 0 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getFarmersPaged(limit: Int, offset: Int): List<FarmerEntity>

    @Query("SELECT * FROM farmers WHERE id = :id")
    suspend fun getFarmerById(id: String): FarmerEntity?

    @Query("SELECT * FROM farmers")
    suspend fun getFarmersList(): List<FarmerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: FarmerEntity)

    @Update
    suspend fun updateFarmer(farmer: FarmerEntity)

    @Query("UPDATE farmers SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteFarmer(id: String)

    @Query("SELECT * FROM farmers WHERE isSynced = 0")
    suspend fun getUnsyncedFarmers(): List<FarmerEntity>

    @Query("UPDATE farmers SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
