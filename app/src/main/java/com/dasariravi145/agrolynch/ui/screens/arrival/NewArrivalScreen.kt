package com.dasariravi145.agrolynch.ui.screens.arrival

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewArrivalScreen(
    viewModel: ArrivalViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    ocrFarmer: String = "",
    ocrPhone: String = "",
    ocrVillage: String = "",
    ocrProduct: String = "",
    ocrCategory: String = "",
    ocrGrade: String = "",
    ocrQty: Double = 0.0,
    ocrRate: Double = 0.0,
    ocrUnit: String = "KG",
    ocrNumBoxes: Int = 0,
    ocrWeightTon: Double = 0.0,
    ocrEmptyBoxWeight: Double = 0.0,
    ocrSpoilagePercent: Double = 0.0,
    ocrComm: Double = 5.0,
    ocrDeductions: String = "",
    onBack: () -> Unit,
    onVoiceEntryClick: () -> Unit = {},
    onScanBillClick: () -> Unit = {}
) {
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val matchedProduct by viewModel.matchedProduct.collectAsStateWithLifecycle()
    val isNewProduct by viewModel.isNewProduct.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val autoBillNumber by viewModel.billNumber.collectAsStateWithLifecycle()
    val deductions by viewModel.deductions.collectAsStateWithLifecycle()
    val totalOtherDeductions by viewModel.totalDeductions.collectAsStateWithLifecycle()

    var farmerName by remember { mutableStateOf(ocrFarmer) }
    var farmerPhone by remember { mutableStateOf(ocrPhone) }
    var farmerVillage by remember { mutableStateOf(ocrVillage) }
    var billNo by remember { mutableStateOf(ocrBillNo) }

    LaunchedEffect(autoBillNumber) {
        if (ocrBillNo.isEmpty()) {
            billNo = autoBillNumber
        }
    }
    var entryDate by remember { mutableStateOf(if (ocrDate > 0) ocrDate else System.currentTimeMillis()) }
    
    var selectedCategory by remember { mutableStateOf("Fruit") }
    var selectedUnit by remember { mutableStateOf(if(ocrUnit.isNotEmpty()) ocrUnit else "KG") }
    
    LaunchedEffect(ocrDeductions) {
        if (ocrDeductions.isNotEmpty()) {
            ocrDeductions.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val type = parts[0]
                    val amount = parts[1].toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.addDeduction(type, amount)
                    }
                }
            }
        }
    }
    
    var commissionInput by remember { mutableStateOf(Formatter.formatWeight(ocrComm)) }
    var laborInput by remember { mutableStateOf("0") }
    var transportInput by remember { mutableStateOf("0") }
    var packingInput by remember { mutableStateOf("0") }
    var otherInput by remember { mutableStateOf("0") }
    
    // Multi-Grade Entry State
    var gradeEntries by remember { 
        val initialQty = if (selectedUnit == "Boxes" && ocrWeightTon > 0) ocrWeightTon else ocrQty
        mutableStateOf(listOf(ArrivalViewModel.GradeEntry(
            grade = ocrGrade.ifEmpty { "Grade A" }, 
            quantity = initialQty, 
            boxCount = ocrNumBoxes,
            avgGrossWeight = ocrEmptyBoxWeight,
            spoilage = ocrSpoilagePercent,
            rate = ocrRate,
            unit = selectedUnit
        )))
    }

    LaunchedEffect(matchedProduct) {
        if (ocrCategory.isNotEmpty()) {
            selectedCategory = ocrCategory
        } else {
            matchedProduct?.let { selectedCategory = it.category }
        }
    }

    val totalNetQuantity by remember {
        derivedStateOf { gradeEntries.sumOf { it.netQuantity } }
    }
    
    val totalGrossAmount by remember {
        derivedStateOf { gradeEntries.sumOf { it.grossAmount } }
    }
    
    val commissionAmount by remember {
        derivedStateOf { (totalGrossAmount * (commissionInput.toDoubleOrNull() ?: 0.0)) / 100 }
    }
    
    val netAmount by remember {
        derivedStateOf { 
            totalGrossAmount - commissionAmount -
            (laborInput.toDoubleOrNull() ?: 0.0) - 
            (transportInput.toDoubleOrNull() ?: 0.0) - 
            (packingInput.toDoubleOrNull() ?: 0.0) - 
            totalOtherDeductions
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")
    var pendingFileForAction by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(exportStatus) {
        if (exportStatus.startsWith("SUCCESS:")) {
            pendingFileForAction = File(exportStatus.removePrefix("SUCCESS:"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            if (pendingFileForAction == null) {
                // If PDF generation was too fast or too slow, we wait for it or just go back
                // but usually we want to show the Print/Share dialog if it succeeded
            }
            
            if (viewModel.adMobManager.shouldShowAds()) {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    viewModel.adMobManager.showInterstitialAd(activity) {
                        if (pendingFileForAction == null) onBack()
                    }
                } else {
                    if (pendingFileForAction == null) onBack()
                }
            } else {
                if (pendingFileForAction == null) onBack()
            }
        }
    }

    if (pendingFileForAction != null) {
        AlertDialog(
            onDismissRequest = { 
                pendingFileForAction = null
                onBack()
            },
            title = { Text("Bill Generated") },
            text = { Text("Would you like to Print or Share this bill?") },
            confirmButton = {
                Button(onClick = { 
                    com.dasariravi145.agrolynch.util.PdfGenerator.printPdf(context, pendingFileForAction!!)
                    pendingFileForAction = null
                    onBack()
                }) {
                    Icon(Icons.Default.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Print")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    com.dasariravi145.agrolynch.util.PdfGenerator.sharePdf(context, pendingFileForAction!!)
                    pendingFileForAction = null
                    onBack()
                }) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.farmer_arrival)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Voice & Scan Entry Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanBillClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Bill", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onVoiceEntryClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Voice Entry", fontWeight = FontWeight.Bold)
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

            // 2. Product Section
            SectionHeader(stringResource(R.string.product_category))
            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = { viewModel.onProductQueryChange(it) },
                label = { Text(stringResource(R.string.product_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (isNewProduct) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Fruit", "Vegetable").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Unit Section
            SectionHeader(stringResource(R.string.unit_type))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("KG", "Ton", "Boxes").forEach { unit ->
                    FilterChip(
                        selected = selectedUnit == unit,
                        onClick = { selectedUnit = unit },
                        label = { Text(unit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Grade Entries
            SectionHeader(stringResource(R.string.stock_qty_per_grade))
            gradeEntries.forEachIndexed { index, entry ->
                GradeEntryRow(
                    entry = entry.copy(unit = selectedUnit),
                    unit = selectedUnit,
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

            // 5. Rate & Charges
            SectionHeader(stringResource(R.string.deductions_commissions))
            
            // Deductions Dropdown Rows
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
                    IconButton(onClick = { viewModel.removeDeduction(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }

            // Add Deduction Form
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
                                        viewModel.addDeduction(deductionType, amt, customName)
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

            // Summary Card
            val totalQtySum = gradeEntries.sumOf { it.quantity }
            val totalSpoilageSum = gradeEntries.sumOf { it.spoilage }
            val totalSpoilageKgSum = gradeEntries.sumOf { it.calculatedTotalSpoilageKg }
            val totalBoxCount = gradeEntries.sumOf { it.totalBoxes }
            val totalTareWeight = gradeEntries.sumOf { it.totalTareWeightKg }
            val totalGrossWeight = gradeEntries.sumOf { it.totalGrossWeightKg }
            val totalNetWeight = gradeEntries.sumOf { it.totalNetWeightKg }
            
            StockCalculationSummary(
                farmerName = farmerName,
                productName = productSearchQuery,
                category = selectedCategory,
                grade = gradeEntries.joinToString(", ") { it.grade },
                date = entryDate,
                billNo = billNo,
                totalQty = totalQtySum,
                spoilage = totalSpoilageSum,
                totalSpoilageKg = totalSpoilageKgSum,
                boxCount = totalBoxCount,
                tareWeight = totalTareWeight,
                netQty = totalNetQuantity,
                purchaseRate = if (totalNetWeight > 0) totalGrossAmount / totalNetWeight else 0.0,
                grossAmount = totalGrossAmount,
                commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                commissionAmount = commissionAmount,
                labor = laborInput.toDoubleOrNull() ?: 0.0,
                transport = transportInput.toDoubleOrNull() ?: 0.0,
                otherCharges = totalOtherDeductions + (packingInput.toDoubleOrNull() ?: 0.0),
                netPayable = netAmount,
                unit = selectedUnit,
                totalNetWeight = totalNetWeight,
                emptyBoxWeightPerBox = if (gradeEntries.isNotEmpty()) gradeEntries[0].avgGrossWeight else 0.0,
                totalGrossKg = totalGrossWeight,
                spoilagePercentValue = totalSpoilageSum,
                totalSpoilageKgValue = totalSpoilageKgSum,
                tareWeightValue = totalTareWeight,
                deductionList = deductions,
                gradeEntries = gradeEntries
            )

            Button(
                onClick = {
                    viewModel.saveArrivalBatch(
                        context = context,
                        farmerName = farmerName,
                        farmerPhone = farmerPhone,
                        farmerVillage = farmerVillage,
                        productName = productSearchQuery,
                        productCategory = selectedCategory,
                        unit = selectedUnit,
                        commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                        laborCharges = laborInput.toDoubleOrNull() ?: 0.0,
                        transportCharges = transportInput.toDoubleOrNull() ?: 0.0,
                        packingCharges = packingInput.toDoubleOrNull() ?: 0.0,
                        otherDeductionsUnused = 0.0,
                        billNumberUnused = billNo,
                        gradeEntries = gradeEntries
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp),
                enabled = farmerName.isNotBlank() && productSearchQuery.isNotBlank() && netAmount > 0 && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White)
                else Text(stringResource(R.string.confirm_save_stock), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun GradeEntryRow(
    entry: ArrivalViewModel.GradeEntry,
    unit: String,
    onEntryChange: (ArrivalViewModel.GradeEntry) -> Unit,
    onRemove: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Grade, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.grade,
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Grade A", "Grade B", "Grade C", "Premium", "Local").forEach { g ->
                            DropdownMenuItem(text = { Text(g) }, onClick = { onEntryChange(entry.copy(grade = g)); expanded = false })
                        }
                    }
                }
                onRemove?.let {
                    IconButton(onClick = it) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
            
            if (unit == "Boxes") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (entry.quantity == 0.0) "" else Formatter.formatWeight(entry.quantity),
                        onValueChange = { onEntryChange(entry.copy(quantity = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Total Weight (Ton)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = if (entry.boxCount == 0) "" else entry.boxCount.toString(),
                        onValueChange = { onEntryChange(entry.copy(boxCount = it.toIntOrNull() ?: 0)) },
                        label = { Text("Number of Boxes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (entry.avgGrossWeight == 0.0) "" else Formatter.formatWeight(entry.avgGrossWeight),
                        onValueChange = { onEntryChange(entry.copy(avgGrossWeight = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Empty Weight/Box (KG)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = if (entry.spoilage == 0.0) "" else Formatter.formatWeight(entry.spoilage),
                        onValueChange = { onEntryChange(entry.copy(spoilage = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Spoilage %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                OutlinedTextField(
                    value = if (entry.rate == 0.0) "" else Formatter.formatWeight(entry.rate),
                    onValueChange = { onEntryChange(entry.copy(rate = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Rate per KG") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            } else {
                // KG / Ton mode
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (entry.quantity == 0.0) "" else Formatter.formatWeight(entry.quantity),
                        onValueChange = { onEntryChange(entry.copy(quantity = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text(stringResource(R.string.total_qty)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = if (entry.spoilage == 0.0) "" else Formatter.formatWeight(entry.spoilage),
                        onValueChange = { onEntryChange(entry.copy(spoilage = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text(if (unit == "Ton") "Spoilage per Ton (KG)" else stringResource(R.string.spoilage) + " (KG)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (entry.rate == 0.0) "" else Formatter.formatWeight(entry.rate),
                        onValueChange = { onEntryChange(entry.copy(rate = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text(if (unit == "Ton") "Rate per KG" else stringResource(R.string.rate_per_unit, unit.removeSuffix("es"))) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(stringResource(R.string.gross_amount), fontSize = 10.sp, color = Color.Gray)
                            Text("₹${Formatter.formatCurrency(entry.grossAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
            
            Surface(color = Color(0xFFF1F8E9), shape = RoundedCornerShape(4.dp)) {
                Text(
                    if (unit == "Boxes") "Net Weight: ${Formatter.formatWeight(entry.totalNetWeightKg)} KG | ${entry.totalBoxes} Boxes"
                    else "Net Available: ${Formatter.formatWeight(entry.netQuantity)} $unit",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StockCalculationSummary(
    farmerName: String,
    productName: String,
    category: String,
    grade: String,
    date: Long,
    billNo: String,
    totalQty: Double,
    spoilage: Double,
    totalSpoilageKg: Double,
    boxCount: Int,
    tareWeight: Double,
    netQty: Double,
    purchaseRate: Double,
    grossAmount: Double,
    commissionPercent: Double,
    commissionAmount: Double,
    labor: Double,
    transport: Double,
    otherCharges: Double,
    netPayable: Double,
    unit: String,
    totalNetWeight: Double = 0.0,
    emptyBoxWeightPerBox: Double = 0.0,
    totalGrossKg: Double = 0.0,
    spoilagePercentValue: Double = 0.0,
    totalSpoilageKgValue: Double = 0.0,
    tareWeightValue: Double = 0.0,
    deductionList: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity> = emptyList(),
    gradeEntries: List<ArrivalViewModel.GradeEntry> = emptyList()
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)), // Very light green
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFC5E1A5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.calculation_summary),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF388E3C),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SummaryDetailRow(stringResource(R.string.farmer_name_label), farmerName)
                SummaryDetailRow(stringResource(R.string.product_name), productName)
                SummaryDetailRow(stringResource(R.string.category), category)
                if (gradeEntries.size <= 1) {
                    SummaryDetailRow(stringResource(R.string.grade_label), grade)
                }
                SummaryDetailRow(stringResource(R.string.date_label, ""), dateFormat.format(Date(date)))
                SummaryDetailRow("Bill", if (billNo.isEmpty()) "---" else billNo)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFC5E1A5))
            
            // Multi-Grade Table
            if (gradeEntries.size > 1) {
                Text("Grade Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFFE8F5E9)).padding(4.dp)) {
                    Text("Grade", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Net KG", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Rate/KG", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Amount", Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
                gradeEntries.forEach { entry ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(entry.grade, Modifier.weight(1f), fontSize = 11.sp)
                        Text(Formatter.formatWeight(entry.totalNetWeightKg), Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                        Text("₹${Formatter.formatWeight(entry.rate)}", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                        Text("₹${Formatter.formatCurrency(entry.grossAmount)}", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFC5E1A5))
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text("Total", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(Formatter.formatWeight(totalNetWeight), Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("-", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                    Text("₹${Formatter.formatCurrency(grossAmount)}", Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(12.dp))
            } else {
                // Calculation Section
                if (unit == "Ton") {
                    CalculationRow(stringResource(R.string.total_qty), "${Formatter.formatWeight(totalQty)} Ton")
                    CalculationRow("Total Weight", "${Formatter.formatWeight(totalQty * 1000)} KG")
                    CalculationRow("Total Spoilage", "${Formatter.formatWeight(totalSpoilageKg)} KG", color = Color.Red)
                    CalculationRow("Net Weight", "${Formatter.formatWeight(totalNetWeight)} KG", isBold = true)
                } else if (unit == "Boxes") {
                    CalculationRow(stringResource(R.string.gross_wt), "${Formatter.formatWeight(totalQty)} Ton")
                    CalculationRow(stringResource(R.string.boxes), "$boxCount")
                    CalculationRow(stringResource(R.string.box_wt), "${Formatter.formatWeight(emptyBoxWeightPerBox)} KG")
                    CalculationRow("Total Waste", "${Formatter.formatWeight(totalSpoilageKgValue)} KG", color = Color.Red)
                    CalculationRow(stringResource(R.string.net_wt), "${Formatter.formatWeight(totalNetWeight)} KG", isBold = true)
                } else {
                    CalculationRow(stringResource(R.string.total_qty), "${Formatter.formatWeight(totalQty)} $unit")
                    CalculationRow("Waste", "${Formatter.formatWeight(totalSpoilageKg)} KG", color = Color.Red)
                    CalculationRow(stringResource(R.string.net_available), "${Formatter.formatWeight(netQty)} $unit", isBold = true)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                CalculationRow(stringResource(R.string.rate), "₹${Formatter.formatCurrency(purchaseRate)}${if(unit == "Ton" || unit == "Boxes") " / KG" else ""}")
                
                Spacer(modifier = Modifier.height(8.dp))
                CalculationRow(stringResource(R.string.gross_amount), "₹${Formatter.formatCurrency(grossAmount)}", color = Color(0xFF2E7D32), isBold = true)
            }
            Spacer(modifier = Modifier.height(8.dp))
            CalculationRow(stringResource(R.string.commission), "₹${Formatter.formatCurrency(commissionAmount)}", color = Color.Red)
            CalculationRow(stringResource(R.string.labor_charges), "₹${Formatter.formatCurrency(labor)}", color = Color.Red)
            CalculationRow(stringResource(R.string.transport), "₹${Formatter.formatCurrency(transport)}", color = Color.Red)
            
            if (deductionList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Other Deductions:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                deductionList.forEach { d ->
                    CalculationRow(
                        label = if (d.deductionType == "Other") " • ${d.customName}" else " • ${d.deductionType}",
                        value = "₹${Formatter.formatCurrency(d.amount)}",
                        color = Color.Red
                    )
                }
            }

            CalculationRow(stringResource(R.string.other_deductions), "₹${Formatter.formatCurrency(otherCharges)}", color = Color.Red, isBold = true)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFC5E1A5))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.net_payable),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                Text(
                    text = "₹${Formatter.formatCurrency(netPayable)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}

@Composable
fun SummaryDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label: ", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun CalculationRow(label: String, value: String, color: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 15.sp, color = if (isBold) Color.Black else Color.DarkGray)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
fun ArrivalSummaryCard(totalQty: Double, spoilage: Double, netQty: Double, grossAmt: Double, netAmt: Double, unit: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.summary), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            SummaryRow(stringResource(R.string.total_qty_with_unit, unit), Formatter.formatWeight(totalQty))
            SummaryRow(stringResource(R.string.spoilage_deduction), "- ${Formatter.formatWeight(spoilage)} KG")
            SummaryRow(stringResource(R.string.net_available), "${Formatter.formatWeight(netQty)} $unit", isBold = true)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            SummaryRow(stringResource(R.string.gross_amount), "₹${Formatter.formatCurrency(grossAmt)}")
            SummaryRow(stringResource(R.string.net_farmer_payable), "₹${Formatter.formatCurrency(netAmt)}", color = Color(0xFF1B5E20), isBold = true)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Black) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = color)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun FarmerSelectionField(value: String, onValueChange: (String) -> Unit, farmers: List<FarmerEntity>, onFarmerSelected: (FarmerEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val filteredFarmers = farmers.filter { it.name.contains(value, ignoreCase = true) }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(stringResource(R.string.farmer_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Person, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        if (expanded && filteredFarmers.isNotEmpty() && value.isNotEmpty()) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                filteredFarmers.take(5).forEach { farmer ->
                    DropdownMenuItem(text = { Text("${farmer.name} (${farmer.village})") }, onClick = { onFarmerSelected(farmer); expanded = false })
                }
            }
        }
    }
}
