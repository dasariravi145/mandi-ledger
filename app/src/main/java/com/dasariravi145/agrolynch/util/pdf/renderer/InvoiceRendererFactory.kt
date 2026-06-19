package com.dasariravi145.agrolynch.util.pdf.renderer

import com.dasariravi145.agrolynch.domain.model.BillTemplateType

object InvoiceRendererFactory {
    fun getRenderer(type: String): InvoiceRenderer {
        return when (type) {
            "GK_FRUITS_CLASSIC" -> GkClassicRenderer()
            "ROYAL_HERITAGE_MANDI" -> RoyalHeritageRenderer()
            "DIAMOND_BUSINESS_ELITE" -> DiamondEliteRenderer()
            "PREMIUM_FRUIT_GALLERY" -> PremiumFruitRenderer()
            "EXECUTIVE_GLASS_STYLE" -> ExecutiveGlassRenderer()
            "COMPACT_THERMAL_PRINT" -> CompactPrintRenderer()
            else -> GkClassicRenderer()
        }
    }
}
