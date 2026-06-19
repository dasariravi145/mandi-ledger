package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.Formatter
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import java.io.File
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    ocrBillNo: String = "",
    ocrAmount: Double = 0.0,
    ocrDate: Long = 0L,
    ocrBuyer: String = "",
    ocrProduct: String = "",
    ocrQty: Double = 0.0,
    ocrRate: Double = 0.0,
    ocrDeductions: String = "",
    onBackClick: () -> Unit
) {
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()
    val saleItems by viewModel.saleItems.collectAsStateWithLifecycle()
    val transactionTotal by viewModel.transactionTotal.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val autoBillNumber by viewModel.billNumber.collectAsStateWithLifecycle()
    val deductions by viewModel.deductions.collectAsStateWithLifecycle()
    val totalOtherDeductions by viewModel.totalDeductions.collectAsStateWithLifecycle()

    var selectedBuyer by remember { 
        mutableStateOf(buyers.find { it.name.equals(ocrBuyer, ignoreCase = true) }) 
    }
    var buyerSearchText by remember { mutableStateOf(ocrBuyer) }

    LaunchedEffect(buyers) {
        if (selectedBuyer == null && ocrBuyer.isNotEmpty()) {
            selectedBuyer = buyers.find { it.name.equals(ocrBuyer, ignoreCase = true) }
        }
    }

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
    
    var buyerMobile by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }
    var buyerGst by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddItemSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
            selectedBuyer = null
            buyerSearchText = ""
            buyerMobile = ""
            buyerAddress = ""
            buyerGst = ""
            
            if (viewModel.adMobManager.shouldShowAds()) {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    viewModel.adMobManager.showInterstitialAd(activity) {
                        if (pendingFileForAction == null) onBackClick()
                    }
                } else {
                    if (pendingFileForAction == null) onBackClick()
                }
            } else {
                if (pendingFileForAction == null) onBackClick()
            }
        }
    }

    if (pendingFileForAction != null) {
        AlertDialog(
            onDismissRequest = { 
                pendingFileForAction = null
                onBackClick()
            },
            title = { Text("Bill Generated") },
            text = { Text("Would you like to Print or Share this bill?") },
            confirmButton = {
                Button(onClick = { 
                    com.dasariravi145.agrolynch.util.PdfGenerator.printPdf(context, pendingFileForAction!!)
                    pendingFileForAction = null
                    onBackClick()
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
                    onBackClick()
                }) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.buyer_sale), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (saleItems.isNotEmpty() && (selectedBuyer != null || buyerSearchText.isNotBlank())) {
                val mobileError = stringResource(R.string.mobile_number_error)
                SaleTransactionSummaryCard(
                    total = transactionTotal,
                    isLoading = isLoading,
                    onSave = {
                        if (selectedBuyer != null) {
                            viewModel.createSale(context = context, buyer = selectedBuyer!!)
                        } else {
                            if (buyerMobile.length != 10) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(mobileError)
                                }
                            } else {
                                viewModel.registerBuyerAndCreateSale(
                                    context = context,
                                    name = buyerSearchText,
                                    mobile = buyerMobile,
                                    address = buyerAddress,
                                    gst = buyerGst
                                )
                            }
                        }
                    }
                )
            }
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
            // STEP 1: BUYER SECTION (EXISTING)
            BuyerSearchableDropdown(
                value = buyerSearchText,
                onValueChange = { 
                    buyerSearchText = it
                    if (selectedBuyer?.name != it) selectedBuyer = null
                },
                selectedBuyer = selectedBuyer,
                buyers = buyers,
                onBuyerSelected = { 
                    selectedBuyer = it
                    buyerSearchText = it.name
                }
            )

            if (selectedBuyer == null && buyerSearchText.isNotBlank()) {
                NewBuyerDetailsFields(
                    mobile = buyerMobile,
                    onMobileChange = { buyerMobile = it },
                    address = buyerAddress,
                    onAddressChange = { buyerAddress = it },
                    gst = buyerGst,
                    onGstChange = { buyerGst = it }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // STEP 2: SALE ITEMS LIST
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(stringResource(R.string.sale_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (selectedBuyer != null || buyerSearchText.isNotBlank()) {
                    Button(
                        onClick = { showAddItemSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_item))
                    }
                }
            }

            if (saleItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_items_added), color = Color.Gray)
                }
            } else {
                saleItems.forEach { item ->
                    SaleItemRowCard(
                        item = item, 
                        onUpdate = { updated -> viewModel.updateSaleItem(updated) },
                        onRemove = { viewModel.removeSaleItem(item.id) }
                    )
                }
                
            // Detailed Confirmation Summary
            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.other_deductions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            deductions.forEachIndexed { index, deduction ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (deduction.deductionType == "Other") deduction.customName else deduction.deductionType, modifier = Modifier.weight(1f))
                    Text("₹${Formatter.formatCurrency(deduction.amount)}", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.removeDeduction(index) }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
                }
            }

            var showAddDeduction by remember { mutableStateOf(false) }
            if (!showAddDeduction) {
                OutlinedButton(onClick = { showAddDeduction = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Other Charge")
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var dtype by remember { mutableStateOf("Loading") }
                        var cname by remember { mutableStateOf("") }
                        var amt by remember { mutableStateOf("") }
                        var exp by remember { mutableStateOf(false) }

                        Box {
                            OutlinedTextField(value = dtype, onValueChange = {}, readOnly = true, label = { Text("Type") }, modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { IconButton(onClick = { exp = true }) { Icon(Icons.Default.ArrowDropDown, null) } })
                            DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                Constants.DEFAULT_DEDUCTION_TYPES.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { dtype = type; exp = false })
                                }
                            }
                        }
                        if (dtype == "Other") {
                            OutlinedTextField(value = cname, onValueChange = { cname = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedTextField(value = amt, onValueChange = { amt = it }, label = { Text("Amount ₹") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Row(Modifier.fillMaxWidth(), Arrangement.End) {
                            TextButton(onClick = { showAddDeduction = false }) { Text("Cancel") }
                            Button(onClick = {
                                val v = amt.toDoubleOrNull() ?: 0.0
                                if (v > 0) { viewModel.addDeduction(dtype, v, cname); showAddDeduction = false }
                            }, enabled = amt.isNotBlank()) { Text("Add") }
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SaleConfirmationSummary(
                buyerName = if (selectedBuyer != null) selectedBuyer!!.name else buyerSearchText,
                buyerMobile = if (selectedBuyer != null) selectedBuyer!!.mobileNumber else buyerMobile,
                buyerAddress = if (selectedBuyer != null) selectedBuyer!!.address else buyerAddress,
                buyerGst = if (selectedBuyer != null) selectedBuyer!!.gstNumber else buyerGst,
                totals = transactionTotal,
                otherDeductionsTotal = totalOtherDeductions,
                billNumber = autoBillNumber
            )
            }
            
            Spacer(modifier = Modifier.height(180.dp))
        }
    }

    if (showAddItemSheet) {
        AddItemModalSheet(
            viewModel = viewModel,
            onDismiss = { showAddItemSheet = false },
            onItemAdded = { 
                viewModel.addSaleItem(it)
                showAddItemSheet = false 
            }
        )
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
                                        Text("${stringResource(R.string.pending)}: ₹${Formatter.formatCurrency(buyer.pendingAmount)}", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun NewBuyerDetailsFields(
    mobile: String,
    onMobileChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    gst: String,
    onGstChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.new_buyer_details),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        OutlinedTextField(
            value = mobile,
            onValueChange = { 
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    onMobileChange(it)
                }
            },
            label = { Text(stringResource(R.string.mobile_number) + " *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text(stringResource(R.string.address) + " *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = gst,
            onValueChange = onGstChange,
            label = { Text(stringResource(R.string.gst_number) + " (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SaleItemRowCard(
    item: com.dasariravi145.agrolynch.ui.screens.sale.SaleItemDraft, 
    onUpdate: (com.dasariravi145.agrolynch.ui.screens.sale.SaleItemDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Farmer: ${item.arrival.farmerName}", fontSize = 12.sp, color = Color.Gray)
                    Text("${item.arrival.productName} (${item.arrival.grade})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Category: ${item.arrival.productCategory}", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            
            HorizontalDivider(thickness = 0.5.dp)

            // Available Info
            val unit = item.arrival.unit
            val availableOrig = item.arrival.remainingQuantity
            val availableKg = availableOrig * (if(unit == "Ton" || unit == "Boxes") 1000.0 else 1.0)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stock: ${Formatter.formatWeight(availableOrig)} $unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                if (unit != "KG") {
                    Text("Net: ${Formatter.formatWeight(availableKg)} KG", fontSize = 12.sp, color = Color.Gray)
                }
                Text("P. Rate: ₹${Formatter.formatCurrency(if(item.arrival.ratePerKg > 0) item.arrival.ratePerKg else item.arrival.purchaseRate / (if(unit == "Ton") 1000.0 else 1.0))}/KG", fontSize = 12.sp, color = Color.Gray)
            }

            // Editable Fields
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isTon = unit == "Ton"
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = item.rawInputQuantity.ifEmpty { 
                            if(item.inputQuantity == 0.0) "" else Formatter.formatWeight(item.inputQuantity) 
                        },
                        onValueChange = { input ->
                            // Requirement 7: Allow max 3 decimal places
                            if (input.contains(".") && input.substring(input.indexOf(".") + 1).length > 3) return@OutlinedTextField
                            
                            // Requirement 6: If user types ".84", convert/display as "0.84"
                            val sanitizedInput = if (input.startsWith(".")) "0$input" else input
                            
                            val v = sanitizedInput.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(inputQuantity = v, rawInputQuantity = sanitizedInput))
                        },
                        label = { Text("Sale Qty ($unit)") },
                        placeholder = { if (isTon) Text("0.00") }, // Requirement 3: Show "0.00" only as placeholder
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = item.inputQuantity > (availableOrig + 0.0001)
                    )
                    if (isTon && item.inputQuantity > 0) {
                        Text(
                            "Equivalent: ${Formatter.formatNetWeight(item.inputQuantity * 1000.0)}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                    if (item.inputQuantity > (availableOrig + 0.0001)) {
                        Text(
                            "Sale quantity exceeds available stock",
                            fontSize = 10.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = if(item.saleRate == 0.0) "" else Formatter.formatWeight(item.saleRate),
                    onValueChange = { 
                        val v = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(saleRate = v))
                    },
                    label = { Text("Sale Rate/KG") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if(item.laborCharges == 0.0) "" else Formatter.formatWeight(item.laborCharges),
                    onValueChange = { 
                        val v = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(laborCharges = v))
                    },
                    label = { Text("Labor ₹") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = if(item.transportCharges == 0.0) "" else Formatter.formatWeight(item.transportCharges),
                    onValueChange = { 
                        val v = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(transportCharges = v))
                    },
                    label = { Text("Trans ₹") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            
            OutlinedTextField(
                value = if(item.otherCharges == 0.0) "" else Formatter.formatWeight(item.otherCharges),
                onValueChange = { 
                    val v = it.toDoubleOrNull() ?: 0.0
                    onUpdate(item.copy(otherCharges = v))
                },
                label = { Text("Other Charges ₹") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            HorizontalDivider(thickness = 0.5.dp)
            
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Qty: ${Formatter.formatQuantityDisplay(item.inputQuantity, item.arrival.unit)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (item.arrival.unit != "KG") {
                        Text("Net: ${Formatter.formatNetWeight(item.quantity)}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_collection), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        Formatter.formatAmount(item.netAmount),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, color: Color = Color.Black) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SaleConfirmationSummary(
    buyerName: String,
    buyerMobile: String,
    buyerAddress: String,
    buyerGst: String,
    totals: TransactionTotal,
    otherDeductionsTotal: Double,
    billNumber: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(stringResource(R.string.summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Text("Bill: $billNumber", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Text(stringResource(R.string.new_buyer_details), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SummaryRow(stringResource(R.string.farmer_name_label), buyerName)
                SummaryRow(stringResource(R.string.mobile_number), buyerMobile)
                SummaryRow(stringResource(R.string.address), buyerAddress)
                if (buyerGst.isNotBlank()) SummaryRow(stringResource(R.string.gst_number), buyerGst)
            }
            
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            
            Text(stringResource(R.string.amount), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SummaryRow(stringResource(R.string.total_items), Formatter.formatWeight(totals.totalQuantity))
                SummaryRow(stringResource(R.string.gross_amount), "₹${Formatter.formatCurrency(totals.totalSaleAmount)}")
                SummaryRow(stringResource(R.string.labor_rs), "₹${Formatter.formatCurrency(totals.totalLabor)}", color = Color.Red)
                SummaryRow(stringResource(R.string.transport_rs), "₹${Formatter.formatCurrency(totals.totalTransport)}", color = Color.Red)
                SummaryRow(stringResource(R.string.other_rs), "₹${Formatter.formatCurrency(totals.totalOther + otherDeductionsTotal)}", color = Color.Red)
                
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.total_collection), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("₹${Formatter.formatCurrency(totals.totalNetAmount + otherDeductionsTotal)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF15803D))
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemModalSheet(
    viewModel: SaleViewModel,
    onDismiss: () -> Unit,
    onItemAdded: (com.dasariravi145.agrolynch.ui.screens.sale.SaleItemDraft) -> Unit
) {
    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val farmersWithStock by viewModel.farmersWithStock.collectAsStateWithLifecycle()
    
    var selectedFarmerId by remember { mutableStateOf<String?>(null) }
    val stockByFarmer by if (selectedFarmerId != null) 
        viewModel.getAvailableStockByFarmer(selectedFarmerId!!).collectAsStateWithLifecycle(emptyList())
        else remember { mutableStateOf(emptyList<ArrivalEntity>()) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = modalState) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.add_item), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Step 1: Select Farmer
            Text(stringResource(R.string.select_farmer), style = MaterialTheme.typography.labelMedium)
            SelectionDropdown(
                items = farmersWithStock.map { it.farmerName },
                selectedIndex = farmersWithStock.indexOfFirst { it.farmerId == selectedFarmerId },
                onSelect = { index ->
                    selectedFarmerId = farmersWithStock[index].farmerId
                }
            )

            // Step 2: Load Stock Entries
            if (selectedFarmerId != null) {
                val stockList = stockByFarmer.filter { arrival ->
                    // Additional safety check to prevent duplicate selection
                    viewModel.saleItems.value.none { it.arrival.id == arrival.id }
                }
                
                if (stockList.isEmpty()) {
                    Text("No more available stock for this farmer.", modifier = Modifier.padding(8.dp), color = Color.Gray)
                } else {
                    Text(stringResource(R.string.select_stock_entry), style = MaterialTheme.typography.labelMedium)
                    stockList.forEach { arrival ->
                        FarmerStockSelectionCard(
                            arrival = arrival,
                            isSelected = false,
                            onClick = { 
                                val defaultSaleRate = if (arrival.ratePerKg > 0) arrival.ratePerKg else (arrival.purchaseRate / (if(arrival.unit == "Ton") 1000.0 else 1.0))
                                
                                onItemAdded(com.dasariravi145.agrolynch.ui.screens.sale.SaleItemDraft(
                                    arrival = arrival,
                                    inputQuantity = 0.0,
                                    saleRate = defaultSaleRate,
                                    laborCharges = 0.0,
                                    transportCharges = 0.0,
                                    otherCharges = 0.0
                                ))
                                android.widget.Toast.makeText(context, "Added ${arrival.productName} ${arrival.grade}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FarmerStockSelectionCard(arrival: ArrivalEntity, isSelected: Boolean, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(arrival.date))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF2E7D32) else Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${arrival.productName} (${arrival.grade})", fontWeight = FontWeight.Bold)
                Text(dateStr, fontSize = 11.sp, color = Color.Gray)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                val availableInUnit = if (arrival.unit == "Boxes") {
                    if (arrival.quantity > 0) (arrival.remainingQuantity / arrival.quantity) * arrival.numberOfBoxes else 0.0
                } else arrival.remainingQuantity
                
                Text("${stringResource(R.string.stock_label)}: ${Formatter.formatWeight(availableInUnit)} ${arrival.unit}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                Text("${stringResource(R.string.rate)}: ₹${Formatter.formatCurrency(if(arrival.ratePerKg > 0) arrival.ratePerKg else arrival.purchaseRate / (if(arrival.unit == "Ton") 1000.0 else 1.0))}${if(arrival.unit == "Ton" || arrival.unit == "Boxes") "/KG" else ""}", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SelectionDropdown(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
            color = Color.White
        ) {
            Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (selectedIndex >= 0) items[selectedIndex] else "Select...")
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}

@Composable
fun SaleTransactionSummaryCard(
    total: TransactionTotal,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = Color(0xFF111827),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.total_items), color = Color.Gray, fontSize = 12.sp)
                    // We don't have a single unit for multiple items, but we can show total weight or something descriptive
                    Text("Total Net: ${Formatter.formatNetWeight(total.totalQuantity)}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_collection), color = Color.Gray, fontSize = 12.sp)
                    Text(Formatter.formatAmount(total.totalNetAmount), color = Color(0xFF4ADE80), fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                // Requirement 11: Save button should enable when conditions met
                enabled = !isLoading && total.totalQuantity > 0 && total.totalNetAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(stringResource(R.string.confirm_save_sale), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
