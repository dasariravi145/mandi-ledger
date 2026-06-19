package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_deductions",
    indices = [
        androidx.room.Index(value = ["entryId"]),
        androidx.room.Index(value = ["billId"])
    ]
)
data class EntryDeductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: String,
    val entryType: String, // STOCK, SALE, PAYMENT
    val billId: String,
    val deductionType: String, // CAT, Paper, Advance, etc.
    val customName: String = "",
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
