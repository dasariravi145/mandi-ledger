package com.dasariravi145.agrolynch.domain.model

import com.dasariravi145.agrolynch.data.local.entity.*

data class BackupData(
    val companyProfile: List<CompanyProfileEntity> = emptyList(),
    val farmers: List<FarmerEntity> = emptyList(),
    val buyers: List<BuyerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val arrivals: List<ArrivalEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val marketRates: List<MarketRateEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val ocrScans: List<OcrScanEntity> = emptyList(),
    val boxWeightItems: List<BoxWeightItemEntity> = emptyList(),
    val billSeries: List<BillNumberSeriesEntity> = emptyList(),
    val deductions: List<EntryDeductionEntity> = emptyList(),
    val templatePositions: List<InvoiceTemplatePositionEntity> = emptyList()
)
