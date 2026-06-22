package com.dasariravi145.agrolynch.util.ocr

import com.google.mlkit.vision.text.Text
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object BillStructureParser {

    fun parse(visionText: Text): ExtractedBillData {
        val rawText = visionText.text
        val blocks = visionText.textBlocks
        val allLines = blocks.flatMap { it.lines }
        
        var businessName = ""
        var proprietorName = ""
        var businessType = ""
        var location = ""
        var mobileNumbers = mutableListOf<String>()
        var originalBillRefNo: String? = null
        var date = System.currentTimeMillis()
        var farmerName = ""
        var farmerPlace = ""
        
        val items = mutableListOf<ExtractedBillItem>()
        
        // Deductions
        var commission = 0.0
        var labour = 0.0
        var transport = 0.0
        var gateOrPaper = 0.0
        var advance = 0.0
        var others = 0.0

        // Regex patterns
        val mobileRegex = Regex("""[6-9]\d{9}""")
        val dateRegex = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""")
        val billNoRegex = Regex("""(?i)(?:No|Bill\s*No|Invoice\s*No)[:.\s]*(\d+)""")
        val amountRegex = Regex("""\d+""")

        // 1. First pass: Header and simple fields
        allLines.forEach { line ->
            val text = line.text.trim()
            
            // Business Name Detection
            if (businessName.isEmpty() && (text.contains("FRUITS", true) || text.contains("MANDI", true) || text.contains("GK FRUITS", true))) {
                businessName = text
            }
            
            // Proprietor Detection
            if (proprietorName.isEmpty() && (text.contains("Prop", true) || text.contains("Proprietor", true))) {
                proprietorName = text.replace(Regex("""(?i)Prop[:.]*|Proprietor[:.]*|Owner[:.]*"""), "").trim()
            }
            
            // Business Type Detection
            if (businessType.isEmpty() && (text.contains("Merchant", true) || text.contains("Commission Agent", true))) {
                businessType = text
            }
            
            // Location Detection
            if (location.isEmpty() && (text.contains("Yard", true) || text.contains("Kodur", true) || text.contains("Dist", true) || text.contains("Mango Yard", true))) {
                location = text
            }
            
            // Mobile Numbers
            mobileRegex.findAll(text).forEach { mobileNumbers.add(it.value) }
            
            // Bill Number (Reference only)
            if (originalBillRefNo == null) {
                billNoRegex.find(text)?.let { originalBillRefNo = it.groupValues[1] }
            }
            
            // Date
            dateRegex.find(text)?.let { date = parseDate(it.value) }
            
            // Farmer Name Detection (Handwritten rule: Find value after "M/s")
            if (farmerName.isEmpty() && text.contains("M/s", true)) {
                farmerName = text.replace(Regex("""(?i)M/s[:.]*"""), "").trim()
                
                // If farmerName is still empty on this line, check elements or next line
                if (farmerName.isEmpty()) {
                    // Try to find the closest element to the right or below
                    // For now, we'll check if there's text in brackets on the same line or next
                }
                
                // Place detection: text inside brackets or nearby word like Adhoni
                if (text.contains("(") && text.contains(")")) {
                    farmerPlace = text.substringAfter("(").substringBefore(")")
                } else if (farmerName.split(" ").size > 1) {
                    // If last word is a known place or looks like one
                }
            }
        }

        // Rule 3 & 4: Items detection
        val headerWordsToIgnore = listOf(
            "GK FRUITS", "Merchant", "Commission Agent", "Date", "Weight", "Kgs", 
            "Rate", "Amount", "Particulars", "Transport", "Coolie", "Gate", 
            "Advance", "Cell", "Prop", "Mango Yard", "M/s", "Mobile", "Mandi", "Ledger"
        )

        // 2. Second pass: Item Table Detection using Bounding Boxes
        val extractedItems = extractTableItems(visionText, headerWordsToIgnore)
        items.addAll(extractedItems)

        // 3. Third pass: Deductions detection
        allLines.forEach { line ->
            val text = line.text.trim()
            val numericValue = amountRegex.findAll(text).lastOrNull()?.value?.toDoubleOrNull() ?: 0.0

            if (text.contains("Coolie", true) || text.contains("Cooli", true) || text.contains("Labour", true) || text.contains("Labor", true)) {
                labour = numericValue
            } else if (text.contains("Gate", true) || text.contains("Paper", true)) {
                gateOrPaper = numericValue
            } else if (text.contains("Transport", true) || text.contains("Trans", true) || text.contains("Vandi", true)) {
                transport = numericValue
            } else if (text.contains("Advance", true) || text.contains("Adv", true)) {
                advance = numericValue
            } else if (text.contains("Commission", true) || text.contains("Comm", true)) {
                commission = numericValue
            } else if (text.contains("CAT", true)) {
                others += numericValue
            }
        }

        val validatedData = BillValidationEngine.validate(ExtractedBillData(
            businessName = businessName,
            proprietorName = proprietorName,
            businessType = businessType,
            location = location,
            mobileNumbers = mobileNumbers.distinct().joinToString(" / "),
            originalBillRefNo = originalBillRefNo,
            date = date,
            farmerName = farmerName,
            farmerPlace = farmerPlace,
            items = items,
            commission = commission,
            labour = labour,
            transport = transport,
            gateOrPaper = gateOrPaper,
            advance = advance,
            others = others,
            rawText = rawText
        ))

        return validatedData
    }

    private fun extractTableItems(visionText: Text, ignoreList: List<String>): List<ExtractedBillItem> {
        val items = mutableListOf<ExtractedBillItem>()
        val allLines = visionText.textBlocks.flatMap { it.lines }
        
        // Group lines that are at roughly the same Y position
        val yThreshold = 25 // pixels
        val sortedLines = allLines.sortedBy { it.boundingBox?.top ?: 0 }
        val rowGroups = mutableListOf<MutableList<Text.Line>>()
        
        if (sortedLines.isNotEmpty()) {
            var currentGroup = mutableListOf(sortedLines[0])
            rowGroups.add(currentGroup)
            
            for (i in 1 until sortedLines.size) {
                val line = sortedLines[i]
                val lastLine = currentGroup.last()
                val lastY = lastLine.boundingBox?.top ?: 0
                val currentY = line.boundingBox?.top ?: 0
                
                if (abs(currentY - lastY) < yThreshold) {
                    currentGroup.add(line)
                } else {
                    currentGroup = mutableListOf(line)
                    rowGroups.add(currentGroup)
                }
            }
        }

        // Process each row group
        rowGroups.forEach { row ->
            val rowText = row.joinToString(" ") { it.text }.trim()
            
            // Rule 3: Ignore header words
            if (ignoreList.any { rowText.contains(it, true) }) {
                return@forEach
            }

            // Identify elements in columns by X position
            val elements = row.flatMap { it.elements }.sortedBy { it.boundingBox?.left ?: 0 }
            if (elements.isEmpty()) return@forEach

            val rowWidth = elements.last().boundingBox?.right?.minus(elements.first().boundingBox?.left ?: 0) ?: 1
            val leftBound = elements.first().boundingBox?.left ?: 0
            
            var productName = ""
            var qty = 0.0
            var rate = 0.0
            var amount = 0.0
            
            elements.forEach { element ->
                val text = element.text.trim()
                val xPosRel = (element.boundingBox?.left?.minus(leftBound) ?: 0).toFloat() / rowWidth.toFloat()
                
                // 1. Product detection
                val normalizedFruit = normalizeFruitName(text)
                if (normalizedFruit != null) {
                    productName = normalizedFruit
                } else if (xPosRel < 0.4 && text.any { it.isLetter() } && text.length >= 3) {
                    // Only take it if it doesn't look like a number
                    if (text.toDoubleOrNull() == null) {
                        productName = text
                    }
                }
                
                // 2. Numeric detection
                val numStr = text.replace(",", "").replace(Regex("[^0-9.]"), "")
                val num = numStr.toDoubleOrNull()
                if (num != null && num > 0) {
                    if (xPosRel > 0.3 && xPosRel < 0.6) {
                        qty = num
                    } else if (xPosRel >= 0.6 && xPosRel < 0.85) {
                        rate = num
                    } else if (xPosRel >= 0.85) {
                        amount = num
                    }
                }
            }

            // Rule 4: Product extraction logic
            if (productName.isNotEmpty() || (qty > 0 && rate > 0)) {
                var warning: String? = null
                if (productName.isNotEmpty() && (qty <= 0 || rate <= 0)) {
                    warning = "Needs manual entry"
                }
                
                // Fallback for amount
                if (amount <= 0 && qty > 0 && rate > 0) {
                    amount = qty * rate
                }

                if (productName.isNotEmpty() || (qty > 0 && rate > 0)) {
                    items.add(ExtractedBillItem(
                        productName = productName.ifEmpty { "Detected Product" },
                        quantityKg = qty,
                        rate = rate,
                        amount = amount,
                        warning = warning
                    ))
                }
            }
        }

        return items
    }

    private fun normalizeFruitName(text: String): String? {
        return when {
            text.contains("Sindhura", true) || text.contains("Sindhuwa", true) || text.contains("Sindhuva", true) -> "Sindhura"
            text.contains("Totapuri", true) || text.contains("Tothapani", true) || text.contains("Totapani", true) -> "Totapuri"
            text.contains("Mango", true) -> "Mango"
            text.contains("Neelam", true) -> "Neelam"
            text.contains("Benishan", true) -> "Benishan"
            else -> null
        }
    }

    private fun parseDate(dateStr: String): Long {
        val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy", "yyyy-MM-dd")
        formats.forEach { format ->
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(dateStr)?.time ?: return@forEach
            } catch (e: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
