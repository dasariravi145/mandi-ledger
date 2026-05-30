package com.dasariravi145.agrolynch.domain.repository

import android.net.Uri
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<ProductEntity>>
    suspend fun getProductById(id: String): ProductEntity?
    suspend fun getProductByName(name: String): ProductEntity?
    suspend fun addProduct(product: ProductEntity, imageUri: Uri?): Resource<Unit>
    suspend fun updateProduct(product: ProductEntity, imageUri: Uri?): Resource<Unit>
    suspend fun deleteProduct(id: String): Resource<Unit>
    suspend fun syncProducts(): Resource<Unit>
}
