package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val location: String = "",
    val pinHash: String = "", // In production, use hashed PIN
    val createdAt: Long = System.currentTimeMillis()
)
