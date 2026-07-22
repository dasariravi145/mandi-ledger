package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_history")
data class BackupEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String,
    val filePath: String, // Local absolute path or Download URL
    val storagePath: String = "", // Firebase Storage path (backups/uid/filename)
    val size: Long,
    val type: String, // "LOCAL", "CLOUD"
    val reportType: String, // "WEEKLY", "MONTHLY", "MANUAL", "PRE_RESTORE_SAFETY"
    val status: String, // "SUCCESS", "FAILED", "UPLOADING", "RESTORING"
    val phoneNumber: String = "",
    val userName: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
