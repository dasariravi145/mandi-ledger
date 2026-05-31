package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val location: String = "",
    val pinHash: String = "", 
    val isPremium: Boolean = false,
    val premiumExpiry: Long = 0L,
    val cloudBackupEnabled: Boolean = false,
    val multiDeviceSyncEnabled: Boolean = false,
    val voiceEntryEnabled: Boolean = false,
    val ocrEnabled: Boolean = false,
    val ocrCloudStorageEnabled: Boolean = false,
    val pdfCloudStorageEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
