package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items ORDER BY date DESC")
    fun getAllSaleItems(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sales")
    suspend fun getAllSalesList(): List<SaleEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsList(): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(item: SaleItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsBySaleId(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE farmerId = :farmerId")
    suspend fun getItemsByFarmer(farmerId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sales WHERE buyerId = :buyerId AND isDeleted = 0")
    suspend fun getSalesByBuyer(buyerId: String): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE buyerId = :buyerId AND isDeleted = 0 ORDER BY date DESC")
    fun getSalesByBuyerFlow(buyerId: String): Flow<List<SaleEntity>>

    @Query("""
        SELECT * FROM sale_items 
        WHERE saleId IN (SELECT id FROM sales WHERE buyerId = :buyerId AND isDeleted = 0)
    """)
    fun getSaleItemsByBuyerFlow(buyerId: String): Flow<List<SaleItemEntity>>

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: String): SaleEntity?

    @Query("UPDATE sales SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteSale(id: String)

    @Query("UPDATE sales SET isDeleted = 1, isSynced = 0 WHERE id IN (:ids)")
    suspend fun softDeleteSales(ids: List<String>)

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("UPDATE sales SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE sales SET buyerId = :newId WHERE buyerId = :oldId")
    suspend fun updateBuyerId(oldId: String, newId: String)

    @Query("UPDATE sale_items SET farmerId = :newId WHERE farmerId = :oldId")
    suspend fun updateFarmerIdInItems(oldId: String, newId: String)

    @Query("SELECT SUM(totalNetAmount) FROM sales WHERE buyerId = :buyerId AND isDeleted = 0")
    suspend fun getSumTotalNetAmountForBuyer(buyerId: String): Double?

    @Query("SELECT SUM(paidAmount) FROM sales WHERE buyerId = :buyerId AND isDeleted = 0")
    suspend fun getSumPaidAmountForBuyer(buyerId: String): Double?

    @Query("""
        SELECT * FROM sale_items 
        WHERE saleId IN (SELECT id FROM sales WHERE buyerId = :buyerId AND isDeleted = 0)
    """)
    suspend fun getItemsByBuyerSales(buyerId: String): List<SaleItemEntity>

    @Query("""
        SELECT * FROM entry_deductions 
        WHERE entryId IN (SELECT id FROM sales WHERE buyerId = :buyerId AND isDeleted = 0)
    """)
    suspend fun getDeductionsForBuyerSales(buyerId: String): List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>
}
