package com.dasariravi145.agrolynch.util.pdf

import android.content.Context
import android.util.Base64
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.pdf.renderer.BusinessProfile
import com.dasariravi145.agrolynch.util.pdf.renderer.InvoiceData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TemplateHtmlRenderer {

    fun renderExecutiveGlassTemplate(
        html: String,
        profile: BusinessProfile,
        invoice: InvoiceData
    ): String {
        var renderedHtml = html
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Business Info
        renderedHtml = renderedHtml.replace("{{companyName}}", profile.companyName)
        renderedHtml = renderedHtml.replace("{{address}}", profile.address)
        renderedHtml = renderedHtml.replace("{{mobile}}", profile.mobile)
        renderedHtml = renderedHtml.replace("{{gstNumber}}", profile.gstNumber)
        renderedHtml = renderedHtml.replace("{{tagline}}", profile.tagline)

        // Invoice Info
        renderedHtml = renderedHtml.replace("{{billNumber}}", invoice.billNumber)
        renderedHtml = renderedHtml.replace("{{date}}", dateFormat.format(Date(invoice.date)))
        renderedHtml = renderedHtml.replace("{{invoiceDate}}", dateFormat.format(Date(invoice.date)))
        renderedHtml = renderedHtml.replace("{{customerName}}", invoice.customerName)
        renderedHtml = renderedHtml.replace("{{customerMobile}}", invoice.customerMobile)
        renderedHtml = renderedHtml.replace("{{vehicleNumber}}", "")
        renderedHtml = renderedHtml.replace("{{vehicleHidden}}", "hidden")
        renderedHtml = renderedHtml.replace("{{toName}}", "")
        renderedHtml = renderedHtml.replace("{{toNameHidden}}", "hidden")

        // Totals
        renderedHtml = renderedHtml.replace("{{subtotal}}", Formatter.formatCurrency(invoice.subtotal))
        renderedHtml = renderedHtml.replace("{{commission}}", Formatter.formatCurrency(invoice.commission))
        renderedHtml = renderedHtml.replace("{{transport}}", Formatter.formatCurrency(invoice.transport))
        renderedHtml = renderedHtml.replace("{{labour}}", Formatter.formatCurrency(invoice.labour))
        renderedHtml = renderedHtml.replace("{{advance}}", Formatter.formatCurrency(invoice.advance))
        renderedHtml = renderedHtml.replace("{{others}}", Formatter.formatCurrency(invoice.others))
        renderedHtml = renderedHtml.replace("{{grandTotal}}", Formatter.formatCurrency(invoice.grandTotal))
        renderedHtml = renderedHtml.replace("{{netPayable}}", Formatter.formatCurrency(invoice.grandTotal))

        // Images
        renderedHtml = replaceImage(renderedHtml, "{{logoBase64}}", "{{logoHidden}}", profile.logoPath)
        renderedHtml = replaceImage(renderedHtml, "{{godImageBase64}}", "{{godImageHidden}}", profile.godImagePath)
        renderedHtml = replaceImage(renderedHtml, "{{qrBase64}}", "{{qrHidden}}", profile.qrPath)
        renderedHtml = replaceImage(renderedHtml, "{{signatureBase64}}", "{{signatureHidden}}", profile.signaturePath)
        renderedHtml = replaceImage(renderedHtml, "{{stampBase64}}", "{{stampHidden}}", profile.stampPath)

        // Product Rows
        val rowsHtml = StringBuilder()
        invoice.products.forEachIndexed { index, product ->
            rowsHtml.append("<tr>")
            rowsHtml.append("<td class='text-center'>${index + 1}</td>")
            rowsHtml.append("<td class='text-left'>${product.name}</td>")
            rowsHtml.append("<td class='text-center'>${product.grade}</td>")
            rowsHtml.append("<td class='text-center'>${product.quantity}</td>")
            rowsHtml.append("<td class='text-center'>₹${Formatter.formatCurrency(product.rate)}</td>")
            rowsHtml.append("<td class='text-right'>₹${Formatter.formatCurrency(product.amount)}</td>")
            rowsHtml.append("</tr>")
        }
        renderedHtml = renderedHtml.replace("{{productRows}}", rowsHtml.toString())

        return renderedHtml
    }

    private fun replaceImage(html: String, placeholder: String, hiddenPlaceholder: String, path: String?): String {
        var result = html
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                result = result.replace(placeholder, "data:image/png;base64,$base64")
                result = result.replace(hiddenPlaceholder, "")
            } else {
                result = result.replace(placeholder, "")
                result = result.replace(hiddenPlaceholder, "hidden")
            }
        } else {
            result = result.replace(placeholder, "")
            result = result.replace(hiddenPlaceholder, "hidden")
        }
        return result
    }
}
