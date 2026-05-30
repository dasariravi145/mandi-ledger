package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import timber.log.Timber
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    onBackClick: () -> Unit
) {
    Timber.d("SaleScreen: Improved Grade Selection Flow Initialized")
    
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedGrade by viewModel.selectedGrade.collectAsStateWithLifecycle()
    val availableGrades by viewModel.availableGrades.collectAsStateWithLifecycle()
    val availableStocks by viewModel.availableStocks.collectAsStateWithLifecycle()
    val selectedQuantities by viewModel.selectedQuantities.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedBuyer by remember { mutableStateOf<BuyerEntity?>(null) }
    var buyerSearchText by remember { mutableStateOf("") }
    
    var buyerMobile by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }
    var buyerGst by remember { mutableStateOf("") }
    
    // OCR Auto-fill
    var saleRateInput by remember { mutableStateOf(if (ocrAmount > 0) ocrAmount.toString() else "") }
    var transportInput by remember { mutableStateOf("") }
    var laborInput by remember { mutableStateOf("") }

    // Live Calculations
    val totalQty by remember { 
        derivedStateOf { selectedQuantities.values.sum() } 
    }
    
    val purchaseTotal by remember {
        derivedStateOf {
            selectedQuantities.entries.sumOf { (id, qty) ->
                val arrival = availableStocks.find { it.id == id }
                qty * (arrival?.purchaseRate ?: 0.0)
            }
        }
    }
    
    val saleRate by remember {
        derivedStateOf { saleRateInput.toDoubleOrNull() ?: 0.0 }
    }
    
    val transportAmt by remember {
        derivedStateOf { transportInput.toDoubleOrNull() ?: 0.0 }
    }
    
    val laborAmt by remember {
        derivedStateOf { laborInput.toDoubleOrNull() ?: 0.0 }
    }

    val saleTotal by remember { 
        derivedStateOf { totalQty * saleRate } 
    }
    
    val commissionMargin by remember { 
        derivedStateOf { saleTotal - purchaseTotal } 
    }
    
    val finalBuyerTotal by remember {
        derivedStateOf { saleTotal + transportAmt + laborAmt }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            selectedBuyer = null
            buyerSearchText = ""
            buyerMobile = ""
            buyerAddress = ""
            buyerGst = ""
            saleRateInput = ""
            transportInput = ""
            laborInput = ""
            snackbarHostState.showSnackbar("Sale Entry Saved Successfully!")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sale_entry), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedProduct != null && selectedGrade != null) {
                SaleSummaryStickyCard(
                    selectedCount = selectedQuantities.size,
                    totalQty = totalQty,
                    purchaseTotal = purchaseTotal,
                    saleTotal = saleTotal,
                    margin = commissionMargin,
                    transport = transportAmt,
                    labor = laborAmt,
                    finalTotal = finalBuyerTotal,
                    enabled = (selectedBuyer != null || (buyerSearchText.isNotBlank() && buyerMobile.length >= 10 && buyerAddress.isNotBlank())) 
                            && selectedQuantities.isNotEmpty() && saleRate > 0 && !isLoading,
                    isLoading = isLoading,
                    onSave = {
                        if (selectedBuyer != null) {
                            viewModel.createSale(
                                buyer = selectedBuyer!!, 
                                saleRate = saleRate,
                                transportCharges = transportAmt,
                                otherCharges = laborAmt
                            )
                        } else {
                            viewModel.registerBuyerAndCreateSale(
                                name = buyerSearchText,
                                mobile = buyerMobile,
                                address = buyerAddress,
                                gst = buyerGst,
                                saleRate = saleRate,
                                transportCharges = transportAmt,
                                otherCharges = laborAmt
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FAFB)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // OCR Info Badge
            if (ocrAmount > 0) {
                item {
                    Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = Color(0xFF1565C0))
                            Spacer(Modifier.width(12.dp))
                            Text("OCR Data: Bill #$ocrBillNo | Amt: ₹$ocrAmount", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // STEP 1: SELECT OR ENTER BUYER
            item {
                BuyerSearchableDropdown(
                    value = buyerSearchText,
                    onValueChange = { 
                        buyerSearchText = it
                        if (selectedBuyer?.name != it) {
                            selectedBuyer = null
                        }
                    },
                    selectedBuyer = selectedBuyer,
                    buyers = buyers,
                    onBuyerSelected = { 
                        selectedBuyer = it
                        buyerSearchText = it.name
                    }
                )
            }

            // NEW BUYER FIELDS
            if (selectedBuyer == null && buyerSearchText.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.new_buyer_details),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        OutlinedTextField(
                            value = buyerMobile,
                            onValueChange = { buyerMobile = it },
                            label = { Text(stringResource(R.string.mobile_number) + " *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = buyerAddress,
                            onValueChange = { buyerAddress = it },
                            label = { Text(stringResource(R.string.address) + " *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = buyerGst,
                            onValueChange = { buyerGst = it },
                            label = { Text(stringResource(R.string.gst_number) + " (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // STEP 2: SELECT PRODUCT
            item {
                ProductSelectorSection(
                    selectedProduct = selectedProduct,
                    products = products,
                    onProductSelected = { viewModel.selectProduct(it) }
                )
            }

            // STEP 3: SELECT GRADE (Mandatory after Product)
            if (selectedProduct != null) {
                item {
                    GradeSelectorSection(
                        selectedGrade = selectedGrade,
                        grades = availableGrades,
                        onGradeSelected = { viewModel.selectGrade(it) }
                    )
                }
            }

            // STEP 4: BUYER SALE RATE & CHARGES
            if (selectedProduct != null && selectedGrade != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = saleRateInput,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) saleRateInput = it },
                            label = { Text(stringResource(R.string.buyer_sale_rate) + " ₹") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, null, tint = Color(0xFF2E7D32)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = transportInput,
                                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) transportInput = it },
                                label = { Text("Transport / రవాణా ₹") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = laborInput,
                                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) laborInput = it },
                                label = { Text("Labor / హమాలీ ₹") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // STEP 5: AVAILABLE FARMER STOCK (Filtered by Product + Grade)
            if (selectedProduct != null && selectedGrade != null) {
                item {
                    Text(
                        stringResource(R.string.available_farmer_stock) + " (${selectedGrade})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1B5E20)
                    )
                }

                if (availableStocks.isEmpty()) {
                    item {
                        EmptyStockPlaceholder()
                    }
                } else {
                    items(availableStocks, key = { it.id }) { arrival ->
                        FarmerStockEntryCard(
                            arrival = arrival,
                            currentQty = selectedQuantities[arrival.id] ?: 0.0,
                            onQuantityChange = { qty: Double ->
                                viewModel.updateQuantity(arrival.id, qty)
                            }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }
}

@Composable
fun BuyerSearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    selectedBuyer: BuyerEntity?,
    buyers: List<BuyerEntity>,
    onBuyerSelected: (BuyerEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = buyers.filter { it.name.contains(value, ignoreCase = true) }

    Column {
        Text(stringResource(R.string.select_enter_buyer), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it); expanded = true },
                placeholder = { Text(stringResource(R.string.type_buyer_name)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { 
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            if (expanded && filtered.isNotEmpty()) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                ) {
                    filtered.take(10).forEach { buyer ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(buyer.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Row {
                                        Text("Pending: ₹${String.format("%.0f", buyer.pendingAmount)}", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(12.dp))
                                        Text(buyer.mobileNumber, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            },
                            onClick = { onBuyerSelected(buyer); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductSelectorSection(
    selectedProduct: ProductEntity?,
    products: List<ProductEntity>,
    onProductSelected: (ProductEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(stringResource(R.string.product_v), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedProduct != null) Color(0xFF2E7D32) else Color.LightGray),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedProduct?.name ?: stringResource(R.string.select_product_stock),
                    fontWeight = if (selectedProduct != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedProduct != null) Color.Black else Color.Gray
                )
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            products.forEach { prod ->
                DropdownMenuItem(text = { Text(prod.name) }, onClick = { onProductSelected(prod); expanded = false })
            }
        }
    }
}

@Composable
fun GradeSelectorSection(
    selectedGrade: String?,
    grades: List<String>,
    onGradeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Select Grade / గ్రేడ్ ఎంచుకోండి *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedGrade != null) Color(0xFF2E7D32) else Color(0xFFC62828)),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedGrade ?: "Select Grade...",
                    fontWeight = if (selectedGrade != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedGrade != null) Color.Black else Color.Gray
                )
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            if (grades.isEmpty()) {
                DropdownMenuItem(text = { Text("General") }, onClick = { onGradeSelected("General"); expanded = false })
            } else {
                grades.forEach { grade ->
                    DropdownMenuItem(text = { Text(grade) }, onClick = { onGradeSelected(grade); expanded = false })
                }
            }
        }
    }
}

@Composable
fun FarmerStockEntryCard(
    arrival: ArrivalEntity,
    currentQty: Double,
    onQuantityChange: (Double) -> Unit
) {
    var qtyInput by remember(arrival.id) { mutableStateOf(if (currentQty > 0) currentQty.toString() else "") }
    val isSelected = currentQty > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(arrival.farmerName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(stringResource(R.string.grade_label) + ": ${arrival.grade.ifEmpty { "General" }}", fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Available", fontSize = 10.sp, color = Color.Gray)
                    Text("${arrival.remainingQuantity} ${arrival.unit}", fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Rate: ₹${arrival.purchaseRate}/${arrival.unit}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            val valDouble = it.toDoubleOrNull() ?: 0.0
                            if (valDouble <= arrival.remainingQuantity) {
                                qtyInput = it
                                onQuantityChange(valDouble)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.enter_sale_qty)) },
                    modifier = Modifier.width(140.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun SaleSummaryStickyCard(
    selectedCount: Int,
    totalQty: Double,
    purchaseTotal: Double,
    saleTotal: Double,
    margin: Double,
    transport: Double,
    labor: Double,
    finalTotal: Double,
    enabled: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111827), // Dark grey/black theme for summary
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Summary Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Farmers: $selectedCount | Qty: ${String.format("%.1f", totalQty)}", color = Color.LightGray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Margin: ", color = Color.White, fontSize = 14.sp)
                        Text("₹${String.format("%.0f", margin)}", color = if (margin >= 0) Color(0xFF4ADE80) else Color(0xFFF87171), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sale Total", color = Color.LightGray, fontSize = 12.sp)
                    Text("₹${String.format("%.0f", saleTotal)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }

            HorizontalDivider(color = Color.DarkGray)

            // Charges and Final Total
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryChargeItem("Purchase", purchaseTotal)
                    SummaryChargeItem("Transport", transport)
                    SummaryChargeItem("Labor", labor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Final Buyer Total", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.0f", finalTotal)}", color = Color(0xFF4ADE80), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.confirm_multi_farmer_sale), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryChargeItem(label: String, amount: Double) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text("₹${String.format("%.0f", amount)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyStockPlaceholder() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))) {
        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, null, tint = Color.Red)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.no_available_stock), color = Color.Red, fontWeight = FontWeight.Medium)
            }
        }
    }
}
