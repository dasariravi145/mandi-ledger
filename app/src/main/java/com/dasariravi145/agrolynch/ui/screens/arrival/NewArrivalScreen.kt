package com.dasariravi145.agrolynch.ui.screens.arrival

import androidx.compose.foundation.background
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
    onBack: () -> Unit
) {
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // Product states from ViewModel (Optimized & Debounced)
    val matchedProduct by viewModel.matchedProduct.collectAsStateWithLifecycle()
    val isNewProduct by viewModel.isNewProduct.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()

    var farmerName by remember { mutableStateOf("") }
    var farmerPhone by remember { mutableStateOf("") }
    var farmerVillage by remember { mutableStateOf("") }
    
    // local screen-only state
    var selectedCategory by remember { mutableStateOf("Fruit") }
    var selectedGrade by remember { mutableStateOf("A Grade") }
    
    // OCR Auto-fill
    var quantityInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("") }
    var billNo by remember { mutableStateOf(ocrBillNo) }

    // Logic to handle OCR pre-fill (Amount from OCR is usually the Gross or Net)
    // For Stock Entry, we can't easily map OCR total to Rate/Qty without one of them.
    // So we'll just keep the OCR Amount in a temporary reference or pre-fill rate if possible.

    // Auto-update category when product matches
    LaunchedEffect(matchedProduct) {
        matchedProduct?.let {
            selectedCategory = it.category
        }
    }

    val categories = listOf("Fruit", "Vegetable")
    val grades = listOf("A Grade", "B Grade", "C Grade", "Premium", "Local")
    
    var selectedUnit by remember { mutableStateOf("KG") }
    val units = listOf("KG", "Ton", "Boxes", "Bags", "Crates")
    
    var commissionInput by remember { mutableStateOf("5") } // Default 5%

    // Calculations optimized with derivedStateOf
    val grossAmount by remember {
        derivedStateOf { 
            (quantityInput.toDoubleOrNull() ?: 0.0) * (rateInput.toDoubleOrNull() ?: 0.0) 
        }
    }
    
    val commissionAmount by remember {
        derivedStateOf {
            val commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0
            (grossAmount * commissionPercent) / 100
        }
    }
    
    val netAmount by remember {
        derivedStateOf { grossAmount - commissionAmount }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            onBack()
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
            // OCR Info Badge if available
            if (ocrAmount > 0) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(12.dp))
                        Text("OCR Auto-fill active (Amt: ₹$ocrAmount)", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }

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
                        onValueChange = { farmerPhone = it },
                        label = { Text(stringResource(R.string.phone)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = farmerVillage,
                        onValueChange = { farmerVillage = it },
                        label = { Text(stringResource(R.string.village)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Product Section
            SectionHeader(stringResource(R.string.product_grade))
            
            // Re-using ProductSelectionField but passing state to ViewModel for debounced search
            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = { viewModel.onProductQueryChange(it) },
                label = { Text(stringResource(R.string.product_label)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.Inventory, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Conditional Fields: Only show or enable if product is new
            if (isNewProduct) {
                Text("Category Type / వర్గం", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (selectedCategory == category) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            } else if (matchedProduct != null) {
                // Show existing category as info
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Category: ${matchedProduct?.category ?: "General"}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            GradeSelectionField(
                value = selectedGrade,
                onValueChange = { selectedGrade = it },
                grades = if (matchedProduct != null && matchedProduct!!.availableGrades.isNotEmpty()) 
                            matchedProduct!!.availableGrades 
                         else grades
            )

            // 3. Unit Section
            SectionHeader(stringResource(R.string.unit))
            UnitSelector(
                selectedUnit = selectedUnit,
                onUnitSelected = { selectedUnit = it },
                units = units
            )

            // 4. Quantity Section
            SectionHeader(stringResource(R.string.quantity_rate))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuantityInputField(
                    label = when(selectedUnit) {
                        "KG" -> "Weight (KG)"
                        "Ton" -> "Weight (Ton)"
                        "Boxes" -> "Boxes"
                        "Bags" -> "Bags"
                        "Crates" -> "Crates"
                        else -> "Quantity"
                    },
                    value = quantityInput,
                    modifier = Modifier.weight(1.2f)
                ) { quantityInput = it }
                
                QuantityInputField(
                    label = stringResource(R.string.rate),
                    value = rateInput,
                    modifier = Modifier.weight(0.8f)
                ) { rateInput = it }
            }

            // Commission Section
            SectionHeader(stringResource(R.string.commission))
            QuantityInputField(stringResource(R.string.commission) + " (%)", commissionInput) { commissionInput = it }

            // 5. Calculation Summary Card
            CalculationSummaryCard(
                grossAmount = grossAmount, 
                commissionAmount = commissionAmount, 
                netAmount = netAmount, 
                percentage = commissionInput.toDoubleOrNull() ?: 0.0
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val commissionPercent = commissionInput.toDoubleOrNull() ?: 0.0
                    if (commissionPercent < 0 || commissionPercent > 100) {
                        return@Button
                    }
                    viewModel.saveArrival(
                        farmerName = farmerName,
                        farmerPhone = farmerPhone,
                        farmerVillage = farmerVillage,
                        productName = productSearchQuery,
                        productCategory = selectedCategory,
                        grade = selectedGrade,
                        unit = selectedUnit,
                        quantity = quantityInput.toDoubleOrNull() ?: 0.0,
                        rate = rateInput.toDoubleOrNull() ?: 0.0,
                        grossAmount = grossAmount,
                        commissionPercent = commissionPercent,
                        commissionAmount = commissionAmount,
                        netAmount = netAmount
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = farmerName.isNotBlank() && productSearchQuery.isNotBlank() && grossAmount > 0 && !isLoading,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.save_arrival), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CalculationSummaryCard(grossAmount: Double, commissionAmount: Double, netAmount: Double, percentage: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Light green background
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.gross_amount), color = Color.DarkGray, fontSize = 14.sp)
                Text("₹${String.format(Locale.US, "%.2f", grossAmount)}", color = Color.Black, fontWeight = FontWeight.Medium)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.commission) + " (${percentage}%)", color = Color(0xFFC62828), fontSize = 14.sp)
                Text("- ₹${String.format(Locale.US, "%.2f", commissionAmount)}", color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
            }
            
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    stringResource(R.string.farmer_amount), 
                    color = Color(0xFF1B5E20), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${String.format(Locale.US, "%.2f", netAmount)}",
                    color = Color(0xFF1B5E20),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
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
fun QuantityInputField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onValueChange(it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun UnitSelector(selectedUnit: String, onUnitSelected: (String) -> Unit, units: List<String>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        units.forEach { unit ->
            FilterChip(
                selected = selectedUnit == unit,
                onClick = { onUnitSelected(unit) },
                label = { Text(unit, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
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

@Composable
fun ProductSelectionField(value: String, onValueChange: (String) -> Unit, products: List<ProductEntity>) {
    var expanded by remember { mutableStateOf(false) }
    val filteredProducts = products.filter { it.name.contains(value, ignoreCase = true) }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(stringResource(R.string.product_label)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Inventory, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        if (expanded && filteredProducts.isNotEmpty() && value.isNotEmpty()) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                filteredProducts.take(5).forEach { product ->
                    DropdownMenuItem(text = { Text(product.name) }, onClick = { onValueChange(product.name); expanded = false })
                }
            }
        }
    }
}

@Composable
fun GradeSelectionField(value: String, onValueChange: (String) -> Unit, grades: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(stringResource(R.string.grade_label)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            grades.forEach { grade ->
                DropdownMenuItem(text = { Text(grade) }, onClick = { onValueChange(grade); expanded = false })
            }
        }
    }
}
