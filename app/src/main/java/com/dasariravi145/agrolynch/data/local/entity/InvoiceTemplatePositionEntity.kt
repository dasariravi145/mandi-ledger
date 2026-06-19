package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template_positions")
data class InvoiceTemplatePositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateType: String, // BillTemplateType.name
    val fieldKey: String, // InvoiceFieldKey
    val x: Float = 0f, // Legacy
    val y: Float = 0f, // Legacy
    val xPercent: Float = 0f,
    val yPercent: Float = 0f,
    val widthPercent: Float = 0f,
    val heightPercent: Float = 0f,
    val width: Float = 0f, // Legacy
    val height: Float = 0f, // Legacy
    val fontSize: Float = 12f,
    val fontColor: Int = -16777216, // Black default
    val backgroundColor: Int = 0, // Transparent
    val borderColor: Int = -7829368, // Gray
    val borderEnabled: Boolean = false,
    val bold: Boolean = false,
    val alignment: String = "LEFT",
    val isVisible: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
