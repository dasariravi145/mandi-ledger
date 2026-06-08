package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface CompanyRepository {
    fun getProfile(): Flow<CompanyProfileEntity?>
    suspend fun updateProfile(profile: CompanyProfileEntity): Resource<Unit>
    suspend fun incrementBillNumber()
}
