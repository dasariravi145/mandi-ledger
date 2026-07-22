package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.ProductTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductTypeDao {
    @Query("SELECT * FROM product_types WHERE productId = :productId ORDER BY productTypeName ASC")
    fun getProductTypesByProduct(productId: String): Flow<List<ProductTypeEntity>>

    @Query("SELECT * FROM product_types WHERE productId = :productId ORDER BY productTypeName ASC")
    suspend fun getProductTypesByProductList(productId: String): List<ProductTypeEntity>

    @Query("SELECT * FROM product_types")
    suspend fun getAllProductTypesList(): List<ProductTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductType(productType: ProductTypeEntity)

    @Query("SELECT * FROM product_types WHERE productId = :productId AND productTypeName = :name LIMIT 1")
    suspend fun getProductTypeByName(productId: String, name: String): ProductTypeEntity?

    @Query("DELETE FROM product_types WHERE id = :id")
    suspend fun deleteProductType(id: String)
}
