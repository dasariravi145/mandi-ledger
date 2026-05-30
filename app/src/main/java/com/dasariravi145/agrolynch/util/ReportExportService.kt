package com.dasariravi145.agrolynch.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.dasariravi145.agrolynch.data.local.dao.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExportService @Inject constructor() {

    fun exportToCsv(context: Context, reportName: String, data: List<Any>): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${reportName}_$timestamp.csv"
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgroLynch/Reports")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            
            FileOutputStream(file).use { out ->
                if (data.isNotEmpty()) {
                    val headers = getHeaders(data.first()).joinToString(",") + "\n"
                    out.write(headers.toByteArray())
                    
                    data.forEach { item ->
                        val line = getRowData(item).joinToString(",") + "\n"
                        out.write(line.toByteArray())
                    }
                }
            }
            file
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    fun exportToExcel(context: Context, reportName: String, data: List<Any>): File? {
        return exportToCsv(context, reportName, data)?.let { csvFile ->
            val excelFile = File(csvFile.parent, csvFile.name.replace(".csv", ".xls"))
            csvFile.renameTo(excelFile)
            excelFile
        }
    }

    fun exportToPdf(context: Context, reportName: String, data: List<Any>): File? {
        return try {
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val titlePaint = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val subTitlePaint = Paint().apply {
                textSize = 12f
                color = Color.DKGRAY
            }
            val headerPaint = Paint().apply {
                textSize = 9f
                isFakeBoldText = true
                color = Color.WHITE
            }
            val bodyPaint = Paint().apply {
                textSize = 8f
                color = Color.BLACK
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
            }

            var pageNumber = 1
            var myPageInfo = PdfDocument.PageInfo.Builder(842, 595, pageNumber).create()
            var myPage = pdfDocument.startPage(myPageInfo)
            var canvas = myPage.canvas

            var y = 40f
            canvas.drawText("AgroLynch - $reportName", 40f, y, titlePaint)
            y += 25f
            canvas.drawText("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, y, subTitlePaint)
            y += 40f

            if (data.isNotEmpty()) {
                val headers = getHeaders(data.first())
                val colWidths = calculateColumnWidths(data.first())
                
                // Draw Table Header Background
                paint.color = Color.rgb(27, 94, 32)
                canvas.drawRect(40f, y, 802f, y + 20f, paint)
                
                var x = 45f
                headers.forEachIndexed { index, h ->
                    canvas.drawText(h, x, y + 14f, headerPaint)
                    x += colWidths.getOrElse(index) { 60f }
                }
                y += 25f

                data.forEachIndexed { index, item ->
                    if (y > 540) {
                        pdfDocument.finishPage(myPage)
                        pageNumber++
                        myPageInfo = PdfDocument.PageInfo.Builder(842, 595, pageNumber).create()
                        myPage = pdfDocument.startPage(myPageInfo)
                        canvas = myPage.canvas
                        y = 50f
                        
                        paint.color = Color.rgb(27, 94, 32)
                        canvas.drawRect(40f, y, 802f, y + 20f, paint)
                        var hx = 45f
                        headers.forEachIndexed { hIdx, h ->
                            canvas.drawText(h, hx, y + 14f, headerPaint)
                            hx += colWidths.getOrElse(hIdx) { 60f }
                        }
                        y += 25f
                    }

                    val values = getRowData(item)
                    
                    var itemX = 45f
                    values.forEachIndexed { vIdx, v ->
                        val text = v.take(20)
                        canvas.drawText(text, itemX, y, bodyPaint)
                        itemX += colWidths.getOrElse(vIdx) { 60f }
                    }
                    canvas.drawLine(40f, y + 5f, 802f, y + 5f, linePaint)
                    y += 20f
                }
            }

            pdfDocument.finishPage(myPage)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${reportName}_$timestamp.pdf"
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgroLynch/Reports")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            Timber.d("Report PDF Export Success: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Report PDF Export Failed")
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    private fun calculateColumnWidths(item: Any): List<Float> = when (item) {
        is DetailedSaleReportModel -> listOf(55f, 100f, 40f, 90f, 70f, 50f, 40f, 55f, 45f, 45f, 55f, 55f, 55f)
        is DetailedArrivalReportModel -> listOf(55f, 100f, 40f, 90f, 70f, 50f, 40f, 55f, 55f, 55f, 55f, 55f)
        is CommissionReportModel -> listOf(55f, 90f, 90f, 80f, 60f, 50f, 60f, 45f, 55f, 55f, 60f)
        is ProductPerformanceModel -> listOf(100f, 80f, 80f, 70f, 70f, 70f, 100f)
        is OutstandingAgingModel -> listOf(150f, 60f, 100f, 100f, 80f)
        is PaymentReportModel -> listOf(60f, 80f, 150f, 80f, 80f, 80f)
        else -> listOf(100f)
    }

    private fun getHeaders(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> listOf("Date", "Buyer Name", "Bill", "Product", "Grade", "Qty", "Rate", "Gross", "Labor", "Trans", "Total", "Paid", "Pending")
        is DetailedArrivalReportModel -> listOf("Date", "Farmer Name", "Bill", "Product", "Grade", "Qty", "Rate", "Gross", "Comm", "Net", "Paid", "Pending")
        is CommissionReportModel -> listOf("Date", "Buyer", "Farmer", "Product", "Grade", "Qty", "SaleAmt", "Comm%", "CommAmt", "Margin", "Total")
        is ProductPerformanceModel -> listOf("Product", "Category", "Grade", "Arrivals", "Sold", "Stock", "Profit")
        is OutstandingAgingModel -> listOf("Entity Name", "Type", "Pending Amt", "Last Pmt", "Days")
        is PaymentReportModel -> listOf("Date", "ID", "Party Name", "Type", "Amount", "Mode")
        else -> listOf("Data")
    }

    private fun getRowData(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> {
            Timber.d("Mapped Buyer Data: ${item.buyerName}, ID: ${item.id}")
            listOf(
                formatDate(item.date),
                item.buyerName,
                item.id.takeLast(4),
                item.productName,
                item.grade,
                "${item.quantity}${item.unit}",
                String.format(Locale.US, "%.1f", item.rate),
                String.format(Locale.US, "%.0f", item.saleAmount),
                String.format(Locale.US, "%.0f", item.laborCharges),
                String.format(Locale.US, "%.0f", item.transportCharges),
                String.format(Locale.US, "%.0f", item.totalAmount),
                String.format(Locale.US, "%.0f", item.paidAmount),
                String.format(Locale.US, "%.0f", item.pendingAmount)
            )
        }
        is DetailedArrivalReportModel -> {
            Timber.d("Mapped Farmer Data: ${item.farmerName}, ID: ${item.id}")
            listOf(
                formatDate(item.date),
                item.farmerName,
                item.id.takeLast(4),
                item.productName,
                item.grade,
                "${item.quantity}${item.unit}",
                String.format(Locale.US, "%.1f", item.rate),
                String.format(Locale.US, "%.0f", item.grossAmount),
                String.format(Locale.US, "%.0f", item.commissionAmount),
                String.format(Locale.US, "%.0f", item.netAmount),
                "0.0", // Paid
                String.format(Locale.US, "%.0f", item.pendingAmount)
            )
        }
        is CommissionReportModel -> listOf(
            formatDate(item.date),
            item.buyerName,
            item.farmerName,
            item.productName,
            item.grade,
            item.quantity.toString(),
            String.format(Locale.US, "%.0f", item.saleAmount),
            "${item.commissionPercent}%",
            String.format(Locale.US, "%.0f", item.commissionAmount),
            String.format(Locale.US, "%.0f", item.marginAmount),
            String.format(Locale.US, "%.0f", item.commissionAmount + item.marginAmount)
        )
        is ProductPerformanceModel -> listOf(
            item.productName,
            item.category,
            item.grade,
            item.totalArrivals.toString(),
            item.totalSold.toString(),
            item.currentStock.toString(),
            String.format(Locale.US, "%.0f", item.totalProfit)
        )
        is OutstandingAgingModel -> listOf(
            item.name,
            item.type,
            String.format(Locale.US, "%.0f", item.pendingAmount),
            item.lastPaymentDate?.let { formatDate(it) } ?: "N/A",
            item.daysPending.toString()
        )
        is PaymentReportModel -> listOf(
            formatDate(item.date),
            item.id.takeLast(4),
            item.partyName,
            item.partyType,
            String.format(Locale.US, "%.0f", item.amount),
            item.paymentMode
        )
        else -> listOf(item.toString())
    }

    private fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))
}
