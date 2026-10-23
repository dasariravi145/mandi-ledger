package com.dasariravi145.agrolynch.ui.screens.arrival

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.ui.screens.sale.FarmerArrivalData
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.Formatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerArrivalForm(
    productName: String,
    productType: String,
    category: String,
    farmers: List<FarmerEntity>,
    autoBillNo: String,
    initialData: FarmerArrivalData? = null,
    onConfirm: (FarmerArrivalData) -> Unit,
    onCancel: () -> Unit
) {
    val initialDataValue = initialData
    var farmerName by remember { mutableStateOf(initialDataValue?.farmerName ?: "") }
    var farmerPhone by remember { mutableStateOf(initialDataValue?.farmerPhone ?: "") }
    var farmerVillage by remember { mutableStateOf(initialDataValue?.farmerVillage ?: "") }
    var billNo by remember { mutableStateOf(initialDataValue?.billNumber?.ifEmpty { autoBillNo } ?: autoBillNo) }

    var selectedCategory by remember { mutableStateOf(initialDataValue?.productCategory?.ifEmpty { category } ?: category) }
    var commissionInput by remember { mutableStateOf(Formatter.formatWeight(initialDataValue?.commissionPercent ?: 5.0)) }
    var laborInput by remember { mutableStateOf(Formatter.formatWeight(initialDataValue?.laborCharges ?: 0.0)) }
    var transportInput by remember { mutableStateOf(Formatter.formatWeight(initialDataValue?.transportCharges ?: 0.0)) }
    var packingInput by remember { mutableStateOf(Formatter.formatWeight(initialDataValue?.packingCharges ?: 0.0)) }
    var buyerRateInput by remember { mutableStateOf(if ((initialDataValue?.buyerSaleRate ?: 0.0) > 0) Formatter.formatWeight(initialDataValue!!.buyerSaleRate) else "") }

    var deductions by remember { mutableStateOf(initialDataValue?.deductions ?: emptyList<EntryDeductionEntity>()) }

    var gradeEntries by remember {
        mutableStateOf(
            if (!initialDataValue?.gradeEntries.isNullOrEmpty()) {
                initialDataValue!!.gradeEntries
            } else {
                listOf(
                    ArrivalViewModel.GradeEntry(
                        grade = "Grade A",
                        quantity = 0.0,
                        rate = 0.0,
                        unit = "KG"
                    )
                )
            }
        )
    }

    val totalDeductionsAmount = deductions.sumOf { it.amount }

    val totalGrossAmount by remember {
        derivedStateOf { gradeEntries.sumOf { it.grossAmount } }
    }

    val commissionAmount by remember {
        derivedStateOf { (totalGrossAmount * (commissionInput.toDoubleOrNull() ?: 0.0)) / 100.0 }
    }

    val netAmount by remember {
        derivedStateOf {
            totalGrossAmount - commissionAmount -
                    (laborInput.toDoubleOrNull() ?: 0.0) -
                    (transportInput.toDoubleOrNull() ?: 0.0) -
                    (packingInput.toDoubleOrNull() ?: 0.0) -
                    totalDeductionsAmount
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialDataValue == null) "Add Farmer Arrival" else "Edit Farmer Arrival",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (productType.isNotBlank()) "$productName ($productType)" else productName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 1. Farmer Section
            SectionHeader(stringResource(R.string.farmer_details))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1.5f)) {
                    FarmerSelectionField(
                        value = farmerName,
                        onValueChange = { farmerName = it },
                        farmers = farmers,
                        onFarmerSelected = {
                            farmerName = it.name
                            farmerPhone = it.mobileNumber
                            farmerVillage = it.village
                        }
                    )
                }
                OutlinedTextField(
                    value = billNo,
                    onValueChange = { billNo = it },
                    label = { Text("Bill No") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (farmers.none { it.name.equals(farmerName, ignoreCase = true) } && farmerName.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = farmerPhone,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                farmerPhone = it
                            }
                        },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = farmerVillage,
                        onValueChange = { farmerVillage = it },
                        label = { Text("Village") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Product summary line
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Product: $productName", fontWeight = FontWeight.Bold)
                        if (productType.isNotBlank()) {
                            Text("Variety: $productType", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(selectedCategory) }
                    )
                }
            }

            // 2. Grade Entries
            SectionHeader(stringResource(R.string.stock_qty_per_grade))
            gradeEntries.forEachIndexed { index, entry ->
                GradeEntryRow(
                    entry = entry,
                    onEntryChange = { updated ->
                        val newList = gradeEntries.toMutableList()
                        newList[index] = updated
                        gradeEntries = newList
                    },
                    onRemove = if (gradeEntries.size > 1) { {
                        val newList = gradeEntries.toMutableList()
                        newList.removeAt(index)
                        gradeEntries = newList
                    } } else null
                )
            }

            TextButton(
                onClick = { gradeEntries = gradeEntries + ArrivalViewModel.GradeEntry(grade = "Grade ${'A' + gradeEntries.size}") },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_another_grade))
            }

            // 3. Deductions & Charges (Farmer Side)
            SectionHeader(stringResource(R.string.deductions_commissions))

            deductions.forEachIndexed { index, deduction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (deduction.deductionType == "Other") deduction.customName else deduction.deductionType,
                        modifier = Modifier.weight(1.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "₹${Formatter.formatCurrency(deduction.amount)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val newList = deductions.toMutableList()
                        if (index in newList.indices) {
                            newList.removeAt(index)
                            deductions = newList
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }

            var showAddDeduction by remember { mutableStateOf(false) }
            if (!showAddDeduction) {
                OutlinedButton(
                    onClick = { showAddDeduction = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Other Deduction")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var deductionType by remember { mutableStateOf("Other") }
                        var customName by remember { mutableStateOf("") }
                        var amount by remember { mutableStateOf("") }
                        var expanded by remember { mutableStateOf(false) }

                        Box {
                            OutlinedTextField(
                                value = deductionType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Deduction Type") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                Constants.DEFAULT_DEDUCTION_TYPES.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            deductionType = type
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (deductionType == "Other") {
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Custom Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount ₹") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDeduction = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val amt = amount.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) {
                                        deductions = deductions + EntryDeductionEntity(
                                            entryId = "",
                                            entryType = Constants.EntryType.STOCK,
                                            billId = billNo,
                                            deductionType = deductionType,
                                            customName = customName,
                                            amount = amt
                                        )
                                        showAddDeduction = false
                                    }
                                },
                                enabled = amount.isNotBlank() && (deductionType != "Other" || customName.isNotBlank())
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = { commissionInput = it },
                    label = { Text(stringResource(R.string.comm_percent)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = laborInput,
                    onValueChange = { laborInput = it },
                    label = { Text(stringResource(R.string.labor_charges)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = transportInput,
                    onValueChange = { transportInput = it },
                    label = { Text(stringResource(R.string.transport)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = packingInput,
                    onValueChange = { packingInput = it },
                    label = { Text(stringResource(R.string.packing)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // 4. BUYER SIDE PRICE PER KG
            SectionHeader("Buyer Sale Price")
            OutlinedTextField(
                value = buyerRateInput,
                onValueChange = { buyerRateInput = it },
                label = { Text("Buyer Sale Rate per KG (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            // Calculation Summary
            val totalGrossWeight = gradeEntries.sumOf { it.totalGrossWeightKg }
            val totalNetWeight = gradeEntries.sumOf { it.totalNetWeightKg }

            StockCalculationSummary(
                farmerName = farmerName,
                productName = productName,
                productType = productType,
                category = selectedCategory,
                grade = gradeEntries.joinToString(", ") { it.grade },
                date = System.currentTimeMillis(),
                billNo = billNo,
                grossAmount = totalGrossAmount,
                commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                commissionAmount = commissionAmount,
                labor = laborInput.toDoubleOrNull() ?: 0.0,
                transport = transportInput.toDoubleOrNull() ?: 0.0,
                otherCharges = totalDeductionsAmount + (packingInput.toDoubleOrNull() ?: 0.0),
                netPayable = netAmount,
                totalNetWeight = totalNetWeight,
                totalGrossKg = totalGrossWeight,
                deductionList = deductions,
                gradeEntries = gradeEntries
            )

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        errorMessage = null
                        if (farmerName.isBlank()) {
                            errorMessage = "Please select or enter a farmer name."
                            return@Button
                        }
                        val buyerRate = buyerRateInput.toDoubleOrNull() ?: 0.0
                        if (buyerRate <= 0) {
                            errorMessage = "Please enter a valid Buyer Sale Rate per KG."
                            return@Button
                        }
                        for (entry in gradeEntries) {
                            if (entry.quantity <= 0) {
                                errorMessage = "Quantity must be greater than 0 for ${entry.grade}"
                                return@Button
                            }
                            if (entry.rate <= 0) {
                                errorMessage = "Farmer rate must be greater than 0 for ${entry.grade}"
                                return@Button
                            }
                            if (entry.totalNetWeightKg <= 0) {
                                errorMessage = "Net weight must be greater than 0 for ${entry.grade}"
                                return@Button
                            }
                        }

                        val existingFarmer = farmers.find { it.name.equals(farmerName, ignoreCase = true) }
                        val farmerIdToUse = existingFarmer?.id ?: (initialDataValue?.farmerId?.ifEmpty { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString())

                        val resultData = FarmerArrivalData(
                            id = initialDataValue?.id ?: UUID.randomUUID().toString(),
                            farmerId = farmerIdToUse,
                            farmerName = farmerName,
                            farmerPhone = farmerPhone,
                            farmerVillage = farmerVillage,
                            billNumber = billNo,
                            productName = productName,
                            productType = productType,
                            productCategory = selectedCategory,
                            gradeEntries = gradeEntries,
                            commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                            laborCharges = laborInput.toDoubleOrNull() ?: 0.0,
                            transportCharges = transportInput.toDoubleOrNull() ?: 0.0,
                            packingCharges = packingInput.toDoubleOrNull() ?: 0.0,
                            deductions = deductions,
                            buyerSaleRate = buyerRate
                        )

                        onConfirm(resultData)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Farmer Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
