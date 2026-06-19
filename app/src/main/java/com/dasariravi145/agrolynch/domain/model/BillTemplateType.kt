package com.dasariravi145.agrolynch.domain.model

enum class BillTemplateType(val displayName: String, val isPremium: Boolean) {
    GK_FRUITS_CLASSIC("GK Fruits Classic", false),
    ROYAL_HERITAGE_MANDI("Royal Heritage Mandi", true),
    DIAMOND_BUSINESS_ELITE("Diamond Business Elite", true),
    PREMIUM_FRUIT_GALLERY("Premium Fruit Gallery", true),
    EXECUTIVE_GLASS_STYLE("Executive Glass Style", true),
    COMPACT_THERMAL_PRINT("Compact Thermal Print", true),
    CUSTOM_TEMPLATE("Custom Template", true);

    companion object {
        fun fromId(id: String): BillTemplateType = entries.find { it.name == id } ?: GK_FRUITS_CLASSIC
    }
}
