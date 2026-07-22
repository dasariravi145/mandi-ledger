package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.data.remote.model.*
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.util.PhoneNumberUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: AgroLynchDatabase,
    private val premiumStateManager: PremiumStateManager,
    private val userDao: UserDao,
    private val farmerDao: FarmerDao,
    private val buyerDao: BuyerDao,
    private val productDao: ProductDao,
    private val arrivalDao: ArrivalDao,
    private val saleDao: SaleDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val billNumberDao: BillNumberSeriesDao,
    private val profileDao: CompanyProfileDao,
    private val productTypeDao: ProductTypeDao,
    private val deduplicationManager: com.dasariravi145.agrolynch.util.DeduplicationManager
) : SyncRepository {

    private val userId: String?
        get() = auth.currentUser?.uid

    private fun getBackupRef(uid: String) = firestore.collection("users").document(uid).collection("backup")

    override suspend fun syncAllData(): Resource<Unit> = withContext(Dispatchers.IO) {
        Timber.d("CLOUD_ACCOUNT_BACKUP_STARTED")
        val uid = userId ?: run {
            Timber.e("CLOUD_ACCOUNT_BACKUP_FAILED: User not authenticated")
            return@withContext Resource.Error("Please sign in again.")
        }
        Timber.d("CLOUD_ACCOUNT_AUTH_UID_RESOLVED: $uid")

        if (!premiumStateManager.isPremium.first()) {
            Timber.w("CLOUD_ACCOUNT_BACKUP_FAILED: Premium required")
            return@withContext Resource.Error("Premium Subscription is required for Cloud Sync.")
        }

        return@withContext try {
            val backupRef = getBackupRef(uid)
            Timber.d("CLOUD_ACCOUNT_FIRESTORE_PATH_SELECTED: ${backupRef.path}")
            
            Timber.d("Sync started for UID: $uid")

            // Sync Sections with chunking support
            syncSection("farmers", farmerDao.getAllFarmers().first(), backupRef)
            syncSection("buyers", buyerDao.getAllBuyers().first(), backupRef)
            syncSection("products", productDao.getAllProducts().first(), backupRef)
            syncSection("arrivals", arrivalDao.getAllArrivals().first(), backupRef)
            syncSection("sales", saleDao.getAllSales().first(), backupRef)
            syncSection("sale_items", saleDao.getAllSaleItems().first(), backupRef)
            syncSection("payments", paymentDao.getAllPayments().first(), backupRef)
            syncSection("expenses", expenseDao.getAllExpenses().first(), backupRef)
            syncSection("product_types", productTypeDao.getAllProductTypesList(), backupRef)
            
            // Settings
            billNumberDao.getAllSeries().first().forEach { series ->
                backupRef.document("settings_${series.seriesType}").set(series).await()
            }

            // Profile
            profileDao.getProfile().first()?.let { profile ->
                backupRef.document("profile_current").set(profile).await()
            }

            // Update local user's last backup timestamp
            userDao.getUserById(uid)?.let { user ->
                userDao.insertUser(user.copy(lastUpdatedAt = System.currentTimeMillis()))
            }

            Timber.d("CLOUD_ACCOUNT_BACKUP_SUCCESS")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CLOUD_ACCOUNT_BACKUP_FAILED")
            val msg = if (e.message?.contains("PERMISSION_DENIED", true) == true) {
                "Cloud access was denied. Please try signing in again or contact support."
            } else {
                "Cloud sync failed: ${e.localizedMessage}"
            }
            Resource.Error(msg)
        }
    }

    private suspend fun syncSection(sectionName: String, items: List<Any>, backupRef: CollectionReference) {
        if (items.isEmpty()) return
        Timber.d("Syncing section: $sectionName, items: ${items.size}")
        
        items.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { item ->
                val id = when(item) {
                    is com.dasariravi145.agrolynch.data.local.entity.FarmerEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.BuyerEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.ProductEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.SaleEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.PaymentEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity -> item.id
                    is com.dasariravi145.agrolynch.data.local.entity.ProductTypeEntity -> item.id
                    else -> java.util.UUID.randomUUID().toString()
                }
                
                // Ensure the entity is converted to a map that Firestore can handle correctly
                // or just pass it as is if it uses stable field names.
                // Given the user's issue, we explicitly convert to map and ensure 'id' field is present.
                val map = when(item) {
                    is com.dasariravi145.agrolynch.data.local.entity.FarmerEntity -> mapOf(
                        "id" to item.id, "name" to item.name, "mobileNumber" to item.mobileNumber, "village" to item.village,
                        "notes" to item.notes, "totalArrivals" to item.totalArrivals, "totalPayments" to item.totalPayments,
                        "pendingAmount" to item.pendingAmount, "advanceAmount" to item.advanceAmount, "lastUpdated" to item.lastUpdated,
                        "isSynced" to true, "isDeleted" to item.isDeleted
                    )
                    is com.dasariravi145.agrolynch.data.local.entity.BuyerEntity -> mapOf(
                        "id" to item.id, "name" to item.name, "mobileNumber" to item.mobileNumber, "address" to item.address,
                        "gstNumber" to item.gstNumber, "totalPurchase" to item.totalPurchase, "totalPaid" to item.totalPaid,
                        "pendingAmount" to item.pendingAmount, "lastUpdated" to item.lastUpdated, "isSynced" to true, "isDeleted" to item.isDeleted
                    )
                    else -> item
                }
                batch.set(backupRef.document("${sectionName}_$id"), map)
            }
            batch.commit().await()
        }
    }

    override suspend fun restoreAllData(): Resource<Unit> = withContext(Dispatchers.IO) {
        Timber.d("CLOUD_ACCOUNT_RESTORE_STARTED")
        val uid = userId ?: run {
            Timber.e("CLOUD_ACCOUNT_RESTORE_FAILED: User not authenticated")
            return@withContext Resource.Error("Please sign in again.")
        }
        
        return@withContext try {
            val backupRef = getBackupRef(uid)
            Timber.d("CLOUD_ACCOUNT_FIRESTORE_PATH_SELECTED: ${backupRef.path}")
            
            val allDocs = backupRef.get().await()
            
            if (allDocs.isEmpty) {
                Timber.w("CLOUD_ACCOUNT_RESTORE_FAILED: No backup found")
                return@withContext Resource.Error("No cloud backup was found.")
            }

            // 1. Prepare Canonical ID Mappings
            val localFarmers = farmerDao.getFarmersList()
            val localBuyers = buyerDao.getBuyersList()
            val localProducts = productDao.getProductsList()
            val localArrivals = arrivalDao.getAllArrivalsList()

            val farmerPhoneMap = localFarmers.filter { it.mobileNumber.isNotBlank() }
                .associateBy({ PhoneNumberUtils.normalize(it.mobileNumber) }, { it.id })
            
            val farmerNameVillageMap = localFarmers.associateBy({ "${it.name.lowercase()}_${it.village.lowercase()}" }, { it.id })
            
            val buyerPhoneMap = localBuyers.filter { it.mobileNumber.isNotBlank() }
                .associateBy({ PhoneNumberUtils.normalize(it.mobileNumber) }, { it.id })

            val buyerNameAddressMap = localBuyers.associateBy({ "${it.name.lowercase()}_${it.address.lowercase()}" }, { it.id })

            val productNameMap = localProducts.associateBy({ it.name.lowercase() }, { it.id })
            
            val arrivalNaturalKeyMap = localArrivals.associateBy({ "${it.farmerId}_${it.productId}_${it.date}_${it.netQuantity}" }, { it.id })

            val farmerIdMap = mutableMapOf<String, String>()
            val buyerIdMap = mutableMapOf<String, String>()
            val productIdMap = mutableMapOf<String, String>()
            val arrivalIdMap = mutableMapOf<String, String>()

            database.withTransaction {
                // 1. Restore products and build mapping
                allDocs.documents.filter { it.id.startsWith("products_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ProductEntity::class.java)?.let { product ->
                        val canonicalId = productNameMap[product.name.lowercase()] ?: product.id
                        productIdMap[product.id] = canonicalId
                        productDao.insertProduct(product.copy(id = canonicalId))
                    }
                }

                // 2. Farmers and Buyers
                allDocs.documents.filter { it.id.startsWith("farmers_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.FarmerEntity::class.java)?.let { farmer ->
                        val normalizedPhone = PhoneNumberUtils.normalize(farmer.mobileNumber)
                        val canonicalId = if (normalizedPhone.isNotBlank()) {
                            farmerPhoneMap[normalizedPhone] ?: farmer.id
                        } else {
                            farmerNameVillageMap["${farmer.name.lowercase()}_${farmer.village.lowercase()}"] ?: farmer.id
                        }
                        farmerIdMap[farmer.id] = canonicalId
                        farmerDao.insertFarmer(farmer.copy(id = canonicalId))
                    }
                }
                allDocs.documents.filter { it.id.startsWith("buyers_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.BuyerEntity::class.java)?.let { buyer ->
                        val normalizedPhone = PhoneNumberUtils.normalize(buyer.mobileNumber)
                        val canonicalId = if (normalizedPhone.isNotBlank()) {
                            buyerPhoneMap[normalizedPhone] ?: buyer.id
                        } else {
                            buyerNameAddressMap["${buyer.name.lowercase()}_${buyer.address.lowercase()}"] ?: buyer.id
                        }
                        buyerIdMap[buyer.id] = canonicalId
                        buyerDao.insertBuyer(buyer.copy(id = canonicalId))
                    }
                }

                // 3. Arrivals
                allDocs.documents.filter { it.id.startsWith("arrivals_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity::class.java)?.let { arrival ->
                        val pid = productIdMap[arrival.productId] ?: arrival.productId
                        val fid = farmerIdMap[arrival.farmerId] ?: arrival.farmerId
                        
                        val existingIdById = localArrivals.find { it.id == arrival.id }?.id
                        val existingIdByNaturalKey = arrivalNaturalKeyMap["${fid}_${pid}_${arrival.date}_${arrival.netQuantity}"]
                        
                        val canonicalId = existingIdById ?: existingIdByNaturalKey ?: arrival.id
                        arrivalIdMap[arrival.id] = canonicalId
                        
                        arrivalDao.insertArrival(arrival.copy(id = canonicalId, productId = pid, farmerId = fid))
                    }
                }
                allDocs.documents.filter { it.id.startsWith("sales_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.SaleEntity::class.java)?.let { sale ->
                        val bid = buyerIdMap[sale.buyerId] ?: sale.buyerId
                        val pid = productIdMap[sale.productId] ?: sale.productId
                        saleDao.insertSale(sale.copy(buyerId = bid, productId = pid))
                    }
                }
                allDocs.documents.filter { it.id.startsWith("payments_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.PaymentEntity::class.java)?.let { payment ->
                        val pid = if (payment.partyType == "FARMER") {
                            farmerIdMap[payment.partyId] ?: payment.partyId
                        } else {
                            buyerIdMap[payment.partyId] ?: payment.partyId
                        }
                        paymentDao.insertPayment(payment.copy(partyId = pid))
                    }
                }
                allDocs.documents.filter { it.id.startsWith("expenses_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity::class.java)?.let { expenseDao.insertExpense(it) }
                }

                // Restore sale items with remapped IDs
                allDocs.documents.filter { it.id.startsWith("sale_items_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity::class.java)?.let { item ->
                        val fid = farmerIdMap[item.farmerId] ?: item.farmerId
                        val pid = productIdMap[item.productId] ?: item.productId
                        val aid = arrivalIdMap[item.arrivalId] ?: item.arrivalId
                        saleDao.insertSaleItem(item.copy(farmerId = fid, productId = pid, arrivalId = aid))
                    }
                }

                // 4. Settings
                allDocs.documents.filter { it.id.startsWith("product_types_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ProductTypeEntity::class.java)?.let { type ->
                        val pid = productIdMap[type.productId] ?: type.productId
                        productTypeDao.insertProductType(type.copy(productId = pid))
                    }
                }

                allDocs.documents.filter { it.id.startsWith("settings_") }.forEach { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity::class.java)?.let { billNumberDao.insertSeries(it) }
                }
                allDocs.documents.find { it.id == "profile_current" }?.let { doc ->
                    doc.toObject(com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity::class.java)?.let { profile ->
                        profileDao.updateProfile(profile)
                    }
                }
            }

            // 5. Final step: Recalculate all affected master records
            deduplicationManager.recalculateAllAffected(farmerIdMap.values.toSet(), buyerIdMap.values.toSet())

            Timber.d("CLOUD_ACCOUNT_RESTORE_SUCCESS")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CLOUD_ACCOUNT_RESTORE_FAILED")
            val msg = when {
                e.message?.contains("PERMISSION_DENIED", true) == true -> 
                    "Cloud access was denied. Please try signing in again or contact support."
                e.message?.contains("UNAVAILABLE", true) == true ->
                    "Internet connection is unavailable."
                else -> e.localizedMessage ?: "Cloud restore failed. Please try again."
            }
            Resource.Error(msg)
        }
    }

    override suspend fun uploadFile(file: File, remotePath: String): Resource<String> = Resource.Error("Not implemented")
    override suspend fun downloadFile(remotePath: String, localFile: File): Resource<File> = Resource.Error("Not implemented")

    override suspend fun saveUserProfile(profile: FirestoreUserProfile): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            firestore.collection("users").document(uid).set(profile).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save profile: ${e.message}")
        }
    }

    override fun isSyncEnabled(): Flow<Boolean> = premiumStateManager.isPremium
}
