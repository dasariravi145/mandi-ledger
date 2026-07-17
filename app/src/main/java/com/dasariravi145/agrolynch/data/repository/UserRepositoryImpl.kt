package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.UserDao
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.data.remote.model.FirestoreUserProfile
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import com.dasariravi145.agrolynch.util.Resource
import com.dasariravi145.agrolynch.util.SecurityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val securityManager: SecurityManager,
    private val settingsRepository: SettingsRepository
) : UserRepository {

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getUserProfile(): Flow<UserEntity?> = userDao.getUserByIdFlow(userId ?: "")

    override suspend fun saveProfile(user: UserEntity): Resource<Unit> {
        return try {
            userDao.insertUser(user)
            syncProfileToCloud()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save profile")
        }
    }

    override suspend fun syncProfileToCloud(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        val localUser = userDao.getUserById(uid) ?: return Resource.Error("Local profile not found")
        
        return try {
            val firestoreProfile = FirestoreUserProfile(
                mobileNumber = localUser.phoneNumber,
                fullName = localUser.name,
                address = localUser.location,
                pinHash = localUser.pinHash,
                biometricEnabled = securityManager.isBiometricEnabled(),
                language = settingsRepository.languageCode.first(),
                isPremium = localUser.isPremium,
                premiumPlan = localUser.premiumPlan,
                premiumStartDate = localUser.premiumStartDate,
                premiumExpiryDate = localUser.premiumExpiryDate,
                purchaseToken = localUser.purchaseToken,
                productId = localUser.productId,
                premiumExpiry = localUser.premiumExpiry,
                backupEnabled = localUser.cloudBackupEnabled,
                multiDeviceSyncEnabled = localUser.multiDeviceSyncEnabled,
                voiceEntryEnabled = localUser.voiceEntryEnabled,
                ocrEnabled = localUser.ocrEnabled,
                ocrCloudStorageEnabled = localUser.ocrCloudStorageEnabled,
                pdfCloudStorageEnabled = localUser.pdfCloudStorageEnabled,
                isProfileCompleted = localUser.isProfileCompleted,
                updatedAt = System.currentTimeMillis()
            )
            // Use UID as document ID to match AuthRepositoryImpl
            firestore.collection("users").document(uid).set(firestoreProfile).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync failed")
            Resource.Error("Cloud sync failed: ${e.message}")
        }
    }

    override suspend fun fetchProfileFromCloud(): Resource<UserEntity?> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            // Use UID as document ID to match AuthRepositoryImpl
            val doc = firestore.collection("users").document(uid).get().await()
            val profile = doc.toObject(FirestoreUserProfile::class.java)
            val userEntity = profile?.let {
                UserEntity(
                    id = uid, // Use UID
                    name = it.fullName,
                    phoneNumber = it.mobileNumber,
                    location = it.address,
                    pinHash = it.pinHash,
                    isPremium = it.isPremium,
                    premiumPlan = it.premiumPlan,
                    premiumStartDate = it.premiumStartDate,
                    premiumExpiryDate = it.premiumExpiryDate,
                    purchaseToken = it.purchaseToken,
                    productId = it.productId,
                    premiumExpiry = it.premiumExpiry,
                    cloudBackupEnabled = it.backupEnabled,
                    multiDeviceSyncEnabled = it.multiDeviceSyncEnabled,
                    voiceEntryEnabled = it.voiceEntryEnabled,
                    ocrEnabled = it.ocrEnabled,
                    ocrCloudStorageEnabled = it.ocrCloudStorageEnabled,
                    pdfCloudStorageEnabled = it.pdfCloudStorageEnabled,
                    isProfileCompleted = it.isProfileCompleted,
                    createdAt = it.createdAt
                )
            }
            if (userEntity != null) {
                userDao.insertUser(userEntity)
                securityManager.saveSession(
                    uid,
                    profile.mobileNumber,
                    profile.fullName,
                    profile.address,
                    profile.pinHash,
                    isHashed = true
                )
                securityManager.setBiometricEnabled(profile.biometricEnabled)
                settingsRepository.updateLanguage(profile.language)
            }
            Resource.Success(userEntity)
        } catch (e: Exception) {
            Timber.e(e, "Fetch failed")
            Resource.Error("Fetch failed: ${e.message}")
        }
    }
}
