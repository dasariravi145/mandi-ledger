package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.remote.model.*
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
    private val ocrScanDao: OcrScanDao
) : SyncRepository {

    private val userId: String?
        get() = auth.currentUser?.uid

    override suspend fun syncAllData(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        if (!premiumStateManager.isPremium.first()) return Resource.Error("Premium subscription required for sync")

        return try {
            syncFarmers(uid)
            syncBuyers(uid)
            syncProducts(uid)
            syncArrivals(uid)
            syncSales(uid)
            syncPayments(uid)
            syncOcrScans(uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    private suspend fun syncFarmers(uid: String) {
        val farmers = farmerDao.getAllFarmers().first()
        farmers.forEach { farmer ->
            val firestoreFarmer = FirestoreFarmer(
                farmerId = farmer.id,
                ownerUserId = uid,
                name = farmer.name,
                phone = farmer.mobileNumber,
                village = farmer.village,
                createdAt = 0L, // Should ideally be in Entity
                updatedAt = farmer.lastUpdated
            )
            firestore.collection("farmers").document(farmer.id).set(firestoreFarmer).await()
        }
    }

    private suspend fun syncBuyers(uid: String) {
        val buyers = buyerDao.getAllBuyers().first()
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
            firestore.collection("buyers").document(buyer.id).set(firestoreBuyer).await()
        }
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
                unit = "", // Assuming unit is handled elsewhere or needed in Entity
                createdAt = 0L,
                updatedAt = System.currentTimeMillis()
            )
            firestore.collection("products").document(product.id).set(firestoreProduct).await()
        }
    }

    private suspend fun syncArrivals(uid: String) {
        val arrivals = arrivalDao.getAllArrivals().first()
        arrivals.forEach { arrival ->
            val firestoreStock = FirestoreStockEntry(
                stockId = arrival.id,
                ownerUserId = uid,
                farmerId = arrival.farmerId,
                productId = arrival.productId,
                category = arrival.productCategory,
                grade = arrival.grade,
                quantity = arrival.quantity,
                rate = arrival.purchaseRate,
                laborCharges = 0.0, // Not in ArrivalEntity currently
                transportCharges = 0.0,
                grossAmount = arrival.grossAmount,
                netAmount = arrival.netAmount,
                createdAt = arrival.date
            )
            firestore.collection("stock_entries").document(arrival.id).set(firestoreStock).await()
        }
    }

    private suspend fun syncSales(uid: String) {
        val sales = saleDao.getAllSales().first()
        sales.forEach { sale ->
            val firestoreSale = FirestoreSale(
                saleId = sale.id,
                ownerUserId = uid,
                buyerId = sale.buyerId,
                productId = sale.productId,
                category = "General", // Placeholder
                grade = sale.grade,
                quantity = sale.totalQuantity,
                rate = 0.0, // Should be calculated or in Entity
                grossAmount = sale.totalAmount,
                commissionAmount = 0.0,
                laborCharges = sale.otherCharges,
                transportCharges = sale.transportCharges,
                netAmount = sale.totalAmount + sale.transportCharges + sale.otherCharges,
                createdAt = sale.date
            )
            firestore.collection("sales").document(sale.id).set(firestoreSale).await()
        }
    }

    private suspend fun syncPayments(uid: String) {
        val payments = paymentDao.getAllPayments().first()
        payments.forEach { payment ->
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
            firestore.collection("payments").document(payment.id).set(firestorePayment).await()
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
            firestore.collection("ocr_scans").document(scan.scanId).set(firestoreOcr).await()
        }
    }

    override suspend fun restoreAllData(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        
        return try {
            // Restore Farmers
            val farmerDocs = firestore.collection("farmers").whereEqualTo("ownerUserId", uid).get().await()
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
            val buyerDocs = firestore.collection("buyers").whereEqualTo("ownerUserId", uid).get().await()
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
            val productDocs = firestore.collection("products").whereEqualTo("ownerUserId", uid).get().await()
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

            // Similarly for Arrivals, Sales, Payments, and OCR scans...
            
            // Restore Arrivals
            val arrivalDocs = firestore.collection("stock_entries").whereEqualTo("ownerUserId", uid).get().await()
            arrivalDocs.documents.forEach { doc ->
                val it = doc.toObject(FirestoreStockEntry::class.java)
                it?.let {
                    arrivalDao.insertArrival(com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity(
                        id = it.stockId,
                        farmerId = it.farmerId,
                        productId = it.productId,
                        productCategory = it.category,
                        grade = it.grade,
                        quantity = it.quantity,
                        remainingQuantity = it.quantity, // Reset for safety or fetch correct value
                        purchaseRate = it.rate,
                        grossAmount = it.grossAmount,
                        netAmount = it.netAmount,
                        date = it.createdAt,
                        isSynced = true
                    ))
                }
            }

            // Restore Sales
            val saleDocs = firestore.collection("sales").whereEqualTo("ownerUserId", uid).get().await()
            saleDocs.documents.forEach { doc ->
                val it = doc.toObject(FirestoreSale::class.java)
                it?.let {
                    saleDao.insertSale(com.dasariravi145.agrolynch.data.local.entity.SaleEntity(
                        id = it.saleId,
                        buyerId = it.buyerId,
                        productId = it.productId,
                        grade = it.grade,
                        totalQuantity = it.quantity,
                        totalAmount = it.grossAmount,
                        transportCharges = it.transportCharges,
                        otherCharges = it.laborCharges,
                        pendingAmount = it.netAmount,
                        date = it.createdAt,
                        isSynced = true
                    ))
                }
            }

            // Restore Payments
            val paymentDocs = firestore.collection("payments").whereEqualTo("ownerUserId", uid).get().await()
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
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Restore failed: ${e.message}")
        }
    }

    override suspend fun uploadFile(file: File, remotePath: String): Resource<String> {
        val uid = userId ?: return Resource.Error("User not logged in")
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
