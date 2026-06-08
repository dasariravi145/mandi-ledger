package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.CompanyProfileDao
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepositoryImpl @Inject constructor(
    private val dao: CompanyProfileDao
) : CompanyRepository {
    override fun getProfile(): Flow<CompanyProfileEntity?> = dao.getProfile()

    override suspend fun updateProfile(profile: CompanyProfileEntity): Resource<Unit> {
        return try {
            dao.updateProfile(profile.copy(lastUpdated = System.currentTimeMillis()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    override suspend fun incrementBillNumber() {
        dao.incrementBillNumber()
    }
}
