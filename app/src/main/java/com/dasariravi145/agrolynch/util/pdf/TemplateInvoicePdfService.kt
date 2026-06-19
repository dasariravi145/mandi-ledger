package com.dasariravi145.agrolynch.util.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.util.pdf.renderer.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateInvoicePdfService @Inject constructor() {

    private fun getRenderer(type: String): InvoiceRenderer {
        return InvoiceRendererFactory.getRenderer(type)
    }

    suspend fun generateFarmerArrivalPdf(context: Context, profile: CompanyProfileEntity, arrivals: List<ArrivalEntity>, deductions: List<EntryDeductionEntity>, farmerMobile: String): File? {
        val renderer = getRenderer(profile.defaultTemplate)
        val pdfDocument = PdfDocument()
        
        val businessProfile = profile.toBusinessProfile()
        val invoiceData = arrivals.toInvoiceData(farmerMobile, deductions)
        
        renderer.render(pdfDocument, businessProfile, invoiceData)
        
        return savePdfToFile(context, "Arrival_${arrivals[0].billNumber}", pdfDocument)
    }

    suspend fun generateBuyerSalePdf(context: Context, profile: CompanyProfileEntity, sale: SaleEntity, items: List<SaleItemEntity>, deductions: List<EntryDeductionEntity>, buyerMobile: String): File? {
        val renderer = getRenderer(profile.defaultTemplate)
        val pdfDocument = PdfDocument()
        
        val businessProfile = profile.toBusinessProfile()
        val products = items.map { InvoiceProduct(it.productName, it.grade, it.quantitySold, it.saleRate, it.saleAmount) }
        val invoiceData = InvoiceData(
            billNumber = sale.billNumber,
            date = sale.date,
            customerName = sale.buyerName,
            customerMobile = buyerMobile,
            products = products,
            subtotal = sale.totalNetAmount,
            commission = 0.0,
            transport = 0.0,
            labour = 0.0,
            advance = 0.0,
            others = 0.0,
            grandTotal = sale.totalNetAmount
        )
        
        renderer.render(pdfDocument, businessProfile, invoiceData)
        return savePdfToFile(context, "Sale_${sale.billNumber}", pdfDocument)
    }

    suspend fun generatePaymentReceiptPdf(context: Context, profile: CompanyProfileEntity, payment: PaymentEntity, isFarmer: Boolean): File? {
        val renderer = getRenderer(profile.defaultTemplate)
        val pdfDocument = PdfDocument()
        
        val businessProfile = profile.toBusinessProfile()
        val invoiceData = InvoiceData(
            billNumber = payment.billNumber,
            date = payment.date,
            customerName = payment.partyName,
            customerMobile = "",
            products = emptyList(),
            subtotal = payment.amount,
            commission = 0.0,
            transport = 0.0,
            labour = 0.0,
            advance = 0.0,
            others = 0.0,
            grandTotal = payment.amount
        )
        
        renderer.render(pdfDocument, businessProfile, invoiceData)
        return savePdfToFile(context, "Receipt_${payment.billNumber}", pdfDocument)
    }

    suspend fun generatePaymentReceiptPdf(context: Context, profile: CompanyProfileEntity, data: com.dasariravi145.agrolynch.domain.model.ReceiptData): File? {
        val renderer = getRenderer(profile.defaultTemplate)
        val pdfDocument = PdfDocument()
        
        val businessProfile = profile.toBusinessProfile()
        val invoiceData = InvoiceData(
            billNumber = data.receiptId,
            date = data.date,
            customerName = data.partyName,
            customerMobile = data.agentContact,
            products = data.items.map { InvoiceProduct(it.description, "", it.quantity.toDoubleOrNull() ?: 0.0, it.rate.toDoubleOrNull() ?: 0.0, it.amount.toDoubleOrNull() ?: 0.0) },
            subtotal = data.totalAmount,
            commission = 0.0,
            transport = 0.0,
            labour = 0.0,
            advance = 0.0,
            others = 0.0,
            grandTotal = data.totalAmount
        )
        
        renderer.render(pdfDocument, businessProfile, invoiceData)
        return savePdfToFile(context, "Receipt_${data.receiptId}", pdfDocument)
    }

    private fun CompanyProfileEntity.toBusinessProfile() = BusinessProfile(
        companyName = companyName,
        address = address,
        mobile = mobile1,
        gstNumber = gstNumber,
        tagline = tagline,
        logoPath = logoPath,
        qrPath = upiQrPath,
        signaturePath = signaturePath,
        godImagePath = godImagePath,
        stampPath = stampPath
    )

    private fun List<ArrivalEntity>.toInvoiceData(mobile: String, deductions: List<EntryDeductionEntity>): InvoiceData {
        val first = this[0]
        val prods = this.map { InvoiceProduct(it.productName, it.grade, it.finalNetWeightKg, it.ratePerKg, it.grossAmount) }
        val sub = sumOf { it.grossAmount }
        val grand = sub - sumOf { it.commissionAmount + it.transportCharges + it.laborCharges } - deductions.sumOf { it.amount }
        
        return InvoiceData(
            billNumber = first.billNumber,
            date = first.date,
            customerName = first.farmerName,
            customerMobile = mobile,
            products = prods,
            subtotal = sub,
            commission = sumOf { it.commissionAmount },
            transport = sumOf { it.transportCharges },
            labour = sumOf { it.laborCharges },
            advance = 0.0,
            others = deductions.sumOf { it.amount },
            grandTotal = grand
        )
    }

    private fun savePdfToFile(context: Context, fileName: String, document: PdfDocument): File? {
        val root = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MandiLedger/Invoices")
        if (!root.exists()) root.mkdirs()
        val file = File(root, "$fileName.pdf")
        
        return try {
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }
}
