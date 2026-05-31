package com.dasariravi145.agrolynch.data.remote.model

data class FirestoreUserProfile(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "AGENT",
    val isPremium: Boolean = false,
    val premiumExpiry: Long = 0L,
    val cloudBackupEnabled: Boolean = false,
    val multiDeviceSyncEnabled: Boolean = false,
    val voiceEntryEnabled: Boolean = false,
    val ocrEnabled: Boolean = false,
    val ocrCloudStorageEnabled: Boolean = false,
    val pdfCloudStorageEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreFarmer(
    val farmerId: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val phone: String = "",
    val village: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreBuyer(
    val buyerId: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreProduct(
    val productId: String = "",
    val ownerUserId: String = "",
    val productName: String = "",
    val category: String = "",
    val grade: String = "",
    val unit: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreStockEntry(
    val stockId: String = "",
    val ownerUserId: String = "",
    val farmerId: String = "",
    val productId: String = "",
    val category: String = "",
    val grade: String = "",
    val quantity: Double = 0.0,
    val rate: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val grossAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class FirestoreSale(
    val saleId: String = "",
    val ownerUserId: String = "",
    val buyerId: String = "",
    val productId: String = "",
    val category: String = "",
    val grade: String = "",
    val quantity: Double = 0.0,
    val rate: Double = 0.0,
    val grossAmount: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val netAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class FirestorePayment(
    val paymentId: String = "",
    val ownerUserId: String = "",
    val partyId: String = "",
    val partyType: String = "", // FARMER or BUYER
    val amount: Double = 0.0,
    val paymentMode: String = "",
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FirestoreOcrScan(
    val scanId: String = "",
    val ownerUserId: String = "",
    val billNumber: String = "",
    val billDate: Long = 0L,
    val amount: Double = 0.0,
    val ocrText: String = "",
    val imageUrl: String? = null,
    val scanType: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
