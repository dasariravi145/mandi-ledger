package com.dasariravi145.agrolynch.util.pdf.renderer

data class BusinessProfile(
    val companyName: String,
    val address: String,
    val mobile: String,
    val gstNumber: String,
    val tagline: String,
    val logoPath: String?,
    val qrPath: String?,
    val signaturePath: String?,
    val godImagePath: String?,
    val stampPath: String?
)
