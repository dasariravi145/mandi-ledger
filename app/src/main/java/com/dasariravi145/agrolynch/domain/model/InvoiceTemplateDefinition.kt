package com.dasariravi145.agrolynch.domain.model

data class InvoiceTemplateDefinition(
    val templateId: String,
    val displayName: String,
    val primaryColor: String,
    val accentColor: String,
    val backgroundColor: String,
    val canvasWidth: Int = 1000,
    val canvasHeight: Int = 1000,
    val defaultLayouts: Map<String, ElementLayout>,
    val structuralKeys: List<String> = listOf(
        InvoiceWizardConfig.KEY_PRODUCT_TABLE,
        InvoiceWizardConfig.KEY_TOTALS_BOX,
        InvoiceWizardConfig.KEY_FOOTER
    )
) {
    companion object {
        fun getDefinition(templateId: String): InvoiceTemplateDefinition {
            return when (templateId) {
                "DIAMOND_BUSINESS_ELITE" -> diamondDefinition
                "EXECUTIVE_GLASS_STYLE" -> executiveDefinition
                "GK_FRUITS_CLASSIC" -> classicDefinition
                "PREMIUM_FRUIT_GALLERY" -> premiumDefinition
                "ROYAL_HERITAGE_MANDI" -> royalDefinition
                else -> classicDefinition
            }
        }

        private val classicDefinition = InvoiceTemplateDefinition(
            templateId = "GK_FRUITS_CLASSIC",
            displayName = "GK Fruits Classic",
            primaryColor = "#16A34A",
            accentColor = "#1E3A8A",
            backgroundColor = "#FFFDF7",
            defaultLayouts = mapOf(
                InvoiceWizardConfig.KEY_GOD_IMAGE to ElementLayout(xPercent = 30.0f, yPercent = 0.0f, widthPercent = 40.0f, heightPercent = 21.5f),
                InvoiceWizardConfig.KEY_SHOP_NAME to ElementLayout(xPercent = 36.5f, yPercent = 21.8f, widthPercent = 27.0f, heightPercent = 12.2f, fontSize = 46f, alignment = "CENTER", fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_ADDRESS to ElementLayout(xPercent = 3.0f, yPercent = 15.5f, widthPercent = 22.0f, heightPercent = 12.0f, fontSize = 12f),
                InvoiceWizardConfig.KEY_TAGLINE to ElementLayout(xPercent = 71.5f, yPercent = 17.0f, widthPercent = 24.0f, heightPercent = 14.0f, fontSize = 15f, alignment = "CENTER"),
                InvoiceWizardConfig.KEY_BILL_INFO to ElementLayout(xPercent = 3.0f, yPercent = 37.5f, widthPercent = 30.0f, heightPercent = 5.3f, borderThickness = "THIN"),
                InvoiceWizardConfig.KEY_CUSTOMER_INFO to ElementLayout(xPercent = 56.5f, yPercent = 37.5f, widthPercent = 36.5f, heightPercent = 5.3f, borderThickness = "THIN"),
                InvoiceWizardConfig.KEY_PRODUCT_TABLE to ElementLayout(xPercent = 3.0f, yPercent = 44.2f, widthPercent = 94.0f, heightPercent = 25.3f),
                InvoiceWizardConfig.KEY_THANK_YOU to ElementLayout(xPercent = 4.0f, yPercent = 71.5f, widthPercent = 23.0f, heightPercent = 6.0f, alignment = "CENTER", fontSize = 16f, fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_QR_CODE to ElementLayout(xPercent = 33.5f, yPercent = 76.0f, widthPercent = 15.5f, heightPercent = 15.5f),
                InvoiceWizardConfig.KEY_TOTALS_BOX to ElementLayout(xPercent = 54.0f, yPercent = 69.5f, widthPercent = 43.0f, heightPercent = 18.0f),
                InvoiceWizardConfig.KEY_FOOTER to ElementLayout(xPercent = 0.0f, yPercent = 96.0f, widthPercent = 100.0f, heightPercent = 4.0f, fontSize = 10f, alignment = "CENTER", backgroundColor = "#16A34A", fontColor = "#FFFFFF")
            )
        )

        private val diamondDefinition = InvoiceTemplateDefinition(
            templateId = "DIAMOND_BUSINESS_ELITE",
            displayName = "Diamond Business Style",
            primaryColor = "#071f4f",
            accentColor = "#d4af37",
            backgroundColor = "#FFFFFF",
            defaultLayouts = mapOf(
                InvoiceWizardConfig.KEY_LOGO to ElementLayout(xPercent = 3.5f, yPercent = 3.3f, widthPercent = 25.8f, heightPercent = 12.2f),
                InvoiceWizardConfig.KEY_TAGLINE to ElementLayout(xPercent = 38.9f, yPercent = 4.1f, widthPercent = 22.2f, heightPercent = 8.1f, fontSize = 14f, alignment = "CENTER"),
                InvoiceWizardConfig.KEY_GOD_IMAGE to ElementLayout(xPercent = 73.8f, yPercent = 3.3f, widthPercent = 18.3f, heightPercent = 10.4f),
                InvoiceWizardConfig.KEY_ADDRESS to ElementLayout(xPercent = 3.5f, yPercent = 17.4f, widthPercent = 34.5f, heightPercent = 9.0f, fontSize = 11f),
                InvoiceWizardConfig.KEY_SHOP_NAME to ElementLayout(xPercent = 43.6f, yPercent = 17.4f, widthPercent = 17.4f, heightPercent = 9.0f, fontSize = 38f, alignment = "CENTER", fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_PHONE to ElementLayout(xPercent = 71.8f, yPercent = 17.4f, widthPercent = 20.6f, heightPercent = 10.2f, fontSize = 11f, alignment = "RIGHT"),
                InvoiceWizardConfig.KEY_BILL_INFO to ElementLayout(xPercent = 3.4f, yPercent = 32.7f, widthPercent = 46.6f, heightPercent = 10.1f),
                InvoiceWizardConfig.KEY_CUSTOMER_INFO to ElementLayout(xPercent = 50.0f, yPercent = 32.7f, widthPercent = 46.6f, heightPercent = 10.1f),
                InvoiceWizardConfig.KEY_PRODUCT_TABLE to ElementLayout(xPercent = 3.4f, yPercent = 44.0f, widthPercent = 93.2f, heightPercent = 17.8f),
                InvoiceWizardConfig.KEY_THANK_YOU to ElementLayout(xPercent = 3.4f, yPercent = 63.8f, widthPercent = 39.1f, heightPercent = 5.6f, fontSize = 16f, fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_QR_CODE to ElementLayout(xPercent = 26.8f, yPercent = 70.7f, widthPercent = 15.7f, heightPercent = 11.8f),
                InvoiceWizardConfig.KEY_TOTALS_BOX to ElementLayout(xPercent = 50.8f, yPercent = 63.1f, widthPercent = 45.8f, heightPercent = 20.4f),
                InvoiceWizardConfig.KEY_SIGNATURE_BLOCK to ElementLayout(xPercent = 61.8f, yPercent = 86.0f, widthPercent = 30.2f, heightPercent = 7.6f),
                InvoiceWizardConfig.KEY_FOOTER to ElementLayout(xPercent = 0f, yPercent = 96.6f, widthPercent = 100f, heightPercent = 3.4f, fontSize = 10f, alignment = "CENTER", backgroundColor = "#071f4f", fontColor = "#FFFFFF")
            )
        )

        private val executiveDefinition = InvoiceTemplateDefinition(
            templateId = "EXECUTIVE_GLASS_STYLE",
            displayName = "Executive Glass Style",
            primaryColor = "#00897b",
            accentColor = "#009688",
            backgroundColor = "#e0f7fa",
            defaultLayouts = mapOf(
                InvoiceWizardConfig.KEY_GOD_IMAGE to ElementLayout(xPercent = 7.5f, yPercent = 4.8f, widthPercent = 21.8f, heightPercent = 17.9f),
                InvoiceWizardConfig.KEY_SHOP_NAME to ElementLayout(xPercent = 32.7f, yPercent = 2.3f, widthPercent = 34.0f, heightPercent = 25.3f, fontSize = 40f, alignment = "CENTER", fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_ADDRESS to ElementLayout(xPercent = 68.3f, yPercent = 14.8f, widthPercent = 28.2f, heightPercent = 12.1f, fontSize = 11f, alignment = "RIGHT"),
                InvoiceWizardConfig.KEY_TAGLINE to ElementLayout(xPercent = 71.3f, yPercent = 6.3f, widthPercent = 17.8f, heightPercent = 5.9f, fontSize = 14f, alignment = "CENTER"),
                InvoiceWizardConfig.KEY_BILL_INFO to ElementLayout(xPercent = 5.8f, yPercent = 33.1f, widthPercent = 43.4f, heightPercent = 8.6f),
                InvoiceWizardConfig.KEY_CUSTOMER_INFO to ElementLayout(xPercent = 50.8f, yPercent = 33.1f, widthPercent = 43.4f, heightPercent = 8.6f),
                InvoiceWizardConfig.KEY_PRODUCT_TABLE to ElementLayout(xPercent = 5.8f, yPercent = 43.1f, widthPercent = 88.4f, heightPercent = 21.1f),
                InvoiceWizardConfig.KEY_QR_CODE to ElementLayout(xPercent = 34.8f, yPercent = 65.9f, widthPercent = 21.9f, heightPercent = 17.7f),
                InvoiceWizardConfig.KEY_TOTALS_BOX to ElementLayout(xPercent = 59.2f, yPercent = 65.4f, widthPercent = 35.0f, heightPercent = 19.2f),
                InvoiceWizardConfig.KEY_SIGNATURE_BLOCK to ElementLayout(xPercent = 69.0f, yPercent = 86.3f, widthPercent = 25.2f, heightPercent = 8.7f),
                InvoiceWizardConfig.KEY_FOOTER to ElementLayout(xPercent = 0f, yPercent = 96.0f, widthPercent = 100f, heightPercent = 4.0f, fontSize = 10f, alignment = "CENTER")
            )
        )

        private val premiumDefinition = InvoiceTemplateDefinition(
            templateId = "PREMIUM_FRUIT_GALLERY",
            displayName = "Premium Fruit Gallery",
            primaryColor = "#4a148c",
            accentColor = "#fbc02d",
            backgroundColor = "#FFFFFF",
            defaultLayouts = mapOf(
                InvoiceWizardConfig.KEY_SHOP_NAME to ElementLayout(xPercent = 33.4f, yPercent = 7.6f, widthPercent = 33.2f, heightPercent = 14.6f, fontSize = 42f, alignment = "CENTER", fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_ADDRESS to ElementLayout(xPercent = 3.6f, yPercent = 23.5f, widthPercent = 21.8f, heightPercent = 9.7f, fontSize = 11f),
                InvoiceWizardConfig.KEY_TAGLINE to ElementLayout(xPercent = 72.5f, yPercent = 24.0f, widthPercent = 23.9f, heightPercent = 7.0f, fontSize = 14f, alignment = "CENTER"),
                InvoiceWizardConfig.KEY_BILL_INFO to ElementLayout(xPercent = 3.4f, yPercent = 35.9f, widthPercent = 46.6f, heightPercent = 7.8f),
                InvoiceWizardConfig.KEY_CUSTOMER_INFO to ElementLayout(xPercent = 50.0f, yPercent = 35.9f, widthPercent = 46.6f, heightPercent = 7.8f),
                InvoiceWizardConfig.KEY_PRODUCT_TABLE to ElementLayout(xPercent = 3.4f, yPercent = 44.8f, widthPercent = 93.2f, heightPercent = 19.4f),
                InvoiceWizardConfig.KEY_QR_CODE to ElementLayout(xPercent = 6.8f, yPercent = 65.4f, widthPercent = 16.3f, heightPercent = 16.1f),
                InvoiceWizardConfig.KEY_TOTALS_BOX to ElementLayout(xPercent = 55.4f, yPercent = 65.4f, widthPercent = 40.4f, heightPercent = 15.3f),
                InvoiceWizardConfig.KEY_THANK_YOU to ElementLayout(xPercent = 38.0f, yPercent = 83.1f, widthPercent = 24.0f, heightPercent = 4.5f, fontSize = 16f, fontWeight = "BOLD"),
                InvoiceWizardConfig.KEY_SIGNATURE_BLOCK to ElementLayout(xPercent = 72.0f, yPercent = 82.4f, widthPercent = 17.2f, heightPercent = 5.0f),
                InvoiceWizardConfig.KEY_FOOTER to ElementLayout(xPercent = 0f, yPercent = 95.4f, widthPercent = 100f, heightPercent = 4.6f, fontSize = 10f, alignment = "CENTER", backgroundColor = "#4a148c", fontColor = "#FFFFFF")
            )
        )

        private val royalDefinition = InvoiceTemplateDefinition(
            templateId = "ROYAL_HERITAGE_MANDI",
            displayName = "Royal Heritage Mandi",
            primaryColor = "#064E3B",
            accentColor = "#C9A227",
            backgroundColor = "#FFFDF3",
            defaultLayouts = mapOf(
                InvoiceWizardConfig.KEY_GOD_IMAGE to ElementLayout(xPercent = 37.5f, yPercent = 4.8f, widthPercent = 25.0f, heightPercent = 19.0f),
                InvoiceWizardConfig.KEY_LOGO to ElementLayout(xPercent = 6.8f, yPercent = 7.7f, widthPercent = 21.6f, heightPercent = 10.8f),
                InvoiceWizardConfig.KEY_ADDRESS to ElementLayout(xPercent = 4.5f, yPercent = 20.3f, widthPercent = 23.9f, heightPercent = 12.5f, fontSize = 12f),
                InvoiceWizardConfig.KEY_TAGLINE to ElementLayout(xPercent = 72.0f, yPercent = 8.5f, widthPercent = 20.8f, heightPercent = 18.8f, fontSize = 15f, alignment = "CENTER"),
                InvoiceWizardConfig.KEY_BILL_INFO to ElementLayout(xPercent = 4.5f, yPercent = 34.8f, widthPercent = 43.5f, heightPercent = 7.6f),
                InvoiceWizardConfig.KEY_CUSTOMER_INFO to ElementLayout(xPercent = 54.0f, yPercent = 34.8f, widthPercent = 40.5f, heightPercent = 7.6f),
                InvoiceWizardConfig.KEY_PRODUCT_TABLE to ElementLayout(xPercent = 3.8f, yPercent = 43.8f, widthPercent = 92.0f, heightPercent = 22.5f),
                InvoiceWizardConfig.KEY_QR_CODE to ElementLayout(xPercent = 26.8f, yPercent = 77.0f, widthPercent = 17.4f, heightPercent = 12.5f),
                InvoiceWizardConfig.KEY_TOTALS_BOX to ElementLayout(xPercent = 50.5f, yPercent = 67.8f, widthPercent = 45.0f, heightPercent = 13.2f),
                InvoiceWizardConfig.KEY_SIGNATURE_BLOCK to ElementLayout(xPercent = 68.0f, yPercent = 82.5f, widthPercent = 27.5f, heightPercent = 7.0f),
                InvoiceWizardConfig.KEY_THANK_YOU to ElementLayout(xPercent = 36.5f, yPercent = 93.0f, widthPercent = 31.0f, heightPercent = 2.5f, alignment = "CENTER", fontSize = 16f, fontWeight = "BOLD")
            )
        )
    }
}
