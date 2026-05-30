package com.dasariravi145.agrolynch.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.dasariravi145.agrolynch.domain.model.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerExportService @Inject constructor() {

    fun exportLedgerToPdf(context: Context, summary: LedgerSummary, partyType: String): File? {
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
            var pageInfo = PdfDocument.PageInfo.Builder(842, 595, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Header
            var y = 40f
            canvas.drawText("MANDI LEDGER - ${if (partyType == "FARMER") "FARMER" else "BUYER"} ACCOUNT", 40f, y, titlePaint)
            y += 20f
            canvas.drawText("Party Name: ${summary.partyName}", 40f, y, subTitlePaint)
            y += 15f
            canvas.drawText("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, y, subTitlePaint)
            
            // Summary Box
            y += 30f
            paint.color = Color.rgb(240, 240, 240)
            canvas.drawRect(40f, y, 802f, y + 50f, paint)
            paint.color = Color.BLACK
            
            val summaryY = y + 20f
            canvas.drawText("Total Transactions: ${summary.totalTransactions}", 55f, summaryY, bodyPaint)
            canvas.drawText("Total Amount: ₹${String.format(Locale.US, "%.2f", summary.totalDebit)}", 200f, summaryY, bodyPaint)
            canvas.drawText("Total Paid: ₹${String.format(Locale.US, "%.2f", summary.totalCredit)}", 350f, summaryY, bodyPaint)
            
            val balancePaint = Paint(bodyPaint).apply { isFakeBoldText = true; textSize = 11f }
            canvas.drawText("Pending Balance: ₹${String.format(Locale.US, "%.2f", summary.balance)}", 55f, summaryY + 20f, balancePaint)
            
            y += 70f
            
            // Table Headers
            paint.color = Color.rgb(27, 94, 32)
            canvas.drawRect(40f, y, 802f, y + 20f, paint)
            
            val headers = listOf("Date", "Bill#", "Product", "Cat", "Grade", "Qty", "Rate", "Gross", "Comm", "Labor", "Trans", "Net", "Paid", "Balance")
            val colWidths = listOf(50f, 40f, 90f, 40f, 60f, 50f, 40f, 55f, 45f, 45f, 45f, 55f, 55f, 60f)

            var currentX = 45f
            headers.forEachIndexed { index, title ->
                canvas.drawText(title, currentX, y + 14f, headerPaint)
                currentX += colWidths[index]
            }
            
            y += 35f
            
            summary.entries.asReversed().forEachIndexed { index, entry ->
                if (y > 540) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(842, 595, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                    
                    paint.color = Color.rgb(27, 94, 32)
                    canvas.drawRect(40f, y, 802f, y + 20f, paint)
                    var hx = 45f
                    headers.forEachIndexed { hIdx, h ->
                        canvas.drawText(h, hx, y + 14f, headerPaint)
                        hx += colWidths[hIdx]
                    }
                    y += 35f
                }
                
                val details = entry.details ?: LedgerEntryDetails()
                val values = listOf(
                    dateFormat.format(Date(entry.date)),
                    details.billNumber,
                    details.productName,
                    details.category.take(3),
                    details.grade,
                    "${details.quantity}${details.unit}",
                    String.format(Locale.US, "%.1f", details.rate),
                    String.format(Locale.US, "%.0f", details.grossAmount),
                    if (details.commissionAmount > 0) String.format(Locale.US, "%.0f", details.commissionAmount) else "0",
                    if (details.laborCharges > 0) String.format(Locale.US, "%.0f", details.laborCharges) else "0",
                    if (details.transportCharges > 0) String.format(Locale.US, "%.0f", details.transportCharges) else "0",
                    String.format(Locale.US, "%.0f", details.netAmount),
                    if (details.paymentMade > 0) String.format(Locale.US, "%.0f", details.paymentMade) else "0",
                    String.format(Locale.US, "%.0f", entry.balance)
                )

                Timber.d("PDF Row Generated: $index, Data: ${values.joinToString("|")}")

                var itemX = 45f
                values.forEachIndexed { vIdx, v ->
                    canvas.drawText(v.take(15), itemX, y, bodyPaint)
                    itemX += colWidths[vIdx]
                }
                
                canvas.drawLine(40f, y + 10f, 802f, y + 10f, linePaint)
                y += 25f
            }
            
            pdfDocument.finishPage(page)
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${if (partyType == "FARMER") "FarmerLedger" else "BuyerLedger"}_${summary.partyName.replace(" ", "_")}_$timeStamp.pdf"
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgroLynch/Ledger")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            Timber.d("Ledger PDF Export Success: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Ledger PDF Export Failed")
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }
}
