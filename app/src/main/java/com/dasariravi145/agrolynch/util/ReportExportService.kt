package com.dasariravi145.agrolynch.util

import android.content.Context
import android.os.Environment
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.util.Formatter
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

    fun exportToPdf(context: Context, profile: CompanyProfileEntity, reportName: String, data: List<Any>): File? {
        return try {
            val range = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).let { df ->
                val now = System.currentTimeMillis()
                "Generated on ${df.format(Date(now))}"
            }

            val firstItem = data.firstOrNull()
            Timber.d("REPORT_EXPORT_PDF: name=$reportName, items=${data.size}, firstItemType=${firstItem?.javaClass?.simpleName}")

            when {
                firstItem is DetailedArrivalReportModel || reportName.contains("Farmer", true) -> {
                    PdfGenerator.generateFarmerReport(context, profile, FarmerEntity(), data.filterIsInstance<DetailedArrivalReportModel>(), range)
                }
                firstItem is DetailedSaleReportModel || reportName.contains("Buyer", true) || reportName.contains("Sales", true) -> {
                    PdfGenerator.generateBuyerReport(context, profile, BuyerEntity(), data.filterIsInstance<DetailedSaleReportModel>(), range)
                }
                firstItem is CommissionReportModel || reportName.contains("Commission", true) -> {
                    PdfGenerator.generateCommissionReport(context, profile, data.filterIsInstance<CommissionReportModel>(), range)
                }
                // Prioritize Pending/Outstanding before generic Payment check
                firstItem is OutstandingAgingModel || reportName.contains("Pending", true) || reportName.contains("Outstanding", true) || reportName.contains("Aging", true) -> {
                    PdfGenerator.generateOutstandingAgingReport(context, profile, data.filterIsInstance<OutstandingAgingModel>(), range)
                }
                firstItem is PaymentReportModel || (reportName.contains("Payment", true) && !reportName.contains("Pending", true)) || reportName.contains("Expense", true) -> {
                    PdfGenerator.generatePaymentReport(context, profile, data.filterIsInstance<PaymentReportModel>(), range)
                }
                firstItem is ProductPerformanceModel || reportName.contains("Item", true) || reportName.contains("Product", true) || reportName.contains("Stats", true) -> {
                    PdfGenerator.generateProductPerformanceReport(context, profile, data.filterIsInstance<ProductPerformanceModel>(), range)
                }
                else -> {
                    Timber.w("REPORT_EXPORT: Unknown report type for PDF: $reportName. Attempting generic export if possible.")
                    if (reportName.contains("బాకీ", true) || reportName.contains("विवरण", true)) {
                         PdfGenerator.generateOutstandingAgingReport(context, profile, emptyList(), range)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "REPORT_EXPORT: PDF generation failed for $reportName. Error: ${e.message}")
            null
        }
    }

    private fun getHeaders(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> listOf("Date", "Buyer", "Product", "Grade", "Qty", "Rate", "Gross", "Labor", "Trans", "Total")
        is DetailedArrivalReportModel -> listOf("Date", "Farmer", "Product", "Grade", "Qty", "Rate", "Gross", "Comm", "Net")
        is CommissionReportModel -> listOf("Date", "Farmer", "Product", "Category", "Grade", "Qty", "NetQty", "Rate", "GrossAmt", "CommAmt")
        is PaymentReportModel -> listOf("Date", "Party", "Type", "Mode", "Amount", "Balance", "Status")
        is OutstandingAgingModel -> listOf("Name", "Type", "Pending Amount", "Last Payment Date", "Age Days")
        is ProductPerformanceModel -> listOf("Item Name", "Grade", "Arrivals", "Sold", "Stock", "Avg Buy", "Avg Sale", "Margin")
        else -> listOf("Data")
    }

    private fun getRowDataForCsv(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> {
            val qtyDisplay = if (item.unit == "KG") Formatter.formatWeight(item.quantity)
                             else "${Formatter.formatWeight(item.inputQuantity)} (${Formatter.formatWeight(item.quantity)} KG)"
            listOf(formatDate(item.date), item.buyerName, item.productName, item.grade, qtyDisplay, Formatter.formatCurrency(item.rate), Formatter.formatCurrency(item.saleAmount), Formatter.formatCurrency(item.laborCharges), Formatter.formatCurrency(item.transportCharges), Formatter.formatCurrency(item.totalAmount))
        }
        is DetailedArrivalReportModel -> listOf(formatDate(item.date), item.farmerName, item.productName, item.grade, "${Formatter.formatWeight(item.quantity)}${item.unit}", Formatter.formatCurrency(item.rate), Formatter.formatCurrency(item.grossAmount), Formatter.formatCurrency(item.commissionAmount), Formatter.formatCurrency(item.netAmount))
        is CommissionReportModel -> listOf(formatDate(item.date), item.farmerName, item.productName, item.category, item.grade, Formatter.formatWeight(item.quantity), Formatter.formatWeight(item.netQuantity), Formatter.formatCurrency(item.rate), Formatter.formatCurrency(item.grossAmount), Formatter.formatCurrency(item.commissionAmount))
        is PaymentReportModel -> listOf(formatDate(item.date), item.partyName, item.partyType, item.paymentMode, Formatter.formatCurrency(item.amount), Formatter.formatCurrency(item.remainingBalance), item.status)
        is OutstandingAgingModel -> listOf(item.name, item.type, Formatter.formatCurrency(item.pendingAmount), item.lastPaymentDate?.let { formatDate(it) } ?: "-", item.daysPending.toString())
        is ProductPerformanceModel -> listOf(item.productName, item.grade, Formatter.formatWeight(item.totalArrivals), Formatter.formatWeight(item.totalSold), Formatter.formatWeight(item.currentStock), Formatter.formatCurrency(item.avgPurchaseRate), Formatter.formatCurrency(item.avgSaleRate), Formatter.formatCurrency(item.avgSaleRate - item.avgPurchaseRate))
        else -> listOf(item.toString().replace(",", " "))
    }

    private fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))
}
