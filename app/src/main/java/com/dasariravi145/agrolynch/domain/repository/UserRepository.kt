package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(): Flow<UserEntity?>
    suspend fun saveProfile(user: UserEntity): Resource<Unit>
    suspend fun syncProfileToCloud(): Resource<Unit>
    suspend fun fetchProfileFromCloud(): Resource<UserEntity?>
}
