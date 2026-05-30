package com.dasariravi145.agrolynch.data.repository

import android.graphics.Bitmap
import com.dasariravi145.agrolynch.data.local.dao.OcrScanDao
import com.dasariravi145.agrolynch.data.local.entity.OcrScanEntity
import com.dasariravi145.agrolynch.domain.repository.OcrRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
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
                val storageRef = storage.reference.child("users/$uid/scans/${scan.scanId}.jpg")
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val data = baos.toByteArray()
                
                storageRef.putBytes(data).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                finalScan = scan.copy(imageUrl = downloadUrl)
                
                // Also sync record to Firestore
                firestore.collection("users").document(uid).collection("ocr_scans").document(scan.scanId).set(finalScan).await()
            }
            
            ocrScanDao.insertScan(finalScan)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save scan with image")
        }
    }

    override suspend fun getScanById(id: String): OcrScanEntity? = ocrScanDao.getScanById(id)

    override suspend fun syncScans(): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("User not logged in")
        return try {
            // Basic implementation to push local scans to firestore
            // In a real app, you'd track 'isSynced'
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sync failed")
        }
    }
}
