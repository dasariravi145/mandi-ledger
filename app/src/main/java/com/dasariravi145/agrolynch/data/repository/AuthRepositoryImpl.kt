package com.dasariravi145.agrolynch.data.repository

import android.app.Activity
import com.dasariravi145.agrolynch.data.local.dao.UserDao
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.data.remote.model.FirestoreUserProfile
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.util.SecurityManager
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val securityManager: SecurityManager,
    private val settingsRepository: SettingsRepository,
    private val premiumStateManager: PremiumStateManager
) : AuthRepository {

    override fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>> = callbackFlow {
        Timber.tag("OtpFlow").d("Send OTP started for: $phoneNumber")
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Timber.tag("OtpFlow").d("OTP_AUTO_VERIFIED")
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                Timber.tag("OtpFlow").e(exception, "OTP_SEND_FAILED")
                trySend(Result.failure(exception))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Timber.tag("OtpFlow").d("OTP_CODE_SENT: $verificationId")
                trySend(Result.success(verificationId))
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Timber.tag("OtpFlow").e(e, "Error initiating verifyPhoneNumber")
            trySend(Result.failure(e))
        }
        awaitClose { }
    }

    override fun verifyOtp(verificationId: String, otp: String): Flow<Result<Unit>> = callbackFlow {
        Timber.tag("OtpFlow").d("Verify OTP started")
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.tag("ProfileCheck").d("OTP Verified")
                    trySend(Result.success(Unit))
                } else {
                    Timber.tag("OtpFlow").e(task.exception, "OTP verification failed")
                    trySend(Result.failure(task.exception ?: Exception("Verification failed")))
                }
                close()
            }
        awaitClose { }
    }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override fun logout() {
        auth.signOut()
        securityManager.clear()
    }

    override fun getCurrentUserPhoneNumber(): String? = auth.currentUser?.phoneNumber

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun checkUserExists(uid: String): Result<FirestoreUserProfile?> {
        Timber.tag("ProfileCheck").d("Current UID: $uid")
        Timber.tag("ProfileCheck").d("Firestore path: users/$uid")
        
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                Timber.tag("ProfileCheck").d("Document exists: true")
                val data = doc.data
                Timber.tag("ProfileCheck").d("Document fields: $data")

                val profile = doc.toObject(FirestoreUserProfile::class.java)
                
                // Comprehensive completion check
                val isCompleted = profile?.let {
                    it.isProfileCompleted || 
                    (it.fullName.isNotBlank() && it.mobileNumber.isNotBlank() && it.address.isNotBlank() && it.pinHash.isNotBlank())
                } ?: false

                Timber.tag("ProfileCheck").d("Completion flag (isProfileCompleted): ${profile?.isProfileCompleted}")
                Timber.tag("ProfileCheck").d("Final completion result: $isCompleted")

                Result.success(profile?.copy(isProfileCompleted = isCompleted))
            } else {
                Timber.tag("ProfileCheck").d("Document exists: false")
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.tag("ProfileCheck").e(e, "Firestore check failed")
            Result.failure(e)
        }
    }

    override suspend fun registerUser(fullName: String, address: String, pin: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

        val uid = currentUser.uid
        val mobile = currentUser.phoneNumber ?: ""
        
        val profile = FirestoreUserProfile(
            mobileNumber = mobile,
            fullName = fullName,
            address = address,
            pinHash = hashPin(pin),
            isProfileCompleted = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return try {
            firestore.collection("users").document(uid).set(profile).await()
            syncLocalProfileWithUid(uid, profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("RegistrationFlow").e(e, "Firestore write failed")
            Result.failure(e)
        }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val localUser = getLocalUser() ?: return false
        return localUser.pinHash == hashPin(pin)
    }

    override suspend fun syncLocalProfile(profile: FirestoreUserProfile) {
        val uid = getCurrentUserId() ?: profile.mobileNumber
        syncLocalProfileWithUid(uid, profile)
    }
    
    private suspend fun syncLocalProfileWithUid(uid: String, profile: FirestoreUserProfile) {
        val userEntity = UserEntity(
            id = uid,
            name = profile.fullName,
            phoneNumber = profile.mobileNumber,
            location = profile.address,
            pinHash = profile.pinHash,
            isPremium = profile.isPremium,
            cloudBackupEnabled = profile.backupEnabled,
            isProfileCompleted = profile.isProfileCompleted,
            createdAt = profile.createdAt
        )
        userDao.insertUser(userEntity)
        securityManager.saveSession(
            uid, 
            profile.mobileNumber, 
            profile.fullName, 
            profile.address, 
            profile.pinHash,
            isHashed = true
        )
        // Ensure biometric preference is synced from Firestore during profile restore
        securityManager.setBiometricEnabled(profile.biometricEnabled)
        settingsRepository.updateLanguage(profile.language)
        
        // Restore premium status locally from Firestore profile
        premiumStateManager.updatePremiumStatus(profile.isPremium, profile.premiumExpiry)
        
        Timber.tag("ProfileCheck").d("Profile synced: biometricEnabled=${profile.biometricEnabled}, isPremium=${profile.isPremium}")
        Timber.tag("ProfileCheck").d("Saved Locally")
    }

    override suspend fun getLocalUser(): UserEntity? {
        val uid = getCurrentUserId() ?: return null
        return userDao.getUserById(uid)
    }

    override fun hashPin(pin: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    override suspend fun updatePin(newPin: String): Result<Unit> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
        val hashedPin = hashPin(newPin)
        return try {
            firestore.collection("users").document(uid)
                .update(
                    "pinHash", hashedPin,
                    "updatedAt", System.currentTimeMillis()
                )
                .await()
            
            val localUser = userDao.getUserById(uid)
            localUser?.let {
                userDao.insertUser(it.copy(pinHash = hashedPin))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("RegistrationFlow").e(e, "UPDATE_PIN_FAILED")
            Result.failure(e)
        }
    }

    override suspend fun updateBiometricEnabled(enabled: Boolean): Result<Unit> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestore.collection("users").document(uid)
                .update(
                    "biometricEnabled", enabled,
                    "updatedAt", System.currentTimeMillis()
                )
                .await()
            securityManager.setBiometricEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("BiometricFlow").e(e, "UPDATE_BIOMETRIC_FAILED")
            Result.failure(e)
        }
    }
}
