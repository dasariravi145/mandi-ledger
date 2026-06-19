package com.dasariravi145.agrolynch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceLayout(
    val templateId: String,
    val backgroundImage: String,
    val pageWidth: Float = 595f,
    val pageHeight: Float = 842f,
    val elements: List<LayoutElement> = emptyList()
)

@Serializable
data class LayoutElement(
    val id: String,
    val label: String,
    val type: String, // "TEXT", "IMAGE", "TABLE"
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val fontSize: Float = 12f,
    val color: String = "#000000",
    val bold: Boolean = false,
    val align: String = "LEFT", // "LEFT", "CENTER", "RIGHT"
    val dynamicKey: String, // e.g., "company.name", "bill.number"
    val visible: Boolean = true,
    val zIndex: Int = 0
)
