package com.dasariravi145.agrolynch.data.repository

import android.graphics.Bitmap
import com.dasariravi145.agrolynch.data.local.dao.OcrScanDao
import com.dasariravi145.agrolynch.data.local.entity.OcrScanEntity
import com.dasariravi145.agrolynch.data.remote.model.FirestoreOcrScan
import com.dasariravi145.agrolynch.domain.repository.OcrRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val ocrScanDao: OcrScanDao,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OcrRepository {

    override fun getScanHistory(): Flow<List<OcrScanEntity>> = ocrScanDao.getAllScans()

    override suspend fun saveScan(scan: OcrScanEntity): Resource<Unit> {
        return try {
            ocrScanDao.insertScan(scan)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save scan")
        }
    }

    override suspend fun saveScanWithImage(scan: OcrScanEntity, bitmap: Bitmap?): Resource<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            var finalScan = scan
            
            // Premium Logic: Save to Firebase if bitmap is present and user is logged in
            val uid = auth.currentUser?.uid
            if (uid != null && bitmap != null) {
                val fileName = "${scan.scanId}.jpg"
                val storagePath = "ocr_images/$uid/$fileName"
                val storageRef = storage.reference.child(storagePath)
                
                Timber.d("Uploading OCR image to Cloud: Path=$storagePath, User=$uid")
                
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val data = baos.toByteArray()
                
                if (data.isEmpty()) {
                    Timber.e("Upload failed: Compressed bitmap data is empty")
                } else {
                    Timber.d("Starting Firebase Storage putBytes to: $storagePath")
                    val uploadSnapshot = storageRef.putBytes(data).await()
                    
                    var downloadUrl: String? = null
                    var lastError: Exception? = null
                    for (i in 1..3) {
                        try {
                            val uri = uploadSnapshot.metadata?.reference?.downloadUrl?.await()
                            downloadUrl = uri?.toString()
                            if (downloadUrl != null) break
                        } catch (e: Exception) {
                            lastError = e
                            kotlinx.coroutines.delay(1000L * i)
                        }
                    }

                    if (downloadUrl != null) {
                        Timber.d("Generated Download URL: $downloadUrl")
                        finalScan = scan.copy(imageUrl = downloadUrl)
                        
                        // Sync record to Firestore (Top-level collection with ownerUserId)
                        val firestoreScan = FirestoreOcrScan(
                            scanId = scan.scanId,
                            ownerUserId = uid,
                            billNumber = scan.billNumber,
                            billDate = scan.billDate,
                            amount = scan.amount,
                            ocrText = scan.ocrText,
                            imageUrl = downloadUrl,
                            scanType = scan.transactionType,
                            createdAt = scan.createdAt
                        )
                        firestore.collection("users").document(uid).collection("ocr_scans").document(scan.scanId).set(firestoreScan).await()
                    } else {
                        Timber.e(lastError, "Download URL generation failed after retries")
                        if (lastError?.message?.contains("Object does not exist", ignoreCase = true) == true) {
                            Timber.e("CRITICAL: Object missing at path: $storagePath immediately after successful putBytes.")
                        }
                    }
                }
            }
            
            ocrScanDao.insertScan(finalScan)
            Resource.Success(Unit)
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to save scan with image"
            Timber.e(e, "OCR Scan process failed")
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            Resource.Error(msg)
        }
    }

    override suspend fun getScanById(id: String): OcrScanEntity? = ocrScanDao.getScanById(id)

    override suspend fun syncScans(): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = ocrScanDao.getAllScans().first() // In a real app, use getUnsynced
            for (scan in unsynced) {
                val firestoreScan = FirestoreOcrScan(
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
                firestore.collection("users").document(uid).collection("ocr_scans").document(scan.scanId).set(firestoreScan).await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sync failed")
        }
    }
}
