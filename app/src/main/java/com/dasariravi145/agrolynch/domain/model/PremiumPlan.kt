package com.dasariravi145.agrolynch.domain.model

/**
 * Represents a premium subscription plan.
 *
 * @property basePlanId The base plan identifier in Google Play Console (monthly, 3-months, 6-months, yearly).
 * @property name Display name of the plan.
 * @property price Numeric price (fallback).
 * @property formattedPrice Display price (fallback).
 * @property monthlyPrice Calculated monthly equivalent (fallback).
 * @property durationText Description of the validity period.
 * @property badge Marketing badge for the plan.
 * @property durationDays Total validity duration in days.
 */
data class PremiumPlan(
    val basePlanId: String,
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
        basePlanId = "monthly",
        name = "Monthly",
        price = 149.0,
        formattedPrice = "₹149",
        monthlyPrice = "₹149/month",
        durationText = "Valid for 1 month",
        badge = "Basic",
        durationDays = 30
    ),
    PremiumPlan(
        basePlanId = "3-months",
        name = "3 Months",
        price = 399.0,
        formattedPrice = "₹399",
        monthlyPrice = "₹133/month",
        durationText = "Valid for 3 months",
        badge = "Popular",
        durationDays = 90
    ),
    PremiumPlan(
        basePlanId = "6-months",
        name = "6 Months",
        price = 699.0,
        formattedPrice = "₹699",
        monthlyPrice = "₹116/month",
        durationText = "Valid for 6 months",
        badge = "Best Value",
        durationDays = 180
    ),
    PremiumPlan(
        basePlanId = "yearly",
        name = "Yearly",
        price = 1299.0,
        formattedPrice = "₹1299",
        monthlyPrice = "₹108/month",
        durationText = "Valid for 1 year",
        badge = "Maximum Savings",
        durationDays = 365
    )
)
