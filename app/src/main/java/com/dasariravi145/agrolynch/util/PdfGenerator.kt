package com.dasariravi145.agrolynch.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generateBackupPDF(
        context: Context,
        farmers: List<FarmerEntity>,
        buyers: List<BuyerEntity>,
        sales: List<SaleEntity>,
        arrivals: List<ArrivalEntity>,
        products: List<ProductEntity>,
        expenses: List<ExpenseEntity>,
        payments: List<PaymentEntity>,
        reportType: String
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        var pageNumber = 1
        var yPosition = 40f

        // Helper to start a new page
        fun startNewPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            val page = pdfDocument.startPage(pageInfo)
            yPosition = 40f
            return page
        }

        var currentPage = startNewPage()
        var canvas = currentPage.canvas

        // Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 18f
        canvas.drawText("AgroLynch - $reportType Backup Report", 40f, yPosition, titlePaint)
        yPosition += 30f
        
        paint.textSize = 12f
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, yPosition, paint)
        yPosition += 40f

        // Sections
        fun drawSectionHeader(title: String) {
            if (yPosition > 750f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            titlePaint.textSize = 14f
            canvas.drawText(title, 40f, yPosition, titlePaint)
            yPosition += 20f
            canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
            yPosition += 25f
        }

        // 1. Farmers
        drawSectionHeader("Farmers Data")
        farmers.forEach { farmer ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            canvas.drawText("${farmer.name} | ${farmer.mobileNumber} | ${farmer.village}", 40f, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 20f

        // 2. Buyers
        drawSectionHeader("Buyers/Traders Data")
        buyers.forEach { buyer ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            canvas.drawText("${buyer.name} | ${buyer.mobileNumber} | ${buyer.address}", 40f, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 20f

        // 3. Arrivals
        drawSectionHeader("Stock Arrivals")
        arrivals.take(50).forEach { arrival ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            canvas.drawText("${arrival.productName} | ${arrival.farmerName} | ${arrival.quantity} ${arrival.unit} @ ${arrival.purchaseRate}", 40f, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 20f

        // 4. Sales
        drawSectionHeader("Sales Records")
        sales.take(50).forEach { sale ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            canvas.drawText("${sale.productName} | Buyer: ${sale.buyerName} | Amt: ₹${sale.totalAmount}", 40f, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 20f

        // 5. Expenses
        drawSectionHeader("Expenses")
        expenses.take(50).forEach { expense ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            canvas.drawText("${expense.type} | ${expense.description} | ₹${expense.amount}", 40f, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 20f

        // 6. Payments
        drawSectionHeader("Recent Payments")
        payments.take(50).forEach { payment ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
            }
            canvas.drawText("${payment.partyName} | ${payment.partyType} | ${payment.paymentMode} | ₹${payment.amount}", 40f, yPosition, paint)
            yPosition += 20f
        }

        // Finish the last page
        pdfDocument.finishPage(currentPage)

        // Save to local storage
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgroLynch/Backups")
        if (!directory.exists()) directory.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AgroLynch_Backup_${reportType}_$timeStamp.pdf"
        val file = File(directory, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun generateReceiptPdf(context: Context, data: ReceiptData): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        var yPosition = 50f

        // Company/Agent Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 20f
        titlePaint.color = Color.BLACK
        canvas.drawText(data.agentName, 40f, yPosition, titlePaint)
        yPosition += 25f
        
        paint.textSize = 12f
        canvas.drawText("Contact: ${data.agentContact}", 40f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(data.date))}", 40f, yPosition, paint)
        yPosition += 30f
        
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 30f

        // Transaction Details
        titlePaint.textSize = 16f
        canvas.drawText("RECEIPT: ${data.transactionType}", 40f, yPosition, titlePaint)
        yPosition += 25f
        
        paint.textSize = 14f
        canvas.drawText("To: ${data.partyName} (${data.partyType})", 40f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("Receipt ID: ${data.receiptId}", 40f, yPosition, paint)
        yPosition += 40f

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Description", 40f, yPosition, paint)
        canvas.drawText("Qty", 300f, yPosition, paint)
        canvas.drawText("Rate", 400f, yPosition, paint)
        canvas.drawText("Amount", 500f, yPosition, paint)
        yPosition += 10f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 25f

        // Table Content
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        data.items.forEach { item ->
            canvas.drawText(item.description, 40f, yPosition, paint)
            canvas.drawText(item.quantity, 300f, yPosition, paint)
            canvas.drawText(item.rate, 400f, yPosition, paint)
            canvas.drawText(item.amount, 500f, yPosition, paint)
            yPosition += 20f
        }

        yPosition += 20f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 30f

        // Total
        titlePaint.textSize = 16f
        canvas.drawText("Total Amount: ₹${data.totalAmount}", 400f, yPosition, titlePaint)
        yPosition += 40f

        if (data.notes.isNotEmpty()) {
            paint.textSize = 12f
            canvas.drawText("Notes: ${data.notes}", 40f, yPosition, paint)
        }

        // Footer
        yPosition = 800f
        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("Computer generated receipt. No signature required.", 40f, yPosition, paint)

        pdfDocument.finishPage(page)

        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AgroLynch/Receipts")
        if (!directory.exists()) directory.mkdirs()

        val fileName = "Receipt_${data.receiptId}.pdf"
        val file = File(directory, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Receipt"))
    }
}
