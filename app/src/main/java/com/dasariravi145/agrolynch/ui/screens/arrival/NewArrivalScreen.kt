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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.util.findActivity
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
    ocrLabor: Double = 0.0,
    ocrTransport: Double = 0.0,
    ocrDeductions: String = "",
    ocrAutoSave: Boolean = false,
    ocrItems: String = "",
    onBack: () -> Unit,
    onVoiceEntryClick: () -> Unit = {}
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
    var laborInput by remember { mutableStateOf(Formatter.formatWeight(ocrLabor)) }
    var transportInput by remember { mutableStateOf(Formatter.formatWeight(ocrTransport)) }
    var packingInput by remember { mutableStateOf("0") }
    
    // Multi-Grade Entry State
    var gradeEntries by remember { 
        if (ocrItems.isNotEmpty()) {
            val decoded = ocrItems.split(";").mapNotNull { itemStr ->
                val parts = itemStr.split("|")
                if (parts.size >= 6) {
                    ArrivalViewModel.GradeEntry(
                        grade = parts[1].ifEmpty { "Grade A" },
                        quantity = parts[2].toDoubleOrNull() ?: 0.0,
                        rate = parts[3].toDoubleOrNull() ?: 0.0,
                        unit = parts[5],
                        spoilage = if (parts.size >= 7) parts[6].toDoubleOrNull() ?: 0.0 else 0.0
                    )
                } else null
            }
            if (decoded.isNotEmpty()) {
                mutableStateOf(decoded)
            } else {
                val initialQty = if (ocrUnit == "Boxes" && ocrWeightTon > 0) ocrWeightTon else ocrQty
                mutableStateOf(listOf(ArrivalViewModel.GradeEntry(
                    grade = ocrGrade.ifEmpty { "Grade A" }, 
                    quantity = initialQty, 
                    boxCount = ocrNumBoxes,
                    avgGrossWeight = ocrEmptyBoxWeight,
                    spoilage = ocrSpoilagePercent,
                    rate = ocrRate,
                    unit = ocrUnit
                )))
            }
        } else {
            val initialQty = if (ocrUnit == "Boxes" && ocrWeightTon > 0) ocrWeightTon else ocrQty
            mutableStateOf(listOf(ArrivalViewModel.GradeEntry(
                grade = ocrGrade.ifEmpty { "Grade A" }, 
                quantity = initialQty, 
                boxCount = ocrNumBoxes,
                avgGrossWeight = ocrEmptyBoxWeight,
                spoilage = ocrSpoilagePercent,
                rate = ocrRate,
                unit = ocrUnit
            )))
        }
    }

    LaunchedEffect(matchedProduct) {
        if (ocrCategory.isNotEmpty()) {
            selectedCategory = ocrCategory
        } else {
            matchedProduct?.let { selectedCategory = it.category }
        }
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                val (parsedFarmer, parsedItem, parsedGrade, parsedUnit, parsedQty, parsedRate, parsedWaste, parsedEach) = parseVoiceInput(spokenText)
                
                var missingFields = mutableListOf<String>()
                
                if (parsedFarmer.isNotEmpty()) farmerName = parsedFarmer else missingFields.add("Farmer")
                if (parsedItem.isNotEmpty()) viewModel.onProductQueryChange(parsedItem) else missingFields.add("Item")
                
                val currentGrade = parsedGrade.ifEmpty { "Grade A" }
                val qtyValue = if (parsedEach > 0 && parsedQty > 0 && parsedUnit == "Boxes") (parsedQty * parsedEach) / 1000.0 else parsedQty
                
                gradeEntries = listOf(ArrivalViewModel.GradeEntry(
                    grade = currentGrade,
                    quantity = qtyValue,
                    boxCount = if (parsedUnit == "Boxes") parsedQty.toInt() else 0,
                    rate = parsedRate,
                    spoilage = parsedWaste,
                    unit = parsedUnit.ifEmpty { "KG" }
                ))
                
                if (parsedRate <= 0) missingFields.add("Rate")
                if (parsedQty <= 0) missingFields.add("Quantity")

                scope.launch {
                    if (missingFields.isEmpty()) {
                        snackbarHostState.showSnackbar("Voice details filled. Please verify before saving.")
                    } else {
                        snackbarHostState.showSnackbar("Some details missing: ${missingFields.joinToString(", ")}. Please fill manually.")
                    }
                }
            }
        }
    }

    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")
    var pendingFileForAction by remember { mutableStateOf<File?>(null) }

    // Voice Entry Enhanced State
    var showLanguageSelector by remember { mutableStateOf(false) }
    var parsedVoiceData by remember { mutableStateOf<ParsedArrivalVoiceData?>(null) }
    
    val voiceEntryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                parsedVoiceData = VoiceEntryParser.parse(spokenText)
            }
        }
    }

    LaunchedEffect(exportStatus) {
        // Removed auto-showing dialog logic
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            android.widget.Toast.makeText(context, "Saved successfully", android.widget.Toast.LENGTH_SHORT).show()
            
            if (viewModel.adMobManager.shouldShowAds()) {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    viewModel.adMobManager.showInterstitialAd(activity) {
                        onBack()
                    }
                } else {
                    onBack()
                }
            } else {
                onBack()
            }
        }
    }

    if (false) { // Disabled Bill Generated Dialog
        AlertDialog(
            onDismissRequest = { 
                pendingFileForAction = null
                onBack()
            },
            title = { Text("Bill Generated") },
            text = { Text("Would you like to Print or Share this bill?") },
            confirmButton = {
                Button(onClick = {
                    val file = pendingFileForAction
                    if (file != null) {
                        val uri = com.dasariravi145.agrolynch.util.PdfGenerator.getUriFromFile(context, file)
                        pendingFileForAction = null
                        scope.launch {
                            kotlinx.coroutines.delay(200)
                            val activity = context.findActivity()
                            if (activity != null) {
                                android.util.Log.d("PRINT_DEBUG", "context=${context::class.java.name}, activity=${activity::class.java.name}")
                                activity.runOnUiThread {
                                    com.dasariravi145.agrolynch.util.PdfPrintHelper.print(activity, uri)
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Print requires active screen", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            onBack()
                        }
                    }
                }) {
                    Icon(Icons.Default.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Print")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val file = pendingFileForAction
                    if (file != null) {
                        val uri = com.dasariravi145.agrolynch.util.PdfGenerator.getUriFromFile(context, file)
                        pendingFileForAction = null
                        com.dasariravi145.agrolynch.util.PdfActionManager.sharePdf(context, uri)
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        )
    }

    if (showLanguageSelector) {
        VoiceLanguageSelector(
            onLanguageSelected = { lang ->
                showLanguageSelector = false
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.locale)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak arrival details (e.g., Dinesh mango 5 ton rate 15)")
                }
                voiceEntryLauncher.launch(intent)
            },
            onDismiss = { showLanguageSelector = false }
        )
    }

    parsedVoiceData?.let { data ->
        VoiceEntryReviewDialog(
            data = data,
            onConfirm = { d ->
                if (!d.farmerName.isNullOrEmpty()) farmerName = d.farmerName
                if (!d.product.isNullOrEmpty()) viewModel.onProductQueryChange(d.product)
                if (!d.grade.isNullOrEmpty() || d.quantity != null || d.rate != null) {
                    gradeEntries = listOf(ArrivalViewModel.GradeEntry(
                        grade = d.grade ?: "Grade A",
                        quantity = d.quantity ?: 0.0,
                        rate = d.rate ?: 0.0,
                        unit = d.unit ?: "KG"
                    ))
                }
                if (d.commission != null) commissionInput = Formatter.formatWeight(d.commission)
                if (d.transport != null) transportInput = Formatter.formatWeight(d.transport)
                if (d.labor != null) laborInput = Formatter.formatWeight(d.labor)
                if (d.otherDeduction != null) viewModel.addDeduction("Other", d.otherDeduction, "Voice CAT")
                
                parsedVoiceData = null
                scope.launch { snackbarHostState.showSnackbar("Form filled from voice input.") }
            },
            onRetry = {
                parsedVoiceData = null
                showLanguageSelector = true
            },
            onDismiss = { parsedVoiceData = null }
        )
    }

    LaunchedEffect(ocrAutoSave, isLoading) {
        if (ocrAutoSave && !isLoading && farmerName.isNotBlank() && productSearchQuery.isNotBlank() && netAmount > 0) {
            viewModel.saveArrivalBatch(
                context = context,
                farmerName = farmerName,
                farmerPhone = farmerPhone,
                farmerVillage = farmerVillage,
                productName = productSearchQuery,
                productCategory = selectedCategory,
                commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                laborCharges = laborInput.toDoubleOrNull() ?: 0.0,
                transportCharges = transportInput.toDoubleOrNull() ?: 0.0,
                packingCharges = packingInput.toDoubleOrNull() ?: 0.0,
                otherDeductionsUnused = 0.0,
                billNumberUnused = billNo,
                gradeEntries = gradeEntries
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Entry Mode
            Button(
                onClick = { onVoiceEntryClick() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text("Voice Entry", fontWeight = FontWeight.Bold)
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

            // 3. Grade Entries
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
            val totalGrossWeight = gradeEntries.sumOf { it.totalGrossWeightKg }
            val totalNetWeight = gradeEntries.sumOf { it.totalNetWeightKg }
            
            StockCalculationSummary(
                farmerName = farmerName,
                productName = productSearchQuery,
                category = selectedCategory,
                grade = gradeEntries.joinToString(", ") { it.grade },
                date = entryDate,
                billNo = billNo,
                grossAmount = totalGrossAmount,
                commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                commissionAmount = commissionAmount,
                labor = laborInput.toDoubleOrNull() ?: 0.0,
                transport = transportInput.toDoubleOrNull() ?: 0.0,
                otherCharges = totalOtherDeductions + (packingInput.toDoubleOrNull() ?: 0.0),
                netPayable = netAmount,
                totalNetWeight = totalNetWeight,
                totalGrossKg = totalGrossWeight,
                deductionList = deductions,
                gradeEntries = gradeEntries
            )

            Button(
                onClick = {
                    if (!isLoading) {
                        viewModel.saveArrivalBatch(
                            context = context,
                            farmerName = farmerName,
                            farmerPhone = farmerPhone,
                            farmerVillage = farmerVillage,
                            productName = productSearchQuery,
                            productCategory = selectedCategory,
                            commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0,
                            laborCharges = laborInput.toDoubleOrNull() ?: 0.0,
                            transportCharges = transportInput.toDoubleOrNull() ?: 0.0,
                            packingCharges = packingInput.toDoubleOrNull() ?: 0.0,
                            otherDeductionsUnused = 0.0,
                            billNumberUnused = billNo,
                            gradeEntries = gradeEntries
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp),
                enabled = farmerName.isNotBlank() && productSearchQuery.isNotBlank() && netAmount > 0 && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(stringResource(R.string.confirm_save_stock), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun GradeEntryRow(
    entry: ArrivalViewModel.GradeEntry,
    onEntryChange: (ArrivalViewModel.GradeEntry) -> Unit,
    onRemove: (() -> Unit)?
) {
    // Local text states to preserve user typing (e.g., "2.0")
    var qtyInput by remember(entry.unit) { mutableStateOf(if (entry.quantity == 0.0) "" else Formatter.formatWeight(entry.quantity)) }
    var boxCountInput by remember(entry.unit) { mutableStateOf(if (entry.boxCount == 0) "" else entry.boxCount.toString()) }
    var emptyWtInput by remember(entry.unit) { mutableStateOf(if (entry.avgGrossWeight == 0.0) "" else Formatter.formatWeight(entry.avgGrossWeight)) }
    var spoilageInput by remember(entry.unit) { mutableStateOf(if (entry.spoilage == 0.0) "" else Formatter.formatWeight(entry.spoilage)) }
    var rateInput by remember(entry.unit) { mutableStateOf(if (entry.rate == 0.0) "" else Formatter.formatWeight(entry.rate)) }

    // Sync from entry prop only if numeric values differ (prevents cursor jumps while allowing external resets)
    LaunchedEffect(entry) {
        if (qtyInput.toDoubleOrNull() ?: 0.0 != entry.quantity) {
            qtyInput = if (entry.quantity == 0.0) "" else Formatter.formatWeight(entry.quantity)
        }
        if ((boxCountInput.toIntOrNull() ?: 0) != entry.boxCount) {
            boxCountInput = if (entry.boxCount == 0) "" else entry.boxCount.toString()
        }
        if (emptyWtInput.toDoubleOrNull() ?: 0.0 != entry.avgGrossWeight) {
            emptyWtInput = if (entry.avgGrossWeight == 0.0) "" else Formatter.formatWeight(entry.avgGrossWeight)
        }
        if (spoilageInput.toDoubleOrNull() ?: 0.0 != entry.spoilage) {
            spoilageInput = if (entry.spoilage == 0.0) "" else Formatter.formatWeight(entry.spoilage)
        }
        if (rateInput.toDoubleOrNull() ?: 0.0 != entry.rate) {
            rateInput = if (entry.rate == 0.0) "" else Formatter.formatWeight(entry.rate)
        }
    }

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
            
            // Unit Selector for this grade
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("KG", "Ton", "Boxes").forEach { u ->
                    FilterChip(
                        selected = entry.unit == u,
                        onClick = { onEntryChange(entry.copy(unit = u)) },
                        label = { Text(u) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            if (entry.unit == "Boxes") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { 
                            qtyInput = it
                            onEntryChange(entry.copy(quantity = it.toDoubleOrNull() ?: 0.0)) 
                        },
                        label = { Text("Total Weight (Ton)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = boxCountInput,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                boxCountInput = it
                                onEntryChange(entry.copy(boxCount = it.toIntOrNull() ?: 0))
                            }
                        },
                        label = { Text("Number of Boxes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emptyWtInput,
                        onValueChange = { 
                            emptyWtInput = it
                            onEntryChange(entry.copy(avgGrossWeight = it.toDoubleOrNull() ?: 0.0)) 
                        },
                        label = { Text("Empty Weight/Box (KG)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = spoilageInput,
                        onValueChange = { 
                            spoilageInput = it
                            onEntryChange(entry.copy(spoilage = it.toDoubleOrNull() ?: 0.0)) 
                        },
                        label = { Text("Spoilage %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { 
                        rateInput = it
                        onEntryChange(entry.copy(rate = it.toDoubleOrNull() ?: 0.0)) 
                    },
                    label = { Text("Rate per KG") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            } else {
                // KG / Ton mode
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { 
                            qtyInput = it
                            onEntryChange(entry.copy(quantity = it.toDoubleOrNull() ?: 0.0)) 
                        },
                        label = { Text(if (entry.unit == "Ton") "Total Quantity (Ton)" else stringResource(R.string.total_qty)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = spoilageInput,
                        onValueChange = { 
                            spoilageInput = it
                            onEntryChange(entry.copy(spoilage = it.toDoubleOrNull() ?: 0.0)) 
                        },
                        label = { Text(if (entry.unit == "Ton") "Spoilage per Ton (KG)" else stringResource(R.string.spoilage) + " (KG)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { 
                            rateInput = it
                            onEntryChange(entry.copy(rate = it.toDoubleOrNull() ?: 0.0)) 
                        },
                        label = { Text(if (entry.unit == "Ton") "Rate per KG" else stringResource(R.string.rate_per_unit, entry.unit.removeSuffix("es"))) },
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
                    if (entry.unit == "Boxes") "Net Weight: ${Formatter.formatWeight(entry.totalNetWeightKg)} KG | ${entry.totalBoxes} Boxes"
                    else "Net Available: ${Formatter.formatWeight(entry.netQuantity)} ${entry.unit} | ${Formatter.formatWeight(entry.totalNetWeightKg)} KG",
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
    grossAmount: Double,
    commissionPercent: Double,
    commissionAmount: Double,
    labor: Double,
    transport: Double,
    otherCharges: Double,
    netPayable: Double,
    totalNetWeight: Double = 0.0,
    totalGrossKg: Double = 0.0,
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
            Text("Grade Breakdown:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFE8F5E9)).padding(4.dp)) {
                Text("Grade", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Unit", Modifier.weight(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Net KG", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("Rate/KG", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("Amount", Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            gradeEntries.forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(entry.grade, Modifier.weight(1f), fontSize = 11.sp)
                    Text(entry.unit, Modifier.weight(0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    Text(Formatter.formatWeight(entry.totalNetWeightKg), Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                    Text("₹${Formatter.formatWeight(entry.rate)}", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                    Text("₹${Formatter.formatCurrency(entry.grossAmount)}", Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFC5E1A5))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("Total", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("", Modifier.weight(0.7f))
                Text(Formatter.formatWeight(totalNetWeight), Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("-", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                Text("₹${Formatter.formatCurrency(grossAmount)}", Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(12.dp))

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

fun parseVoiceInput(text: String): ParsedArrival {
    // ... logic moved to VoiceEntryParser
    return VoiceEntryParser.parse(text).let {
        ParsedArrival(
            farmer = it.farmerName ?: "",
            item = it.product ?: "",
            grade = it.grade ?: "",
            unit = it.unit ?: "KG",
            quantity = it.quantity ?: 0.0,
            rate = it.rate ?: 0.0,
            waste = it.otherDeduction ?: 0.0,
            each = 0.0
        )
    }
}

data class ParsedArrival(
    val farmer: String,
    val item: String,
    val grade: String,
    val unit: String,
    val quantity: Double,
    val rate: Double,
    val waste: Double,
    val each: Double
)

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
