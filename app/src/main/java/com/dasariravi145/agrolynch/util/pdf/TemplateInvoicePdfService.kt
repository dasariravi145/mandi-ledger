package com.dasariravi145.agrolynch.util.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.BackupData
import com.dasariravi145.agrolynch.util.pdf.renderer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TemplateInvoicePdfService @Inject constructor(
    private val templateRepository: com.dasariravi145.agrolynch.domain.repository.TemplatePositionRepository
) {

    suspend fun generateFarmerArrivalPdf(context: Context, profile: CompanyProfileEntity, arrivals: List<ArrivalEntity>, deductions: List<EntryDeductionEntity>, farmerMobile: String): File? {
        val businessProfile = profile.toBusinessProfile()
        val invoiceData = arrivals.toInvoiceData(farmerMobile, deductions)
        val templateId = mapTemplateId(profile.defaultTemplate)
        val config = templateRepository.getWizardConfig(profile.defaultTemplate)
        
        val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, invoiceData, config)
        return generatePdfFromHtml(context, html, "Arrival_${arrivals[0].billNumber}")
    }

    suspend fun generateBuyerSalePdf(context: Context, profile: CompanyProfileEntity, sale: SaleEntity, items: List<SaleItemEntity>, deductions: List<EntryDeductionEntity>, buyerMobile: String): File? {
        val businessProfile = profile.toBusinessProfile()
        val products = items.map { 
            InvoiceProduct(
                name = it.productName, 
                productType = it.productType,
                grade = it.grade, 
                unit = if (it.unit == "Boxes") "KG" else it.unit, 
                quantity = it.quantitySold, 
                rate = it.saleRate, 
                amount = it.saleAmount
            ) 
        }
        val invoiceData = InvoiceData(
            billNumber = sale.billNumber,
            date = sale.date,
            customerName = sale.buyerName,
            customerMobile = buyerMobile,
            products = products,
            subtotal = sale.totalAmount,
            commission = sale.totalCommission,
            transport = sale.transportCharges,
            labour = sale.laborCharges,
            advance = 0.0,
            others = sale.otherCharges,
            grandTotal = sale.totalNetAmount,
            vehicleNumber = "",
            customerGstin = "", // Not available in SaleEntity
            customerState = "", // Not available in SaleEntity
            placeOfSupply = profile.state,
            reverseCharge = false,
            paymentMode = "CASH"
        )
        val templateId = mapTemplateId(profile.defaultTemplate)
        val config = templateRepository.getWizardConfig(profile.defaultTemplate)
        
        val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, invoiceData, config)
        return generatePdfFromHtml(context, html, "Sale_${sale.billNumber}")
    }

    suspend fun generatePaymentReceiptPdf(context: Context, profile: CompanyProfileEntity, payment: PaymentEntity, isFarmer: Boolean): File? {
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
            grandTotal = payment.amount,
            vehicleNumber = ""
        )
        val templateId = mapTemplateId(profile.defaultTemplate)
        val config = templateRepository.getWizardConfig(profile.defaultTemplate)
        
        val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, invoiceData, config)
        return generatePdfFromHtml(context, html, "Receipt_${payment.billNumber}")
    }

    suspend fun generatePaymentReceiptPdf(context: Context, profile: CompanyProfileEntity, data: com.dasariravi145.agrolynch.domain.model.ReceiptData): File? {
        val businessProfile = profile.toBusinessProfile()
        val invoiceData = InvoiceData(
            billNumber = data.receiptId,
            date = data.date,
            customerName = data.partyName,
            customerMobile = data.agentContact,
            products = data.items.map { 
                InvoiceProduct(
                    name = it.description,
                    productType = "",
                    grade = "",
                    unit = "QTY",
                    quantity = it.quantity.toDoubleOrNull() ?: 0.0,
                    rate = it.rate.toDoubleOrNull() ?: 0.0,
                    amount = it.amount.toDoubleOrNull() ?: 0.0
                ) 
            },
            subtotal = data.totalAmount,
            commission = 0.0,
            transport = 0.0,
            labour = 0.0,
            advance = 0.0,
            others = 0.0,
            grandTotal = data.totalAmount,
            vehicleNumber = ""
        )
        val templateId = mapTemplateId(profile.defaultTemplate)
        val config = templateRepository.getWizardConfig(profile.defaultTemplate)
        
        val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, invoiceData, config)
        return generatePdfFromHtml(context, html, "Receipt_${data.receiptId}")
    }

    suspend fun generateArchivePdf(context: Context, profile: CompanyProfileEntity, archive: AccountBookArchiveEntity, snapshot: BackupData): File? {
        val businessProfile = profile.toBusinessProfile()
        
        val products = mutableListOf<InvoiceProduct>()
        snapshot.arrivals.forEach {
            products.add(InvoiceProduct(it.productName, it.productType, it.grade, it.unit, it.finalNetWeightKg, it.ratePerKg, it.grossAmount))
        }
        snapshot.sales.forEach {
            products.add(InvoiceProduct("Sale: ${it.productName}", it.productType, it.grade, "QTY", it.totalQuantity, 0.0, it.totalNetAmount))
        }
        
        val invoiceData = InvoiceData(
            billNumber = "ARCHIVE-${archive.archiveId.take(8).uppercase()}",
            date = archive.archivedAt,
            customerName = archive.partyName,
            customerMobile = archive.partyPhone,
            products = products,
            subtotal = archive.totalAmount,
            commission = snapshot.arrivals.sumOf { it.commissionAmount },
            transport = snapshot.arrivals.sumOf { it.transportCharges } + snapshot.sales.sumOf { it.transportCharges },
            labour = snapshot.arrivals.sumOf { it.laborCharges } + snapshot.sales.sumOf { it.laborCharges },
            advance = archive.totalAmount - archive.paidAmount - archive.pendingAmount, // Rough estimate for display
            others = 0.0,
            grandTotal = archive.paidAmount,
            vehicleNumber = ""
        )
        val templateId = mapTemplateId(profile.defaultTemplate)
        val config = templateRepository.getWizardConfig(profile.defaultTemplate)
        
        val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, invoiceData, config)
        return generatePdfFromHtml(context, html, "Archive_${archive.partyName}_${archive.archiveId.take(8)}")
    }

    private fun mapTemplateId(type: String): String {
        return when (type) {
            "GK_FRUITS_CLASSIC" -> "gk_fruits_classic"
            "ROYAL_HERITAGE_MANDI" -> "royal_heritage_mandi"
            "DIAMOND_BUSINESS_ELITE" -> "diamond_business_elite"
            "PREMIUM_FRUIT_GALLERY" -> "premium_fruit_gallery"
            "EXECUTIVE_GLASS_STYLE" -> "executive_glass_style"
            "COMPACT_THERMAL_PRINT" -> "compact_print"
            else -> "gk_fruits_classic"
        }
    }

    private suspend fun generatePdfFromHtml(context: Context, html: String, fileName: String): File? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.settings.textZoom = 100
            
            // Set fixed width for A4 (794px)
            webView.layout(0, 0, 794, 1123)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()

                    val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MandiLedger/Invoices")
                    if (!pdfDir.exists()) pdfDir.mkdirs()
                    val pdfFile = File(pdfDir, "$fileName.pdf")

                    val adapter = webView.createPrintDocumentAdapter(fileName)
                    
                    // Use reflection to bypass package-private constructor issue if it occurs
                    // But actually, we can just use the adapter and let the PrintManager handle it
                    // OR drive it manually using a helper that uses reflection.
                    
                    try {
                        driveAdapterToPdf(adapter, attributes, pdfFile) { success ->
                            if (success) continuation.resume(pdfFile)
                            else continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to drive adapter")
                        continuation.resume(null)
                    }
                }
            }
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
        }
    }

    private fun driveAdapterToPdf(adapter: PrintDocumentAdapter, attributes: PrintAttributes, file: File, callback: (Boolean) -> Unit) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE)

            val layoutCallback = android.print.PrintResultWrapper.getLayoutCallback(
                onLayoutFinished = { info, changed ->
                    val writeCallback = android.print.PrintResultWrapper.getWriteCallback(
                        onWriteFinished = {
                            pfd.close()
                            callback(true)
                        },
                        onWriteFailed = {
                            try { pfd.close() } catch (e: Exception) {}
                            callback(false)
                        }
                    )
                    adapter.onWrite(arrayOf(PageRange.ALL_PAGES), pfd, CancellationSignal(), writeCallback)
                },
                onLayoutFailed = {
                    try { pfd.close() } catch (e: Exception) {}
                    callback(false)
                }
            )

            adapter.onLayout(null, attributes, CancellationSignal(), layoutCallback, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to drive adapter")
            callback(false)
        }
    }

    private fun CompanyProfileEntity.toBusinessProfile() = BusinessProfile(
        companyName = companyName,
        address = address,
        village = village,
        mobile = com.dasariravi145.agrolynch.util.Formatter.formatBusinessPhones(mobile1, mobile2),
        proprietor = proprietorName,
        gstNumber = gstNumber,
        tagline = tagline,
        logoPath = logoPath,
        qrPath = upiQrPath,
        signaturePath = signaturePath,
        godImagePath = godImagePath,
        stampPath = stampPath,
        watermarkImagePath = customTemplatePath,
        marketName = marketName,
        city = city,
        state = state,
        pincode = pincode
    )

    private fun List<ArrivalEntity>.toInvoiceData(mobile: String, deductions: List<EntryDeductionEntity>): InvoiceData {
        val first = this[0]
        val prods = this.map { 
            InvoiceProduct(
                name = it.productName, 
                productType = it.productType,
                grade = it.grade, 
                unit = if (it.unit == "Boxes") "KG" else it.unit, 
                quantity = it.finalNetWeightKg, 
                rate = it.ratePerKg, 
                amount = it.grossAmount
            ) 
        }
        
        val sub = sumOf { it.grossAmount }
        val comm = sumOf { it.commissionAmount }
        val lab = sumOf { it.laborCharges }
        val trans = sumOf { it.transportCharges }
        val pack = sumOf { it.packingCharges }
        
        // totalExtraDeductions includes everything from the extra deductions list (CAT, Advance, etc.)
        val totalExtraDeductions = sumOf { it.otherDeductions } 
        
        // Prefer saved netAmount from database for accuracy
        val savedNetAmount = sumOf { it.netAmount }
        val calculatedGrand = sub - comm - lab - trans - pack - totalExtraDeductions
        val finalGrand = if (savedNetAmount > 0) savedNetAmount else calculatedGrand

        // Breakdown for InvoiceData (separating Advance from others)
        val advanceAmt = deductions.filter { it.deductionType == "Advance" }.sumOf { it.amount }
        val othersAmt = (totalExtraDeductions - advanceAmt).coerceAtLeast(0.0)
        
        // Debug Log as requested
        Timber.d("FARMER_PDF_CALC: grossAmount=$sub, commission=$comm, labour=$lab, transport=$trans, others=${othersAmt + pack}, advance=$advanceAmt, netPayable=$finalGrand")

        return InvoiceData(
            billNumber = first.billNumber,
            date = first.date,
            customerName = first.farmerName,
            customerMobile = mobile,
            products = prods,
            subtotal = sub,
            commission = comm,
            transport = trans,
            labour = lab,
            advance = advanceAmt,
            others = othersAmt + pack,
            grandTotal = finalGrand,
            vehicleNumber = "", // Not yet implemented in entity
            customerGstin = "",
            customerState = "",
            placeOfSupply = "",
            reverseCharge = false,
            paymentMode = "CASH"
        )
    }
}
