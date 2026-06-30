package com.dasariravi145.agrolynch.util.pdf

import android.util.Base64
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.pdf.renderer.BusinessProfile
import com.dasariravi145.agrolynch.util.pdf.renderer.InvoiceData
import java.io.File
import java.util.*

object PremiumFruitGalleryTemplateRenderer {

    fun renderPremiumFruitGalleryTemplate(
        html: String,
        profile: BusinessProfile,
        invoice: InvoiceData
    ): String {
        var renderedHtml = html

        // Company Information
        renderedHtml = renderedHtml.replace("{{companyName}}", (profile.companyName ?: "").uppercase())
        renderedHtml = renderedHtml.replace("{{address}}", profile.address ?: "")
        renderedHtml = renderedHtml.replace("{{mobile}}", profile.mobile ?: "")
        renderedHtml = renderedHtml.replace("{{gstNumber}}", (profile.gstNumber ?: "").uppercase())
        renderedHtml = renderedHtml.replace("{{gstHidden}}", if (profile.gstNumber.isBlank()) "hidden" else "")
        renderedHtml = renderedHtml.replace("{{tagline}}", profile.tagline ?: "")

        // Invoice Details
        renderedHtml = renderedHtml.replace("{{billNumber}}", invoice.billNumber)
        renderedHtml = renderedHtml.replace("{{invoiceDate}}", Formatter.formatDate(invoice.date))
        renderedHtml = renderedHtml.replace("{{customerName}}", (invoice.customerName ?: "").uppercase())
        renderedHtml = renderedHtml.replace("{{customerMobile}}", invoice.customerMobile ?: "")
        renderedHtml = renderedHtml.replace("{{paymentMode}}", "CASH") // Default

        // Images
        renderedHtml = replaceImage(renderedHtml, "{{logoBase64}}", "{{logoHidden}}", profile.logoPath)
        renderedHtml = replaceImage(renderedHtml, "{{godImageBase64}}", "{{godImageHidden}}", profile.godImagePath)
        renderedHtml = replaceImage(renderedHtml, "{{qrBase64}}", "{{qrHidden}}", profile.qrPath)
        renderedHtml = replaceImage(renderedHtml, "{{signatureBase64}}", "{{signatureHidden}}", profile.signaturePath)

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

        // Summary Section
        renderedHtml = renderedHtml.replace("{{subTotal}}", Formatter.formatCurrency(invoice.subtotal))
        
        // Commission
        val commPercent = if (invoice.subtotal > 0) (invoice.commission / invoice.subtotal * 100).toInt() else 0
        renderedHtml = renderedHtml.replace("{{commissionPercent}}", commPercent.toString())
        renderedHtml = renderedHtml.replace("{{commission}}", Formatter.formatCurrency(invoice.commission))
        renderedHtml = renderedHtml.replace("{{commissionHidden}}", if (invoice.commission <= 0) "hidden" else "")

        renderedHtml = renderedHtml.replace("{{transport}}", Formatter.formatCurrency(invoice.transport))
        renderedHtml = renderedHtml.replace("{{transportHidden}}", if (invoice.transport <= 0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{labour}}", Formatter.formatCurrency(invoice.labour))
        renderedHtml = renderedHtml.replace("{{labourHidden}}", if (invoice.labour <= 0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{advance}}", Formatter.formatCurrency(invoice.advance))
        renderedHtml = renderedHtml.replace("{{advanceHidden}}", if (invoice.advance <= 0) "hidden" else "")
        
        renderedHtml = renderedHtml.replace("{{others}}", Formatter.formatCurrency(invoice.others))
        renderedHtml = renderedHtml.replace("{{othersHidden}}", if (invoice.others <= 0) "hidden" else "")
        
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
