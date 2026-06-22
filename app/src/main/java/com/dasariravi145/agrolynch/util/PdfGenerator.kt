package com.dasariravi145.agrolynch.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun printPdf(context: Context, file: File) {
        val activity = context.findActivity()
        if (activity != null) {
            android.util.Log.d("PRINT_DEBUG", "context=${context::class.java.name}, activity=${activity::class.java.name}")
            val uri = getUriFromFile(context, file)
            PdfPrintHelper.print(activity, uri)
        } else {
            android.widget.Toast.makeText(context, "Print requires active screen", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun getUriFromFile(context: Context, file: File): android.net.Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun sharePdf(context: Context, file: File) {
        val uri = getUriFromFile(context, file)
        PdfActionManager.sharePdf(context, uri)
    }

    fun generateFarmerReport(context: Context, profile: CompanyProfileEntity, farmer: FarmerEntity, data: List<DetailedArrivalReportModel>, range: String): File? {
        Timber.d("FarmerReportPdf: entries count = ${data.size}")
        val pdf = PdfDocument()
        val title = if (farmer.name.isNotBlank()) "FARMER REPORT: ${farmer.name}" else "FARMER REPORT"
        val headers = listOf("Farmer", "Bill No", "Date", "Gross Amount", "Commission", "Net Payable")
        val alignments = listOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)
        
        val tableData = data.map { item ->
            listOf(
                item.farmerName,
                item.billNumber,
                formatDateProfessional(item.date),
                "₹${Formatter.formatCurrencyStrict(item.grossAmount)}",
                "₹${Formatter.formatCurrencyStrict(item.commissionAmount)}",
                "₹${Formatter.formatCurrencyStrict(item.netAmount)}"
            )
        }
        val summary = mapOf(
            "GROSS AMOUNT" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.grossAmount })}",
            "COMMISSION" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.commissionAmount })}",
            "NET PAYABLE" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.netAmount })}"
        )
        
        drawProfessionalReport(pdf, profile, title, range, headers, tableData, summary, alignments)
        val file = savePdf(pdf, context, "Reports", "FarmerReport")
        Timber.d("FarmerReportPdf: pdf created = ${file?.absolutePath}")
        return file
    }

    fun generateBuyerReport(context: Context, profile: CompanyProfileEntity, buyer: BuyerEntity, data: List<DetailedSaleReportModel>, range: String): File? {
        Timber.d("BuyerReportPdf: entries count = ${data.size}")
        val pdf = PdfDocument()
        val title = if (buyer.name.isNotBlank()) "BUYER REPORT: ${buyer.name}" else "BUYER REPORT"
        val headers = listOf("Buyer", "Bill No", "Date", "Sale Amount", "Payments", "Balance")
        val alignments = listOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)

        val tableData = data.map { item ->
            listOf(
                item.buyerName,
                item.billNumber,
                formatDateProfessional(item.date),
                "₹${Formatter.formatCurrencyStrict(item.saleAmount)}",
                "₹${Formatter.formatCurrencyStrict(item.paidAmount)}",
                "₹${Formatter.formatCurrencyStrict(item.totalAmount - item.paidAmount)}"
            )
        }
        val summary = mapOf(
            "TOTAL SALES" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.saleAmount })}",
            "TOTAL PAYMENTS" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.paidAmount })}",
            "TOTAL BALANCE" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.totalAmount - it.paidAmount })}"
        )
        
        drawProfessionalReport(pdf, profile, title, range, headers, tableData, summary, alignments)
        val file = savePdf(pdf, context, "Reports", "BuyerReport")
        Timber.d("BuyerReportPdf: pdf created = ${file?.absolutePath}")
        return file
    }

    fun generateCommissionReport(context: Context, profile: CompanyProfileEntity, data: List<CommissionReportModel>, range: String): File? {
        Timber.d("BusinessReportPdf: entries count = ${data.size}")
        val pdf = PdfDocument()
        val headers = listOf("Date", "Farmer", "Item/Grade", "Qty", "Rate", "Gross", "Comm %", "Earned")
        val tableData = data.map { item ->
            listOf(
                formatDate(item.date),
                item.farmerName,
                "${item.productName}\n${item.grade}",
                Formatter.formatWeight(item.quantity),
                Formatter.formatCurrency(item.rate),
                Formatter.formatCurrency(item.grossAmount),
                "${Formatter.formatWeight(item.commissionPercent)}%",
                Formatter.formatCurrency(item.commissionAmount)
            )
        }
        val summary = mapOf(
            "TOTAL GROSS" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.grossAmount })}",
            "TOTAL COMM" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.commissionAmount })}"
        )
        
        drawProfessionalReport(pdf, profile, "COMMISSION REPORT", range, headers, tableData, summary, List(headers.size) { if (it == 0) Paint.Align.LEFT else if (it == 2) Paint.Align.LEFT else Paint.Align.RIGHT })
        val file = savePdf(pdf, context, "Reports", "CommissionReport")
        Timber.d("CommissionReportPdf: pdf created = ${file?.absolutePath}")
        return file
    }

    fun generatePaymentReport(context: Context, profile: CompanyProfileEntity, data: List<PaymentReportModel>, range: String): File? {
        Timber.d("PaymentReportPdf: entries count = ${data.size}")
        val pdf = PdfDocument()
        val headers = listOf("Date", "Party Name", "Type", "Mode", "Amount", "Balance")
        val alignments = listOf(Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT)
        
        val tableData = data.map { item ->
            listOf(
                formatDateProfessional(item.date),
                item.partyName,
                item.partyType,
                item.paymentMode,
                "₹${Formatter.formatCurrencyStrict(item.amount)}",
                "₹${Formatter.formatCurrencyStrict(item.remainingBalance)}"
            )
        }
        val summary = mapOf(
            "TOTAL PAID/RECV" to "₹${Formatter.formatCurrencyStrict(data.sumOf { it.amount })}"
        )
        
        drawProfessionalReport(pdf, profile, "PAYMENT REPORT", range, headers, tableData, summary, alignments)
        val file = savePdf(pdf, context, "Reports", "PaymentReport")
        Timber.d("PaymentReportPdf: pdf created = ${file?.absolutePath}")
        return file
    }

    fun generateOutstandingAgingReport(context: Context, profile: CompanyProfileEntity, data: List<OutstandingAgingModel>, range: String): File? {
        return try {
            Timber.d("PendingPaymentsPdf: entries count = ${data.size}")
            val pdf = PdfDocument()
            val headers = listOf("Name", "Type", "Pending Amount", "Last Payment", "Age")
            val alignments = listOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.CENTER, Paint.Align.RIGHT)
            
            val tableData = if (data.isEmpty()) {
                Timber.d("PendingPaymentsPdf: Data is empty, creating empty report.")
                emptyList<List<String>>()
            } else {
                data.map { item ->
                    listOf(
                        item.name ?: "Unknown",
                        item.type ?: "PARTY",
                        "₹${Formatter.formatCurrencyStrict(item.pendingAmount ?: 0.0)}",
                        item.lastPaymentDate?.let { formatDateProfessional(it) } ?: "-",
                        "${item.daysPending ?: 0} days"
                    )
                }
            }
            val totalPending = data.sumOf { it.pendingAmount ?: 0.0 }
            val avgAge = if (data.isNotEmpty()) data.map { it.daysPending ?: 0 }.average().takeIf { !it.isNaN() } ?: 0.0 else 0.0
            
            val summary = mapOf(
                "TOTAL PENDING" to "₹${Formatter.formatCurrencyStrict(totalPending)}",
                "AVERAGE AGE" to "${Formatter.formatWeight(avgAge)} Days"
            )
            
            val title = "PENDING PAYMENTS REPORT"
            drawProfessionalReport(pdf, profile, title, range, headers, tableData, summary, alignments)
            val file = savePdf(pdf, context, "Reports", "PendingPayments")
            Timber.d("PendingPaymentsPdf: pdf created = ${file?.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "PendingPaymentsPdf: PDF generation failed")
            null
        }
    }

    fun generateProductPerformanceReport(context: Context, profile: CompanyProfileEntity, data: List<ProductPerformanceModel>, range: String): File? {
        return try {
            Timber.d("ItemStatsPdf: entries count = ${data.size}")
            val pdf = PdfDocument()
            val headers = listOf("Product", "Grade", "Arrivals", "Sold", "Stock", "Avg Buy", "Avg Sale", "Margin")
            val alignments = listOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)
            
            val tableData = if (data.isEmpty()) emptyList() else data.map { item ->
                listOf(
                    item.productName,
                    item.grade,
                    Formatter.formatWeight(item.totalArrivals),
                    Formatter.formatWeight(item.totalSold),
                    Formatter.formatWeight(item.currentStock),
                    "₹${Formatter.formatCurrencyStrict(item.avgPurchaseRate)}",
                    "₹${Formatter.formatCurrencyStrict(item.avgSaleRate)}",
                    "₹${Formatter.formatCurrencyStrict(item.avgSaleRate - item.avgPurchaseRate)}"
                )
            }
            
            drawProfessionalReport(pdf, profile, "ITEM STATS", range, headers, tableData, emptyMap(), alignments)
            val file = savePdf(pdf, context, "Reports", "ItemStats")
            Timber.d("ItemStatsPdf: pdf created = ${file?.absolutePath}")
            file
        } catch (e: Exception) {
            android.util.Log.e("ItemStatsPdf", "PDF generation failed", e)
            null
        }
    }

    private fun drawProfessionalReport(
        pdf: PdfDocument,
        profile: CompanyProfileEntity,
        reportTitle: String,
        range: String,
        headers: List<String>,
        tableData: List<List<String>>,
        summary: Map<String, String>,
        alignments: List<Paint.Align>
    ) {
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        
        val paint = Paint()
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.LTGRAY
        }
        val headerFillPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#1B5E20") // Dark Green
        }
        val rowShadePaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#F5F5F5") // Light Gray Shading
        }
        
        val horizontalPadding = 40f
        var y = 50f

        // 1. HEADER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawText(profile.companyName ?: "MANDI LEDGER", horizontalPadding, y, paint)
        y += 25f
        
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 14f
        paint.color = Color.BLACK
        canvas.drawText(reportTitle, horizontalPadding, y, paint)
        y += 18f
        
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated On: $timestamp", horizontalPadding, y, paint)
        y += 15f
        canvas.drawText("Filter Range: $range", horizontalPadding, y, paint)
        y += 20f
        
        canvas.drawLine(horizontalPadding, y, PAGE_WIDTH - horizontalPadding, y, borderPaint)
        y += 30f

        // 2. SUMMARY BOX
        if (summary.isNotEmpty()) {
            val boxWidth = 300f
            val boxHeight = (summary.size * 20f) + 20f
            val rect = RectF(horizontalPadding, y, horizontalPadding + boxWidth, y + boxHeight)
            canvas.drawRect(rect, borderPaint)
            
            var summaryY = y + 25f
            summary.forEach { (label, value) ->
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.BLACK
                canvas.drawText(label, horizontalPadding + 15f, summaryY, paint)
                
                paint.typeface = Typeface.DEFAULT
                canvas.drawText(value, horizontalPadding + 150f, summaryY, paint)
                summaryY += 20f
            }
            y += boxHeight + 40f
        }

        if (tableData.isEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.color = Color.RED
            canvas.drawText("No records found for the selected range.", horizontalPadding, y, paint)
            pdf.finishPage(page)
            return
        }

        // 3. TABLE
        val colWidths = mutableListOf<Float>()
        val totalTableWidth = PAGE_WIDTH - (horizontalPadding * 2)
        
        // Dynamic column width distribution
        headers.forEachIndexed { index, _ ->
            // Use 1.0 weight as default, but Name columns get more
            val weight = if (headers[index].contains("Name", true) || headers[index].contains("Product", true) || headers[index].contains("Buyer", true) || headers[index].contains("Farmer", true)) 2.0f else 1.0f
            colWidths.add(weight)
        }
        val totalWeight = colWidths.sum()
        for (i in colWidths.indices) {
            colWidths[i] = (colWidths[i] / totalWeight) * totalTableWidth
        }

        // 3.1 TABLE HEADER ROW
        val headerHeight = 25f
        canvas.drawRect(horizontalPadding, y - 18f, PAGE_WIDTH - horizontalPadding, y + 7f, headerFillPaint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.color = Color.WHITE
        
        var currentX = horizontalPadding
        headers.forEachIndexed { index, header ->
            val align = alignments.getOrElse(index) { Paint.Align.LEFT }
            drawTextInCell(canvas, header, currentX, y, colWidths[index], paint, align)
            currentX += colWidths[index]
        }
        
        // Draw header borders
        currentX = horizontalPadding
        for (width in colWidths) {
            canvas.drawRect(currentX, y - 18f, currentX + width, y + 7f, borderPaint)
            currentX += width
        }
        
        y += headerHeight

        // 3.2 TABLE DATA ROWS
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.BLACK
        paint.textSize = 8f
        
        tableData.forEachIndexed { rowIndex, row ->
            // Calculate height needed for this row (support wrapping)
            var maxLines = 1
            row.forEachIndexed { colIndex, text ->
                val lines = paint.breakTextProfessional(text ?: "", colWidths[colIndex] - 10f)
                maxLines = maxOf(maxLines, lines)
            }
            val rowHeight = (maxLines * 12f) + 10f
            
            // Page overflow check
            if (y + rowHeight > PAGE_HEIGHT - 60f) {
                drawFooter(canvas, pageNumber)
                pdf.finishPage(page)
                
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                
                // Redraw Continued Header
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#1B5E20")
                canvas.drawText("$reportTitle (Continued)", horizontalPadding, y, paint)
                y += 20f
                
                // Redraw table header on new page
                canvas.drawRect(horizontalPadding, y - 18f, PAGE_WIDTH - horizontalPadding, y + 7f, headerFillPaint)
                paint.color = Color.WHITE
                currentX = horizontalPadding
                headers.forEachIndexed { index, header ->
                    val align = alignments.getOrElse(index) { Paint.Align.LEFT }
                    drawTextInCell(canvas, header, currentX, y, colWidths[index], paint, align)
                    currentX += colWidths[index]
                }
                
                // Cell borders for header
                currentX = horizontalPadding
                for (width in colWidths) {
                    canvas.drawRect(currentX, y - 18f, currentX + width, y + 7f, borderPaint)
                    currentX += width
                }
                
                y += headerHeight
                paint.typeface = Typeface.DEFAULT
                paint.color = Color.BLACK
            }

            // Alternate row shading
            if (rowIndex % 2 != 0) {
                canvas.drawRect(horizontalPadding, y - 18f, PAGE_WIDTH - horizontalPadding, y + rowHeight - 18f, rowShadePaint)
            }
            
            currentX = horizontalPadding
            row.forEachIndexed { colIndex, text ->
                val align = alignments.getOrElse(colIndex) { Paint.Align.LEFT }
                drawWrappedText(canvas, text ?: "", currentX + 5f, y, colWidths[colIndex] - 10f, paint, align)
                
                // Cell borders
                canvas.drawRect(currentX, y - 18f, currentX + colWidths[colIndex], y + rowHeight - 18f, borderPaint)
                currentX += colWidths[colIndex]
            }
            
            y += rowHeight
        }
        
        drawFooter(canvas, pageNumber)
        pdf.finishPage(page)
    }

    private fun drawTextInCell(canvas: Canvas, text: String, x: Float, y: Float, width: Float, paint: Paint, align: Paint.Align) {
        paint.textAlign = align
        val drawX = when (align) {
            Paint.Align.LEFT -> x + 5f
            Paint.Align.CENTER -> x + (width / 2f)
            Paint.Align.RIGHT -> x + width - 5f
        }
        canvas.drawText(text, drawX, y, paint)
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, width: Float, paint: Paint, align: Paint.Align): Int {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(testLine) <= width) {
                currentLine.append(if (currentLine.isEmpty()) "" else " ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        
        var currentY = y
        lines.forEach { line ->
            paint.textAlign = align
            val drawX = when (align) {
                Paint.Align.LEFT -> x
                Paint.Align.CENTER -> x + (width / 2f)
                Paint.Align.RIGHT -> x + width
            }
            canvas.drawText(line, drawX, currentY, paint)
            currentY += 12f
        }
        return lines.size
    }

    private fun Paint.breakTextProfessional(text: String, maxWidth: Float): Int {
        val words = text.split(" ")
        if (words.isEmpty()) return 1
        var lines = 0
        var currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) "" else " ").append(word)
            } else {
                lines++
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines++
        return maxOf(1, lines)
    }

    private fun drawFooter(canvas: Canvas, page: Int) {
        val paint = Paint().apply {
            textSize = 9f
            color = Color.DKGRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val y = PAGE_HEIGHT - 30f
        canvas.drawLine(40f, y - 15f, PAGE_WIDTH - 40f, y - 15f, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
        canvas.drawText("Generated by Mandi Ledger", 40f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $page", PAGE_WIDTH - 40f, y, paint)
    }

    private fun formatDateProfessional(t: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(t))

    private fun formatDate(t: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t))

    fun generateDashboardSummary(context: Context, profile: CompanyProfileEntity, summary: DashboardSummary, stock: List<StockReportModel>): File? {
        val pdf = PdfDocument()
        val range = "Current Overview"
        val headers = listOf("Item", "Unit", "Stock")
        val alignments = listOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT)
        val tableData = stock.map { listOf(it.productName, it.unit, Formatter.formatWeight(it.totalQuantity)) }
        val summ = mapOf(
            "TODAY SALES" to "₹${Formatter.formatCurrencyStrict(summary.todaySales)}",
            "COMM EARNED" to "₹${Formatter.formatCurrencyStrict(summary.todayCommission)}"
        )
        
        drawProfessionalReport(pdf, profile, "DASHBOARD SUMMARY", range, headers, tableData, summ, alignments)
        return savePdf(pdf, context, "Reports", "DashboardSummary")
    }

    fun savePdf(pdf: PdfDocument, context: Context, dir: String, fileName: String): File? {
        val root = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MandiLedger/$dir")
        if (!root.exists()) root.mkdirs()
        val file = File(root, "${fileName}_${System.currentTimeMillis()}.pdf")
        return try { 
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()
            Timber.d("PDF_FILE_CREATED: ${file.absolutePath}")
            file 
        } catch (e: Exception) { 
            pdf.close()
            Timber.e(e, "PDF_GENERATION_FAILED")
            null 
        }
    }

    fun generateBackupPDF(c: Context, f: List<FarmerEntity>, b: List<BuyerEntity>, s: List<SaleEntity>, a: List<ArrivalEntity>, pr: List<ProductEntity>, ex: List<ExpenseEntity>, pa: List<PaymentEntity>, t: String): File? {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { textSize = 12f; color = Color.BLACK }
        var y = 50f
        canvas.drawText("MANDI LEDGER - DATA BACKUP ($t)", 50f, y, paint.apply { typeface = Typeface.DEFAULT_BOLD }); y += 30f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Summary: Farmers: ${f.size}, Buyers: ${b.size}, Sales: ${s.size}, Arrivals: ${a.size}", 50f, y, paint)
        pdf.finishPage(page)
        return savePdf(pdf, c, "Backups", "Backup_$t")
    }

}
