package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_history")
data class BackupEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String,
    val filePath: String,
    val size: Long,
    val type: String, // "LOCAL", "CLOUD"
    val reportType: String, // "WEEKLY", "MONTHLY", "MANUAL"
    val status: String // "SUCCESS", "FAILED"
)
