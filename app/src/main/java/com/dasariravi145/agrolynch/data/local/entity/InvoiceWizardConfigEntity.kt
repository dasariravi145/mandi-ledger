package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_wizard_configs")
data class InvoiceWizardConfigEntity(
    @PrimaryKey val templateId: String,
    val configJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
