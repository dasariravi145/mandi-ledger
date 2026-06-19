package com.dasariravi145.agrolynch.util

import android.content.Context
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerExportService @Inject constructor(
    private val pdfService: TemplateInvoicePdfService
) {

    suspend fun exportArrivalToPdf(context: Context, profile: CompanyProfileEntity, arrivals: List<com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity>, deductions: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>, farmerMobile: String = ""): File? {
        return pdfService.generateFarmerArrivalPdf(context, profile, arrivals, deductions, farmerMobile)
    }

    suspend fun exportSaleToPdf(context: Context, profile: CompanyProfileEntity, sale: com.dasariravi145.agrolynch.data.local.entity.SaleEntity, items: List<com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity>, deductions: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>, buyerMobile: String = ""): File? {
        return pdfService.generateBuyerSalePdf(context, profile, sale, items, deductions, buyerMobile)
    }

    suspend fun exportPaymentToPdf(context: Context, profile: CompanyProfileEntity, payment: com.dasariravi145.agrolynch.data.local.entity.PaymentEntity, partyType: String): File? {
        return pdfService.generatePaymentReceiptPdf(context, profile, payment, partyType == "Farmer")
    }
}
