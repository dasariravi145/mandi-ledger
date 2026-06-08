package com.dasariravi145.agrolynch.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.data.local.dao.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private const val PAGE_WIDTH_P = 595
    private const val PAGE_HEIGHT_P = 842
    private const val PAGE_WIDTH_L = 842
    private const val PAGE_HEIGHT_L = 595
    private const val MARGIN = 30f

    // Theme Colors
    private val themeGreen = Color.rgb(232, 245, 233)
    private val themeBlue = Color.rgb(227, 242, 253)
    private val themeOrange = Color.rgb(255, 243, 224)
    private val themePurple = Color.rgb(243, 229, 245)

    private val textGreen = Color.rgb(27, 94, 32)
    private val textBlue = Color.rgb(13, 71, 161)
    private val textOrange = Color.rgb(230, 81, 0)
    private val textPurple = Color.rgb(123, 31, 162)

    private val titlePaint = Paint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
    private val headerPaint = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
    private val bodyPaint = Paint().apply { textSize = 9f; color = Color.BLACK }
    private val subPaint = Paint().apply { textSize = 8f; color = Color.DKGRAY }
    private val borderPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE }
    private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f; style = Paint.Style.STROKE }
    private val fillPaint = Paint().apply { style = Paint.Style.FILL }

    fun generateLedgerPdf(context: Context, profile: CompanyProfileEntity, summary: LedgerSummary, isFarmer: Boolean): File? {
        val pdf = PdfDocument()
        var page = startNewPage(pdf, false) // Use Portrait for block layout
        var canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "${if(isFarmer) "FARMER" else "BUYER"} ACCOUNT STATEMENT", false)

        drawInfoRow(canvas, "Statement For: ${summary.partyName}", "Pending Amount Balance: ₹${String.format("%,.2f", summary.balance)}", y, false)
        y += 25f

        var totalQty = 0.0
        var totalGross = 0.0
        var totalCharges = 0.0
        var totalPaid = summary.totalCredit
        var totalNet = 0.0

        // Reverse to show chronological order if sorted by date desc usually
        val sortedEntries = summary.entries.sortedBy { it.date }

        sortedEntries.forEach { entry ->
            val d = entry.details ?: LedgerEntryDetails()
            val blocks = if (!isFarmer && entry.transactionType == TransactionType.SALE && d.saleItems.isNotEmpty()) {
                d.saleItems.map { item ->
                    TransactionBlock(
                        type = "SALE",
                        date = entry.date,
                        bgColor = themeBlue,
                        textColor = textBlue,
                        details = listOf(
                            "Farmer Name" to item.farmerName,
                            "Product" to item.productName,
                            "Grade" to item.grade,
                            "Quantity" to "${item.quantitySold} ${item.unit}",
                            "Sale Rate" to "₹${String.format("%,.2f", item.saleRate)}",
                            "Sale Amount" to "₹${String.format("%,.2f", item.saleAmount)}",
                            "Commission" to "₹${String.format("%,.2f", item.commissionAmount)}",
                            "Labor" to "₹${String.format("%,.2f", item.laborCharges)}",
                            "Transport" to "₹${String.format("%,.2f", item.transportCharges)}",
                            "Paid Amount" to "-",
                            "Pending Collection" to "₹${String.format("%,.2f", entry.balance)}"
                        )
                    )
                }
            } else if (isFarmer) {
                val isArrival = entry.transactionType == TransactionType.ARRIVAL
                if (isArrival) {
                    listOf(TransactionBlock(
                        type = "STOCK ARRIVAL",
                        date = entry.date,
                        bgColor = themeGreen,
                        textColor = textGreen,
                        details = listOf(
                            "Product" to d.productName,
                            "Grade" to d.grade,
                            "Quantity" to "${d.quantity} ${d.unit}",
                            "Damage / Soot" to "${d.damageQuantity} ${d.unit}",
                            "Net Quantity" to "${d.netQuantity} ${d.unit}",
                            "Rate" to "₹${String.format("%,.2f", d.rate)}",
                            "Gross Amount" to "₹${String.format("%,.2f", d.grossAmount)}",
                            "Commission" to "₹${String.format("%,.2f", d.commissionAmount)}",
                            "Labor" to "₹${String.format("%,.2f", d.laborCharges)}",
                            "Transport" to "₹${String.format("%,.2f", d.transportCharges)}",
                            "Packing" to "₹${String.format("%,.2f", d.packingCharges)}",
                            "Farmer Payable" to "₹${String.format("%,.2f", d.netAmount)}",
                            "Paid Amount" to "-",
                            "Pending Amount" to "₹${String.format("%,.2f", entry.balance)}"
                        )
                    ))
                } else {
                    listOf(TransactionBlock(
                        type = "PAYMENT",
                        date = entry.date,
                        bgColor = themeOrange,
                        textColor = textOrange,
                        details = listOf(
                            "Paid Amount" to "₹${String.format("%,.2f", entry.amount)}",
                            "Payment Mode" to entry.title.replace("Payment: ", ""),
                            "Pending Amount After Payment" to "₹${String.format("%,.2f", entry.balance)}"
                        )
                    ))
                }
            } else {
                // Buyer Collection
                listOf(TransactionBlock(
                    type = "COLLECTION",
                    date = entry.date,
                    bgColor = themePurple,
                    textColor = textPurple,
                    details = listOf(
                        "Collected Amount" to "₹${String.format("%,.2f", entry.amount)}",
                        "Payment Mode" to entry.title.replace("Receipt: ", ""),
                        "Pending Collection After Payment" to "₹${String.format("%,.2f", entry.balance)}"
                    )
                ))
            }

            blocks.forEach { block ->
                val blockHeight = 25f + (block.details.size * 15f) + 10f
                if (y + blockHeight > PAGE_HEIGHT_P - 80f) {
                    drawFooter(canvas, profile, pdf.pages.size, false)
                    pdf.finishPage(page)
                    page = startNewPage(pdf, false)
                    canvas = page.canvas
                    y = drawProfessionalHeader(canvas, profile, "${if(isFarmer) "FARMER" else "BUYER"} ACCOUNT STATEMENT (Cont.)", false)
                    y += 10f
                }
                y = drawTransactionBlock(canvas, block, y)
            }

            // Accumulate totals
            if (isFarmer && entry.transactionType == TransactionType.ARRIVAL) {
                totalQty += d.quantity
                totalGross += d.grossAmount
                totalCharges += (d.commissionAmount + d.laborCharges + d.transportCharges + d.packingCharges + d.otherDeductions)
                totalNet += d.netAmount
            } else if (!isFarmer && entry.transactionType == TransactionType.SALE) {
                totalQty += d.quantity
                totalGross += d.grossAmount
                totalCharges += (d.commissionAmount + d.laborCharges + d.transportCharges + d.otherDeductions)
                totalNet += d.netAmount
            }
        }

        if (y + 100f > PAGE_HEIGHT_P - 80f) {
            drawFooter(canvas, profile, pdf.pages.size, false)
            pdf.finishPage(page)
            page = startNewPage(pdf, false)
            canvas = page.canvas
            y = drawProfessionalHeader(canvas, profile, "SUMMARY", false)
        }

        y += 15f
        y = drawSummarySection(canvas, totalQty, totalGross, totalCharges, totalNet, totalPaid, summary.balance, y, false)

        drawFooter(canvas, profile, pdf.pages.size, false)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Ledgers", "${if (isFarmer) "Farmer" else "Buyer"}_Ledger_${summary.partyName}")
    }

    private data class TransactionBlock(
        val type: String,
        val date: Long,
        val bgColor: Int,
        val textColor: Int,
        val details: List<Pair<String, String>>
    )

    private fun drawTransactionBlock(canvas: Canvas, block: TransactionBlock, y: Float): Float {
        val endX = PAGE_WIDTH_P - MARGIN
        var cy = y

        // Block Header
        fillPaint.color = block.bgColor
        canvas.drawRect(MARGIN, cy, endX, cy + 20f, fillPaint)
        canvas.drawRect(MARGIN, cy, endX, cy + 20f, borderPaint)

        val headerTextPaint = Paint(headerPaint).apply { color = block.textColor }
        canvas.drawText("${block.type} - ${formatDate(block.date)}", MARGIN + 8f, cy + 14f, headerTextPaint)
        cy += 20f

        // Block Details
        block.details.forEach { (label, value) ->
            canvas.drawText(label, MARGIN + 10f, cy + 12f, bodyPaint)
            val valWidth = bodyPaint.measureText(value)
            canvas.drawText(value, endX - valWidth - 10f, cy + 12f, bodyPaint)
            cy += 15f
        }

        // Block Border (entire)
        canvas.drawRect(MARGIN, y, endX, cy + 5f, borderPaint)
        
        return cy + 15f
    }

    private fun drawSummarySection(canvas: Canvas, qty: Double, gross: Double, charges: Double, net: Double, paid: Double, pending: Double, y: Float, isLandscape: Boolean): Float {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        val endX = width - MARGIN
        var cy = y

        fillPaint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(MARGIN, cy, endX, cy + 100f, fillPaint)
        canvas.drawRect(MARGIN, cy, endX, cy + 100f, borderPaint)

        cy += 15f
        val labelX = MARGIN + 15f
        val valX = width - MARGIN - 15f
        
        cy = drawSummaryRow(canvas, "Total Quantity Business:", "${String.format("%,.2f", qty)} Units", cy, labelX, valX)
        cy = drawSummaryRow(canvas, "Total Gross Amount:", "₹${String.format("%,.2f", gross)}", cy, labelX, valX)
        cy = drawSummaryRow(canvas, "Total Charges / Deductions:", "₹${String.format("%,.2f", charges)}", cy, labelX, valX)
        cy = drawSummaryRow(canvas, "Net Business Amount:", "₹${String.format("%,.2f", net)}", cy, labelX, valX, true)
        cy = drawSummaryRow(canvas, "Total Paid Amount:", "₹${String.format("%,.2f", paid)}", cy, labelX, valX)
        cy = drawSummaryRow(canvas, "Pending Amount Balance:", "₹${String.format("%,.2f", pending)}", cy, labelX, valX, true, if(pending > 0) Color.RED else textGreen)

        return cy + 10f
    }

    private fun drawSummaryRow(canvas: Canvas, label: String, value: String, y: Float, lX: Float, vX: Float, isBold: Boolean = false, color: Int = Color.BLACK): Float {
        val p = if (isBold) headerPaint else bodyPaint
        val vp = Paint(p).apply { this.color = color; textAlign = Paint.Align.RIGHT }
        canvas.drawText(label, lX, y, p)
        canvas.drawText(value, vX, y, vp)
        return y + 15f
    }

    private fun drawProfessionalHeader(canvas: Canvas, profile: CompanyProfileEntity, title: String, isLandscape: Boolean): Float {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        val endX = width - MARGIN
        var cy = MARGIN

        // Logo Top-Left (Vertical Center)
        profile.logoPath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                val rect = RectF(MARGIN, cy, MARGIN + 60, cy + 60)
                canvas.drawBitmap(bmp, null, rect, null)
            }
        }

        // God Image Top-Right (Vertical Center)
        profile.godImagePath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                val rect = RectF(endX - 60, cy, endX, cy + 60)
                canvas.drawBitmap(bmp, null, rect, null)
            }
        }

        // Center Content
        val centerX = width / 2f
        val namePaint = Paint(titlePaint).apply { textSize = 22f; color = textGreen; textAlign = Paint.Align.CENTER }
        val centerHeaderPaint = Paint(headerPaint).apply { textAlign = Paint.Align.CENTER; textSize = 10f }
        val centerSubPaint = Paint(subPaint).apply { textAlign = Paint.Align.CENTER; textSize = 9f }

        canvas.drawText(profile.companyName.uppercase(), centerX, cy + 20f, namePaint)
        canvas.drawText("Proprietor: ${profile.proprietorName}", centerX, cy + 38f, centerHeaderPaint)
        canvas.drawText("${profile.address}, ${profile.village}", centerX, cy + 52f, centerSubPaint)
        canvas.drawText("${profile.district}, ${profile.state}", centerX, cy + 64f, centerSubPaint)
        
        if (profile.gstNumber.isNotEmpty()) {
            canvas.drawText("GSTIN: ${profile.gstNumber}", centerX, cy + 78f, centerHeaderPaint)
        }

        // Phone Numbers Right Aligned below God Image
        val phoneStr = "Mob: ${profile.mobile1}${if(profile.mobile2.isNotEmpty()) " / " + profile.mobile2 else ""}"
        val phonePaint = Paint(subPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 9f; isFakeBoldText = true }
        canvas.drawText(phoneStr, endX, cy + 72f, phonePaint)

        cy += 100f
        val invoiceTitlePaint = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER; textSize = 14f; color = Color.DKGRAY }
        canvas.drawText(title, centerX, cy, invoiceTitlePaint)
        
        return cy + 25f
    }

    private fun startNewPage(pdf: PdfDocument, isLandscape: Boolean): PdfDocument.Page {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        val height = if (isLandscape) PAGE_HEIGHT_L else PAGE_HEIGHT_P
        return pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pdf.pages.size + 1).create())
    }

    private fun drawFooter(canvas: Canvas, profile: CompanyProfileEntity, pageNum: Int, isLandscape: Boolean) {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        val height = if (isLandscape) PAGE_HEIGHT_L else PAGE_HEIGHT_P
        val endX = width - MARGIN
        val bottomY = height - 25f
        
        // Right Aligned Signature Section
        val signPaint = Paint(subPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT_BOLD }
        val normalSignPaint = Paint(subPaint).apply { textAlign = Paint.Align.RIGHT }
        
        var currentSignY = bottomY - 100f
        
        // "For <Company Name>"
        canvas.drawText("For ${profile.companyName.uppercase()}", endX, currentSignY, signPaint)
        
        // Company Stamp Image (Above Signature)
        profile.stampPath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                canvas.drawBitmap(bmp, null, RectF(endX - 100f, currentSignY + 5f, endX, currentSignY + 55f), null)
            }
        }
        
        currentSignY += 65f
        
        // Authorized Signature Text
        canvas.drawText("Authorized Signature", endX, currentSignY, normalSignPaint)
        
        // Signature Image (If any, overlaying or near Authorized Signature)
        profile.signaturePath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                canvas.drawBitmap(bmp, null, RectF(endX - 90f, currentSignY - 40f, endX - 10f, currentSignY - 5f), null)
            }
        }
        
        currentSignY += 15f
        
        // Proprietor Signature Text
        canvas.drawText("Proprietor Signature", endX, currentSignY, normalSignPaint)

        // Bottom Footer Line and Info
        canvas.drawLine(MARGIN, bottomY - 12f, endX, bottomY - 12f, linePaint)
        canvas.drawText("Page $pageNum", MARGIN, bottomY, subPaint)
        val timeStr = "Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}"
        canvas.drawText(timeStr, endX - subPaint.measureText(timeStr), bottomY, subPaint)
    }

    private fun drawInfoRow(canvas: Canvas, left: String, right: String, y: Float, isLandscape: Boolean) {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        canvas.drawText(left, MARGIN, y, headerPaint)
        if (right.isNotEmpty()) {
            val rw = headerPaint.measureText(right)
            canvas.drawText(right, width - MARGIN - rw, y, headerPaint)
        }
    }

    private fun savePdf(pdf: PdfDocument, context: Context, dir: String, fileName: String): File? {
        val root = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgroLynch/$dir")
        if (!root.exists()) root.mkdirs()
        val file = File(root, "${fileName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        return try { FileOutputStream(file).use { pdf.writeTo(it) }; pdf.close(); file } catch (e: Exception) { pdf.close(); null }
    }

    private fun formatDate(t: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t))
    private fun formatDateTime(t: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(t))

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply { 
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) 
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    }

    // --- RECEIPT GENERATORS ---

    fun generateBuyerPaymentReceipt(context: Context, profile: CompanyProfileEntity, payment: PaymentEntity, items: List<SaleItemEntity>): File? {
        val pdf = PdfDocument()
        val page = startNewPage(pdf, false)
        val canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "BUYER COLLECTION RECEIPT", false)

        val block = TransactionBlock(
            type = "COLLECTION",
            date = payment.date,
            bgColor = themePurple,
            textColor = textPurple,
            details = listOf(
                "Buyer Name" to payment.partyName,
                "Reference" to payment.referenceNumber,
                "Collected Amount" to "₹${String.format("%,.2f", payment.amount)}",
                "Payment Mode" to payment.paymentMode,
                "Pending Amount Balance" to "₹${String.format("%,.2f", payment.remainingBalance)}"
            )
        )
        y = drawTransactionBlock(canvas, block, y)

        if (items.isNotEmpty()) {
            y += 10f
            canvas.drawText("Adjusted Sale Items:", MARGIN + 10f, y + 12f, headerPaint)
            y += 20f
            items.forEach { item ->
                val itemBlock = TransactionBlock(
                    type = "SALE ADJUSTMENT",
                    date = item.date,
                    bgColor = themeBlue.adjustAlpha(0.2f),
                    textColor = textBlue,
                    details = listOf(
                        "Farmer" to item.farmerName,
                        "Product" to "${item.productName} (${item.grade})",
                        "Qty Sold" to "${item.quantitySold} ${item.unit}",
                        "Amount" to "₹${String.format("%,.2f", item.saleAmount)}"
                    )
                )
                if (y + 100f > PAGE_HEIGHT_P - 80f) {
                    drawFooter(canvas, profile, pdf.pages.size, false)
                    pdf.finishPage(page)
                    return savePdf(pdf, context, "Receipts", "Buyer_Collection_${payment.id}")
                }
                y = drawTransactionBlock(canvas, itemBlock, y)
            }
        }

        drawFooter(canvas, profile, 1, false)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Receipts", "Buyer_Collection_${payment.id}")
    }

    fun generateFarmerPaymentReceipt(context: Context, profile: CompanyProfileEntity, payment: PaymentEntity, arrival: ArrivalEntity): File? {
        val pdf = PdfDocument()
        val page = startNewPage(pdf, false)
        val canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "FARMER PAYMENT RECEIPT", false)

        val block = TransactionBlock(
            type = "PAYMENT",
            date = payment.date,
            bgColor = themeOrange,
            textColor = textOrange,
            details = listOf(
                "Farmer Name" to payment.partyName,
                "Paid Amount" to "₹${String.format("%,.2f", payment.amount)}",
                "Payment Mode" to payment.paymentMode,
                "Pending Amount Balance" to "₹${String.format("%,.2f", payment.remainingBalance)}"
            )
        )
        y = drawTransactionBlock(canvas, block, y)

        y += 10f
        val stockBlock = TransactionBlock(
            type = "STOCK DETAILS",
            date = arrival.date,
            bgColor = themeGreen,
            textColor = textGreen,
            details = listOf(
                "Product" to arrival.productName,
                "Grade" to arrival.grade,
                "Quantity" to "${arrival.quantity} ${arrival.unit}",
                "Net Amount" to "₹${String.format("%,.2f", arrival.netAmount)}"
            )
        )
        y = drawTransactionBlock(canvas, stockBlock, y)

        drawFooter(canvas, profile, 1, false)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Receipts", "Farmer_Payment_${payment.id}")
    }

    fun generateFarmerArrivalPdf(context: Context, profile: CompanyProfileEntity, arrivals: List<ArrivalEntity>): File? {
        val pdf = PdfDocument()
        val page = startNewPage(pdf, false)
        val canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "STOCK ARRIVAL RECEIPT", false)

        arrivals.forEach { arrival ->
            val block = TransactionBlock(
                type = "ARRIVAL",
                date = arrival.date,
                bgColor = themeGreen,
                textColor = textGreen,
                details = listOf(
                    "Product" to arrival.productName,
                    "Grade" to arrival.grade,
                    "Quantity" to "${arrival.quantity} ${arrival.unit}",
                    "Gross Amount" to "₹${String.format("%,.2f", arrival.grossAmount)}",
                    "Net Payable" to "₹${String.format("%,.2f", arrival.netAmount)}"
                )
            )
            if (y + 120f > PAGE_HEIGHT_P - 80f) {
                drawFooter(canvas, profile, pdf.pages.size, false)
                pdf.finishPage(page)
                return savePdf(pdf, context, "Arrivals", "Farmer_Arrival")
            }
            y = drawTransactionBlock(canvas, block, y)
        }

        drawFooter(canvas, profile, 1, false)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Arrivals", "Farmer_Arrival")
    }

    fun generateBuyerSalePdf(context: Context, profile: CompanyProfileEntity, sale: SaleEntity, items: List<SaleItemEntity>): File? {
        val pdf = PdfDocument()
        val page = startNewPage(pdf, false)
        val canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "SALE INVOICE", false)

        val headerBlock = TransactionBlock(
            type = "INVOICE",
            date = sale.date,
            bgColor = themeBlue,
            textColor = textBlue,
            details = listOf(
                "Buyer Name" to sale.buyerName,
                "Total Quantity" to "${sale.totalQuantity}",
                "Total Amount" to "₹${String.format("%,.2f", sale.totalAmount)}",
                "Final Collection" to "₹${String.format("%,.2f", sale.totalNetAmount)}"
            )
        )
        y = drawTransactionBlock(canvas, headerBlock, y)

        y += 10f
        items.forEach { item ->
            val itemBlock = TransactionBlock(
                type = "ITEM",
                date = item.date,
                bgColor = Color.WHITE,
                textColor = Color.BLACK,
                details = listOf(
                    "Farmer" to item.farmerName,
                    "Product" to "${item.productName} (${item.grade})",
                    "Qty Sold" to "${item.quantitySold} ${item.unit}",
                    "Rate" to "₹${String.format("%,.2f", item.saleRate)}",
                    "Amount" to "₹${String.format("%,.2f", item.saleAmount)}"
                )
            )
            if (y + 100f > PAGE_HEIGHT_P - 80f) {
                drawFooter(canvas, profile, pdf.pages.size, false)
                pdf.finishPage(page)
                return savePdf(pdf, context, "Sales", "Sale_Invoice_${sale.id}")
            }
            y = drawTransactionBlock(canvas, itemBlock, y)
        }

        drawFooter(canvas, profile, 1, false)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Sales", "Sale_Invoice_${sale.id}")
    }

    private fun Int.adjustAlpha(factor: Float): Int {
        val alpha = (Color.alpha(this) * factor).toInt()
        return Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
    }

    // --- COMPATIBILITY STUBS ---
    fun generatePaymentReceiptPdf(c: Context, pr: CompanyProfileEntity, pa: PaymentEntity): File? = generateBuyerPaymentReceipt(c, pr, pa, emptyList())
    fun generateFarmerReport(context: Context, profile: CompanyProfileEntity, farmer: FarmerEntity, data: List<DetailedArrivalReportModel>, range: String): File? {
        val pdf = PdfDocument()
        var page = startNewPage(pdf, true)
        var canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "FARMER TRANSACTION REPORT", true)
        
        drawInfoRow(canvas, "Period: $range", "Total Records: ${data.size}", y, true)
        y += 25f

        val headers = listOf("Date", "Product", "Grade", "Qty", "Rate", "Gross", "Comm.", "Net Payable")
        val colWidths = floatArrayOf(80f, 100f, 60f, 70f, 70f, 80f, 70f, 90f)
        y = drawTableHeaders(canvas, headers, colWidths, y, true)

        data.forEach { item ->
            if (y > PAGE_HEIGHT_L - 80f) {
                drawFooter(canvas, profile, pdf.pages.size, true)
                pdf.finishPage(page)
                page = startNewPage(pdf, true)
                canvas = page.canvas
                y = drawProfessionalHeader(canvas, profile, "FARMER TRANSACTION REPORT (Cont.)", true)
                y += 10f
                y = drawTableHeaders(canvas, headers, colWidths, y, true)
            }
            val row = listOf(
                formatDate(item.date),
                item.productName,
                item.grade,
                "${item.quantity} ${item.unit}",
                "₹${String.format("%.2f", item.rate)}",
                "₹${String.format("%.0f", item.grossAmount)}",
                "₹${String.format("%.0f", item.commissionAmount)}",
                "₹${String.format("%.2f", item.netAmount)}"
            )
            y = drawTableRow(canvas, row, colWidths, y, true)
        }

        drawFooter(canvas, profile, pdf.pages.size, true)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Reports", "Farmer_Report_${System.currentTimeMillis()}")
    }

    fun generateBuyerReport(context: Context, profile: CompanyProfileEntity, buyer: BuyerEntity, data: List<DetailedSaleReportModel>, range: String): File? {
        val pdf = PdfDocument()
        var page = startNewPage(pdf, true)
        var canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "BUYER SALES REPORT", true)
        
        drawInfoRow(canvas, "Period: $range", "Total Records: ${data.size}", y, true)
        y += 25f

        val headers = listOf("Date", "Buyer Name", "Product", "Grade", "Qty", "Rate", "Charges", "Total Amount")
        val colWidths = floatArrayOf(80f, 120f, 100f, 60f, 70f, 70f, 80f, 90f)
        y = drawTableHeaders(canvas, headers, colWidths, y, true)

        data.forEach { item ->
            if (y > PAGE_HEIGHT_L - 80f) {
                drawFooter(canvas, profile, pdf.pages.size, true)
                pdf.finishPage(page)
                page = startNewPage(pdf, true)
                canvas = page.canvas
                y = drawProfessionalHeader(canvas, profile, "BUYER SALES REPORT (Cont.)", true)
                y += 10f
                y = drawTableHeaders(canvas, headers, colWidths, y, true)
            }
            val charges = item.laborCharges + item.transportCharges + item.otherCharges
            val row = listOf(
                formatDate(item.date),
                item.buyerName,
                item.productName,
                item.grade,
                "${item.quantity} ${item.unit}",
                "₹${String.format("%.2f", item.rate)}",
                "₹${String.format("%.0f", charges)}",
                "₹${String.format("%.2f", item.totalAmount)}"
            )
            y = drawTableRow(canvas, row, colWidths, y, true)
        }

        drawFooter(canvas, profile, pdf.pages.size, true)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Reports", "Buyer_Report_${System.currentTimeMillis()}")
    }

    fun generateCommissionReport(context: Context, profile: CompanyProfileEntity, data: List<CommissionReportModel>, range: String): File? {
        val pdf = PdfDocument()
        var page = startNewPage(pdf, true)
        var canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "COMMISSION EARNINGS REPORT", true)
        
        drawInfoRow(canvas, "Period: $range", "Total Records: ${data.size}", y, true)
        y += 25f

        val headers = listOf("Date", "Buyer", "Farmer", "Product", "Qty", "Sale Amt", "Comm %", "Earned")
        val colWidths = floatArrayOf(80f, 100f, 100f, 100f, 70f, 80f, 60f, 90f)
        y = drawTableHeaders(canvas, headers, colWidths, y, true)

        data.forEach { item ->
            if (y > PAGE_HEIGHT_L - 80f) {
                drawFooter(canvas, profile, pdf.pages.size, true)
                pdf.finishPage(page)
                page = startNewPage(pdf, true)
                canvas = page.canvas
                y = drawProfessionalHeader(canvas, profile, "COMMISSION EARNINGS (Cont.)", true)
                y += 10f
                y = drawTableHeaders(canvas, headers, colWidths, y, true)
            }
            val row = listOf(
                formatDate(item.date),
                item.buyerName,
                item.farmerName,
                item.productName,
                "${item.quantity} KG",
                "₹${String.format("%.0f", item.saleAmount)}",
                "${item.commissionPercent}%",
                "₹${String.format("%.2f", item.commissionAmount)}"
            )
            y = drawTableRow(canvas, row, colWidths, y, true)
        }

        drawFooter(canvas, profile, pdf.pages.size, true)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Reports", "Commission_Report_${System.currentTimeMillis()}")
    }

    private fun drawTableHeaders(canvas: Canvas, headers: List<String>, widths: FloatArray, y: Float, isLandscape: Boolean): Float {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        val endX = width - MARGIN
        
        fillPaint.color = Color.rgb(240, 240, 240)
        canvas.drawRect(MARGIN, y, endX, y + 20f, fillPaint)
        canvas.drawRect(MARGIN, y, endX, y + 20f, borderPaint)

        var curX = MARGIN + 5f
        headers.forEachIndexed { i, text ->
            canvas.drawText(text, curX, y + 14f, headerPaint)
            curX += widths[i]
        }
        return y + 20f
    }

    private fun drawTableRow(canvas: Canvas, row: List<String>, widths: FloatArray, y: Float, isLandscape: Boolean): Float {
        val width = if (isLandscape) PAGE_WIDTH_L else PAGE_WIDTH_P
        val endX = width - MARGIN
        
        var curX = MARGIN + 5f
        row.forEachIndexed { i, text ->
            canvas.drawText(text, curX, y + 14f, bodyPaint)
            curX += widths[i]
        }
        
        canvas.drawLine(MARGIN, y + 20f, endX, y + 20f, borderPaint)
        return y + 20f
    }

    fun generateDashboardSummary(context: Context, profile: CompanyProfileEntity, summary: DashboardSummary, stock: List<StockReportModel>): File? {
        val pdf = PdfDocument()
        val page = startNewPage(pdf, false)
        val canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "BUSINESS SUMMARY REPORT", false)

        y = drawSummarySection(canvas, 0.0, summary.todaySales, summary.commissionEarned, summary.todaySales, summary.cashReceived, summary.buyerPending, y, false)
        
        y += 30f
        canvas.drawText("CURRENT STOCK LEVELS", MARGIN, y, headerPaint)
        y += 10f
        
        val headers = listOf("Product", "Total Quantity", "Unit")
        val widths = floatArrayOf(200f, 150f, 100f)
        y = drawTableHeaders(canvas, headers, widths, y, false)
        
        stock.forEach { item ->
            val row = listOf(item.productName, String.format("%.2f", item.totalQuantity), item.unit)
            y = drawTableRow(canvas, row, widths, y, false)
        }

        drawFooter(canvas, profile, 1, false)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Reports", "Business_Summary_${System.currentTimeMillis()}")
    }

    fun generatePaymentReport(context: Context, profile: CompanyProfileEntity, data: List<PaymentReportModel>, range: String): File? {
        val pdf = PdfDocument()
        var page = startNewPage(pdf, true)
        var canvas = page.canvas
        var y = drawProfessionalHeader(canvas, profile, "PAYMENT HISTORY REPORT", true)
        
        drawInfoRow(canvas, "Period: $range", "Total Records: ${data.size}", y, true)
        y += 25f

        val headers = listOf("Date", "Party Name", "Type", "Mode", "Amount", "Balance", "Status")
        val colWidths = floatArrayOf(80f, 150f, 80f, 100f, 100f, 100f, 80f)
        y = drawTableHeaders(canvas, headers, colWidths, y, true)

        data.forEach { item ->
            if (y > PAGE_HEIGHT_L - 80f) {
                drawFooter(canvas, profile, pdf.pages.size, true)
                pdf.finishPage(page)
                page = startNewPage(pdf, true)
                canvas = page.canvas
                y = drawProfessionalHeader(canvas, profile, "PAYMENT HISTORY (Cont.)", true)
                y += 10f
                y = drawTableHeaders(canvas, headers, colWidths, y, true)
            }
            val row = listOf(
                formatDate(item.date),
                item.partyName,
                item.partyType,
                item.paymentMode,
                "₹${String.format("%.2f", item.amount)}",
                "₹${String.format("%.2f", item.remainingBalance)}",
                item.status
            )
            y = drawTableRow(canvas, row, colWidths, y, true)
        }

        drawFooter(canvas, profile, pdf.pages.size, true)
        pdf.finishPage(page)
        return savePdf(pdf, context, "Reports", "Payment_Report_${System.currentTimeMillis()}")
    }

    fun generateReceiptPdf(c: Context, d: ReceiptData): File? = null
    fun generateBackupPDF(c: Context, f: List<FarmerEntity>, b: List<BuyerEntity>, s: List<SaleEntity>, a: List<ArrivalEntity>, pr: List<ProductEntity>, ex: List<ExpenseEntity>, pa: List<PaymentEntity>, t: String): File? = null
}
