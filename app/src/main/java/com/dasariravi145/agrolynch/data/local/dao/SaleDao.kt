package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.SaleEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsBySaleId(saleId: String): List<SaleItemEntity>

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: String): SaleEntity?

    @Query("UPDATE sales SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteSale(id: String)

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("UPDATE sales SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
