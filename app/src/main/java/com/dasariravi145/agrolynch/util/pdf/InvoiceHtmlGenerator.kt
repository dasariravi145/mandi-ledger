package com.dasariravi145.agrolynch.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.dasariravi145.agrolynch.util.pdf.renderer.BusinessProfile
import com.dasariravi145.agrolynch.util.pdf.renderer.InvoiceData
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig
import com.dasariravi145.agrolynch.domain.model.ElementLayout
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object InvoiceHtmlGenerator {

    // Performance Optimization: Thread-safe cache for Base64 strings to avoid repeated heavy I/O and compression
    private val base64Cache = ConcurrentHashMap<String, String>()

    fun buildHtml(context: Context, templateId: String, profile: BusinessProfile, invoice: InvoiceData, config: InvoiceWizardConfig? = null): String {
        val template = loadTemplate(context, templateId)
        val body = TemplateDataMapper.map(template, profile, invoice, templateId, config)
        val validBody = validateHtml(body)
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=1000">
                <style>
                    @page { size: A4; margin: 0; }
                    * { box-sizing: border-box; -webkit-print-color-adjust: exact; }
                    body { margin: 0; padding: 0; background: white; display: flex; justify-content: center; }
                    .invoice-canvas { 
                        width: 1000px; 
                        height: 1000px; 
                        position: relative; 
                        overflow: hidden;
                        background: white;
                    }
                    img { max-width: 100%; height: auto; image-rendering: -webkit-optimize-contrast; }
                    .hidden { display: none !important; }
                    
                    /* Standard Table Cell Styles */
                    .serial-cell, .grade-cell, .qty-cell, .rate-cell { text-align: center; }
                    .description-cell { text-align: left; white-space: normal; overflow-wrap: anywhere; }
                    .amount-cell { text-align: right; white-space: nowrap; }
                    
                    /* Metadata Panel Styles */
                    .metadata-panel { border: 1px solid currentColor; padding: 8px 10px; overflow: hidden; }
                    .meta-row { display: grid; grid-template-columns: 38% 62%; align-items: start; gap: 6px; min-width: 0; margin-bottom: 4px; }
                    .meta-row:last-child { margin-bottom: 0; }
                    .meta-label { font-weight: 700; white-space: nowrap; }
                    .meta-value { font-weight: 600; min-width: 0; overflow-wrap: anywhere; }
                </style>
            </head>
            <body>
                $validBody
            </body>
            </html>
        """.trimIndent()
    }

    fun clearCache() {
        base64Cache.clear()
    }

    private fun isFileValid(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun loadTemplate(context: Context, templateId: String): String {
        return try {
            val assetPath = "invoice_templates/$templateId.html"
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.tag("InvoiceTemplate").e(e, "Failed to load template: $templateId")
            ""
        }
    }

    private fun validateHtml(html: String): String {
        var result = html
        if (result.contains("{{") || result.contains("}}")) {
            result = result.replace(Regex("\\{\\{.*?\\}\\}"), "")
        }
        // Final sanity check for images
        result = result.replace("src=\"null\"", "src=\"\" class=\"hidden\"")
        result = result.replace("src=\"undefined\"", "src=\"\" class=\"hidden\"")
        return result
    }

    private object TemplateDataMapper {
        
        private fun formatQtyWithUnit(qty: Double, unit: String): String {
            val df = java.text.DecimalFormat("0.##")
            df.roundingMode = java.math.RoundingMode.FLOOR
            val formattedQty = df.format(qty)
            return "$formattedQty $unit"
        }

        private fun escapeHtml(text: String): String {
            return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }

        private fun normalizeImageDataUrl(value: String?, defaultMimeType: String = "image/jpeg"): String? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.trim().replace("\\s".toRegex(), "")
            if (trimmed.startsWith("data:image/")) return trimmed
            return "data:$defaultMimeType;base64,$trimmed"
        }

        fun map(html: String, profile: BusinessProfile, invoice: InvoiceData, templateId: String, config: InvoiceWizardConfig? = null): String {
            var result = html

            // 1. Business Profile
            result = result.replace("{{companyName}}", escapeHtml(profile.companyName.uppercase(Locale.getDefault())))
            result = result.replace("{{tagline}}", escapeHtml(profile.tagline))
            result = result.replace("{{address}}", escapeHtml(profile.address))
            result = result.replace("{{mobile}}", escapeHtml(profile.mobile))
            result = result.replace("{{gstNumber}}", escapeHtml(profile.gstNumber.uppercase(Locale.getDefault())))
            result = result.replace("{{proprietor}}", if (profile.proprietor.isNotBlank()) "👤 ${escapeHtml(profile.proprietor)}" else "")
            result = result.replace("{{marketName}}", escapeHtml(profile.marketName ?: ""))
            result = result.replace("{{city}}", escapeHtml(profile.city ?: ""))
            result = result.replace("{{state}}", escapeHtml(profile.state ?: ""))

            // 2. Invoice Metadata
            result = result.replace("{{billNumber}}", escapeHtml(invoice.billNumber))
            result = result.replace("{{billDate}}", Formatter.formatDate(invoice.date))
            result = result.replace("{{date}}", Formatter.formatDate(invoice.date))
            result = result.replace("{{customerName}}", escapeHtml(invoice.customerName.uppercase(Locale.getDefault())))
            result = result.replace("{{customerMobile}}", escapeHtml(invoice.customerMobile))
            result = result.replace("{{customerGstin}}", escapeHtml(invoice.customerGstin ?: "").uppercase(Locale.getDefault()))
            result = result.replace("{{customerState}}", escapeHtml(invoice.customerState ?: ""))
            result = result.replace("{{vehicleNumber}}", escapeHtml(invoice.vehicleNumber))
            result = result.replace("{{placeOfSupply}}", escapeHtml(invoice.placeOfSupply ?: profile.state ?: ""))
            result = result.replace("{{reverseCharge}}", if (invoice.reverseCharge) "YES" else "NO")
            result = result.replace("{{paymentMode}}", escapeHtml(invoice.paymentMode ?: "CASH"))

            // 3. Hidden classes for metadata
            result = result.replace("{{customerMobileHidden}}", if (invoice.customerMobile.isBlank()) "hidden" else "")
            result = result.replace("{{vehicleHidden}}", if (invoice.vehicleNumber.isBlank()) "hidden" else "")
            result = result.replace("{{taglineHidden}}", if (profile.tagline.isBlank()) "hidden" else "")
            result = result.replace("{{mobileHidden}}", if (profile.mobile.isBlank()) "hidden" else "")
            result = result.replace("{{gstHidden}}", if (profile.gstNumber.isBlank()) "hidden" else "")
            result = result.replace("{{proprietorHidden}}", if (profile.proprietor.isBlank()) "hidden" else "")

            // 4. Asset Base64 Replacements (No Escaping)
            result = result.replace("{{logoBase64}}", normalizeImageDataUrl(imageToBase64(profile.logoPath)) ?: "")
            result = result.replace("{{godImageBase64}}", normalizeImageDataUrl(imageToBase64(profile.godImagePath)) ?: "")
            result = result.replace("{{qrBase64}}", normalizeImageDataUrl(imageToBase64(profile.qrPath), "image/png") ?: "")
            result = result.replace("{{signatureBase64}}", normalizeImageDataUrl(imageToBase64(profile.signaturePath), "image/png") ?: "")
            result = result.replace("{{stampBase64}}", normalizeImageDataUrl(imageToBase64(profile.stampPath), "image/png") ?: "")

            // Apply Customizations from Config
            if (config != null) {
                // Text Styles
                result = result.replace("{{companyNameStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_SHOP_NAME))
                result = result.replace("{{addressStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_ADDRESS))
                result = result.replace("{{mobileStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_PHONE))
                result = result.replace("{{taglineStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_TAGLINE))
                result = result.replace("{{gstinStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_GSTIN))
                result = result.replace("{{thankYouStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_THANK_YOU))
                
                // Asset Styles
                result = result.replace("{{logoStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_LOGO))
                result = result.replace("{{godImageStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_GOD_IMAGE))
                result = result.replace("{{qrStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_QR_CODE))
                result = result.replace("{{signatureStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_SIGNATURE_BLOCK))
                result = result.replace("{{stampStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_STAMP))
                
                // Fixed Structural Styles
                val definition = com.dasariravi145.agrolynch.domain.model.InvoiceTemplateDefinition.getDefinition(templateId.uppercase())
                result = result.replace("{{productTableStyle}}", getFixedStyle(definition, InvoiceWizardConfig.KEY_PRODUCT_TABLE))
                result = result.replace("{{totalsBoxStyle}}", getFixedStyle(definition, InvoiceWizardConfig.KEY_TOTALS_BOX))
                result = result.replace("{{billInfoStyle}}", getFixedStyle(definition, InvoiceWizardConfig.KEY_BILL_INFO))
                result = result.replace("{{customerInfoStyle}}", getFixedStyle(definition, InvoiceWizardConfig.KEY_CUSTOMER_INFO))
                result = result.replace("{{footerStyle}}", getFixedStyle(definition, InvoiceWizardConfig.KEY_FOOTER))

                // Watermark
                val wmLayout = config.getLayout(InvoiceWizardConfig.KEY_WATERMARK)
                if (wmLayout.visible) {
                    result = result.replace("{{watermarkStyle}}", getElementStyle(config, InvoiceWizardConfig.KEY_WATERMARK))
                    if (config.watermarkType == "IMAGE" && !profile.watermarkImagePath.isNullOrEmpty()) {
                         result = result.replace("{{watermarkContent}}", "<img src='${normalizeImageDataUrl(imageToBase64(profile.watermarkImagePath))}' style='width: 100%; height: 100%; object-fit: contain;'>")
                    } else {
                         result = result.replace("{{watermarkContent}}", escapeHtml(config.watermarkText ?: (profile.companyName ?: "")))
                    }
                    result = result.replace("{{watermarkHidden}}", "")
                } else {
                    result = result.replace("{{watermarkHidden}}", "hidden")
                }

                // Visibility overrides
                result = result.replace("{{logoHidden}}", if (config.getLayout(InvoiceWizardConfig.KEY_LOGO).visible && isFileValid(profile.logoPath)) "" else "hidden")
                result = result.replace("{{godImageHidden}}", if (config.getLayout(InvoiceWizardConfig.KEY_GOD_IMAGE).visible && isFileValid(profile.godImagePath)) "" else "hidden")
                result = result.replace("{{qrHidden}}", if (config.getLayout(InvoiceWizardConfig.KEY_QR_CODE).visible && isFileValid(profile.qrPath)) "" else "hidden")
                result = result.replace("{{signatureHidden}}", if (config.getLayout(InvoiceWizardConfig.KEY_SIGNATURE_BLOCK).visible && isFileValid(profile.signaturePath)) "" else "hidden")
                result = result.replace("{{stampHidden}}", if (config.getLayout(InvoiceWizardConfig.KEY_STAMP).visible && isFileValid(profile.stampPath)) "" else "hidden")
                
                // Page Styling
                val pageStyle = getPageStyle(config, templateId)
                result = result.replace("class=\"invoice-canvas\"", "class=\"invoice-canvas\" style=\"$pageStyle\"")
                result = result.replace("class='invoice-canvas'", "class='invoice-canvas' style=\"$pageStyle\"")
                
                // QR Label
                if (config.showQrLabel) {
                    val qrLabelStyle = getQrLabelStyle(config, templateId)
                    result = result.replace(Regex("SCAN (TO|&|AND) PAY", RegexOption.IGNORE_CASE), "<div style=\"$qrLabelStyle\">SCAN TO PAY</div>")
                } else {
                    result = result.replace(Regex("SCAN (TO|&|AND) PAY", RegexOption.IGNORE_CASE), "")
                }
                
            }

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

        private fun getFixedStyle(definition: com.dasariravi145.agrolynch.domain.model.InvoiceTemplateDefinition, key: String): String {
            val layout = definition.defaultLayouts[key] ?: return ""
            return """
                position: absolute;
                left: ${layout.xPercent}%;
                top: ${layout.yPercent}%;
                width: ${layout.widthPercent}%;
                height: ${layout.heightPercent}%;
                z-index: ${layout.zIndex};
            """.trimIndent()
        }

        private fun getElementStyle(config: InvoiceWizardConfig, key: String): String {
            val layout = config.getLayout(key)
            val base = """
                position: absolute;
                left: ${layout.xPercent}%;
                top: ${layout.yPercent}%;
                width: ${if (layout.widthPercent > 0) "${layout.widthPercent}%" else "auto"};
                height: ${if (layout.heightPercent > 0) "${layout.heightPercent}%" else "auto"};
                z-index: ${layout.zIndex};
                opacity: ${layout.opacity};
                transform: rotate(${layout.rotation}deg);
                display: ${if (layout.visible) "block" else "none"};
                overflow: hidden;
            """.trimIndent()
            
            val color = when(layout.textColorOption) {
                "BLACK" -> "#000000"
                "TEMPLATE" -> getTemplatePrimaryColor(config.template.lowercase())
                else -> "" // Default: use template CSS
            }

            val textStyle = if (key in listOf(
                    InvoiceWizardConfig.KEY_SHOP_NAME,
                    InvoiceWizardConfig.KEY_TAGLINE,
                    InvoiceWizardConfig.KEY_ADDRESS,
                    InvoiceWizardConfig.KEY_PHONE,
                    InvoiceWizardConfig.KEY_GSTIN,
                    InvoiceWizardConfig.KEY_THANK_YOU,
                    InvoiceWizardConfig.KEY_FOOTER,
                    InvoiceWizardConfig.KEY_AUTHORIZED_LABEL,
                    InvoiceWizardConfig.KEY_WATERMARK
                )) {
                "font-size: ${layout.fontSize}px; text-align: ${layout.alignment.lowercase()}; ${if(color.isNotEmpty()) "color: $color;" else ""} font-weight: ${layout.fontWeight.lowercase()}; background-color: ${layout.backgroundColor};"
            } else ""
            
            val shapeStyle = if (key in listOf(InvoiceWizardConfig.KEY_LOGO, InvoiceWizardConfig.KEY_GOD_IMAGE, InvoiceWizardConfig.KEY_SIGNATURE_BLOCK, InvoiceWizardConfig.KEY_STAMP, InvoiceWizardConfig.KEY_WATERMARK)) {
                getShapeStyle(layout.shape)
            } else ""

            val borderStyle = getBorderStyle(layout, config.template.lowercase())
            
            // QR Size adjustment
            val qrStyle = if (key == InvoiceWizardConfig.KEY_QR_CODE) {
                val size = when(config.qrSizeOption) {
                    "SMALL" -> "80px"
                    "LARGE" -> "160px"
                    else -> "120px" // MEDIUM
                }
                "width: $size; height: $size; background: white; padding: 5px; border-radius: 4px;"
            } else ""
            
            return "$base $textStyle $shapeStyle $borderStyle $qrStyle"
        }

        private fun getPageStyle(config: InvoiceWizardConfig, templateId: String): String {
            val bgColor = when(config.pageBackgroundOption) {
                "WHITE" -> "#FFFFFF"
                "SOFT_TINT" -> getTemplateSoftTint(templateId)
                else -> "" // Use template default (CSS class)
            }
            
            val border = when(config.pageBorderThickness) {
                "THIN" -> "1px solid #333"
                "MEDIUM" -> "3px solid #000"
                else -> ""
            }
            
            val radius = when(config.pageCornerStyle) {
                "SLIGHTLY_ROUNDED" -> "20px"
                "ROUNDED" -> "40px"
                else -> "0px"
            }
            
            val styles = mutableListOf<String>()
            if (bgColor.isNotEmpty()) styles.add("background-color: $bgColor !important")
            if (border.isNotEmpty()) styles.add("border: $border !important")
            if (radius != "0px") styles.add("border-radius: $radius !important")
            
            return styles.joinToString("; ")
        }

        private fun getQrLabelStyle(config: InvoiceWizardConfig, templateId: String): String {
            val color = when(config.qrLabelColorOption) {
                "BLACK" -> "#000000"
                "TEMPLATE" -> getTemplatePrimaryColor(templateId)
                else -> ""
            }
            val weight = config.qrLabelStyle.lowercase()
            return "text-align: center; font-size: 10px; margin-top: 4px; ${if(color.isNotEmpty()) "color: $color;" else ""} font-weight: $weight;"
        }

        private fun getTemplatePrimaryColor(templateId: String): String {
            return com.dasariravi145.agrolynch.domain.model.InvoiceTemplateDefinition.getDefinition(templateId.uppercase()).primaryColor
        }

        private fun getTemplateSoftTint(templateId: String): String {
            return com.dasariravi145.agrolynch.domain.model.InvoiceTemplateDefinition.getDefinition(templateId.uppercase()).backgroundColor
        }

        private fun getBorderStyle(layout: ElementLayout, templateId: String): String {
            if (layout.borderThickness == "NONE") return ""
            
            val width = when(layout.borderThickness) {
                "THIN" -> "1px"
                "MEDIUM" -> "2px"
                "THICK" -> "4px"
                else -> "0px"
            }
            
            val style = layout.borderStyle.lowercase()
            val radius = when(layout.cornerStyle) {
                "SLIGHTLY_ROUNDED" -> "4px"
                "ROUNDED" -> "12px"
                else -> "0px"
            }
            
            val padding = when(layout.padding) {
                "COMPACT" -> "2px"
                "NORMAL" -> "6px"
                "SPACIOUS" -> "12px"
                else -> "0px"
            }
            
            val color = if (layout.borderColor == "#000000") getTemplatePrimaryColor(templateId) else layout.borderColor

            return "border: $width $style $color; border-radius: $radius; padding: $padding;"
        }

        private fun getAssetStyle(position: String, scale: Float, shape: String): String {
            return "transform: scale($scale); ${getShapeStyle(shape)} ${getPositionStyle(position)}"
        }

        private fun getShapeStyle(shape: String): String {
            return when (shape) {
                "CIRCLE" -> "border-radius: 50%; object-fit: cover;"
                "ROUNDED" -> "border-radius: 12px; object-fit: cover;"
                "DIAMOND" -> "clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%); object-fit: cover; padding: 5%;"
                else -> "object-fit: contain;" // ORIGINAL
            }
        }

        private fun getPositionStyle(position: String): String {
            return when (position) {
                "TOP_LEFT", "LEFT" -> "float: left; text-align: left;"
                "TOP_RIGHT", "RIGHT" -> "float: right; text-align: right;"
                "TOP_CENTER", "CENTER" -> "margin-left: auto; margin-right: auto; display: block; text-align: center;"
                "BOTTOM_LEFT" -> "position: absolute; bottom: 50px; left: 34px;"
                "BOTTOM_RIGHT" -> "position: absolute; bottom: 50px; right: 34px;"
                "BOTTOM_CENTER" -> "position: absolute; bottom: 50px; left: 50%; transform: translateX(-50%);"
                else -> ""
            }
        }

        private fun buildProductRows(invoice: InvoiceData, templateId: String): String {
            val sb = StringBuilder()
            val isCompact = templateId == "compact_print"
            
            invoice.products.forEachIndexed { index, product ->
                val displayName = if (product.productType.isNotBlank()) "${product.name} - ${product.productType}" else product.name
                
                if (isCompact) {
                    // Compact 4 columns: Item, Quantity, Rate, Amount
                    val desc = if (product.grade.isNotBlank()) "$displayName (${product.grade})" else displayName
                    sb.append("<tr>")
                    sb.append("<td class='description-cell'>$desc</td>")
                    sb.append("<td class='qty-cell'>${formatQtyWithUnit(product.quantity, product.unit)}</td>")
                    sb.append("<td class='rate-cell'>${product.rate}</td>")
                    sb.append("<td class='amount-cell'>₹${Formatter.formatCurrency(product.amount)}</td>")
                    sb.append("</tr>")
                } else {
                    // Full 6 columns: Serial, Description, Grade, Quantity, Rate, Amount
                    sb.append("<tr>")
                    sb.append("<td class='serial-cell'>${index + 1}</td>")
                    sb.append("<td class='description-cell'>$displayName</td>")
                    sb.append("<td class='grade-cell'>${product.grade}</td>")
                    sb.append("<td class='qty-cell'>${formatQtyWithUnit(product.quantity, product.unit)}</td>")
                    sb.append("<td class='rate-cell'>${product.rate}</td>")
                    sb.append("<td class='amount-cell'>₹${Formatter.formatCurrency(product.amount)}</td>")
                    sb.append("</tr>")
                }
            }
            
            if (invoice.products.size < 8 && !isCompact) {
                repeat(8 - invoice.products.size) {
                    sb.append("<tr class='empty-row'><td colspan='6'>&nbsp;</td></tr>")
                }
            }
            return sb.toString()
        }

        private fun imageToBase64(path: String?): String {
            if (path == null) return ""
            // Check cache first
            base64Cache[path]?.let { return it }

            return try {
                val file = File(path)
                if (file.exists()) {
                    // Optimized decoding: Keep resolution but compress efficiently.
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565 // Reduces memory usage
                    }
                    val bitmap = BitmapFactory.decodeFile(path, options) ?: return ""
                    val outputStream = ByteArrayOutputStream()
                    
                    // Downsample if image is too large for HTML embedding
                    val finalBitmap = if (bitmap.width > 1200 || bitmap.height > 1200) {
                         val scale = 1200f / maxOf(bitmap.width, bitmap.height)
                         Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else bitmap

                    val format = if (path.lowercase().endsWith(".png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    finalBitmap.compress(format, 80, outputStream)
                    val byteArray = outputStream.toByteArray()
                    if (finalBitmap != bitmap) finalBitmap.recycle()
                    bitmap.recycle()

                    val mime = if (path.lowercase().endsWith(".png")) "image/png" else "image/jpeg"
                    val base64 = "data:$mime;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    
                    // Add to cache
                    base64Cache[path] = base64
                    base64
                } else ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}
