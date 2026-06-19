package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.BoxWeightItemEntity

@Dao
interface BoxWeightDao {
    @Query("SELECT * FROM box_weight_items")
    suspend fun getAllItemsList(): List<BoxWeightItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BoxWeightItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxWeights(items: List<BoxWeightItemEntity>)

    @Query("SELECT * FROM box_weight_items WHERE arrivalId = :arrivalId")
    suspend fun getBoxWeightsForArrival(arrivalId: String): List<BoxWeightItemEntity>
}
