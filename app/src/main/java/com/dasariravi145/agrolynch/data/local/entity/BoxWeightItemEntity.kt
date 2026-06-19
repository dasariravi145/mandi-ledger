package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "box_weight_items",
    indices = [
        androidx.room.Index(value = ["arrivalId"])
    ]
)
data class BoxWeightItemEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val arrivalId: String = "",
    val boxNumber: Int = 0,
    val grossWeightKg: Double = 0.0,
    val tareWeightKg: Double = 0.0,
    val spoilageKg: Double = 0.0,
    val netWeightKg: Double = 0.0
)
