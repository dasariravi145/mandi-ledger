package com.dasariravi145.agrolynch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ElementLayout(
    val xPercent: Float = 0f,
    val yPercent: Float = 0f,
    val widthPercent: Float = 0f,
    val heightPercent: Float = 0f,
    val zIndex: Int = 1,
    val visible: Boolean = true,
    val fontSize: Float = 12f,
    val fontColor: String = "#000000",
    val backgroundColor: String = "transparent",
    val fontWeight: String = "NORMAL",
    val alignment: String = "LEFT",
    val shape: String = "ORIGINAL",
    val opacity: Float = 1.0f,
    val rotation: Float = 0f,
    val borderThickness: String = "NONE", // NONE, THIN, MEDIUM, THICK
    val borderStyle: String = "SOLID", // SOLID, DASHED
    val borderColor: String = "#000000",
    val cornerStyle: String = "SQUARE", // SQUARE, SLIGHTLY_ROUNDED, ROUNDED
    val padding: String = "NORMAL", // COMPACT, NORMAL, SPACIOUS
    val textColorOption: String = "DEFAULT", // DEFAULT, BLACK, TEMPLATE
    val headerVisible: Boolean = false,
    val headerStyle: String = "TEXT" // TEXT, FILLED, UNDERLINE
)

@Serializable
data class InvoiceWizardConfig(
    val template: String = "GK_FRUITS_CLASSIC",
    val configVersion: Int = 3,
    val layoutConfigVersion: Int = 2,
    
    // Element Layouts Map (New coordinate-based system)
    val layouts: Map<String, ElementLayout> = emptyMap(),
    
    // Legacy fields (kept for compatibility)
    val logoPosition: String = "TOP_CENTER", 
    val logoScale: Float = 1.0f,
    val showLogo: Boolean = true,
    val logoShape: String = "ROUNDED", 
    
    val godImagePosition: String = "HIDE", 
    val godImageScale: Float = 1.0f,
    val showGodImage: Boolean = false,
    val godImageShape: String = "CIRCLE", 
    
    val qrPosition: String = "BOTTOM_RIGHT", 
    val qrScale: Float = 1.0f,
    val showQr: Boolean = true,
    
    val signaturePosition: String = "BOTTOM_RIGHT", 
    val signatureScale: Float = 1.0f,
    val showSignature: Boolean = true,
    val signatureShape: String = "ORIGINAL", 
    
    val stampPosition: String = "HIDE", 
    val stampScale: Float = 1.0f,
    val showStamp: Boolean = false,
    val stampShape: String = "ORIGINAL", 
    
    val shopNameFontSize: Float = 46f,
    val shopNameAlignment: String = "CENTER",
    val shopNamePosition: String = "TOP_CENTER",
    val showShopName: Boolean = true,
    
    val addressFontSize: Float = 13f,
    val addressAlignment: String = "LEFT",
    val addressPosition: String = "TOP_LEFT",
    val showAddress: Boolean = true,
    
    val phoneFontSize: Float = 13f,
    val phoneAlignment: String = "LEFT",
    val phonePosition: String = "TOP_LEFT",
    val showPhone: Boolean = true,
    
    val taglineFontSize: Float = 15f,
    val taglineAlignment: String = "CENTER",
    val taglinePosition: String = "TOP_RIGHT",
    val showTagline: Boolean = true,
    
    val watermarkType: String = "TEXT", 
    val watermarkText: String? = null,
    val watermarkOpacity: Float = 0.06f,
    val watermarkScale: Float = 1.3f,
    val showWatermark: Boolean = true,
    val watermarkShape: String = "ROUNDED", 
    
    val theme: String = "GREEN",
    val showGst: Boolean = true,
    
    // Page Style
    val pageBackgroundOption: String = "DEFAULT", // DEFAULT, WHITE, SOFT_TINT
    val pageBorderThickness: String = "NONE", // NONE, THIN, MEDIUM
    val pageCornerStyle: String = "SQUARE", // SQUARE, SLIGHTLY_ROUNDED, ROUNDED
    
    // QR Settings
    val qrSizeOption: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val showQrLabel: Boolean = true,
    val qrLabelStyle: String = "BOLD", // NORMAL, BOLD
    val qrLabelColorOption: String = "DEFAULT", // DEFAULT, BLACK, TEMPLATE
) {
    companion object {
        const val KEY_SHOP_NAME = "SHOP_NAME"
        const val KEY_TAGLINE = "TAGLINE"
        const val KEY_ADDRESS = "ADDRESS"
        const val KEY_PHONE = "PHONE"
        const val KEY_LOGO = "LOGO"
        const val KEY_GOD_IMAGE = "GOD_IMAGE"
        const val KEY_QR_CODE = "QR_CODE"
        const val KEY_SIGNATURE_BLOCK = "SIGNATURE_BLOCK"
        const val KEY_STAMP = "STAMP"
        const val KEY_PRODUCT_TABLE = "PRODUCT_TABLE"
        const val KEY_TOTALS_BOX = "TOTALS_BOX"
        const val KEY_WATERMARK = "WATERMARK"
        const val KEY_AUTHORIZED_LABEL = "AUTH_LABEL"
        const val KEY_FOOTER = "FOOTER"
        const val KEY_GSTIN = "GSTIN"
        const val KEY_THANK_YOU = "THANK_YOU"

        // Legacy / Structural Keys (Internal only)
        const val KEY_BILL_INFO = "BILL_INFO"
        const val KEY_CUSTOMER_INFO = "CUSTOMER_INFO"

        val STRUCTURAL_KEYS = setOf(KEY_PRODUCT_TABLE, KEY_TOTALS_BOX, KEY_FOOTER, KEY_BILL_INFO, KEY_CUSTOMER_INFO)
    }

    fun getLayout(key: String): ElementLayout {
        if (layoutConfigVersion < 2 && STRUCTURAL_KEYS.contains(key)) {
             return getDefaultLayoutForTemplate(template, key)
        }
        return layouts[key] ?: getDefaultLayoutForTemplate(template, key)
    }

    private fun getDefaultLayoutForTemplate(templateId: String, key: String): ElementLayout {
        val definition = InvoiceTemplateDefinition.getDefinition(templateId)
        return definition.defaultLayouts[key] ?: getDefaultLayout(key)
    }

    private fun getDefaultLayout(key: String): ElementLayout {
        return when (key) {
            KEY_GOD_IMAGE -> ElementLayout(xPercent = 40f, yPercent = 2f, widthPercent = 20f, heightPercent = 10f, visible = showGodImage, shape = godImageShape, zIndex = 5)
            KEY_SHOP_NAME -> ElementLayout(xPercent = 10f, yPercent = 12f, widthPercent = 80f, heightPercent = 8f, fontSize = shopNameFontSize, alignment = "CENTER", visible = showShopName, fontWeight = "BOLD", zIndex = 6)
            KEY_TAGLINE -> ElementLayout(xPercent = 10f, yPercent = 20f, widthPercent = 80f, heightPercent = 4f, fontSize = taglineFontSize, alignment = "CENTER", visible = showTagline, zIndex = 5)
            
            KEY_ADDRESS -> ElementLayout(xPercent = 5f, yPercent = 5f, widthPercent = 35f, heightPercent = 10f, fontSize = addressFontSize, alignment = "LEFT", visible = showAddress, zIndex = 5)
            KEY_PHONE -> ElementLayout(xPercent = 5f, yPercent = 15f, widthPercent = 35f, heightPercent = 3f, fontSize = phoneFontSize, alignment = "LEFT", visible = showPhone, zIndex = 5)
            KEY_GSTIN -> ElementLayout(xPercent = 5f, yPercent = 18f, widthPercent = 35f, heightPercent = 3f, fontSize = phoneFontSize, alignment = "LEFT", visible = true, zIndex = 5)
            
            KEY_LOGO -> ElementLayout(xPercent = 80f, yPercent = 5f, widthPercent = 15f, heightPercent = 12f, visible = showLogo, shape = logoShape, zIndex = 5)
            
            KEY_PRODUCT_TABLE -> ElementLayout(xPercent = 5f, yPercent = 30f, widthPercent = 90f, heightPercent = 40f, zIndex = 4)
            
            KEY_QR_CODE -> ElementLayout(xPercent = 5f, yPercent = 75f, widthPercent = 15f, heightPercent = 15f, visible = showQr, zIndex = 5)
            KEY_THANK_YOU -> ElementLayout(xPercent = 25f, yPercent = 80f, widthPercent = 30f, heightPercent = 5f, alignment = "CENTER", visible = true, fontSize = 16f, fontWeight = "BOLD", zIndex = 5)
            
            KEY_TOTALS_BOX -> ElementLayout(xPercent = 60f, yPercent = 70f, widthPercent = 35f, heightPercent = 20f, zIndex = 5)
            KEY_SIGNATURE_BLOCK -> ElementLayout(xPercent = 70f, yPercent = 91f, widthPercent = 25f, heightPercent = 8f, visible = showSignature, zIndex = 6)
            KEY_STAMP -> ElementLayout(xPercent = 52f, yPercent = 88f, widthPercent = 15f, heightPercent = 10f, visible = showStamp, zIndex = 7)
            
            KEY_FOOTER -> ElementLayout(xPercent = 0f, yPercent = 97f, widthPercent = 100f, heightPercent = 3f, alignment = "CENTER", fontSize = 10f, backgroundColor = "#0F172A", fontColor = "#D4AF37", zIndex = 10)
            KEY_WATERMARK -> ElementLayout(xPercent = 20f, yPercent = 30f, widthPercent = 60f, heightPercent = 40f, visible = showWatermark, opacity = 0.05f, rotation = -35f, zIndex = 1)

            else -> ElementLayout()
        }
    }
}
