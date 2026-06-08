package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id = 1")
    fun getProfile(): Flow<CompanyProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: CompanyProfileEntity)

    @Query("UPDATE company_profile SET nextBillNumber = nextBillNumber + 1 WHERE id = 1")
    suspend fun incrementBillNumber()

    @Query("UPDATE company_profile SET nextInvoiceNumber = nextInvoiceNumber + 1 WHERE id = 1")
    suspend fun incrementInvoiceNumber()

    @Query("UPDATE company_profile SET nextReceiptNumber = nextReceiptNumber + 1 WHERE id = 1")
    suspend fun incrementReceiptNumber()
}
