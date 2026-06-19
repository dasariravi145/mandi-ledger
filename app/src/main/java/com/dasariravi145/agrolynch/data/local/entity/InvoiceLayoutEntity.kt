package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_layouts")
data class InvoiceLayoutEntity(
    @PrimaryKey val templateId: String, // Maps to BillTemplateType.name
    val layoutJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
