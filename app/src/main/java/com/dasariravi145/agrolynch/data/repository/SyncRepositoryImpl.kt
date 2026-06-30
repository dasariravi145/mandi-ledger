package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.remote.model.*
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val premiumStateManager: PremiumStateManager,
    private val farmerDao: FarmerDao,
    private val buyerDao: BuyerDao,
    private val productDao: ProductDao,
    private val arrivalDao: ArrivalDao,
    private val saleDao: SaleDao,
    private val paymentDao: PaymentDao,
    private val ocrScanDao: OcrScanDao,
    private val expenseDao: ExpenseDao
) : SyncRepository {

    private val userId: String?
        get() = auth.currentUser?.uid

    override suspend fun syncAllData(): Resource<Unit> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext Resource.Error("User not logged in")
        if (!premiumStateManager.isPremium.first()) return@withContext Resource.Error("Premium subscription required for sync")

        return@withContext try {
            val startTime = System.currentTimeMillis()
            syncFarmers(uid)
            syncBuyers(uid)
            syncProducts(uid)
            syncArrivals(uid)
            syncSales(uid)
            syncPayments(uid)
            syncOcrScans(uid)
            syncExpenses(uid)
            timber.log.Timber.i("Full Sync: Completed in %dms", System.currentTimeMillis() - startTime)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    private suspend fun syncFarmers(uid: String) {
        val farmers = farmerDao.getAllFarmers().first()
        val batch = firestore.batch()
        farmers.forEach { farmer ->
            val firestoreFarmer = FirestoreFarmer(
                farmerId = farmer.id,
                ownerUserId = uid,
                name = farmer.name,
                phone = farmer.mobileNumber,
                village = farmer.village,
                createdAt = 0L, 
                updatedAt = farmer.lastUpdated
            )
            val docRef = firestore.collection("users").document(uid).collection("farmers").document(farmer.id)
            batch.set(docRef, firestoreFarmer)
        }
        batch.commit().await()
    }

    private suspend fun syncBuyers(uid: String) {
        val buyers = buyerDao.getAllBuyers().first()
        val batch = firestore.batch()
        buyers.forEach { buyer ->
            val firestoreBuyer = FirestoreBuyer(
                buyerId = buyer.id,
                ownerUserId = uid,
                name = buyer.name,
                phone = buyer.mobileNumber,
                address = buyer.address,
                createdAt = 0L,
                updatedAt = buyer.lastUpdated
            )
            val docRef = firestore.collection("users").document(uid).collection("buyers").document(buyer.id)
            batch.set(docRef, firestoreBuyer)
        }
        batch.commit().await()
    }

    private suspend fun syncProducts(uid: String) {
        val products = productDao.getAllProducts().first()
        products.forEach { product ->
            val firestoreProduct = FirestoreProduct(
                productId = product.id,
                ownerUserId = uid,
                productName = product.name,
                category = product.category,
                grade = product.availableGrades.joinToString(","),
                unit = "", 
                createdAt = 0L,
                updatedAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).collection("products").document(product.id).set(firestoreProduct).await()
        }
    }

    private suspend fun syncArrivals(uid: String) {
        val arrivals = arrivalDao.getAllArrivals().first()
        arrivals.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { arrival ->
                val firestoreStock = FirestoreStockEntry(
                    stockId = arrival.id,
                    ownerUserId = uid,
                    farmerId = arrival.farmerId,
                    productId = arrival.productId,
                    category = arrival.productCategory,
                    grade = arrival.grade,
                    quantity = arrival.quantity,
                    rate = arrival.purchaseRate,
                    laborCharges = 0.0, 
                    transportCharges = 0.0,
                    grossAmount = arrival.grossAmount,
                    netAmount = arrival.netAmount,
                    createdAt = arrival.date
                )
                val docRef = firestore.collection("users").document(uid).collection("arrivals").document(arrival.id)
                batch.set(docRef, firestoreStock)
            }
            batch.commit().await()
        }
    }

    private suspend fun syncSales(uid: String) {
        val sales = saleDao.getAllSales().first()
        sales.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { sale ->
                val firestoreSale = FirestoreSale(
                    saleId = sale.id,
                    ownerUserId = uid,
                    buyerId = sale.buyerId,
                    productId = sale.productId,
                    category = "General", 
                    grade = sale.grade,
                    quantity = sale.totalQuantity,
                    rate = 0.0, 
                    grossAmount = sale.totalAmount,
                    commissionAmount = sale.totalCommission,
                    laborCharges = sale.laborCharges,
                    transportCharges = sale.transportCharges,
                    netAmount = sale.totalNetAmount,
                    createdAt = sale.date
                )
                val docRef = firestore.collection("users").document(uid).collection("sales").document(sale.id)
                batch.set(docRef, firestoreSale)
            }
            batch.commit().await()
        }
    }

    private suspend fun syncPayments(uid: String) {
        val payments = paymentDao.getAllPayments().first()
        payments.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { payment ->
                val firestorePayment = FirestorePayment(
                    paymentId = payment.id,
                    ownerUserId = uid,
                    partyId = payment.partyId,
                    partyType = payment.partyType,
                    amount = payment.amount,
                    paymentMode = payment.paymentMode,
                    remarks = payment.notes,
                    createdAt = payment.date
                )
                val docRef = firestore.collection("users").document(uid).collection("payments").document(payment.id)
                batch.set(docRef, firestorePayment)
            }
            batch.commit().await()
        }
    }

    private suspend fun syncOcrScans(uid: String) {
        val scans = ocrScanDao.getAllScans().first()
        scans.forEach { scan ->
            val firestoreOcr = FirestoreOcrScan(
                scanId = scan.scanId,
                ownerUserId = uid,
                billNumber = scan.billNumber,
                billDate = scan.billDate,
                amount = scan.amount,
                ocrText = scan.ocrText,
                imageUrl = scan.imageUrl,
                scanType = scan.transactionType,
                createdAt = scan.createdAt
            )
            firestore.collection("users").document(uid).collection("ocr_scans").document(scan.scanId).set(firestoreOcr).await()
        }
    }

    private suspend fun syncExpenses(uid: String) {
        val expenses = expenseDao.getAllExpenses().first()
        expenses.forEach { expense ->
            val firestoreExpense = FirestoreExpense(
                expenseId = expense.id,
                ownerUserId = uid,
                category = expense.type,
                amount = expense.amount,
                description = expense.description,
                date = expense.date,
                lastUpdated = expense.lastUpdated,
                isDeleted = expense.isDeleted
            )
            firestore.collection("users").document(uid).collection("expenses").document(expense.id).set(firestoreExpense).await()
        }
    }

    override suspend fun restoreAllData(): Resource<Unit> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext Resource.Error("User not logged in")
        
        return@withContext try {
            val startTime = System.currentTimeMillis()
            // Restore Farmers
            val farmerDocs = firestore.collection("users").document(uid).collection("farmers").get().await()
            farmerDocs.documents.forEach { doc ->
                val firestoreFarmer = doc.toObject(FirestoreFarmer::class.java)
                firestoreFarmer?.let {
                    farmerDao.insertFarmer(com.dasariravi145.agrolynch.data.local.entity.FarmerEntity(
                        id = it.farmerId,
                        name = it.name,
                        mobileNumber = it.phone,
                        village = it.village,
                        lastUpdated = it.updatedAt,
                        isSynced = true
                    ))
                }
            }

            // Restore Buyers
            val buyerDocs = firestore.collection("users").document(uid).collection("buyers").get().await()
            buyerDocs.documents.forEach { doc ->
                val firestoreBuyer = doc.toObject(FirestoreBuyer::class.java)
                firestoreBuyer?.let {
                    buyerDao.insertBuyer(com.dasariravi145.agrolynch.data.local.entity.BuyerEntity(
                        id = it.buyerId,
                        name = it.name,
                        mobileNumber = it.phone,
                        address = it.address,
                        lastUpdated = it.updatedAt,
                        isSynced = true
                    ))
                }
            }

            // Restore Products
            val productDocs = firestore.collection("users").document(uid).collection("products").get().await()
            productDocs.documents.forEach { doc ->
                val firestoreProduct = doc.toObject(FirestoreProduct::class.java)
                firestoreProduct?.let {
                    productDao.insertProduct(com.dasariravi145.agrolynch.data.local.entity.ProductEntity(
                        id = it.productId,
                        name = it.productName,
                        category = it.category,
                        availableGrades = it.grade.split(","),
                        isSynced = true
                    ))
                }
            }

            // Restore Arrivals and recalculate stock (Fixes M1)
            val arrivalDocs = firestore.collection("users").document(uid).collection("arrivals").get().await()
            val saleDocs = firestore.collection("users").document(uid).collection("sales").get().await()
            
            val firestoreArrivals = arrivalDocs.documents.mapNotNull { it.toObject(FirestoreStockEntry::class.java) }
            val firestoreSales = saleDocs.documents.mapNotNull { it.toObject(FirestoreSale::class.java) }

            firestoreArrivals.forEach { arrival ->
                // Recalculate remaining stock based on sales synced in cloud
                val totalSoldKg = firestoreSales
                    .filter { it.productId == arrival.productId && it.grade == arrival.grade }
                    .sumOf { it.quantity }
                
                val remaining = maxOf(0.0, arrival.quantity - totalSoldKg)

                arrivalDao.insertArrival(com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity(
                    id = arrival.stockId,
                    farmerId = arrival.farmerId,
                    productId = arrival.productId,
                    productCategory = arrival.category,
                    grade = arrival.grade,
                    quantity = arrival.quantity,
                    remainingQuantity = remaining, 
                    purchaseRate = arrival.rate,
                    grossAmount = arrival.grossAmount,
                    netAmount = arrival.netAmount,
                    date = arrival.createdAt,
                    isSynced = true
                ))
            }

            // Restore Sales
            firestoreSales.forEach {
                saleDao.insertSale(com.dasariravi145.agrolynch.data.local.entity.SaleEntity(
                    id = it.saleId,
                    buyerId = it.buyerId,
                    productId = it.productId,
                    productName = "Restored Sale",
                    grade = it.grade,
                    totalQuantity = it.quantity,
                    totalAmount = it.grossAmount,
                    transportCharges = it.transportCharges,
                    laborCharges = it.laborCharges,
                    totalCommission = it.commissionAmount,
                    totalNetAmount = it.netAmount,
                    pendingAmount = it.netAmount,
                    date = it.createdAt,
                    isSynced = true
                ))
            }

            // Restore Payments
            val paymentDocs = firestore.collection("users").document(uid).collection("payments").get().await()
            paymentDocs.documents.forEach { doc ->
                val it = doc.toObject(FirestorePayment::class.java)
                it?.let {
                    paymentDao.insertPayment(com.dasariravi145.agrolynch.data.local.entity.PaymentEntity(
                        id = it.paymentId,
                        partyId = it.partyId,
                        partyType = it.partyType,
                        amount = it.amount,
                        paymentMode = it.paymentMode,
                        notes = it.remarks,
                        date = it.createdAt,
                        isSynced = true
                    ))
                }
            }

            // Restore OCR Scans
            val ocrDocs = firestore.collection("users").document(uid).collection("ocr_scans").get().await()
            ocrDocs.documents.forEach { doc ->
                val it = doc.toObject(FirestoreOcrScan::class.java)
                it?.let {
                    ocrScanDao.insertScan(com.dasariravi145.agrolynch.data.local.entity.OcrScanEntity(
                        scanId = it.scanId,
                        billNumber = it.billNumber,
                        billDate = it.billDate,
                        amount = it.amount,
                        ocrText = it.ocrText,
                        imageUrl = it.imageUrl,
                        transactionType = it.scanType,
                        createdAt = it.createdAt
                    ))
                }
            }

            // Restore Expenses
            val expenseDocs = firestore.collection("users").document(uid).collection("expenses").get().await()
            expenseDocs.documents.forEach { doc ->
                val it = doc.toObject(FirestoreExpense::class.java)
                it?.let {
                    expenseDao.insertExpense(com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity(
                        id = it.expenseId,
                        type = it.category,
                        amount = it.amount,
                        description = it.description,
                        date = it.date,
                        lastUpdated = it.lastUpdated,
                        isDeleted = it.isDeleted,
                        isSynced = true
                    ))
                }
            }
            
            timber.log.Timber.i("Restore All Data: Completed in %dms", System.currentTimeMillis() - startTime)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Restore failed: ${e.message}")
        }
    }

    override suspend fun uploadFile(file: File, remotePath: String): Resource<String> {
        val uid = userId ?: return Resource.Error("User not logged in. Please login again.")
        if (!premiumStateManager.isPremium.first()) return Resource.Error("Premium subscription required for cloud storage")

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storagePath = "$remotePath/$uid/${timestamp}_${file.name}"

        timber.log.Timber.d("Upload Started: Path=$storagePath, File=${file.absolutePath}, Size=${file.length()}")

        if (!file.exists()) {
            timber.log.Timber.e("Upload Failed: File Not Found at ${file.absolutePath}")
            return Resource.Error("Local file not found")
        }

        if (file.length() == 0L) {
            timber.log.Timber.e("Upload Failed: File is empty")
            return Resource.Error("File is empty")
        }

        return try {
            val ref = storage.reference.child(storagePath)
            
            timber.log.Timber.d("Starting Firebase Storage putFile to: ${ref.path}")
            val uploadSnapshot = ref.putFile(Uri.fromFile(file)).await()
            
            if (uploadSnapshot.task.isSuccessful) {
                timber.log.Timber.i("Upload Success: ${uploadSnapshot.bytesTransferred} bytes transferred")
                
                var downloadUrl: String? = null
                var lastError: Exception? = null
                for (i in 1..3) {
                    try {
                        val uri = ref.downloadUrl.await()
                        downloadUrl = uri.toString()
                        break
                    } catch (e: Exception) {
                        lastError = e
                        timber.log.Timber.w("Download URL attempt $i failed: ${e.message}")
                        kotlinx.coroutines.delay(1000L * i)
                    }
                }

                if (downloadUrl != null) {
                    timber.log.Timber.d("Generated Download URL: $downloadUrl")
                    Resource.Success(downloadUrl)
                } else {
                    throw lastError ?: Exception("Download URL generation failed after upload")
                }
            } else {
                val error = uploadSnapshot.task.exception?.message ?: "Unknown upload failure"
                timber.log.Timber.e("Upload Snapshot Failed: $error")
                Resource.Error(error)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Sync Upload failed"
            timber.log.Timber.e(e, "Sync Upload Error at path: $storagePath")
            
            if (msg.contains("Object does not exist", ignoreCase = true)) {
                timber.log.Timber.e("CRITICAL: Firebase reports object missing immediately after upload. Check storage rules or consistency.")
            }

            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            Resource.Error(msg)
        }
    }

    override suspend fun downloadFile(remotePath: String, localFile: File): Resource<File> {
        return try {
            val ref = storage.getReferenceFromUrl(remotePath)
            ref.getFile(localFile).await()
            Resource.Success(localFile)
        } catch (e: Exception) {
            Resource.Error("Download failed: ${e.message}")
        }
    }

    override suspend fun saveUserProfile(profile: FirestoreUserProfile): Resource<Unit> {
        return try {
            firestore.collection("users").document(profile.userId).set(profile).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save profile: ${e.message}")
        }
    }

    override fun isSyncEnabled(): Flow<Boolean> = premiumStateManager.isPremium
}
