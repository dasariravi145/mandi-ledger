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
    private val gson: Gson
) {
    private val backupDir = File(context.filesDir, "backups")

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    suspend fun createLocalBackup(): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val data = BackupData(
                companyProfile = listOfNotNull(database.companyProfileDao().getProfile().first()),
                farmers = database.farmerDao().getFarmersList(),
                buyers = database.buyerDao().getBuyersList(),
                products = database.productDao().getProductsList(),
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
                templatePositions = database.templatePositionDao().getAllPositionsList()
            )

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(backupDir, "backup_$timestamp.json")
            
            FileWriter(file).use { writer ->
                gson.toJson(data, writer)
            }
            
            Resource.Success(file)
        } catch (e: Exception) {
            Timber.e(e, "Local backup failed")
            Resource.Error("Local backup failed: ${e.message}")
        }
    }

    suspend fun restoreLocalBackup(file: File): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = FileReader(file).use { reader ->
                gson.fromJson(reader, BackupData::class.java)
            }

            database.withTransaction {
                data.companyProfile.forEach { database.companyProfileDao().insertProfile(it) }
                data.farmers.forEach { database.farmerDao().insertFarmer(it) }
                data.buyers.forEach { database.buyerDao().insertBuyer(it) }
                data.products.forEach { database.productDao().insertProduct(it) }
                data.arrivals.forEach { database.arrivalDao().insertArrival(it) }
                data.sales.forEach { database.saleDao().insertSale(it) }
                data.saleItems.forEach { database.saleDao().insertSaleItem(it) }
                data.payments.forEach { database.paymentDao().insertPayment(it) }
                data.expenses.forEach { database.expenseDao().insertExpense(it) }
                data.marketRates.forEach { database.marketRateDao().insertMarketRate(it) }
                data.transactions.forEach { database.transactionDao().insertTransaction(it) }
                data.ocrScans.forEach { database.ocrScanDao().insertScan(it) }
                data.boxWeightItems.forEach { database.boxWeightDao().insertItem(it) }
                data.billSeries.forEach { database.billNumberSeriesDao().insertSeries(it) }
                data.deductions.forEach { database.entryDeductionDao().insertDeduction(it) }
                data.templatePositions.forEach { database.templatePositionDao().insertPosition(it) }
            }
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Restore failed")
            Resource.Error("Restore failed: ${e.message}")
        }
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

    suspend fun restoreSelectedCloudBackup(storagePath: String): Resource<Unit> {
        return when (val downloadResult = downloadBackupFromFirebase(storagePath)) {
            is Resource.Success -> restoreLocalBackup(downloadResult.data!!)
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
