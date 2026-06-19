package com.dasariravi145.agrolynch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceWizardConfig(
    val template: String = "GK_FRUITS_CLASSIC",
    val logoPosition: String = "CENTER", // LEFT, CENTER, RIGHT
    val godImagePosition: String = "HIDE", // LEFT, CENTER, RIGHT, HIDE
    val qrPosition: String = "BOTTOM_RIGHT", // BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    val signaturePosition: String = "BOTTOM_RIGHT", // BOTTOM_LEFT, BOTTOM_RIGHT
    val stampPosition: String = "HIDE", // TOP_RIGHT, BOTTOM_RIGHT, HIDE
    val theme: String = "GREEN",
    val showGst: Boolean = true,
    val showTagline: Boolean = true
)
