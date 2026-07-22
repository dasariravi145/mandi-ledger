package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_book_archives")
data class AccountBookArchiveEntity(
    @PrimaryKey val archiveId: String,
    val partyType: String, // "FARMER" or "BUYER"
    val originalPartyId: String,
    val partyName: String,
    val partyPhone: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val pendingAmount: Double,
    val settlementDate: Long,
    val archivedAt: Long = System.currentTimeMillis(),
    val snapshotJson: String,
    val archiveVersion: Int = 1,
    val backupReference: String? = null,
    val status: String = "ARCHIVED" // ARCHIVED, RESTORED, DELETED
)
