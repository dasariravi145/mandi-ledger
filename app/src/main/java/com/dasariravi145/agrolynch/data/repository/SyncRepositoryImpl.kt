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
    private val profileDao: CompanyProfileDao
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
            syncSection("payments", paymentDao.getAllPayments().first(), backupRef)
            syncSection("expenses", expenseDao.getAllExpenses().first(), backupRef)
            
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
                    else -> java.util.UUID.randomUUID().toString()
                }
                batch.set(backupRef.document("${sectionName}_$id"), item)
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

            database.withTransaction {
                for (doc in allDocs.documents) {
                    val id = doc.id
                    when {
                        id.startsWith("farmers_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.FarmerEntity::class.java)?.let { farmerDao.insertFarmer(it) }
                        id.startsWith("buyers_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.BuyerEntity::class.java)?.let { buyerDao.insertBuyer(it) }
                        id.startsWith("products_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ProductEntity::class.java)?.let { productDao.insertProduct(it) }
                        id.startsWith("arrivals_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity::class.java)?.let { arrivalDao.insertArrival(it) }
                        id.startsWith("sales_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.SaleEntity::class.java)?.let { saleDao.insertSale(it) }
                        id.startsWith("payments_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.PaymentEntity::class.java)?.let { paymentDao.insertPayment(it) }
                        id.startsWith("expenses_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity::class.java)?.let { expenseDao.insertExpense(it) }
                        id.startsWith("settings_") -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity::class.java)?.let { billNumberDao.insertSeries(it) }
                        id == "profile_current" -> doc.toObject(com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity::class.java)?.let { profileDao.updateProfile(it) }
                    }
                }
            }

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
