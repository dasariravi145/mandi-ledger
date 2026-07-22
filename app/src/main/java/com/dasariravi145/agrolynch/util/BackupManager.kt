package com.dasariravi145.agrolynch.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.BackupData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dasariravi145.agrolynch.util.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class CloudBackupResult(val storagePath: String, val downloadUrl: String)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AgroLynchDatabase,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val gson: Gson,
    private val deduplicationManager: DeduplicationManager
) {
    private val backupDir = File(context.filesDir, "backups")

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    suspend fun createLocalBackup(fileName: String? = null): Resource<File> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch data from DB in a single consistent snapshot
            val data = database.withTransaction {
                BackupData(
                    companyProfile = listOfNotNull(database.companyProfileDao().getProfile().first()),
                    farmers = database.farmerDao().getFarmersList(),
                    buyers = database.buyerDao().getBuyersList(),
                    products = database.productDao().getProductsList(),
                    productTypes = database.productTypeDao().getAllProductTypesList(),
                    arrivals = database.arrivalDao().getAllArrivalsList(),
                    sales = database.saleDao().getAllSalesList(),
                    saleItems = database.saleDao().getAllSaleItemsList(),
                    payments = database.paymentDao().getAllPaymentsList(),
                    expenses = database.expenseDao().getAllExpensesList(),
                    marketRates = database.marketRateDao().getMarketRatesList(),
                    transactions = database.transactionDao().getAllTransactionsList(),
                    ocrScans = database.ocrScanDao().getAllScansList(),
                    boxWeightItems = database.boxWeightDao().getAllItemsList(),
                    billSeries = database.billNumberSeriesDao().getAllSeriesList(),
                    deductions = database.entryDeductionDao().getAllDeductionsList(),
                    templatePositions = database.templatePositionDao().getAllPositionsList(),
                    invoiceLayouts = database.invoiceLayoutDao().getAllLayoutsList(),
                    invoiceWizardConfigs = database.invoiceWizardDao().getAllConfigsList(),
                    accountBookArchives = database.accountBookArchiveDao().getAllArchivesList()
                )
            }

            // 2. Perform serialization using streaming writer to minimize memory footprint
            val finalFile = if (fileName != null) {
                File(backupDir, fileName)
            } else {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                File(backupDir, "backup_$timestamp.json")
            }

            // 3. Atomic Write: Write to temp file first, then rename
            val tempFile = File(backupDir, "${finalFile.name}.tmp")
            tempFile.parentFile?.mkdirs()
            
            withContext(Dispatchers.Default) {
                tempFile.bufferedWriter().use { writer ->
                    gson.toJson(data, writer)
                }
            }
            
            if (tempFile.exists() && tempFile.length() > 0) {
                if (finalFile.exists()) finalFile.delete()
                if (tempFile.renameTo(finalFile)) {
                    Resource.Success(finalFile)
                } else {
                    Resource.Error("Failed to finalize backup file")
                }
            } else {
                Resource.Error("Backup serialization produced an empty file")
            }
        } catch (e: Exception) {
            Timber.e(e, "Local backup failed")
            Resource.Error("Local backup failed: ${e.message}")
        }
    }

    suspend fun restoreLocalBackup(file: File): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = FileReader(file).use { reader ->
                gson.fromJson(reader, BackupData::class.java)
            } ?: return@withContext Resource.Error("The backup file is empty or corrupted.")

            // 1. Prepare Canonical ID Mappings BEFORE clearing
            val localFarmers = database.farmerDao().getFarmersList()
            val localBuyers = database.buyerDao().getBuyersList()
            val localProducts = database.productDao().getProductsList()
            val localArrivals = database.arrivalDao().getAllArrivalsList()

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

            // 2. Map Master Data
            val mappedFarmers = data.farmers.map { farmer ->
                val normalizedPhone = PhoneNumberUtils.normalize(farmer.mobileNumber)
                val canonicalId = if (normalizedPhone.isNotBlank()) {
                    farmerPhoneMap[normalizedPhone] ?: farmer.id
                } else {
                    farmerNameVillageMap["${farmer.name.lowercase()}_${farmer.village.lowercase()}"] ?: farmer.id
                }
                farmerIdMap[farmer.id] = canonicalId
                farmer.copy(id = canonicalId)
            }

            val mappedBuyers = data.buyers.map { buyer ->
                val normalizedPhone = PhoneNumberUtils.normalize(buyer.mobileNumber)
                val canonicalId = if (normalizedPhone.isNotBlank()) {
                    buyerPhoneMap[normalizedPhone] ?: buyer.id
                } else {
                    buyerNameAddressMap["${buyer.name.lowercase()}_${buyer.address.lowercase()}"] ?: buyer.id
                }
                buyerIdMap[buyer.id] = canonicalId
                buyer.copy(id = canonicalId)
            }

            val mappedProducts = data.products.map { product ->
                val canonicalId = productNameMap[product.name.lowercase()] ?: product.id
                productIdMap[product.id] = canonicalId
                product.copy(id = canonicalId)
            }

            database.withTransaction {
                // Clear all tables first
                database.clearSupportedTables()

                // 1. Profile & Settings
                data.companyProfile.forEach { database.companyProfileDao().insertProfile(it) }
                data.billSeries.forEach { database.billNumberSeriesDao().insertSeries(it) }
                data.templatePositions.forEach { database.templatePositionDao().insertPosition(it) }

                // 2. Master Data
                mappedProducts.forEach { database.productDao().insertProduct(it) }
                data.productTypes.forEach { type ->
                    val pid = productIdMap[type.productId] ?: type.productId
                    database.productTypeDao().insertProductType(type.copy(productId = pid))
                }
                mappedFarmers.forEach { database.farmerDao().insertFarmer(it) }
                mappedBuyers.forEach { database.buyerDao().insertBuyer(it) }

                // 3. Arrivals - Build Mapping
                data.arrivals.forEach { arrival ->
                    val pid = productIdMap[arrival.productId] ?: arrival.productId
                    val fid = farmerIdMap[arrival.farmerId] ?: arrival.farmerId
                    
                    val existingIdById = localArrivals.find { it.id == arrival.id }?.id
                    val existingIdByNaturalKey = arrivalNaturalKeyMap["${fid}_${pid}_${arrival.date}_${arrival.netQuantity}"]
                    
                    val canonicalId = existingIdById ?: existingIdByNaturalKey ?: arrival.id
                    arrivalIdMap[arrival.id] = canonicalId
                    
                    database.arrivalDao().insertArrival(arrival.copy(id = canonicalId, productId = pid, farmerId = fid))
                }

                // 4. Sales and Items
                data.sales.forEach { sale ->
                    val bid = buyerIdMap[sale.buyerId] ?: sale.buyerId
                    val pid = productIdMap[sale.productId] ?: sale.productId
                    database.saleDao().insertSale(sale.copy(buyerId = bid, productId = pid))
                }

                data.saleItems.forEach { item ->
                    val fid = farmerIdMap[item.farmerId] ?: item.farmerId
                    val pid = productIdMap[item.productId] ?: item.productId
                    val aid = arrivalIdMap[item.arrivalId] ?: item.arrivalId
                    database.saleDao().insertSaleItem(item.copy(farmerId = fid, productId = pid, arrivalId = aid))
                }

                data.payments.forEach { payment ->
                    val pid = if (payment.partyType == "FARMER") {
                        farmerIdMap[payment.partyId] ?: payment.partyId
                    } else {
                        buyerIdMap[payment.partyId] ?: payment.partyId
                    }
                    database.paymentDao().insertPayment(payment.copy(partyId = pid))
                }
                
                // 5. Supporting Data
                data.expenses.forEach { database.expenseDao().insertExpense(it) }
                data.marketRates.forEach { rate ->
                    val pid = productIdMap[rate.productId] ?: rate.productId
                    database.marketRateDao().insertMarketRate(rate.copy(productId = pid))
                }
                data.transactions.forEach { trans ->
                    val fid = farmerIdMap[trans.farmerId] ?: trans.farmerId
                    val pid = productIdMap[trans.productId] ?: trans.productId
                    database.transactionDao().insertTransaction(trans.copy(farmerId = fid, productId = pid))
                }
                data.ocrScans.forEach { database.ocrScanDao().insertScan(it) }
                data.boxWeightItems.forEach { database.boxWeightDao().insertItem(it) }
                data.deductions.forEach { database.entryDeductionDao().insertDeduction(it) }
                data.invoiceLayouts.forEach { database.invoiceLayoutDao().saveLayout(it) }
                data.invoiceWizardConfigs.forEach { database.invoiceWizardDao().saveConfig(it) }
                data.accountBookArchives.forEach { database.accountBookArchiveDao().insertArchive(it) }
            }
            
            // 4. Final step: Recalculate all affected master records
            val affectedFarmerIds = farmerIdMap.values.toSet()
            val affectedBuyerIds = buyerIdMap.values.toSet()
            deduplicationManager.recalculateAllAffected(affectedFarmerIds, affectedBuyerIds)

            // 5. Verify restored counts
            val verificationResult = verifyRestoredData(data)
            if (!verificationResult) {
                Timber.e("Restore verification failed")
                return@withContext Resource.Error("Restore could not be verified. Some data may be missing.")
            }

            Timber.i("Restore Success: Farmers=${mappedFarmers.size}, Buyers=${mappedBuyers.size}, Sales=${data.sales.size}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Restore failed")
            Resource.Error("Restore failed: ${e.message}")
        }
    }

    private suspend fun verifyRestoredData(expected: BackupData): Boolean {
        val actualFarmers = database.farmerDao().getFarmersList().size
        val actualBuyers = database.buyerDao().getBuyersList().size
        val actualArrivals = database.arrivalDao().getAllArrivalsList().size
        val actualSales = database.saleDao().getAllSalesList().size

        // We allow actual > expected if the DB already had some records (should not happen if clearSupportedTables worked correctly)
        // But for point-in-time restore, they should match exactly after clearSupportedTables.
        
        Timber.d("Verification: Farmers (E:${expected.farmers.size} A:$actualFarmers), " +
                "Buyers (E:${expected.buyers.size} A:$actualBuyers), " +
                "Arrivals (E:${expected.arrivals.size} A:$actualArrivals), " +
                "Sales (E:${expected.sales.size} A:$actualSales)")

        return actualFarmers >= expected.farmers.size &&
               actualBuyers >= expected.buyers.size &&
               actualArrivals >= expected.arrivals.size &&
               actualSales >= expected.sales.size
    }

    suspend fun createSafetyBackup(restoreBackupId: String): Resource<File> {
        return createLocalBackup()
    }

    /*suspend fun uploadBackupToFirebase(file: File): Resource<CloudBackupResult> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            android.util.Log.d("BACKUP_UPLOAD", "User not logged in")
            return@withContext Resource.Error("User not logged in")
        }

        val storagePath = "backups/${user.uid}/${file.name}"
        val ref = storage.reference.child(storagePath)

        android.util.Log.d("BACKUP_UPLOAD", "user=${user.uid}")
        android.util.Log.d("BACKUP_UPLOAD", "file=${file.absolutePath}")
        android.util.Log.d("BACKUP_UPLOAD", "exists=${file.exists()}")
        android.util.Log.d("BACKUP_UPLOAD", "size=${file.length()}")
        android.util.Log.d("BACKUP_UPLOAD", "storagePath=$storagePath")

        if (!file.exists()) {
            return@withContext Resource.Error("Local backup file missing: ${file.absolutePath}")
        }

        try {
            android.util.Log.d("BACKUP_UPLOAD", "putFile started")
            ref.putFile(Uri.fromFile(file)).await()
            android.util.Log.d("BACKUP_UPLOAD", "putFile success")
            
            val downloadUrl = ref.downloadUrl.await().toString()
            android.util.Log.d("BACKUP_UPLOAD", "downloadUrl=$downloadUrl")
            
            Resource.Success(CloudBackupResult(storagePath, downloadUrl))
        } catch (e: Exception) {
            android.util.Log.e("BACKUP_UPLOAD", "BACKUP_UPLOAD_FAILED: ${e.message}")
            Resource.Error("Cloud upload failed: ${e.message}")
        }
    }*/

    suspend fun uploadBackupToFirebase(file: File): Resource<CloudBackupResult> {
        return try {
            val user = auth.currentUser
                ?: return Resource.Error("User not logged in. Please login again.")

            if (!file.exists()) {
                return Resource.Error("Local backup file missing: ${file.absolutePath}")
            }

            val storagePath = "backups/${user.uid}/${file.name}"

            val ref = storage.reference.child(storagePath)

            android.util.Log.d("BACKUP_UPLOAD", "user=${user.uid}")
            android.util.Log.d("BACKUP_UPLOAD", "file=${file.absolutePath}")
            android.util.Log.d("BACKUP_UPLOAD", "exists=${file.exists()} size=${file.length()}")
            android.util.Log.d("BACKUP_UPLOAD", "storagePath=$storagePath")

            android.util.Log.d("BACKUP_UPLOAD", "putFile started")
            ref.putFile(Uri.fromFile(file)).await()

            android.util.Log.d("BACKUP_UPLOAD", "putFile success")

            val downloadUrl = ref.downloadUrl.await().toString()

            android.util.Log.d("BACKUP_UPLOAD", "downloadUrl=$downloadUrl")

            // Cleanup old backups (Retention Policy: Keep latest 5)
            try {
                val listResult = ref.parent?.listAll()?.await()
                if (listResult != null && listResult.items.size > 5) {
                    val sortedItems = listResult.items.sortedByDescending { it.name }
                    val itemsToDelete = sortedItems.drop(5)
                    itemsToDelete.forEach { it.delete().await() }
                    Timber.d("Retention: Deleted ${itemsToDelete.size} old cloud backups")
                }
            } catch (e: Exception) {
                Timber.e(e, "Retention cleanup failed")
            }

            Resource.Success(
                CloudBackupResult(
                    downloadUrl = downloadUrl,
                    storagePath = storagePath
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("BACKUP_UPLOAD", "BACKUP_UPLOAD_FAILED", e)
            Resource.Error("Cloud upload failed: ${e.message}")
        }
    }

    suspend fun downloadBackupFromFirebase(storagePath: String): Resource<File> {
        Timber.d("RESTORE_START: $storagePath")
        return try {
            if (storagePath.isBlank()) {
                return Resource.Error("Storage path is empty")
            }
            
            if (storagePath.startsWith("http") || storagePath.startsWith("gs://")) {
                Timber.e("DOWNLOAD_FAILED: location should not be a full URL. Received: $storagePath")
                return Resource.Error("Invalid storage path. Use relative storagePath, not full URL")
            }

            val localFile = File(context.cacheDir, "restore_${System.currentTimeMillis()}.json")
            Timber.d("STORAGE_PATH: $storagePath")

            storage.reference
                .child(storagePath)
                .getFile(localFile)
                .await()

            Timber.d("DOWNLOAD_SUCCESS: ${localFile.absolutePath}")
            Resource.Success(localFile)
        } catch (e: Exception) {
            Timber.e(e, "DOWNLOAD_FAILED")
            Resource.Error("Cloud download failed: ${e.message}")
        }
    }

    suspend fun listCloudBackupsForCurrentUser(): Resource<List<String>> {
        val user = auth.currentUser ?: return Resource.Error("User not logged in")
        return try {
            val listResult = storage.reference
                .child("backups/${user.uid}")
                .listAll()
                .await()
            
            val paths = listResult.items.map { it.path }
            Resource.Success(paths)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list backups")
            Resource.Error("List failed: ${e.message}")
        }
    }

    suspend fun deleteBackupFromFirebase(storagePath: String): Resource<Unit> {
        return try {
            storage.reference.child(storagePath).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete from Firebase: $storagePath")
            Resource.Error("Cloud deletion failed: ${e.message}")
        }
    }

    suspend fun restoreSelectedCloudBackup(storagePath: String): Resource<Unit> {
        return when (val downloadResult = downloadBackupFromFirebase(storagePath)) {
            is Resource.Success -> {
                val file = downloadResult.data!!
                val result = restoreLocalBackup(file)
                file.delete() // Clean up temp file
                result
            }
            is Resource.Error -> Resource.Error(downloadResult.message ?: "Download failed")
            else -> Resource.Error("Unknown error")
        }
    }

    suspend fun restoreLatestCloudBackup(): Resource<Unit> {
        return when (val listResult = listCloudBackupsForCurrentUser()) {
            is Resource.Success -> {
                val latest = listResult.data?.sortedDescending()?.firstOrNull()
                if (latest != null) {
                    restoreSelectedCloudBackup(latest)
                } else {
                    Resource.Error("No backups found")
                }
            }
            is Resource.Error -> Resource.Error(listResult.message ?: "List failed")
            else -> Resource.Error("Unknown error")
        }
    }
}
