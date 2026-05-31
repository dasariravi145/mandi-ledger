package com.dasariravi145.agrolynch.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.dasariravi145.agrolynch.data.local.dao.*
import kotlinx.coroutines.tasks.await
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
                    val headerStr = getHeaders(data.first()).joinToString(",") + "\n"
                    out.write(headerStr.toByteArray())
                    
                    data.forEach { item ->
                        val line = getRowDataForCsv(item).joinToString(",") + "\n"
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
            val titlePaint = Paint().apply {
                textSize = 22f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val subTitlePaint = Paint().apply {
                textSize = 14f
                isFakeBoldText = true
                color = Color.DKGRAY
            }
            val labelPaint = Paint().apply {
                textSize = 12f
                color = Color.GRAY
            }
            val valuePaint = Paint().apply {
                textSize = 12f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val summaryPaint = Paint().apply {
                textSize = 13f
                isFakeBoldText = true
                color = Color.rgb(27, 94, 32) // Professional Green
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var pageNumber = 1
            var myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var myPage = pdfDocument.startPage(myPageInfo)
            var canvas = myPage.canvas

            var y = 60f
            canvas.drawText("MANDI LEDGER REPORT", 40f, y, titlePaint)
            y += 35f
            canvas.drawText(reportName.uppercase(), 40f, y, subTitlePaint)
            y += 20f
            canvas.drawText("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, y, labelPaint)
            y += 45f

            // Summary Section
            if (data.isNotEmpty()) {
                canvas.drawText("SUMMARY TOTALS", 40f, y, subTitlePaint)
                y += 30f
                
                var totalGross = 0.0
                var totalPaid = 0.0
                var totalPending = 0.0
                
                data.forEach { item ->
                    when (item) {
                        is DetailedSaleReportModel -> {
                            totalGross += item.totalAmount
                            totalPaid += item.paidAmount
                            totalPending += item.pendingAmount
                        }
                        is DetailedArrivalReportModel -> {
                            totalGross += item.grossAmount
                            totalPending += item.pendingAmount
                        }
                    }
                }

                canvas.drawText("Total Transactions: ${data.size}", 50f, y, summaryPaint)
                y += 22f
                canvas.drawText("Total Gross Amount: ₹${String.format(Locale.US, "%,.2f", totalGross)}", 50f, y, summaryPaint)
                y += 22f
                canvas.drawText("Total Paid Amount: ₹${String.format(Locale.US, "%,.2f", totalPaid)}", 50f, y, summaryPaint)
                y += 22f
                canvas.drawText("Total Pending Amount: ₹${String.format(Locale.US, "%,.2f", totalPending)}", 50f, y, summaryPaint)
                y += 40f
            }

            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 40f

            data.forEachIndexed { index, item ->
                // Check pagination - Each block takes approx 240 pixels
                if (y > 600) {
                    pdfDocument.finishPage(myPage)
                    pageNumber++
                    myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    myPage = pdfDocument.startPage(myPageInfo)
                    canvas = myPage.canvas
                    y = 60f
                }

                Timber.d("PDF Row Generated: Report Index $index")

                when (item) {
                    is DetailedSaleReportModel -> y = renderSaleBlock(canvas, item, y, labelPaint, valuePaint)
                    is DetailedArrivalReportModel -> y = renderArrivalBlock(canvas, item, y, labelPaint, valuePaint)
                    is CommissionReportModel -> y = renderCommissionBlock(canvas, item, y, labelPaint, valuePaint)
                    else -> {
                        canvas.drawText("Other Entry: ${item.toString().take(50)}", 40f, y, valuePaint)
                        y += 30f
                    }
                }

                y += 20f
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 40f
            }

            pdfDocument.finishPage(myPage)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${reportName.replace(" ", "_")}_$timestamp.pdf"
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

    private fun renderSaleBlock(canvas: android.graphics.Canvas, item: DetailedSaleReportModel, y: Float, labelPaint: Paint, valuePaint: Paint): Float {
        var cy = y
        // Block Header
        canvas.drawText("Date: ${formatDate(item.date)}", 40f, cy, valuePaint)
        canvas.drawText("Bill No: ${item.id.takeLast(6).uppercase()}", 350f, cy, valuePaint)
        cy += 30f

        canvas.drawText("Party: ", 40f, cy, labelPaint)
        canvas.drawText(item.buyerName, 120f, cy, valuePaint)
        cy += 25f

        canvas.drawText("Product: ", 40f, cy, labelPaint)
        canvas.drawText(item.productName, 120f, cy, valuePaint)
        canvas.drawText("Category: ", 300f, cy, labelPaint)
        canvas.drawText("General", 380f, cy, valuePaint) // Placeholder since not in model
        canvas.drawText("Grade: ", 460f, cy, labelPaint)
        canvas.drawText(item.grade, 510f, cy, valuePaint)
        cy += 25f

        canvas.drawText("Quantity: ", 40f, cy, labelPaint)
        canvas.drawText("${item.quantity} ${item.unit}", 120f, cy, valuePaint)
        canvas.drawText("Rate: ", 300f, cy, labelPaint)
        canvas.drawText("₹${String.format(Locale.US, "%.2f", item.rate)}", 380f, cy, valuePaint)
        cy += 40f

        // Financial Grid
        cy = drawFinRow(canvas, "Gross Amount", item.saleAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Labor Charges", item.laborCharges, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Transport Charges", item.transportCharges, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Net Amount", item.totalAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Paid Amount", item.paidAmount, cy, labelPaint, valuePaint)
        
        val pendingPaint = Paint(valuePaint).apply { color = if (item.pendingAmount > 0) Color.RED else Color.BLACK }
        cy = drawFinRow(canvas, "Pending Amount", item.pendingAmount, cy, labelPaint, pendingPaint)
        
        return cy
    }

    private fun renderArrivalBlock(canvas: android.graphics.Canvas, item: DetailedArrivalReportModel, y: Float, labelPaint: Paint, valuePaint: Paint): Float {
        var cy = y
        canvas.drawText("Date: ${formatDate(item.date)}", 40f, cy, valuePaint)
        canvas.drawText("Bill No: ${item.id.takeLast(6).uppercase()}", 350f, cy, valuePaint)
        cy += 30f

        canvas.drawText("Party: ", 40f, cy, labelPaint)
        canvas.drawText(item.farmerName, 120f, cy, valuePaint)
        cy += 25f

        canvas.drawText("Product: ", 40f, cy, labelPaint)
        canvas.drawText(item.productName, 120f, cy, valuePaint)
        canvas.drawText("Grade: ", 300f, cy, labelPaint)
        canvas.drawText(item.grade, 380f, cy, valuePaint)
        cy += 25f

        canvas.drawText("Quantity: ", 40f, cy, labelPaint)
        canvas.drawText("${item.quantity} ${item.unit}", 120f, cy, valuePaint)
        canvas.drawText("Rate: ", 300f, cy, labelPaint)
        canvas.drawText("₹${String.format(Locale.US, "%.2f", item.rate)}", 380f, cy, valuePaint)
        cy += 40f

        cy = drawFinRow(canvas, "Gross Amount", item.grossAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Commission Amount", item.commissionAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Net Payable", item.netAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Deduction Amount", item.grossAmount - item.netAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Pending Amount", item.pendingAmount, cy, labelPaint, valuePaint)
        
        return cy
    }

    private fun renderCommissionBlock(canvas: android.graphics.Canvas, item: CommissionReportModel, y: Float, labelPaint: Paint, valuePaint: Paint): Float {
        var cy = y
        canvas.drawText("Date: ${formatDate(item.date)}", 40f, cy, valuePaint)
        cy += 30f
        canvas.drawText("Buyer: ${item.buyerName}", 40f, cy, valuePaint)
        canvas.drawText("Farmer: ${item.farmerName}", 300f, cy, valuePaint)
        cy += 25f
        canvas.drawText("Product: ${item.productName} (${item.grade})", 40f, cy, valuePaint)
        cy += 40f
        
        cy = drawFinRow(canvas, "Sale Amount", item.saleAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Commission Earned", item.commissionAmount, cy, labelPaint, valuePaint)
        cy = drawFinRow(canvas, "Margin (Profit)", item.marginAmount, cy, labelPaint, valuePaint)
        
        val totalEarned = item.commissionAmount + item.marginAmount
        cy = drawFinRow(canvas, "Total Payout", totalEarned, cy, labelPaint, valuePaint)
        
        return cy
    }

    private fun drawFinRow(canvas: android.graphics.Canvas, label: String, value: Double, y: Float, lPaint: Paint, vPaint: Paint): Float {
        canvas.drawText("$label : ", 60f, y, lPaint)
        canvas.drawText("₹${String.format(Locale.US, "%,.2f", value)}", 400f, y, vPaint)
        return y + 22f
    }

    private fun getHeaders(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> listOf("Date", "Buyer", "Bill", "Product", "Grade", "Qty", "Rate", "Gross", "Labor", "Trans", "Total", "Paid", "Pending")
        is DetailedArrivalReportModel -> listOf("Date", "Farmer", "Bill", "Product", "Grade", "Qty", "Rate", "Gross", "Comm", "Labor", "Net", "Paid", "Pending")
        is CommissionReportModel -> listOf("Date", "Buyer", "Farmer", "Product", "Grade", "Qty", "SaleAmt", "Comm%", "CommAmt", "Margin", "Total")
        is ProductPerformanceModel -> listOf("Product", "Category", "Grade", "Arrivals", "Sold", "Stock", "Profit")
        is OutstandingAgingModel -> listOf("Entity Name", "Type", "Pending Amt", "Last Pmt", "Days")
        is PaymentReportModel -> listOf("Date", "ID", "Party Name", "Type", "Amount", "Mode")
        else -> listOf("Data")
    }

    private fun getRowDataForCsv(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> listOf(formatDate(item.date), item.buyerName, item.id.takeLast(4), item.productName, item.grade, "${item.quantity}${item.unit}", item.rate.toString(), item.saleAmount.toString(), item.laborCharges.toString(), item.transportCharges.toString(), item.totalAmount.toString(), item.paidAmount.toString(), item.pendingAmount.toString())
        is DetailedArrivalReportModel -> listOf(formatDate(item.date), item.farmerName, item.id.takeLast(4), item.productName, item.grade, "${item.quantity}${item.unit}", item.rate.toString(), item.grossAmount.toString(), item.commissionAmount.toString(), "0.0", item.netAmount.toString(), "0.0", item.pendingAmount.toString())
        is CommissionReportModel -> listOf(formatDate(item.date), item.buyerName, item.farmerName, item.productName, item.grade, item.quantity.toString(), item.saleAmount.toString(), item.commissionPercent.toString(), item.commissionAmount.toString(), item.marginAmount.toString(), (item.commissionAmount + item.marginAmount).toString())
        else -> listOf(item.toString())
    }

    private fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))

    suspend fun uploadToCloud(file: File, remotePath: String): Resource<String> {
        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        
        Timber.d("Uploading file to Cloud: Path=$remotePath, File=${file.absolutePath}, User=$uid")

        if (!file.exists()) {
            Timber.e("Upload failed: Local file does not exist")
            return Resource.Error("Local file not found")
        }
        if (file.length() == 0L) {
            Timber.e("Upload failed: Local file is empty")
            return Resource.Error("File is empty")
        }

        return try {
            val ref = storage.reference.child(remotePath)
            
            Timber.d("Starting Firebase Storage putFile to: $remotePath")
            val uploadSnapshot = ref.putFile(android.net.Uri.fromFile(file)).await()
            
            Timber.d("Upload Success. Bytes transferred: ${uploadSnapshot.bytesTransferred}")
            
            // Wait a moment for eventual consistency and retry download URL
            var downloadUrl: String? = null
            var lastError: Exception? = null
            
            for (i in 1..3) {
                try {
                    val uri = uploadSnapshot.metadata?.reference?.downloadUrl?.await()
                    downloadUrl = uri?.toString()
                    if (downloadUrl != null) break
                } catch (e: Exception) {
                    lastError = e
                    Timber.w("Download URL attempt $i failed: ${e.message}. Retrying in 1s...")
                    kotlinx.coroutines.delay(1000L * i)
                }
            }
            
            if (downloadUrl != null) {
                Timber.d("Generated Download URL: $downloadUrl")
                Resource.Success(downloadUrl)
            } else {
                throw lastError ?: Exception("Could not generate download URL after multiple attempts")
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            Timber.e(e, "Upload failed at location: $remotePath. Error: $msg")
            
            if (msg.contains("Object does not exist", ignoreCase = true)) {
                Timber.e("CRITICAL: Firebase reports object missing immediately after upload. This may be a rule or path issue.")
            }

            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                recordException(e)
                log("Storage Path Failure: $remotePath")
            }
            Resource.Error("Upload failed: $msg")
        }
    }
}
