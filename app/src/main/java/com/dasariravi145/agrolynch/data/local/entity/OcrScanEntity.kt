package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_scans",
    indices = [
        androidx.room.Index(value = ["billDate"]),
        androidx.room.Index(value = ["billNumber"]),
        androidx.room.Index(value = ["createdAt"])
    ]
)
data class OcrScanEntity(
    @PrimaryKey val scanId: String,
    val billNumber: String = "",
    val amount: Double = 0.0,
    val billDate: Long = System.currentTimeMillis(),
    val ocrText: String = "",
    val imageUrl: String? = null,
    val transactionType: String = "", // STOCK_ENTRY, SALE_ENTRY, PAYMENT
    val createdAt: Long = System.currentTimeMillis()
)
