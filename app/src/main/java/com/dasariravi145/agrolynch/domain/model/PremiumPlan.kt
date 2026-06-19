package com.dasariravi145.agrolynch.domain.model

data class PremiumPlan(
    val productId: String,
    val name: String,
    val price: Double,
    val formattedPrice: String,
    val monthlyPrice: String,
    val durationText: String,
    val badge: String,
    val durationDays: Int
)

val PREMIUM_PLANS = listOf(
    PremiumPlan(
        productId = "premium_1_month",
        name = "Monthly",
        price = 149.0,
        formattedPrice = "₹149",
        monthlyPrice = "₹149/month",
        durationText = "Valid for 1 month",
        badge = "Basic",
        durationDays = 30
    ),
    PremiumPlan(
        productId = "premium_3_months",
        name = "3 Months",
        price = 399.0,
        formattedPrice = "₹399",
        monthlyPrice = "₹133/month",
        durationText = "Valid for 3 months",
        badge = "Popular",
        durationDays = 90
    ),
    PremiumPlan(
        productId = "premium_6_months",
        name = "6 Months",
        price = 699.0,
        formattedPrice = "₹699",
        monthlyPrice = "₹116/month",
        durationText = "Valid for 6 months",
        badge = "Best Value",
        durationDays = 180
    ),
    PremiumPlan(
        productId = "premium_1_year",
        name = "Yearly",
        price = 1299.0,
        formattedPrice = "₹1299",
        monthlyPrice = "₹108/month",
        durationText = "Valid for 1 year",
        badge = "Maximum Savings",
        durationDays = 365
    )
)
