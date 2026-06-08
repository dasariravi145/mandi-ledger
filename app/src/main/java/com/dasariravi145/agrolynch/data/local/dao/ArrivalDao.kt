package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import kotlinx.coroutines.flow.Flow

data class FarmerStockInfo(
    val farmerId: String,
    val farmerName: String
)

data class ProductStockInfo(
    val productId: String,
    val productName: String
)

@Dao
interface ArrivalDao {
    @Query("SELECT * FROM arrivals WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllArrivals(): Flow<List<ArrivalEntity>>

    @Query("SELECT * FROM arrivals WHERE productId = :productId AND remainingQuantity > 0 AND isDeleted = 0 ORDER BY date ASC")
    fun getAvailableStockByProduct(productId: String): Flow<List<ArrivalEntity>>

    @Query("SELECT * FROM arrivals WHERE productId = :productId AND grade = :grade AND remainingQuantity > 0 AND isDeleted = 0 ORDER BY date ASC")
    fun getAvailableStockByProductAndGrade(productId: String, grade: String): Flow<List<ArrivalEntity>>

    @Query("SELECT DISTINCT farmerId, farmerName FROM arrivals WHERE remainingQuantity > 0 AND isDeleted = 0")
    fun getFarmersWithStock(): Flow<List<FarmerStockInfo>>

    @Query("SELECT * FROM arrivals WHERE farmerId = :farmerId AND remainingQuantity > 0 AND isDeleted = 0 ORDER BY date ASC")
    fun getAvailableStockByFarmer(farmerId: String): Flow<List<ArrivalEntity>>

    @Query("SELECT * FROM arrivals WHERE id = :id")
    suspend fun getArrivalById(id: String): ArrivalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArrival(arrival: ArrivalEntity)

    @Update
    suspend fun updateArrival(arrival: ArrivalEntity)

    @Query("UPDATE arrivals SET remainingQuantity = remainingQuantity - :soldQuantity WHERE id = :id")
    suspend fun reduceStock(id: String, soldQuantity: Double)

    @Query("UPDATE arrivals SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteArrival(id: String)

    @Query("SELECT * FROM arrivals WHERE isSynced = 0")
    suspend fun getUnsyncedArrivals(): List<ArrivalEntity>

    @Query("UPDATE arrivals SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
