package com.dasariravi145.agrolynch.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.domain.model.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

object PdfGenerator {

    fun printPdf(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print("${context.packageName} Bill", object : PrintDocumentAdapter() {
            override fun onLayout(old: PrintAttributes?, newAttrs: PrintAttributes, signal: CancellationSignal?, cb: LayoutResultCallback, extras: Bundle?) {
                if (signal?.isCanceled == true) { cb.onLayoutCancelled(); return }
                val info = PrintDocumentInfo.Builder(file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
                cb.onLayoutFinished(info, true)
            }
            override fun onWrite(pages: Array<out PageRange>?, dest: ParcelFileDescriptor?, signal: CancellationSignal?, cb: WriteResultCallback?) {
                try {
                    FileInputStream(file).use { input -> FileOutputStream(dest?.fileDescriptor).use { output -> input.copyTo(output) } }
                    cb?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) { cb?.onWriteFailed(e.message) }
            }
        }, null)
    }

    fun sharePdf(context: Context, file: File) {
        Timber.d("PDF_SHARE_CLICKED")
        try {
            val filePath = file.absolutePath
            val exists = file.exists()
            Timber.d("PDF_FILE_PATH: $filePath")
            Timber.d("PDF_FILE_EXISTS: $exists")

            if (!exists || file.length() == 0L) {
                Timber.e("PDF_FILE_MISSING_OR_EMPTY")
                android.widget.Toast.makeText(context, "Backup file not found or empty", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Timber.d("PDF_URI_CREATED: $uri")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Timber.d("PDF_SHARE_INTENT_STARTED")
            context.startActivity(Intent.createChooser(intent, "Share Bill"))
        } catch (e: Exception) {
            Timber.e(e, "PDF_SHARE_FAILED")
            android.widget.Toast.makeText(context, "Failed to share PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun generateFarmerReport(context: Context, profile: CompanyProfileEntity, farmer: FarmerEntity, data: List<DetailedArrivalReportModel>, range: String): File? {
        return generateReportBase(context, "FarmerReport", "FARMER REPORT: ${farmer.name} | RANGE: $range")
    }

    fun generateBuyerReport(context: Context, profile: CompanyProfileEntity, buyer: BuyerEntity, data: List<DetailedSaleReportModel>, range: String): File? {
        return generateReportBase(context, "BuyerReport", "BUYER REPORT: ${buyer.name} | RANGE: $range")
    }

    fun generateCommissionReport(context: Context, profile: CompanyProfileEntity, data: List<CommissionReportModel>, range: String): File? {
        return generateReportBase(context, "CommissionReport", "COMMISSION REPORT | RANGE: $range")
    }

    fun generatePaymentReport(context: Context, profile: CompanyProfileEntity, data: List<PaymentReportModel>, range: String): File? {
        return generateReportBase(context, "PaymentReport", "PAYMENT REPORT | RANGE: $range")
    }

    fun generateDashboardSummary(context: Context, profile: CompanyProfileEntity, summary: DashboardSummary, stock: List<StockReportModel>): File? {
        return generateReportBase(context, "DashboardSummary", "DASHBOARD SUMMARY | SALES: ₹${Formatter.formatCurrency(summary.todaySales)}")
    }

    private fun generateReportBase(context: Context, fileName: String, headerText: String): File? {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawText(headerText, 50f, 100f, Paint().apply { textSize = 12f; color = Color.BLACK })
        pdf.finishPage(page)
        return savePdf(pdf, context, "Reports", fileName)
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

    private fun formatDate(t: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t))
}
