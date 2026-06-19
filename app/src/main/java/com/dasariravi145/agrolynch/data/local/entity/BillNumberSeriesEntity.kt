package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_number_series")
data class BillNumberSeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seriesType: String, // STOCK, SALE, PAYMENT, LEDGER
    val prefix: String,
    val currentNumber: Long,
    val startingNumber: Long,
    val resetYearly: Boolean,
    val financialYearEnabled: Boolean,
    val lastGeneratedDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
