package com.dasariravi145.agrolynch.util.pdf

import android.util.Base64
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.pdf.renderer.BusinessProfile
import com.dasariravi145.agrolynch.util.pdf.renderer.InvoiceData
import java.io.File
import java.util.*

object CompactPrintPremiumTemplateRenderer {

    fun renderCompactPrintPremiumTemplate(
        html: String,
        profile: BusinessProfile,
        invoice: InvoiceData
    ): String {
        var renderedHtml = html

        // Company Information
        renderedHtml = renderedHtml.replace("{{companyName}}", profile.companyName.orEmpty().uppercase(Locale.getDefault()))
        renderedHtml = renderedHtml.replace("{{address}}", profile.address.orEmpty())
        renderedHtml = renderedHtml.replace("{{mobile}}", profile.mobile.orEmpty())
        renderedHtml = renderedHtml.replace("{{gstNumber}}", profile.gstNumber.orEmpty().uppercase(Locale.getDefault()))
        renderedHtml = renderedHtml.replace("{{tagline}}", profile.tagline.orEmpty())

        // Invoice Details
        renderedHtml = renderedHtml.replace("{{billNumber}}", invoice.billNumber.orEmpty())
        renderedHtml = renderedHtml.replace("{{invoiceDate}}", Formatter.formatDate(invoice.date))
        renderedHtml = renderedHtml.replace("{{customerName}}", invoice.customerName.orEmpty().uppercase(Locale.getDefault()))

        // QR Image
        renderedHtml = replaceImage(renderedHtml, "{{qrBase64}}", "{{qrHidden}}", profile.qrPath)

        // Product Rows
        val rowsHtml = StringBuilder()
        invoice.products.forEach { product ->
            rowsHtml.append("<tr>")
            rowsHtml.append("<td>${product.name.orEmpty()} ${if (product.grade.orEmpty().isNotEmpty()) "(${product.grade})" else ""}</td>")
            rowsHtml.append("<td class='text-right qty-cell'>${product.quantity} ${product.unit}</td>")
            rowsHtml.append("<td class='text-right'>${Formatter.formatCurrency(product.rate)}</td>")
            rowsHtml.append("<td class='text-right'>${Formatter.formatCurrency(product.amount)}</td>")
            rowsHtml.append("</tr>")
        }
        renderedHtml = renderedHtml.replace("{{productRows}}", rowsHtml.toString())

        // Totals & Deductions
        renderedHtml = renderedHtml.replace("{{subTotal}}", Formatter.formatCurrency(invoice.subtotal))
        
        renderedHtml = renderedHtml.replace("{{commission}}", Formatter.formatCurrency(invoice.commission))
        renderedHtml = renderedHtml.replace("{{commissionHidden}}", if (invoice.commission <= 0.0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{transport}}", Formatter.formatCurrency(invoice.transport))
        renderedHtml = renderedHtml.replace("{{transportHidden}}", if (invoice.transport <= 0.0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{labour}}", Formatter.formatCurrency(invoice.labour))
        renderedHtml = renderedHtml.replace("{{labourHidden}}", if (invoice.labour <= 0.0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{advance}}", Formatter.formatCurrency(invoice.advance))
        renderedHtml = renderedHtml.replace("{{advanceHidden}}", if (invoice.advance <= 0.0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{others}}", Formatter.formatCurrency(invoice.others))
        renderedHtml = renderedHtml.replace("{{othersHidden}}", if (invoice.others <= 0.0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{grandTotal}}", Formatter.formatCurrency(invoice.grandTotal))

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
