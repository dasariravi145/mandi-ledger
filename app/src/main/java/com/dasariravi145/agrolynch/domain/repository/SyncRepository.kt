package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SyncRepository {
    suspend fun syncAllData(): Resource<Unit>
    suspend fun restoreAllData(): Resource<Unit>
    suspend fun uploadFile(file: File, remotePath: String): Resource<String>
    suspend fun downloadFile(remotePath: String, localFile: File): Resource<File>
    suspend fun saveUserProfile(profile: com.dasariravi145.agrolynch.data.remote.model.FirestoreUserProfile): Resource<Unit>
    fun isSyncEnabled(): Flow<Boolean>
}
