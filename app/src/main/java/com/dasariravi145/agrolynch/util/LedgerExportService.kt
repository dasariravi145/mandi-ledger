package com.dasariravi145.agrolynch.util

import android.content.Context
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerExportService @Inject constructor(
    private val pdfService: TemplateInvoicePdfService,
    private val farmerRepository: com.dasariravi145.agrolynch.domain.repository.FarmerRepository,
    private val buyerRepository: com.dasariravi145.agrolynch.domain.repository.BuyerRepository
) {

    suspend fun exportArrivalToPdf(context: Context, profile: CompanyProfileEntity, arrivals: List<com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity>, deductions: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>, farmerMobile: String = ""): File? {
        val mobile = if (farmerMobile.isBlank() && arrivals.isNotEmpty()) {
            farmerRepository.getFarmerById(arrivals[0].farmerId)?.mobileNumber ?: ""
        } else farmerMobile
        return pdfService.generateFarmerArrivalPdf(context, profile, arrivals, deductions, mobile)
    }

    suspend fun exportSaleToPdf(context: Context, profile: CompanyProfileEntity, sale: com.dasariravi145.agrolynch.data.local.entity.SaleEntity, items: List<com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity>, deductions: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>, buyerMobile: String = ""): File? {
        val mobile = if (buyerMobile.isBlank()) {
            buyerRepository.getBuyerById(sale.buyerId)?.mobileNumber ?: ""
        } else buyerMobile
        return pdfService.generateBuyerSalePdf(context, profile, sale, items, deductions, mobile)
    }

    suspend fun exportPaymentToPdf(context: Context, profile: CompanyProfileEntity, payment: com.dasariravi145.agrolynch.data.local.entity.PaymentEntity, partyType: String): File? {
        return pdfService.generatePaymentReceiptPdf(context, profile, payment, partyType.equals("FARMER", ignoreCase = true))
    }
}
