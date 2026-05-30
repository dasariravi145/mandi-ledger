package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        androidx.room.Index(value = ["name"]),
        androidx.room.Index(value = ["isDeleted"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val category: String = "", // Fruit or Vegetable
    val imageUrl: String = "",
    val availableGrades: List<String> = emptyList(), // e.g., ["A Grade", "B Grade", "Premium"]
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
