package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "",
    val proprietorName: String = "",
    val mobile1: String = "",
    val mobile2: String = "",
    val address: String = "",
    val village: String = "",
    val district: String = "",
    val state: String = "",
    val gstNumber: String = "",
    val licenseNumber: String = "",
    val logoPath: String? = null,
    val godImagePath: String? = null,
    val signaturePath: String? = null,
    val stampPath: String? = null,
    val billPrefix: String = "BILL",
    val startingBillNumber: Int = 1,
    val nextBillNumber: Int = 1,
    val nextInvoiceNumber: Int = 1,
    val nextReceiptNumber: Int = 1,
    val billLanguage: String = "English + Telugu",
    val lastUpdated: Long = System.currentTimeMillis()
)
