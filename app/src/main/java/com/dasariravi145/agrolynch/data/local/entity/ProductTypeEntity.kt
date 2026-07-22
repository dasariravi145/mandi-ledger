package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "product_types",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["productName"]),
        Index(value = ["productTypeName"])
    ]
)
data class ProductTypeEntity(
    @PrimaryKey val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productTypeName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
