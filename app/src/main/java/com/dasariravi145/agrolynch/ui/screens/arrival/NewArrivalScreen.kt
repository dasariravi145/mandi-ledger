package com.dasariravi145.agrolynch.ui.screens.arrival

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewArrivalScreen(
    viewModel: ArrivalViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    ocrFarmer: String = "",
    ocrProduct: String = "",
    ocrQty: Double = 0.0,
    ocrRate: Double = 0.0,
    onBack: () -> Unit
) {
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val matchedProduct by viewModel.matchedProduct.collectAsStateWithLifecycle()
    val isNewProduct by viewModel.isNewProduct.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()

    var farmerName by remember { mutableStateOf(ocrFarmer) }
    var farmerPhone by remember { mutableStateOf("") }
    var farmerVillage by remember { mutableStateOf("") }
    
    var selectedCategory by remember { mutableStateOf("Fruit") }
    var selectedUnit by remember { mutableStateOf("KG") }
    
    // Multi-Grade Entry State
    var gradeEntries by remember { 
        mutableStateOf(listOf(ArrivalViewModel.GradeEntry("Grade A", ocrQty, 0.0, ocrRate))) 
    }

    LaunchedEffect(ocrProduct) {
        if (ocrProduct.isNotEmpty()) {
            viewModel.onProductQueryChange(ocrProduct)
        }
    }
    
    var commissionInput by remember { mutableStateOf("5") }
    var laborInput by remember { mutableStateOf("0") }
    var transportInput by remember { mutableStateOf("0") }
    var packingInput by remember { mutableStateOf("0") }
    var otherInput by remember { mutableStateOf("0") }

    LaunchedEffect(matchedProduct) {
        matchedProduct?.let { selectedCategory = it.category }
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
            (otherInput.toDoubleOrNull() ?: 0.0)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stock_entry)) },
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
            // 1. Farmer Section
            SectionHeader(stringResource(R.string.farmer_details))
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
                    entry = entry,
                    showBoxFields = selectedUnit == "Boxes",
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
                onClick = { gradeEntries = gradeEntries + ArrivalViewModel.GradeEntry("Grade ${'A' + gradeEntries.size}", 0.0, 0.0, 0.0) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_another_grade))
            }

            // 5. Rate & Charges
            SectionHeader(stringResource(R.string.deductions_commissions))
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

            OutlinedTextField(
                value = otherInput,
                onValueChange = { otherInput = it },
                label = { Text(stringResource(R.string.other_deductions)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Summary Card
            ArrivalSummaryCard(
                totalQty = gradeEntries.sumOf { it.quantity },
                spoilage = gradeEntries.sumOf { it.spoilage },
                netQty = totalNetQuantity,
                grossAmt = totalGrossAmount,
                netAmt = netAmount,
                unit = selectedUnit
            )

            Button(
                onClick = {
                    viewModel.saveArrivalBatch(
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
                        otherDeductions = otherInput.toDoubleOrNull() ?: 0.0,
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
    showBoxFields: Boolean,
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (entry.quantity == 0.0) "" else entry.quantity.toString(),
                    onValueChange = { onEntryChange(entry.copy(quantity = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text(stringResource(R.string.total_qty)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = if (entry.spoilage == 0.0) "" else entry.spoilage.toString(),
                    onValueChange = { onEntryChange(entry.copy(spoilage = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text(stringResource(R.string.spoilage)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (entry.rate == 0.0) "" else entry.rate.toString(),
                    onValueChange = { onEntryChange(entry.copy(rate = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text(stringResource(R.string.rate_per_unit, unit.removeSuffix("es"))) },
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
                        Text("₹${String.format("%.2f", entry.grossAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }
            
            if (showBoxFields) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (entry.boxCount == 0) "" else entry.boxCount.toString(),
                        onValueChange = { onEntryChange(entry.copy(boxCount = it.toIntOrNull() ?: 0)) },
                        label = { Text("Box Count") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = if (entry.tareWeight == 0.0) "" else entry.tareWeight.toString(),
                        onValueChange = { onEntryChange(entry.copy(tareWeight = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Tare Wt.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            
            Surface(color = Color(0xFFF1F8E9), shape = RoundedCornerShape(4.dp)) {
                Text(
                    "Net Available: ${entry.netQuantity} $unit",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold
                )
            }
        }
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
            SummaryRow(stringResource(R.string.total_qty_with_unit, unit), totalQty.toString())
            SummaryRow(stringResource(R.string.spoilage_deduction), "- $spoilage")
            SummaryRow(stringResource(R.string.net_available), netQty.toString(), isBold = true)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            SummaryRow(stringResource(R.string.gross_amount), "₹${String.format("%.2f", grossAmt)}")
            SummaryRow(stringResource(R.string.net_farmer_payable), "₹${String.format("%.2f", netAmt)}", color = Color(0xFF1B5E20), isBold = true)
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
