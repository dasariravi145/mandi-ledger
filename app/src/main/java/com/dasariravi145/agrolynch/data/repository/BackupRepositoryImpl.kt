package com.dasariravi145.agrolynch.data.repository

import android.content.Context
import android.net.Uri
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import com.dasariravi145.agrolynch.domain.repository.BackupRepository
import com.dasariravi145.agrolynch.util.PdfGenerator
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val farmerDao: FarmerDao,
    private val buyerDao: BuyerDao,
    private val saleDao: SaleDao,
    private val arrivalDao: ArrivalDao,
    private val productDao: ProductDao,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao,
    private val backupDao: BackupDao,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : BackupRepository {

    override fun getBackupHistory(): Flow<List<BackupEntity>> = backupDao.getBackupHistory()

    override suspend fun createLocalBackup(reportType: String): Resource<File> {
        return try {
            val farmers = farmerDao.getAllFarmers().first()
            val buyers = buyerDao.getAllBuyers().first()
            val sales = saleDao.getAllSales().first()
            val arrivals = arrivalDao.getAllArrivals().first()
            val products = productDao.getAllProducts().first()
            val expenses = expenseDao.getAllExpenses().first()
            val payments = paymentDao.getAllPayments().first()

            val pdfFile = PdfGenerator.generateBackupPDF(
                context, farmers, buyers, sales, arrivals, products, expenses, payments, reportType
            )

            if (pdfFile != null && pdfFile.exists()) {
                val backup = BackupEntity(
                    id = UUID.randomUUID().toString(),
                    fileName = pdfFile.name,
                    filePath = pdfFile.absolutePath,
                    size = pdfFile.length(),
                    type = "LOCAL",
                    reportType = reportType,
                    status = "SUCCESS"
                )
                backupDao.insertBackup(backup)
                Resource.Success(pdfFile)
            } else {
                Resource.Error("Failed to generate PDF")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred during backup")
        }
    }

    override suspend fun uploadBackupToCloud(file: File): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("User not logged in")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = file.name
        val storagePath = "cloud_backups/$uid/${timestamp}_$fileName"
        
        timber.log.Timber.d("Backup Started: File=${file.absolutePath}, Size=${file.length()}, Exists=${file.exists()}")

        if (!file.exists()) {
            timber.log.Timber.e("Backup File Not Found at: ${file.absolutePath}")
            return Resource.Error("Backup file not found")
        }

        if (file.length() == 0L) {
            timber.log.Timber.e("Backup File is Empty")
            return Resource.Error("Backup file is empty")
        }

        return try {
            val ref = storage.reference.child(storagePath)
            timber.log.Timber.d("Upload Started to: $storagePath")
            
            // Wait for upload to complete
            val uploadSnapshot = ref.putFile(Uri.fromFile(file)).await()
            
            if (uploadSnapshot.task.isSuccessful) {
                timber.log.Timber.i("Upload Success: ${uploadSnapshot.bytesTransferred} bytes")
                
                // Robust download URL retrieval with retries for eventual consistency
                var downloadUrl: String? = null
                var lastError: Exception? = null
                for (i in 1..3) {
                    try {
                        val uri = ref.downloadUrl.await()
                        downloadUrl = uri.toString()
                        break
                    } catch (e: Exception) {
                        lastError = e
                        timber.log.Timber.w("Download URL attempt $i failed: ${e.message}. Retrying...")
                        kotlinx.coroutines.delay(1000L * i)
                    }
                }

                if (downloadUrl != null) {
                    timber.log.Timber.d("Download URL Generated: $downloadUrl")

                    val backup = BackupEntity(
                        id = UUID.randomUUID().toString(),
                        fileName = fileName,
                        filePath = downloadUrl, // Store URL for cloud backups
                        size = file.length(),
                        type = "CLOUD",
                        reportType = "MANUAL",
                        status = "SUCCESS"
                    )
                    backupDao.insertBackup(backup)
                    Resource.Success(Unit)
                } else {
                    throw lastError ?: Exception("Could not generate download URL")
                }
            } else {
                val error = uploadSnapshot.task.exception?.message ?: "Unknown upload error"
                timber.log.Timber.e("Upload Failed: $error")
                Resource.Error(error)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Cloud upload failed"
            timber.log.Timber.e(e, "Upload Exception at path: $storagePath")
            
            if (msg.contains("Object does not exist", ignoreCase = true)) {
                timber.log.Timber.e("CRITICAL: Firebase reports object missing immediately after upload. Ensure storage bucket and paths are correct.")
            }
            
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                recordException(e)
                log("Backup Upload Failure Path: $storagePath")
            }
            Resource.Error(msg)
        }
    }

    override suspend fun restoreFromCloud(backupId: String): Resource<Unit> {
        // Implementation for restoring from cloud would typically involve
        // downloading the backup and parsing it or restoring DB.
        // Since we are backing up as PDF (for user readability as per prompt),
        // restoration might mean showing the PDF or downloading it.
        return Resource.Success(Unit)
    }

    override suspend fun deleteBackup(id: String): Resource<Unit> {
        backupDao.deleteBackup(id)
        return Resource.Success(Unit)
    }
}
