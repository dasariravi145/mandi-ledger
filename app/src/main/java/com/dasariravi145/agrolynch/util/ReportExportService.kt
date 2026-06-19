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
            when {
                reportName.contains("Farmer", true) -> {
                    @Suppress("UNCHECKED_CAST")
                    PdfGenerator.generateFarmerReport(context, profile, FarmerEntity(), data as List<DetailedArrivalReportModel>, "Selected Range")
                }
                reportName.contains("Buyer", true) || reportName.contains("Sales", true) -> {
                    @Suppress("UNCHECKED_CAST")
                    PdfGenerator.generateBuyerReport(context, profile, BuyerEntity(), data as List<DetailedSaleReportModel>, "Selected Range")
                }
                reportName.contains("Commission", true) -> {
                    @Suppress("UNCHECKED_CAST")
                    PdfGenerator.generateCommissionReport(context, profile, data as List<CommissionReportModel>, "Selected Range")
                }
                reportName.contains("Payment", true) || reportName.contains("Expense", true) -> {
                    @Suppress("UNCHECKED_CAST")
                    PdfGenerator.generatePaymentReport(context, profile, data as List<PaymentReportModel>, "Selected Range")
                }
                else -> {
                    Timber.w("REPORT_EXPORT: Unknown report type for PDF: $reportName")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "REPORT_EXPORT: PDF generation failed for $reportName")
            null
        }
    }

    private fun getHeaders(item: Any): List<String> = when (item) {
        is DetailedSaleReportModel -> listOf("Date", "Buyer", "Product", "Grade", "Qty", "Rate", "Gross", "Labor", "Trans", "Total")
        is DetailedArrivalReportModel -> listOf("Date", "Farmer", "Product", "Grade", "Qty", "Rate", "Gross", "Comm", "Net")
        is CommissionReportModel -> listOf("Date", "Farmer", "Product", "Category", "Grade", "Qty", "NetQty", "Rate", "GrossAmt", "CommAmt")
        is PaymentReportModel -> listOf("Date", "Party", "Type", "Mode", "Amount", "Balance", "Status")
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
        else -> listOf(item.toString().replace(",", " "))
    }

    private fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))
}
