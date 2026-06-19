package com.dasariravi145.agrolynch.data.repository

import android.net.Uri
import com.dasariravi145.agrolynch.data.local.dao.ProductDao
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.domain.repository.ProductRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ProductRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    override suspend fun getProductById(id: String): ProductEntity? = productDao.getProductById(id)

    override suspend fun getProductByName(name: String): ProductEntity? = productDao.getProductByName(name)

    override suspend fun addProduct(product: ProductEntity, imageUri: Uri?): Resource<Unit> {
        return try {
            // Save locally first
            productDao.insertProduct(product)
            
            // Sync in background
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        var imageUrl = ""
                        if (imageUri != null) {
                            val ref = storage.reference.child("users/$uid/products/${product.id}")
                            ref.putFile(imageUri).await()
                            imageUrl = ref.downloadUrl.await().toString()
                        }
                        val productMap = product.javaClass.declaredFields.associate { field ->
                            field.isAccessible = true
                            field.name to field.get(product)
                        }.toMutableMap()
                        productMap["imageUrl"] = imageUrl
                        productMap["ownerUserId"] = uid
                        
                        firestore.collection("users").document(uid).collection("products").document(product.id).set(productMap).await()
                        productDao.markAsSynced(product.id)
                    } catch (e: Exception) {
                        // Synced later by SyncWorker
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateProduct(product: ProductEntity, imageUri: Uri?): Resource<Unit> {
        return try {
            val updatedProduct = product.copy(
                isSynced = false,
                lastUpdated = System.currentTimeMillis()
            )
            productDao.updateProduct(updatedProduct)
            
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        var imageUrl = updatedProduct.imageUrl
                        if (imageUri != null) {
                            val ref = storage.reference.child("users/$uid/products/${product.id}")
                            ref.putFile(imageUri).await()
                            imageUrl = ref.downloadUrl.await().toString()
                        }
                        val finalProduct = updatedProduct.copy(imageUrl = imageUrl)
                        if (imageUrl != updatedProduct.imageUrl) {
                            productDao.updateProduct(finalProduct)
                        }
                        val productMap = finalProduct.javaClass.declaredFields.associate { field ->
                            field.isAccessible = true
                            field.name to field.get(finalProduct)
                        }.toMutableMap()
                        productMap["ownerUserId"] = uid
                        
                        firestore.collection("users").document(uid).collection("products").document(product.id).set(productMap).await()
                        productDao.markAsSynced(product.id)
                    } catch (e: Exception) {
                        // Synced later by SyncWorker
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun deleteProduct(id: String): Resource<Unit> {
        return try {
            productDao.softDeleteProduct(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("products").document(id).update("isDeleted", true).await()
                    } catch (e: Exception) {
                        // Synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun syncProducts(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsyncedProducts = productDao.getUnsyncedProducts()
            for (product in unsyncedProducts) {
                val productMap = product.javaClass.declaredFields.associate { field ->
                    field.isAccessible = true
                    field.name to field.get(product)
                }.toMutableMap()
                productMap["ownerUserId"] = uid
                firestore.collection("users").document(uid).collection("products").document(product.id).set(productMap).await()
                productDao.markAsSynced(product.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
