package com.dasariravi145.agrolynch.util.ocr

object BillValidationEngine {

    data class ValidationResult(
        val isValid: Boolean,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList()
    )

    fun validate(data: ExtractedBillData): ExtractedBillData {
        // 1. Recalculate item amounts if missing or suspicious
        val validatedItems = data.items.map { item ->
            val calculatedAmount = item.quantityKg * item.rate
            var warning = item.warning
            
            val updatedAmount = if (item.amount == 0.0 && calculatedAmount > 0) {
                warning = "Amount calculated automatically"
                calculatedAmount
            } else if (item.amount > 0 && calculatedAmount > 0 && Math.abs(item.amount - calculatedAmount) > 1.0) {
                // Keep original amount if detected, but warning already handled in UI
                item.amount
            } else if (item.amount == 0.0 && calculatedAmount == 0.0) {
                item.amount
            } else {
                item.amount
            }
            
            item.copy(amount = updatedAmount, warning = warning)
        }

        // 2. Recalculate totals
        val gross = validatedItems.sumOf { it.amount }
        val totalDeductions = data.commission + data.labour + data.transport + data.gateOrPaper + data.advance + data.others
        val net = gross - totalDeductions

        return data.copy(
            items = validatedItems,
            grossAmount = gross,
            totalDeductions = totalDeductions,
            netAmount = net
        )
    }

    fun checkStatus(data: ExtractedBillData): ValidationResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        if (data.farmerName.isBlank()) {
            errors.add("Farmer Name is required")
        } else {
            warnings.add("Please verify handwritten farmer name")
        }
        
        val validItems = data.items.filter { it.productName.isNotBlank() && it.quantityKg > 0 && it.rate > 0 }

        if (validItems.isEmpty()) {
            errors.add("At least one valid item with Quantity and Rate is required")
        }

        data.items.forEachIndexed { index, item ->
            if (item.productName.isBlank()) warnings.add("Item ${index + 1} has no product name")
            else if (item.warning != null) warnings.add("${item.productName}: ${item.warning}")
            
            if (item.productName.isNotBlank()) {
                if (item.quantityKg <= 0) warnings.add("Verify quantity for ${item.productName}")
                if (item.rate <= 0) warnings.add("Verify rate for ${item.productName}")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            warnings = warnings,
            errors = errors
        )
    }
}
