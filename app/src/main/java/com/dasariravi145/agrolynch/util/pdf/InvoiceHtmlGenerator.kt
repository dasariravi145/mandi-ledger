package com.dasariravi145.agrolynch.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.dasariravi145.agrolynch.util.pdf.renderer.BusinessProfile
import com.dasariravi145.agrolynch.util.pdf.renderer.InvoiceData
import com.dasariravi145.agrolynch.util.Formatter
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

object InvoiceHtmlGenerator {

    fun buildHtml(context: Context, templateId: String, profile: BusinessProfile, invoice: InvoiceData): String {
        val template = loadTemplate(context, templateId)
        val html = TemplateDataMapper.map(template, profile, invoice, templateId)
        return validateHtml(html)
    }

    private fun loadTemplate(context: Context, templateId: String): String {
        return try {
            context.assets.open("invoice_templates/$templateId.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load template: $templateId")
            ""
        }
    }

    private fun validateHtml(html: String): String {
        if (html.contains("{{") || html.contains("}}")) {
            Timber.e("HTML validation failed: Unreplaced placeholders detected.")
            // Use regex to hide any remaining placeholders
            return html.replace(Regex("\\{\\{.*?\\}\\}"), "")
        }
        return html
    }

    private object TemplateDataMapper {
        
        private fun formatQtyWithUnit(qty: Double, unit: String): String {
            val df = java.text.DecimalFormat("0.##")
            df.roundingMode = java.math.RoundingMode.FLOOR
            val formattedQty = df.format(qty)
            return "$formattedQty $unit"
        }

        fun map(html: String, profile: BusinessProfile, invoice: InvoiceData, templateId: String): String {
            var result = html

            // 1. Business Profile
            result = result.replace("{{companyName}}", (profile.companyName ?: "").uppercase(Locale.getDefault()))
            result = result.replace("{{tagline}}", profile.tagline ?: "")
            result = result.replace("{{address}}", profile.address ?: "")
            result = result.replace("{{mobile}}", profile.mobile ?: "")
            result = result.replace("{{gstNumber}}", (profile.gstNumber ?: "").uppercase(Locale.getDefault()))
            result = result.replace("{{proprietor}}", if (profile.proprietor.isNotBlank()) "👤 ${profile.proprietor}" else "")

            // 2. Invoice Data
            result = result.replace("{{customerName}}", (invoice.customerName ?: "").uppercase(Locale.getDefault()))
            result = result.replace("{{customerMobile}}", invoice.customerMobile ?: "")
            result = result.replace("{{billNumber}}", invoice.billNumber ?: "")
            result = result.replace("{{date}}", Formatter.formatDate(invoice.date))
            result = result.replace("{{vehicleNumber}}", invoice.vehicleNumber ?: "")

            // 3. Conditional Visibility (Hide rows if empty)
            result = result.replace("{{taglineHidden}}", if (profile.tagline.isBlank()) "hidden" else "")
            result = result.replace("{{gstHidden}}", if (profile.gstNumber.isBlank()) "hidden" else "")
            result = result.replace("{{proprietorHidden}}", if (profile.proprietor.isBlank()) "hidden" else "")
            result = result.replace("{{addressHidden}}", if (profile.address.isBlank()) "hidden" else "")
            result = result.replace("{{vehicleHidden}}", if (invoice.vehicleNumber.isBlank()) "hidden" else "")
            result = result.replace("{{mobileHidden}}", if (profile.mobile.isBlank()) "hidden" else "")
            result = result.replace("{{customerMobileHidden}}", if (invoice.customerMobile.isBlank()) "hidden" else "")
            result = result.replace("{{invoiceDate}}", Formatter.formatDate(invoice.date))

            // 4. Images (Base64)
            result = result.replace("{{logoBase64}}", profile.logoPath?.let { path -> imageToBase64(path) } ?: "")
            result = result.replace("{{godImageBase64}}", profile.godImagePath?.let { path -> imageToBase64(path) } ?: "")
            result = result.replace("{{qrBase64}}", profile.qrPath?.let { path -> imageToBase64(path) } ?: "")
            result = result.replace("{{signatureBase64}}", profile.signaturePath?.let { path -> imageToBase64(path) } ?: "")
            result = result.replace("{{stampBase64}}", profile.stampPath?.let { path -> imageToBase64(path) } ?: "")

            result = result.replace("{{logoHidden}}", if (profile.logoPath.isNullOrEmpty()) "hidden" else "")
            result = result.replace("{{godImageHidden}}", if (profile.godImagePath.isNullOrEmpty()) "hidden" else "")
            result = result.replace("{{qrHidden}}", if (profile.qrPath.isNullOrEmpty()) "hidden" else "")
            result = result.replace("{{signatureHidden}}", if (profile.signaturePath.isNullOrEmpty()) "hidden" else "")
            result = result.replace("{{stampHidden}}", if (profile.stampPath.isNullOrEmpty()) "hidden" else "")

            // 5. Items Table
            result = result.replace("{{productRows}}", buildProductRows(invoice, templateId))

            // 6. Totals
            result = result.replace("{{subtotal}}", Formatter.formatCurrency(invoice.subtotal))
            result = result.replace("{{commission}}", Formatter.formatCurrency(invoice.commission))
            result = result.replace("{{labour}}", Formatter.formatCurrency(invoice.labour))
            result = result.replace("{{transport}}", Formatter.formatCurrency(invoice.transport))
            result = result.replace("{{advance}}", Formatter.formatCurrency(invoice.advance))
            result = result.replace("{{others}}", Formatter.formatCurrency(invoice.others))
            result = result.replace("{{grandTotal}}", Formatter.formatCurrency(invoice.grandTotal))
            result = result.replace("{{amountInWords}}", Formatter.numberToWords(invoice.grandTotal.toLong()))

            result = result.replace("{{commissionHidden}}", if (invoice.commission <= 0) "hidden" else "")
            result = result.replace("{{labourHidden}}", if (invoice.labour <= 0) "hidden" else "")
            result = result.replace("{{transportHidden}}", if (invoice.transport <= 0) "hidden" else "")
            result = result.replace("{{advanceHidden}}", if (invoice.advance <= 0) "hidden" else "")
            result = result.replace("{{othersHidden}}", if (invoice.others <= 0) "hidden" else "")

            return result
        }

        private fun buildProductRows(invoice: InvoiceData, templateId: String): String {
            val sb = StringBuilder()
            invoice.products.forEachIndexed { index, product ->
                when (templateId) {
                    "compact_print" -> {
                        sb.append("<tr>")
                        sb.append("<td class='text-left'>${product.name} ${product.grade}</td>")
                        sb.append("<td class='text-center qty-cell'>${formatQtyWithUnit(product.quantity, product.unit)}</td>")
                        sb.append("<td class='text-right'>${product.rate}</td>")
                        sb.append("<td class='text-right'>${Formatter.formatCurrency(product.amount)}</td>")
                        sb.append("</tr>")
                    }
                    "royal_heritage_mandi", "gk_fruits_classic" -> {
                        sb.append("<tr>")
                        sb.append("<td class='text-center'>${index + 1}</td>")
                        sb.append("<td class='text-left'>${product.name}</td>")
                        sb.append("<td class='text-center'>${product.grade}</td>")
                        sb.append("<td class='text-center'>${formatQtyWithUnit(product.quantity, product.unit)}</td>")
                        sb.append("<td class='text-right'>${Formatter.formatCurrency(product.amount)}</td>")
                        sb.append("</tr>")
                    }
                    else -> {
                        sb.append("<tr>")
                        sb.append("<td class='text-center'>${index + 1}</td>")
                        sb.append("<td class='text-left particulars-cell'>${product.name}</td>")
                        sb.append("<td class='text-center'>${product.grade}</td>")
                        sb.append("<td class='text-center qty-cell'>${formatQtyWithUnit(product.quantity, product.unit)}</td>")
                        sb.append("<td class='text-center'>${product.rate}</td>")
                        sb.append("<td class='text-right amount-cell'>₹${Formatter.formatCurrency(product.amount)}</td>")
                        sb.append("</tr>")
                    }
                }
            }
            // Add padding rows for professional look
            if (invoice.products.size < 8 && templateId != "compact_print") {
                repeat(8 - invoice.products.size) {
                    sb.append("<tr class='empty-row'><td colspan='100%'>&nbsp;</td></tr>")
                }
            }
            return sb.toString()
        }

        private fun imageToBase64(path: String): String {
            return try {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val byteArray = outputStream.toByteArray()
                    "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                } else ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}
