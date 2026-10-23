package com.dasariravi145.agrolynch.ui.screens.sale

import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import com.dasariravi145.agrolynch.ui.screens.arrival.ArrivalViewModel
import java.util.UUID

enum class WeightMatchStatus {
    NO_WEIGHT_ENTERED,
    MATCHED,
    UNDER_ALLOCATED,
    OVER_ALLOCATED
}

data class FarmerArrivalData(
    val id: String = UUID.randomUUID().toString(),
    val farmerId: String = UUID.randomUUID().toString(),
    val farmerName: String = "",
    val farmerPhone: String = "",
    val farmerVillage: String = "",
    val billNumber: String = "",
    val productName: String = "",
    val productType: String = "",
    val productCategory: String = "Fruit",
    val gradeEntries: List<ArrivalViewModel.GradeEntry> = emptyList(),
    val commissionPercent: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val packingCharges: Double = 0.0,
    val deductions: List<EntryDeductionEntity> = emptyList(),
    val buyerSaleRate: Double = 0.0
) {
    val totalGrossWeightKg: Double get() = gradeEntries.sumOf { it.totalGrossWeightKg }
    val totalNetWeightKg: Double get() = gradeEntries.sumOf { it.totalNetWeightKg }
    val farmerGrossAmount: Double get() = gradeEntries.sumOf { it.grossAmount }
    val farmerCommissionAmount: Double get() = (farmerGrossAmount * commissionPercent) / 100.0
    val farmerTotalDeductions: Double get() = deductions.sumOf { it.amount }
    val farmerNetPayable: Double get() = farmerGrossAmount - farmerCommissionAmount - laborCharges - transportCharges - packingCharges - farmerTotalDeductions
    val buyerSaleAmount: Double get() = totalNetWeightKg * buyerSaleRate
}

data class ProduceGroupDraft(
    val id: String = UUID.randomUUID().toString(),
    val productName: String = "",
    val productType: String = "",
    val category: String = "Fruit",
    val farmers: List<FarmerArrivalData> = emptyList()
) {
    val totalGrossWeightKg: Double get() = farmers.sumOf { it.totalGrossWeightKg }
    val totalNetWeightKg: Double get() = farmers.sumOf { it.totalNetWeightKg }
    val totalFarmerPurchaseAmount: Double get() = farmers.sumOf { it.farmerGrossAmount }
    val totalBuyerSaleAmount: Double get() = farmers.sumOf { it.buyerSaleAmount }
}

data class BuyerSaleTransactionTotal(
    val saleQuantityInput: Double = 0.0,
    val saleQuantityUnit: String = "Quintal",
    val buyerGrossWeightKg: Double = 0.0,
    val buyerNetWeightKg: Double = 0.0,
    val totalFarmerGrossWeightKg: Double = 0.0,
    val totalFarmerNetWeightKg: Double = 0.0,
    val weightDifferenceKg: Double = 0.0,
    val weightMatchStatus: WeightMatchStatus = WeightMatchStatus.NO_WEIGHT_ENTERED,
    val totalFarmerPurchaseAmount: Double = 0.0,
    val totalBuyerSaleSubtotal: Double = 0.0,
    val laborPercentage: Double = 0.0,
    val laborEquivalentKg: Double = 0.0,
    val totalLaborCharges: Double = 0.0,
    val totalTransportCharges: Double = 0.0,
    val totalOtherDeductions: Double = 0.0,
    val totalFinalCollection: Double = 0.0,
    val isSaveEnabled: Boolean = false
)
