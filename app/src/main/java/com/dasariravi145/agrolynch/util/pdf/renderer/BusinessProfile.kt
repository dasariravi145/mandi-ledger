package com.dasariravi145.agrolynch.util.pdf.renderer

data class BusinessProfile(
    val companyName: String,
    val address: String,
    val village: String = "",
    val mobile: String,
    val proprietor: String = "",
    val gstNumber: String,
    val tagline: String,
    val logoPath: String?,
    val qrPath: String?,
    val signaturePath: String?,
    val godImagePath: String?,
    val stampPath: String?,
    val watermarkImagePath: String? = null,
    val marketName: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null
)
